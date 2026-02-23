package com.example.rec.service;

import com.example.rec.model.Content;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * X-Inspired Hybrid Recommendation Strategy
 * Based on: https://github.com/xai-org/x-algorithm
 * 
 * Phase 25 优化：
 * - 新增转发/引用权重
 * - 热门话题加成
 * - 互动率因子
 * - 优化时间衰减
 * 
 * Phase 28 增强：
 * - TF-IDF 内容相似度加成
 * - 分段时间衰减曲线（模拟内容生命周期）
 * - 支持动态权重参数（答辩演示调参）
 */
@Component
public class HybridRecommendationStrategy implements RecommendationStrategy {

    private final PersonaService personaService;
    private final TrendingService trendingService;
    private final TfIdfService tfIdfService;

    // === WEIGHT CONFIGURATION (X-Style Multi-Action Weights) ===
    private static final double WEIGHT_LIKE = 0.5;
    private static final double WEIGHT_REPLY = 1.2;       // 评论权重提高
    private static final double WEIGHT_VIEW = 0.05;       // 浏览权重降低
    private static final double WEIGHT_REPOST = 2.0;      // 新增: 转发
    private static final double WEIGHT_QUOTE = 1.8;       // 新增: 引用
    private static final double WEIGHT_IN_NETWORK_BOOST = 1.5;  // Boost for followed accounts
    private static final double AUTHOR_DIVERSITY_DECAY = 0.7;   // Each repeat author gets 70% of previous score
    
    // === NEW BOOST FACTORS ===
    private static final double TRENDING_BOOST = 50.0;    // 热门话题加成
    private static final double ENGAGEMENT_RATE_WEIGHT = 0.3; // 互动率权重
    private static final double CONTENT_SIMILARITY_BOOST = 80.0; // TF-IDF 内容相似度加成系数

    public HybridRecommendationStrategy(PersonaService personaService, 
                                         TrendingService trendingService,
                                         TfIdfService tfIdfService) {
        this.personaService = personaService;
        this.trendingService = trendingService;
        this.tfIdfService = tfIdfService;
    }

    @Override
    public List<Content> recommend(Long userId, List<Content> candidates) {
        // 0. Get user interests (for personalization boost)
        List<String> userInterests = new ArrayList<>();
        Map<String, Double> userProfile = Collections.emptyMap();
        Map<String, Double> globalIdf = Collections.emptyMap();
        
        if (userId != null) {
            try {
                Map<String, Object> persona = personaService.getUserPersona(userId);
                if (persona.containsKey("interestTags")) {
                    userInterests = (List<String>) persona.get("interestTags");
                }
            } catch (Exception e) {
                System.err.println("Failed to get persona for user " + userId);
            }
            
            // Phase 28: 构建 TF-IDF 用户画像
            try {
                userProfile = tfIdfService.getUserProfileVector(userId);
                if (!userProfile.isEmpty()) {
                    globalIdf = tfIdfService.buildGlobalIdf();
                }
            } catch (Exception e) {
                System.err.println("Failed to build TF-IDF profile for user " + userId);
            }
        }
        final List<String> interests = userInterests;
        final Map<String, Double> profile = userProfile;
        final Map<String, Double> idf = globalIdf;

        // 1. Calculate raw scores for all candidates
        List<ScoredContent> scoredList = candidates.stream()
                .map(c -> new ScoredContent(c, calculateScore(c, interests, profile, idf)))
                .collect(Collectors.toList());

        // 2. Apply Author Diversity Penalty (X-Style)
        applyAuthorDiversityPenalty(scoredList);

        // 3. Sort by final score (descending) and extract Content objects
        return scoredList.stream()
                .sorted(Comparator.comparingDouble(ScoredContent::getScore).reversed())
                .map(ScoredContent::getContent)
                .collect(Collectors.toList());
    }

