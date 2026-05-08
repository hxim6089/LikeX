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
 * AI 大模型驱动的推荐策略
 *
 * 【算法概述】
 * 使用本地部署的 Ollama（Qwen 8B）大语言模型，
 * 根据用户行为画像对候选帖子进行语义理解和智能排序，
 * 并为每条推荐生成自然语言推荐理由。
 *
 * 【与传统算法的区别】
 * - 传统算法（HybridRecommendationStrategy）：基于数值公式打分，可解释性强，速度快
 * - AI 算法（本类）：基于语言模型的语义理解，能捕捉公式难以表达的深层兴趣，但推理较慢
 *
 * 【执行流程】
 * 1. 构建用户行为画像（复用 UserBehaviorProfileService）
 * 2. 对候选帖子按基础互动量预排序，取 Top 25 作为 AI 输入
 * 3. 构造 Prompt：将用户画像 + 候选帖子信息输入给大模型
 * 4. 调用 Ollama /api/generate 接口获取排序结果
 * 5. 解析 AI 返回的 JSON（ranking + reasons）
 * 6. 将 AI 排序结果与剩余候选帖子合并输出
 * 7. 若 AI 调用失败，自动降级到按互动量排序
 *
 * 【性能优化】
 * 通过 AiRecCacheService 实现异步预计算 + 缓存，
 * 首次请求降级返回传统结果，后台触发 AI 计算，
 * 后续请求直接读取缓存（毫秒级响应）。
 *
 * 【设计模式】
 * 策略模式（Strategy Pattern），通过 Admin 面板全局切换。
 */
@Component
public class AiRecommendationStrategy implements RecommendationStrategy {

    private final UserBehaviorProfileService behaviorProfileService;
    private final RestTemplate restTemplate;

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;               // Ollama 服务地址（本地部署）

    @Value("${ollama.rec.model:qwen3:8b}")
    private String recModel;                // 推荐使用的模型名称

    @Value("${ollama.rec.temperature:0.3}")
    private double temperature;             // 生成温度（越低越确定性，推荐任务用低温）

    @Value("${ollama.rec.num-predict:500}")
    private int numPredict;                 // 最大生成 token 数

    // 送入 AI 的最大候选帖子数（控制 Prompt 长度和推理时间）
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

    // ==================== 预排序：按基础互动分取 Top N ====================
    // AI 模型的上下文窗口有限，不能把所有帖子都塞进 Prompt，
    // 所以先用简单的互动分公式筛选出最有潜力的 25 条送入 AI。

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

    // ==================== Prompt 构造 ====================
    // 将用户画像和候选帖子结构化为自然语言，要求 AI 返回 JSON 排序结果

    /**
     * 构造发送给大模型的 Prompt
     * 包含：用户画像摘要 + 候选帖子列表 + 输出格式要求
     * AI 需要返回 {"ranking": [id1, id2, ...], "reasons": {"id1": "理由", ...}}
     */
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

    // ==================== Ollama HTTP API 调用 ====================
    // 通过 POST /api/generate 接口调用本地大模型，stream=false 等待完整响应

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

    // ==================== AI 响应 JSON 解析 ====================
    // 从 AI 返回文本中提取 JSON，解析 ranking 数组和 reasons 字典

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

    // ==================== 排序合并 ====================
    // 将 AI 排序结果 + 预排序补充 + 剩余候选合并为最终列表

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

    // ==================== 降级排序（Fallback） ====================
    // 当 Ollama 服务不可用或响应解析失败时，回退到按互动量排序

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

    // ==================== Ollama 健康检测 ====================
    // 通过 GET /api/tags 检测 Ollama 服务是否在线

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
