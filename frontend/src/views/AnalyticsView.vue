<template>
  <div class="analytics-view">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据统计中心</h2>
        <p class="page-desc">聚合平台运行、用户行为、内容生态、负反馈与广告分发数据</p>
      </div>
      <div class="header-actions">
        <el-radio-group v-model="selectedRange" size="small" @change="loadDashboard">
          <el-radio-button
            v-for="item in rangeOptions"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
        <el-button type="primary" :loading="loading" @click="loadDashboard">刷新数据</el-button>
      </div>
    </div>

    <div v-if="loading && !dashboard" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>正在汇总统计数据...</span>
    </div>

    <template v-else-if="dashboard">
      <div class="range-note">
        当前统计范围：<strong>{{ rangeMeta.label || '全部' }}</strong>
        <span v-if="rangeMeta.since">，起始时间 {{ formatDateTime(rangeMeta.since) }}</span>
      </div>

      <div class="overview-grid">
        <div
          v-for="card in metricCards"
          :key="card.key"
          class="metric-card"
          :class="card.className"
        >
          <el-tooltip :content="card.tip" placement="top">
            <div class="metric-label">{{ card.label }}</div>
          </el-tooltip>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-sub">{{ card.sub }}</div>
        </div>
      </div>

      <section class="panel source-panel">
        <div class="panel-header">
          <h3>数据来源与内容质量</h3>
          <span>帖子来源、图片与标签覆盖情况</span>
        </div>
        <div class="source-grid">
          <div v-for="item in sourceCards" :key="item.key" class="source-card">
            <div class="source-top">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
            <div class="source-bar">
              <div :style="{ width: item.percent + '%' }"></div>
            </div>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </section>

      <div class="charts-grid two">
        <section class="panel">
          <div class="panel-header">
            <h3>{{ rangeMeta.trendDays || 30 }} 天行为趋势</h3>
            <span>浏览、点赞、评论、点踩、搜索</span>
          </div>
          <div ref="behaviorTrendChart" class="chart tall"></div>
        </section>
        <section class="panel">
          <div class="panel-header">
            <h3>行为类型分布</h3>
            <span>多维行为采集情况</span>
          </div>
          <div ref="behaviorPieChart" class="chart tall"></div>
        </section>
      </div>

      <div class="charts-grid two">
        <section class="panel">
          <div class="panel-header">
            <h3>用户阶段分布</h3>
            <span>冷启动、初级、活跃用户</span>
          </div>
          <div ref="stageChart" class="chart"></div>
        </section>
        <section class="panel">
          <div class="panel-header">
            <h3>用户类型分布</h3>
            <span>创作者 / 互动型 / 浏览型</span>
          </div>
          <div ref="userTypeChart" class="chart"></div>
        </section>
      </div>

      <div class="charts-grid content-row">
        <section class="panel">
          <div class="panel-header">
            <h3>内容分类分布</h3>
            <span>平台内容生态</span>
          </div>
          <div ref="categoryChart" class="chart"></div>
        </section>

        <section class="panel">
          <div class="panel-header">
            <h3>内容标签分布</h3>
            <span>按帖子标签聚合统计</span>
          </div>
          <div ref="tagChart" class="chart"></div>
        </section>

        <section class="panel list-panel">
          <div class="panel-header">
            <h3>全站热门话题 Top10</h3>
            <span>帖子数 + 互动量加权</span>
          </div>
          <div class="rank-list">
            <div v-for="(topic, index) in contentStats.trendingTopics || []" :key="topic.name" class="rank-item">
              <span class="rank-index">#{{ index + 1 }}</span>
              <div class="rank-main">
                <div class="rank-name">#{{ topic.name }}</div>
                <div class="rank-meta">
                  {{ topic.postCount }} 帖子 · {{ topic.engagement }} 互动 · 热度 {{ topic.score }}
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel list-panel">
          <div class="panel-header">
            <h3>用户兴趣标签 Top10</h3>
            <span>来自全站行为画像</span>
          </div>
          <div class="rank-list">
            <div v-for="(tag, index) in userStats.topInterestTags || []" :key="tag.name" class="rank-item">
              <span class="rank-index blue">#{{ index + 1 }}</span>
              <div class="rank-main">
                <div class="rank-name">{{ tag.name }}</div>
                <div class="rank-meta">兴趣权重 {{ tag.value }}</div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="charts-grid two">
        <section class="panel">
          <div class="panel-header">
            <h3>{{ rangeMeta.trendDays || 30 }} 天发帖趋势</h3>
            <span>内容生产趋势</span>
          </div>
          <div ref="postTrendChart" class="chart"></div>
        </section>
        <section class="panel">
          <div class="panel-header">
            <h3>24 小时活跃时段</h3>
            <span>行为发生时间分布</span>
          </div>
          <div ref="hourlyChart" class="chart"></div>
        </section>
      </div>

      <div class="bottom-grid">
        <section class="panel top-posts">
          <div class="panel-header">
            <h3>高互动帖子 Top10</h3>
            <span>按浏览、点赞、评论、转发综合排序</span>
          </div>
          <el-table :data="contentStats.topPosts || []" stripe height="360">
            <el-table-column label="#" width="52">
              <template #default="{ $index }">{{ $index + 1 }}</template>
            </el-table-column>
            <el-table-column label="作者" prop="author" width="110" />
            <el-table-column label="内容" min-width="280">
              <template #default="{ row }">
                <div class="post-text">{{ row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column label="互动分" prop="engagementScore" width="92" />
            <el-table-column label="赞" prop="likeCount" width="70" />
            <el-table-column label="评" prop="commentCount" width="70" />
            <el-table-column label="转" prop="repostCount" width="70" />
          </el-table>
        </section>

        <section class="panel side-summary">
          <div class="panel-header">
            <h3>负反馈统计</h3>
            <span>反向信号采集情况</span>
          </div>
          <div class="feedback-list">
            <div v-for="item in feedbackStats.breakdown || []" :key="item.type" class="feedback-item">
              <span>{{ item.label }}</span>
              <strong>{{ formatNum(item.count) }}</strong>
            </div>
          </div>

          <div class="panel-divider"></div>

          <div class="panel-header compact">
            <h3>广告分发摘要</h3>
            <span>平台商业化统计</span>
          </div>
          <div class="ad-summary-grid">
            <div>
              <span>广告数</span>
              <strong>{{ formatNum(adSummary.totalAds) }}</strong>
            </div>
            <div>
              <span>展示</span>
              <strong>{{ formatNum(adSummary.totalImpressions) }}</strong>
            </div>
            <div>
              <span>点击</span>
              <strong>{{ formatNum(adSummary.totalClicks) }}</strong>
            </div>
            <div>
              <span>收入</span>
              <strong>¥{{ adSummary.estimatedRevenue || 0 }}</strong>
            </div>
          </div>
          <div class="mini-rank">
            <div v-for="item in (adSummary.categoryCtr || []).slice(0, 5)" :key="item.name" class="mini-rank-item">
              <span>{{ item.name }}</span>
              <strong>{{ item.ctr }}%</strong>
            </div>
          </div>
        </section>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import api from '../api'
import * as echarts from 'echarts'

const dashboard = ref(null)
const loading = ref(false)
const selectedRange = ref('all')

const rangeOptions = [
  { label: '今日', value: 'today' },
  { label: '7 天', value: '7d' },
  { label: '30 天', value: '30d' },
  { label: '全部', value: 'all' }
]

const behaviorTrendChart = ref(null)
const behaviorPieChart = ref(null)
const stageChart = ref(null)
const userTypeChart = ref(null)
const categoryChart = ref(null)
const tagChart = ref(null)
const postTrendChart = ref(null)
const hourlyChart = ref(null)

let charts = []
const resizeHandler = () => charts.forEach(chart => chart.resize())

const overview = computed(() => dashboard.value?.overview || {})
const behaviorStats = computed(() => dashboard.value?.behaviorStats || {})
const userStats = computed(() => dashboard.value?.userStats || {})
const contentStats = computed(() => dashboard.value?.contentStats || {})
const sourceStats = computed(() => dashboard.value?.sourceStats || {})
const feedbackStats = computed(() => dashboard.value?.feedbackStats || {})
const adSummary = computed(() => dashboard.value?.adSummary || {})
const rangeMeta = computed(() => dashboard.value?.range || {})

const totalEngagement = computed(() =>
  (overview.value.totalLikes || 0)
  + (overview.value.totalComments || 0)
  + (overview.value.totalReposts || 0)
)

const feedbackTotal = computed(() =>
  (feedbackStats.value.totalNegativeSignals || 0)
  + (feedbackStats.value.totalDislikes || 0)
)

const metricCards = computed(() => [
  {
    key: 'users',
    label: '注册用户',
    value: formatNum(overview.value.totalUsers),
    sub: `范围内新增 ${formatNum(overview.value.periodNewUsers)} 人`,
    tip: '系统内已注册的全部账号数量，新增用户按当前时间范围统计。',
    className: 'primary'
  },
  {
    key: 'activeUsers',
    label: '活跃用户',
    value: formatNum(overview.value.activeUsers),
    sub: '范围内产生过行为的用户',
    tip: '当前时间范围内至少产生一次浏览、点赞、评论、转发、搜索等行为的用户数。'
  },
  {
    key: 'posts',
    label: '帖子数',
    value: formatNum(overview.value.totalPosts),
    sub: '不含评论回复',
    tip: '当前时间范围内发布的主帖数量，不包含评论回复。'
  },
  {
    key: 'behaviors',
    label: '行为记录',
    value: formatNum(overview.value.totalBehaviors),
    sub: `今日行为 ${formatNum(overview.value.todayBehaviors)}`,
    tip: '用户浏览、点赞、评论、转发、引用、点踩、搜索等行为记录总数。'
  },
  {
    key: 'views',
    label: '浏览量',
    value: formatNum(overview.value.totalViews),
    sub: '内容曝光规模',
    tip: '当前时间范围内内容累计浏览数。'
  },
  {
    key: 'engagement',
    label: '互动量',
    value: formatNum(totalEngagement.value),
    sub: '点赞 + 评论 + 转发',
    tip: '互动量 = 点赞数 + 评论数 + 转发数。'
  },
  {
    key: 'feedback',
    label: '负反馈',
    value: formatNum(feedbackTotal.value),
    sub: '点踩、屏蔽、静音、不感兴趣',
    tip: '负反馈 = 点踩行为 + 不感兴趣、屏蔽作者、静音作者等负面信号。'
  },
  {
    key: 'ctr',
    label: '广告 CTR',
    value: `${adSummary.value.overallCtr || 0}%`,
    sub: `估算收入 ¥${adSummary.value.estimatedRevenue || 0}`,
    tip: 'CTR = 广告点击数 / 广告展示数。',
    className: 'revenue'
  }
])

const sourceCards = computed(() => {
  const total = sourceStats.value.totalPosts || 0
  return [
    {
      key: 'local',
      label: '用户/系统发布',
      value: formatNum(sourceStats.value.localPublished),
      percent: sourceStats.value.localPublished ? Math.min(100, sourceStats.value.localPublished / Math.max(total, 1) * 100) : 0,
      desc: '由站内用户或系统种子数据产生的帖子'
    },
    {
      key: 'external',
      label: '外部导入线索',
      value: formatNum(sourceStats.value.externalImported),
      percent: sourceStats.value.externalImported ? Math.min(100, sourceStats.value.externalImported / Math.max(total, 1) * 100) : 0,
      desc: '根据 X 爬取作者、导入标题或图片域名识别'
    },
    {
      key: 'image',
      label: '含图片帖子',
      value: `${sourceStats.value.imageRatio || 0}%`,
      percent: sourceStats.value.imageRatio || 0,
      desc: `${formatNum(sourceStats.value.withImage)} 条内容带图片`
    },
    {
      key: 'tagged',
      label: '已打标签帖子',
      value: `${sourceStats.value.taggedRatio || 0}%`,
      percent: sourceStats.value.taggedRatio || 0,
      desc: `${formatNum(sourceStats.value.withTags)} 条内容包含话题标签`
    }
  ]
})

const formatNum = (n) => {
  const value = Number(n || 0)
  if (value >= 10000) return `${(value / 10000).toFixed(1)}万`
  if (value >= 1000) return `${(value / 1000).toFixed(1)}k`
  return value.toLocaleString()
}

const formatDateTime = (value) => {
  if (!value) return ''
  return value.replace('T', ' ').slice(0, 16)
}

const disposeCharts = () => {
  charts.forEach(chart => chart.dispose())
  charts = []
}

const initChart = (elRef, option) => {
  if (!elRef.value) return
  const chart = echarts.init(elRef.value)
  chart.setOption(option)
  charts.push(chart)
}

const renderCharts = () => {
  disposeCharts()

  const behaviorDistribution = behaviorStats.value.typeDistribution || []
  const dailyTrend = behaviorStats.value.dailyTrend || []
  const dailyLabels = dailyTrend.map(item => item.date)

  initChart(behaviorTrendChart, {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, data: ['浏览', '点赞', '评论', '点踩', '搜索'] },
    grid: { left: 36, right: 20, top: 48, bottom: 28 },
    xAxis: { type: 'category', data: dailyLabels },
    yAxis: { type: 'value' },
    series: [
      lineSeries('浏览', dailyTrend.map(i => i.VIEW || 0), '#1d9bf0'),
      lineSeries('点赞', dailyTrend.map(i => i.LIKE || 0), '#f91880'),
      lineSeries('评论', dailyTrend.map(i => i.COMMENT || 0), '#00ba7c'),
      lineSeries('点踩', dailyTrend.map(i => i.DISLIKE || 0), '#f4212e'),
      lineSeries('搜索', dailyTrend.map(i => i.SEARCH || 0), '#7856ff')
    ]
  })

  initChart(behaviorPieChart, {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '70%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      data: behaviorDistribution
        .filter(item => Number(item.value || item.count || 0) > 0)
        .map(item => ({ name: item.label, value: item.count }))
    }]
  })

  initChart(stageChart, pieOption(userStats.value.stageDistribution || [], ['#1d9bf0', '#ffad1f', '#00ba7c']))
  initChart(userTypeChart, pieOption(userStats.value.typeDistribution || [], ['#1d9bf0', '#7856ff', '#00ba7c']))

  initChart(categoryChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 26, bottom: 36 },
    xAxis: { type: 'category', data: (contentStats.value.categoryDistribution || []).map(i => i.name) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: (contentStats.value.categoryDistribution || []).map(i => i.value),
      itemStyle: { color: '#1d9bf0', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 42
    }]
  })

  const tagDistribution = contentStats.value.tagDistribution || []
  initChart(tagChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 26, bottom: 54 },
    xAxis: {
      type: 'category',
      data: tagDistribution.map(i => i.name),
      axisLabel: { interval: 0, rotate: 30, fontSize: 11 }
    },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: tagDistribution.map(i => i.value),
      itemStyle: { color: '#7856ff', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 42
    }]
  })

  const postTrend = contentStats.value.dailyPostTrend || []
  initChart(postTrendChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 26, bottom: 28 },
    xAxis: { type: 'category', data: postTrend.map(i => i.date) },
    yAxis: { type: 'value' },
    series: [lineSeries('发帖数', postTrend.map(i => i.count || 0), '#1d9bf0', true)]
  })

  const hourly = behaviorStats.value.hourlyDistribution || []
  initChart(hourlyChart, {
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 20, top: 26, bottom: 28 },
    xAxis: { type: 'category', data: hourly.map(i => `${i.hour}:00`) },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: hourly.map(i => i.count || 0),
      itemStyle: { color: '#00ba7c', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 20
    }]
  })
}

