package com.example.rec.repository;

import com.example.rec.model.Behavior;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BehaviorRepository extends JpaRepository<Behavior, Long> {
    List<Behavior> findByUserId(Long userId);
    List<Behavior> findByContentId(Long contentId);
    List<Behavior> findByUserIdAndType(Long userId, String type);
    
    // 获取所有某类型行为（用于协同过滤）
    List<Behavior> findByType(String type);
}
