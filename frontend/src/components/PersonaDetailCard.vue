<template>
  <div class="persona-detail" v-if="persona">

    <!-- 用户分型三角图 -->
    <div class="section type-section" v-if="persona.userTypeDetail">
      <h4>🧬 用户行为分型</h4>
      <div class="type-content">
        <div ref="radarChart" class="radar-chart"></div>
        <div class="type-label">
          <span class="type-badge" :class="persona.userTypeDetail.type?.toLowerCase()">
            {{ persona.userTypeDetail.label || persona.userTypeDetail.type }}
          </span>
        </div>
      </div>
    </div>

    <!-- 活跃度等级 -->
    <div class="section activity-section" v-if="persona.activityScore !== undefined">
      <h4>⚡ 活跃度等级</h4>
      <div class="activity-content">
        <div class="activity-ring">
          <svg viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="42" fill="none" stroke="#e1e8ed" stroke-width="8" />
            <circle cx="50" cy="50" r="42" fill="none" 
              :stroke="activityColor" stroke-width="8" stroke-linecap="round"
              :stroke-dasharray="activityDash" stroke-dashoffset="0"
              transform="rotate(-90 50 50)" />
          </svg>
          <div class="ring-text">
            <span class="ring-score">{{ persona.activityScore }}</span>
            <span class="ring-label">分</span>
          </div>
        </div>
        <div class="activity-badge" :class="persona.activityLevel?.toLowerCase().replace(' ', '-')">
          {{ persona.activityLevel }}
        </div>
      </div>
    </div>

    <!-- 兴趣衰减条 -->
    <div class="section interest-section" v-if="persona.interestDecay && persona.interestDecay.length">
      <h4>📉 兴趣衰减评分</h4>
      <div class="interest-list">
        <div class="interest-item" v-for="item in persona.interestDecay" :key="item.tag">
          <span class="int-tag">{{ item.tag }}</span>
          <div class="int-bar-wrap">
            <div class="int-bar" :style="{ width: (item.score * 100) + '%' }"></div>
          </div>
          <span class="int-score">{{ item.score }}</span>
          <span class="int-trend" :class="item.trend">
            {{ item.trend === 'rising' ? '📈' : item.trend === 'falling' ? '📉' : '➡️' }}
          </span>
        </div>
      </div>
    </div>

    <!-- 活跃时段热力图 -->
    <div class="section time-section" v-if="persona.hourlyDistribution">
      <h4>🕐 活跃时段分布</h4>
      <div class="time-content">
        <div class="heatmap">
          <div class="heat-cell" v-for="h in persona.hourlyDistribution" :key="h.hour"
               :style="{ background: heatColor(h.count) }"
               :title="h.hour + ':00 — ' + h.count + '次'"
               :class="{ peak: persona.peakHours && persona.peakHours.includes(h.hour) }">
            <span class="cell-hour" v-if="h.hour % 4 === 0">{{ h.hour }}</span>
          </div>
        </div>
        <div class="night-owl" v-if="persona.nightOwlIndex !== undefined">
          🦉 夜猫子指数：<strong>{{ Math.round(persona.nightOwlIndex * 100) }}%</strong>
        </div>
      </div>
    </div>

    <!-- 内容偏好 -->
    <div class="section pref-section" v-if="persona.contentPreference">
      <h4>📖 内容偏好</h4>
      <div class="pref-tags">
        <span class="pref-tag">
          📏 {{ prefLengthLabel }}
        </span>
        <span class="pref-tag">
          🖼️ 图片偏好 {{ Math.round((persona.contentPreference.imagePreference || 0) * 100) }}%
        </span>
        <span class="pref-tag">
          🌐 话题多样性 {{ Math.round((persona.contentPreference.topicDiversity || 0) * 100) }}%
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  persona: { type: Object, default: () => ({}) }
})

const radarChart = ref(null)
let chartInstance = null

const activityColor = computed(() => {
  const level = props.persona?.activityLevel
  if (level === 'Power User') return '#FFAD1F'
  if (level === 'High') return '#17BF63'
  if (level === 'Medium') return '#1DA1F2'
  return '#AAB8C2'
})

const activityDash = computed(() => {
  const score = props.persona?.activityScore || 0
  const circumference = 2 * Math.PI * 42
  const filled = circumference * score / 100
  return `${filled} ${circumference}`
})

const prefLengthLabel = computed(() => {
  const len = props.persona?.contentPreference?.avgReadLength
  if (len === 'long') return '长文爱好者'
  if (len === 'medium') return '中等篇幅'
  if (len === 'short') return '快餐阅读'
  return '未知'
})

