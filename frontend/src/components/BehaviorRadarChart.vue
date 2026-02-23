<template>
  <div class="radar-chart-container">
    <h4 class="section-title">📊 行为分布雷达图</h4>
    <div v-if="hasData" ref="chartRef" class="chart"></div>
    <div v-else class="empty-state">
      <p>暂无行为数据</p>
      <small>与内容互动来生成分析图吧！</small>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)
let chart = null

const hasData = computed(() => {
  if (!props.stats) return false
  const total = Object.values(props.stats).reduce((a, b) => a + b, 0)
  return total > 0
})

const initChart = () => {
  if (!chartRef.value || !hasData.value) return
  
  if (chart) {
    chart.dispose()
  }
  
  chart = echarts.init(chartRef.value)
  
  const stats = props.stats
  const maxValue = Math.max(
    stats.likes || 0,
    stats.comments || 0,
    stats.reposts || 0,
    stats.views || 0,
    1 // 防止全为0
  )
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: [
        { name: '点赞', max: maxValue },
        { name: '评论', max: maxValue },
        { name: '转发', max: maxValue },
        { name: '浏览', max: maxValue }
      ],
      radius: '65%',
      axisName: {
        color: '#495057',
        fontSize: 14,
        fontWeight: 500
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(29, 161, 242, 0.05)', 'rgba(29, 161, 242, 0.1)', 
                  'rgba(29, 161, 242, 0.15)', 'rgba(29, 161, 242, 0.2)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(0, 0, 0, 0.1)'
        }
      }
    },
    series: [{
      name: '行为统计',
      type: 'radar',
      data: [{
        value: [
          stats.likes || 0,
          stats.comments || 0,
          stats.reposts || 0,
          stats.views || 0
        ],
        name: '行为分布',
        areaStyle: {
          color: 'rgba(29, 161, 242, 0.4)'
        },
        lineStyle: {
          color: '#1da1f2',
          width: 2
        },
        itemStyle: {
          color: '#1da1f2'
        }
      }]
    }]
  }
  
  chart.setOption(option)
}

const handleResize = () => {
  if (chart) {
    chart.resize()
  }
}

onMounted(() => {
  setTimeout(initChart, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) {
    chart.dispose()
  }
  window.removeEventListener('resize', handleResize)
})

watch(() => props.stats, () => {
  setTimeout(initChart, 100)
}, { deep: true })
</script>

<style scoped>
.radar-chart-container {
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
  height: 280px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #536471;
}

.empty-state p {
  margin: 0 0 8px 0;
  font-size: 15px;
}

.empty-state small {
  color: #8899a6;
}
</style>
