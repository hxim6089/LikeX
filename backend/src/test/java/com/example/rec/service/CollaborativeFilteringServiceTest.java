package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollaborativeFilteringServiceTest {

    @Mock private BehaviorRepository behaviorRepository;
    @Mock private ContentRepository contentRepository;

    @Test
    void skipDoesNotBecomePositiveCollaborativeSignal() {
        Behavior skip = new Behavior();
        skip.setUserId(7L);
        skip.setContentId(18L);
        skip.setType("SKIP");
        skip.setCreatedAt(LocalDateTime.now());
        when(behaviorRepository.findByUserId(7L)).thenReturn(List.of(skip));

        CollaborativeFilteringService service =
                new CollaborativeFilteringService(behaviorRepository, contentRepository);

        Map<Long, Double> vector = service.getUserBehaviorVector(7L);

        assertEquals(0.0, vector.get(18L));
    }
}
