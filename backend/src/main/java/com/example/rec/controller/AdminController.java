package com.example.rec.controller;

import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.UserRepository;
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

    public AdminController(UserRepository userRepository,
                           ContentRepository contentRepository,
                           BehaviorRepository behaviorRepository) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
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
}