const lineSeries = (name, data, color, area = false) => ({
  name,
  type: 'line',
  smooth: true,
  symbolSize: 6,
  data,
  itemStyle: { color },
  lineStyle: { width: 3 },
  areaStyle: area ? { opacity: 0.12 } : undefined
})

const pieOption = (data, colors) => ({
  tooltip: { trigger: 'item' },
  color: colors,
  legend: { bottom: 0 },
  series: [{
    type: 'pie',
    radius: ['45%', '72%'],
    center: ['50%', '44%'],
    data
  }]
})

const loadDashboard = async () => {
  loading.value = true
  try {
    const res = await api.get('/analytics/dashboard', { params: { range: selectedRange.value } })
    dashboard.value = res.data
    await nextTick()
    renderCharts()
  } catch (e) {
    console.error('Failed to load analytics dashboard', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeHandler)
  disposeCharts()
})
</script>

<style scoped>
.analytics-view {
  max-width: 1220px;
  margin: 0 auto;
  padding: 24px;
  color: #0f1419;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 800;
}

.page-desc {
  margin: 6px 0 0;
  color: #536471;
  font-size: 14px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.range-note {
  margin-bottom: 14px;
  color: #536471;
  font-size: 13px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.metric-card {
  border: 1px solid #e1e8ed;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.metric-card.primary {
  background: #e8f5fe;
  border-color: #b9e2fb;
}

.metric-card.revenue {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.metric-label {
  display: inline-flex;
  font-size: 13px;
  color: #536471;
  font-weight: 600;
  cursor: help;
}

.metric-value {
  font-size: 28px;
  font-weight: 800;
  margin-top: 6px;
}

.metric-sub {
  font-size: 12px;
  color: #536471;
  margin-top: 4px;
}

.charts-grid {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.charts-grid.two {
  grid-template-columns: 1fr 1fr;
}

.charts-grid.content-row {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  background: #fff;
  border: 1px solid #e1e8ed;
  border-radius: 8px;
  padding: 16px;
  min-width: 0;
  margin-bottom: 16px;
}

.source-panel {
  margin-bottom: 16px;
}

.source-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.source-card {
  padding: 12px;
  border-radius: 8px;
  background: #f7f9fa;
}

.source-top {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  color: #536471;
}

.source-top strong {
  color: #0f1419;
  font-size: 16px;
}

.source-bar {
  height: 7px;
  background: #e1e8ed;
  border-radius: 999px;
  overflow: hidden;
  margin: 10px 0 8px;
}

.source-bar div {
  height: 100%;
  background: #1d9bf0;
  border-radius: inherit;
}

.source-card p {
  margin: 0;
  color: #536471;
  font-size: 12px;
  line-height: 1.45;
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-header.compact {
  margin-top: 0;
}

.panel-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 800;
}

.panel-header span {
  font-size: 12px;
  color: #536471;
  white-space: nowrap;
}

.chart {
  width: 100%;
  height: 280px;
}

.chart.tall {
  height: 320px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 280px;
  overflow: auto;
}

.rank-item {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 9px 10px;
  border-radius: 8px;
  background: #f7f9fa;
}

.rank-index {
  font-weight: 800;
  color: #00ba7c;
  width: 34px;
}

.rank-index.blue {
  color: #1d9bf0;
}

.rank-main {
  min-width: 0;
}

.rank-name {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-meta {
  font-size: 12px;
  color: #536471;
}

.bottom-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.post-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.feedback-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feedback-item,
.mini-rank-item {
  display: flex;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f7f9fa;
  border-radius: 8px;
}

.feedback-item span,
.mini-rank-item span,
.ad-summary-grid span {
  color: #536471;
  font-size: 13px;
}

.panel-divider {
  height: 1px;
  background: #e1e8ed;
  margin: 18px 0;
}

.ad-summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 14px;
}

.ad-summary-grid div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 8px;
  background: #f0fdf4;
}

.mini-rank {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.loading-state {
  min-height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 12px;
  color: #536471;
}

@media (max-width: 980px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-actions {
    justify-content: flex-start;
  }

  .overview-grid,
  .source-grid,
  .charts-grid.two,
  .charts-grid.content-row,
  .bottom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
