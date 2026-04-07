package com.example.rec.controller;

import com.example.rec.dto.SearchResult;
import com.example.rec.dto.SearchSuggestion;
import com.example.rec.model.Content;
import com.example.rec.model.Tag;
import com.example.rec.model.User;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.TagRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索控制器
 * 提供综合搜索和搜索建议功能
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;

    public SearchController(ContentRepository contentRepository,
                           UserRepository userRepository,
                           TagRepository tagRepository) {
        this.contentRepository = contentRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    /**
     * 综合搜索
     * GET /api/search?q=关键词&type=all
     * 
     * @param q 搜索关键词
     * @param type 搜索类型: all/posts/users/topics
     */
    @GetMapping
    public Object search(
            @RequestParam String q,
            @RequestParam(defaultValue = "all") String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        if (q == null || q.trim().isEmpty()) {
            SearchResult result = new SearchResult();
            result.setPosts(new ArrayList<>());
            result.setUsers(new ArrayList<>());
            result.setTopics(new ArrayList<>());
            return result;
        }

        String keyword = q.trim();
        String cleanKeyword = keyword.startsWith("#") ? keyword.substring(1) : keyword;

        // posts 类型使用分页
        if ("posts".equals(type)) {
            Page<Content> postPage = contentRepository.findByContentContainingIgnoreCase(
                    cleanKeyword, PageRequest.of(page, size));
            Map<String, Object> pagedResult = new HashMap<>();
            pagedResult.put("posts", postPage.getContent());
            pagedResult.put("totalPosts", postPage.getTotalElements());
            pagedResult.put("totalPages", postPage.getTotalPages());
            pagedResult.put("currentPage", page);
            return pagedResult;
        }

        SearchResult result = new SearchResult();
        switch (type) {
            case "users":
                List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrHandleContainingIgnoreCase(cleanKeyword, cleanKeyword);
                result.setUsers(users);
                result.setTotalUsers(users.size());
                break;
                
            case "topics":
                List<Tag> topics = tagRepository.findByNameContainingIgnoreCase(cleanKeyword);
                result.setTopics(topics);
                result.setTotalTopics(topics.size());
                break;
                
            case "all":
            default:
                List<Content> allPosts = contentRepository.findByContentContainingIgnoreCase(cleanKeyword);
                List<User> allUsers = userRepository.findByUsernameContainingIgnoreCaseOrHandleContainingIgnoreCase(cleanKeyword, cleanKeyword);
                List<Tag> allTopics = tagRepository.findByNameContainingIgnoreCase(cleanKeyword);
                
                result.setPosts(allPosts.stream().limit(10).collect(Collectors.toList()));
                result.setUsers(allUsers.stream().limit(5).collect(Collectors.toList()));
                result.setTopics(allTopics.stream().limit(5).collect(Collectors.toList()));
                result.setTotalPosts(allPosts.size());
                result.setTotalUsers(allUsers.size());
                result.setTotalTopics(allTopics.size());
                break;
        }

        return result;
    }

    /**
     * 搜索建议 (自动补全)
     * GET /api/search/suggest?q=关键词
     * 
     * @param q 输入的关键词 (>=2字符)
     * @return 建议列表 (最多10条)
     */
    @GetMapping("/suggest")
    public List<SearchSuggestion> getSuggestions(@RequestParam String q) {
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        if (q == null || q.trim().length() < 2) {
            return suggestions;
        }

        String keyword = q.trim();
        
        // 1. 如果以@开头，优先搜索用户
        if (keyword.startsWith("@")) {
            String handleKeyword = keyword.substring(1);
            List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrHandleContainingIgnoreCase(handleKeyword, handleKeyword);
            for (User user : users.stream().limit(5).collect(Collectors.toList())) {
                suggestions.add(SearchSuggestion.forUser(
                    user.getUsername(),
                    user.getHandle() != null ? user.getHandle() : "@" + user.getUsername(),
                    user.getAvatarUrl()
                ));
            }
        }
        // 2. 如果以#开头，优先搜索话题
        else if (keyword.startsWith("#")) {
            String tagKeyword = keyword.substring(1);
            List<Tag> tags = tagRepository.findByNameContainingIgnoreCase(tagKeyword);
            for (Tag tag : tags.stream().limit(5).collect(Collectors.toList())) {
                // 获取话题帖子数量
                long postCount = contentRepository.findByTags_Name(tag.getName()).size();
                suggestions.add(SearchSuggestion.forTopic(tag.getName(), postCount));
            }
        }
        // 3. 普通搜索：混合用户和话题
        else {
            // 用户建议
            List<User> users = userRepository.findByUsernameContainingIgnoreCaseOrHandleContainingIgnoreCase(keyword, keyword);
            for (User user : users.stream().limit(3).collect(Collectors.toList())) {
                suggestions.add(SearchSuggestion.forUser(
                    user.getUsername(),
                    user.getHandle() != null ? user.getHandle() : "@" + user.getUsername(),
                    user.getAvatarUrl()
                ));
            }
            
            // 话题建议
            List<Tag> tags = tagRepository.findByNameContainingIgnoreCase(keyword);
            for (Tag tag : tags.stream().limit(3).collect(Collectors.toList())) {
                long postCount = contentRepository.findByTags_Name(tag.getName()).size();
                suggestions.add(SearchSuggestion.forTopic(tag.getName(), postCount));
            }
        }

        // 限制最多10条
        return suggestions.stream().limit(10).collect(Collectors.toList());
    }
}
