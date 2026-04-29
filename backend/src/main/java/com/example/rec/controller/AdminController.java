package com.example.rec.controller;

import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.UserRepository;
import com.example.rec.service.RecommendationStrategyManager;
import com.example.rec.service.XCrawlerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员专用接口
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final BehaviorRepository behaviorRepository;
    private final RecommendationStrategyManager strategyManager;
    private final XCrawlerService xCrawlerService;

    public AdminController(UserRepository userRepository,
                           ContentRepository contentRepository,
                           BehaviorRepository behaviorRepository,
                           RecommendationStrategyManager strategyManager,
                           XCrawlerService xCrawlerService) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
        this.strategyManager = strategyManager;
        this.xCrawlerService = xCrawlerService;
    }

    /**
     * 平台数据概览
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPosts", contentRepository.count());
        stats.put("totalBehaviors", behaviorRepository.count());

        // 今日新增帖子
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayPosts = contentRepository.findByCreatedAtAfter(todayStart).size();
        stats.put("todayNewPosts", todayPosts);

        return stats;
    }

    /**
     * 获取当前推荐策略信息
     */
    @GetMapping("/rec-strategy")
    public Map<String, Object> getRecStrategy() {
        return strategyManager.getStrategyInfo();
    }

    /**
     * 切换推荐策略
     */
    @PutMapping("/rec-strategy")
    public Map<String, Object> switchRecStrategy(@RequestBody Map<String, String> payload) {
        String strategy = payload.get("strategy");
        if (strategy == null || strategy.isBlank()) {
            return Map.of("error", "Missing 'strategy' field");
        }
        try {
            strategyManager.switchStrategy(strategy);
            return Map.of("message", "策略已切换", "current", strategyManager.getCurrentStrategyType());
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * 一键批量爬取 50 条 X 推文（从多个热门账号）
     */
    @PostMapping("/crawl-x-batch")
    public Map<String, Object> crawlXBatch(@RequestBody(required = false) Map<String, Object> payload) {
        int target = 50;
        if (payload != null && payload.get("target") != null) {
            target = ((Number) payload.get("target")).intValue();
        }
        XCrawlerService.CrawlResult result = xCrawlerService.batchCrawl(target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("importedCount", result.importedCount);
        resp.put("skippedDuplicate", result.skippedDuplicate);
        resp.put("parseErrors", result.parseErrors);
        resp.put("startTime", result.startTime != null ? result.startTime.toString() : null);
        resp.put("endTime", result.endTime != null ? result.endTime.toString() : null);
        return resp;
    }

    /**
     * 从 X 爬取指定用户的最新推文
     */
    @PostMapping("/crawl-x")
    public Map<String, Object> crawlX(@RequestBody Map<String, Object> payload) {
        String screenName = (String) payload.get("screenName");
        if (screenName == null || screenName.isBlank()) {
            return Map.of("success", false, "message", "缺少 screenName 参数");
        }
        screenName = screenName.replaceAll("^@", "");

        Long importAsUserId = null;
        if (payload.get("importAsUserId") != null) {
            importAsUserId = ((Number) payload.get("importAsUserId")).longValue();
        }

        XCrawlerService.CrawlResult result = xCrawlerService.crawl(screenName, importAsUserId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("screenName", result.screenName);
        resp.put("importedCount", result.importedCount);
        resp.put("skippedDuplicate", result.skippedDuplicate);
        resp.put("parseErrors", result.parseErrors);
        resp.put("startTime", result.startTime != null ? result.startTime.toString() : null);
        resp.put("endTime", result.endTime != null ? result.endTime.toString() : null);
        return resp;
    }
}
