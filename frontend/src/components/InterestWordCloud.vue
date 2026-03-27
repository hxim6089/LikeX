<template>
  <div class="word-cloud-container">
    <h4 class="section-title">🏷️ 兴趣标签词云</h4>
    <div v-if="data && data.length > 0" ref="chartRef" class="chart"></div>
    <div v-else class="empty-state">
      <p>暂无兴趣数据</p>
      <small>多点赞一些内容来生成词云吧！</small>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'

const props = defineProps({
  data: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chart = null

const initChart = () => {
  if (!chartRef.value || !props.data || props.data.length === 0) return
  
  if (chart) {
    chart.dispose()
  }
  
  chart = echarts.init(chartRef.value)
  
  const option = {
    tooltip: {
      show: true,
      formatter: (params) => `${params.name}: ${params.value} 次`
    },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      left: 'center',
      top: 'center',
      width: '100%',
      height: '100%',
      sizeRange: [14, 48],
      rotationRange: [-45, 45],
      rotationStep: 15,
      gridSize: 8,
      drawOutOfBound: false,
      layoutAnimation: true,
      textStyle: {
        fontFamily: 'system-ui, -apple-system, sans-serif',
        fontWeight: 'bold',
        color: function () {
          const colors = [
            '#1da1f2', '#17bf63', '#ffad1f', '#f45d22', 
            '#794bc4', '#e0245e', '#5c6bc0', '#26a69a'
          ]
          return colors[Math.floor(Math.random() * colors.length)]
        }
      },
      emphasis: {
        textStyle: {
          shadowBlur: 10,
          shadowColor: '#333'
        }
      },
      data: props.data.map(item => ({
        name: item.name,
        value: item.value
      }))
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

watch(() => props.data, () => {
  setTimeout(initChart, 100)
}, { deep: true })
</script>

<style scoped>
.word-cloud-container {
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
  height: 300px;
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
