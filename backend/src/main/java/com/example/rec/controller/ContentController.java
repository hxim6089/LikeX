package com.example.rec.controller;

import com.example.rec.model.Content;
import com.example.rec.service.ContentService;
import com.example.rec.service.RecommendationService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import java.util.List;

@RestController
@RequestMapping("/api/content")
public class ContentController {

    private final ContentService contentService;
    private final RecommendationService recommendationService;

    public ContentController(ContentService contentService, RecommendationService recommendationService) {
        this.contentService = contentService;
        this.recommendationService = recommendationService;
    }

    /**
     * 首页推荐流接口
     * 策略: 如果前端传了 `personalized=true` (For You), 则走算法推荐; 否则走普通时间流.
     * debug=true 时返回带评分详情的结果（用于答辩展示）
     */
    @GetMapping("/feed")
    public Object getFeed(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(defaultValue = "false") boolean personalized,
                          @RequestParam(required = false) Long userId,
                          @RequestParam(defaultValue = "false") boolean debug) {
        
        // Debug 模式：返回带评分详情的结果（用于答辩展示）
        if (debug && personalized && userId != null) {
            java.util.List<com.example.rec.dto.ContentWithScore> recList = 
                recommendationService.getRecommendedFeedWithScore(userId);
            int start = Math.min((int)PageRequest.of(page, size).getOffset(), recList.size());
            int end = Math.min((start + size), recList.size());
            
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("content", recList.subList(start, end));
            result.put("totalElements", recList.size());
            result.put("totalPages", (int) Math.ceil((double) recList.size() / size));
            result.put("debug", true);
            return result;
        }
        
        if (personalized && userId != null) {
            // 算法推荐流 (返回 List, 暂时手动转 Page 以兼容前端)
            List<Content> recList = recommendationService.getRecommendedFeed(userId);
            int start = Math.min((int)PageRequest.of(page, size).getOffset(), recList.size());
            int end = Math.min((start + size), recList.size());
            return new PageImpl<>(recList.subList(start, end), PageRequest.of(page, size), recList.size());
        }
        // 普通时间倒序流
        return contentService.getFeed(page, size, userId);
    }
    
    /**
     * "关注"页面的内容流
     */
    @GetMapping("/following")
    public Page<Content> getFollowingFeed(@RequestParam Long userId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        return contentService.getFollowingFeed(userId, page, size);
    }

    /**
     * 发布推文
     */
    @PostMapping("/publish")
    public Content publish(@RequestBody java.util.Map<String, Object> payload) {
        Long authorId = Long.valueOf(payload.get("authorId").toString());
        String content = (String) payload.get("content");
        String imageUrl = (String) payload.get("imageUrl"); // 可选
        return contentService.publish(authorId, content, imageUrl);
    }

    /**
     * 发布评论
     */
    @PostMapping("/{id}/comment")
    public Content addComment(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        Long authorId = Long.valueOf(payload.get("authorId").toString());
        String content = (String) payload.get("content");
        return contentService.addComment(id, authorId, content);
    }

    @GetMapping("/{id}/comments")
    public List<Content> getComments(@PathVariable Long id) {
        return contentService.getComments(id);
    }

    @GetMapping("/{id}")
    public Content getContent(@PathVariable Long id) {
        return contentService.getContentById(id);
    }

    @GetMapping("/search")
    public List<Content> search(@RequestParam String keyword) {
        return contentService.search(keyword);
    }

    @GetMapping("/user/{userId}")
    public List<Content> getUserPosts(@PathVariable Long userId) {
        return contentService.getUserPosts(userId);
    }

    @GetMapping("/user/{userId}/replies")
    public List<Content> getUserReplies(@PathVariable Long userId) {
        return contentService.getUserReplies(userId);
    }

    @GetMapping("/user/{userId}/likes")
    public List<Content> getLikedContent(@PathVariable Long userId) {
        return contentService.getLikedContent(userId);
    }
    
    /**
     * 转发帖子
     */
    @PostMapping("/{id}/repost")
    public Content repost(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        Long authorId = Long.valueOf(payload.get("authorId").toString());
        return contentService.repost(id, authorId);
    }
    
    /**
     * 引用帖子
     */
    @PostMapping("/{id}/quote")
    public Content quote(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        Long authorId = Long.valueOf(payload.get("authorId").toString());
        String content = (String) payload.get("content");
        return contentService.quote(id, authorId, content);
    }

    /**
     * 删除帖子（仅作者本人可删除）
     */
    @DeleteMapping("/{id}")
    public java.util.Map<String, String> deleteContent(@PathVariable Long id,
                                                        @RequestParam Long userId) {
        contentService.deleteContent(id, userId);
        return java.util.Map.of("message", "Deleted");
    }
}
