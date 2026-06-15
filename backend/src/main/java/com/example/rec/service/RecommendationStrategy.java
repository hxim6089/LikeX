package com.example.rec.service;

import com.example.rec.model.Content;
import java.util.List;

/**
 * 推荐策略接口（策略模式）
 *
 * 定义推荐算法的统一接口，系统中有两个实现：
 * 1. HybridRecommendationStrategy — 传统多因子加权打分算法
 * 2. AiRecommendationStrategy — AI 大模型驱动的推荐算法
 *
 * 通过 RecommendationStrategyManager 在运行时动态切换，
 * 管理员可在后台管理页面一键切换全局推荐策略。
 */
public interface RecommendationStrategy {
    /**
     * 对候选内容进行个性化排序
     *
     * @param userId     当前用户 ID（null 则使用默认策略）
     * @param candidates 候选内容池
     * @return 排序后的推荐列表
     */
    List<Content> recommend(Long userId, List<Content> candidates);
}
