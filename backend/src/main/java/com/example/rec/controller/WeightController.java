package com.example.rec.controller;

import com.example.rec.model.User;
import com.example.rec.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 推荐算法权重管理 Controller
 * 
 * 用于保存/读取/重置用户自定义的推荐算法参数,
 * 实现对比实验页 → 主页推荐的参数联动。
 */
@RestController
@RequestMapping("/api/weights")
public class WeightController {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // 默认权重（与 HybridRecommendationStrategy 中的常量对应）
    private static final Map<String, Double> DEFAULT_WEIGHTS = Map.of(
            "wLike", 0.5,
            "wReply", 1.2,
            "wRepost", 2.0,
            "wPersonalization", 100.0,
            "wTrending", 50.0,
            "wSimilarity", 80.0
    );

    public WeightController(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 保存自定义权重（从对比实验页"应用到主页"）
     */
    @PostMapping("/save")
    public Map<String, Object> saveWeights(@RequestBody Map<String, Object> payload) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Object> weights = (Map<String, Object>) payload.get("weights");

            Optional<User> opt = userRepository.findById(userId);
            if (opt.isEmpty()) {
                result.put("success", false);
                result.put("error", "User not found");
                return result;
            }

            User user = opt.get();
            user.setCustomWeights(objectMapper.writeValueAsString(weights));
            userRepository.save(user);

            result.put("success", true);
            result.put("message", "自定义权重已保存");
            result.put("weights", weights);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 获取当前权重（有自定义则返回自定义，否则返回默认）
     */
    @GetMapping("/{userId}")
    public Map<String, Object> getWeights(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isPresent() && opt.get().getCustomWeights() != null) {
                Map<String, Double> custom = objectMapper.readValue(
                        opt.get().getCustomWeights(),
                        new TypeReference<Map<String, Double>>() {}
                );
                result.put("weights", custom);
                result.put("isCustom", true);
            } else {
                result.put("weights", DEFAULT_WEIGHTS);
                result.put("isCustom", false);
            }
        } catch (Exception e) {
            result.put("weights", DEFAULT_WEIGHTS);
            result.put("isCustom", false);
        }
        return result;
    }

    /**
     * 重置为默认权重
     */
    @DeleteMapping("/{userId}")
    public Map<String, Object> resetWeights(@PathVariable Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Optional<User> opt = userRepository.findById(userId);
            if (opt.isPresent()) {
                User user = opt.get();
                user.setCustomWeights(null);
                userRepository.save(user);
                result.put("success", true);
                result.put("message", "已恢复默认算法参数");
            } else {
                result.put("success", false);
                result.put("error", "User not found");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
