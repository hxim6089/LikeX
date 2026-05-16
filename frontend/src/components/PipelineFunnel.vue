<template>
  <div class="pipeline-funnel">
    <h3 class="funnel-title">📊 推荐管道 Pipeline</h3>
    <div ref="funnelChart" class="funnel-chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  stats: {
    type: Object,
    default: () => ({})
  }
})

const funnelChart = ref(null)
let chartInstance = null

const renderChart = () => {
  if (!funnelChart.value || !props.stats) return
  
  if (!chartInstance) {
    chartInstance = echarts.init(funnelChart.value)
  }

  const { totalCandidates, afterNegativeFilter, inNetworkCount, outNetworkCount, afterScoring, afterDiversity } = props.stats
  const sourceSplitCount = (inNetworkCount || 0) + (outNetworkCount || 0)
  const sourceSplitDetail = `关注作者 ${inNetworkCount || 0} 条，其他作者 ${outNetworkCount || 0} 条`

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const detail = params.data?.detail
        return detail ? `${params.name}: ${params.value} 条<br/>${detail}` : `${params.name}: ${params.value} 条`
      }
    },
    color: ['#1DA1F2', '#17BF63', '#FFAD1F', '#F45D22', '#794BC4', '#E0245E'],
    series: [{
      name: '推荐管道',
      type: 'funnel',
      left: '10%',
      top: 40,
      bottom: 10,
      width: '80%',
      min: 0,
      max: totalCandidates || 100,
      minSize: '15%',
      maxSize: '100%',
      sort: 'none',
      gap: 4,
      label: {
        show: true,
        position: 'inside',
        formatter: '{b}\n{c} 条',
        fontSize: 13,
        color: '#fff',
        fontWeight: 'bold'
      },
      labelLine: { show: false },
      itemStyle: {
        borderColor: '#fff',
        borderWidth: 2,
        borderRadius: 4
      },
      emphasis: {
        label: { fontSize: 15 }
      },
      data: [
        { value: totalCandidates || 0, name: '全部帖子候选' },
        { value: sourceSplitCount, name: '关注/其他作者候选', detail: sourceSplitDetail },
        { value: afterNegativeFilter || 0, name: '过滤不感兴趣内容' },
        { value: afterScoring || 0, name: '按评分截取候选' },
        { value: afterDiversity || 0, name: '最终推荐给用户' }
      ]
    }]
  }

  chartInstance.setOption(option)
}

onMounted(() => {
  nextTick(() => renderChart())
})

watch(() => props.stats, () => {
  nextTick(() => renderChart())
}, { deep: true })
</script>

<style scoped>
.pipeline-funnel {
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e1e8ed;
  margin-bottom: 16px;
}

.funnel-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f1419;
  margin: 0 0 8px 0;
}

.funnel-chart {
  width: 100%;
  height: 300px;
}
</style>
