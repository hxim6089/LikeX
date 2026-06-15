<template>
  <div class="ad-card" @click="handleClick">
    <div class="ad-badge">推广 · Ad</div>
    
    <div class="ad-body">
      <div class="ad-avatar">
        <span>{{ advertiserInitial }}</span>
      </div>
      <div class="ad-content">
        <div class="ad-advertiser">{{ ad.advertiser }}</div>
        <div class="ad-title">{{ ad.title }}</div>
        <div class="ad-desc">{{ ad.description }}</div>
        
        <img v-if="ad.imageUrl" :src="ad.imageUrl" class="ad-image" @error="imgError = true" v-show="!imgError" />

        <div class="ad-footer">
          <a class="ad-cta" :href="ad.targetUrl" target="_blank" @click.stop="handleClick">
            了解更多 →
          </a>
          <span class="ad-why" @click.stop="showWhy = !showWhy">
            ℹ️ 为什么看到这条
          </span>
        </div>

        <div class="ad-why-panel" v-if="showWhy">
          <p>该广告基于您的兴趣投放：</p>
          <div class="matched-tags">
            <span v-for="tag in matchedTags" :key="tag" class="tag-chip">{{ tag }}</span>
            <span v-if="matchedTags.length === 0" class="no-match">通用投放</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '../api'

const props = defineProps({
  ad: { type: Object, required: true },
  matchedTags: { type: Array, default: () => [] },
  userId: { type: Number, default: null }
})

const showWhy = ref(false)
const imgError = ref(false)

const advertiserInitial = computed(() => {
  return props.ad.advertiser ? props.ad.advertiser.charAt(0).toUpperCase() : 'A'
})

// 记录展示
onMounted(() => {
  if (props.ad.id) {
    api.post(`/ads/${props.ad.id}/impression`, null, {
      params: { userId: props.userId }
    }).catch(() => {})
  }
})

// 记录点击
const handleClick = () => {
  if (props.ad.id) {
    api.post(`/ads/${props.ad.id}/click`).catch(() => {})
  }
  if (props.ad.targetUrl) {
    window.open(props.ad.targetUrl, '_blank')
  }
}
</script>

<style scoped>
.ad-card {
  padding: 12px 16px;
  border-bottom: 1px solid #eff3f4;
  cursor: pointer;
  transition: background 0.2s;
  position: relative;
}

.ad-card:hover {
  background: #f7f9fa;
}

.ad-badge {
  font-size: 11px;
  color: #1DA1F2;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 0.5px;
}

.ad-body {
  display: flex;
  gap: 12px;
}

.ad-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1DA1F2, #0d8bd9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
  flex-shrink: 0;
}

.ad-content {
  flex: 1;
  min-width: 0;
}

.ad-advertiser {
  font-weight: 700;
  font-size: 15px;
  color: #0f1419;
}

.ad-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f1419;
  margin: 4px 0;
}

.ad-desc {
  font-size: 13px;
  color: #536471;
  line-height: 1.4;
  margin-bottom: 8px;
}

.ad-image {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 8px;
}

.ad-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ad-cta {
  background: #1DA1F2;
  color: white;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.2s;
}

.ad-cta:hover {
  background: #0d8bd9;
}

.ad-why {
  font-size: 12px;
  color: #AAB8C2;
  cursor: pointer;
}

.ad-why:hover {
  color: #1DA1F2;
}

.ad-why-panel {
  margin-top: 8px;
  padding: 10px;
  background: #f7f9fa;
  border-radius: 8px;
}

.ad-why-panel p {
  font-size: 12px;
  color: #536471;
  margin: 0 0 6px 0;
}

.matched-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag-chip {
  background: #E8F5FE;
  color: #1DA1F2;
  padding: 2px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.no-match {
  font-size: 12px;
  color: #AAB8C2;
}
</style>
