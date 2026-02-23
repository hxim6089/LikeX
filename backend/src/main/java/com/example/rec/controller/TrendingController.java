package com.example.rec.controller;

import com.example.rec.dto.TrendingTopic;
import com.example.rec.service.TrendingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 热门话题控制器
 */
@RestController
@RequestMapping("/api/trending")
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    /**
     * 获取热门话题列表
     * GET /api/trending?limit=10&hours=24
     * 
     * @param limit 返回数量，默认10
     * @param hours 时间窗口（小时），默认24
     */
    @GetMapping
    public List<TrendingTopic> getTrending(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24") int hours) {
        return trendingService.getTrendingTopics(limit, hours);
    }
}
