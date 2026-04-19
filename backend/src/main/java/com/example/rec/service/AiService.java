package com.example.rec.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AiService {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final RestTemplate restTemplate;

    private static final String SYSTEM_PROMPT = """
            你是 Grok，一个类似 X/Twitter 社交媒体推荐平台的 AI 智能助手。

            你所在的平台具备以下核心功能：
            - 信息流推荐：基于多因子混合排序算法（行为加权、TF-IDF 内容相似度、协同过滤、时间衰减、热门话题加成）
            - 用户画像系统：兴趣标签分布、行为分型（Creator/Interactor/Consumer）、活跃度分析
            - 社交互动：发帖、评论、点赞、转发、引用、关注
            - 广告智能投放：基于用户兴趣标签匹配的原生广告分发
            - 算法可视化：排序管道漏斗图、权重动态调节、推荐流 vs 时间流对比实验

            你的风格：专业但友好，回答简洁有洞察力，适当幽默。可以用中文或英文回答，跟随用户的语言。
            当用户询问平台功能时，基于以上信息给出准确回答。
            """;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> chat(String userMessage, List<Map<String, String>> history) {
        if ("sk-placeholder".equals(apiKey)) {
            Map<String, Object> mockRes = new HashMap<>();
            mockRes.put("reply", "Grok (Mock): Please set your API Key in application.properties to use real AI.");
            return mockRes;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Build messages list with system prompt + history + current message
            List<Map<String, String>> messages = new ArrayList<>();
            
            // System prompt
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);
            
            // Conversation history (if provided)
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    if (role != null && content != null 
                        && ("user".equals(role) || "assistant".equals(role))) {
                        Map<String, String> histMsg = new HashMap<>();
                        histMsg.put("role", role);
                        histMsg.put("content", content);
                        messages.add(histMsg);
                    }
                }
            } else {
                // No history, just send the single user message
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);
                messages.add(userMsg);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            Map<String, Object> resBody = response.getBody();
            if (resBody != null && resBody.containsKey("choices")) {
                 java.util.List choices = (java.util.List) resBody.get("choices");
                 if (!choices.isEmpty()) {
                     Map choice = (Map) choices.get(0);
                     Map message = (Map) choice.get("message");
                     String content = (String) message.get("content");
                     
                     Map<String, Object> result = new HashMap<>();
                     result.put("reply", content);
                     return result;
                 }
            }
            
            return Collections.singletonMap("reply", "AI returned empty response.");

        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonMap("reply", "Error communicating with AI: " + e.getMessage());
        }
    }
}
