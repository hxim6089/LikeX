<template>
  <div class="topic-card" @click="goToTopic">
    <div class="topic-icon">#</div>
    <div class="info">
      <span class="name">{{ topic.name }}</span>
      <span class="count" v-if="postCount !== null">{{ formatCount(postCount) }} 帖子</span>
    </div>
    <el-icon class="arrow"><ArrowRight /></el-icon>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ArrowRight } from '@element-plus/icons-vue'

const props = defineProps({
  topic: {
    type: Object,
    required: true
  },
  postCount: {
    type: Number,
    default: null
  }
})

const router = useRouter()

const goToTopic = () => {
  router.push(`/search?q=%23${encodeURIComponent(props.topic.name)}`)
}

const formatCount = (count) => {
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count
}
</script>

<style scoped>
.topic-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eff3f4;
  cursor: pointer;
  transition: background-color 0.2s;
}

.topic-card:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.topic-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1da1f2 0%, #0d8bd9 100%);
  color: white;
  font-size: 20px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.info {
  flex: 1;
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}

.name {
  font-weight: 700;
  color: #0f1419;
  font-size: 15px;
}

.count {
  color: #536471;
  font-size: 13px;
  margin-top: 2px;
}

.arrow {
  color: #536471;
  font-size: 18px;
}
</style>
