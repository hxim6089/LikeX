package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BehaviorServiceTest {

    @Mock
    private BehaviorRepository behaviorRepository;
    @Mock
    private ContentRepository contentRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private RecommendationStrategyManager strategyManager;

    @Test
    void recordsStrongInteractionAndInvalidatesRecommendationCache() {
        BehaviorService service = new BehaviorService(
                behaviorRepository, contentRepository, notificationService, strategyManager);

        service.recordInteraction(7L, 18L, "COMMENT");

        ArgumentCaptor<Behavior> captor = ArgumentCaptor.forClass(Behavior.class);
        verify(behaviorRepository).save(captor.capture());
        assertEquals(7L, captor.getValue().getUserId());
        assertEquals(18L, captor.getValue().getContentId());
        assertEquals("COMMENT", captor.getValue().getType());
        verify(strategyManager).invalidateAiCache(7L);
    }

    @Test
    void rejectsUnsupportedInteractionType() {
        BehaviorService service = new BehaviorService(
                behaviorRepository, contentRepository, notificationService, strategyManager);

        assertThrows(IllegalArgumentException.class,
                () -> service.recordInteraction(7L, 18L, "UNKNOWN"));
    }
}
