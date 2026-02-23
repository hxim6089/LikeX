package com.example.rec.controller;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.dto.PipelineStats;
import com.example.rec.model.Content;
import com.example.rec.service.ContentService;
import com.example.rec.service.RecommendationService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 推荐效果对比 Controller (Phase 28)
 * 
 * 提供推荐算法 vs 时间序的对比 API，
 * 用于答辩展示推荐算法的效果。
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private final RecommendationService recommendationService;
    private final ContentService contentService;

    public CompareController(RecommendationService recommendationService,
                             ContentService contentService) {
        this.recommendationService = recommendationService;
        this.contentService = contentService;
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

        List<ContentWithScore> result = recommendationService.getRecommendedFeedWithWeights(userId, weights);

        Map<String, Object> response = new HashMap<>();
        response.put("contents", result);
        response.put("weights", weights);
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
}
