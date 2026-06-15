package com.example.rec.service;

import com.example.rec.model.Content;
import com.example.rec.model.User;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import com.example.rec.repository.NegativeSignalRepository;
import com.example.rec.repository.NotificationRepository;
import com.example.rec.repository.TagRepository;
import com.example.rec.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentServiceBehaviorTest {

    @Mock private ContentRepository contentRepository;
    @Mock private RelationService relationService;
    @Mock private NotificationService notificationService;
    @Mock private TagRepository tagRepository;
    @Mock private BehaviorRepository behaviorRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NegativeSignalRepository negativeSignalRepository;
    @Mock private AiTaggingService aiTaggingService;
    @Mock private BehaviorService behaviorService;

    private ContentService contentService;
    private Content original;

    @BeforeEach
    void setUp() {
        contentService = new ContentService(
                contentRepository, relationService, notificationService, tagRepository,
                behaviorRepository, userRepository, notificationRepository,
                negativeSignalRepository, aiTaggingService, behaviorService);

        User author = new User();
        author.setId(3L);
        original = new Content();
        original.setId(11L);
        original.setAuthor(author);
        original.setCategory("Tech");
        original.setCommentCount(0);
        original.setRepostCount(0);

        User actor = new User();
        actor.setId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(actor));
        when(contentRepository.findById(11L)).thenReturn(Optional.of(original));
    }

    @Test
    void commentRecordsBehaviorAgainstParentContent() {
        contentService.addComment(11L, 7L, "reply");

        verify(behaviorService).recordInteraction(7L, 11L, "COMMENT");
    }

    @Test
    void repostRecordsBehaviorAgainstOriginalContent() {
        contentService.repost(11L, 7L);

        verify(behaviorService).recordInteraction(7L, 11L, "REPOST");
    }

    @Test
    void quoteRecordsBehaviorAgainstOriginalContent() {
        contentService.quote(11L, 7L, "quote");

        verify(behaviorService).recordInteraction(7L, 11L, "QUOTE");
    }
}
