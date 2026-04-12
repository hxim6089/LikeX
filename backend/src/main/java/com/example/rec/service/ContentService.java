package com.example.rec.service;

import com.example.rec.model.Content;
import com.example.rec.repository.ContentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ContentService {
    private final ContentRepository contentRepository;
    private final RelationService relationService;
    private final NotificationService notificationService;
    private final com.example.rec.repository.TagRepository tagRepository;
    private final com.example.rec.repository.BehaviorRepository behaviorRepository;
    private final com.example.rec.repository.UserRepository userRepository;
    private final AiTaggingService aiTaggingService;

    public ContentService(ContentRepository contentRepository, RelationService relationService, NotificationService notificationService, com.example.rec.repository.TagRepository tagRepository, com.example.rec.repository.BehaviorRepository behaviorRepository, com.example.rec.repository.UserRepository userRepository, AiTaggingService aiTaggingService) {
        this.contentRepository = contentRepository;
        this.relationService = relationService;
        this.notificationService = notificationService;
        this.tagRepository = tagRepository;
        this.behaviorRepository = behaviorRepository;
        this.userRepository = userRepository;
        this.aiTaggingService = aiTaggingService;
    }

    /**
     * 获取全站推荐流 (Feed)
     */
    public Page<Content> getFeed(int page, int size, Long currentUserId) {
        Page<Content> feed = contentRepository.findByParentContentIsNullOrderByCreatedAtDesc(PageRequest.of(page, size));
        if (currentUserId != null) {
            fillIsLiked(feed.getContent(), currentUserId);
        }
        return feed;
    }

    /**
     * 获取关注流 (Following Feed)
     */
    public Page<Content> getFollowingFeed(Long userId, int page, int size) {
        // 1. 获取当前用户关注的所有用户ID列表
        List<Long> followingIds = relationService.getFollowingIds(userId);
        if (followingIds.isEmpty()) {
            return Page.empty();
        }
        // 2. 数据库查询
        Page<Content> feed = contentRepository.findByAuthorIds(followingIds, PageRequest.of(page, size));
        fillIsLiked(feed.getContent(), userId);
        return feed;
    }

    private void fillIsLiked(List<Content> contents, Long userId) {
        if (contents == null || contents.isEmpty()) return;
        List<com.example.rec.model.Behavior> likes = behaviorRepository.findByUserIdAndType(userId, "LIKE");
        java.util.Set<Long> likedContentIds = likes.stream()
                .map(com.example.rec.model.Behavior::getContentId)
                .collect(java.util.stream.Collectors.toSet());
        
        for (Content c : contents) {
            if (likedContentIds.contains(c.getId())) {
                c.setLiked(true);
            }
        }
    }
    
    public List<Content> getAll() {
        return contentRepository.findAll();
    }

    /**
     * 发布推文
     */
    public Content publish(Long authorId, String text, String imageUrl) {
        // 模拟: 创建作者对象 (在真实场景应查询 User表)
        com.example.rec.model.User author = new com.example.rec.model.User();
        author.setId(authorId);

        Content content = new Content();
        content.setAuthor(author);
        content.setContent(text);
        content.setImageUrl(imageUrl); // 设置图片URL (如有)
        content.setTitle("Tweet"); 
        content.setCategory("Life"); 
        
        // Parse Hashtags (显式 #tag)
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("#([a-zA-Z0-9_\\u4e00-\\u9fa5]+)");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String tagName = matcher.group(1);
            com.example.rec.model.Tag tag = tagRepository.findByName(tagName)
                    .orElseGet(() -> tagRepository.save(new com.example.rec.model.Tag(tagName)));
            content.getTags().add(tag);
        }
        
        Content saved = contentRepository.save(content);
        
        // AI 自动语义打标 (异步, 不阻塞发帖)
        try {
            new Thread(() -> {
                try {
                    aiTaggingService.tagContent(saved);
                } catch (Exception e) {
                    System.err.println("AI auto-tagging failed for content #" + saved.getId() + ": " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            // AI 打标失败不影响正常发帖
            System.err.println("Failed to start AI tagging thread: " + e.getMessage());
        }
        
        return saved;
    }

    /**
     * 发布评论
     * 评论本质上也是 Content, 但关联了 parentContent
     */
    public Content addComment(Long parentId, Long authorId, String text) {
        // 1. 查找父推文
        Content parent = contentRepository.findById(parentId).orElseThrow();
        
        // 2. 获取完整的 Author 信息
        com.example.rec.model.User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 3. 创建评论实体
        Content comment = new Content();
        comment.setAuthor(author);
        comment.setParentContent(parent); // 关联父级
        comment.setContent(text);
        comment.setTitle("Comment");
        comment.setCategory(parent.getCategory()); // 继承父级分类
        
        // 4. 更新父级互动数 (Comment Count + 1) -> 影响推荐算法得分
        parent.setCommentCount(parent.getCommentCount() + 1);
        contentRepository.save(parent);

        Content savedComment = contentRepository.save(comment);
        
        // 5. Trigger Notification
        notificationService.createNotification(parent.getAuthor().getId(), authorId, "COMMENT", parentId);
        
        return savedComment;
    }

    /**
     * 获取某条推文的所有评论 (支持二级评论)
     */
    public List<Content> getComments(Long parentId) {
        List<Content> comments = contentRepository.findByParentContentId(parentId);
        // Populate one level of sub-comments (replies)
        for (Content comment : comments) {
             List<Content> replies = contentRepository.findByParentContentId(comment.getId());
             comment.setReplies(replies);
        }
        return comments;
    }

    public Content getContentById(Long id) {
        return contentRepository.findById(id).orElseThrow();
    }

    /**
     * 全局搜索
     * 简单的数据库模糊查询 (LIKE %keyword%)
     */
    public List<Content> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        return contentRepository.findByContentContainingIgnoreCase(keyword);
    }

    /**
     * 获取指定用户的发布列表
     */
    public List<Content> getUserPosts(Long userId) {
        // Only top-level posts (not replies)
        return contentRepository.findByAuthorIdAndParentContentIsNullOrderByCreatedAtDesc(userId);
    }

    public List<Content> getUserReplies(Long userId) {
        return contentRepository.findByAuthorIdAndParentContentIsNotNullOrderByCreatedAtDesc(userId);
    }

    public List<Content> getLikedContent(Long userId) {
        List<com.example.rec.model.Behavior> likes = behaviorRepository.findByUserIdAndType(userId, "LIKE");
        List<Long> contentIds = likes.stream().map(com.example.rec.model.Behavior::getContentId).toList();
        return contentRepository.findAllById(contentIds);
    }
    
    /**
     * 转发帖子 (Repost)
     */
    public Content repost(Long originalId, Long authorId) {
        Content original = contentRepository.findById(originalId)
                .orElseThrow(() -> new RuntimeException("Original post not found"));
        com.example.rec.model.User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Content repost = new Content();
        repost.setAuthor(author);
        repost.setRepostOf(original);
        repost.setTitle("Repost");
        repost.setContent(""); // 纯转发无正文
        repost.setCategory(original.getCategory());
        
        // 更新原帖转发数
        original.setRepostCount((original.getRepostCount() != null ? original.getRepostCount() : 0) + 1);
        contentRepository.save(original);
        
        // 发送通知
        notificationService.createNotification(original.getAuthor().getId(), authorId, "REPOST", originalId);
        
        return contentRepository.save(repost);
    }
    
    /**
     * 引用帖子 (Quote)
     */
    public Content quote(Long originalId, Long authorId, String comment) {
        Content original = contentRepository.findById(originalId)
                .orElseThrow(() -> new RuntimeException("Original post not found"));
        com.example.rec.model.User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Content quote = new Content();
        quote.setAuthor(author);
        quote.setQuoteOf(original);
        quote.setTitle("Quote");
        quote.setContent(comment);
        quote.setCategory(original.getCategory());
        
        // 更新原帖转发数
        original.setRepostCount((original.getRepostCount() != null ? original.getRepostCount() : 0) + 1);
        contentRepository.save(original);
        
        // 发送通知
        notificationService.createNotification(original.getAuthor().getId(), authorId, "QUOTE", originalId);
        
        return contentRepository.save(quote);
    }

    /**
     * 删除帖子（仅作者本人可删除）
     */
    public void deleteContent(Long contentId, Long userId) {
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        // 管理员或作者本人可删除
        com.example.rec.model.User operator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!"ADMIN".equals(operator.getRole()) && !content.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("No permission to delete this post");
        }
        contentRepository.delete(content);
    }
}
