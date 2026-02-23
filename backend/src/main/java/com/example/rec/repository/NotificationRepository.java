package com.example.rec.repository;

import com.example.rec.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    // Find notifications for a user, ordered by time
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);
    
    // Count unread notifications
    long countByRecipientIdAndIsReadFalse(Long recipientId);
}
