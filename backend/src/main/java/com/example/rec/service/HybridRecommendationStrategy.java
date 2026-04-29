package com.example.rec.service;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.dto.ScoreBreakdown;
import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.service.UserBehaviorProfileService.BehaviorProfile;
import com.example.rec.service.UserBehaviorProfileService.DynamicWeights;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于用户行为画像的个性化混合推荐策略
 *
 * 核心改进（相比固定权重版本）：
 * 1. 每个用户拥有独立的动态权重，由 UserBehaviorProfileService 根据行为自动计算
 * 2. 新增打分因子：话题亲和度、作者亲密度、内容深度匹配、新鲜度偏好
 * 3. 每条推荐附带可解释的推荐理由
 * 4. 冷启动 / 初级 / 活跃用户采用不同策略
 */
@Component
public class HybridRecommendationStrategy implements RecommendationStrategy {

    private final PersonaService personaService;
    private final TrendingService trendingService;
    private final TfIdfService tfIdfService;
    private final UserBehaviorProfileService behaviorProfileService;
    private final CollaborativeFilteringService collaborativeFilteringService;
    private final BehaviorRepository behaviorRepository;

    private static final double WEIGHT_VIEW = 0.05;
    private static final double WEIGHT_IN_NETWORK_BOOST = 1.5;
    private static final double AUTHOR_DIVERSITY_DECAY = 0.7;
    private static final double ENGAGEMENT_RATE_WEIGHT = 0.3;
    private static final int CF_CANDIDATE_LIMIT = 50;
    private static final double DISLIKE_PENALTY_FACTOR = 0.1;
    private static final double LIKE_PENALTY_FACTOR = 0.5;
    private static final double VIEWED_PENALTY_FACTOR = 0.7;
    private static final int VIEW_THRESHOLD_SECONDS = 5;

    public HybridRecommendationStrategy(PersonaService personaService,
                                         TrendingService trendingService,
                                         TfIdfService tfIdfService,
                                         UserBehaviorProfileService behaviorProfileService,
                                         CollaborativeFilteringService collaborativeFilteringService,
                                         BehaviorRepository behaviorRepository) {
        this.personaService = personaService;
        this.trendingService = trendingService;
        this.tfIdfService = tfIdfService;
        this.behaviorProfileService = behaviorProfileService;
        this.collaborativeFilteringService = collaborativeFilteringService;
        this.behaviorRepository = behaviorRepository;
    }

    @Override
    public List<Content> recommend(Long userId, List<Content> candidates) {
        BehaviorProfile profile = userId != null
                ? behaviorProfileService.buildProfile(userId) : new BehaviorProfile();
        DynamicWeights dw = userId != null
                ? behaviorProfileService.computeDynamicWeights(userId) : new DynamicWeights();

        List<String> userInterests = getUserInterests(userId);
        Map<String, Double> userTfIdf = Collections.emptyMap();
        Map<String, Double> globalIdf = Collections.emptyMap();
        if (userId != null) {
            try {
                userTfIdf = tfIdfService.getUserProfileVector(userId);
                if (!userTfIdf.isEmpty()) globalIdf = tfIdfService.buildGlobalIdf();
            } catch (Exception ignored) {}
        }

        final List<String> interests = userInterests;
        final Map<String, Double> tfidfProfile = userTfIdf;
        final Map<String, Double> idf = globalIdf;

        Set<Long> cfRecommendedIds = getCfRecommendedIds(userId);
        Set<Long> dislikedIds = getDislikedContentIds(userId);
        Set<Long> likedIds = getLikedContentIds(userId);
        Set<Long> viewedIds = getViewedContentIds(userId);

        List<ScoredContent> scoredList = candidates.stream()
                .map(c -> {
                    double score = calculateScore(c, interests, tfidfProfile, idf, profile, dw, cfRecommendedIds, dislikedIds);
                    score = applyInteractionPenalty(c.getId(), score, dislikedIds, likedIds, viewedIds);
                    return new ScoredContent(c, score);
                })
                .collect(Collectors.toList());

        applyAuthorDiversityPenalty(scoredList);
        scoredList.sort(Comparator.comparingDouble(ScoredContent::getScore).reversed());
        return weightedShuffle(scoredList, dw.explorationFactor);
    }

