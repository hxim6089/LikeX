package com.example.rec.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    private final RestTemplate restTemplate;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> chat(String userMessage) {
        if ("sk-placeholder".equals(apiKey)) {
            // Mock response if key is not set
            Map<String, Object> mockRes = new HashMap<>();
            mockRes.put("reply", "Grok (Mock): Please set your API Key in application.properties to use real AI.");
            return mockRes;
        }

        try {
            // Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            // Prepare Body (Standard OpenAI Format)
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            
            body.put("messages", Collections.singletonList(userMsg));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Send Request
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            
            // Parse Response (Simplified optimization)
            // OpenAI structure: choices[0].message.content
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
