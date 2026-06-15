package com.example.rec.controller;

import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.UserRepository;
import com.example.rec.service.KaggleImportService;
import com.example.rec.service.RecommendationStrategyManager;
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
    private final KaggleImportService kaggleImportService;

    public AdminController(UserRepository userRepository,
                           ContentRepository contentRepository,
                           BehaviorRepository behaviorRepository,
                           RecommendationStrategyManager strategyManager,
                           KaggleImportService kaggleImportService) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
        this.strategyManager = strategyManager;
        this.kaggleImportService = kaggleImportService;
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalPosts", contentRepository.count());
        stats.put("totalBehaviors", behaviorRepository.count());

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        long todayPosts = contentRepository.findByCreatedAtAfter(todayStart).size();
        stats.put("todayNewPosts", todayPosts);

        return stats;
    }

    @GetMapping("/rec-strategy")
    public Map<String, Object> getRecStrategy() {
        return strategyManager.getStrategyInfo();
    }

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
     * 一键从 Kaggle 批量导入帖文（默认 News Category 数据集）
     */
    @PostMapping("/import-kaggle-batch")
    public Map<String, Object> importKaggleBatch(@RequestBody(required = false) Map<String, Object> payload) {
        int target = 50;
        if (payload != null && payload.get("target") != null) {
            target = ((Number) payload.get("target")).intValue();
        }
        KaggleImportService.ImportResult result = kaggleImportService.batchImport(target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("importedCount", result.importedCount);
        resp.put("skippedDuplicate", result.skippedDuplicate);
        resp.put("source", result.source);
        resp.put("startTime", result.startTime != null ? result.startTime.toString() : null);
        resp.put("endTime", result.endTime != null ? result.endTime.toString() : null);
        return resp;
    }

    /**
     * 从指定 Kaggle 数据集导入
     */
    @PostMapping("/import-kaggle")
    public Map<String, Object> importKaggle(@RequestBody Map<String, Object> payload) {
        String datasetSlug = (String) payload.get("datasetSlug");
        if (datasetSlug == null || datasetSlug.isBlank()) {
            return Map.of("success", false, "message", "缺少 datasetSlug 参数");
        }
        datasetSlug = datasetSlug.trim();

        int target = 50;
        if (payload.get("target") != null) {
            target = ((Number) payload.get("target")).intValue();
        }

        KaggleImportService.ImportResult result = kaggleImportService.importFromDataset(datasetSlug, target);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", result.success);
        resp.put("message", result.message);
        resp.put("source", result.source);
        resp.put("importedCount", result.importedCount);
        resp.put("skippedDuplicate", result.skippedDuplicate);
        resp.put("startTime", result.startTime != null ? result.startTime.toString() : null);
        resp.put("endTime", result.endTime != null ? result.endTime.toString() : null);
        return resp;
    }
}
