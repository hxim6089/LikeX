package com.example.rec.dto;

import com.example.rec.model.Content;
import com.example.rec.model.Tag;
import com.example.rec.model.User;

import java.util.List;

/**
 * 综合搜索结果 DTO
 */
public class SearchResult {
    private List<Content> posts;     // 帖子结果
    private List<User> users;        // 用户结果
    private List<Tag> topics;        // 话题结果
    private Integer totalPosts;      // 帖子总数
    private Integer totalUsers;      // 用户总数
    private Integer totalTopics;     // 话题总数

    public SearchResult() {}

    // Getters and Setters
    public List<Content> getPosts() { return posts; }
    public void setPosts(List<Content> posts) { this.posts = posts; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public List<Tag> getTopics() { return topics; }
    public void setTopics(List<Tag> topics) { this.topics = topics; }

    public Integer getTotalPosts() { return totalPosts; }
    public void setTotalPosts(Integer totalPosts) { this.totalPosts = totalPosts; }

    public Integer getTotalUsers() { return totalUsers; }
    public void setTotalUsers(Integer totalUsers) { this.totalUsers = totalUsers; }

    public Integer getTotalTopics() { return totalTopics; }
    public void setTotalTopics(Integer totalTopics) { this.totalTopics = totalTopics; }
}
