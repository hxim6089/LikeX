package com.example.rec.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;

@Data
@Entity
@Table(name = "contents")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonIgnoreProperties({"parentContent", "repostOf", "quoteOf", "replies"})
    private Content parentContent; // For Comments

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    private String category;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "like_count")
    private Integer likeCount = 0;

    @Column(name = "comment_count")
    private Integer commentCount = 0;
    
    @Column(name = "dislike_count")
    private Integer dislikeCount = 0;

    @Column(name = "repost_count")
    private Integer repostCount = 0;
    
    // 转发原帖
    @ManyToOne
    @JoinColumn(name = "repost_of_id")
    @JsonIgnoreProperties({"parentContent", "repostOf", "quoteOf", "replies"})
    private Content repostOf;
    
    // 引用原帖
    @ManyToOne
    @JoinColumn(name = "quote_of_id")
    @JsonIgnoreProperties({"parentContent", "repostOf", "quoteOf", "replies"})
    private Content quoteOf;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @ManyToMany
    @JoinTable(
        name = "content_tags",
        joinColumns = @JoinColumn(name = "content_id"),
        inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private java.util.Set<Tag> tags = new java.util.HashSet<>(); // N:N 关联标签

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Transient
    private boolean isLiked;

    @Transient
    private boolean isDisliked;

    @Transient
    private java.util.List<Content> replies = new java.util.ArrayList<>();

    @Transient
    private String networkSource; // IN_NETWORK / OUT_OF_NETWORK (不覆盖 category)

    // Manual Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Content getParentContent() { return parentContent; }
    public void setParentContent(Content parentContent) { this.parentContent = parentContent; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }

    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }

    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.util.Set<Tag> getTags() { return tags; }
    public void setTags(java.util.Set<Tag> tags) { this.tags = tags; }

    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }

    public boolean isDisliked() { return isDisliked; }
    public void setDisliked(boolean disliked) { isDisliked = disliked; }

    public Integer getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(Integer dislikeCount) { this.dislikeCount = dislikeCount; }

    public java.util.List<Content> getReplies() { return replies; }
    public void setReplies(java.util.List<Content> replies) { this.replies = replies; }
    
    public Integer getRepostCount() { return repostCount; }
    public void setRepostCount(Integer repostCount) { this.repostCount = repostCount; }
    
    public Content getRepostOf() { return repostOf; }
    public void setRepostOf(Content repostOf) { this.repostOf = repostOf; }
    
    public Content getQuoteOf() { return quoteOf; }
    public void setQuoteOf(Content quoteOf) { this.quoteOf = quoteOf; }

    public String getNetworkSource() { return networkSource; }
    public void setNetworkSource(String networkSource) { this.networkSource = networkSource; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Content content = (Content) o;
        return id != null && id.equals(content.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Content{id=" + id + ", title='" + title + "'}";
    }
}