    /**
     * X-Style Multi-Action Weighted Score (Phase 28 增强版)
     * Final Score = baseEngagement × inNetworkBoost × (1 + engagementRate) / timeDecay
     *             + trendingBoost + personalizationBoost + contentSimilarityBoost + randomJitter
     */
    private double calculateScore(Content content, List<String> userInterests,
                                   Map<String, Double> userProfile, Map<String, Double> idf) {
        // === Multi-Action Weighted Engagement (新增转发/引用) ===
        int likeCount = content.getLikeCount() != null ? content.getLikeCount() : 0;
        int commentCount = content.getCommentCount() != null ? content.getCommentCount() : 0;
        int viewCount = content.getViewCount() != null ? content.getViewCount() : 0;
        int repostCount = content.getRepostCount() != null ? content.getRepostCount() : 0;
        
        double engagementScore = 
                (likeCount * WEIGHT_LIKE) +
                (commentCount * WEIGHT_REPLY) +
                (viewCount * WEIGHT_VIEW) +
                (repostCount * WEIGHT_REPOST);
        
        // === 互动率因子 (Engagement Rate) ===
        double engagementRate = 0.0;
        if (viewCount > 0) {
            engagementRate = (double)(likeCount + commentCount + repostCount) / viewCount;
        }
        engagementScore *= (1.0 + engagementRate * ENGAGEMENT_RATE_WEIGHT);

        // === In-Network Boost (Thunder) ===
        if ("IN_NETWORK".equals(content.getCategory())) {
            engagementScore *= WEIGHT_IN_NETWORK_BOOST;
        }

        // === Personalization Boost (标签匹配) ===
        double personalizationBoost = 0.0;
        if (userInterests != null && !userInterests.isEmpty() && content.getTags() != null) {
            for (var tag : content.getTags()) {
                if (userInterests.contains(tag.getName())) {
                    personalizationBoost += 100.0;
                }
            }
        }

        // === TF-IDF 内容相似度加成 (Phase 28 新增) ===
        double contentSimilarityBoost = 0.0;
        if (userProfile != null && !userProfile.isEmpty() && idf != null && !idf.isEmpty()) {
            try {
                double similarity = tfIdfService.getContentSimilarityScore(content, userProfile, idf);
                contentSimilarityBoost = similarity * CONTENT_SIMILARITY_BOOST;
            } catch (Exception e) {
                // 静默处理
            }
        }

        // === 热门话题加成 (Trending Boost) ===
        double trendingBoost = 0.0;
        try {
            int trendingTagCount = trendingService.countTrendingTagsInContent(content);
            trendingBoost = trendingTagCount * TRENDING_BOOST;
        } catch (Exception e) {
            // 静默处理
        }

        // === Time Decay (Phase 28: 分段指数衰减) ===
        double timeDecay = calculateTimeDecay(content);

        // === Random Jitter ===
        double jitter = Math.random() * 5.0;

        // === Final Score ===
        return (engagementScore / timeDecay) + personalizationBoost + contentSimilarityBoost + trendingBoost + jitter;
    }

    /**
     * Phase 28: 分段时间衰减函数
     * 模拟真实内容生命周期：
     * - 0-6h:   黄金期，衰减极慢
     * - 6-24h:  正常衰减
     * - 24-72h: 加速衰减
     * - 72h+:   快速衰减但永不归零
     */
    private double calculateTimeDecay(Content content) {
        long hoursDiff = 1;
        if (content.getCreatedAt() != null) {
            hoursDiff = Math.max(1, Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours());
        }
        
        if (hoursDiff <= 6) {
            // 黄金期：几乎不衰减
            return 1.0 + hoursDiff * 0.02;
        } else if (hoursDiff <= 24) {
            // 正常衰减期
            return 1.12 + (hoursDiff - 6) * 0.05;
        } else if (hoursDiff <= 72) {
            // 加速衰减期
            return 2.02 + (hoursDiff - 24) * 0.08;
        } else {
            // 长尾衰减：对数增长，永不归零
            return 5.86 + Math.log(hoursDiff - 72 + 1) * 2.0;
        }
    }

    /**
     * Author Diversity Penalty (X-Style)
     */
    private void applyAuthorDiversityPenalty(List<ScoredContent> scoredList) {
        scoredList.sort(Comparator.comparingDouble(ScoredContent::getScore).reversed());
        Map<Long, Integer> authorCount = new HashMap<>();

        for (ScoredContent sc : scoredList) {
            Long authorId = sc.getContent().getAuthor() != null ? sc.getContent().getAuthor().getId() : 0L;
            int count = authorCount.getOrDefault(authorId, 0);
            
            if (count > 0) {
                double penalty = Math.pow(AUTHOR_DIVERSITY_DECAY, count);
                sc.setScore(sc.getScore() * penalty);
            }
            
            authorCount.put(authorId, count + 1);
        }
    }

    // =====================================================
    // === DEBUG MODE: 带评分详情的推荐（用于答辩展示） ===
    // =====================================================

    /**
     * 带评分详情的推荐方法（支持自定义权重参数）
     */
    public List<com.example.rec.dto.ContentWithScore> recommendWithScore(Long userId, List<Content> candidates) {
        return recommendWithScore(userId, candidates, null);
    }

