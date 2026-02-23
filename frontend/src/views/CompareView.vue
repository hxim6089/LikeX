<template>
  <div class="compare-view">
    <h2 class="page-title">🔬 推荐算法效果对比实验</h2>
    <p class="page-desc">左侧为个性化推荐排序，右侧为时间倒序排列，直观验证算法效果</p>

    <!-- 参数调节面板 -->
    <WeightTuner @apply="handleTunedWeights" />

    <!-- 管道漏斗图 -->
    <PipelineFunnel :stats="pipelineStats" v-if="pipelineStats" />

    <!-- 统计卡片 -->
    <div class="stats-bar" v-if="stats">
      <div class="stat-card highlight">
        <div class="stat-value">{{ stats.improvementRatio || 0 }}x</div>
        <div class="stat-label">个性化提升倍率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.personalizedAvgScore || 0 }}</div>
        <div class="stat-label">推荐平均分</div>
        <div class="stat-sub">vs {{ stats.chronologicalAvgScore || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ Math.round((stats.personalizedTagHitRate || 0) * 100) }}%</div>
        <div class="stat-label">标签命中率</div>
        <div class="stat-sub">vs {{ Math.round((stats.chronologicalTagHitRate || 0) * 100) }}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.personalizedSimilarityAvg || 0 }}</div>
        <div class="stat-label">TF-IDF 相似度</div>
        <div class="stat-sub">vs {{ stats.chronologicalSimilarityAvg || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.personalizedEngagementAvg || 0 }}</div>
        <div class="stat-label">平均互动量</div>
        <div class="stat-sub">vs {{ stats.chronologicalEngagementAvg || 0 }}</div>
      </div>
    </div>

    <!-- ECharts 对比柱状图 -->
    <div class="chart-section" v-if="stats">
      <div ref="barChart" class="bar-chart"></div>
    </div>

    <!-- 双栏对比 -->
    <div class="compare-columns" v-if="!loading">
      <div class="compare-column">
        <div class="column-header personalized">
          <span>🎯 个性化推荐</span>
          <span class="count">{{ personalizedFeed.length }} 篇</span>
        </div>
        <div class="feed-list">
          <div v-for="(item, index) in personalizedFeed" :key="'p-' + index" class="compare-card">
            <div class="card-rank">#{{ item.rank }}</div>
            <div class="card-body">
              <div class="card-author">{{ item.content?.author?.username || 'Unknown' }}</div>
              <div class="card-text">{{ truncate(item.content?.content, 80) }}</div>
              <div class="card-score">
                <span class="score-badge">总分 {{ item.scoreBreakdown?.finalScore || 0 }}</span>
                <span class="score-detail" v-if="item.scoreBreakdown?.matchedTags">
                  🏷️ {{ item.scoreBreakdown.matchedTags }}
                </span>
                <span class="score-detail" v-if="item.scoreBreakdown?.contentSimilarityBoost > 0">
                  📐 TF-IDF +{{ item.scoreBreakdown.contentSimilarityBoost }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="compare-column">
        <div class="column-header chronological">
          <span>🕐 时间倒序</span>
          <span class="count">{{ chronologicalFeed.length }} 篇</span>
        </div>
        <div class="feed-list">
          <div v-for="(item, index) in chronologicalFeed" :key="'c-' + index" class="compare-card">
            <div class="card-rank dim">#{{ item.rank }}</div>
            <div class="card-body">
              <div class="card-author">{{ item.content?.author?.username || 'Unknown' }}</div>
              <div class="card-text">{{ truncate(item.content?.content, 80) }}</div>
              <div class="card-score">
                <span class="score-badge dim">总分 {{ item.scoreBreakdown?.finalScore || 0 }}</span>
                <span class="score-detail" v-if="item.scoreBreakdown?.matchedTags">
                  🏷️ {{ item.scoreBreakdown.matchedTags }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在计算推荐对比...</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import api from '../api'
import * as echarts from 'echarts'
import PipelineFunnel from '../components/PipelineFunnel.vue'
import WeightTuner from '../components/WeightTuner.vue'

const personalizedFeed = ref([])
const chronologicalFeed = ref([])
const stats = ref(null)
const pipelineStats = ref(null)
const loading = ref(false)
const barChart = ref(null)
let chartInstance = null

const getCurrentUserId = () => {
  try {
    const user = JSON.parse(localStorage.getItem('user'))
    return user?.id
  } catch { return null }
}

const loadCompare = async () => {
  const userId = getCurrentUserId()
  if (!userId) return
  
  loading.value = true
  try {
    const res = await api.get('/compare/feed', { params: { userId } })
    personalizedFeed.value = res.data.personalized || []
    chronologicalFeed.value = res.data.chronological || []
    stats.value = res.data.stats || {}
    pipelineStats.value = res.data.pipelineStats || {}
    
    nextTick(() => renderBarChart())
  } catch (e) {
    console.error('Compare load failed', e)
  } finally {
    loading.value = false
  }
}

const handleTunedWeights = async (weights) => {
  const userId = getCurrentUserId()
  if (!userId) return

  loading.value = true
  try {
    const res = await api.get('/compare/tuned', { params: { userId, ...weights } })
    personalizedFeed.value = res.data.personalized || []
    chronologicalFeed.value = res.data.chronological || []
    stats.value = res.data.stats || {}
    
    nextTick(() => renderBarChart())
  } catch (e) {
    console.error('Tuned feed failed', e)
  } finally {
    loading.value = false
  }
}

const renderBarChart = () => {
  if (!barChart.value || !stats.value) return
  
  if (!chartInstance) {
    chartInstance = echarts.init(barChart.value)
  }

  const s = stats.value
  const option = {
    tooltip: { trigger: 'axis' },
    legend: { data: ['个性化推荐', '时间倒序'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: ['平均分', '标签命中率(%)', '互动量', 'TF-IDF 相似度']
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '个性化推荐',
        type: 'bar',
        barGap: '10%',
        itemStyle: { color: '#1DA1F2', borderRadius: [4, 4, 0, 0] },
        data: [
          s.personalizedAvgScore || 0,
          Math.round((s.personalizedTagHitRate || 0) * 100),
          s.personalizedEngagementAvg || 0,
          s.personalizedSimilarityAvg || 0
        ]
      },
      {
        name: '时间倒序',
        type: 'bar',
        itemStyle: { color: '#AAB8C2', borderRadius: [4, 4, 0, 0] },
        data: [
          s.chronologicalAvgScore || 0,
          Math.round((s.chronologicalTagHitRate || 0) * 100),
          s.chronologicalEngagementAvg || 0,
          s.chronologicalSimilarityAvg || 0
        ]
      }
    ]
  }

  chartInstance.setOption(option)
}

const truncate = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '...' : text
}

onMounted(() => {
  loadCompare()
})
</script>

<style scoped>
.compare-view {
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

/* 统计卡片 */
.stats-bar {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
  border: 1px solid #e1e8ed;
}

.stat-card.highlight {
  background: linear-gradient(135deg, #1DA1F2, #0d8bd9);
  color: white;
  border: none;
}

.stat-card.highlight .stat-label,
.stat-card.highlight .stat-sub {
  color: rgba(255,255,255,0.8);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #536471;
  margin-top: 4px;
}

.stat-sub {
  font-size: 11px;
  color: #AAB8C2;
  margin-top: 2px;
}

/* 图表 */
.chart-section {
  background: white;
  border-radius: 16px;
  padding: 16px;
  border: 1px solid #e1e8ed;
  margin-bottom: 20px;
}

.bar-chart {
  width: 100%;
  height: 280px;
}

/* 双栏对比 */
.compare-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.compare-column {
  background: white;
  border-radius: 16px;
  border: 1px solid #e1e8ed;
  overflow: hidden;
}

.column-header {
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 700;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.column-header.personalized {
  background: linear-gradient(135deg, #E8F5FE, #D3ECFD);
  color: #1DA1F2;
}

.column-header.chronological {
  background: #f7f9fa;
  color: #536471;
}

.count {
  font-size: 13px;
  font-weight: 400;
}

.feed-list {
  max-height: 600px;
  overflow-y: auto;
}

.compare-card {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  gap: 12px;
  transition: background 0.2s;
}

.compare-card:hover {
  background: #f7f9fa;
}

.card-rank {
  font-size: 18px;
  font-weight: 800;
  color: #1DA1F2;
  min-width: 36px;
  display: flex;
  align-items: flex-start;
  padding-top: 2px;
}

.card-rank.dim {
  color: #AAB8C2;
}

.card-body {
  flex: 1;
  min-width: 0;
}

.card-author {
  font-weight: 700;
  font-size: 14px;
  color: #0f1419;
}

.card-text {
  font-size: 13px;
  color: #536471;
  margin: 4px 0;
  line-height: 1.4;
}

.card-score {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  align-items: center;
}

.score-badge {
  background: #E8F5FE;
  color: #1DA1F2;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.score-badge.dim {
  background: #f0f0f0;
  color: #AAB8C2;
}

.score-detail {
  font-size: 11px;
  color: #536471;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px 0;
  color: #536471;
}
</style>
