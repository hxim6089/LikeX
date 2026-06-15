package com.example.rec.repository;

import com.example.rec.model.Behavior;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BehaviorRepository extends JpaRepository<Behavior, Long> {
    List<Behavior> findByUserId(Long userId);
    List<Behavior> findByContentId(Long contentId);
    List<Behavior> findByUserIdAndType(Long userId, String type);
    
    List<Behavior> findByType(String type);

    long countByUserId(Long userId);

    List<Behavior> findByUserIdAndContentIdAndType(Long userId, Long contentId, String type);

    void deleteByContentId(Long contentId);
}
