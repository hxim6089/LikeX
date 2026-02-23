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
}
