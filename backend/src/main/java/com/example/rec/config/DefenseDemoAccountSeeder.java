package com.example.rec.config;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.model.Follow;
import com.example.rec.model.User;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.FollowRepository;
import com.example.rec.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Creates two stable, contrasting accounts for the graduation defense demo.
 *
 * The seeder is additive and idempotent: it never deletes existing data and
 * checks every follow and behavior before inserting it.
 */
@Component
@Order(20)
@ConditionalOnProperty(name = "demo.accounts.enabled", havingValue = "true", matchIfMissing = true)
public class DefenseDemoAccountSeeder implements CommandLineRunner {

    static final String DEMO_PASSWORD = "password";

    private static final List<String> TECH_KEYWORDS = List.of(
            "ai", "tech", "java", "vue", "software", "data", "algorithm", "recommend",
            "machine learning", "科技", "人工智能", "算法", "数据", "推荐", "机器学习", "编程");
    private static final List<String> LIFESTYLE_KEYWORDS = List.of(
            "sports", "football", "basketball", "fitness", "health", "life", "travel",
            "food", "music", "movie", "体育", "足球", "篮球", "健身", "生活", "旅行", "美食", "娱乐");

    private final UserRepository userRepository;
    private final ContentRepository contentRepository;
    private final BehaviorRepository behaviorRepository;
    private final FollowRepository followRepository;

    public DefenseDemoAccountSeeder(UserRepository userRepository,
                                    ContentRepository contentRepository,
                                    BehaviorRepository behaviorRepository,
                                    FollowRepository followRepository) {
        this.userRepository = userRepository;
        this.contentRepository = contentRepository;
        this.behaviorRepository = behaviorRepository;
        this.followRepository = followRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Content> allContents = contentRepository.findAll().stream()
                .filter(content -> content.getId() != null && content.getParentContent() == null)
                .toList();
        if (allContents.isEmpty()) {
            System.out.println("Skipped defense demo accounts: no content available.");
            return;
        }

        User dabian1 = ensureUser(
                "dabian1",
                "@dabian1",
                "答辩主展示账号：偏好人工智能、科技、算法与深度内容。",
                "https://api.dicebear.com/7.x/avataaars/svg?seed=dabian1");
        User dabian2 = ensureUser(
                "dabian2",
                "@dabian2",
                "答辩对比账号：偏好体育、生活、娱乐与短内容。",
                "https://api.dicebear.com/7.x/avataaars/svg?seed=dabian2");

        List<Content> techFirst = prioritize(allContents, TECH_KEYWORDS);
        List<Content> lifestyleFirst = prioritize(allContents, LIFESTYLE_KEYWORDS);

        seedCommenterProfile(dabian1, techFirst);
        seedLikerProfile(dabian2, lifestyleFirst);
        seedFollows(dabian1, techFirst, 8);
        seedFollows(dabian2, lifestyleFirst, 8);

        System.out.println("Defense demo accounts ready: dabian1 / dabian2 (password: password).");
    }

    private User ensureUser(String username, String handle, String bio, String avatarUrl) {
        User user = userRepository.findByUsername(username).orElseGet(User::new);
        user.setUsername(username);
        user.setHandle(handle);
        user.setBio(bio);
        user.setAvatarUrl(avatarUrl);
        user.setPassword(DEMO_PASSWORD);
        user.setRole("USER");
        user.setBanned(false);
        return userRepository.save(user);
    }

    private void seedCommenterProfile(User user, List<Content> contents) {
        seedBehaviors(user, contents, "COMMENT", 35, null);
        seedBehaviors(user, contents, "REPOST", 15, null);
        seedBehaviors(user, contents, "LIKE", 8, null);
        seedBehaviors(user, contents, "VIEW", 25, 20);
    }

    private void seedLikerProfile(User user, List<Content> contents) {
        seedBehaviors(user, contents, "LIKE", 45, null);
        seedBehaviors(user, contents, "VIEW", 20, 4);
        seedBehaviors(user, contents, "COMMENT", 5, null);
        seedBehaviors(user, contents, "REPOST", 5, null);
    }

    private void seedBehaviors(User user, List<Content> contents, String type, int limit, Integer duration) {
        int count = Math.min(limit, contents.size());
        for (int i = 0; i < count; i++) {
            Content content = contents.get(i);
            if (!behaviorRepository.findByUserIdAndContentIdAndType(user.getId(), content.getId(), type).isEmpty()) {
                continue;
            }
            Behavior behavior = new Behavior();
            behavior.setUserId(user.getId());
            behavior.setContentId(content.getId());
            behavior.setType(type);
            behavior.setDuration(duration);
            behaviorRepository.save(behavior);
        }
    }

    private void seedFollows(User user, List<Content> preferredContents, int limit) {
        Set<Long> authorIds = new LinkedHashSet<>();
        for (Content content : preferredContents) {
            if (content.getAuthor() == null || content.getAuthor().getId() == null
                    || content.getAuthor().getId().equals(user.getId())) {
                continue;
            }
            authorIds.add(content.getAuthor().getId());
            if (authorIds.size() >= limit) break;
        }

        for (Long authorId : authorIds) {
            if (followRepository.existsByFollowerIdAndFolloweeId(user.getId(), authorId)) continue;
            Follow follow = new Follow();
            follow.setFollowerId(user.getId());
            follow.setFolloweeId(authorId);
            followRepository.save(follow);
        }
    }

    private List<Content> prioritize(List<Content> contents, List<String> keywords) {
        List<Content> result = new ArrayList<>(contents);
        result.sort(Comparator
                .comparingInt((Content content) -> preferenceScore(content, keywords)).reversed()
                .thenComparing(Content::getId));
        return result;
    }

    private int preferenceScore(Content content, List<String> keywords) {
        String searchable = String.join(" ",
                safe(content.getCategory()),
                safe(content.getTitle()),
                safe(content.getContent())).toLowerCase(Locale.ROOT);
        int score = 0;
        for (String keyword : keywords) {
            if (searchable.contains(keyword.toLowerCase(Locale.ROOT))) {
                score++;
            }
        }
        return score;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
