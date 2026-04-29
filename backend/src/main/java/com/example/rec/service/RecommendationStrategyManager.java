package com.example.rec.service;

import com.example.rec.dto.ContentWithScore;
import com.example.rec.model.Content;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 推荐策略管理器
 *
 * 持有传统算法和 AI 推荐两套策略，通过 Admin 面板全局切换。
 * 重启后恢复默认策略（传统算法）。
 */
@Service
public class RecommendationStrategyManager {

    public static final String STRATEGY_TRADITIONAL = "traditional";
    public static final String STRATEGY_AI = "ai";

    private final HybridRecommendationStrategy traditionalStrategy;
    private final AiRecommendationStrategy aiStrategy;
    private final AtomicReference<String> activeStrategy = new AtomicReference<>(STRATEGY_TRADITIONAL);

    public RecommendationStrategyManager(HybridRecommendationStrategy traditionalStrategy,
                                          AiRecommendationStrategy aiStrategy) {
        this.traditionalStrategy = traditionalStrategy;
        this.aiStrategy = aiStrategy;
    }

    public String getCurrentStrategyType() {
        return activeStrategy.get();
    }

    public void switchStrategy(String type) {
        if (STRATEGY_TRADITIONAL.equals(type) || STRATEGY_AI.equals(type)) {
            activeStrategy.set(type);
        } else {
            throw new IllegalArgumentException("Unknown strategy: " + type + ". Use 'traditional' or 'ai'.");
        }
    }

    public RecommendationStrategy getActiveStrategy() {
        return STRATEGY_AI.equals(activeStrategy.get()) ? aiStrategy : traditionalStrategy;
    }

    /**
     * 统一的 recommend 方法，代理到当前活跃策略
     */
    public List<Content> recommend(Long userId, List<Content> candidates) {
        return getActiveStrategy().recommend(userId, candidates);
    }

    /**
     * 统一的 recommendWithScore 方法，代理到当前活跃策略
     */
    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates) {
        if (STRATEGY_AI.equals(activeStrategy.get())) {
            return aiStrategy.recommendWithScore(userId, candidates);
        }
        return traditionalStrategy.recommendWithScore(userId, candidates);
    }

    /**
     * 带自定义权重的推荐（仅传统策略支持）
     */
    public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates, Map<String, Double> weights) {
        if (STRATEGY_AI.equals(activeStrategy.get())) {
            return aiStrategy.recommendWithScore(userId, candidates);
        }
        return traditionalStrategy.recommendWithScore(userId, candidates, weights);
    }

    /**
     * 直接获取传统策略（用于对比页面等需要固定传统策略的场景）
     */
    public HybridRecommendationStrategy getTraditionalStrategy() {
        return traditionalStrategy;
    }

    /**
     * 检查 Ollama AI 服务是否可用
     */
    public boolean isAiAvailable() {
        return aiStrategy.isOllamaAvailable();
    }

    /**
     * 获取策略描述信息
     */
    public Map<String, Object> getStrategyInfo() {
        return Map.of(
            "current", activeStrategy.get(),
            "strategies", List.of(
                Map.of(
                    "type", STRATEGY_TRADITIONAL,
                    "name", "传统多因子算法",
                    "description", "基于用户行为画像的多因子加权打分：互动热度、TF-IDF 内容相似度、话题亲和度、作者亲密度、内容深度匹配、新鲜度偏好 + 动态权重 + 作者多样性惩罚",
                    "active", STRATEGY_TRADITIONAL.equals(activeStrategy.get())
                ),
                Map.of(
                    "type", STRATEGY_AI,
                    "name", "AI 大模型推荐",
                    "description", "Ollama Qwen 8B 大语言模型驱动，根据用户行为画像理解用户兴趣，智能分析候选内容语义并排序，生成个性化推荐理由",
                    "active", STRATEGY_AI.equals(activeStrategy.get())
                )
            ),
            "aiAvailable", isAiAvailable()
        );
    }
}
