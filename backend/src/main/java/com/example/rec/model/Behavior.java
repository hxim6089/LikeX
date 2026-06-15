package com.example.rec.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户行为实体
 *
 * 记录用户对内容的每一次交互行为，是推荐算法的核心数据源。
 * 行为数据用于：用户画像构建、协同过滤、TF-IDF 兴趣建模、交互惩罚判定。
 *
 * 行为类型（type）说明：
 * - VIEW: 浏览（duration 记录停留秒数，>5秒视为深度浏览）
 * - LIKE: 点赞（正向信号，权重 1.0）
 * - DISLIKE: 点踩（负向信号，触发内容降权）
 * - COMMENT: 评论（强正向信号，权重 2.5）
 * - REPOST: 转发（最强正向信号，权重 3.0）
 * - SKIP: 快速滑过（弱负向信号）
 * - SEARCH: 搜索行为（用于兴趣发现）
 */
@Data
@Entity
@Table(name = "behaviors")
public class Behavior {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;         // 产生行为的用户

    @Column(name = "content_id")
    private Long contentId;      // 行为关联的帖子 ID

    private String type;         // 行为类型：VIEW/LIKE/DISLIKE/COMMENT/REPOST/SKIP/SEARCH

    private Integer duration;    // 浏览停留时长（秒），仅 VIEW 类型有效，用于判断深度阅读

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
}
