package com.example.rec.dto;

import java.util.List;

/**
 * 推荐评分详情 DTO
 * 用于答辩展示，详细分解评分因子
 */
public class ScoreBreakdown {
    
    private double finalScore;           // 最终评分
    private double baseEngagement;       // 基础互动分
    private double engagementRate;       // 互动率
    private double timeDecayFactor;      // 时间衰减系数
    private double trendingBoost;        // 热门话题加成
    private double personalizationBoost; // 个性化加成
    private double jitter;               // 探索因子
    private boolean isInNetwork;         // 是否关注来源
    private String matchedTags;          // 匹配的兴趣标签
    private long hoursAgo;               // 发布距今小时数
    
    // 详细分项（用于前端展示）
    private int likeCount;
    private int commentCount;
    private int repostCount;
    private int viewCount;
    private double contentSimilarityBoost; // TF-IDF 内容相似度加成
    
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
}
