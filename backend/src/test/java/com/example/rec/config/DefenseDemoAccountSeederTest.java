package com.example.rec.config;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.model.Follow;
import com.example.rec.model.User;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.FollowRepository;
import com.example.rec.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefenseDemoAccountSeederTest {

    @Mock private UserRepository userRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private BehaviorRepository behaviorRepository;
    @Mock private FollowRepository followRepository;

    @Test
    void createsDistinctActiveDefenseAccountsWithoutDuplicatingDataOnRerun() throws Exception {
        Map<String, User> users = new HashMap<>();
        List<Behavior> savedBehaviors = new ArrayList<>();
        Set<String> savedFollows = new HashSet<>();

        when(userRepository.findByUsername(any())).thenAnswer(invocation ->
                Optional.ofNullable(users.get(invocation.getArgument(0))));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId((long) users.size() + 101);
            }
            users.put(user.getUsername(), user);
            return user;
        });

        when(contentRepository.findAll()).thenReturn(buildContents());
        when(behaviorRepository.findByUserIdAndContentIdAndType(any(), any(), any()))
                .thenAnswer(invocation -> {
                    Long userId = invocation.getArgument(0);
                    Long contentId = invocation.getArgument(1);
                    String type = invocation.getArgument(2);
                    return savedBehaviors.stream()
                            .filter(b -> userId.equals(b.getUserId())
                                    && contentId.equals(b.getContentId())
                                    && type.equals(b.getType()))
                            .toList();
                });
        when(behaviorRepository.save(any(Behavior.class))).thenAnswer(invocation -> {
            Behavior behavior = invocation.getArgument(0);
            savedBehaviors.add(behavior);
            return behavior;
        });

        when(followRepository.existsByFollowerIdAndFolloweeId(any(), any()))
                .thenAnswer(invocation -> savedFollows.contains(
                        invocation.getArgument(0) + ":" + invocation.getArgument(1)));
        when(followRepository.save(any(Follow.class))).thenAnswer(invocation -> {
            Follow follow = invocation.getArgument(0);
            savedFollows.add(follow.getFollowerId() + ":" + follow.getFolloweeId());
            return follow;
        });

        DefenseDemoAccountSeeder seeder = new DefenseDemoAccountSeeder(
                userRepository, contentRepository, behaviorRepository, followRepository);

        seeder.run();
        int behaviorCountAfterFirstRun = savedBehaviors.size();
        int followCountAfterFirstRun = savedFollows.size();
        seeder.run();

        User dabian1 = users.get("dabian1");
        User dabian2 = users.get("dabian2");
        assertEquals("password", dabian1.getPassword());
        assertEquals("password", dabian2.getPassword());
        assertEquals("USER", dabian1.getRole());
        assertEquals("USER", dabian2.getRole());

        List<Behavior> firstProfile = behaviorsFor(savedBehaviors, dabian1.getId());
        List<Behavior> secondProfile = behaviorsFor(savedBehaviors, dabian2.getId());
        assertTrue(firstProfile.size() >= 60);
        assertTrue(secondProfile.size() >= 60);
        assertTrue(countType(firstProfile, "COMMENT") > countType(firstProfile, "LIKE"));
        assertTrue(countType(secondProfile, "LIKE") > countType(secondProfile, "COMMENT"));

        assertEquals(behaviorCountAfterFirstRun, savedBehaviors.size());
        assertEquals(followCountAfterFirstRun, savedFollows.size());
    }

    private List<Behavior> behaviorsFor(List<Behavior> behaviors, Long userId) {
        return behaviors.stream().filter(b -> userId.equals(b.getUserId())).toList();
    }

    private long countType(List<Behavior> behaviors, String type) {
        return behaviors.stream().filter(b -> type.equals(b.getType())).count();
    }

    private List<Content> buildContents() {
        List<Content> contents = new ArrayList<>();
        for (int i = 1; i <= 45; i++) {
            contents.add(content((long) i, "Tech", "AI machine learning Java recommendation " + i, 1000L + i));
        }
        for (int i = 46; i <= 90; i++) {
            contents.add(content((long) i, "Sports", "sports fitness football lifestyle " + i, 1000L + i));
        }
        return contents;
    }

    private Content content(Long id, String category, String text, Long authorId) {
        User author = new User();
        author.setId(authorId);
        author.setUsername("author" + authorId);

        Content content = new Content();
        content.setId(id);
        content.setCategory(category);
        content.setContent(text);
        content.setAuthor(author);
        return content;
    }
}
