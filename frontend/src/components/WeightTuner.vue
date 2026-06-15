<template>
  <div class="weight-tuner">
    <div class="tuner-header">
      <h3>🎛️ 算法参数调节</h3>
      <el-button type="primary" size="small" @click="$emit('apply', currentWeights)" :icon="Refresh">
        重新计算
      </el-button>
    </div>

    <div class="tuner-sliders">
      <div class="slider-item" v-for="param in params" :key="param.key">
        <div class="slider-label">
          <span class="label-text">{{ param.label }}</span>
          <span class="label-value">{{ currentWeights[param.key].toFixed(1) }}</span>
        </div>
        <el-slider
          v-model="currentWeights[param.key]"
          :min="param.min"
          :max="param.max"
          :step="param.step"
          :show-tooltip="false"
        />
        <div class="slider-range">
          <span>{{ param.min }}</span>
          <span>{{ param.max }}</span>
        </div>
      </div>
    </div>

    <div class="tuner-presets">
      <span class="preset-label">预设方案：</span>
      <el-button size="small" @click="applyPreset('default')">默认</el-button>
      <el-button size="small" @click="applyPreset('engagement')">互动优先</el-button>
      <el-button size="small" @click="applyPreset('personalized')">个性化优先</el-button>
      <el-button size="small" @click="applyPreset('trending')">热门优先</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { Refresh } from '@element-plus/icons-vue'

const emit = defineEmits(['apply'])

const params = [
  { key: 'wLike', label: '点赞权重', min: 0, max: 5, step: 0.1 },
  { key: 'wReply', label: '评论权重', min: 0, max: 5, step: 0.1 },
  { key: 'wRepost', label: '转发权重', min: 0, max: 5, step: 0.1 },
  { key: 'wPersonal', label: '个性化加成', min: 0, max: 300, step: 10 },
  { key: 'wTrending', label: '热门话题加成', min: 0, max: 200, step: 5 },
  { key: 'wSimilarity', label: 'TF-IDF 相似度加成', min: 0, max: 200, step: 5 }
]

const currentWeights = reactive({
  wLike: 0.5,
  wReply: 1.2,
  wRepost: 2.0,
  wPersonal: 100,
  wTrending: 50,
  wSimilarity: 80
})

const presets = {
  default: { wLike: 0.5, wReply: 1.2, wRepost: 2.0, wPersonal: 100, wTrending: 50, wSimilarity: 80 },
  engagement: { wLike: 2.0, wReply: 3.0, wRepost: 4.0, wPersonal: 30, wTrending: 20, wSimilarity: 20 },
  personalized: { wLike: 0.3, wReply: 0.5, wRepost: 0.5, wPersonal: 200, wTrending: 10, wSimilarity: 150 },
  trending: { wLike: 0.5, wReply: 1.0, wRepost: 1.5, wPersonal: 50, wTrending: 200, wSimilarity: 30 }
}

const applyPreset = (name) => {
  const preset = presets[name]
  if (preset) {
    Object.assign(currentWeights, preset)
    emit('apply', { ...currentWeights })
  }
}
</script>

<style scoped>
.weight-tuner {
  background: white;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e1e8ed;
  margin-bottom: 16px;
}

.tuner-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.tuner-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: #0f1419;
  margin: 0;
}

.tuner-sliders {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.slider-item {
  padding: 0 4px;
}

.slider-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.label-text {
  font-size: 13px;
  color: #536471;
  font-weight: 500;
}

.label-value {
  font-size: 14px;
  font-weight: 700;
  color: #1DA1F2;
  min-width: 40px;
  text-align: right;
}

.slider-range {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #9ca3af;
  margin-top: -4px;
}

.tuner-presets {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e1e8ed;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preset-label {
  font-size: 13px;
  color: #536471;
  font-weight: 500;
}
</style>
