package com.example.rec.service;

import com.example.rec.model.Content;
import com.example.rec.model.Tag;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 自动打标签服务
 * 
 * 通过本地部署的 Ollama (Qwen3.5-0.8B) 分析帖文内容,
 * 自动从候选标签中选择 1-3 个最相关的语义标签。
 * 
 * 核心功能:
 * 1. tagContent()     - 单条帖文 AI 打标
 * 2. batchTagAll()    - 批量补标所有无标签帖文
 */
@Service
public class AiTaggingService {

    @Value("${ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${ollama.model:qwen3.5:0.8b}")
    private String ollamaModel;

    private final RestTemplate restTemplate;
    private final TagRepository tagRepository;
    private final ContentRepository contentRepository;

    public AiTaggingService(RestTemplate restTemplate,
                            TagRepository tagRepository,
                            ContentRepository contentRepository) {
        this.restTemplate = restTemplate;
        this.tagRepository = tagRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 对单条帖文进行 AI 打标签
     * 1. 获取系统中所有已有标签作为候选列表
     * 2. 构造 Prompt 发送给 Ollama
     * 3. 解析返回的标签名
     * 4. 写入 content_tags 关联表
     */
    public List<String> tagContent(Content content) {
        if (content == null || content.getContent() == null || content.getContent().isBlank()) {
            return Collections.emptyList();
        }

        // 1. 获取所有候选标签
        List<String> candidateTags = tagRepository.findAll().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());

        // 2. 构造 Prompt
        String prompt = buildPrompt(content.getContent(), candidateTags);

        // 3. 调用 Ollama API
        String aiResponse = callOllama(prompt);
        if (aiResponse == null || aiResponse.isBlank()) {
            return Collections.emptyList();
        }

        // 4. 解析标签
        List<String> tagNames = parseTagResponse(aiResponse, candidateTags);

        // 5. 写入数据库
        for (String tagName : tagNames) {
            Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));
            content.getTags().add(tag);
        }
        contentRepository.save(content);

        return tagNames;
    }

    /**
     * 批量补标所有无标签帖文
     * 返回: 成功打标的帖文数量
     */
    public Map<String, Object> batchTagAll() {
        // 查找所有无标签的帖文
        List<Content> allContents = contentRepository.findAll();
        List<Content> untagged = allContents.stream()
                .filter(c -> c.getTags() == null || c.getTags().isEmpty())
                .filter(c -> c.getParentContent() == null) // 跳过评论
                .collect(Collectors.toList());

        int total = untagged.size();
        int success = 0;
        int failed = 0;
        List<Map<String, Object>> details = new ArrayList<>();

        for (Content content : untagged) {
            try {
                List<String> tags = tagContent(content);
                if (!tags.isEmpty()) {
                    success++;
                    Map<String, Object> detail = new HashMap<>();
                    detail.put("contentId", content.getId());
                    detail.put("preview", truncate(content.getContent(), 50));
                    detail.put("tags", tags);
                    details.add(detail);
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                System.err.println("AI tagging failed for content #" + content.getId() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        result.put("details", details.size() > 20 ? details.subList(0, 20) : details);
        return result;
    }

    /**
     * 构造 Prompt
     * 使用限定性指令让 AI 只输出标签名
     */
    private String buildPrompt(String postContent, List<String> candidateTags) {
        String tagList = String.join(", ", candidateTags);
        return "You are a content tagging AI. Analyze the post below and select 1-3 most relevant tags from the candidate list.\n" +
               "Rules:\n" +
               "- ONLY output tag names separated by commas\n" +
               "- NO explanations, NO extra text\n" +
               "- If no candidate tag fits, suggest ONE new short tag\n" +
               "- Prefer existing candidate tags\n\n" +
               "Candidate tags: " + tagList + "\n\n" +
               "Post: " + truncate(postContent, 300);
    }

    /**
     * 调用 Ollama API (POST /api/generate)
     * 使用 stream: false 获取完整响应
     * 设置 think: false 关闭思考模式加速推理
     */
    private String callOllama(String prompt) {
        try {
            String url = ollamaUrl + "/api/generate";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("model", ollamaModel);
            body.put("prompt", prompt);
            body.put("stream", false);

            // 关闭 thinking 模式,加速推理 (Qwen3.5 特性)
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", 0.3); // 低温度 = 更确定性的输出
            options.put("num_predict", 50);   // 限制输出长度,标签不需要太多 token
            body.put("options", options);
            body.put("think", false);         // 关闭 CoT 思考

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("response")) {
                return ((String) response.get("response")).trim();
            }

            return null;
        } catch (Exception e) {
            System.err.println("Ollama API call failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * 解析 AI 返回的标签文本
     * 处理各种返回格式: "AI, Tech" / "AI,Tech" / "- AI\n- Tech"
     */
    private List<String> parseTagResponse(String response, List<String> candidateTags) {
        // 清理 thinking 标签残留 (以防万一)
        response = response.replaceAll("<think>.*?</think>", "").trim();
        
        // 去掉可能的 markdown 列表格式
        response = response.replaceAll("^[\\-\\*•]\\s*", "").trim();
        response = response.replaceAll("\n[\\-\\*•]\\s*", ",").trim();

        // 按逗号、换行分割
        String[] parts = response.split("[,，\\n]+");

        Set<String> candidateSet = new HashSet<>(candidateTags);
        List<String> result = new ArrayList<>();

        for (String part : parts) {
            String tag = part.trim()
                    .replaceAll("^[#\"'`]+", "")   // 去掉前缀符号
                    .replaceAll("[\"'`\\.]+$", "")  // 去掉后缀符号
                    .trim();

            if (tag.isEmpty() || tag.length() > 30) continue;

            // 精确匹配候选标签 (忽略大小写)
            String matched = candidateTags.stream()
                    .filter(c -> c.equalsIgnoreCase(tag))
                    .findFirst()
                    .orElse(null);

            if (matched != null) {
                if (!result.contains(matched)) {
                    result.add(matched);
                }
            } else if (result.size() < 3 && tag.matches("[a-zA-Z0-9_\\u4e00-\\u9fa5]+")) {
                // 允许新标签 (但限制为合法字符)
                if (!result.contains(tag)) {
                    result.add(tag);
                }
            }

            if (result.size() >= 3) break;
        }

        return result;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
