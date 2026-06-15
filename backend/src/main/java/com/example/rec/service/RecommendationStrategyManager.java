package com.example.rec.service;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.model.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 推荐策略管理器
 *
 * 持有传统算法和 AI 推荐两套策略，通过 Admin 面板全局切换。
 * AI 模式下采用「缓存优先 + 异步预计算」架构：
 * - 缓存命中 → 毫秒级返回 AI 结果
 * - 缓存未命中 → 立即返回传统算法结果 + 后台异步触发 AI 计算
 * - 下次请求即可享受 AI 推荐结果
 */
@Service
public class RecommendationStrategyManager {

    private static final Logger log = LoggerFactory.getLogger(RecommendationStrategyManager.class);

    public static final String STRATEGY_TRADITIONAL = "traditional";
    public static final String STRATEGY_AI = "ai";

    private final HybridRecommendationStrategy traditionalStrategy;
    private final AiRecommendationStrategy aiStrategy;
    private final AiRecCacheService aiRecCacheService;
    private final AtomicReference<String> activeStrategy = new AtomicReference<>(STRATEGY_TRADITIONAL);

    public RecommendationStrategyManager(HybridRecommendationStrategy traditionalStrategy,
                                          AiRecommendationStrategy aiStrategy,
                                          @Lazy AiRecCacheService aiRecCacheService) {
        this.traditionalStrategy = traditionalStrategy;
        this.aiStrategy = aiStrategy;
        this.aiRecCacheService = aiRecCacheService;
    }

    public String getCurrentStrategyType() {
        return activeStrategy.get();
    }

    public void switchStrategy(String type) {
        if (STRATEGY_TRADITIONAL.equals(type) || STRATEGY_AI.equals(type)) {
            activeStrategy.set(type);
            if (STRATEGY_TRADITIONAL.equals(type)) {
                aiRecCacheService.clearAll();
            }
            log.info("[Strategy] Switched to: {}", type);
        } else {
            throw new IllegalArgumentException("Unknown strategy: " + type + ". Use 'traditional' or 'ai'.");
        }
    }

    public RecommendationStrategy getActiveStrategy() {
        return STRATEGY_AI.equals(activeStrategy.get()) ? aiStrategy : traditionalStrategy;
    }

    /**
     * 统一的 recommend 方法
     * AI 模式下走缓存服务（缓存命中秒回，未命中降级传统 + 异步触发 AI）
     */
    public List<Content> recommend(Long userId, List<Content> candidates) {
        if (STRATEGY_AI.equals(activeStrategy.get())) {
            return aiRecCacheService.getRecommendation(userId, candidates);
        }
        return traditionalStrategy.recommend(userId, candidates);
    }

    /**
     * 统一的 recommendWithScore 方法
     */
    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates) {
        if (STRATEGY_AI.equals(activeStrategy.get())) {
            return aiRecCacheService.getRecommendationWithScore(userId, candidates);
        }
        return traditionalStrategy.recommendWithScore(userId, candidates);
    }

    /**
     * 带自定义权重的推荐（仅传统策略支持）
     */
    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates, Map<String, Double> weights) {
        if (STRATEGY_AI.equals(activeStrategy.get())) {
            return aiRecCacheService.getRecommendationWithScore(userId, candidates);
        }
        return traditionalStrategy.recommendWithScore(userId, candidates, weights);
    }

    /**
     * 失效指定用户的 AI 缓存（用户行为变化时调用）
     */
    public void invalidateAiCache(Long userId) {
        aiRecCacheService.invalidateCache(userId);
    }

    /**
     * 直接获取传统策略（用于对比页面等需要固定传统策略的场景）
     */
    public HybridRecommendationStrategy getTraditionalStrategy() {
        return traditionalStrategy;
    }

    /**
     * 检查 Ollama AI 服务是否可用
     */
    public boolean isAiAvailable() {
        return aiStrategy.isOllamaAvailable();
    }

    /**
     * 获取策略描述信息（含缓存统计）
     */
    public Map<String, Object> getStrategyInfo() {
        AiRecCacheService.CacheStats cacheStats = aiRecCacheService.getStats();
        return Map.of(
            "current", activeStrategy.get(),
            "strategies", List.of(
                Map.of(
                    "type", STRATEGY_TRADITIONAL,
                    "name", "传统多因子算法",
                    "description", "基于用户行为画像的多因子加权打分：互动热度、TF-IDF 内容相似度、话题亲和度、作者亲密度、内容深度匹配、新鲜度偏好 + 动态权重 + 作者多样性惩罚",
                    "active", STRATEGY_TRADITIONAL.equals(activeStrategy.get())
                ),
                Map.of(
                    "type", STRATEGY_AI,
                    "name", "AI 大模型推荐（缓存加速）",
                    "description", "Ollama Qwen 8B 大语言模型驱动 + 异步预计算缓存。首次请求降级为传统算法并后台触发 AI 计算，后续请求毫秒级返回 AI 结果。",
                    "active", STRATEGY_AI.equals(activeStrategy.get())
                )
            ),
            "aiAvailable", isAiAvailable(),
            "cacheStats", Map.of(
                "cachedUsers", cacheStats.totalEntries,
                "freshEntries", cacheStats.freshEntries,
                "ttlMinutes", cacheStats.ttlMinutes
            )
        );
    }
}
