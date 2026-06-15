# -*- coding: utf-8 -*-
from __future__ import annotations

from pathlib import Path

import build_appendix_core_code_docx as base
from build_appendix_core_code_docx import SectionSpec, extract_block


ROOT = Path(__file__).resolve().parents[1]
base.OUT_PATH = ROOT / "论文相关" / "附录_程序源代码核心代码_精简版.docx"


def build_slim_sections() -> list[SectionSpec]:
    hybrid = "backend/src/main/java/com/example/rec/service/HybridRecommendationStrategy.java"
    profile = "backend/src/main/java/com/example/rec/service/UserBehaviorProfileService.java"
    ai = "backend/src/main/java/com/example/rec/service/AiRecommendationStrategy.java"
    ad = "backend/src/main/java/com/example/rec/service/AdService.java"
    behavior = "backend/src/main/java/com/example/rec/service/BehaviorService.java"

    return [
        SectionSpec(
            "A.1 推荐排序模块核心代码",
            "保留推荐排序主流程和评分计算代码，用于说明系统如何对候选内容进行个性化排序。",
            [
                extract_block(hybrid, "public List<ContentWithScore> recommendWithScore(Long userId, List<Content> candidates, Map<String, Double> manualWeights)"),
                extract_block(hybrid, "private ScoredContentWithDetails calculateScoreWithDetails(Content content,"),
            ],
        ),
        SectionSpec(
            "A.2 用户画像与动态权重核心代码",
            "保留用户画像数据结构、画像生成流程和动态权重计算代码，用于说明推荐权重如何根据用户行为变化。",
            [
                extract_block(profile, "public static class BehaviorProfile"),
                extract_block(profile, "public BehaviorProfile buildProfile(Long userId)"),
                extract_block(profile, "public static class DynamicWeights"),
                extract_block(profile, "public DynamicWeights computeDynamicWeights(Long userId)"),
            ],
        ),
        SectionSpec(
            "A.3 AI 推荐与降级核心代码",
            "保留提示词构造、AI 排序调用和降级排序代码，用于说明 AI 推荐模块在可用和不可用两种情况下的处理方式。",
            [
                extract_block(ai, "private String buildPrompt(BehaviorProfile profile, List<Content> candidates)"),
                extract_block(ai, "private AiRankingResult callAiRankingFull(BehaviorProfile profile, List<Content> candidates)"),
                extract_block(ai, "private List<Content> fallbackSort(List<Content> candidates)"),
            ],
        ),
        SectionSpec(
            "A.4 广告分发核心代码",
            "保留广告相关性匹配和展示、点击记录代码，用于说明广告如何与用户画像及信息流分发结合。",
            [
                extract_block(ad, "public List<Map<String, Object>> getRelevantAds(Long userId, int count)"),
                extract_block(ad, "public void recordImpression(Long adId, Long userId)"),
                extract_block(ad, "public void recordClick(Long adId)"),
            ],
        ),
        SectionSpec(
            "A.5 用户行为记录核心代码",
            "保留浏览和点赞两类代表性行为记录代码，用于说明推荐系统的数据反馈来源。",
            [
                extract_block(behavior, "public void recordView(Long userId, Long contentId, Integer duration)"),
                extract_block(behavior, "public void likeContent(Long userId, Long contentId)"),
            ],
        ),
    ]


if __name__ == "__main__":
    base.build_sections = build_slim_sections
    print(base.build_docx())
