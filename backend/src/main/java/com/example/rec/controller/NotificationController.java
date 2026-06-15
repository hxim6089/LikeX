package com.example.rec.controller;

import com.example.rec.model.Notification;
import com.example.rec.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 获取用户的通知列表
     * @param userId 必须
     * @param page 页码 (默认0)
     * @param size 每页数量 (默认20)
     */
    @GetMapping
    public Page<Notification> getNotifications(@RequestParam Long userId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return notificationService.getUserNotifications(userId, page, size);
    }

    /**
     * 获取未读通知总数 (用于红点显示)
     */
    @GetMapping("/unread-count")
    public long getUnreadCount(@RequestParam Long userId) {
        return notificationService.getUnreadCount(userId);
    }
    
    /**
     * 标记单条通知为已读
     */
    @PostMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }

    /**
     * 标记所有通知为已读
     */
    @PostMapping("/read-all")
    public void markAllAsRead(@RequestParam Long userId) {
        notificationService.markAllAsRead(userId);
    }
}
