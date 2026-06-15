<template>
  <div class="score-panel" v-if="visible">
    <!-- 推荐理由标签 -->
    <div class="recommend-reasons" v-if="score.recommendReasons && score.recommendReasons.length">
      <span class="reason-tag" v-for="(reason, i) in score.recommendReasons" :key="i">{{ reason }}</span>
    </div>

    <div class="score-header" @click="expanded = !expanded">
      <span class="rank-badge">#{{ rank }}</span>
      <span class="score-value">推荐评分: {{ score.finalScore }}</span>
      <span class="stage-badge" :class="stageClass">{{ stageLabel }}</span>
      <el-icon class="expand-icon" :class="{ expanded }">
        <ArrowDown />
      </el-icon>
    </div>
    
    <transition name="slide">
      <div v-if="expanded" class="score-details">
        <!-- 用户画像摘要 -->
        <div class="profile-summary" v-if="score.profileSummary">
          <span class="summary-label">行为画像</span>
          <span class="summary-text">{{ score.profileSummary }}</span>
        </div>

        <!-- 基础互动分 -->
        <div class="score-item">
          <span class="label">基础互动分</span>
          <span class="value">{{ score.baseEngagement }}</span>
        </div>
        <div class="score-sub">
          <span>点赞 {{ score.likeCount }} | 评论 {{ score.commentCount }} | 转发 {{ score.repostCount }}</span>
        </div>
        
        <!-- 互动率 -->
        <div class="score-item" v-if="score.engagementRate > 0">
          <span class="label">互动率</span>
          <span class="value">{{ (score.engagementRate * 100).toFixed(1) }}%</span>
        </div>

        <!-- 话题亲和度 -->
        <div class="score-item boost" v-if="score.topicAffinityBoost > 0">
          <span class="label">话题亲和度</span>
          <span class="value">+{{ score.topicAffinityBoost }}</span>
        </div>

        <!-- 作者亲密度 -->
        <div class="score-item boost" v-if="score.authorAffinityBoost > 0">
          <span class="label">作者亲密度</span>
          <span class="value">+{{ score.authorAffinityBoost }}</span>
        </div>

        <!-- 内容深度匹配 -->
        <div class="score-item boost" v-if="score.depthMatchBoost > 0">
          <span class="label">内容深度匹配</span>
          <span class="value">+{{ score.depthMatchBoost }}</span>
        </div>

        <!-- 新鲜度匹配 -->
        <div class="score-item boost" v-if="score.freshnessBoost > 0">
          <span class="label">新鲜度匹配</span>
          <span class="value">+{{ score.freshnessBoost }}</span>
        </div>
        
        <!-- 时间衰减 -->
        <div class="score-item">
          <span class="label">时间衰减</span>
          <span class="value">&times;{{ score.timeDecayFactor }} ({{ score.hoursAgo }}h)</span>
        </div>
        
        <!-- 热门话题加成 -->
        <div class="score-item boost" v-if="score.trendingBoost > 0">
          <span class="label">热门话题加成</span>
          <span class="value">+{{ score.trendingBoost }}</span>
        </div>
        
        <!-- 兴趣标签匹配 -->
        <div class="score-item boost" v-if="score.personalizationBoost > 0">
          <span class="label">兴趣标签匹配</span>
          <span class="value">+{{ score.personalizationBoost }}</span>
        </div>
        <div class="matched-tags" v-if="score.matchedTags">
          {{ score.matchedTags }}
        </div>

        <!-- TF-IDF 相似度 -->
        <div class="score-item boost" v-if="score.contentSimilarityBoost > 0">
          <span class="label">内容相似度</span>
          <span class="value">+{{ score.contentSimilarityBoost }}</span>
        </div>
        
        <!-- In-Network 标识 -->
        <div class="score-item" v-if="score.inNetwork">
          <span class="label">关注来源</span>
          <span class="value boost-text">&times;1.5 加成</span>
        </div>
        
        <!-- 探索因子 -->
        <div class="score-item">
          <span class="label">探索因子</span>
          <span class="value">+{{ score.jitter }}</span>
        </div>

        <!-- 动态权重展示 -->
        <div class="dynamic-weights" v-if="score.dynamicWeights" @click.stop>
          <div class="weights-header" @click="weightsExpanded = !weightsExpanded">
            <span class="weights-label">当前动态权重</span>
            <el-icon class="expand-icon" :class="{ expanded: weightsExpanded }"><ArrowDown /></el-icon>
          </div>
          <div v-if="weightsExpanded" class="weights-grid">
            <div class="weight-item" v-for="(val, key) in score.dynamicWeights" :key="key">
              <span class="w-key">{{ weightLabel(key) }}</span>
              <span class="w-val">{{ val }}</span>
            </div>
          </div>
        </div>
        
        <!-- 最终评分 -->
        <div class="score-final">
          <span class="label">最终评分</span>
          <span class="value">{{ score.finalScore }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  score: { type: Object, default: () => ({}) },
  rank: { type: Number, default: 0 },
  visible: { type: Boolean, default: false }
})

