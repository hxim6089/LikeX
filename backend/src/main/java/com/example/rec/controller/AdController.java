package com.example.rec.controller;

import com.example.rec.model.AdConfig;
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
     * 获取所有广告列表（管理用）
     */
    @GetMapping
    public ResponseEntity<?> getAllAds() {
        return ResponseEntity.ok(adService.getAllAds());
    }

    /**
     * 创建广告
     */
    @PostMapping
    public ResponseEntity<?> createAd(@RequestBody com.example.rec.model.Ad ad) {
        return ResponseEntity.ok(adService.createAd(ad));
    }

    /**
     * 编辑广告
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAd(@PathVariable Long id, @RequestBody com.example.rec.model.Ad ad) {
        return ResponseEntity.ok(adService.updateAd(id, ad));
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

    /**
     * 获取广告投放配置
     */
    @GetMapping("/config")
    public ResponseEntity<?> getAdConfig() {
        return ResponseEntity.ok(adService.getAdConfig());
    }

    /**
     * 更新广告投放配置
     */
    @PutMapping("/config")
    public ResponseEntity<?> updateAdConfig(@RequestBody AdConfig config) {
        return ResponseEntity.ok(adService.updateAdConfig(config));
    }
}
