package com.example.rec.service;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.model.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 推荐结果缓存服务
 *
 * 核心设计：异步预计算 + 内存缓存
 * - 首次请求：立即降级到传统算法响应，同时触发异步 AI 计算
 * - 后续请求：直接返回缓存的 AI 结果（毫秒级）
 * - 行为变化：失效缓存，下次请求重新触发 AI 计算
 */
@Service
public class AiRecCacheService {

    private static final Logger log = LoggerFactory.getLogger(AiRecCacheService.class);

    private final AiRecommendationStrategy aiStrategy;
    private final HybridRecommendationStrategy traditionalStrategy;

    @Value("${ai.cache.ttl-minutes:10}")
    private int cacheTtlMinutes;

    private final ConcurrentHashMap<Long, CachedResult> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, AtomicBoolean> computingFlags = new ConcurrentHashMap<>();

    public AiRecCacheService(AiRecommendationStrategy aiStrategy,
                             HybridRecommendationStrategy traditionalStrategy) {
        this.aiStrategy = aiStrategy;
        this.traditionalStrategy = traditionalStrategy;
    }

    /**
     * 获取推荐结果（带缓存）
     * 缓存命中 → 直接返回 AI 结果
     * 缓存未命中 → 返回传统算法结果 + 触发异步 AI 计算
     */
    public List<Content> getRecommendation(Long userId, List<Content> candidates) {
        CachedResult cached = getValidCache(userId);
        if (cached != null && cached.recommendations != null) {
            log.info("[AI Cache] HIT for user {}, age={}s", userId, cached.getAgeSeconds());
            return filterByCurrentCandidates(cached.recommendations, candidates);
        }

        triggerAsyncComputation(userId, candidates);
        log.info("[AI Cache] MISS for user {}, returning traditional results", userId);
        return traditionalStrategy.recommend(userId, candidates);
    }

    /**
     * 获取带评分的推荐结果（带缓存）
     */
    public List<ContentWithScore> getRecommendationWithScore(Long userId, List<Content> candidates) {
        CachedResult cached = getValidCache(userId);
        if (cached != null && cached.scoredRecommendations != null) {
            log.info("[AI Cache] HIT (scored) for user {}, age={}s", userId, cached.getAgeSeconds());
            return cached.scoredRecommendations;
        }

        triggerAsyncScoredComputation(userId, candidates);
        log.info("[AI Cache] MISS (scored) for user {}, returning traditional results", userId);
        return traditionalStrategy.recommendWithScore(userId, candidates);
    }

    /**
     * 异步计算 AI 推荐并写入缓存
     */
    @Async("aiRecExecutor")
    public void triggerAsyncComputation(Long userId, List<Content> candidates) {
        AtomicBoolean computing = computingFlags.computeIfAbsent(userId, k -> new AtomicBoolean(false));
        if (!computing.compareAndSet(false, true)) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            List<Content> result = aiStrategy.recommend(userId, candidates);
            long elapsed = System.currentTimeMillis() - start;

            CachedResult cachedResult = new CachedResult();
            cachedResult.recommendations = result;
            cachedResult.timestamp = System.currentTimeMillis();
            cache.put(userId, cachedResult);

            log.info("[AI Cache] Computed for user {} in {}ms, cached {} items", userId, elapsed, result.size());
        } catch (Exception e) {
            log.error("[AI Cache] Async computation failed for user {}: {}", userId, e.getMessage());
        } finally {
            computing.set(false);
        }
    }

    /**
     * 异步计算带评分的 AI 推荐并写入缓存
     */
    @Async("aiRecExecutor")
    public void triggerAsyncScoredComputation(Long userId, List<Content> candidates) {
        AtomicBoolean computing = computingFlags.computeIfAbsent(userId, k -> new AtomicBoolean(false));
        if (!computing.compareAndSet(false, true)) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            List<ContentWithScore> result = aiStrategy.recommendWithScore(userId, candidates);
            long elapsed = System.currentTimeMillis() - start;

            CachedResult cachedResult = cache.getOrDefault(userId, new CachedResult());
            cachedResult.scoredRecommendations = result;
            cachedResult.timestamp = System.currentTimeMillis();
            cache.put(userId, cachedResult);

            log.info("[AI Cache] Computed (scored) for user {} in {}ms, cached {} items", userId, elapsed, result.size());
        } catch (Exception e) {
            log.error("[AI Cache] Async scored computation failed for user {}: {}", userId, e.getMessage());
        } finally {
            computing.set(false);
        }
    }

    /**
     * 失效指定用户的缓存（用户行为变化时调用）
     */
    public void invalidateCache(Long userId) {
        if (userId != null) {
            cache.remove(userId);
            log.debug("[AI Cache] Invalidated cache for user {}", userId);
        }
    }

    /**
     * 清空所有缓存（切换策略时调用）
     */
    public void clearAll() {
        cache.clear();
        log.info("[AI Cache] All caches cleared");
    }

    /**
     * 检查缓存是否命中且未过期
     */
    public boolean hasFreshCache(Long userId) {
        return getValidCache(userId) != null;
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        stats.totalEntries = cache.size();
        stats.ttlMinutes = cacheTtlMinutes;
        long now = System.currentTimeMillis();
        stats.freshEntries = (int) cache.values().stream()
                .filter(c -> (now - c.timestamp) < cacheTtlMinutes * 60_000L)
                .count();
        return stats;
    }

    private CachedResult getValidCache(Long userId) {
        if (userId == null) return null;
        CachedResult cached = cache.get(userId);
        if (cached == null) return null;

        long age = System.currentTimeMillis() - cached.timestamp;
        if (age > cacheTtlMinutes * 60_000L) {
            cache.remove(userId);
            return null;
        }
        return cached;
    }

    private List<Content> filterByCurrentCandidates(List<Content> cachedList, List<Content> currentCandidates) {
        java.util.Set<Long> validIds = currentCandidates.stream()
                .map(Content::getId)
                .collect(java.util.stream.Collectors.toSet());
        return cachedList.stream()
                .filter(c -> validIds.contains(c.getId()))
                .collect(java.util.stream.Collectors.toList());
    }

    static class CachedResult {
        List<Content> recommendations;
        List<ContentWithScore> scoredRecommendations;
        long timestamp;

        long getAgeSeconds() {
            return (System.currentTimeMillis() - timestamp) / 1000;
        }
    }

    public static class CacheStats {
        public int totalEntries;
        public int freshEntries;
        public int ttlMinutes;
    }
}
