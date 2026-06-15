package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PersonaService {
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final BehaviorRepository behaviorRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final com.example.rec.repository.FollowRepository followRepository;

    public PersonaService(BehaviorRepository behaviorRepository, ContentRepository contentRepository, UserRepository userRepository, com.example.rec.repository.FollowRepository followRepository) {
        this.behaviorRepository = behaviorRepository;
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    /**
     * 用户画像 (Phase 29 精细化版本)
     * 
     * 维度：
     * 1. 兴趣衰减评分 (interestDecay)
     * 2. 用户行为分型 (userType)
     * 3. 活跃度等级 (activityLevel)
     * 4. 活跃时段 (peakHours, nightOwlIndex)
     * 5. 内容偏好 (contentPreference)
     */
    @Cacheable(value = "persona", key = "#userId")
    public Map<String, Object> getUserPersona(Long userId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // ========== 基础信息 ==========
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                result.put("error", "User not found");
                return result;
            }
            
            result.put("id", user.getId());
            result.put("username", user.getUsername() != null ? user.getUsername() : "");
            result.put("handle", user.getHandle() != null ? user.getHandle() : "@user" + user.getId());
            result.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            result.put("bio", user.getBio() != null ? user.getBio() : "");
            result.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");

            // 关注/粉丝计数
            result.put("followingCount", followRepository.countByFollowerId(userId));
            result.put("followerCount", followRepository.countByFolloweeId(userId));

            // ========== 行为数据 ==========
            List<Behavior> allBehaviors = behaviorRepository.findByUserId(userId);
            if (allBehaviors == null) allBehaviors = new ArrayList<>();
            
            List<Behavior> likes = allBehaviors.stream()
                    .filter(b -> "LIKE".equals(b.getType()))
                    .collect(Collectors.toList());
            
            List<Long> likedContentIds = likes.stream().map(Behavior::getContentId).collect(Collectors.toList());
            List<Content> likedContents = likedContentIds.isEmpty() ? new ArrayList<>() : contentRepository.findAllById(likedContentIds);

            // ========== 原有画像数据 ==========
            
            // 分类分布
            Map<String, Long> categoryCount = likedContents.stream()
                    .filter(c -> c != null && c.getCategory() != null)
                    .collect(Collectors.groupingBy(Content::getCategory, Collectors.counting()));

            List<Map.Entry<String, Long>> topCategories = categoryCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .collect(Collectors.toList());

            result.put("interestTags", topCategories.stream().map(Map.Entry::getKey).collect(Collectors.toList()));
            result.put("totalLikes", likes.size());
            
            // 称号
            String title = "Newcomer";
            if (!topCategories.isEmpty()) {
                String top = topCategories.get(0).getKey();
                if ("Tech".equals(top)) title = "Tech Enthusiast (科技迷)";
                else if ("Life".equals(top)) title = "Life Observer (生活家)";
                else if ("Sports".equals(top)) title = "Sports Fan (运动狂)";
                else if ("News".equals(top)) title = "News Reader (读报人)";
            }
            result.put("personaTitle", title);

            // 行为统计 (雷达图)
            Map<String, Integer> behaviorStats = new HashMap<>();
            behaviorStats.put("likes", (int) allBehaviors.stream().filter(b -> "LIKE".equals(b.getType())).count());
            behaviorStats.put("comments", (int) allBehaviors.stream().filter(b -> "COMMENT".equals(b.getType())).count());
            behaviorStats.put("reposts", (int) allBehaviors.stream().filter(b -> "REPOST".equals(b.getType())).count());
            behaviorStats.put("views", (int) allBehaviors.stream().filter(b -> "VIEW".equals(b.getType())).count());
            result.put("behaviorStats", behaviorStats);
            
            // 分类分布详情
            long totalCategoryCount = categoryCount.values().stream().mapToLong(Long::longValue).sum();
            List<Map<String, Object>> categoryDistribution = new ArrayList<>();
            for (Map.Entry<String, Long> entry : categoryCount.entrySet()) {
                Map<String, Object> item = new HashMap<>();
                item.put("category", entry.getKey());
                item.put("count", entry.getValue());
                item.put("percentage", totalCategoryCount > 0 ? 
                        Math.round(entry.getValue() * 100.0 / totalCategoryCount) : 0);
                categoryDistribution.add(item);
            }
            categoryDistribution.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
            result.put("categoryDistribution", categoryDistribution);
            
            // 词云数据
            Map<String, Long> tagCount = new HashMap<>();
            for (Content c : likedContents) {
                if (c != null && c.getTags() != null) {
                    for (var tag : c.getTags()) {
                        tagCount.merge(tag.getName(), 1L, Long::sum);
                    }
                }
            }
            List<Map<String, Object>> wordCloudData = tagCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(20)
                    .map(entry -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("name", entry.getKey());
                        item.put("value", entry.getValue());
                        return item;
                    })
                    .collect(Collectors.toList());
            result.put("wordCloudData", wordCloudData);

            // 推荐匹配度
            int totalInteractions = behaviorStats.values().stream().mapToInt(Integer::intValue).sum();
            double matchRate = Math.min(1.0, totalInteractions / 100.0);
            result.put("recommendationMatchRate", Math.round(matchRate * 100));

            // ======================================================
            // Phase 29: 精细化画像维度
            // ======================================================

            LocalDateTime now = LocalDateTime.now();

            // ----- 5a. 兴趣衰减评分 -----
            result.put("interestDecay", computeInterestDecay(allBehaviors, likedContents, now));

            // ----- 5b. 用户行为分型 -----
            result.put("userTypeDetail", computeUserType(userId, allBehaviors));

            // ----- 5c. 活跃度等级 -----
            Map<String, Object> activity = computeActivityLevel(allBehaviors, now);
            result.put("activityLevel", activity.get("level"));
            result.put("activityScore", activity.get("score"));

            // ----- 5d. 活跃时段分析 -----
            Map<String, Object> timeAnalysis = computeTimeAnalysis(allBehaviors);
            result.put("peakHours", timeAnalysis.get("peakHours"));
            result.put("nightOwlIndex", timeAnalysis.get("nightOwlIndex"));
            result.put("hourlyDistribution", timeAnalysis.get("hourlyDistribution"));

            // ----- 5e. 内容偏好分析 -----
            result.put("contentPreference", computeContentPreference(likedContents));

            // ----- 5f. 近期兴趣序列 -----
            result.put("recentInterestSequence", computeRecentInterestSequence(allBehaviors, now));

        } catch (Exception e) {
            e.printStackTrace();
            result.put("error", e.getMessage());
        }

        return result;
    }

    // ============================================================
    // Phase 29: 精细化画像计算方法
    // ============================================================

    /**
     * 5a. 兴趣衰减评分
     * 对每个标签计算带时间衰减的加权分，并判断趋势
     */
    private List<Map<String, Object>> computeInterestDecay(List<Behavior> allBehaviors, List<Content> likedContents, LocalDateTime now) {
        // 为每个帖子建 ID→Content 索引
        Map<Long, Content> contentMap = likedContents.stream()
                .filter(c -> c != null)
                .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));

        // 按标签聚合带权重的分数
        Map<String, Double> tagScoreRecent = new HashMap<>();  // 最近30天
        Map<String, Double> tagScorePrevious = new HashMap<>(); // 30-60天
        Map<String, Double> tagScoreTotal = new HashMap<>();

        for (Behavior b : allBehaviors) {
            Content c = contentMap.get(b.getContentId());
            if (c == null || c.getTags() == null) continue;

            double behaviorWeight = getBehaviorWeight(b.getType());
            long daysSince = b.getCreatedAt() != null ? 
                    Math.max(0, Duration.between(b.getCreatedAt(), now).toDays()) : 999;
            
            // 时间衰减系数
            double timeWeight;
            if (daysSince <= 30) timeWeight = 1.0;
            else if (daysSince <= 90) timeWeight = 0.5;
            else timeWeight = 0.2;

            double score = behaviorWeight * timeWeight;

            for (var tag : c.getTags()) {
                String tagName = tag.getName();
                tagScoreTotal.merge(tagName, score, Double::sum);
                
                if (daysSince <= 30) {
                    tagScoreRecent.merge(tagName, score, Double::sum);
                } else if (daysSince <= 60) {
                    tagScorePrevious.merge(tagName, score, Double::sum);
                }
            }
        }

        // 归一化 + 趋势判断
        double maxScore = tagScoreTotal.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        List<Map<String, Object>> interestDecay = new ArrayList<>();
        for (Map.Entry<String, Double> entry : tagScoreTotal.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            String tagName = entry.getKey();
            double normalizedScore = Math.min(1.0, entry.getValue() / maxScore);
            item.put("tag", tagName);
            item.put("score", Math.round(normalizedScore * 100.0) / 100.0);

            // 趋势：近30天 vs 30-60天
            double recent = tagScoreRecent.getOrDefault(tagName, 0.0);
            double previous = tagScorePrevious.getOrDefault(tagName, 0.0);
            if (previous > 0 && recent / previous > 1.2) {
                item.put("trend", "rising");
            } else if (previous > 0 && recent / previous < 0.8) {
                item.put("trend", "falling");
            } else {
                item.put("trend", "stable");
            }

            interestDecay.add(item);
        }

        // 降序排列
        interestDecay.sort((a, b) -> Double.compare((Double) b.get("score"), (Double) a.get("score")));
        return interestDecay.stream().limit(10).collect(Collectors.toList());
    }

    /**
     * 5b. 用户行为分型
     * Creator / Interactor / Consumer 三角模型
     */
    private Map<String, Object> computeUserType(Long userId, List<Behavior> allBehaviors) {
        Map<String, Object> typeDetail = new HashMap<>();

        // 统计发帖数
        long postCount = contentRepository.countByAuthorId(userId);
        long commentCount = allBehaviors.stream().filter(b -> "COMMENT".equals(b.getType())).count();
        long repostCount = allBehaviors.stream().filter(b -> "REPOST".equals(b.getType())).count();
        long likeCount = allBehaviors.stream().filter(b -> "LIKE".equals(b.getType())).count();
        long viewCount = allBehaviors.stream().filter(b -> "VIEW".equals(b.getType())).count();

        double total = postCount + commentCount + repostCount + likeCount + viewCount;
        if (total == 0) total = 1;

        double creatorScore = Math.min(1.0, postCount / Math.max(total * 0.3, 1));
        double interactorScore = Math.min(1.0, (commentCount + repostCount) / Math.max(total * 0.4, 1));
        double consumerScore = Math.min(1.0, (likeCount + viewCount) / Math.max(total * 0.6, 1));

        // 确定主类型
        String type;
        if (creatorScore >= interactorScore && creatorScore >= consumerScore) {
            type = "Creator";
        } else if (interactorScore >= consumerScore) {
            type = "Interactor";
        } else {
            type = "Consumer";
        }

        typeDetail.put("type", type);
        typeDetail.put("creatorScore", Math.round(creatorScore * 100.0) / 100.0);
        typeDetail.put("interactorScore", Math.round(interactorScore * 100.0) / 100.0);
        typeDetail.put("consumerScore", Math.round(consumerScore * 100.0) / 100.0);

        result_putUserType(typeDetail, type);
        return typeDetail;
    }

    private void result_putUserType(Map<String, Object> detail, String type) {
        String emoji;
        switch (type) {
            case "Creator": emoji = "🎨 Content Creator"; break;
            case "Interactor": emoji = "💬 Social Butterfly"; break;
            default: emoji = "📖 Content Consumer"; break;
        }
        detail.put("label", emoji);
    }

    /**
     * 5c. 活跃度等级
     */
    private Map<String, Object> computeActivityLevel(List<Behavior> allBehaviors, LocalDateTime now) {
        Map<String, Object> activity = new HashMap<>();
        
        int totalBehaviors = allBehaviors.size();
        long recentBehaviors = allBehaviors.stream()
                .filter(b -> b.getCreatedAt() != null && 
                        Duration.between(b.getCreatedAt(), now).toDays() <= 7)
                .count();

        int activityScore = (int) Math.min(100, totalBehaviors * 0.5 + recentBehaviors * 3);

        String level;
        if (activityScore >= 80) level = "Power User";
        else if (activityScore >= 50) level = "High";
        else if (activityScore >= 25) level = "Medium";
        else level = "Low";

        activity.put("score", activityScore);
        activity.put("level", level);
        return activity;
    }

    /**
     * 5d. 活跃时段分析
     */
    private Map<String, Object> computeTimeAnalysis(List<Behavior> allBehaviors) {
        Map<String, Object> result = new HashMap<>();

        // 按小时统计
        int[] hourlyCount = new int[24];
        int nightCount = 0;
        int totalWithTime = 0;

        for (Behavior b : allBehaviors) {
            if (b.getCreatedAt() != null) {
                int hour = b.getCreatedAt().getHour();
                hourlyCount[hour]++;
                totalWithTime++;
                // 22:00-06:00 算夜间
                if (hour >= 22 || hour < 6) {
                    nightCount++;
                }
            }
        }

        // Top 3 高峰小时
        List<Integer> peakHours = new ArrayList<>();
        int[] sorted = hourlyCount.clone();
        Arrays.sort(sorted);
        int threshold = sorted.length > 3 ? sorted[sorted.length - 3] : 0;
        for (int h = 0; h < 24; h++) {
            if (hourlyCount[h] >= threshold && hourlyCount[h] > 0 && peakHours.size() < 3) {
                peakHours.add(h);
            }
        }

        // 小时分布数据（给前端热力图用）
        List<Map<String, Object>> hourlyDistribution = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            Map<String, Object> item = new HashMap<>();
            item.put("hour", h);
            item.put("count", hourlyCount[h]);
            hourlyDistribution.add(item);
        }

        result.put("peakHours", peakHours);
        result.put("nightOwlIndex", totalWithTime > 0 ? 
                Math.round((double) nightCount / totalWithTime * 100.0) / 100.0 : 0.0);
        result.put("hourlyDistribution", hourlyDistribution);
        return result;
    }

    /**
     * 5e. 内容偏好分析
     */
    private Map<String, Object> computeContentPreference(List<Content> likedContents) {
        Map<String, Object> pref = new HashMap<>();

        if (likedContents.isEmpty()) {
            pref.put("avgReadLength", "unknown");
            pref.put("imagePreference", 0.0);
            pref.put("topicDiversity", 0.0);
            return pref;
        }

        // 平均阅读长度
        double avgLength = likedContents.stream()
                .filter(c -> c.getContent() != null)
                .mapToInt(c -> c.getContent().length())
                .average()
                .orElse(0);
        
        if (avgLength < 50) pref.put("avgReadLength", "short");
        else if (avgLength < 200) pref.put("avgReadLength", "medium");
        else pref.put("avgReadLength", "long");

        // 图片偏好
        long withImage = likedContents.stream()
                .filter(c -> c.getImageUrl() != null && !c.getImageUrl().isEmpty())
                .count();
        pref.put("imagePreference", Math.round((double) withImage / likedContents.size() * 100.0) / 100.0);

        // 话题多样性 (香农熵)
        Map<String, Integer> tagFreq = new HashMap<>();
        int totalTags = 0;
        for (Content c : likedContents) {
            if (c.getTags() != null) {
                for (var tag : c.getTags()) {
                    tagFreq.merge(tag.getName(), 1, Integer::sum);
                    totalTags++;
                }
            }
        }
        
        double entropy = 0;
        if (totalTags > 0) {
            for (int count : tagFreq.values()) {
                double p = (double) count / totalTags;
                if (p > 0) entropy -= p * Math.log(p);
            }
            double maxEntropy = tagFreq.size() > 1 ? Math.log(tagFreq.size()) : 1.0;
            pref.put("topicDiversity", Math.round(entropy / maxEntropy * 100.0) / 100.0);
        } else {
            pref.put("topicDiversity", 0.0);
        }

        return pref;
    }

    /**
     * 5f. 近期兴趣序列
     * 按天聚合最近 14 天的互动内容标签，用于观察用户兴趣是否发生迁移。
     */
    private Map<String, Object> computeRecentInterestSequence(List<Behavior> allBehaviors, LocalDateTime now) {
        Map<String, Object> result = new LinkedHashMap<>();
        int days = 14;
        LocalDate start = now.toLocalDate().minusDays(days - 1L);

        List<Long> contentIds = allBehaviors.stream()
                .map(Behavior::getContentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Content> contentMap = contentIds.isEmpty()
                ? Collections.emptyMap()
                : contentRepository.findAllById(contentIds).stream()
                        .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));

        Map<String, double[]> tagSeries = new HashMap<>();
        List<Map<String, Object>> timeline = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            Map<String, Double> dayScores = new HashMap<>();
            long behaviorCount = 0;

            for (Behavior behavior : allBehaviors) {
                if (behavior.getCreatedAt() == null || !behavior.getCreatedAt().toLocalDate().equals(day)) continue;
                Content content = contentMap.get(behavior.getContentId());
                if (content == null) continue;

                Set<String> labels = extractInterestLabels(content);
                if (labels.isEmpty()) continue;

                behaviorCount++;
                double score = getBehaviorWeight(behavior.getType());
                for (String label : labels) {
                    dayScores.merge(label, score, Double::sum);
                    tagSeries.computeIfAbsent(label, key -> new double[days])[i] += score;
                }
            }

            String topTag = dayScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("暂无");
            double totalScore = dayScores.values().stream().mapToDouble(Double::doubleValue).sum();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(DAY_FMT));
            item.put("topTag", topTag);
            item.put("behaviorCount", behaviorCount);
            item.put("totalScore", round(totalScore));
            timeline.add(item);
        }

        List<String> labels = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            labels.add(start.plusDays(i).format(DAY_FMT));
        }

        List<Map<String, Object>> series = tagSeries.entrySet().stream()
                .sorted((a, b) -> Double.compare(sum(b.getValue()), sum(a.getValue())))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("data", Arrays.stream(entry.getValue())
                            .map(v -> Math.round(v * 100.0) / 100.0)
                            .boxed()
                            .collect(Collectors.toList()));
                    return item;
                })
                .collect(Collectors.toList());

        result.put("labels", labels);
        result.put("series", series);
        result.put("timeline", timeline);
        return result;
    }

    private Set<String> extractInterestLabels(Content content) {
        Set<String> labels = new LinkedHashSet<>();
        if (content.getTags() != null) {
            content.getTags().stream()
                    .map(tag -> tag.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .forEach(labels::add);
        }
        if (labels.isEmpty() && content.getCategory() != null && !content.getCategory().isBlank()) {
            labels.add(content.getCategory());
        }
        return labels;
    }

    private double sum(double[] values) {
        return Arrays.stream(values).sum();
    }

    /**
     * 行为权重
     */
    private double getBehaviorWeight(String type) {
        if (type == null) return 0;
        switch (type) {
            case "VIEW": return 0.1;
            case "LIKE": return 1.0;
            case "COMMENT": return 2.0;
            case "REPOST": return 3.0;
            default: return 0.5;
        }
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
