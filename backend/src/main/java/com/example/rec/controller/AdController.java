package com.example.rec.controller;

import com.example.rec.service.AdService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ads")
public class AdController {

    private final AdService adService;

    public AdController(AdService adService) {
        this.adService = adService;
    }

    /**
     * 获取与用户画像匹配的广告
     */
    @GetMapping("/relevant")
    public ResponseEntity<?> getRelevantAds(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "2") int count) {
        List<Map<String, Object>> ads = adService.getRelevantAds(userId, count);
        return ResponseEntity.ok(ads);
    }

    /**
     * 记录广告展示
     */
    @PostMapping("/{id}/impression")
    public ResponseEntity<?> recordImpression(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId) {
        adService.recordImpression(id, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 记录广告点击
     */
    @PostMapping("/{id}/click")
    public ResponseEntity<?> recordClick(@PathVariable Long id) {
        adService.recordClick(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 广告统计报表
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getAdStats() {
        Map<String, Object> stats = adService.getAdStats();
        return ResponseEntity.ok(stats);
    }
}
