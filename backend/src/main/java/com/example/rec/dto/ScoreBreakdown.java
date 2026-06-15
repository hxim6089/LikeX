package com.example.rec.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 推荐评分详情 DTO
 * 详细分解评分因子 + 行为画像驱动的个性化信息
 */
public class ScoreBreakdown {
    
    private double finalScore;
    private double baseEngagement;
    private double engagementRate;
    private double timeDecayFactor;
    private double trendingBoost;
    private double personalizationBoost;
    private double jitter;
    private boolean isInNetwork;
    private String matchedTags;
    private long hoursAgo;
    
    private int likeCount;
    private int commentCount;
    private int repostCount;
    private int viewCount;
    private double contentSimilarityBoost;

    // ===== 新增：行为画像驱动的个性化字段 =====
    private double topicAffinityBoost;     // 话题亲和度加成
    private double authorAffinityBoost;    // 作者亲密度加成
    private double depthMatchBoost;        // 内容深度匹配加成
    private double freshnessBoost;         // 新鲜度匹配加成
    private double collaborativeFilteringBoost; // 协同过滤加成
    private List<String> recommendReasons = new ArrayList<>(); // 推荐理由列表
    private String userStage;              // 用户阶段: COLD_START / BEGINNER / ACTIVE
    private String profileSummary;         // 用户画像摘要
    private Map<String, Double> dynamicWeights; // 当前生效的动态权重

    public ScoreBreakdown() {}

    // ===== Getters and Setters =====
    
    public double getFinalScore() { return finalScore; }
    public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

    public double getBaseEngagement() { return baseEngagement; }
    public void setBaseEngagement(double baseEngagement) { this.baseEngagement = baseEngagement; }

    public double getEngagementRate() { return engagementRate; }
    public void setEngagementRate(double engagementRate) { this.engagementRate = engagementRate; }

    public double getTimeDecayFactor() { return timeDecayFactor; }
    public void setTimeDecayFactor(double timeDecayFactor) { this.timeDecayFactor = timeDecayFactor; }

    public double getTrendingBoost() { return trendingBoost; }
    public void setTrendingBoost(double trendingBoost) { this.trendingBoost = trendingBoost; }

    public double getPersonalizationBoost() { return personalizationBoost; }
    public void setPersonalizationBoost(double personalizationBoost) { this.personalizationBoost = personalizationBoost; }

    public double getJitter() { return jitter; }
    public void setJitter(double jitter) { this.jitter = jitter; }

    public boolean isInNetwork() { return isInNetwork; }
    public void setInNetwork(boolean inNetwork) { isInNetwork = inNetwork; }

    public String getMatchedTags() { return matchedTags; }
    public void setMatchedTags(String matchedTags) { this.matchedTags = matchedTags; }

    public long getHoursAgo() { return hoursAgo; }
    public void setHoursAgo(long hoursAgo) { this.hoursAgo = hoursAgo; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getRepostCount() { return repostCount; }
    public void setRepostCount(int repostCount) { this.repostCount = repostCount; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public double getContentSimilarityBoost() { return contentSimilarityBoost; }
    public void setContentSimilarityBoost(double contentSimilarityBoost) { this.contentSimilarityBoost = contentSimilarityBoost; }

    public double getTopicAffinityBoost() { return topicAffinityBoost; }
    public void setTopicAffinityBoost(double topicAffinityBoost) { this.topicAffinityBoost = topicAffinityBoost; }

    public double getAuthorAffinityBoost() { return authorAffinityBoost; }
    public void setAuthorAffinityBoost(double authorAffinityBoost) { this.authorAffinityBoost = authorAffinityBoost; }

    public double getDepthMatchBoost() { return depthMatchBoost; }
    public void setDepthMatchBoost(double depthMatchBoost) { this.depthMatchBoost = depthMatchBoost; }

    public double getFreshnessBoost() { return freshnessBoost; }
    public void setFreshnessBoost(double freshnessBoost) { this.freshnessBoost = freshnessBoost; }

    public double getCollaborativeFilteringBoost() { return collaborativeFilteringBoost; }
    public void setCollaborativeFilteringBoost(double collaborativeFilteringBoost) { this.collaborativeFilteringBoost = collaborativeFilteringBoost; }

    public List<String> getRecommendReasons() { return recommendReasons; }
    public void setRecommendReasons(List<String> recommendReasons) { this.recommendReasons = recommendReasons; }

    public String getUserStage() { return userStage; }
    public void setUserStage(String userStage) { this.userStage = userStage; }

    public String getProfileSummary() { return profileSummary; }
    public void setProfileSummary(String profileSummary) { this.profileSummary = profileSummary; }

    public Map<String, Double> getDynamicWeights() { return dynamicWeights; }
    public void setDynamicWeights(Map<String, Double> dynamicWeights) { this.dynamicWeights = dynamicWeights; }
}