    /**
     * 带评分详情的推荐方法（支持自定义权重参数 - Phase 28 参数调节面板）
     * 
     * @param weights 可选的自定义权重：wLike, wReply, wRepost, wPersonal, wTrending, wSimilarity
     */
    public List<com.example.rec.dto.ContentWithScore> recommendWithScore(Long userId, List<Content> candidates, Map<String, Double> weights) {
        // 获取用户兴趣
        List<String> userInterests = new ArrayList<>();
        Map<String, Double> userProfile = Collections.emptyMap();
        Map<String, Double> globalIdf = Collections.emptyMap();

        if (userId != null) {
            try {
                Map<String, Object> persona = personaService.getUserPersona(userId);
                if (persona.containsKey("interestTags")) {
                    userInterests = (List<String>) persona.get("interestTags");
                }
            } catch (Exception e) {
                System.err.println("Failed to get persona for user " + userId);
            }
            
            try {
                userProfile = tfIdfService.getUserProfileVector(userId);
                if (!userProfile.isEmpty()) {
                    globalIdf = tfIdfService.buildGlobalIdf();
                }
            } catch (Exception e) {
                System.err.println("Failed to build TF-IDF profile for user " + userId);
            }
        }
        final List<String> interests = userInterests;
        final Map<String, Double> profile = userProfile;
        final Map<String, Double> idf = globalIdf;

        // 计算评分详情
        List<ScoredContentWithDetails> scoredList = candidates.stream()
                .map(c -> calculateScoreWithDetails(c, interests, profile, idf, weights))
                .collect(Collectors.toList());

        // 应用作者多样性惩罚
        applyAuthorDiversityPenaltyWithDetails(scoredList);

        // 排序并构建返回结果
        scoredList.sort(Comparator.comparingDouble(ScoredContentWithDetails::getFinalScore).reversed());
        
        List<com.example.rec.dto.ContentWithScore> result = new ArrayList<>();
        int rank = 1;
        for (ScoredContentWithDetails sc : scoredList) {
            result.add(new com.example.rec.dto.ContentWithScore(sc.getContent(), sc.getBreakdown(), rank++));
        }
        return result;
    }

