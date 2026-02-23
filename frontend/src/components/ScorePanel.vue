<template>
  <div class="score-panel" v-if="visible">
    <div class="score-header" @click="expanded = !expanded">
      <span class="rank-badge">#{{ rank }}</span>
      <span class="score-value">📊 推荐评分: {{ score.finalScore }}</span>
      <el-icon class="expand-icon" :class="{ expanded }">
        <ArrowDown />
      </el-icon>
    </div>
    
    <transition name="slide">
      <div v-if="expanded" class="score-details">
        <!-- 基础互动分 -->
        <div class="score-item">
          <span class="label">💬 基础互动分</span>
          <span class="value">{{ score.baseEngagement }}</span>
        </div>
        <div class="score-sub">
          <span>❤️ 点赞 {{ score.likeCount }}×0.5 = {{ (score.likeCount * 0.5).toFixed(1) }}</span>
          <span>💬 评论 {{ score.commentCount }}×1.2 = {{ (score.commentCount * 1.2).toFixed(1) }}</span>
          <span>🔄 转发 {{ score.repostCount }}×2.0 = {{ (score.repostCount * 2.0).toFixed(1) }}</span>
        </div>
        
        <!-- 互动率 -->
        <div class="score-item" v-if="score.engagementRate > 0">
          <span class="label">📈 互动率</span>
          <span class="value">{{ (score.engagementRate * 100).toFixed(1) }}%</span>
        </div>
        
        <!-- 时间衰减 -->
        <div class="score-item">
          <span class="label">⏰ 时间衰减</span>
          <span class="value">×{{ score.timeDecayFactor }} ({{ score.hoursAgo }}小时前)</span>
        </div>
        
        <!-- 热门话题加成 -->
        <div class="score-item boost" v-if="score.trendingBoost > 0">
          <span class="label">🔥 热门话题加成</span>
          <span class="value">+{{ score.trendingBoost }}</span>
        </div>
        
        <!-- 个性化加成 -->
        <div class="score-item boost" v-if="score.personalizationBoost > 0">
          <span class="label">❤️ 兴趣匹配加成</span>
          <span class="value">+{{ score.personalizationBoost }}</span>
        </div>
        <div class="matched-tags" v-if="score.matchedTags">
          标签匹配: {{ score.matchedTags }}
        </div>
        
        <!-- In-Network 标识 -->
        <div class="score-item" v-if="score.inNetwork">
          <span class="label">👥 关注来源</span>
          <span class="value boost-text">×1.5 加成</span>
        </div>
        
        <!-- 探索因子 -->
        <div class="score-item">
          <span class="label">🎲 探索因子</span>
          <span class="value">+{{ score.jitter }}</span>
        </div>
        
        <!-- 最终评分 -->
        <div class="score-final">
          <span class="label">🏆 最终评分</span>
          <span class="value">{{ score.finalScore }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  score: {
    type: Object,
    default: () => ({})
  },
  rank: {
    type: Number,
    default: 0
  },
  visible: {
    type: Boolean,
    default: false
  }
})

const expanded = ref(false)
</script>

<style scoped>
.score-panel {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 12px;
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #dee2e6;
}

.score-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
}

.score-header:hover {
  background: rgba(0, 0, 0, 0.03);
}

.rank-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 10px;
  border-radius: 20px;
  font-weight: bold;
  font-size: 14px;
  margin-right: 12px;
}

.score-value {
  flex: 1;
  font-weight: 600;
  color: #333;
}

.expand-icon {
  transition: transform 0.3s;
  color: #666;
}

.expand-icon.expanded {
  transform: rotate(180deg);
}

.score-details {
  padding: 16px;
  border-top: 1px solid #dee2e6;
}

.score-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #e9ecef;
}

.score-item:last-child {
  border-bottom: none;
}

.score-item .label {
  color: #495057;
  font-size: 14px;
}

.score-item .value {
  color: #212529;
  font-weight: 600;
}

.score-item.boost .value {
  color: #28a745;
}

.boost-text {
  color: #28a745 !important;
}

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

.score-final .label {
  font-weight: 600;
}

.score-final .value {
  font-size: 24px;
  font-weight: bold;
}

/* Transition */
.slide-enter-active, .slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from, .slide-leave-to {
  opacity: 0;
  max-height: 0;
}

.slide-enter-to, .slide-leave-from {
  opacity: 1;
  max-height: 500px;
}
</style>
