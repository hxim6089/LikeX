package com.example.rec.service;

import com.example.rec.model.NegativeSignal;
import com.example.rec.repository.NegativeSignalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NegativeSignalService {
    
    private final NegativeSignalRepository negativeSignalRepository;
    
    public NegativeSignalService(NegativeSignalRepository negativeSignalRepository) {
        this.negativeSignalRepository = negativeSignalRepository;
    }
    
    /**
     * 记录"不感兴趣"信号
     */
    public void markNotInterested(Long userId, Long contentId) {
        saveIfAbsent(userId, NegativeSignal.TargetType.CONTENT, contentId, NegativeSignal.SignalType.NOT_INTERESTED);
    }
    
    /**
     * 屏蔽作者
     */
    public void blockAuthor(Long userId, Long authorId) {
        saveIfAbsent(userId, NegativeSignal.TargetType.AUTHOR, authorId, NegativeSignal.SignalType.BLOCK);
    }
    
    /**
     * 静音作者
     */
    public void muteAuthor(Long userId, Long authorId) {
        saveIfAbsent(userId, NegativeSignal.TargetType.AUTHOR, authorId, NegativeSignal.SignalType.MUTE);
    }

    private void saveIfAbsent(Long userId, NegativeSignal.TargetType targetType, Long targetId,
                              NegativeSignal.SignalType signalType) {
        if (userId == null || targetId == null) return;
        if (negativeSignalRepository.existsByUserIdAndTargetTypeAndTargetIdAndSignalType(
                userId, targetType, targetId, signalType)) {
            return;
        }

        NegativeSignal signal = new NegativeSignal();
        signal.setUserId(userId);
        signal.setTargetType(targetType);
        signal.setTargetId(targetId);
        signal.setSignalType(signalType);
        negativeSignalRepository.save(signal);
    }
    
    /**
     * 取消屏蔽
     */
    @Transactional
    public void unblockAuthor(Long userId, Long authorId) {
        negativeSignalRepository.deleteByUserIdAndTargetTypeAndTargetId(userId, NegativeSignal.TargetType.AUTHOR, authorId);
    }
    
    /**
     * 获取用户屏蔽的作者ID列表
     */
    public Set<Long> getBlockedAuthorIds(Long userId) {
        return negativeSignalRepository.findByUserIdAndTargetType(userId, NegativeSignal.TargetType.AUTHOR)
                .stream()
                .filter(s -> s.getSignalType() == NegativeSignal.SignalType.BLOCK || s.getSignalType() == NegativeSignal.SignalType.MUTE)
                .map(NegativeSignal::getTargetId)
                .collect(Collectors.toSet());
    }
    
    /**
     * 获取用户标记为"不感兴趣"的内容ID列表
     */
    public Set<Long> getHiddenContentIds(Long userId) {
        return negativeSignalRepository.findByUserIdAndTargetType(userId, NegativeSignal.TargetType.CONTENT)
                .stream()
                .map(NegativeSignal::getTargetId)
                .collect(Collectors.toSet());
    }
}
