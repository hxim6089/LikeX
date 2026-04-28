package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        boolean alreadyLiked = behaviorRepository.findByUserIdAndType(userId, "LIKE").stream()
                .anyMatch(b -> b.getContentId().equals(contentId));
        
        if (alreadyLiked) {
            return; 
        }

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("LIKE");
        behaviorRepository.save(behavior);

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));
        content.setLikeCount(content.getLikeCount() + 1);
        contentRepository.save(content);
        
        notificationService.createNotification(content.getAuthor().getId(), userId, "LIKE", contentId);
    }

    @Transactional
    public void recordView(Long userId, Long contentId, Integer duration) {
        if (userId == null || contentId == null) return;

        List<Behavior> existing = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "VIEW");
        if (!existing.isEmpty()) {
            Behavior last = existing.get(existing.size() - 1);
            if (duration != null && (last.getDuration() == null || duration > last.getDuration())) {
                last.setDuration(duration);
                behaviorRepository.save(last);
            }
            return;
        }

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("VIEW");
        behavior.setDuration(duration);
        behaviorRepository.save(behavior);

        contentRepository.findById(contentId).ifPresent(content -> {
            content.setViewCount((content.getViewCount() != null ? content.getViewCount() : 0) + 1);
            contentRepository.save(content);
        });
    }

    @Transactional
    public void recordSkip(Long userId, Long contentId) {
        if (userId == null || contentId == null) return;

        List<Behavior> existing = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "SKIP");
        if (!existing.isEmpty()) return;

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("SKIP");
        behaviorRepository.save(behavior);
    }
}
