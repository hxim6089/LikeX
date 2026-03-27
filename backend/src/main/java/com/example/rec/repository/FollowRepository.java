package com.example.rec.repository;

import com.example.rec.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    List<Follow> findByFollowerId(Long followerId);
    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    java.util.Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);
    long countByFollowerId(Long followerId);
    long countByFolloweeId(Long followeeId);
}
