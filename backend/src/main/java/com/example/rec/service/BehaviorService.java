package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BehaviorService {

    private final BehaviorRepository behaviorRepository;
    private final ContentRepository contentRepository;
    private final NotificationService notificationService;
    private final RecommendationStrategyManager strategyManager;

    public BehaviorService(BehaviorRepository behaviorRepository,
                           ContentRepository contentRepository,
                           NotificationService notificationService,
                           @Lazy RecommendationStrategyManager strategyManager) {
        this.behaviorRepository = behaviorRepository;
        this.contentRepository = contentRepository;
        this.notificationService = notificationService;
        this.strategyManager = strategyManager;
    }

    @Transactional
    public void likeContent(Long userId, Long contentId) {
        List<Behavior> existingLike = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "LIKE");
        if (!existingLike.isEmpty()) return;

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        List<Behavior> existingDislike = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "DISLIKE");
        if (!existingDislike.isEmpty()) {
            behaviorRepository.deleteAll(existingDislike);
            content.setDislikeCount(Math.max(0, (content.getDislikeCount() != null ? content.getDislikeCount() : 0) - 1));
        }

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("LIKE");
        behaviorRepository.save(behavior);

        content.setLikeCount(content.getLikeCount() + 1);
        contentRepository.save(content);

        notificationService.createNotification(content.getAuthor().getId(), userId, "LIKE", contentId);
        strategyManager.invalidateAiCache(userId);
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
    public void dislikeContent(Long userId, Long contentId) {
        List<Behavior> existingDislike = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "DISLIKE");
        if (!existingDislike.isEmpty()) return;

        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Content not found"));

        List<Behavior> existingLike = behaviorRepository.findByUserIdAndContentIdAndType(userId, contentId, "LIKE");
        if (!existingLike.isEmpty()) {
            behaviorRepository.deleteAll(existingLike);
            content.setLikeCount(Math.max(0, content.getLikeCount() - 1));
        }

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType("DISLIKE");
        behaviorRepository.save(behavior);

        content.setDislikeCount((content.getDislikeCount() != null ? content.getDislikeCount() : 0) + 1);
        contentRepository.save(content);
        strategyManager.invalidateAiCache(userId);
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
