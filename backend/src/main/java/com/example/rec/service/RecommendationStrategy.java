package com.example.rec.service;

import com.example.rec.model.Content;
import java.util.List;

public interface RecommendationStrategy {
    List<Content> recommend(Long userId, List<Content> candidates);
}
