package com.example.rec.service;

import com.example.rec.dto.PipelineStats;
import com.example.rec.model.Content;
import com.example.rec.model.Behavior;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.BehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final ContentRepository contentRepository;
    private final RecommendationStrategyManager strategyManager;
    private final RelationService relationService;
    private final NegativeSignalService negativeSignalService;
    private final BehaviorRepository behaviorRepository;
    private final com.example.rec.repository.UserRepository userRepository;

    public RecommendationService(ContentRepository contentRepository,
                                  RecommendationStrategyManager strategyManager,
                                  RelationService relationService,
                                  NegativeSignalService negativeSignalService,
                                  BehaviorRepository behaviorRepository,
                                  com.example.rec.repository.UserRepository userRepository) {
        this.contentRepository = contentRepository;
        this.strategyManager = strategyManager;
        this.relationService = relationService;
        this.negativeSignalService = negativeSignalService;
        this.behaviorRepository = behaviorRepository;
        this.userRepository = userRepository;
    }

    /**
     * X-Inspired Feed Algorithm
     * Dual Sources: In-Network (Thunder) + Out-of-Network (Phoenix)
     * With Negative Signal Filtering
     */
    public List<Content> getRecommendedFeed(Long userId) {
        List<Content> finalCandidates = buildCandidatePool(userId);

        // === RANKING (via active strategy) ===
        List<Content> result = strategyManager.recommend(userId, finalCandidates);
        
        // Fill isLiked status for current user
        if (userId != null) {
            fillIsLiked(result, userId);
        }
        
        return result;
    }
    
    /**
     * 填充用户点赞/点踩状态
     */
    private void fillIsLiked(List<Content> contents, Long userId) {
        if (contents == null || contents.isEmpty()) return;
        List<Behavior> likes = behaviorRepository.findByUserIdAndType(userId, "LIKE");
        Set<Long> likedContentIds = likes.stream()
                .map(Behavior::getContentId)
                .collect(Collectors.toSet());

        List<Behavior> dislikes = behaviorRepository.findByUserIdAndType(userId, "DISLIKE");
        Set<Long> dislikedContentIds = dislikes.stream()
                .map(Behavior::getContentId)
                .collect(Collectors.toSet());

        for (Content c : contents) {
            if (likedContentIds.contains(c.getId())) {
                c.setLiked(true);
            }
            if (dislikedContentIds.contains(c.getId())) {
                c.setDisliked(true);
            }
        }
    }

    /**
     * 带评分详情的推荐 Feed（用于主页 Debug 模式）
     * 优先使用用户自定义权重，如果没有则走默认权重
     */
    public List<com.example.rec.dto.ContentWithScore> getRecommendedFeedWithScore(Long userId) {
        List<Content> finalCandidates = buildCandidatePool(userId);

        // 检查用户是否有自定义权重
        Map<String, Double> customWeights = loadUserCustomWeights(userId);
        if (customWeights != null) {
            return strategyManager.recommendWithScore(userId, finalCandidates, customWeights);
        }
        return strategyManager.recommendWithScore(userId, finalCandidates);
    }

    /**
     * 读取用户自定义权重（从 User.customWeights JSON 字段）
     * 返回 null 表示使用默认权重
     */
    private Map<String, Double> loadUserCustomWeights(Long userId) {
        if (userId == null) return null;
        try {
            Optional<com.example.rec.model.User> opt = userRepository.findById(userId);
            if (opt.isPresent() && opt.get().getCustomWeights() != null) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(
                        opt.get().getCustomWeights(),
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {}
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to load custom weights for user " + userId + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Phase 28: 带管道统计的推荐（用于漏斗图）
     */
    public Map<String, Object> getRecommendedFeedWithPipeline(Long userId) {
        PipelineStats stats = new PipelineStats();
        
        // 统计全量候选
        int totalCandidates = (int) contentRepository.count();
        stats.setTotalCandidates(totalCandidates);

        // 构建候选池（含过滤统计）
        List<Content> candidates = buildCandidatePoolWithStats(userId, stats);
        stats.setAfterNegativeFilter(candidates.size());

        // 获取带评分的结果（对比页面始终使用传统策略）
        List<com.example.rec.dto.ContentWithScore> scored = strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates);
        stats.setAfterScoring(scored.size());
        
        // 最终返回数量
        int finalCount = Math.min(scored.size(), 50);
        stats.setAfterDiversity(finalCount);

        Map<String, Object> result = new HashMap<>();
        result.put("contents", scored);
        result.put("pipelineStats", stats);
        return result;
    }

    /**
     * Phase 28: 时间倒序 Feed（带评分详情，用于对比实验）
     */
    public List<com.example.rec.dto.ContentWithScore> getChronologicalFeedWithScore(Long userId) {
        List<Content> candidates = buildCandidatePool(userId);
        
        // 按时间倒序排列
        candidates.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        // 仍然计算评分（但不改变排序），用于对比展示（始终使用传统策略）
        List<com.example.rec.dto.ContentWithScore> result = strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates);
        
        // 重新按时间排序（recommendWithScore 会改变顺序）
        result.sort((a, b) -> {
            if (a.getContent().getCreatedAt() == null && b.getContent().getCreatedAt() == null) return 0;
            if (a.getContent().getCreatedAt() == null) return 1;
            if (b.getContent().getCreatedAt() == null) return -1;
            return b.getContent().getCreatedAt().compareTo(a.getContent().getCreatedAt());
        });

        // 重新设置 rank
        for (int i = 0; i < result.size(); i++) {
            result.get(i).setRank(i + 1);
        }

        return result;
    }

    /**
     * Phase 28: 带自定义权重的推荐（用于参数调节面板）
     */
    public List<com.example.rec.dto.ContentWithScore> getRecommendedFeedWithWeights(Long userId, Map<String, Double> weights) {
        List<Content> candidates = buildCandidatePool(userId);
        return strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates, weights);
    }

    public String getCurrentStrategyType() {
        return strategyManager.getCurrentStrategyType();
    }

    // ============================================================
    // === 候选池构建（抽取公共逻辑，避免重复代码）===
    // ============================================================

    /**
     * 构建候选内容池（不含统计）
     */
    private List<Content> buildCandidatePool(Long userId) {
        return buildCandidatePoolWithStats(userId, null);
    }

    /**
     * 构建候选内容池（可选统计收集）
     */
    private List<Content> buildCandidatePoolWithStats(Long userId, PipelineStats stats) {
        List<Content> finalCandidates = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();
        
        // === NEGATIVE SIGNAL FILTERING ===
        Set<Long> blockedAuthorIds = new HashSet<>();
        Set<Long> hiddenContentIds = new HashSet<>();
        if (userId != null) {
            blockedAuthorIds = negativeSignalService.getBlockedAuthorIds(userId);
            hiddenContentIds = negativeSignalService.getHiddenContentIds(userId);
        }

        // === SOURCE 1: In-Network (Thunder) ===
        List<Content> inNetworkCandidates = new ArrayList<>();
        if (userId != null) {
            List<Long> followingIds = relationService.getFollowingIds(userId);
            if (!followingIds.isEmpty()) {
                inNetworkCandidates = contentRepository.findByAuthorIdInOrderByCreatedAtDesc(followingIds);
            }
        }
        
        int inNetworkCount = 0;
        for (Content c : inNetworkCandidates) {
            if (c.getParentContent() != null) continue; // 跳过回复
            if (!seenIds.contains(c.getId()) && !hiddenContentIds.contains(c.getId())) {
                if (c.getAuthor() == null || !blockedAuthorIds.contains(c.getAuthor().getId())) {
                    c.setNetworkSource("IN_NETWORK");
                    finalCandidates.add(c);
                    seenIds.add(c.getId());
                    inNetworkCount++;
                }
            }
        }

        // === SOURCE 2: Out-of-Network (Phoenix) ===
        List<Content> outOfNetworkCandidates;
        if (userId != null) {
            outOfNetworkCandidates = contentRepository.findAllByAuthorIdNot(userId);
        } else {
            outOfNetworkCandidates = contentRepository.findAll();
        }
        
        int outNetworkCount = 0;
        for (Content c : outOfNetworkCandidates) {
            if (c.getParentContent() != null) continue; // 跳过回复
            if (!seenIds.contains(c.getId()) && !hiddenContentIds.contains(c.getId())) {
                if (c.getAuthor() == null || !blockedAuthorIds.contains(c.getAuthor().getId())) {
                    c.setNetworkSource("OUT_OF_NETWORK");
                    finalCandidates.add(c);
                    seenIds.add(c.getId());
                    outNetworkCount++;
                }
            }
        }

        // 填充统计数据
        if (stats != null) {
            stats.setInNetworkCount(inNetworkCount);
            stats.setOutNetworkCount(outNetworkCount);
        }

        return finalCandidates;
    }
}
