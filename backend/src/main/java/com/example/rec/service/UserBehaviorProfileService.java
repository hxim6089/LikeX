package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户行为画像服务 —— 基于每个用户的历史行为自动计算个性化推荐策略
 *
 * 核心能力：
 * 1. 分析用户行为模式（话题偏好、作者偏好、互动风格、内容深度、新鲜度偏好、探索度）
 * 2. 为每个用户生成独立的动态推荐权重
 * 3. 根据行为量判定用户阶段（冷启动 / 初级 / 活跃），采用不同策略
 */
@Service
public class UserBehaviorProfileService {

    private final BehaviorRepository behaviorRepository;
    private final ContentRepository contentRepository;

    public UserBehaviorProfileService(BehaviorRepository behaviorRepository,
                                      ContentRepository contentRepository) {
        this.behaviorRepository = behaviorRepository;
        this.contentRepository = contentRepository;
    }

    // ========== 用户阶段判定 ==========

    public enum UserStage { COLD_START, BEGINNER, ACTIVE }

    public UserStage getUserStage(Long userId) {
        long count = behaviorRepository.countByUserId(userId);
        if (count < 10) return UserStage.COLD_START;
        if (count < 50) return UserStage.BEGINNER;
        return UserStage.ACTIVE;
    }

    // ========== 行为画像分析 ==========

    public static class BehaviorProfile {
        public Map<String, Double> topicPreferences = new LinkedHashMap<>();
        public Map<Long, Double> authorPreferences = new LinkedHashMap<>();
        public String engagementStyle = "balanced";   // liker / commenter / silent_reader / balanced
        public String depthPreference = "medium";     // short / medium / long
        public double freshnessPreference = 0.5;      // 0=喜欢老内容, 1=喜欢新内容
        public double explorationRate = 0.5;          // 0=偏好固定话题, 1=喜欢探索
        public String userStage = "COLD_START";
        public String profileSummary = "";
    }

    public BehaviorProfile buildProfile(Long userId) {
        BehaviorProfile profile = new BehaviorProfile();
        if (userId == null) return profile;

        List<Behavior> allBehaviors = behaviorRepository.findByUserId(userId);
        if (allBehaviors == null || allBehaviors.isEmpty()) {
            profile.userStage = UserStage.COLD_START.name();
            profile.profileSummary = "新用户，暂无行为数据";
            return profile;
        }

        UserStage stage = getUserStage(userId);
        profile.userStage = stage.name();

        Set<Long> interactedContentIds = allBehaviors.stream()
                .map(Behavior::getContentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<Content> interactedContents = interactedContentIds.isEmpty()
                ? Collections.emptyList()
                : contentRepository.findAllById(interactedContentIds);
        Map<Long, Content> contentMap = interactedContents.stream()
                .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));

        LocalDateTime now = LocalDateTime.now();

        profile.topicPreferences = computeTopicPreferences(allBehaviors, contentMap, now);
        profile.authorPreferences = computeAuthorPreferences(allBehaviors, contentMap, now);
        profile.engagementStyle = computeEngagementStyle(allBehaviors);
        profile.depthPreference = computeDepthPreference(allBehaviors, contentMap);
        profile.freshnessPreference = computeFreshnessPreference(allBehaviors, contentMap, now);
        profile.explorationRate = computeExplorationRate(profile.topicPreferences);
        profile.profileSummary = generateSummary(profile);

