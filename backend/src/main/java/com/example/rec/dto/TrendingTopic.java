package com.example.rec.dto;

/**
 * 热门话题 DTO
 * 用于返回热门话题数据
 */
public class TrendingTopic {
    private String name;        // 话题名称 (不带#)
    private Long postCount;     // 相关帖子数量
    private Long engagement;    // 总互动量
    private Double score;       // 综合热度分数

    public TrendingTopic() {}

    public TrendingTopic(String name, Long postCount, Long engagement) {
        this.name = name;
        this.postCount = postCount;
        this.engagement = engagement;
        this.score = calculateScore();
    }

    private Double calculateScore() {
        // 热度公式: 帖子数×1 + 互动量×0.5
        return (postCount != null ? postCount : 0) + 
               (engagement != null ? engagement * 0.5 : 0);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getPostCount() { return postCount; }
    public void setPostCount(Long postCount) { this.postCount = postCount; }

    public Long getEngagement() { return engagement; }
    public void setEngagement(Long engagement) { this.engagement = engagement; }

    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}