    /**
     * 计算评分并返回详细分解（支持自定义权重）
     */
    private ScoredContentWithDetails calculateScoreWithDetails(Content content, List<String> userInterests,
                                                                Map<String, Double> userProfile, Map<String, Double> idf,
                                                                Map<String, Double> weights) {
        com.example.rec.dto.ScoreBreakdown breakdown = new com.example.rec.dto.ScoreBreakdown();
        
        // 读取权重（优先使用自定义，否则用默认值）
        double wLike = getWeight(weights, "wLike", WEIGHT_LIKE);
        double wReply = getWeight(weights, "wReply", WEIGHT_REPLY);
        double wRepost = getWeight(weights, "wRepost", WEIGHT_REPOST);
        double wPersonal = getWeight(weights, "wPersonal", 100.0);
        double wTrending = getWeight(weights, "wTrending", TRENDING_BOOST);
        double wSimilarity = getWeight(weights, "wSimilarity", CONTENT_SIMILARITY_BOOST);
        
        // 基础数据
        int likeCount = content.getLikeCount() != null ? content.getLikeCount() : 0;
        int commentCount = content.getCommentCount() != null ? content.getCommentCount() : 0;
        int viewCount = content.getViewCount() != null ? content.getViewCount() : 0;
        int repostCount = content.getRepostCount() != null ? content.getRepostCount() : 0;
        
        breakdown.setLikeCount(likeCount);
        breakdown.setCommentCount(commentCount);
        breakdown.setViewCount(viewCount);
        breakdown.setRepostCount(repostCount);

        // 基础互动分（使用可调权重）
        double baseEngagement = 
                (likeCount * wLike) +
                (commentCount * wReply) +
                (viewCount * WEIGHT_VIEW) +
                (repostCount * wRepost);
        breakdown.setBaseEngagement(Math.round(baseEngagement * 100.0) / 100.0);

        // 互动率
        double engagementRate = 0.0;
        if (viewCount > 0) {
            engagementRate = (double)(likeCount + commentCount + repostCount) / viewCount;
        }
        breakdown.setEngagementRate(Math.round(engagementRate * 1000.0) / 1000.0);
        
        double engagementScore = baseEngagement * (1.0 + engagementRate * ENGAGEMENT_RATE_WEIGHT);

        // In-Network 判断
        boolean isInNetwork = "IN_NETWORK".equals(content.getCategory());
        breakdown.setInNetwork(isInNetwork);
        if (isInNetwork) {
            engagementScore *= WEIGHT_IN_NETWORK_BOOST;
        }

        // 个性化加成（使用可调权重）
        double personalizationBoost = 0.0;
        StringBuilder matchedTags = new StringBuilder();
        if (userInterests != null && !userInterests.isEmpty() && content.getTags() != null) {
            for (var tag : content.getTags()) {
                if (userInterests.contains(tag.getName())) {
                    personalizationBoost += wPersonal;
                    if (matchedTags.length() > 0) matchedTags.append(", ");
                    matchedTags.append(tag.getName());
                }
            }
        }
        breakdown.setPersonalizationBoost(personalizationBoost);
        breakdown.setMatchedTags(matchedTags.toString());

        // TF-IDF 内容相似度加成 (Phase 28)
        double contentSimilarityBoost = 0.0;
        if (userProfile != null && !userProfile.isEmpty() && idf != null && !idf.isEmpty()) {
            try {
                double similarity = tfIdfService.getContentSimilarityScore(content, userProfile, idf);
                contentSimilarityBoost = similarity * wSimilarity;
            } catch (Exception e) {
                // 静默处理
            }
        }
        breakdown.setContentSimilarityBoost(Math.round(contentSimilarityBoost * 100.0) / 100.0);

        // 热门话题加成（使用可调权重）
        double trendingBoost = 0.0;
        try {
            int trendingTagCount = trendingService.countTrendingTagsInContent(content);
            trendingBoost = trendingTagCount * wTrending;
        } catch (Exception e) {
            // 静默处理
        }
        breakdown.setTrendingBoost(trendingBoost);

        // 时间衰减 (Phase 28 分段版本)
        long hoursDiff = 1;
        if (content.getCreatedAt() != null) {
            hoursDiff = Math.max(1, Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours());
        }
        double timeDecay = calculateTimeDecay(content);
        breakdown.setTimeDecayFactor(Math.round((1.0 / timeDecay) * 1000.0) / 1000.0);
        breakdown.setHoursAgo(hoursDiff);

        // 随机探索因子
        double jitter = Math.random() * 5.0;
        breakdown.setJitter(Math.round(jitter * 100.0) / 100.0);

        // 最终评分
        double finalScore = (engagementScore / timeDecay) + personalizationBoost + contentSimilarityBoost + trendingBoost + jitter;
        breakdown.setFinalScore(Math.round(finalScore * 100.0) / 100.0);

        return new ScoredContentWithDetails(content, breakdown);
    }

    /**
     * 从权重Map中读取自定义权重，如果不存在则使用默认值
     */
    private double getWeight(Map<String, Double> weights, String key, double defaultValue) {
        if (weights == null || !weights.containsKey(key)) return defaultValue;
        return weights.get(key);
    }

    /**
     * 应用作者多样性惩罚（带详情版本）
     */
    private void applyAuthorDiversityPenaltyWithDetails(List<ScoredContentWithDetails> scoredList) {
        scoredList.sort(Comparator.comparingDouble(ScoredContentWithDetails::getFinalScore).reversed());
        Map<Long, Integer> authorCount = new HashMap<>();

        for (ScoredContentWithDetails sc : scoredList) {
            Long authorId = sc.getContent().getAuthor() != null ? sc.getContent().getAuthor().getId() : 0L;
            int count = authorCount.getOrDefault(authorId, 0);
            
            if (count > 0) {
                double penalty = Math.pow(AUTHOR_DIVERSITY_DECAY, count);
                double newScore = sc.getFinalScore() * penalty;
                sc.getBreakdown().setFinalScore(Math.round(newScore * 100.0) / 100.0);
            }
            
            authorCount.put(authorId, count + 1);
        }
    }

    // === Helper Classes ===
    private static class ScoredContent {
        private final Content content;
        private double score;

        public ScoredContent(Content content, double score) {
            this.content = content;
            this.score = score;
        }

        public Content getContent() { return content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    private static class ScoredContentWithDetails {
        private final Content content;
        private final com.example.rec.dto.ScoreBreakdown breakdown;

        public ScoredContentWithDetails(Content content, com.example.rec.dto.ScoreBreakdown breakdown) {
            this.content = content;
            this.breakdown = breakdown;
        }

        public Content getContent() { return content; }
        public com.example.rec.dto.ScoreBreakdown getBreakdown() { return breakdown; }
        public double getFinalScore() { return breakdown.getFinalScore(); }
    }
}
