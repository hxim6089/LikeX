# -*- coding: utf-8 -*-
from __future__ import annotations

from pathlib import Path

import build_appendix_core_code_docx as base
from build_appendix_core_code_docx import SectionSpec, extract_block


ROOT = Path(__file__).resolve().parents[1]
base.OUT_PATH = ROOT / "论文相关" / "附录_程序源代码核心代码_精选版.docx"


def build_selected_sections() -> list[SectionSpec]:
    hybrid = "backend/src/main/java/com/example/rec/service/HybridRecommendationStrategy.java"
    profile = "backend/src/main/java/com/example/rec/service/UserBehaviorProfileService.java"
    ai = "backend/src/main/java/com/example/rec/service/AiRecommendationStrategy.java"
    ad = "backend/src/main/java/com/example/rec/service/AdService.java"
    behavior = "backend/src/main/java/com/example/rec/service/BehaviorService.java"

    return [
        SectionSpec(
            "A.1 推荐排序评分核心代码",
            "仅保留推荐算法中最能体现排序依据的评分方法，用于说明热度、时间衰减、兴趣匹配、协同过滤和行为惩罚如何共同影响内容排序。",
            [
                extract_block(hybrid, "private double calculateScore(Content content, List<String> userInterests,"),
            ],
        ),
        SectionSpec(
            "A.2 用户画像与动态权重核心代码",
            "保留画像构建和动态权重计算两个关键方法，用于说明系统如何根据用户历史行为调整推荐策略。",
            [
                extract_block(profile, "public BehaviorProfile buildProfile(Long userId)"),
                extract_block(profile, "public DynamicWeights computeDynamicWeights(Long userId)"),
            ],
        ),
        SectionSpec(
            "A.3 AI 推荐与降级核心代码",
            "保留 AI 调用和不可用时的降级排序代码，用于说明推荐模块的智能排序与可用性保障。",
            [
                extract_block(ai, "private AiRankingResult callAiRankingFull(BehaviorProfile profile, List<Content> candidates)"),
                extract_block(ai, "private List<Content> fallbackSort(List<Content> candidates)"),
            ],
        ),
        SectionSpec(
            "A.4 广告分发核心代码",
            "保留广告相关性匹配方法，用于说明广告如何依据用户画像标签插入信息流。",
            [
                extract_block(ad, "public List<Map<String, Object>> getRelevantAds(Long userId, int count)"),
            ],
        ),
        SectionSpec(
            "A.5 用户行为反馈核心代码",
            "保留浏览行为记录方法，用于说明推荐系统如何采集用户反馈并形成后续推荐依据。",
            [
                extract_block(behavior, "public void recordView(Long userId, Long contentId, Integer duration)"),
            ],
        ),
    ]


if __name__ == "__main__":
    base.build_sections = build_selected_sections
    print(base.build_docx())
