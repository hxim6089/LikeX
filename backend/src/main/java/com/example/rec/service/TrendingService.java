package com.example.rec.service;

import com.example.rec.dto.TrendingTopic;
import com.example.rec.model.Content;
import com.example.rec.model.Tag;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 热门话题服务
 * 统计并返回系统中最热门的话题标签
 */
@Service
public class TrendingService {

    private final TagRepository tagRepository;
    private final ContentRepository contentRepository;

    public TrendingService(TagRepository tagRepository, ContentRepository contentRepository) {
        this.tagRepository = tagRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 获取热门话题列表
     * 
     * @param limit 返回数量，默认10
     * @param hours 时间窗口（小时），默认24
     * @return 热门话题列表，按热度降序
     */
    @Cacheable(value = "trending", key = "'topics:' + #limit + ':' + #hours")
    public List<TrendingTopic> getTrendingTopics(int limit, int hours) {
        if (limit <= 0) limit = 10;
        if (hours <= 0) hours = 24;

        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        
        // 获取所有标签
        List<Tag> allTags = tagRepository.findAll();
        if (allTags.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取时间范围内的所有内容
        List<Content> recentContents = contentRepository.findByCreatedAtAfter(since);
        
        // 统计每个标签的帖子数和互动量
        Map<String, Long> tagPostCount = new HashMap<>();
        Map<String, Long> tagEngagement = new HashMap<>();

        for (Content content : recentContents) {
            if (content.getTags() != null) {
                for (Tag tag : content.getTags()) {
                    String tagName = tag.getName();
                    tagPostCount.merge(tagName, 1L, Long::sum);
                    
                    // 计算互动量: likes + comments + reposts
                    long engagement = (content.getLikeCount() != null ? content.getLikeCount() : 0)
                            + (content.getCommentCount() != null ? content.getCommentCount() : 0)
                            + (content.getRepostCount() != null ? content.getRepostCount() : 0);
                    tagEngagement.merge(tagName, engagement, Long::sum);
                }
            }
        }

        // 构建热门话题列表
        List<TrendingTopic> trendingTopics = tagPostCount.entrySet().stream()
                .map(entry -> new TrendingTopic(
                        entry.getKey(),
                        entry.getValue(),
                        tagEngagement.getOrDefault(entry.getKey(), 0L)
                ))
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());

        return trendingTopics;
    }

    /**
     * 获取热门话题（使用默认参数）
     */
    public List<TrendingTopic> getTrendingTopics() {
        return getTrendingTopics(10, 24);
    }

    /**
     * 检查内容是否包含热门话题
     * 
     * @param content 待检查的内容
     * @return 包含的热门话题数量
     */
    public int countTrendingTagsInContent(Content content) {
        if (content == null || content.getTags() == null || content.getTags().isEmpty()) {
            return 0;
        }

        List<TrendingTopic> trendingTopics = getTrendingTopics(20, 24);
        Set<String> trendingNames = trendingTopics.stream()
                .map(TrendingTopic::getName)
                .collect(Collectors.toSet());

        int count = 0;
        for (Tag tag : content.getTags()) {
            if (trendingNames.contains(tag.getName())) {
                count++;
            }
        }
        return count;
    }
}
