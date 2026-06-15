package com.example.rec.controller;

import com.example.rec.service.NegativeSignalService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/signal")
public class NegativeSignalController {
    
    private final NegativeSignalService negativeSignalService;
    
    public NegativeSignalController(NegativeSignalService negativeSignalService) {
        this.negativeSignalService = negativeSignalService;
    }
    
    /**
     * 标记内容为"不感兴趣"
     */
    @PostMapping("/not-interested")
    public Map<String, String> markNotInterested(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long contentId = payload.get("contentId");
        negativeSignalService.markNotInterested(userId, contentId);
        return Map.of("status", "ok");
    }
    
    /**
     * 屏蔽作者
     */
    @PostMapping("/block")
    public Map<String, String> blockAuthor(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long authorId = payload.get("authorId");
        negativeSignalService.blockAuthor(userId, authorId);
        return Map.of("status", "ok");
    }
    
    /**
     * 静音作者
     */
    @PostMapping("/mute")
    public Map<String, String> muteAuthor(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long authorId = payload.get("authorId");
        negativeSignalService.muteAuthor(userId, authorId);
        return Map.of("status", "ok");
    }
    
    /**
     * 取消屏蔽
     */
    @DeleteMapping("/unblock")
    public Map<String, String> unblockAuthor(@RequestBody Map<String, Long> payload) {
        Long userId = payload.get("userId");
        Long authorId = payload.get("authorId");
        negativeSignalService.unblockAuthor(userId, authorId);
        return Map.of("status", "ok");
    }
}
