package com.example.rec.service;

import com.example.rec.model.Behavior;
import com.example.rec.model.Content;
import com.example.rec.repository.BehaviorRepository;
import com.example.rec.repository.ContentRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于用户的协同过滤推荐服务 (User-Based Collaborative Filtering)
 *
 * 【算法原理】
 * "和你相似的用户也喜欢这些内容"——找到与目标用户行为模式相似的用户，
 * 将他们喜欢但目标用户未看过的内容推荐出来。
 *
 * 【实现细节】
 * 1. 构建用户行为向量：每个用户对每条内容的加权行为分数
 *    - 行为权重：REPOST(3.0) > COMMENT(2.0) > LIKE(1.0) > VIEW(0.1)
 *    - 时间衰减：e^(-0.05 × 天数)，近期行为权重更高
 * 2. 计算用户间的加权余弦相似度，找到 Top-K 相似用户
 * 3. 从相似用户的行为中提取目标用户未互动过的内容
 * 4. 按 "相似度 × 行为强度" 加权打分，排序输出
 *
 * 【在推荐管道中的位置】
 * 协同过滤的输出作为 HybridRecommendationStrategy 的一个加分因子，
 * 当帖子被协同过滤推荐时，会获得额外的 wCollaborative 加成分。
 */
@Service
public class CollaborativeFilteringService {

    private final BehaviorRepository behaviorRepository;
    private final ContentRepository contentRepository;

    // 行为权重配置
    private static final double BEHAVIOR_WEIGHT_LIKE = 1.0;
    private static final double BEHAVIOR_WEIGHT_COMMENT = 2.0;
    private static final double BEHAVIOR_WEIGHT_REPOST = 3.0;
    private static final double BEHAVIOR_WEIGHT_VIEW = 0.1;

    // 时间衰减参数 λ（每天衰减约 5%）
    private static final double TIME_DECAY_LAMBDA = 0.05;

    public CollaborativeFilteringService(BehaviorRepository behaviorRepository,
                                         ContentRepository contentRepository) {
        this.behaviorRepository = behaviorRepository;
        this.contentRepository = contentRepository;
    }

