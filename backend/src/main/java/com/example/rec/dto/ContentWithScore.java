package com.example.rec.dto;

import com.example.rec.model.Content;

/**
 * 带评分详情的内容 DTO
 * 用于 debug 模式下返回评分信息
 */
public class ContentWithScore {
    
    private Content content;
    private ScoreBreakdown scoreBreakdown;
    private int rank; // 排名位置
    
    public ContentWithScore() {}
    
    public ContentWithScore(Content content, ScoreBreakdown scoreBreakdown, int rank) {
        this.content = content;
        this.scoreBreakdown = scoreBreakdown;
        this.rank = rank;
    }

    public Content getContent() { return content; }
    public void setContent(Content content) { this.content = content; }

    public ScoreBreakdown getScoreBreakdown() { return scoreBreakdown; }
    public void setScoreBreakdown(ScoreBreakdown scoreBreakdown) { this.scoreBreakdown = scoreBreakdown; }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
}
