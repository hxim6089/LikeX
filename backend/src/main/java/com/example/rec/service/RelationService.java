package com.example.rec.service;

import com.example.rec.model.Follow;
import com.example.rec.model.User;
import com.example.rec.repository.FollowRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RelationService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RelationService(FollowRepository followRepository, UserRepository userRepository, NotificationService notificationService) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public void followUser(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) throw new RuntimeException("Cannot follow self");
        
        // Check if already following
        if (isFollowing(followerId, followeeId)) return;

        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        followRepository.save(follow);
        
        // Notification
        notificationService.createNotification(followeeId, followerId, "FOLLOW", null);
    }

    public void unfollowUser(Long followerId, Long followeeId) {
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(() -> new RuntimeException("Relation not found"));
        followRepository.delete(follow);
    }

    public boolean isFollowing(Long followerId, Long followeeId) {
        return followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId).isPresent();
    }
    
    public List<Long> getFollowingIds(Long userId) {
        return followRepository.findByFollowerId(userId).stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toList());
    }

    public List<User> getSuggestions(Long userId) {
        // Simple suggestion: just return top 5 users who are not me
        // In real app: check if already followed
        List<Long> followedIds = getFollowingIds(userId);
        return userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(userId) && !followedIds.contains(u.getId()))
                .limit(5)
                .collect(Collectors.toList());
    }
}
