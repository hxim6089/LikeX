package com.example.rec.controller;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.dto.PipelineStats;
import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.service.ContentService;
import com.example.rec.service.RecommendationService;
import com.example.rec.service.UserBehaviorProfileService;
import com.example.rec.service.UserBehaviorProfileService.BehaviorProfile;
import com.example.rec.service.UserBehaviorProfileService.DynamicWeights;
import com.example.rec.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 个性化推荐效果验证 Controller
 *
 * 提供：
 * 1. 推荐 vs 时间序对比（证明算法有效）
 * 2. 多用户行为画像对比（证明千人千面）
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final RecommendationService recommendationService;
    private final ContentService contentService;
    private final UserBehaviorProfileService behaviorProfileService;
    private final UserRepository userRepository;

    public CompareController(RecommendationService recommendationService,
                             ContentService contentService,
                             UserBehaviorProfileService behaviorProfileService,
                             UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.contentService = contentService;
        this.behaviorProfileService = behaviorProfileService;
        this.userRepository = userRepository;
    }

    /**
     * 推荐 vs 时间序对比 API
     * 
     * 返回：
     * - personalized: 个性化推荐排序（带评分详情）
     * - chronological: 时间倒序排列（带评分详情，用于对比）
     * - stats: 两组的统计对比数据
     * - pipelineStats: 推荐管道各阶段数据
     */
    @GetMapping("/feed")
    public Map<String, Object> compareFeed(@RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 1. 获取个性化推荐结果（带评分详情和管道统计）
        Map<String, Object> recResult = recommendationService.getRecommendedFeedWithPipeline(userId);
        List<ContentWithScore> personalized = (List<ContentWithScore>) recResult.get("contents");
        PipelineStats pipelineStats = (PipelineStats) recResult.get("pipelineStats");

        // 2. 获取时间倒序结果（同样计算评分，但按时间排序）
        List<ContentWithScore> chronological = recommendationService.getChronologicalFeedWithScore(userId);

        // 3. 计算统计数据
        Map<String, Object> stats = calculateCompareStats(personalized, chronological, userId);

        result.put("personalized", personalized.stream().limit(20).collect(Collectors.toList()));
        result.put("chronological", chronological.stream().limit(20).collect(Collectors.toList()));
        result.put("stats", stats);
        result.put("pipelineStats", pipelineStats);

        return result;
    }

    /**
     * 带自定义权重的推荐 Feed (用于参数调节面板)
     * 返回完整的对比数据 (personalized + chronological + stats)
     */
    @GetMapping("/tuned")
    public Map<String, Object> tunedFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0.5") double wLike,
            @RequestParam(defaultValue = "1.2") double wReply,
            @RequestParam(defaultValue = "2.0") double wRepost,
            @RequestParam(defaultValue = "100") double wPersonal,
            @RequestParam(defaultValue = "50") double wTrending,
            @RequestParam(defaultValue = "80") double wSimilarity) {

        Map<String, Double> weights = new HashMap<>();
        weights.put("wLike", wLike);
        weights.put("wReply", wReply);
        weights.put("wRepost", wRepost);
        weights.put("wPersonal", wPersonal);
        weights.put("wTrending", wTrending);
        weights.put("wSimilarity", wSimilarity);

        // 使用自定义权重的推荐，并同步计算管道漏斗数据
        Map<String, Object> recResult = recommendationService.getRecommendedFeedWithPipeline(userId, weights);
        List<ContentWithScore> personalized = (List<ContentWithScore>) recResult.get("contents");
        PipelineStats pipelineStats = (PipelineStats) recResult.get("pipelineStats");
        
        // 时间倒序对比组 (同样使用自定义权重计算评分，但按时间排序)
        List<ContentWithScore> chronological = recommendationService.getChronologicalFeedWithScore(userId, weights);

        // 计算统计数据
        Map<String, Object> stats = calculateCompareStats(personalized, chronological, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("personalized", personalized.stream().limit(20).collect(Collectors.toList()));
        response.put("chronological", chronological.stream().limit(20).collect(Collectors.toList()));
        response.put("stats", stats);
        response.put("weights", weights);
        response.put("pipelineStats", pipelineStats);
        return response;
    }

    /**
     * 计算两组 Feed 的统计对比数据
     */
    private Map<String, Object> calculateCompareStats(
            List<ContentWithScore> personalized,
            List<ContentWithScore> chronological,
            Long userId) {
        
        Map<String, Object> stats = new HashMap<>();

        // 限制对比范围为前20条
        List<ContentWithScore> pList = personalized.stream().limit(20).collect(Collectors.toList());
        List<ContentWithScore> cList = chronological.stream().limit(20).collect(Collectors.toList());

        // 平均分对比
        double pAvgScore = pList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getFinalScore())
                .average().orElse(0);
        double cAvgScore = cList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getFinalScore())
                .average().orElse(0);
        stats.put("personalizedAvgScore", Math.round(pAvgScore * 100.0) / 100.0);
        stats.put("chronologicalAvgScore", Math.round(cAvgScore * 100.0) / 100.0);

        // 标签命中率对比
        long pTagHits = pList.stream()
                .filter(c -> c.getScoreBreakdown().getMatchedTags() != null 
                        && !c.getScoreBreakdown().getMatchedTags().isEmpty())
                .count();
        long cTagHits = cList.stream()
                .filter(c -> c.getScoreBreakdown().getMatchedTags() != null 
                        && !c.getScoreBreakdown().getMatchedTags().isEmpty())
                .count();
        stats.put("personalizedTagHitRate", pList.isEmpty() ? 0 : Math.round((double) pTagHits / pList.size() * 100.0) / 100.0);
        stats.put("chronologicalTagHitRate", cList.isEmpty() ? 0 : Math.round((double) cTagHits / cList.size() * 100.0) / 100.0);

        // 平均互动量对比
        double pAvgEngagement = pList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getBaseEngagement())
                .average().orElse(0);
        double cAvgEngagement = cList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getBaseEngagement())
                .average().orElse(0);
        stats.put("personalizedEngagementAvg", Math.round(pAvgEngagement * 100.0) / 100.0);
        stats.put("chronologicalEngagementAvg", Math.round(cAvgEngagement * 100.0) / 100.0);

        // TF-IDF 相似度平均值对比
        double pAvgSimilarity = pList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getContentSimilarityBoost())
                .average().orElse(0);
        double cAvgSimilarity = cList.stream()
                .mapToDouble(c -> c.getScoreBreakdown().getContentSimilarityBoost())
                .average().orElse(0);
        stats.put("personalizedSimilarityAvg", Math.round(pAvgSimilarity * 100.0) / 100.0);
        stats.put("chronologicalSimilarityAvg", Math.round(cAvgSimilarity * 100.0) / 100.0);

        // 个性化提升倍率
        if (cAvgScore > 0) {
            stats.put("improvementRatio", Math.round((pAvgScore / cAvgScore) * 100.0) / 100.0);
        } else {
            stats.put("improvementRatio", 0);
        }

        return stats;
    }

    /**
     * 多用户行为画像对比 API — 核心"千人千面"演示接口
     *
     * 对比两个用户的行为画像、动态权重和 Top 5 推荐结果，
     * 直观展示不同用户获得完全不同的推荐策略和内容排序。
     */
    @GetMapping("/profiles")
    public Map<String, Object> compareProfiles(@RequestParam Long userId,
                                                @RequestParam(required = false) Long compareUserId) {
        Map<String, Object> result = new HashMap<>();

        result.put("currentUser", buildUserProfileData(userId));

        if (compareUserId != null && !compareUserId.equals(userId)) {
            result.put("compareUser", buildUserProfileData(compareUserId));
        }

        List<Map<String, Object>> userList = new ArrayList<>();
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("avatarUrl", u.getAvatarUrl());
            item.put("handle", u.getHandle());
            userList.add(item);
        }
        result.put("users", userList);

        return result;
    }

    private Map<String, Object> buildUserProfileData(Long userId) {
        Map<String, Object> data = new HashMap<>();

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            data.put("error", "User not found");
            return data;
        }
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("handle", user.getHandle());

        BehaviorProfile profile = behaviorProfileService.buildProfile(userId);
        data.put("userStage", profile.userStage);
        data.put("profileSummary", profile.profileSummary);
        data.put("engagementStyle", profile.engagementStyle);
        data.put("depthPreference", profile.depthPreference);
        data.put("freshnessPreference", profile.freshnessPreference);
        data.put("explorationRate", profile.explorationRate);

        List<Map<String, Object>> topTopics = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Double> entry : profile.topicPreferences.entrySet()) {
            if (i++ >= 8) break;
            Map<String, Object> t = new HashMap<>();
            t.put("name", entry.getKey());
            t.put("score", Math.round(entry.getValue() * 100.0) / 100.0);
            topTopics.add(t);
        }
        data.put("topTopics", topTopics);

        DynamicWeights dw = behaviorProfileService.computeDynamicWeights(userId);
        Map<String, Double> weightsMap = new LinkedHashMap<>();
        weightsMap.put("wLike", round(dw.wLike));
        weightsMap.put("wReply", round(dw.wReply));
        weightsMap.put("wRepost", round(dw.wRepost));
        weightsMap.put("wTopicAffinity", round(dw.wTopicAffinity));
        weightsMap.put("wAuthorAffinity", round(dw.wAuthorAffinity));
        weightsMap.put("wTrending", round(dw.wTrending));
        weightsMap.put("wSimilarity", round(dw.wSimilarity));
        weightsMap.put("wFreshness", round(dw.wFreshness));
        weightsMap.put("wDepthMatch", round(dw.wDepthMatch));
        weightsMap.put("explorationFactor", round(dw.explorationFactor));
        data.put("dynamicWeights", weightsMap);

        List<ContentWithScore> topRecs = recommendationService.getRecommendedFeedWithScore(userId);
        data.put("topRecommendations", topRecs.stream().limit(5).collect(Collectors.toList()));

        return data;
    }

    private static double round(double v) { return Math.round(v * 100.0) / 100.0; }
}
