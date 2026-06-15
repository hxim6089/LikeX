package com.example.rec.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知实体类
 * 记录用户之间的互动行为 (点赞, 评论, 关注)
 */
@Entity
@Table(name = "tb_notification")
@Data
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId; // 接收通知的用户ID

    @Column(name = "actor_id", nullable = false)
    private Long actorId; // 触发行为的用户ID (是谁点赞/评论了你)

    @Column(name = "type", nullable = false)
    private String type; // 通知类型: LIKE (点赞), COMMENT (评论), FOLLOW (关注)

    @Column(name = "entity_id")
    private Long entityId; // 关联实体的ID (Content ID 或 User ID)

    @Column(name = "is_read")
    private boolean isRead = false; // 是否已读

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // 非持久化字段，用于前端展示详细信息
    @Transient
    private User actor; // 触发者详情
    @Transient
    private Content content; // 关联的推文详情 (仅 LIKE/COMMENT 类型需要)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getRecipientId() { return recipientId; }
    public void setRecipientId(Long recipientId) { this.recipientId = recipientId; }

    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getActor() { return actor; }
    public void setActor(User actor) { this.actor = actor; }

    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }
}
