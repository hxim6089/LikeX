package com.example.rec.dto;

/**
 * 推荐管道统计数据
 * 用于漏斗图可视化，展示候选内容在各阶段的数量变化
 */
public class PipelineStats {
    
    private int totalCandidates;      // 全量候选
    private int afterNegativeFilter;  // 负信号过滤后
    private int inNetworkCount;       // In-Network 候选数
    private int outNetworkCount;      // Out-of-Network 候选数
    private int afterScoring;         // 评分排序后（Top N）
    private int afterDiversity;       // 多样性调整后（最终返回）

    public PipelineStats() {}

    public int getTotalCandidates() { return totalCandidates; }
    public void setTotalCandidates(int totalCandidates) { this.totalCandidates = totalCandidates; }

    public int getAfterNegativeFilter() { return afterNegativeFilter; }
    public void setAfterNegativeFilter(int afterNegativeFilter) { this.afterNegativeFilter = afterNegativeFilter; }

    public int getInNetworkCount() { return inNetworkCount; }
    public void setInNetworkCount(int inNetworkCount) { this.inNetworkCount = inNetworkCount; }

    public int getOutNetworkCount() { return outNetworkCount; }
    public void setOutNetworkCount(int outNetworkCount) { this.outNetworkCount = outNetworkCount; }

    public int getAfterScoring() { return afterScoring; }
    public void setAfterScoring(int afterScoring) { this.afterScoring = afterScoring; }

    public int getAfterDiversity() { return afterDiversity; }
    public void setAfterDiversity(int afterDiversity) { this.afterDiversity = afterDiversity; }
}