const expanded = ref(false)
const weightsExpanded = ref(false)

const stageClass = computed(() => {
  const s = props.score?.userStage
  if (s === 'ACTIVE') return 'stage-active'
  if (s === 'BEGINNER') return 'stage-beginner'
  return 'stage-cold'
})

const stageLabel = computed(() => {
  const s = props.score?.userStage
  if (s === 'ACTIVE') return '活跃用户'
  if (s === 'BEGINNER') return '初级用户'
  return '冷启动'
})

const weightLabel = (key) => {
  const labels = {
    wLike: '点赞权重', wReply: '评论权重', wRepost: '转发权重',
    wTopicAffinity: '话题亲和', wAuthorAffinity: '作者亲密',
    wTrending: '热门权重', wSimilarity: '相似度权重',
    wFreshness: '新鲜度', wDepthMatch: '深度匹配', explorationFactor: '探索因子'
  }
  return labels[key] || key
}
</script>

<style scoped>
.score-panel {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #dee2e6;
}

.recommend-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 10px 16px 4px;
}

.reason-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  background: linear-gradient(135deg, #e8f4fd 0%, #d1ecf1 100%);
  color: #0c5460;
  border: 1px solid #bee5eb;
}

.score-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.score-header:hover { background: rgba(0, 0, 0, 0.03); }

.rank-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;
  margin-right: 12px;
}

.score-value { flex: 1; font-weight: 600; color: #333; }

.stage-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 600;
  margin-right: 8px;
}
.stage-active { background: #d4edda; color: #155724; }
.stage-beginner { background: #fff3cd; color: #856404; }
.stage-cold { background: #d6d8db; color: #383d41; }

.expand-icon { transition: transform 0.3s; color: #666; }
.expand-icon.expanded { transform: rotate(180deg); }

.score-details { padding: 16px; border-top: 1px solid #dee2e6; }

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: linear-gradient(135deg, #e2e6ea 0%, #d5dbe1 100%);
  border-radius: 8px;
}
.summary-label { font-size: 11px; color: #6c757d; font-weight: 600; }
.summary-text { font-size: 13px; color: #212529; }

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e9ecef;
}
.score-item:last-child { border-bottom: none; }
.score-item .label { color: #495057; font-size: 14px; }
.score-item .value { color: #212529; font-weight: 600; }
.score-item.boost .value { color: #28a745; }
.boost-text { color: #28a745 !important; }

.score-sub {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 8px 0 12px 20px;
  font-size: 12px;
  color: #6c757d;
}

.matched-tags {
  padding: 4px 0 12px 20px;
  font-size: 12px;
  color: #6c757d;
  font-style: italic;
}

.dynamic-weights {
  margin-top: 12px;
  border: 1px solid #e9ecef;
  border-radius: 8px;
  overflow: hidden;
}
.weights-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  cursor: pointer;
  background: #f1f3f5;
}
.weights-label { font-size: 12px; font-weight: 600; color: #495057; }
.weights-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
  padding: 8px 12px;
}
.weight-item { display: flex; justify-content: space-between; font-size: 12px; }
.w-key { color: #6c757d; }
.w-val { font-weight: 600; color: #495057; }

.score-final {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  margin-top: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}
.score-final .label { font-weight: 600; }
.score-final .value { font-size: 24px; font-weight: bold; }

.slide-enter-active, .slide-leave-active { transition: all 0.3s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 800px; }
</style>
