package com.example.rec.service;

import com.example.rec.dto.TrendingTopic;
import com.example.rec.model.Ad;
import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.model.NegativeSignal;
import com.example.rec.model.User;
import com.example.rec.repository.AdRepository;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.NegativeSignalRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private static final List<String> BEHAVIOR_TYPES = List.of(
            "VIEW", "LIKE", "COMMENT", "REPOST", "QUOTE", "DISLIKE", "SKIP", "SEARCH"
    );
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final BehaviorRepository behaviorRepository;
    private final NegativeSignalRepository negativeSignalRepository;
    private final AdRepository adRepository;
    private final TrendingService trendingService;
    private final UserBehaviorProfileService userBehaviorProfileService;

    public AnalyticsService(UserRepository userRepository,
                            ContentRepository contentRepository,
                            BehaviorRepository behaviorRepository,
                            NegativeSignalRepository negativeSignalRepository,
                            AdRepository adRepository,
                            TrendingService trendingService,
                            UserBehaviorProfileService userBehaviorProfileService) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
        this.negativeSignalRepository = negativeSignalRepository;
        this.adRepository = adRepository;
        this.trendingService = trendingService;
        this.userBehaviorProfileService = userBehaviorProfileService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard() {
        return getDashboard("all");
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboard(String range) {
        TimeWindow window = resolveTimeWindow(range);
        List<User> users = userRepository.findAll();
        List<Content> contents = contentRepository.findAll();
        List<Behavior> behaviors = behaviorRepository.findAll();
        List<NegativeSignal> negativeSignals = negativeSignalRepository.findAll();
        List<Ad> ads = adRepository.findAll();

        List<User> periodUsers = filterByCreatedAt(users, window.since(), User::getCreatedAt);
        List<Content> periodContents = filterByCreatedAt(contents, window.since(), Content::getCreatedAt);
        List<Behavior> periodBehaviors = filterByCreatedAt(behaviors, window.since(), Behavior::getCreatedAt);
        List<NegativeSignal> periodNegativeSignals = filterByCreatedAt(negativeSignals, window.since(), NegativeSignal::getCreatedAt);
        List<Ad> periodAds = filterByCreatedAt(ads, window.since(), Ad::getCreatedAt);

        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("range", buildRangeMeta(window));
        dashboard.put("overview", buildOverview(users, periodUsers, periodContents, periodBehaviors, periodAds));
        dashboard.put("behaviorStats", buildBehaviorStats(periodBehaviors, window.trendDays()));
        dashboard.put("userStats", buildUserStats(users, periodBehaviors, periodContents));
        dashboard.put("contentStats", buildContentStats(periodContents, window.trendDays(), window.trendingHours()));
        dashboard.put("sourceStats", buildSourceStats(periodContents));
        dashboard.put("feedbackStats", buildFeedbackStats(periodNegativeSignals, periodBehaviors));
        dashboard.put("adSummary", buildAdSummary(periodAds));
        dashboard.put("generatedAt", LocalDateTime.now().toString());
        return dashboard;
    }

    private Map<String, Object> buildOverview(List<User> users, List<User> periodUsers,
                                              List<Content> contents,
                                              List<Behavior> behaviors, List<Ad> ads) {
        LocalDate today = LocalDate.now();
        List<Content> topLevelPosts = contents.stream()
                .filter(c -> c.getParentContent() == null)
                .toList();

        long todayNewPosts = contents.stream()
                .filter(c -> c.getParentContent() == null)
                .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(today))
                .count();
        long todayBehaviors = behaviors.stream()
                .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().equals(today))
                .count();

        long totalViews = sumContentInt(contents, "view");
        long totalLikes = sumContentInt(contents, "like");
        long totalComments = sumContentInt(contents, "comment");
        long totalDislikes = sumContentInt(contents, "dislike");
        long totalReposts = sumContentInt(contents, "repost");
        long activeAds = ads.stream().filter(ad -> Boolean.TRUE.equals(ad.getActive())).count();
        long activeUsers = behaviors.stream()
                .map(Behavior::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("totalUsers", users.size());
        overview.put("periodNewUsers", periodUsers.size());
        overview.put("activeUsers", activeUsers);
        overview.put("totalPosts", topLevelPosts.size());
        overview.put("totalContents", contents.size());
        overview.put("totalBehaviors", behaviors.size());
        overview.put("todayNewPosts", todayNewPosts);
        overview.put("todayBehaviors", todayBehaviors);
        overview.put("totalViews", totalViews);
        overview.put("totalLikes", totalLikes);
        overview.put("totalComments", totalComments);
        overview.put("totalDislikes", totalDislikes);
        overview.put("totalReposts", totalReposts);
        overview.put("activeAds", activeAds);
        return overview;
    }

    private Map<String, Object> buildBehaviorStats(List<Behavior> behaviors, int trendDays) {
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (String type : BEHAVIOR_TYPES) {
            typeCounts.put(type, 0L);
        }
        for (Behavior b : behaviors) {
            if (b.getType() == null) continue;
            String type = b.getType().toUpperCase();
            typeCounts.merge(type, 1L, Long::sum);
        }

        List<Map<String, Object>> typeDistribution = typeCounts.entrySet().stream()
                .map(e -> metric(e.getKey(), behaviorLabel(e.getKey()), e.getValue()))
                .toList();

        int days = Math.max(1, trendDays);
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(DAY_FMT));
            long total = 0;
            for (String type : BEHAVIOR_TYPES) {
                long count = behaviors.stream()
                        .filter(b -> type.equalsIgnoreCase(b.getType()))
                        .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().toLocalDate().equals(day))
                        .count();
                item.put(type, count);
                total += count;
            }
            item.put("total", total);
            dailyTrend.add(item);
        }

        List<Map<String, Object>> hourlyDistribution = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            int h = hour;
            long count = behaviors.stream()
                    .filter(b -> b.getCreatedAt() != null && b.getCreatedAt().getHour() == h)
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("hour", hour);
            item.put("count", count);
            hourlyDistribution.add(item);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("typeDistribution", typeDistribution);
        stats.put("dailyTrend", dailyTrend);
        stats.put("hourlyDistribution", hourlyDistribution);
        return stats;
    }

    private Map<String, Object> buildUserStats(List<User> users, List<Behavior> behaviors, List<Content> contents) {
        Map<String, Long> stageCounts = new LinkedHashMap<>();
        stageCounts.put("冷启动用户", 0L);
        stageCounts.put("初级用户", 0L);
        stageCounts.put("活跃用户", 0L);

        Map<Long, List<Behavior>> behaviorsByUser = behaviors.stream()
                .filter(b -> b.getUserId() != null)
                .collect(Collectors.groupingBy(Behavior::getUserId));

        for (User user : users) {
            UserBehaviorProfileService.UserStage stage = userBehaviorProfileService.getUserStage(user.getId());
            String label = switch (stage) {
                case COLD_START -> "冷启动用户";
                case BEGINNER -> "初级用户";
                case ACTIVE -> "活跃用户";
            };
            stageCounts.merge(label, 1L, Long::sum);
        }

        Map<String, Long> typeCounts = new LinkedHashMap<>();
        typeCounts.put("Creator", 0L);
        typeCounts.put("Interactor", 0L);
        typeCounts.put("Consumer", 0L);

        for (User user : users) {
            List<Behavior> userBehaviors = behaviorsByUser.getOrDefault(user.getId(), List.of());
            long postCount = contents.stream()
                    .filter(c -> c.getAuthor() != null && Objects.equals(c.getAuthor().getId(), user.getId()))
                    .filter(c -> c.getParentContent() == null)
                    .count();
            long comments = countType(userBehaviors, "COMMENT");
            long reposts = countType(userBehaviors, "REPOST") + countType(userBehaviors, "QUOTE");
            long likes = countType(userBehaviors, "LIKE");
            long views = countType(userBehaviors, "VIEW");

            String type = classifyUserType(postCount, comments, reposts, likes, views);
            typeCounts.merge(type, 1L, Long::sum);
        }

        List<Map<String, Object>> topInterestTags = buildTopInterestTags(behaviors, contents);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("stageDistribution", toNameValueList(stageCounts));
        stats.put("typeDistribution", toNameValueList(typeCounts));
        stats.put("topInterestTags", topInterestTags);
        return stats;
    }

    private Map<String, Object> buildContentStats(List<Content> contents, int trendDays, int trendingHours) {
        List<Content> topLevelPosts = contents.stream()
                .filter(c -> c.getParentContent() == null)
                .toList();

        Map<String, Long> categories = topLevelPosts.stream()
                .collect(Collectors.groupingBy(
                        c -> normalizeBlank(c.getCategory(), "未分类"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        int days = Math.max(1, trendDays);
        LocalDate start = LocalDate.now().minusDays(days - 1L);
        List<Map<String, Object>> dailyPostTrend = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = start.plusDays(i);
            long count = topLevelPosts.stream()
                    .filter(c -> c.getCreatedAt() != null && c.getCreatedAt().toLocalDate().equals(day))
                    .count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(DAY_FMT));
            item.put("count", count);
            dailyPostTrend.add(item);
        }

        long withImage = topLevelPosts.stream()
                .filter(c -> c.getImageUrl() != null && !c.getImageUrl().isBlank())
                .count();
        double imageRatio = topLevelPosts.isEmpty() ? 0 : round((double) withImage / topLevelPosts.size() * 100);

        List<Map<String, Object>> topPosts = topLevelPosts.stream()
                .sorted(Comparator.comparingLong(this::engagementScore).reversed())
                .limit(10)
                .map(this::toTopPost)
                .toList();

        List<TrendingTopic> trending = trendingService.getTrendingTopics(10, trendingHours);
        List<Map<String, Object>> trendingTopics = trending.stream()
                .map(t -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", t.getName());
                    item.put("postCount", t.getPostCount());
                    item.put("engagement", t.getEngagement());
                    item.put("score", round(t.getScore()));
                    return item;
                })
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("categoryDistribution", toNameValueList(categories));
        stats.put("dailyPostTrend", dailyPostTrend);
        stats.put("topPosts", topPosts);
        stats.put("imagePostRatio", imageRatio);
        stats.put("trendingTopics", trendingTopics);
        return stats;
    }

    private Map<String, Object> buildSourceStats(List<Content> contents) {
        List<Content> topLevelPosts = contents.stream()
                .filter(c -> c.getParentContent() == null)
                .toList();

        long externalImported = topLevelPosts.stream().filter(this::isExternalImportedContent).count();
        long localPublished = Math.max(0, topLevelPosts.size() - externalImported);
        long withImage = topLevelPosts.stream()
                .filter(c -> c.getImageUrl() != null && !c.getImageUrl().isBlank())
                .count();
        long withTags = topLevelPosts.stream()
                .filter(c -> c.getTags() != null && !c.getTags().isEmpty())
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPosts", topLevelPosts.size());
        stats.put("localPublished", localPublished);
        stats.put("externalImported", externalImported);
        stats.put("withImage", withImage);
        stats.put("withTags", withTags);
        stats.put("imageRatio", percent(withImage, topLevelPosts.size()));
        stats.put("taggedRatio", percent(withTags, topLevelPosts.size()));
        stats.put("sourceDistribution", List.of(
                metric("LOCAL", "用户/系统发布", localPublished),
                metric("EXTERNAL", "外部导入线索", externalImported),
                metric("IMAGE", "含图片内容", withImage),
                metric("TAGGED", "已打标签内容", withTags)
        ));
        return stats;
    }

    private Map<String, Object> buildFeedbackStats(List<NegativeSignal> negativeSignals, List<Behavior> behaviors) {
        Map<String, Long> signalCounts = new LinkedHashMap<>();
        signalCounts.put("NOT_INTERESTED", 0L);
        signalCounts.put("BLOCK", 0L);
        signalCounts.put("MUTE", 0L);
        for (NegativeSignal signal : negativeSignals) {
            if (signal.getSignalType() != null) {
                signalCounts.merge(signal.getSignalType().name(), 1L, Long::sum);
            }
        }
        long dislikes = behaviors.stream().filter(b -> "DISLIKE".equalsIgnoreCase(b.getType())).count();

        List<Map<String, Object>> breakdown = new ArrayList<>();
        breakdown.add(metric("NOT_INTERESTED", "不感兴趣", signalCounts.getOrDefault("NOT_INTERESTED", 0L)));
        breakdown.add(metric("BLOCK", "屏蔽作者", signalCounts.getOrDefault("BLOCK", 0L)));
        breakdown.add(metric("MUTE", "静音作者", signalCounts.getOrDefault("MUTE", 0L)));
        breakdown.add(metric("DISLIKE", "点踩", dislikes));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalNegativeSignals", negativeSignals.size());
        stats.put("totalDislikes", dislikes);
        stats.put("breakdown", breakdown);
        return stats;
    }

    private Map<String, Object> buildAdSummary(List<Ad> ads) {
        long impressions = ads.stream().mapToLong(ad -> safeInt(ad.getImpressionCount())).sum();
        long clicks = ads.stream().mapToLong(ad -> safeInt(ad.getClickCount())).sum();
        double ctr = impressions == 0 ? 0 : round((double) clicks / impressions * 100);
        double estimatedRevenue = ads.stream()
                .mapToDouble(ad -> safeInt(ad.getImpressionCount()) * safeDouble(ad.getBidPrice()) / 1000.0)
                .sum();

        Map<String, List<Ad>> byCategory = ads.stream()
                .collect(Collectors.groupingBy(ad -> normalizeBlank(ad.getCategory(), "未分类")));
        List<Map<String, Object>> categoryCtr = byCategory.entrySet().stream()
                .map(e -> {
                    long catImp = e.getValue().stream().mapToLong(ad -> safeInt(ad.getImpressionCount())).sum();
                    long catClicks = e.getValue().stream().mapToLong(ad -> safeInt(ad.getClickCount())).sum();
                    double catCtr = catImp == 0 ? 0 : round((double) catClicks / catImp * 100);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("ctr", catCtr);
                    item.put("impressions", catImp);
                    return item;
                })
                .sorted((a, b) -> Double.compare((Double) b.get("ctr"), (Double) a.get("ctr")))
                .toList();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalAds", ads.size());
        summary.put("activeAds", ads.stream().filter(ad -> Boolean.TRUE.equals(ad.getActive())).count());
        summary.put("totalImpressions", impressions);
        summary.put("totalClicks", clicks);
        summary.put("overallCtr", ctr);
        summary.put("estimatedRevenue", round(estimatedRevenue));
        summary.put("categoryCtr", categoryCtr);
        return summary;
    }

    private List<Map<String, Object>> buildTopInterestTags(List<Behavior> behaviors, List<Content> contents) {
        Map<Long, Content> contentMap = contents.stream()
                .filter(c -> c.getId() != null)
                .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));
        Map<String, Double> scores = new HashMap<>();

        for (Behavior b : behaviors) {
            double weight = behaviorInterestWeight(b.getType());
            if (weight <= 0 || b.getContentId() == null) continue;
            Content content = contentMap.get(b.getContentId());
            if (content == null) continue;

            if (content.getTags() != null && !content.getTags().isEmpty()) {
                content.getTags().forEach(tag -> scores.merge(tag.getName(), weight, Double::sum));
            } else if (content.getCategory() != null) {
                scores.merge(content.getCategory(), weight * 0.5, Double::sum);
            }
        }

        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", round(e.getValue()));
                    return item;
                })
                .toList();
    }

    private Map<String, Object> toTopPost(Content c) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", c.getId());
        item.put("author", c.getAuthor() != null ? c.getAuthor().getUsername() : "Unknown");
        item.put("content", c.getContent());
        item.put("category", c.getCategory());
        item.put("viewCount", safeInt(c.getViewCount()));
        item.put("likeCount", safeInt(c.getLikeCount()));
        item.put("commentCount", safeInt(c.getCommentCount()));
        item.put("repostCount", safeInt(c.getRepostCount()));
        item.put("engagementScore", engagementScore(c));
        return item;
    }

    private List<Map<String, Object>> toNameValueList(Map<String, ? extends Number> source) {
        return source.entrySet().stream()
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", e.getKey());
                    item.put("value", e.getValue());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> metric(String type, String label, Number count) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("label", label);
        item.put("count", count);
        item.put("name", label);
        item.put("value", count);
        return item;
    }

    private long sumContentInt(List<Content> contents, String field) {
        return contents.stream().mapToLong(c -> switch (field) {
            case "view" -> safeInt(c.getViewCount());
            case "like" -> safeInt(c.getLikeCount());
            case "comment" -> safeInt(c.getCommentCount());
            case "dislike" -> safeInt(c.getDislikeCount());
            case "repost" -> safeInt(c.getRepostCount());
            default -> 0;
        }).sum();
    }

    private long engagementScore(Content c) {
        return safeInt(c.getLikeCount()) * 2L
                + safeInt(c.getCommentCount()) * 3L
                + safeInt(c.getRepostCount()) * 4L
                + safeInt(c.getViewCount());
    }

    private String classifyUserType(long postCount, long comments, long reposts, long likes, long views) {
        double creatorScore = postCount * 3.0;
        double interactorScore = comments * 2.0 + reposts * 2.5;
        double consumerScore = likes + views * 0.2;
        if (creatorScore >= interactorScore && creatorScore >= consumerScore) return "Creator";
        if (interactorScore >= consumerScore) return "Interactor";
        return "Consumer";
    }

    private long countType(List<Behavior> behaviors, String type) {
        return behaviors.stream().filter(b -> type.equalsIgnoreCase(b.getType())).count();
    }

    private double behaviorInterestWeight(String type) {
        if (type == null) return 0;
        return switch (type.toUpperCase()) {
            case "LIKE" -> 1.0;
            case "COMMENT" -> 2.0;
            case "REPOST", "QUOTE" -> 3.0;
            case "VIEW" -> 0.2;
            default -> 0;
        };
    }

    private String behaviorLabel(String type) {
        return switch (type) {
            case "VIEW" -> "浏览";
            case "LIKE" -> "点赞";
            case "COMMENT" -> "评论";
            case "REPOST" -> "转发";
            case "QUOTE" -> "引用";
            case "DISLIKE" -> "点踩";
            case "SKIP" -> "跳过";
            case "SEARCH" -> "搜索";
            default -> type;
        };
    }

    private String normalizeBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double safeDouble(Double value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double percent(long part, long total) {
        return total == 0 ? 0 : round((double) part / total * 100);
    }

    private boolean isExternalImportedContent(Content content) {
        if (content == null) return false;
        User author = content.getAuthor();
        String username = author != null && author.getUsername() != null
                ? author.getUsername().toLowerCase()
                : "";
        String bio = author != null && author.getBio() != null
                ? author.getBio().toLowerCase()
                : "";
        String title = content.getTitle() != null ? content.getTitle().toLowerCase() : "";
        String imageUrl = content.getImageUrl() != null ? content.getImageUrl().toLowerCase() : "";

        return username.startsWith("x_")
                || bio.contains("imported from x")
                || title.startsWith("tweet from ")
                || imageUrl.contains("pbs.twimg.com")
                || imageUrl.contains("amplify_video_thumb");
    }

    private <T> List<T> filterByCreatedAt(List<T> source, LocalDateTime since, Function<T, LocalDateTime> getter) {
        if (since == null) return source;
        return source.stream()
                .filter(item -> {
                    LocalDateTime createdAt = getter.apply(item);
                    return createdAt != null && !createdAt.isBefore(since);
                })
                .toList();
    }

    private Map<String, Object> buildRangeMeta(TimeWindow window) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("key", window.key());
        meta.put("label", window.label());
        meta.put("since", window.since() == null ? null : window.since().toString());
        meta.put("trendDays", window.trendDays());
        meta.put("trendingHours", window.trendingHours());
        return meta;
    }

    private TimeWindow resolveTimeWindow(String range) {
        String normalized = range == null ? "all" : range.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        return switch (normalized) {
            case "today" -> new TimeWindow("today", "今日", now.toLocalDate().atStartOfDay(), 1, 24);
            case "7d", "week" -> new TimeWindow("7d", "最近 7 天", now.minusDays(6).toLocalDate().atStartOfDay(), 7, 24 * 7);
            case "30d", "month" -> new TimeWindow("30d", "最近 30 天", now.minusDays(29).toLocalDate().atStartOfDay(), 30, 24 * 30);
            default -> new TimeWindow("all", "全部", null, 30, 24 * 30);
        };
    }

    private record TimeWindow(String key, String label, LocalDateTime since, int trendDays, int trendingHours) {}
}
