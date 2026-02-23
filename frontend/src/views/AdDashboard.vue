<template>
  <div class="ad-dashboard">
    <h2 class="page-title">📊 广告效果报表</h2>
    <p class="page-desc">模拟 Google AdSense 广告投放数据，展示推荐系统在商业变现中的应用</p>

    <!-- 指标卡片 -->
    <div class="stats-cards" v-if="stats">
      <div class="stat-card">
        <div class="stat-icon">👁️</div>
        <div class="stat-value">{{ formatNum(stats.totalImpressions) }}</div>
        <div class="stat-label">总展示量</div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🖱️</div>
        <div class="stat-value">{{ formatNum(stats.totalClicks) }}</div>
        <div class="stat-label">总点击量</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-icon">📈</div>
        <div class="stat-value">{{ stats.overallCtr }}%</div>
        <div class="stat-label">整体 CTR</div>
      </div>
      <div class="stat-card revenue">
        <div class="stat-icon">💰</div>
        <div class="stat-value">¥{{ stats.estimatedRevenue }}</div>
        <div class="stat-label">预估收入</div>
      </div>
    </div>

    <!-- 图表区 -->
    <div class="charts-row" v-if="stats">
      <div class="chart-box">
        <h3>各类别平均 CTR</h3>
        <div ref="ctrChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <h3>展示量分布</h3>
        <div ref="pieChart" class="chart"></div>
      </div>
    </div>

    <!-- 广告列表 -->
    <div class="ad-table-box" v-if="stats && stats.ads">
      <h3>📋 广告明细</h3>
      <el-table :data="sortedAds" stripe style="width: 100%" :default-sort="{ prop: 'ctr', order: 'descending' }" @sort-change="handleSort">
        <el-table-column prop="title" label="广告标题" width="200" />
        <el-table-column prop="advertiser" label="广告主" width="120" />
        <el-table-column prop="targetTags" label="定向标签" width="160">
          <template #default="{ row }">
            <div class="tag-list">
              <span v-for="tag in (row.targetTags || '').split(',')" :key="tag" class="mini-tag">{{ tag.trim() }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="bidPrice" label="出价(CPM)" width="100" sortable>
          <template #default="{ row }">¥{{ row.bidPrice }}</template>
        </el-table-column>
        <el-table-column prop="impressions" label="展示" width="80" sortable />
        <el-table-column prop="clicks" label="点击" width="80" sortable />
        <el-table-column prop="ctr" label="CTR" width="90" sortable>
          <template #default="{ row }">
            <span :class="ctrClass(row.ctr)">{{ row.ctr }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="ecpm" label="eCPM" width="90" sortable>
          <template #default="{ row }">¥{{ row.ecpm }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import api from '../api'
import * as echarts from 'echarts'

const stats = ref(null)
const loading = ref(false)
const ctrChart = ref(null)
const pieChart = ref(null)
const sortKey = ref('ctr')
const sortOrder = ref('descending')

const sortedAds = computed(() => {
  if (!stats.value || !stats.value.ads) return []
  return [...stats.value.ads]
})

const handleSort = ({ prop, order }) => {
  sortKey.value = prop
  sortOrder.value = order
}

const ctrClass = (ctr) => {
  if (ctr >= 5) return 'ctr-high'
  if (ctr >= 3) return 'ctr-mid'
  return 'ctr-low'
}

const formatNum = (n) => {
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}

const loadStats = async () => {
  loading.value = true
  try {
    const res = await api.get('/ads/stats')
    stats.value = res.data
    nextTick(() => {
      renderCtrChart()
      renderPieChart()
    })
  } catch (e) {
    console.error('Stats load failed', e)
  } finally {
    loading.value = false
  }
}

const renderCtrChart = () => {
  if (!ctrChart.value || !stats.value.categoryCtr) return
  const chart = echarts.init(ctrChart.value)
  const categories = Object.keys(stats.value.categoryCtr)
  const values = Object.values(stats.value.categoryCtr)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value', name: 'CTR(%)' },
    series: [{
      type: 'bar',
      data: values,
      itemStyle: {
        color: (params) => {
          const colors = ['#1DA1F2', '#17BF63', '#FFAD1F', '#F45D22', '#794BC4']
          return colors[params.dataIndex % colors.length]
        },
        borderRadius: [4, 4, 0, 0]
      },
      label: { show: true, position: 'top', formatter: '{c}%' }
    }]
  })
}

const renderPieChart = () => {
  if (!pieChart.value || !stats.value.categoryImpressions) return
  const chart = echarts.init(pieChart.value)
  const data = Object.entries(stats.value.categoryImpressions).map(([name, value]) => ({ name, value }))

  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    color: ['#1DA1F2', '#17BF63', '#FFAD1F', '#F45D22', '#794BC4'],
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: true,
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{d}%' },
      data
    }]
  })
}

onMounted(loadStats)
</script>

<style scoped>
.ad-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f1419;
  margin: 0 0 4px 0;
}

.page-desc {
  font-size: 14px;
  color: #536471;
  margin: 0 0 20px 0;
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  text-align: center;
  border: 1px solid #e1e8ed;
}

.stat-card.highlight {
  background: linear-gradient(135deg, #17BF63, #0fa854);
  color: white;
  border: none;
}

.stat-card.highlight .stat-label { color: rgba(255,255,255,0.8); }

.stat-card.revenue {
  background: linear-gradient(135deg, #FFAD1F, #f5960f);
  color: white;
  border: none;
}

.stat-card.revenue .stat-label { color: rgba(255,255,255,0.8); }

.stat-icon { font-size: 28px; margin-bottom: 8px; }

.stat-value {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #536471;
  margin-top: 4px;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}

.chart-box {
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e1e8ed;
}

.chart-box h3 {
  font-size: 15px;
  font-weight: 700;
  color: #0f1419;
  margin: 0 0 8px 0;
}

.chart { width: 100%; height: 260px; }

.ad-table-box {
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e1e8ed;
}

.ad-table-box h3 {
  font-size: 15px;
  font-weight: 700;
  color: #0f1419;
  margin: 0 0 12px 0;
}

.tag-list { display: flex; gap: 4px; flex-wrap: wrap; }

.mini-tag {
  background: #E8F5FE;
  color: #1DA1F2;
  padding: 1px 6px;
  border-radius: 8px;
  font-size: 11px;
}

.ctr-high { color: #17BF63; font-weight: 700; }
.ctr-mid { color: #FFAD1F; font-weight: 600; }
.ctr-low { color: #F45D22; }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: #536471;
}
</style>
