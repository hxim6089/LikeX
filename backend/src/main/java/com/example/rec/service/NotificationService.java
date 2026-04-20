package com.example.rec.service;

import com.example.rec.model.Notification;
import com.example.rec.model.User;
import com.example.rec.model.Content;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.NotificationRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository, ContentRepository contentRepository, SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 创建并保存一条通知，同时通过 WebSocket 实时推送
     */
    public void createNotification(Long recipientId, Long actorId, String type, Long entityId) {
        if (recipientId.equals(actorId)) {
            return; // 不给自己发送通知
        }
        
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setType(type);
        notification.setEntityId(entityId);
        
        Notification saved = notificationRepository.save(notification);
        
        // 实时推送通知
        User actor = userRepository.findById(actorId).orElse(null);
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("id", saved.getId());
        payload.put("type", type);
        payload.put("actorId", actorId);
        payload.put("actorName", actor != null ? actor.getUsername() : "Unknown");
        payload.put("createdAt", saved.getCreatedAt().toString());
        if (entityId != null) {
            payload.put("entityId", entityId);
        }
        messagingTemplate.convertAndSendToUser(
            recipientId.toString(),
            "/queue/notifications",
            payload
        );
    }

    /**
     * 获取用户的通知列表 (分页, 按时间倒序)
     * 同时填充 Actor 和 Content 的详细信息，方便前端展示
     */
    public Page<Notification> getUserNotifications(Long userId, int page, int size) {
        Page<Notification> notifs = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        
        // 填充非持久化字段 (Actor, Content)
        notifs.forEach(n -> {
            User actor = userRepository.findById(n.getActorId()).orElse(null);
            n.setActor(actor);
            
            if ("LIKE".equals(n.getType()) || "COMMENT".equals(n.getType())) {
                Content content = contentRepository.findById(n.getEntityId()).orElse(null);
                n.setContent(content);
            }
        });
        
        return notifs;
    }
    
    /**
     * 获取用户的未读通知数量
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }
    
    /**
     * 标记某条通知为已读
     */
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }
    
    public void markAllAsRead(Long userId) {
        List<Notification> unreads = notificationRepository.findByRecipientIdAndIsReadFalse(userId);
        for (Notification n : unreads) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unreads);
    }
}
