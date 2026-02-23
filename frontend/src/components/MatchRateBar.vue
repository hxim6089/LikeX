<template>
  <div class="match-rate-container">
    <h4 class="section-title">📈 分类偏好分布</h4>
    <div v-if="data && data.length > 0" class="bars">
      <div v-for="item in data" :key="item.category" class="bar-item">
        <div class="bar-header">
          <span class="category-name">{{ getCategoryLabel(item.category) }}</span>
          <span class="percentage">{{ item.percentage }}%</span>
        </div>
        <div class="bar-bg">
          <div 
            class="bar-fill" 
            :style="{ width: item.percentage + '%', background: getCategoryColor(item.category) }"
          ></div>
        </div>
        <div class="count-text">{{ item.count }} 次点赞</div>
      </div>
    </div>
    <div v-else class="empty-state">
      <p>暂无分类数据</p>
      <small>多浏览和点赞内容来生成分析！</small>
    </div>
    
    <!-- 推荐匹配度 -->
    <div v-if="matchRate !== null" class="match-section">
      <div class="match-header">
        <span>🎯 推荐匹配度</span>
        <span class="match-value">{{ matchRate }}%</span>
      </div>
      <div class="match-bar-bg">
        <div class="match-bar-fill" :style="{ width: matchRate + '%' }"></div>
      </div>
      <div class="match-hint">
        基于您的互动行为计算的个性化推荐准确度
      </div>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  data: {
    type: Array,
    default: () => []
  },
  matchRate: {
    type: Number,
    default: null
  }
})

const getCategoryLabel = (category) => {
  const labels = {
    'Tech': '🔧 科技',
    'Life': '🌱 生活',
    'Sports': '⚽ 体育',
    'News': '📰 新闻',
    'Entertainment': '🎬 娱乐',
    'Finance': '💰 财经'
  }
  return labels[category] || category
}

const getCategoryColor = (category) => {
  const colors = {
    'Tech': 'linear-gradient(90deg, #667eea 0%, #764ba2 100%)',
    'Life': 'linear-gradient(90deg, #11998e 0%, #38ef7d 100%)',
    'Sports': 'linear-gradient(90deg, #fc4a1a 0%, #f7b733 100%)',
    'News': 'linear-gradient(90deg, #4facfe 0%, #00f2fe 100%)',
    'Entertainment': 'linear-gradient(90deg, #f093fb 0%, #f5576c 100%)',
    'Finance': 'linear-gradient(90deg, #ffecd2 0%, #fcb69f 100%)'
  }
  return colors[category] || 'linear-gradient(90deg, #1da1f2 0%, #0d8bd9 100%)'
}
</script>

<style scoped>
.match-rate-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.section-title {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f1419;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.category-name {
  font-weight: 600;
  color: #0f1419;
  font-size: 15px;
}

.percentage {
  font-weight: 700;
  color: #1da1f2;
  font-size: 15px;
}

.bar-bg {
  height: 10px;
  background: #e9ecef;
  border-radius: 5px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.5s ease;
}

.count-text {
  font-size: 12px;
  color: #536471;
}

.empty-state {
  text-align: center;
  padding: 30px 20px;
  color: #536471;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 15px;
}

.empty-state small {
  color: #8899a6;
}

.match-section {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #eff3f4;
}

.match-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.match-header span:first-child {
  font-weight: 600;
  color: #0f1419;
}

.match-value {
  font-size: 24px;
  font-weight: 800;
  color: #17bf63;
}

.match-bar-bg {
  height: 12px;
  background: #e9ecef;
  border-radius: 6px;
  overflow: hidden;
}

.match-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #17bf63 0%, #1da1f2 100%);
  border-radius: 6px;
  transition: width 0.5s ease;
}

.match-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #8899a6;
}
</style>
