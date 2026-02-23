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
        NegativeSignal signal = new NegativeSignal();
        signal.setUserId(userId);
        signal.setTargetType(NegativeSignal.TargetType.CONTENT);
        signal.setTargetId(contentId);
        signal.setSignalType(NegativeSignal.SignalType.NOT_INTERESTED);
        negativeSignalRepository.save(signal);
    }
    
    /**
     * 屏蔽作者
     */
    public void blockAuthor(Long userId, Long authorId) {
        NegativeSignal signal = new NegativeSignal();
        signal.setUserId(userId);
        signal.setTargetType(NegativeSignal.TargetType.AUTHOR);
        signal.setTargetId(authorId);
        signal.setSignalType(NegativeSignal.SignalType.BLOCK);
        negativeSignalRepository.save(signal);
    }
    
    /**
     * 静音作者
     */
    public void muteAuthor(Long userId, Long authorId) {
        NegativeSignal signal = new NegativeSignal();
        signal.setUserId(userId);
        signal.setTargetType(NegativeSignal.TargetType.AUTHOR);
        signal.setTargetId(authorId);
        signal.setSignalType(NegativeSignal.SignalType.MUTE);
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
