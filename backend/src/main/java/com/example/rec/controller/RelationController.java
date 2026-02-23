package com.example.rec.controller;

import com.example.rec.model.User;
import com.example.rec.service.RelationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relation")
public class RelationController {

    private final RelationService relationService;

    public RelationController(RelationService relationService) {
        this.relationService = relationService;
    }

    @PostMapping("/follow")
    public String follow(@RequestBody Map<String, Long> payload) {
        Long followerId = payload.get("followerId");
        Long followeeId = payload.get("followeeId");
        relationService.followUser(followerId, followeeId);
        return "Followed";
    }

    @PostMapping("/unfollow")
    public String unfollow(@RequestBody Map<String, Long> payload) {
        Long followerId = payload.get("followerId");
        Long followeeId = payload.get("followeeId");
        relationService.unfollowUser(followerId, followeeId);
        return "Unfollowed";
    }

    @GetMapping("/status")
    public boolean isFollowing(@RequestParam Long followerId, @RequestParam Long followeeId) {
        return relationService.isFollowing(followerId, followeeId);
    }
    
    @GetMapping("/suggestions")
    public List<User> getSuggestions(@RequestParam Long userId) {
        return relationService.getSuggestions(userId);
    }
}
