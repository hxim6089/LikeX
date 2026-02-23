package com.example.rec.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 负面信号实体
 * 用于记录用户对内容/作者的负面反馈（不感兴趣、屏蔽、静音）
 */
@Entity
@Table(name = "negative_signals")
public class NegativeSignal {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "target_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;
    
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    
    @Column(name = "signal_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SignalType signalType;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum TargetType {
        CONTENT, AUTHOR
    }
    
    public enum SignalType {
        NOT_INTERESTED, BLOCK, MUTE
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    
    public SignalType getSignalType() { return signalType; }
    public void setSignalType(SignalType signalType) { this.signalType = signalType; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
