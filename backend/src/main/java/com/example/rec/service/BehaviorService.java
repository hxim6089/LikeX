package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户行为记录服务
 *
 * 【职责】
 * 负责记录用户的各类行为事件（点赞、点踩、浏览、跳过），
 * 这些行为数据是推荐算法的核心输入，用于：
 * 1. 构建用户行为画像（UserBehaviorProfileService）
 * 2. 计算协同过滤相似度（CollaborativeFilteringService）
 * 3. 计算 TF-IDF 用户兴趣向量（TfIdfService）
 * 4. 交互惩罚（已赞/已踩/已浏览的内容降权）
 *
 * 【互斥逻辑】
 * - 点赞和点踩互斥：点赞时自动取消点踩，反之亦然
 * - 浏览去重：同一用户对同一帖子只记录一次浏览，但会更新最长停留时间
 *
 * 【缓存联动】
 * 点赞/点踩操作会触发 AI 推荐缓存失效（invalidateAiCache），
 * 确保下次请求能基于最新行为重新计算推荐。
 */
@Service
public class BehaviorService {

    private static final Set<String> STRONG_INTERACTION_TYPES = Set.of("COMMENT", "REPOST", "QUOTE");

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

    @Transactional
    public void recordInteraction(Long userId, Long contentId, String type) {
        if (userId == null || contentId == null) return;

        String normalizedType = type == null ? "" : type.trim().toUpperCase();
        if (!STRONG_INTERACTION_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("Unsupported interaction type: " + type);
        }

        Behavior behavior = new Behavior();
        behavior.setUserId(userId);
        behavior.setContentId(contentId);
        behavior.setType(normalizedType);
        behaviorRepository.save(behavior);
        strategyManager.invalidateAiCache(userId);
    }
}