    /**
     * 获取协同过滤推荐结果 (Phase 28 增强版)
     * 使用多行为加权 + 时间衰减相似度
     */
    public List<Content> getCollaborativeRecommendations(Long userId, int limit) {
        if (userId == null || limit <= 0) {
            return Collections.emptyList();
        }

        // 1. 获取目标用户的加权行为向量
        Map<Long, Double> userBehaviorVector = getUserBehaviorVector(userId);
        if (userBehaviorVector.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 找到相似用户
        List<UserSimilarity> similarUsers = findSimilarUsers(userId, 10);
        if (similarUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 从相似用户的行为中获取推荐内容
        Set<Long> userInteractedIds = userBehaviorVector.keySet();
        Map<Long, Double> contentScores = new HashMap<>();

        for (UserSimilarity sim : similarUsers) {
            Map<Long, Double> theirVector = getUserBehaviorVector(sim.getUserId());
            
            for (Map.Entry<Long, Double> entry : theirVector.entrySet()) {
                Long contentId = entry.getKey();
                if (!userInteractedIds.contains(contentId)) {
                    // 按相似度 × 行为强度加权
                    double score = sim.getSimilarity() * entry.getValue();
                    contentScores.merge(contentId, score, Double::sum);
                }
            }
        }

        if (contentScores.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 获取内容并按分数排序
        List<Content> contents = contentRepository.findAllById(contentScores.keySet());
        contents.sort((a, b) -> {
            Double scoreA = contentScores.getOrDefault(a.getId(), 0.0);
            Double scoreB = contentScores.getOrDefault(b.getId(), 0.0);
            return Double.compare(scoreB, scoreA);
        });

        return contents.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Phase 28: 构建用户的加权行为向量
     * 
     * 每个内容的权重 = 行为类型权重 × 时间衰减因子
     * 时间衰减: weight × e^(-λ × daysSinceBehavior)
     */
    public Map<Long, Double> getUserBehaviorVector(Long userId) {
        List<Behavior> behaviors = behaviorRepository.findByUserId(userId);
        if (behaviors.isEmpty()) return Collections.emptyMap();

        Map<Long, Double> vector = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (Behavior b : behaviors) {
            double behaviorWeight = getBehaviorWeight(b.getType());
            
            // 计算时间衰减
            double daysSince = 1.0;
            if (b.getCreatedAt() != null) {
                daysSince = Math.max(1, Duration.between(b.getCreatedAt(), now).toDays());
            }
            double timeDecay = Math.exp(-TIME_DECAY_LAMBDA * daysSince);
            
            double weightedScore = behaviorWeight * timeDecay;
            vector.merge(b.getContentId(), weightedScore, Double::sum);
        }

        return vector;
    }

    /**
     * 获取行为类型权重
     */
    private double getBehaviorWeight(String type) {
        if (type == null) return 0.0;
        switch (type.toUpperCase()) {
            case "LIKE": return BEHAVIOR_WEIGHT_LIKE;
            case "COMMENT": return BEHAVIOR_WEIGHT_COMMENT;
            case "REPOST": return BEHAVIOR_WEIGHT_REPOST;
            case "QUOTE": return BEHAVIOR_WEIGHT_REPOST;
            case "VIEW": return BEHAVIOR_WEIGHT_VIEW;
            case "SKIP":
            case "DISLIKE":
                return 0.0;
            default: return 0.5;
        }
    }

    /**
     * Phase 28: 使用加权余弦相似度找相似用户
     */
    public List<UserSimilarity> findSimilarUsers(Long userId, int topK) {
        Map<Long, Double> userVector = getUserBehaviorVector(userId);
        if (userVector.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有有行为的用户
        List<Behavior> allBehaviors = behaviorRepository.findAll();
        
        // 按用户分组构建行为向量
        Map<Long, List<Behavior>> userBehaviorMap = allBehaviors.stream()
                .filter(b -> !b.getUserId().equals(userId))
                .collect(Collectors.groupingBy(Behavior::getUserId));

        List<UserSimilarity> similarities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Long, List<Behavior>> entry : userBehaviorMap.entrySet()) {
            Long otherUserId = entry.getKey();
            
            // 构建对方的行为向量
            Map<Long, Double> otherVector = new HashMap<>();
            for (Behavior b : entry.getValue()) {
                double behaviorWeight = getBehaviorWeight(b.getType());
                double daysSince = 1.0;
                if (b.getCreatedAt() != null) {
                    daysSince = Math.max(1, Duration.between(b.getCreatedAt(), now).toDays());
                }
                double timeDecay = Math.exp(-TIME_DECAY_LAMBDA * daysSince);
                otherVector.merge(b.getContentId(), behaviorWeight * timeDecay, Double::sum);
            }
            
            // 计算加权余弦相似度
            double similarity = weightedCosineSimilarity(userVector, otherVector);
            
            if (similarity > 0) {
                // 计算共同互动数
                Set<Long> commonItems = new HashSet<>(userVector.keySet());
                commonItems.retainAll(otherVector.keySet());
                
                similarities.add(new UserSimilarity(otherUserId, similarity, commonItems.size()));
            }
        }

        similarities.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        return similarities.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * Phase 28: 加权余弦相似度
     * 考虑行为强度的余弦相似度计算
     */
    private double weightedCosineSimilarity(Map<Long, Double> vecA, Map<Long, Double> vecB) {
        if (vecA.isEmpty() || vecB.isEmpty()) return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Long, Double> entry : vecA.entrySet()) {
            double valA = entry.getValue();
            normA += valA * valA;
            Double valB = vecB.get(entry.getKey());
            if (valB != null) {
                dotProduct += valA * valB;
            }
        }

        for (double valB : vecB.values()) {
            normB += valB * valB;
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 用户相似度数据类
     */
    public static class UserSimilarity {
        private final Long userId;
        private final Double similarity;
        private final Integer commonLikes;

        public UserSimilarity(Long userId, Double similarity, Integer commonLikes) {
            this.userId = userId;
            this.similarity = similarity;
            this.commonLikes = commonLikes;
        }

        public Long getUserId() { return userId; }
        public Double getSimilarity() { return similarity; }
        public Integer getCommonLikes() { return commonLikes; }
    }
}
