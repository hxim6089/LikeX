package com.example.rec.service;

import com.example.rec.dto.PipelineStats;
import com.example.rec.model.Content;
import com.example.rec.model.Behavior;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.BehaviorRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐服务（推荐管道的入口）
 *
 * 【职责】
 * 1. 构建候选内容池：In-Network（关注用户发布） + Out-of-Network（全站内容）
 * 2. 负反馈过滤：排除被屏蔽的作者和被隐藏的帖子
 * 3. 调用推荐策略（传统/AI）进行排序
 * 4. 填充用户对帖子的点赞/点踩状态
 *
 * 【候选池双源架构（参考 X/Twitter）】
 * - In-Network (Thunder)：用户关注的人发布的内容，标记为 "IN_NETWORK"
 * - Out-of-Network (Phoenix)：全站其他用户发布的内容，标记为 "OUT_OF_NETWORK"
 * 两类内容混合后统一送入推荐策略排序。
 */
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

        List<com.example.rec.dto.ContentWithScore> result;
        Map<String, Double> customWeights = loadUserCustomWeights(userId);
        if (customWeights != null) {
            result = strategyManager.recommendWithScore(userId, finalCandidates, customWeights);
        } else {
            result = strategyManager.recommendWithScore(userId, finalCandidates);
        }

        if (userId != null && result != null && !result.isEmpty()) {
            List<Content> contents = result.stream()
                    .map(com.example.rec.dto.ContentWithScore::getContent)
                    .collect(Collectors.toList());
            fillIsLiked(contents, userId);
        }
        return result;
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
        return getRecommendedFeedWithPipeline(userId, null);
    }

    /**
     * 带管道统计的推荐，可传入临时权重用于算法验证页的参数调节演示。
     */
    public Map<String, Object> getRecommendedFeedWithPipeline(Long userId, Map<String, Double> weights) {
        PipelineStats stats = new PipelineStats();
        
        // 统计全量候选
        int totalCandidates = (int) contentRepository.count();
        stats.setTotalCandidates(totalCandidates);

        // 构建候选池（含过滤统计）
        List<Content> candidates = buildCandidatePoolWithStats(userId, stats);
        stats.setAfterNegativeFilter(candidates.size());

        // 获取带评分的结果（对比页面始终使用传统策略）
        List<com.example.rec.dto.ContentWithScore> scored =
                weights != null && !weights.isEmpty()
                        ? strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates, weights)
                        : strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates);
        
        // 最终返回数量
        int finalCount = Math.min(scored.size(), 50);
        stats.setAfterScoring(calculateRankingCutoffCount(scored, finalCount));
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
        return getChronologicalFeedWithScore(userId, null);
    }

    /**
     * 时间倒序 Feed（带评分详情），可传入临时权重用于调参对比。
     */
    public List<com.example.rec.dto.ContentWithScore> getChronologicalFeedWithScore(Long userId, Map<String, Double> weights) {
        List<Content> candidates = buildCandidatePool(userId);
        
        // 按时间倒序排列
        candidates.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        // 仍然计算评分（但不改变排序），用于对比展示（始终使用传统策略）
        List<com.example.rec.dto.ContentWithScore> result =
                weights != null && !weights.isEmpty()
                        ? strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates, weights)
                        : strategyManager.getTraditionalStrategy().recommendWithScore(userId, candidates);
        
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

    /**
     * 排序截断统计：按当前权重打分后，保留超过动态质量线的候选数量。
     * 这样调参会影响漏斗中的“排序截断”阶段，但不改变最终推荐主流程。
     */
    private int calculateRankingCutoffCount(List<com.example.rec.dto.ContentWithScore> scored, int minimumCount) {
        if (scored == null || scored.isEmpty()) return 0;

        double topScore = finalScore(scored.get(0));
        double averageScore = scored.stream()
                .mapToDouble(this::finalScore)
                .average()
                .orElse(0.0);
        double threshold = Math.max(averageScore, topScore * 0.2);

        int qualified = (int) scored.stream()
                .filter(item -> finalScore(item) >= threshold)
                .count();

        return Math.min(scored.size(), Math.max(minimumCount, qualified));
    }

    private double finalScore(com.example.rec.dto.ContentWithScore item) {
        if (item == null || item.getScoreBreakdown() == null) return 0.0;
        return item.getScoreBreakdown().getFinalScore();
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
        
        Set<Long> sourceSeenIds = new HashSet<>();
        int inNetworkCount = 0;
        for (Content c : inNetworkCandidates) {
            if (c.getParentContent() != null) continue; // 跳过回复
            if (sourceSeenIds.add(c.getId())) {
                inNetworkCount++;
            }
            if (!seenIds.contains(c.getId()) && !hiddenContentIds.contains(c.getId())) {
                if (c.getAuthor() == null || !blockedAuthorIds.contains(c.getAuthor().getId())) {
                    c.setNetworkSource("IN_NETWORK");
                    finalCandidates.add(c);
                    seenIds.add(c.getId());
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
            if (!sourceSeenIds.add(c.getId())) continue;
            outNetworkCount++;
            if (!seenIds.contains(c.getId()) && !hiddenContentIds.contains(c.getId())) {
                if (c.getAuthor() == null || !blockedAuthorIds.contains(c.getAuthor().getId())) {
                    c.setNetworkSource("OUT_OF_NETWORK");
                    finalCandidates.add(c);
                    seenIds.add(c.getId());
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