    /**
     * 带评分详情的推荐（主页 debug 模式 + 对比页面均使用）
     */
    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates) {
        return recommendWithScore(userId, candidates, null);
    }

    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates, Map<String, Double> manualWeights) {
        BehaviorProfile profile = userId != null
                ? behaviorProfileService.buildProfile(userId) : new BehaviorProfile();
        DynamicWeights dw = userId != null
                ? behaviorProfileService.computeDynamicWeights(userId) : new DynamicWeights();

        if (manualWeights != null && !manualWeights.isEmpty()) {
            applyManualOverrides(dw, manualWeights);
        }

        List<String> userInterests = getUserInterests(userId);
        Map<String, Double> tmpTfIdf = Collections.emptyMap();
        Map<String, Double> tmpIdf = Collections.emptyMap();
        if (userId != null) {
            try {
                tmpTfIdf = tfIdfService.getUserProfileVector(userId);
                if (!tmpTfIdf.isEmpty()) tmpIdf = tfIdfService.buildGlobalIdf();
            } catch (Exception ignored) {}
        }
        final Map<String, Double> userTfIdf = tmpTfIdf;
        final Map<String, Double> globalIdf = tmpIdf;

        Map<String, Double> weightsMap = dynamicWeightsToMap(dw);
        Set<Long> cfRecommendedIds = getCfRecommendedIds(userId);
        Set<Long> dislikedIds = getDislikedContentIds(userId);
        Set<Long> likedIds = getLikedContentIds(userId);
        Set<Long> viewedIds = getViewedContentIds(userId);

        List<ScoredContentWithDetails> scoredList = candidates.stream()
                .map(c -> calculateScoreWithDetails(c, userInterests, userTfIdf, globalIdf, profile, dw, weightsMap, cfRecommendedIds, dislikedIds, likedIds, viewedIds))
                .collect(Collectors.toList());

        applyAuthorDiversityPenaltyWithDetails(scoredList);

        Random jitterRandom = new Random();
        double explorationRange = dw.explorationFactor;
        for (ScoredContentWithDetails sc : scoredList) {
            double factor = (1.0 - explorationRange) + (jitterRandom.nextDouble() * explorationRange * 2);
            double jitteredScore = sc.getFinalScore() * factor;
            sc.getBreakdown().setFinalScore(Math.round(jitteredScore * 100.0) / 100.0);
        }

        scoredList.sort(Comparator.comparingDouble(ScoredContentWithDetails::getFinalScore).reversed());

        List<ContentWithScore> result = new ArrayList<>();
        int rank = 1;
        for (ScoredContentWithDetails sc : scoredList) {
            result.add(new ContentWithScore(sc.getContent(), sc.getBreakdown(), rank++));
        }
        return result;
    }

    // ========== 核心打分（简洁版，用于 recommend()） ==========

    private double calculateScore(Content content, List<String> userInterests,
                                   Map<String, Double> userTfIdf, Map<String, Double> idf,
                                   BehaviorProfile profile, DynamicWeights dw,
                                   Set<Long> cfRecommendedIds, Set<Long> dislikedIds) {
        int likeCount = val(content.getLikeCount());
        int commentCount = val(content.getCommentCount());
        int viewCount = val(content.getViewCount());
        int repostCount = val(content.getRepostCount());

        double engagement = likeCount * dw.wLike + commentCount * dw.wReply
                + viewCount * WEIGHT_VIEW + repostCount * dw.wRepost;

        double engagementRate = viewCount > 0
                ? (double)(likeCount + commentCount + repostCount) / viewCount : 0;
        engagement *= (1.0 + engagementRate * ENGAGEMENT_RATE_WEIGHT);

        if ("IN_NETWORK".equals(content.getNetworkSource())) {
            engagement *= WEIGHT_IN_NETWORK_BOOST;
        }

        double topicAffinity = computeTopicAffinity(content, profile) * dw.wTopicAffinity;
        double authorAffinity = computeAuthorAffinity(content, profile) * dw.wAuthorAffinity;

        double personalization = 0;
        if (userInterests != null && !userInterests.isEmpty() && content.getTags() != null) {
            for (var tag : content.getTags()) {
                if (userInterests.contains(tag.getName())) personalization += dw.wTopicAffinity * 0.5;
            }
        }

        double similarity = 0;
        if (userTfIdf != null && !userTfIdf.isEmpty() && idf != null && !idf.isEmpty()) {
            try { similarity = tfIdfService.getContentSimilarityScore(content, userTfIdf, idf) * dw.wSimilarity; }
            catch (Exception ignored) {}
        }

        double trending = 0;
        try { trending = trendingService.countTrendingTagsInContent(content) * dw.wTrending; }
        catch (Exception ignored) {}

        double depthMatch = computeDepthMatch(content, profile) * dw.wDepthMatch;
        double freshness = computeFreshnessMatch(content, profile) * dw.wFreshness;
        double timeDecay = calculateTimeDecay(content);

        double cfBoost = (cfRecommendedIds.contains(content.getId())) ? dw.wCollaborative : 0;

        double base = (engagement / timeDecay) + topicAffinity + authorAffinity
                + personalization + similarity + trending + depthMatch + freshness + cfBoost;
        double jitter = (Math.random() - 0.3) * base * dw.explorationFactor;
        return base + jitter;
    }

    // ========== 核心打分（详细版，用于 recommendWithScore()） ==========

    private ScoredContentWithDetails calculateScoreWithDetails(Content content,
            List<String> userInterests, Map<String, Double> userTfIdf, Map<String, Double> idf,
            BehaviorProfile profile, DynamicWeights dw, Map<String, Double> weightsMap,
            Set<Long> cfRecommendedIds, Set<Long> dislikedIds,
            Set<Long> likedIds, Set<Long> viewedIds) {

        ScoreBreakdown bd = new ScoreBreakdown();
        List<String> reasons = new ArrayList<>();

        int likeCount = val(content.getLikeCount());
        int commentCount = val(content.getCommentCount());
        int viewCount = val(content.getViewCount());
        int repostCount = val(content.getRepostCount());
        bd.setLikeCount(likeCount);
        bd.setCommentCount(commentCount);
        bd.setViewCount(viewCount);
        bd.setRepostCount(repostCount);

        // 基础互动分
        double engagement = likeCount * dw.wLike + commentCount * dw.wReply
                + viewCount * WEIGHT_VIEW + repostCount * dw.wRepost;
        bd.setBaseEngagement(round(engagement));

        double engagementRate = viewCount > 0
                ? (double)(likeCount + commentCount + repostCount) / viewCount : 0;
        bd.setEngagementRate(round3(engagementRate));
        engagement *= (1.0 + engagementRate * ENGAGEMENT_RATE_WEIGHT);

        boolean inNetwork = "IN_NETWORK".equals(content.getNetworkSource());
        bd.setInNetwork(inNetwork);
        if (inNetwork) {
            engagement *= WEIGHT_IN_NETWORK_BOOST;
            reasons.add("来自你关注的人");
        }

        // 话题亲和度
        double topicAffinity = computeTopicAffinity(content, profile) * dw.wTopicAffinity;
        bd.setTopicAffinityBoost(round(topicAffinity));
        if (topicAffinity > 10) {
            List<String> matched = getMatchedTopics(content, profile);
            if (!matched.isEmpty()) reasons.add("你常看: " + String.join("、", matched));
        }

        // 作者亲密度
        double authorAffinity = computeAuthorAffinity(content, profile) * dw.wAuthorAffinity;
        bd.setAuthorAffinityBoost(round(authorAffinity));
        if (authorAffinity > 10 && content.getAuthor() != null) {
            reasons.add("你常互动的作者: " + content.getAuthor().getUsername());
        }

        // 标签匹配
        double personalization = 0;
        StringBuilder matchedTags = new StringBuilder();
        if (userInterests != null && !userInterests.isEmpty() && content.getTags() != null) {
            for (var tag : content.getTags()) {
                if (userInterests.contains(tag.getName())) {
                    personalization += dw.wTopicAffinity * 0.5;
                    if (matchedTags.length() > 0) matchedTags.append(", ");
                    matchedTags.append(tag.getName());
                }
            }
        }
        bd.setPersonalizationBoost(round(personalization));
        bd.setMatchedTags(matchedTags.toString());

        // TF-IDF 内容相似度
        double similarity = 0;
        if (userTfIdf != null && !userTfIdf.isEmpty() && idf != null && !idf.isEmpty()) {
            try { similarity = tfIdfService.getContentSimilarityScore(content, userTfIdf, idf) * dw.wSimilarity; }
            catch (Exception ignored) {}
        }
        bd.setContentSimilarityBoost(round(similarity));
        if (similarity > 15) reasons.add("内容与你的兴趣高度相似");

        // 热门话题
        double trending = 0;
        try { trending = trendingService.countTrendingTagsInContent(content) * dw.wTrending; }
        catch (Exception ignored) {}
        bd.setTrendingBoost(round(trending));
        if (trending > 0) reasons.add("热门话题");

        // 内容深度匹配
        double depthMatch = computeDepthMatch(content, profile) * dw.wDepthMatch;
        bd.setDepthMatchBoost(round(depthMatch));
        if (depthMatch > 15) reasons.add("符合你的阅读偏好");

        // 新鲜度匹配
        double freshnessVal = computeFreshnessMatch(content, profile) * dw.wFreshness;
        bd.setFreshnessBoost(round(freshnessVal));

        // 协同过滤加成
        double cfBoost = cfRecommendedIds.contains(content.getId()) ? dw.wCollaborative : 0;
        bd.setCollaborativeFilteringBoost(round(cfBoost));
        if (cfBoost > 0) reasons.add("相似用户也喜欢");

        // 时间衰减
        long hoursDiff = 1;
        if (content.getCreatedAt() != null) {
            hoursDiff = Math.max(1, Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours());
        }
        double timeDecay = calculateTimeDecay(content);
        bd.setTimeDecayFactor(round3(1.0 / timeDecay));
        bd.setHoursAgo(hoursDiff);

        // 探索因子
        double jitter = Math.random() * 5.0;
        bd.setJitter(round(jitter));

        // 最终评分
        double finalScore = (engagement / timeDecay) + topicAffinity + authorAffinity
                + personalization + similarity + trending + depthMatch + freshnessVal + cfBoost + jitter;

        Long cId = content.getId();
        if (dislikedIds.contains(cId)) {
            finalScore *= DISLIKE_PENALTY_FACTOR;
            reasons.clear();
            reasons.add("你已踩过此内容 (×0.1)");
        } else if (likedIds.contains(cId)) {
            finalScore *= LIKE_PENALTY_FACTOR;
            reasons.add("已赞过，降低优先级 (×0.5)");
        } else if (viewedIds.contains(cId)) {
            finalScore *= VIEWED_PENALTY_FACTOR;
            reasons.add("已浏览过 (×0.7)");
        }

        bd.setFinalScore(round(finalScore));

        // 行为画像信息
        if (reasons.isEmpty()) reasons.add("综合推荐");
        bd.setRecommendReasons(reasons);
        bd.setUserStage(profile.userStage);
        bd.setProfileSummary(profile.profileSummary);
        bd.setDynamicWeights(weightsMap);

        return new ScoredContentWithDetails(content, bd);
    }

    // ========== 新增打分因子计算 ==========

    private double computeTopicAffinity(Content content, BehaviorProfile profile) {
        if (profile.topicPreferences.isEmpty()) return 0;
        double score = 0;
        if (content.getTags() != null) {
            for (var tag : content.getTags()) {
                Double pref = profile.topicPreferences.get(tag.getName());
                if (pref != null) score += pref;
            }
        }
        if (content.getCategory() != null) {
            Double catPref = profile.topicPreferences.get("_cat:" + content.getCategory());
            if (catPref != null) score += catPref;
        }
        double maxPref = profile.topicPreferences.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
        return Math.min(1.0, score / Math.max(maxPref, 1));
    }

    private List<String> getMatchedTopics(Content content, BehaviorProfile profile) {
        List<String> matched = new ArrayList<>();
        if (content.getTags() == null || profile.topicPreferences.isEmpty()) return matched;
        for (var tag : content.getTags()) {
            if (profile.topicPreferences.containsKey(tag.getName())) {
                matched.add(tag.getName());
            }
        }
        return matched.stream().limit(3).collect(Collectors.toList());
    }

    private double computeAuthorAffinity(Content content, BehaviorProfile profile) {
        if (content.getAuthor() == null || profile.authorPreferences.isEmpty()) return 0;
        Double affinity = profile.authorPreferences.get(content.getAuthor().getId());
        if (affinity == null) return 0;
        double maxAffinity = profile.authorPreferences.values().stream()
                .mapToDouble(Double::doubleValue).max().orElse(1);
        return Math.min(1.0, affinity / Math.max(maxAffinity, 1));
    }

    private double computeDepthMatch(Content content, BehaviorProfile profile) {
        if (content.getContent() == null) return 0;
        int len = content.getContent().length();
        switch (profile.depthPreference) {
            case "short":  return len < 60 ? 1.0 : (len < 120 ? 0.5 : 0.1);
            case "long":   return len > 200 ? 1.0 : (len > 100 ? 0.5 : 0.1);
            default:       return len >= 50 && len <= 250 ? 0.8 : 0.4;
        }
    }

    private double computeFreshnessMatch(Content content, BehaviorProfile profile) {
        if (content.getCreatedAt() == null) return 0;
        long hours = Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours();
        double contentFreshness;
        if (hours <= 6) contentFreshness = 1.0;
        else if (hours <= 24) contentFreshness = 0.7;
        else if (hours <= 72) contentFreshness = 0.4;
        else contentFreshness = 0.1;

        double diff = 1.0 - Math.abs(contentFreshness - profile.freshnessPreference);
        return Math.max(0, diff);
    }

    // ========== 时间衰减（沿用分段策略） ==========

    private double calculateTimeDecay(Content content) {
        long hoursDiff = 1;
        if (content.getCreatedAt() != null) {
            hoursDiff = Math.max(1, Duration.between(content.getCreatedAt(), LocalDateTime.now()).toHours());
        }
        if (hoursDiff <= 6)  return 1.0 + hoursDiff * 0.02;
        if (hoursDiff <= 24) return 1.12 + (hoursDiff - 6) * 0.05;
        if (hoursDiff <= 72) return 2.02 + (hoursDiff - 24) * 0.08;
        return 5.86 + Math.log(hoursDiff - 72 + 1) * 2.0;
    }

    // ========== 多样性控制 ==========

    private void applyAuthorDiversityPenalty(List<ScoredContent> scoredList) {
        scoredList.sort(Comparator.comparingDouble(ScoredContent::getScore).reversed());
        Map<Long, Integer> authorCount = new HashMap<>();
        for (ScoredContent sc : scoredList) {
            Long authorId = sc.getContent().getAuthor() != null ? sc.getContent().getAuthor().getId() : 0L;
            int count = authorCount.getOrDefault(authorId, 0);
            if (count > 0) sc.setScore(sc.getScore() * Math.pow(AUTHOR_DIVERSITY_DECAY, count));
            authorCount.put(authorId, count + 1);
        }
    }

    private void applyAuthorDiversityPenaltyWithDetails(List<ScoredContentWithDetails> scoredList) {
        scoredList.sort(Comparator.comparingDouble(ScoredContentWithDetails::getFinalScore).reversed());
        Map<Long, Integer> authorCount = new HashMap<>();
        for (ScoredContentWithDetails sc : scoredList) {
            Long authorId = sc.getContent().getAuthor() != null ? sc.getContent().getAuthor().getId() : 0L;
            int count = authorCount.getOrDefault(authorId, 0);
            if (count > 0) {
                double penalty = Math.pow(AUTHOR_DIVERSITY_DECAY, count);
                sc.getBreakdown().setFinalScore(round(sc.getFinalScore() * penalty));
            }
            authorCount.put(authorId, count + 1);
        }
    }

    private List<Content> weightedShuffle(List<ScoredContent> sortedList, double explorationFactor) {
        if (sortedList.size() <= 5) {
            Collections.shuffle(sortedList);
            return sortedList.stream().map(ScoredContent::getContent).collect(Collectors.toList());
        }

        List<ScoredContent> pool = new ArrayList<>(sortedList);
        List<Content> result = new ArrayList<>();
        Random random = new Random();

        int topPick = random.nextInt(Math.min(5, pool.size()));
        result.add(pool.remove(topPick).getContent());

        while (!pool.isEmpty() && result.size() < 50) {
            double totalWeight = pool.stream().mapToDouble(s -> Math.max(s.getScore(), 1.0)).sum();
            double r = random.nextDouble() * totalWeight;
            double cumulative = 0;
            int selected = 0;
            for (int i = 0; i < pool.size(); i++) {
                cumulative += Math.max(pool.get(i).getScore(), 1.0);
                if (cumulative >= r) { selected = i; break; }
            }
            result.add(pool.remove(selected).getContent());
        }
        return result;
    }

    // ========== 手动权重覆盖（CompareView 调参用） ==========

    private void applyManualOverrides(DynamicWeights dw, Map<String, Double> manual) {
        if (manual.containsKey("wLike")) dw.wLike = manual.get("wLike");
        if (manual.containsKey("wReply")) dw.wReply = manual.get("wReply");
        if (manual.containsKey("wRepost")) dw.wRepost = manual.get("wRepost");
        if (manual.containsKey("wPersonal")) dw.wTopicAffinity = manual.get("wPersonal");
        if (manual.containsKey("wTrending")) dw.wTrending = manual.get("wTrending");
        if (manual.containsKey("wSimilarity")) dw.wSimilarity = manual.get("wSimilarity");
    }

    // ========== 辅助工具 ==========

    private List<String> getUserInterests(Long userId) {
        if (userId == null) return Collections.emptyList();
        try {
            Map<String, Object> persona = personaService.getUserPersona(userId);
            if (persona.containsKey("interestTags")) {
                return (List<String>) persona.get("interestTags");
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private double applyInteractionPenalty(Long contentId, double score,
                                             Set<Long> dislikedIds, Set<Long> likedIds, Set<Long> viewedIds) {
        if (dislikedIds.contains(contentId)) return score * DISLIKE_PENALTY_FACTOR;
        if (likedIds.contains(contentId)) return score * LIKE_PENALTY_FACTOR;
        if (viewedIds.contains(contentId)) return score * VIEWED_PENALTY_FACTOR;
        return score;
    }

    private Set<Long> getLikedContentIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            return behaviorRepository.findByUserIdAndType(userId, "LIKE").stream()
                    .map(Behavior::getContentId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Set<Long> getViewedContentIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            return behaviorRepository.findByUserIdAndType(userId, "VIEW").stream()
                    .filter(b -> b.getDuration() != null && b.getDuration() >= VIEW_THRESHOLD_SECONDS)
                    .map(Behavior::getContentId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Set<Long> getDislikedContentIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            return behaviorRepository.findByUserIdAndType(userId, "DISLIKE").stream()
                    .map(Behavior::getContentId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Set<Long> getCfRecommendedIds(Long userId) {
        if (userId == null) return Collections.emptySet();
        try {
            List<Content> cfRecs = collaborativeFilteringService.getCollaborativeRecommendations(userId, CF_CANDIDATE_LIMIT);
            return cfRecs.stream().map(Content::getId).collect(Collectors.toSet());
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    private Map<String, Double> dynamicWeightsToMap(DynamicWeights dw) {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("wLike", round(dw.wLike));
        m.put("wReply", round(dw.wReply));
        m.put("wRepost", round(dw.wRepost));
        m.put("wTopicAffinity", round(dw.wTopicAffinity));
        m.put("wAuthorAffinity", round(dw.wAuthorAffinity));
        m.put("wTrending", round(dw.wTrending));
        m.put("wSimilarity", round(dw.wSimilarity));
        m.put("wFreshness", round(dw.wFreshness));
        m.put("wDepthMatch", round(dw.wDepthMatch));
        m.put("wCollaborative", round(dw.wCollaborative));
        m.put("explorationFactor", round(dw.explorationFactor));
        return m;
    }

    private static int val(Integer v) { return v != null ? v : 0; }
    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
    private static double round3(double v) { return Math.round(v * 1000.0) / 1000.0; }

    // ========== 内部类 ==========

    private static class ScoredContent {
        private final Content content;
        private double score;
        public ScoredContent(Content content, double score) { this.content = content; this.score = score; }
        public Content getContent() { return content; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }

    private static class ScoredContentWithDetails {
        private final Content content;
        private final ScoreBreakdown breakdown;
        public ScoredContentWithDetails(Content content, ScoreBreakdown breakdown) { this.content = content; this.breakdown = breakdown; }
        public Content getContent() { return content; }
        public ScoreBreakdown getBreakdown() { return breakdown; }
        public double getFinalScore() { return breakdown.getFinalScore(); }
    }
}
