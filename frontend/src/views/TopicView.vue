<template>
  <Layout>
    <div class="topic-page">
      <div class="topic-header">
        <el-icon class="back-btn" @click="router.back()"><ArrowLeft /></el-icon>
        <div class="topic-title-row">
          <span class="hash">#</span>
          <h2>{{ topicName }}</h2>
        </div>
        <div class="topic-meta" v-if="posts.length > 0">{{ posts.length }} 条帖子</div>
      </div>

      <div v-if="loading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        加载中...
      </div>

      <div v-else-if="posts.length === 0" class="empty-state">
        该话题下暂无帖子
      </div>

      <div v-else class="posts-list">
        <TweetCard
          v-for="post in posts"
          :key="post.id"
          :tweet="post"
          @deleted="handleDeleted"
        />
      </div>
    </div>
  </Layout>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import Layout from '../components/Layout.vue'
import TweetCard from '../components/TweetCard.vue'
import api from '../api'

const route = useRoute()
const router = useRouter()

const topicName = ref('')
const posts = ref([])
const loading = ref(false)

const fetchTopicPosts = async () => {
  topicName.value = route.params.name
  if (!topicName.value) return

  loading.value = true
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    const res = await api.get(`/tags/${encodeURIComponent(topicName.value)}`, {
      params: { userId: user.id || undefined }
    })
    posts.value = res.data
  } catch (e) {
    console.error('Failed to fetch topic posts:', e)
    posts.value = []
  } finally {
    loading.value = false
  }
}

const handleDeleted = (id) => {
  posts.value = posts.value.filter(p => p.id !== id)
}

onMounted(fetchTopicPosts)
watch(() => route.params.name, fetchTopicPosts)
</script>

<style scoped>
.topic-page {
  max-width: 600px;
  margin: 0 auto;
  min-height: 100vh;
  border-left: 1px solid #eff3f4;
  border-right: 1px solid #eff3f4;
}

.topic-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eff3f4;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  z-index: 10;
}

.back-btn {
  font-size: 20px;
  cursor: pointer;
  color: #0f1419;
  margin-bottom: 8px;
  padding: 6px;
  border-radius: 50%;
  transition: background 0.2s;
}

.back-btn:hover {
  background: rgba(0, 0, 0, 0.08);
}

.topic-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.hash {
  font-size: 28px;
  font-weight: 800;
  color: #1da1f2;
}

.topic-title-row h2 {
  font-size: 24px;
  font-weight: 800;
  color: #0f1419;
  margin: 0;
}

.topic-meta {
  font-size: 14px;
  color: #536471;
  margin-top: 4px;
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #536471;
  font-size: 15px;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: #536471;
  font-size: 15px;
}
</style>
