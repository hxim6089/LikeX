package com.example.rec.controller;

import com.example.rec.service.BehaviorService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/behavior")
public class BehaviorController {

    private final BehaviorService behaviorService;

    public BehaviorController(BehaviorService behaviorService) {
        this.behaviorService = behaviorService;
    }

    @PostMapping("/like")
    public String like(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long contentId = payload.get("contentId");
        behaviorService.likeContent(userId, contentId);
        return "Liked";
    }

    @PostMapping("/view")
    public String view(@RequestBody Map<String, Object> payload) {
        Long userId = toLong(payload.get("userId"));
        Long contentId = toLong(payload.get("contentId"));
        Integer duration = payload.get("duration") != null ? ((Number) payload.get("duration")).intValue() : null;
        behaviorService.recordView(userId, contentId, duration);
        return "Viewed";
    }

    @PostMapping("/dislike")
    public String dislike(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long contentId = payload.get("contentId");
        behaviorService.dislikeContent(userId, contentId);
        return "Disliked";
    }

    @PostMapping("/skip")
    public String skip(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long contentId = payload.get("contentId");
        behaviorService.recordSkip(userId, contentId);
        return "Skipped";
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long) return (Long) val;
        return ((Number) val).longValue();
    }
}