        return profile;
    }

    /**
     * 话题偏好：统计用户互动内容的标签/分类频次，带时间衰减和行为类型加权
     */
    private Map<String, Double> computeTopicPreferences(List<Behavior> behaviors,
                                                         Map<Long, Content> contentMap,
                                                         LocalDateTime now) {
        Map<String, Double> topicScores = new HashMap<>();

        for (Behavior b : behaviors) {
            Content c = contentMap.get(b.getContentId());
            if (c == null) continue;

            double weight = behaviorWeight(b.getType());
            double timeDecay = timeDecayFactor(b.getCreatedAt(), now);
            double score = weight * timeDecay;

            if (c.getTags() != null) {
                for (var tag : c.getTags()) {
                    topicScores.merge(tag.getName(), score, Double::sum);
                }
            }
            if (c.getCategory() != null) {
                topicScores.merge("_cat:" + c.getCategory(), score * 0.5, Double::sum);
            }
        }

        return topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 作者偏好：统计用户与各作者的互动强度
     */
    private Map<Long, Double> computeAuthorPreferences(List<Behavior> behaviors,
                                                        Map<Long, Content> contentMap,
                                                        LocalDateTime now) {
        Map<Long, Double> authorScores = new HashMap<>();

        for (Behavior b : behaviors) {
            Content c = contentMap.get(b.getContentId());
            if (c == null || c.getAuthor() == null) continue;

            double weight = behaviorWeight(b.getType());
            double timeDecay = timeDecayFactor(b.getCreatedAt(), now);
            authorScores.merge(c.getAuthor().getId(), weight * timeDecay, Double::sum);
        }

        return authorScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(20)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * 互动风格：根据行为类型占比判定
     */
    private String computeEngagementStyle(List<Behavior> behaviors) {
        long likes = behaviors.stream().filter(b -> "LIKE".equals(b.getType())).count();
        long comments = behaviors.stream().filter(b -> "COMMENT".equals(b.getType())).count();
        long reposts = behaviors.stream().filter(b -> "REPOST".equals(b.getType())).count();
        long views = behaviors.stream().filter(b -> "VIEW".equals(b.getType())).count();
        long total = likes + comments + reposts + views;
        if (total == 0) return "balanced";

        double commentRatio = (double) comments / total;
        double likeRatio = (double) likes / total;
        double viewRatio = (double) views / total;

        if (commentRatio > 0.3) return "commenter";
        if (likeRatio > 0.5) return "liker";
        if (viewRatio > 0.7) return "silent_reader";
        return "balanced";
    }

    /**
     * 内容深度偏好：根据互动内容的平均长度
     */
    private String computeDepthPreference(List<Behavior> behaviors, Map<Long, Content> contentMap) {
        List<Behavior> positive = behaviors.stream()
                .filter(b -> !"SKIP".equals(b.getType()) && !"VIEW".equals(b.getType()))
                .collect(Collectors.toList());

        if (positive.isEmpty()) {
            positive = behaviors.stream()
                    .filter(b -> "VIEW".equals(b.getType()) && b.getDuration() != null && b.getDuration() > 5)
                    .collect(Collectors.toList());
        }

        OptionalDouble avgLen = positive.stream()
                .map(b -> contentMap.get(b.getContentId()))
                .filter(c -> c != null && c.getContent() != null)
                .mapToInt(c -> c.getContent().length())
                .average();

        if (avgLen.isEmpty()) return "medium";
        double avg = avgLen.getAsDouble();
        if (avg < 60) return "short";
        if (avg < 200) return "medium";
        return "long";
    }

    /**
     * 新鲜度偏好：用户互动内容的平均发布年龄
     */
    private double computeFreshnessPreference(List<Behavior> behaviors,
                                               Map<Long, Content> contentMap,
                                               LocalDateTime now) {
        List<Double> ages = new ArrayList<>();
        for (Behavior b : behaviors) {
            if ("SKIP".equals(b.getType())) continue;
            Content c = contentMap.get(b.getContentId());
            if (c == null || c.getCreatedAt() == null || b.getCreatedAt() == null) continue;
            long hoursAge = Duration.between(c.getCreatedAt(), b.getCreatedAt()).toHours();
            ages.add((double) Math.max(0, hoursAge));
        }
        if (ages.isEmpty()) return 0.5;

        double avgAge = ages.stream().mapToDouble(Double::doubleValue).average().orElse(24);
        // 0-6h→高新鲜度(0.9+), 6-24h→中(0.5-0.9), 24h+→低(<0.5)
        if (avgAge <= 6) return 0.9;
        if (avgAge <= 24) return 0.5 + 0.4 * (1.0 - (avgAge - 6) / 18.0);
        if (avgAge <= 72) return 0.3 + 0.2 * (1.0 - (avgAge - 24) / 48.0);
        return 0.2;
    }

    /**
     * 探索度：基于话题偏好的香农熵归一化
     */
    private double computeExplorationRate(Map<String, Double> topicPreferences) {
        if (topicPreferences.isEmpty()) return 0.5;

        double total = topicPreferences.values().stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) return 0.5;

        double entropy = 0;
        for (double val : topicPreferences.values()) {
            double p = val / total;
            if (p > 0) entropy -= p * Math.log(p);
        }
        double maxEntropy = topicPreferences.size() > 1 ? Math.log(topicPreferences.size()) : 1.0;
        return Math.min(1.0, entropy / maxEntropy);
    }

    // ========== 动态权重生成（核心差异化） ==========

    public static class DynamicWeights {
        public double wLike = 0.5;
        public double wReply = 1.2;
        public double wRepost = 2.0;
        public double wTopicAffinity = 80.0;
        public double wAuthorAffinity = 60.0;
        public double wTrending = 50.0;
        public double wSimilarity = 80.0;
        public double wFreshness = 1.0;
        public double wDepthMatch = 30.0;
        public double explorationFactor = 0.15;
    }

    public DynamicWeights computeDynamicWeights(Long userId) {
        DynamicWeights weights = new DynamicWeights();
        BehaviorProfile profile = buildProfile(userId);

        UserStage stage = UserStage.valueOf(profile.userStage);
        switch (stage) {
            case COLD_START:
                weights.wTrending = 100.0;
                weights.wTopicAffinity = 20.0;
                weights.wAuthorAffinity = 10.0;
                weights.explorationFactor = 0.30;
                weights.wSimilarity = 30.0;
                break;

            case BEGINNER:
                weights.wTrending = 60.0;
                weights.wTopicAffinity = 60.0;
                weights.wAuthorAffinity = 40.0;
                weights.explorationFactor = 0.20;
                weights.wSimilarity = 60.0;
                adjustByEngagementStyle(weights, profile.engagementStyle);
                break;

            case ACTIVE:
                weights.wTrending = 30.0;
                weights.wTopicAffinity = 100.0;
                weights.wAuthorAffinity = 70.0;
                weights.explorationFactor = 0.10 + profile.explorationRate * 0.15;
                weights.wSimilarity = 90.0;
                adjustByEngagementStyle(weights, profile.engagementStyle);
                adjustByDepthPreference(weights, profile.depthPreference);
                adjustByFreshnessPreference(weights, profile.freshnessPreference);
                break;
        }

        return weights;
    }

    private void adjustByEngagementStyle(DynamicWeights w, String style) {
        switch (style) {
            case "commenter":
                w.wReply = 2.5;
                w.wLike = 0.3;
                break;
            case "liker":
                w.wLike = 1.5;
                w.wReply = 0.8;
                break;
            case "silent_reader":
                w.wLike = 0.2;
                w.wReply = 0.5;
                w.wTrending += 20.0;
                w.wSimilarity += 20.0;
                break;
        }
    }

    private void adjustByDepthPreference(DynamicWeights w, String depthPref) {
        switch (depthPref) {
            case "short":
                w.wDepthMatch = 40.0;
                break;
            case "long":
                w.wDepthMatch = 50.0;
                break;
        }
    }

    private void adjustByFreshnessPreference(DynamicWeights w, double freshness) {
        w.wFreshness = 0.5 + freshness * 1.5; // 0.5~2.0 范围
    }

    // ========== 画像摘要生成 ==========

    private String generateSummary(BehaviorProfile profile) {
        List<String> parts = new ArrayList<>();

        if (!profile.topicPreferences.isEmpty()) {
            List<String> topTopics = profile.topicPreferences.keySet().stream()
                    .filter(k -> !k.startsWith("_cat:"))
                    .limit(3)
                    .collect(Collectors.toList());
            if (!topTopics.isEmpty()) {
                parts.add("偏好话题: " + String.join("、", topTopics));
            }
        }

        switch (profile.engagementStyle) {
            case "commenter": parts.add("活跃评论者"); break;
            case "liker": parts.add("点赞达人"); break;
            case "silent_reader": parts.add("深度阅读者"); break;
            default: parts.add("均衡互动型"); break;
        }

        switch (profile.depthPreference) {
            case "short": parts.add("偏好短内容"); break;
            case "long": parts.add("偏好深度长文"); break;
        }

        if (profile.freshnessPreference > 0.7) {
            parts.add("追求时效性");
        } else if (profile.freshnessPreference < 0.3) {
            parts.add("偏好经典内容");
        }

        if (profile.explorationRate > 0.7) {
            parts.add("喜欢探索新话题");
        } else if (profile.explorationRate < 0.3) {
            parts.add("深耕固定领域");
        }

        return String.join(" | ", parts);
    }

    // ========== 工具方法 ==========

    private double behaviorWeight(String type) {
        if (type == null) return 0;
        switch (type) {
            case "LIKE": return 1.0;
            case "COMMENT": return 2.5;
            case "REPOST": return 3.0;
            case "VIEW": return 0.2;
            case "SKIP": return -0.5;
            default: return 0.5;
        }
    }

    private double timeDecayFactor(LocalDateTime behaviorTime, LocalDateTime now) {
        if (behaviorTime == null) return 0.2;
        long days = Math.max(0, Duration.between(behaviorTime, now).toDays());
        if (days <= 7) return 1.0;
        if (days <= 30) return 0.7;
        if (days <= 90) return 0.4;
        return 0.2;
    }
}
