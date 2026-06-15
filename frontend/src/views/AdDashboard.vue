<template>
  <div class="ad-dashboard">
    <div class="page-header">
      <div>
        <h2 class="page-title">📊 广告效果报表</h2>
        <p class="page-desc">模拟 Google AdSense 广告投放数据，展示推荐系统在商业变现中的应用</p>
      </div>
      <button class="add-ad-btn" @click="openCreateDialog">+ 添加广告</button>
    </div>

    <!-- 投放设置 -->
    <div class="config-card">
      <h3 class="config-title">⚙️ 投放设置</h3>
      <div class="config-body">
        <div class="config-item">
          <div class="config-label">
            <span>全局广告开关</span>
            <span class="config-hint">关闭后信息流中不再展示广告</span>
          </div>
          <el-switch v-model="adConfig.globalEnabled" @change="saveConfig" />
        </div>
        <div class="config-item">
          <div class="config-label">
            <span>广告间隔</span>
            <span class="config-hint">每 <strong>{{ adConfig.adInterval }}</strong> 条帖子后插入一条广告</span>
          </div>
          <el-slider v-model="adConfig.adInterval" :min="2" :max="20" :step="1" :disabled="!adConfig.globalEnabled" style="width: 220px" />
        </div>
        <div class="config-item">
          <div class="config-label">
            <span>单页最大广告数</span>
            <span class="config-hint">单次加载信息流最多展示 <strong>{{ adConfig.maxAdsPerPage }}</strong> 条广告</span>
          </div>
          <el-slider v-model="adConfig.maxAdsPerPage" :min="1" :max="10" :step="1" :disabled="!adConfig.globalEnabled" style="width: 220px" />
        </div>
        <div class="config-actions">
          <el-button type="primary" :loading="savingConfig" @click="saveConfig" :disabled="!configChanged">保存设置</el-button>
          <span v-if="configSaved" class="config-saved-tip">✅ 已保存</span>
        </div>
      </div>
    </div>

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
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 添加/编辑广告弹窗 -->
    <el-dialog 
      v-model="dialogVisible" 
      :title="isEditing ? '编辑广告' : '添加广告'" 
      width="560px"
      destroy-on-close
    >
      <el-form :model="adForm" label-width="90px" label-position="left">
        <el-form-item label="广告标题">
          <el-input v-model="adForm.title" placeholder="请输入广告标题" />
        </el-form-item>
        <el-form-item label="广告描述">
          <el-input v-model="adForm.description" type="textarea" :rows="3" placeholder="请输入广告描述" />
        </el-form-item>
        <el-form-item label="广告主">
          <el-input v-model="adForm.advertiser" placeholder="如：TechCorp" />
        </el-form-item>
        <el-form-item label="定向标签">
          <el-input v-model="adForm.targetTags" placeholder="逗号分隔，如：Tech,AI,Programming" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="adForm.category" placeholder="选择分类" style="width: 100%">
            <el-option label="Tech" value="Tech" />
            <el-option label="Life" value="Life" />
            <el-option label="Education" value="Education" />
            <el-option label="Sports" value="Sports" />
            <el-option label="Finance" value="Finance" />
          </el-select>
        </el-form-item>
        <el-form-item label="出价(CPM)">
          <el-input-number v-model="adForm.bidPrice" :min="0.1" :step="0.5" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="目标URL">
          <el-input v-model="adForm.targetUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="adForm.imageUrl" placeholder="可选，广告配图链接" />
        </el-form-item>
        <el-form-item label="启用状态">
          <el-switch v-model="adForm.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAd" :loading="submitting">{{ isEditing ? '保存' : '创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed, reactive, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import * as echarts from 'echarts'

const stats = ref(null)
const loading = ref(false)
const ctrChart = ref(null)
const pieChart = ref(null)
const sortKey = ref('ctr')
const sortOrder = ref('descending')

// Ad config state
const adConfig = reactive({ adInterval: 5, maxAdsPerPage: 3, globalEnabled: true })
const originalConfig = ref({ adInterval: 5, maxAdsPerPage: 3, globalEnabled: true })
const savingConfig = ref(false)
const configSaved = ref(false)
const configChanged = computed(() =>
  adConfig.adInterval !== originalConfig.value.adInterval ||
  adConfig.maxAdsPerPage !== originalConfig.value.maxAdsPerPage ||
  adConfig.globalEnabled !== originalConfig.value.globalEnabled
)

const loadAdConfig = async () => {
  try {
    const res = await api.get('/ads/config')
    const cfg = res.data
    adConfig.adInterval = cfg.adInterval ?? 5
    adConfig.maxAdsPerPage = cfg.maxAdsPerPage ?? 3
    adConfig.globalEnabled = cfg.globalEnabled !== false
    originalConfig.value = { ...adConfig }
  } catch (e) {
    console.error('Load ad config failed', e)
  }
}

const saveConfig = async () => {
  savingConfig.value = true
  configSaved.value = false
  try {
    const res = await api.put('/ads/config', {
      adInterval: adConfig.adInterval,
      maxAdsPerPage: adConfig.maxAdsPerPage,
      globalEnabled: adConfig.globalEnabled
    })
    const cfg = res.data
    adConfig.adInterval = cfg.adInterval
    adConfig.maxAdsPerPage = cfg.maxAdsPerPage
    adConfig.globalEnabled = cfg.globalEnabled
    originalConfig.value = { ...adConfig }
    configSaved.value = true
    ElMessage.success('投放设置已保存')
    setTimeout(() => configSaved.value = false, 2000)
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    savingConfig.value = false
  }
}

// Dialog state
const dialogVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const submitting = ref(false)

const defaultForm = () => ({
  title: '',
  description: '',
  advertiser: '',
  targetTags: '',
  category: 'Tech',
  bidPrice: 5.0,
  targetUrl: '',
  imageUrl: '',
  active: true
})
const adForm = ref(defaultForm())

const openCreateDialog = () => {
  isEditing.value = false
  editingId.value = null
  adForm.value = defaultForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEditing.value = true
  editingId.value = row.id
  adForm.value = {
    title: row.title || '',
    description: row.description || '',
    advertiser: row.advertiser || '',
    targetTags: row.targetTags || '',
    category: row.category || 'Tech',
    bidPrice: row.bidPrice || 5.0,
    targetUrl: row.targetUrl || '',
    imageUrl: row.imageUrl || '',
    active: row.active !== false
  }
  dialogVisible.value = true
}

const submitAd = async () => {
  if (!adForm.value.title.trim()) {
    ElMessage.warning('请输入广告标题')
    return
  }
  submitting.value = true
  try {
    if (isEditing.value) {
      await api.put(`/ads/${editingId.value}`, adForm.value)
      ElMessage.success('广告已更新')
    } else {
      await api.post('/ads', adForm.value)
      ElMessage.success('广告已创建')
    }
    dialogVisible.value = false
    loadStats() // Refresh
  } catch (e) {
    console.error('Submit failed', e)
  } finally {
    submitting.value = false
  }
}

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

onMounted(() => {
  loadStats()
  loadAdConfig()
})
</script>

<style scoped>
.ad-dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
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
  margin: 0;
}

.add-ad-btn {
  background: #1d9bf0;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: background 0.2s;
}
.add-ad-btn:hover {
  background: #1a8cd8;
}

.config-card {
  background: white;
  border: 1px solid #e1e8ed;
  border-radius: 16px;
  padding: 20px 24px;
  margin-bottom: 20px;
}
.config-title {
  font-size: 16px;
  font-weight: 700;
  color: #0f1419;
  margin: 0 0 16px 0;
}
.config-body {
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.config-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.config-label span:first-child {
  font-size: 14px;
  font-weight: 600;
  color: #0f1419;
}
.config-hint {
  font-size: 12px !important;
  font-weight: 400 !important;
  color: #536471 !important;
}
.config-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
}
.config-saved-tip {
  font-size: 13px;
  color: #00ba7c;
  font-weight: 500;
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
