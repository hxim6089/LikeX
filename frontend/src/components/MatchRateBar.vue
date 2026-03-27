<template>
  <div class="match-rate-container">
    <h4 class="section-title">📈 分类偏好分布</h4>
    <div v-if="data && data.length > 0" ref="chartRef" class="chart"></div>
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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

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

const chartRef = ref(null)
let chart = null

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
    'Tech': '#667eea',
    'Life': '#17bf63',
    'Sports': '#fc4a1a',
    'News': '#4facfe',
    'Entertainment': '#f093fb',
    'Finance': '#fcb69f'
  }
  return colors[category] || '#1da1f2'
}

const initChart = () => {
  if (!chartRef.value || !props.data || props.data.length === 0) return
  
  if (chart) {
    chart.dispose()
  }
  
  chart = echarts.init(chartRef.value)
  
  const totalLikes = props.data.reduce((sum, d) => sum + (d.count || 0), 0)
  
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { fontSize: 13, color: '#536471' },
      formatter: (name) => getCategoryLabel(name)
    },
    series: [{
      type: 'pie',
      radius: ['45%', '72%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 3
      },
      label: {
        show: true,
        position: 'center',
        formatter: () => totalLikes + '\n总点赞',
        fontSize: 18,
        fontWeight: 'bold',
        color: '#0f1419',
        lineHeight: 24
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 20,
          fontWeight: 'bold'
        }
      },
      data: props.data.map(item => ({
        name: item.category,
        value: item.count || 0,
        itemStyle: { color: getCategoryColor(item.category) }
      }))
    }]
  }
  
  chart.setOption(option)
}

const handleResize = () => {
  if (chart) chart.resize()
}

onMounted(() => {
  setTimeout(initChart, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) chart.dispose()
  window.removeEventListener('resize', handleResize)
})

watch(() => props.data, () => {
  setTimeout(initChart, 100)
}, { deep: true })
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

.chart {
  width: 100%;
  height: 260px;
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
