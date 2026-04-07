package com.example.rec.repository;

import com.example.rec.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {
    Page<Content> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Feed: 只返回顶级帖文(不含回复)
    Page<Content> findByParentContentIsNullOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT c FROM Content c WHERE c.author.id IN :authorIds ORDER BY c.createdAt DESC")
    Page<Content> findByAuthorIds(@Param("authorIds") List<Long> authorIds, Pageable pageable);

    List<Content> findByParentContentId(Long parentId);
    
    // For Search
    List<Content> findByContentContainingIgnoreCase(String keyword);
    Page<Content> findByContentContainingIgnoreCase(String keyword, Pageable pageable);
    List<Content> findByTags_Name(String tagName);
    
    // List<Content> findByCategoryIn(List<String> categories);

    List<Content> findByAuthorId(Long authorId);
    
    List<Content> findByAuthorIdAndParentContentIsNullOrderByCreatedAtDesc(Long authorId);
    
    List<Content> findByAuthorIdAndParentContentIsNotNullOrderByCreatedAtDesc(Long authorId);

    // Exclude specific author (for recommendations)
    List<Content> findAllByAuthorIdNot(Long authorId);
    
    // In-Network candidates for X-style recommendation
    List<Content> findByAuthorIdInOrderByCreatedAtDesc(List<Long> authorIds);
    
    // Count posts by author (for persona user type analysis)
    long countByAuthorId(Long authorId);
    
    // For trending topics calculation (time-based query)
    @Query("SELECT c FROM Content c WHERE c.createdAt > :since")
    List<Content> findByCreatedAtAfter(@Param("since") java.time.LocalDateTime since);
}
