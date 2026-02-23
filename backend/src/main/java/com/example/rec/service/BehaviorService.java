package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BehaviorService {

    private final BehaviorRepository behaviorRepository;
    private final ContentRepository contentRepository;
    private final NotificationService notificationService;

    public BehaviorService(BehaviorRepository behaviorRepository, ContentRepository contentRepository, NotificationService notificationService) {
        this.behaviorRepository = behaviorRepository;
        this.contentRepository = contentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void likeContent(Long userId, Long contentId) {
        // 0. Check if already liked to prevent duplicates
        // Note: Ideally we should "Unlike" here, but for now we just prevent duplicates as requested.
        boolean alreadyLiked = behaviorRepository.findByUserIdAndType(userId, "LIKE").stream()
                .anyMatch(b -> b.getContentId().equals(contentId));
        
        if (alreadyLiked) {
            return; 
        }

        // 1. Save Behavior Record
        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("LIKE");
        behaviorRepository.save(behavior);

        // 2. Increment Like Count in Content
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        content.setLikeCount(content.getLikeCount() + 1);
        contentRepository.save(content);
        
        // 3. Trigger Notification
        notificationService.createNotification(content.getAuthor().getId(), userId, "LIKE", contentId);
    }
}
