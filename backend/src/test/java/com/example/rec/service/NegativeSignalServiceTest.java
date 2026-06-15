package com.example.rec.service;

import com.example.rec.model.NegativeSignal;
import com.example.rec.repository.NegativeSignalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NegativeSignalServiceTest {

    @Mock
    private NegativeSignalRepository negativeSignalRepository;

    @Test
    void doesNotStoreDuplicateNotInterestedSignal() {
        when(negativeSignalRepository.existsByUserIdAndTargetTypeAndTargetIdAndSignalType(
                7L,
                NegativeSignal.TargetType.CONTENT,
                18L,
                NegativeSignal.SignalType.NOT_INTERESTED
        )).thenReturn(true);

        NegativeSignalService service = new NegativeSignalService(negativeSignalRepository);
        service.markNotInterested(7L, 18L);

        verify(negativeSignalRepository, never()).save(any(NegativeSignal.class));
    }
}
