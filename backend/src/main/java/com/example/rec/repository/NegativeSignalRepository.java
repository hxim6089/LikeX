package com.example.rec.repository;

import com.example.rec.model.NegativeSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NegativeSignalRepository extends JpaRepository<NegativeSignal, Long> {
    
    List<NegativeSignal> findByUserId(Long userId);
    
    List<NegativeSignal> findByUserIdAndTargetType(Long userId, NegativeSignal.TargetType targetType);
    
    List<NegativeSignal> findByUserIdAndSignalType(Long userId, NegativeSignal.SignalType signalType);
    
    Optional<NegativeSignal> findByUserIdAndTargetTypeAndTargetId(Long userId, NegativeSignal.TargetType targetType, Long targetId);
    
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, NegativeSignal.TargetType targetType, Long targetId);
}
