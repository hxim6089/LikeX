package com.example.rec.service;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.dto.ScoreBreakdown;
import com.example.rec.model.Content;
import com.example.rec.service.UserBehaviorProfileService.BehaviorProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 驱动的推荐策略
 *
 * 使用本地 Ollama（Qwen 8B）大语言模型，根据用户行为画像
 * 对候选帖子进行智能排序并生成个性化推荐理由。
 *
 * 与 HybridRecommendationStrategy 形成两套独立算法，
 * 通过 Admin 管理面板全局切换。
 */
@Component
public class AiRecommendationStrategy implements RecommendationStrategy {

    private final UserBehaviorProfileService behaviorProfileService;
    private final RestTemplate restTemplate;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.rec.model:qwen3:8b}")
    private String recModel;

    @Value("${ollama.rec.temperature:0.3}")
    private double temperature;

    @Value("${ollama.rec.num-predict:500}")
    private int numPredict;

    private static final int MAX_CANDIDATES_FOR_AI = 25;

    public AiRecommendationStrategy(UserBehaviorProfileService behaviorProfileService,
                                     RestTemplate restTemplate) {
        this.behaviorProfileService = behaviorProfileService;
        this.restTemplate = restTemplate;
    }

    @Override
    public List<Content> recommend(Long userId, List<Content> candidates) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        BehaviorProfile profile = userId != null
                ? behaviorProfileService.buildProfile(userId) : new BehaviorProfile();

        List<Content> topCandidates = presortByEngagement(candidates, MAX_CANDIDATES_FOR_AI);

        Map<Long, String> aiResult = callAiRanking(profile, topCandidates);
        if (aiResult == null) {
            return fallbackSort(candidates);
        }

        return applyAiRanking(candidates, topCandidates, aiResult);
    }

    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();

        BehaviorProfile profile = userId != null
                ? behaviorProfileService.buildProfile(userId) : new BehaviorProfile();

        List<Content> topCandidates = presortByEngagement(candidates, MAX_CANDIDATES_FOR_AI);

        AiRankingResult aiResult = callAiRankingFull(profile, topCandidates);
        if (aiResult == null) {
            return buildFallbackScored(candidates, profile);
        }

        return buildScoredResult(candidates, topCandidates, aiResult, profile);
    }

    // ========== 预排序：按基础互动分取 Top N ==========

    private List<Content> presortByEngagement(List<Content> candidates, int limit) {
        return candidates.stream()
                .sorted((a, b) -> {
                    double scoreA = basicEngagement(a);
                    double scoreB = basicEngagement(b);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double basicEngagement(Content c) {
        int likes = c.getLikeCount() != null ? c.getLikeCount() : 0;
        int comments = c.getCommentCount() != null ? c.getCommentCount() : 0;
        int reposts = c.getRepostCount() != null ? c.getRepostCount() : 0;
        int views = c.getViewCount() != null ? c.getViewCount() : 0;
        return likes * 2.0 + comments * 3.0 + reposts * 4.0 + views * 0.1;
    }

    // ========== Prompt 构造 ==========

    private String buildPrompt(BehaviorProfile profile, List<Content> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是社交媒体推荐系统的 AI 推荐引擎。根据用户画像，从候选帖子中选出最适合该用户的内容并排序。\n\n");

        sb.append("用户画像：\n");
        sb.append("- 用户阶段：").append(stageLabel(profile.userStage)).append("\n");
        if (!profile.topicPreferences.isEmpty()) {
            sb.append("- 兴趣话题：");
            profile.topicPreferences.entrySet().stream().limit(5).forEach(e ->
                    sb.append(e.getKey()).append("(").append(String.format("%.1f", e.getValue())).append(") "));
            sb.append("\n");
        }
        sb.append("- 互动风格：").append(styleLabel(profile.engagementStyle)).append("\n");
        sb.append("- 内容深度偏好：").append(depthLabel(profile.depthPreference)).append("\n");
        sb.append("- 新鲜度偏好：").append(profile.freshnessPreference > 0.6 ? "偏好新内容" : profile.freshnessPreference < 0.4 ? "偏好经典内容" : "均衡").append("\n");
        if (!profile.profileSummary.isEmpty()) {
            sb.append("- 画像摘要：").append(profile.profileSummary).append("\n");
        }

        sb.append("\n候选帖子：\n");
        for (int i = 0; i < candidates.size(); i++) {
            Content c = candidates.get(i);
            sb.append(i + 1).append(". [id=").append(c.getId()).append("] ");
            sb.append("\"").append(truncate(c.getContent(), 80)).append("\"");
            if (c.getTags() != null && !c.getTags().isEmpty()) {
                sb.append(" | 标签: ").append(
                        c.getTags().stream().map(t -> t.getName()).limit(3).collect(Collectors.joining(", ")));
            }
            sb.append(" | ").append(val(c.getLikeCount())).append("赞 ")
                    .append(val(c.getCommentCount())).append("评论 ")
                    .append(val(c.getRepostCount())).append("转发");
            sb.append("\n");
        }

        sb.append("\n请严格按以下 JSON 格式输出（不要输出任何其他内容）：\n");
        sb.append("{\"ranking\":[id1,id2,...],\"reasons\":{\"id1\":\"推荐理由\",\"id2\":\"推荐理由\",...}}\n");
        sb.append("ranking 中的 id 必须是上面候选帖子的真实 id 数字，按你认为最适合该用户的顺序排列。\n");
        sb.append("reasons 中每个 id 对应一句简短的中文推荐理由（15字以内）。\n");
        return sb.toString();
    }

    // ========== Ollama 调用 ==========

    private Map<Long, String> callAiRanking(BehaviorProfile profile, List<Content> candidates) {
        AiRankingResult result = callAiRankingFull(profile, candidates);
        return result != null ? result.reasons : null;
    }

    @SuppressWarnings("unchecked")
    private AiRankingResult callAiRankingFull(BehaviorProfile profile, List<Content> candidates) {
        try {
            String prompt = buildPrompt(profile, candidates);
            String url = ollamaUrl + "/api/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", recModel);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("think", false);

            Map<String, Object> options = new HashMap<>();
            options.put("temperature", temperature);
            options.put("num_predict", numPredict);
            body.put("options", options);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("response")) {
                String aiText = ((String) response.get("response")).trim();
                return parseAiResponse(aiText, candidates);
            }
        } catch (Exception e) {
            System.err.println("[AI Rec] Ollama call failed: " + e.getMessage());
        }
        return null;
    }

    // ========== 响应解析 ==========

    @SuppressWarnings("unchecked")
    private AiRankingResult parseAiResponse(String text, List<Content> candidates) {
        try {
            text = text.replaceAll("<think>.*?</think>", "").trim();
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}');
            if (jsonStart < 0 || jsonEnd < 0) return null;
            text = text.substring(jsonStart, jsonEnd + 1);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> json = mapper.readValue(text, Map.class);

            Set<Long> validIds = candidates.stream().map(Content::getId).collect(Collectors.toSet());

            List<Long> ranking = new ArrayList<>();
            Object rankingObj = json.get("ranking");
            if (rankingObj instanceof List) {
                for (Object item : (List<?>) rankingObj) {
                    Long id = item instanceof Number ? ((Number) item).longValue() : null;
                    if (id != null && validIds.contains(id)) {
                        ranking.add(id);
                    }
                }
            }

            Map<Long, String> reasons = new LinkedHashMap<>();
            Object reasonsObj = json.get("reasons");
            if (reasonsObj instanceof Map) {
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) reasonsObj).entrySet()) {
                    try {
                        Long id = Long.parseLong(entry.getKey());
                        if (validIds.contains(id)) {
                            reasons.put(id, String.valueOf(entry.getValue()));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            if (ranking.isEmpty()) return null;

            AiRankingResult result = new AiRankingResult();
            result.ranking = ranking;
            result.reasons = reasons;
            return result;
        } catch (Exception e) {
            System.err.println("[AI Rec] Failed to parse AI response: " + e.getMessage());
            return null;
        }
    }

    // ========== 排序合并 ==========

    private List<Content> applyAiRanking(List<Content> allCandidates, List<Content> topCandidates,
                                          Map<Long, String> aiReasons) {
        Map<Long, Content> candidateMap = allCandidates.stream()
                .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));

        List<Content> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();

        for (Long id : aiReasons.keySet()) {
            Content c = candidateMap.get(id);
            if (c != null && !added.contains(id)) {
                result.add(c);
                added.add(id);
            }
        }

        for (Content c : topCandidates) {
            if (!added.contains(c.getId())) {
                result.add(c);
                added.add(c.getId());
            }
        }

        for (Content c : allCandidates) {
            if (!added.contains(c.getId())) {
                result.add(c);
                added.add(c.getId());
            }
        }

        return result;
    }

    private List<ContentWithScore> buildScoredResult(List<Content> allCandidates,
                                                      List<Content> topCandidates,
                                                      AiRankingResult aiResult,
                                                      BehaviorProfile profile) {
        Map<Long, Content> candidateMap = allCandidates.stream()
                .collect(Collectors.toMap(Content::getId, c -> c, (a, b) -> a));

        List<ContentWithScore> result = new ArrayList<>();
        Set<Long> added = new HashSet<>();
        int rank = 1;
        int totalRanked = aiResult.ranking.size();

        for (Long id : aiResult.ranking) {
            Content c = candidateMap.get(id);
            if (c != null && !added.contains(id)) {
                ScoreBreakdown bd = buildAiScoreBreakdown(c, profile,
                        aiResult.reasons.getOrDefault(id, "AI 推荐"),
                        rank, totalRanked);
                result.add(new ContentWithScore(c, bd, rank++));
                added.add(id);
            }
        }

        for (Content c : topCandidates) {
            if (!added.contains(c.getId())) {
                ScoreBreakdown bd = buildAiScoreBreakdown(c, profile, "候选补充", rank, totalRanked);
                result.add(new ContentWithScore(c, bd, rank++));
                added.add(c.getId());
            }
        }

        for (Content c : allCandidates) {
            if (!added.contains(c.getId())) {
                ScoreBreakdown bd = buildAiScoreBreakdown(c, profile, "常规补充", rank, totalRanked);
                result.add(new ContentWithScore(c, bd, rank++));
                added.add(c.getId());
            }
        }

        return result;
    }

    private ScoreBreakdown buildAiScoreBreakdown(Content c, BehaviorProfile profile,
                                                  String reason, int rank, int total) {
        ScoreBreakdown bd = new ScoreBreakdown();
        double aiScore = total > 0 ? Math.max(0, 100.0 * (1.0 - (double)(rank - 1) / total)) : 50.0;
        bd.setFinalScore(Math.round(aiScore * 100.0) / 100.0);
        bd.setBaseEngagement(basicEngagement(c));
        bd.setLikeCount(val(c.getLikeCount()));
        bd.setCommentCount(val(c.getCommentCount()));
        bd.setRepostCount(val(c.getRepostCount()));
        bd.setViewCount(val(c.getViewCount()));
        bd.setInNetwork("IN_NETWORK".equals(c.getNetworkSource()));
        bd.setUserStage(profile.userStage);
        bd.setProfileSummary(profile.profileSummary);

        List<String> reasons = new ArrayList<>();
        reasons.add(reason);
        bd.setRecommendReasons(reasons);

        if (c.getTags() != null && !c.getTags().isEmpty()) {
            bd.setMatchedTags(c.getTags().stream()
                    .map(t -> t.getName()).limit(3)
                    .collect(Collectors.joining(", ")));
        }
        return bd;
    }

    // ========== 降级排序 ==========

    private List<Content> fallbackSort(List<Content> candidates) {
        return candidates.stream()
                .sorted((a, b) -> Double.compare(basicEngagement(b), basicEngagement(a)))
                .collect(Collectors.toList());
    }

    private List<ContentWithScore> buildFallbackScored(List<Content> candidates, BehaviorProfile profile) {
        List<Content> sorted = fallbackSort(candidates);
        List<ContentWithScore> result = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            Content c = sorted.get(i);
            ScoreBreakdown bd = buildAiScoreBreakdown(c, profile, "AI 降级模式 - 按互动量排序", i + 1, sorted.size());
            result.add(new ContentWithScore(c, bd, i + 1));
        }
        return result;
    }

    // ========== Ollama 健康检测 ==========

    public boolean isOllamaAvailable() {
        try {
            String url = ollamaUrl + "/api/tags";
            restTemplate.getForObject(url, Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ========== 辅助方法 ==========

    private int val(Integer v) { return v != null ? v : 0; }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    private String stageLabel(String stage) {
        return switch (stage) {
            case "ACTIVE" -> "活跃用户";
            case "BEGINNER" -> "初级用户";
            default -> "新用户（冷启动）";
        };
    }

    private String styleLabel(String style) {
        return switch (style) {
            case "liker" -> "点赞达人";
            case "commenter" -> "评论活跃者";
            case "silent_reader" -> "安静的阅读者";
            default -> "均衡互动";
        };
    }

    private String depthLabel(String depth) {
        return switch (depth) {
            case "short" -> "快餐式短内容";
            case "long" -> "深度长文";
            default -> "中等篇幅";
        };
    }

    // ========== 内部数据类 ==========

    private static class AiRankingResult {
        List<Long> ranking = new ArrayList<>();
        Map<Long, String> reasons = new LinkedHashMap<>();
    }
}