const heatColor = (count) => {
  if (!count) return '#f0f0f0'
  const maxCount = Math.max(...(props.persona?.hourlyDistribution?.map(h => h.count) || [1]))
  const intensity = count / maxCount
  const r = Math.round(29 + (1 - intensity) * 220)
  const g = Math.round(161 + (1 - intensity) * 80)
  const b = Math.round(242 + (1 - intensity) * 10)
  return `rgba(${r}, ${g}, ${b}, ${0.2 + intensity * 0.8})`
}

const renderRadar = () => {
  if (!radarChart.value || !props.persona?.userTypeDetail) return
  if (!chartInstance) {
    chartInstance = echarts.init(radarChart.value)
  }

  const d = props.persona.userTypeDetail
  chartInstance.setOption({
    radar: {
      indicator: [
        { name: 'Creator\n创作者', max: 1 },
        { name: 'Consumer\n消费者', max: 1 },
        { name: 'Interactor\n互动者', max: 1 }
      ],
      shape: 'circle',
      splitNumber: 4,
      axisName: { color: '#536471', fontSize: 11 }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [d.creatorScore || 0, d.consumerScore || 0, d.interactorScore || 0],
        areaStyle: { color: 'rgba(29,161,242,0.25)' },
        lineStyle: { color: '#1DA1F2', width: 2 },
        itemStyle: { color: '#1DA1F2' }
      }]
    }]
  })
}

onMounted(() => nextTick(renderRadar))
watch(() => props.persona, () => nextTick(renderRadar), { deep: true })
</script>

<style scoped>
.persona-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section {
  background: white;
  border-radius: 12px;
  padding: 16px;
  border: 1px solid #e1e8ed;
}

.section h4 {
  font-size: 14px;
  font-weight: 700;
  color: #0f1419;
  margin: 0 0 12px 0;
}

/* 用户分型 */
.type-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.radar-chart { width: 180px; height: 160px; }

.type-badge {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 700;
}

.type-badge.creator { background: #FFF3E0; color: #F57C00; }
.type-badge.interactor { background: #E8F5FE; color: #1DA1F2; }
.type-badge.consumer { background: #E8F5E9; color: #43A047; }

/* 活跃度 */
.activity-content {
  display: flex;
  align-items: center;
  gap: 20px;
}

.activity-ring {
  width: 80px;
  height: 80px;
  position: relative;
}

.activity-ring svg { width: 100%; height: 100%; }

.ring-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.ring-score { font-size: 20px; font-weight: 800; color: #0f1419; }
.ring-label { font-size: 11px; color: #536471; display: block; }

.activity-badge {
  padding: 4px 12px;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 700;
}

.activity-badge.low { background: #f0f0f0; color: #AAB8C2; }
.activity-badge.medium { background: #E8F5FE; color: #1DA1F2; }
.activity-badge.high { background: #E8F5E9; color: #17BF63; }
.activity-badge.power-user { background: #FFF3E0; color: #FFAD1F; }

/* 兴趣衰减 */
.interest-list { display: flex; flex-direction: column; gap: 8px; }

.interest-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.int-tag {
  min-width: 60px;
  font-size: 13px;
  font-weight: 600;
  color: #0f1419;
}

.int-bar-wrap {
  flex: 1;
  height: 8px;
  background: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
}

.int-bar {
  height: 100%;
  background: linear-gradient(90deg, #1DA1F2, #0d8bd9);
  border-radius: 4px;
  transition: width 0.5s;
}

.int-score {
  min-width: 30px;
  text-align: right;
  font-size: 12px;
  font-weight: 700;
  color: #536471;
}

.int-trend { font-size: 14px; min-width: 20px; }

/* 热力图 */
.heatmap {
  display: flex;
  gap: 2px;
  margin-bottom: 8px;
}

.heat-cell {
  flex: 1;
  height: 32px;
  border-radius: 4px;
  position: relative;
  min-width: 0;
  cursor: default;
}

.heat-cell.peak {
  border: 2px solid #1DA1F2;
}

.cell-hour {
  position: absolute;
  bottom: -16px;
  left: 50%;
  transform: translateX(-50%);
  font-size: 10px;
  color: #AAB8C2;
}

.night-owl {
  font-size: 13px;
  color: #536471;
  margin-top: 8px;
}

/* 内容偏好 */
.pref-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pref-tag {
  background: #f7f9fa;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  color: #536471;
  font-weight: 500;
}
</style>
