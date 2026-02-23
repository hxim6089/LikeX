package com.example.rec.repository;

import com.example.rec.model.Ad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdRepository extends JpaRepository<Ad, Long> {
    List<Ad> findByActiveTrue();
    List<Ad> findByCategory(String category);
    List<Ad> findByTargetTagsContaining(String tag);
}
