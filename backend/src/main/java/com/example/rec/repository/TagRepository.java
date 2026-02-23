package com.example.rec.repository;

import com.example.rec.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
    
    // 模糊搜索标签
    List<Tag> findByNameContainingIgnoreCase(String keyword);
}
