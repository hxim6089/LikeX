<template>
  <div class="compare-view">
    <h2 class="page-title">个性化推荐效果验证</h2>
    <p class="page-desc">验证推荐算法有效性 + 展示不同用户获得不同推荐（千人千面）</p>

    <!-- ===== 第一部分：当前用户行为画像 ===== -->
    <div class="section" v-if="currentProfile">
      <div class="section-header">
        <h3>当前用户行为画像</h3>
        <span class="stage-badge" :class="'stage-' + currentProfile.userStage">
          {{ stageLabel(currentProfile.userStage) }}
        </span>
      </div>

      <div class="profile-card">
        <div class="profile-top">
          <el-avatar :size="48" :src="currentProfile.avatarUrl || defaultAvatar" />
          <div class="profile-info">
            <div class="profile-name">{{ currentProfile.username }}</div>
            <div class="profile-handle">{{ currentProfile.handle }}</div>
          </div>
        </div>
        <div class="profile-summary" v-if="currentProfile.profileSummary">
          {{ currentProfile.profileSummary }}
        </div>

        <div class="profile-metrics">
          <div class="metric">
            <div class="metric-label">互动风格</div>
            <div class="metric-value">{{ styleLabel(currentProfile.engagementStyle) }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">内容深度</div>
            <div class="metric-value">{{ depthLabel(currentProfile.depthPreference) }}</div>
          </div>
          <div class="metric">
            <div class="metric-label">新鲜度偏好</div>
            <div class="metric-value">{{ Math.round(currentProfile.freshnessPreference * 100) }}%</div>
          </div>
          <div class="metric">
            <div class="metric-label">探索度</div>
            <div class="metric-value">{{ Math.round(currentProfile.explorationRate * 100) }}%</div>
          </div>
        </div>

        <!-- 话题偏好 -->
        <div class="topic-tags" v-if="currentProfile.topTopics && currentProfile.topTopics.length">
          <span class="topic-label">话题偏好：</span>
          <span class="topic-tag" v-for="topic in currentProfile.topTopics" :key="topic.name">
            {{ topic.name.startsWith('_cat:') ? topic.name.slice(5) : topic.name }}
            <span class="topic-score">{{ topic.score }}</span>
          </span>
        </div>

        <!-- 动态权重 -->
        <div class="weights-section" v-if="currentProfile.dynamicWeights">
          <div class="weights-title">自动生成的推荐权重</div>
          <div class="weights-bars">
            <div class="weight-bar-item" v-for="(val, key) in currentProfile.dynamicWeights" :key="key">
              <span class="wb-label">{{ weightLabel(key) }}</span>
              <div class="wb-track">
                <div class="wb-fill" :style="{ width: weightPercent(key, val) + '%' }"></div>
              </div>
              <span class="wb-value">{{ val }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ===== 第二部分：千人千面对比 ===== -->
    <div class="section">
      <div class="section-header">
        <h3>千人千面对比</h3>
        <el-select v-model="compareUserId" placeholder="选择对比用户" size="default" style="width: 200px" @change="loadProfiles">
          <el-option v-for="u in userList" :key="u.id" :label="u.username" :value="u.id" :disabled="u.id === userId" />
        </el-select>
      </div>

      <!-- 双用户对比 -->
      <div class="dual-compare" v-if="compareProfile">
        <!-- 权重雷达对比 -->
        <div class="chart-card">
          <div class="chart-title">动态权重对比</div>
          <div ref="radarChart" class="radar-chart"></div>
        </div>

        <!-- 画像属性对比 -->
        <div class="attr-compare">
          <div class="attr-row header-row">
            <div class="attr-cell user-cell">{{ currentProfile?.username }}</div>
            <div class="attr-cell label-cell">维度</div>
            <div class="attr-cell user-cell">{{ compareProfile.username }}</div>
          </div>
          <div class="attr-row">
            <div class="attr-cell" :class="'stage-' + currentProfile?.userStage">{{ stageLabel(currentProfile?.userStage) }}</div>
            <div class="attr-cell label-cell">用户阶段</div>
            <div class="attr-cell" :class="'stage-' + compareProfile.userStage">{{ stageLabel(compareProfile.userStage) }}</div>
          </div>
          <div class="attr-row">
            <div class="attr-cell">{{ styleLabel(currentProfile?.engagementStyle) }}</div>
            <div class="attr-cell label-cell">互动风格</div>
            <div class="attr-cell">{{ styleLabel(compareProfile.engagementStyle) }}</div>
          </div>
          <div class="attr-row">
            <div class="attr-cell">{{ depthLabel(currentProfile?.depthPreference) }}</div>
            <div class="attr-cell label-cell">内容深度</div>
            <div class="attr-cell">{{ depthLabel(compareProfile.depthPreference) }}</div>
          </div>
          <div class="attr-row">
            <div class="attr-cell">{{ Math.round((currentProfile?.freshnessPreference || 0) * 100) }}%</div>
            <div class="attr-cell label-cell">新鲜度偏好</div>
            <div class="attr-cell">{{ Math.round(compareProfile.freshnessPreference * 100) }}%</div>
          </div>
          <div class="attr-row">
            <div class="attr-cell">{{ Math.round((currentProfile?.explorationRate || 0) * 100) }}%</div>
            <div class="attr-cell label-cell">探索度</div>
            <div class="attr-cell">{{ Math.round(compareProfile.explorationRate * 100) }}%</div>
          </div>
        </div>

        <!-- Top5 推荐对比 -->
        <div class="rec-compare">
          <div class="rec-column">
            <div class="rec-col-header me">{{ currentProfile?.username }} 的 Top 5</div>
            <div v-for="item in (currentProfile?.topRecommendations || [])" :key="'me-' + item.rank" class="rec-item">
              <span class="rec-rank">#{{ item.rank }}</span>
              <span class="rec-text">{{ truncate(item.content?.content, 50) }}</span>
              <span class="rec-score">{{ item.scoreBreakdown?.finalScore }}</span>
            </div>
            <div class="rec-reasons" v-if="currentProfile?.topRecommendations?.[0]?.scoreBreakdown?.recommendReasons">
              Top 1 推荐理由：
              <span class="reason-tag" v-for="(r, i) in currentProfile.topRecommendations[0].scoreBreakdown.recommendReasons" :key="i">{{ r }}</span>
            </div>
          </div>
          <div class="rec-column">
            <div class="rec-col-header other">{{ compareProfile.username }} 的 Top 5</div>
            <div v-for="item in (compareProfile.topRecommendations || [])" :key="'other-' + item.rank" class="rec-item">
              <span class="rec-rank">#{{ item.rank }}</span>
              <span class="rec-text">{{ truncate(item.content?.content, 50) }}</span>
              <span class="rec-score">{{ item.scoreBreakdown?.finalScore }}</span>
            </div>
            <div class="rec-reasons" v-if="compareProfile.topRecommendations?.[0]?.scoreBreakdown?.recommendReasons">
              Top 1 推荐理由：
              <span class="reason-tag" v-for="(r, i) in compareProfile.topRecommendations[0].scoreBreakdown.recommendReasons" :key="i">{{ r }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-compare" v-else>
        <p>请在右上角选择一个对比用户，查看不同用户如何获得不同的推荐策略和内容排序</p>
      </div>
    </div>

    <!-- ===== 第三部分：推荐 vs 时间序对比（保留） ===== -->
    <div class="section">
      <div class="section-header">
        <h3>推荐算法 vs 时间倒序</h3>
        <el-button size="small" :type="showTuner ? 'primary' : 'default'" @click="showTuner = !showTuner">
          {{ showTuner ? '收起调参' : '参数调节' }}
        </el-button>
      </div>

      <WeightTuner v-if="showTuner" @apply="applyTunedWeights" />

      <PipelineFunnel :stats="pipelineStats" v-if="pipelineStats" />

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

      <div class="chart-section" v-if="stats">
        <div ref="barChart" class="bar-chart"></div>
      </div>

      <div class="compare-columns" v-if="!feedLoading">
        <div class="compare-column">
          <div class="column-header personalized">
            <span>个性化推荐</span>
            <span class="count">{{ personalizedFeed.length }} 篇</span>
          </div>
          <div class="feed-list">
            <div v-for="item in personalizedFeed" :key="'p-' + item.rank" class="compare-card">
              <div class="card-rank">#{{ item.rank }}</div>
              <div class="card-body">
                <div class="card-author">{{ item.content?.author?.username || 'Unknown' }}</div>
                <div class="card-text">{{ truncate(item.content?.content, 80) }}</div>
                <div class="card-score">
                  <span class="score-badge">{{ item.scoreBreakdown?.finalScore || 0 }}</span>
                  <span class="reason-tag small" v-for="(r, i) in (item.scoreBreakdown?.recommendReasons || []).slice(0, 2)" :key="i">{{ r }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="compare-column">
          <div class="column-header chronological">
            <span>时间倒序</span>
            <span class="count">{{ chronologicalFeed.length }} 篇</span>
          </div>
          <div class="feed-list">
            <div v-for="item in chronologicalFeed" :key="'c-' + item.rank" class="compare-card">
              <div class="card-rank dim">#{{ item.rank }}</div>
              <div class="card-body">
                <div class="card-author">{{ item.content?.author?.username || 'Unknown' }}</div>
                <div class="card-text">{{ truncate(item.content?.content, 80) }}</div>
                <div class="card-score">
                  <span class="score-badge dim">{{ item.scoreBreakdown?.finalScore || 0 }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="feedLoading" class="loading-state">
        <el-icon class="is-loading" :size="32"><Loading /></el-icon>
        <span>正在计算推荐对比...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import api from '../api'
import * as echarts from 'echarts'
import PipelineFunnel from '../components/PipelineFunnel.vue'
import WeightTuner from '../components/WeightTuner.vue'

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const currentProfile = ref(null)
const compareProfile = ref(null)
const compareUserId = ref(null)
const userList = ref([])

const showTuner = ref(false)
const personalizedFeed = ref([])
const chronologicalFeed = ref([])
const stats = ref(null)
const pipelineStats = ref(null)
const feedLoading = ref(false)

const barChart = ref(null)
const radarChart = ref(null)
let barChartInstance = null
let radarChartInstance = null

const getCurrentUserId = () => {
  try { return JSON.parse(localStorage.getItem('user'))?.id } catch { return null }
}
const userId = getCurrentUserId()

const loadProfiles = async () => {
  if (!userId) return
  try {
    const params = { userId }
    if (compareUserId.value) params.compareUserId = compareUserId.value
    const res = await api.get('/compare/profiles', { params })
    currentProfile.value = res.data.currentUser
    if (res.data.compareUser) {
      compareProfile.value = res.data.compareUser
      nextTick(() => renderRadarChart())
    }
    userList.value = res.data.users || []
  } catch (e) {
    console.error('Profile load failed', e)
  }
}

const loadFeedCompare = async () => {
  if (!userId) return
  feedLoading.value = true
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
    feedLoading.value = false
  }
}

const applyTunedWeights = async (weights) => {
  if (!userId) return
  feedLoading.value = true
  try {
    const res = await api.get('/compare/feed', { params: { userId, ...weights } })
    personalizedFeed.value = res.data.personalized || []
    chronologicalFeed.value = res.data.chronological || []
    stats.value = res.data.stats || {}
    pipelineStats.value = res.data.pipelineStats || {}
    nextTick(() => renderBarChart())
  } catch (e) {
    console.error('Tuned feed load failed', e)
  } finally {
    feedLoading.value = false
  }
}

const renderRadarChart = () => {
  if (!radarChart.value || !currentProfile.value || !compareProfile.value) return
  if (!radarChartInstance) radarChartInstance = echarts.init(radarChart.value)

  const keys = ['wLike', 'wReply', 'wRepost', 'wTopicAffinity', 'wTrending', 'wSimilarity', 'wFreshness', 'wDepthMatch']
  const labels = ['点赞', '评论', '转发', '话题亲和', '热门', '相似度', '新鲜度', '深度匹配']

  const cw = currentProfile.value.dynamicWeights || {}
  const ow = compareProfile.value.dynamicWeights || {}

  const maxVals = keys.map(k => Math.max(cw[k] || 0, ow[k] || 0, 1))

  radarChartInstance.setOption({
    tooltip: {},
    legend: { data: [currentProfile.value.username, compareProfile.value.username], bottom: 0 },
    radar: {
      indicator: keys.map((k, i) => ({ name: labels[i], max: Math.ceil(maxVals[i] * 1.3) })),
      shape: 'circle',
      splitArea: { areaStyle: { color: ['#f8f9fa', '#fff'] } }
    },
    series: [{
      type: 'radar',
      data: [
        { value: keys.map(k => cw[k] || 0), name: currentProfile.value.username, areaStyle: { opacity: 0.2 } },
        { value: keys.map(k => ow[k] || 0), name: compareProfile.value.username, areaStyle: { opacity: 0.2 } }
      ]
    }]
  })
}

const renderBarChart = () => {
  if (!barChart.value || !stats.value) return
  if (!barChartInstance) barChartInstance = echarts.init(barChart.value)
  const s = stats.value
  barChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['个性化推荐', '时间倒序'], top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['平均分', '标签命中率(%)', '互动量', 'TF-IDF 相似度'] },
    yAxis: { type: 'value' },
    series: [
      {
        name: '个性化推荐', type: 'bar', barGap: '10%',
        itemStyle: { color: '#1DA1F2', borderRadius: [4, 4, 0, 0] },
        data: [s.personalizedAvgScore || 0, Math.round((s.personalizedTagHitRate || 0) * 100), s.personalizedEngagementAvg || 0, s.personalizedSimilarityAvg || 0]
      },
      {
        name: '时间倒序', type: 'bar',
        itemStyle: { color: '#AAB8C2', borderRadius: [4, 4, 0, 0] },
        data: [s.chronologicalAvgScore || 0, Math.round((s.chronologicalTagHitRate || 0) * 100), s.chronologicalEngagementAvg || 0, s.chronologicalSimilarityAvg || 0]
      }
    ]
  })
}

const truncate = (text, len) => (!text ? '' : text.length > len ? text.substring(0, len) + '...' : text)

const stageLabel = (s) => ({ ACTIVE: '活跃用户', BEGINNER: '初级用户', COLD_START: '冷启动' }[s] || '未知')
const styleLabel = (s) => ({ commenter: '评论达人', liker: '点赞达人', silent_reader: '深度阅读', balanced: '均衡互动' }[s] || s)
const depthLabel = (s) => ({ short: '偏好短内容', medium: '中等长度', long: '偏好长文' }[s] || s)

const weightLabel = (key) => ({
  wLike: '点赞', wReply: '评论', wRepost: '转发', wTopicAffinity: '话题亲和',
  wAuthorAffinity: '作者亲密', wTrending: '热门', wSimilarity: '相似度',
  wFreshness: '新鲜度', wDepthMatch: '深度匹配', explorationFactor: '探索因子'
}[key] || key)

const weightPercent = (key, val) => {
  const maxMap = { wLike: 3, wReply: 3, wRepost: 4, wTopicAffinity: 120, wAuthorAffinity: 80, wTrending: 120, wSimilarity: 120, wFreshness: 2.5, wDepthMatch: 60, explorationFactor: 0.4 }
  return Math.min(100, (val / (maxMap[key] || 100)) * 100)
}

onMounted(() => {
  loadProfiles()
  loadFeedCompare()
})
</script>

<style scoped>
.compare-view { max-width: 1200px; margin: 0 auto; padding: 20px; }
.page-title { font-size: 24px; font-weight: 800; color: #0f1419; margin: 0 0 4px; }
.page-desc { font-size: 14px; color: #536471; margin: 0 0 24px; }

.section { margin-bottom: 32px; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.section-header h3 { font-size: 18px; font-weight: 700; color: #0f1419; margin: 0; }

.stage-badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.stage-ACTIVE { background: #d4edda; color: #155724; }
.stage-BEGINNER { background: #fff3cd; color: #856404; }
.stage-COLD_START { background: #d6d8db; color: #383d41; }

.profile-card { background: white; border-radius: 16px; border: 1px solid #e1e8ed; padding: 20px; }
.profile-top { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.profile-name { font-weight: 700; font-size: 16px; color: #0f1419; }
.profile-handle { font-size: 14px; color: #536471; }
.profile-summary { padding: 10px 14px; background: #f7f9fa; border-radius: 10px; font-size: 14px; color: #0f1419; margin-bottom: 16px; line-height: 1.6; }

.profile-metrics { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.metric { text-align: center; padding: 12px; background: #f8f9fa; border-radius: 10px; }
.metric-label { font-size: 12px; color: #536471; margin-bottom: 4px; }
.metric-value { font-size: 15px; font-weight: 700; color: #0f1419; }

.topic-tags { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 16px; }
.topic-label { font-size: 13px; color: #536471; font-weight: 500; }
.topic-tag { padding: 4px 10px; border-radius: 16px; background: #e8f4fd; color: #0c5460; font-size: 13px; display: flex; align-items: center; gap: 4px; }
.topic-score { font-size: 11px; font-weight: 700; color: #1DA1F2; }

.weights-section { border-top: 1px solid #e1e8ed; padding-top: 16px; }
.weights-title { font-size: 14px; font-weight: 600; color: #0f1419; margin-bottom: 12px; }
.weights-bars { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 24px; }
.weight-bar-item { display: flex; align-items: center; gap: 8px; }
.wb-label { font-size: 12px; color: #536471; width: 60px; text-align: right; flex-shrink: 0; }
.wb-track { flex: 1; height: 8px; background: #e9ecef; border-radius: 4px; overflow: hidden; }
.wb-fill { height: 100%; background: linear-gradient(90deg, #1DA1F2, #0d8bd9); border-radius: 4px; transition: width 0.3s; }
.wb-value { font-size: 12px; font-weight: 700; color: #0f1419; width: 40px; }

/* 千人千面对比 */
.dual-compare { display: flex; flex-direction: column; gap: 20px; }
.chart-card { background: white; border-radius: 16px; border: 1px solid #e1e8ed; padding: 20px; }
.chart-title { font-size: 15px; font-weight: 600; color: #0f1419; margin-bottom: 8px; }
.radar-chart { width: 100%; height: 340px; }

.attr-compare { background: white; border-radius: 16px; border: 1px solid #e1e8ed; overflow: hidden; }
.attr-row { display: grid; grid-template-columns: 1fr auto 1fr; border-bottom: 1px solid #f0f0f0; }
.attr-row:last-child { border-bottom: none; }
.attr-row.header-row { background: #f7f9fa; font-weight: 700; }
.attr-cell { padding: 10px 16px; text-align: center; font-size: 14px; color: #0f1419; }
.attr-cell.label-cell { background: #f7f9fa; color: #536471; font-size: 13px; min-width: 100px; font-weight: 500; }
.attr-cell.user-cell { font-weight: 700; }

.rec-compare { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.rec-column { background: white; border-radius: 16px; border: 1px solid #e1e8ed; overflow: hidden; }
.rec-col-header { padding: 12px 16px; font-weight: 700; font-size: 14px; }
.rec-col-header.me { background: linear-gradient(135deg, #E8F5FE, #D3ECFD); color: #1DA1F2; }
.rec-col-header.other { background: linear-gradient(135deg, #fce4ec, #f8bbd0); color: #c62828; }
.rec-item { display: flex; align-items: center; gap: 8px; padding: 10px 16px; border-bottom: 1px solid #f0f0f0; font-size: 13px; }
.rec-rank { font-weight: 800; color: #1DA1F2; min-width: 28px; }
.rec-text { flex: 1; color: #0f1419; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rec-score { font-weight: 700; color: #536471; font-size: 12px; }
.rec-reasons { padding: 10px 16px; font-size: 12px; color: #536471; display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }

.reason-tag { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 11px; background: #e8f4fd; color: #0c5460; border: 1px solid #bee5eb; }
.reason-tag.small { font-size: 10px; padding: 1px 6px; }

.empty-compare { text-align: center; padding: 40px; background: white; border-radius: 16px; border: 1px solid #e1e8ed; color: #536471; }

/* 推荐 vs 时间序 */
.stats-bar { display: grid; grid-template-columns: repeat(5, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: white; border-radius: 12px; padding: 16px; text-align: center; border: 1px solid #e1e8ed; }
.stat-card.highlight { background: linear-gradient(135deg, #1DA1F2, #0d8bd9); color: white; border: none; }
.stat-card.highlight .stat-label, .stat-card.highlight .stat-sub { color: rgba(255,255,255,0.8); }
.stat-value { font-size: 28px; font-weight: 800; line-height: 1.2; }
.stat-label { font-size: 12px; color: #536471; margin-top: 4px; }
.stat-sub { font-size: 11px; color: #AAB8C2; margin-top: 2px; }

.chart-section { background: white; border-radius: 16px; padding: 16px; border: 1px solid #e1e8ed; margin-bottom: 20px; }
.bar-chart { width: 100%; height: 280px; }

.compare-columns { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.compare-column { background: white; border-radius: 16px; border: 1px solid #e1e8ed; overflow: hidden; }
.column-header { padding: 14px 16px; font-size: 15px; font-weight: 700; display: flex; justify-content: space-between; align-items: center; }
.column-header.personalized { background: linear-gradient(135deg, #E8F5FE, #D3ECFD); color: #1DA1F2; }
.column-header.chronological { background: #f7f9fa; color: #536471; }
.count { font-size: 13px; font-weight: 400; }
.feed-list { max-height: 600px; overflow-y: auto; }
.compare-card { display: flex; padding: 12px 16px; border-bottom: 1px solid #f0f0f0; gap: 12px; transition: background 0.2s; }
.compare-card:hover { background: #f7f9fa; }
.card-rank { font-size: 18px; font-weight: 800; color: #1DA1F2; min-width: 36px; }
.card-rank.dim { color: #AAB8C2; }
.card-body { flex: 1; min-width: 0; }
.card-author { font-weight: 700; font-size: 14px; color: #0f1419; }
.card-text { font-size: 13px; color: #536471; margin: 4px 0; line-height: 1.4; }
.card-score { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.score-badge { background: #E8F5FE; color: #1DA1F2; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.score-badge.dim { background: #f0f0f0; color: #AAB8C2; }

.loading-state { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 60px 0; color: #536471; }
</style>
