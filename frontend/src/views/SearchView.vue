<template>
  <Layout>
    <div class="header">
      <el-icon class="back-btn" @click="$router.back()"><ArrowLeft /></el-icon>
      <h2>搜索结果</h2>
    </div>

    <div class="search-meta">
      搜索: <strong>{{ query }}</strong>
    </div>

    <!-- Tab 切换 -->
    <div class="tabs">
      <div 
        v-for="tab in tabs" 
        :key="tab.value"
        :class="['tab', { active: activeTab === tab.value }]"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
        <span v-if="tab.count !== null" class="count">({{ tab.count }})</span>
      </div>
    </div>

    <div v-if="loading" class="loading">
      <el-icon class="is-loading"><Loading /></el-icon> 搜索中...
    </div>

    <div v-else-if="isEmpty" class="no-results">
      <el-empty description="没有找到相关结果" />
    </div>
    
    <div v-else class="results-list">
      <!-- 用户结果 -->
      <template v-if="activeTab === 'all' || activeTab === 'users'">
        <div v-if="results.users && results.users.length > 0" class="section">
          <h3 v-if="activeTab === 'all'" class="section-title">用户</h3>
          <UserCard v-for="user in results.users" :key="user.id" :user="user" />
          <div v-if="activeTab === 'all' && results.totalUsers > 5" class="show-more" @click="switchTab('users')">
            查看全部 {{ results.totalUsers }} 位用户
          </div>
        </div>
      </template>

      <!-- 话题结果 -->
      <template v-if="activeTab === 'all' || activeTab === 'topics'">
        <div v-if="results.topics && results.topics.length > 0" class="section">
          <h3 v-if="activeTab === 'all'" class="section-title">话题</h3>
          <TopicCard v-for="topic in results.topics" :key="topic.id" :topic="topic" />
          <div v-if="activeTab === 'all' && results.totalTopics > 5" class="show-more" @click="switchTab('topics')">
            查看全部 {{ results.totalTopics }} 个话题
          </div>
        </div>
      </template>

      <!-- 帖子结果 -->
      <template v-if="activeTab === 'all' || activeTab === 'posts'">
        <div v-if="results.posts && results.posts.length > 0" class="section">
          <h3 v-if="activeTab === 'all'" class="section-title">帖子</h3>
          <TweetCard v-for="item in results.posts" :key="item.id" :tweet="item" />
          <div v-if="activeTab === 'all' && results.totalPosts > 10" class="show-more" @click="switchTab('posts')">
            查看全部 {{ results.totalPosts }} 条帖子
          </div>
        </div>
      </template>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import TweetCard from '../components/TweetCard.vue'
import UserCard from '../components/UserCard.vue'
import TopicCard from '../components/TopicCard.vue'
import { ArrowLeft, Loading } from '@element-plus/icons-vue'
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const query = ref('')
const results = ref({
  posts: [],
  users: [],
  topics: [],
  totalPosts: 0,
  totalUsers: 0,
  totalTopics: 0
})
const loading = ref(false)
const activeTab = ref('all')

const tabs = computed(() => [
  { label: '全部', value: 'all', count: null },
  { label: '帖子', value: 'posts', count: results.value.totalPosts || null },
  { label: '用户', value: 'users', count: results.value.totalUsers || null },
  { label: '话题', value: 'topics', count: results.value.totalTopics || null }
])

const isEmpty = computed(() => {
  if (activeTab.value === 'all') {
    return (!results.value.posts || results.value.posts.length === 0) &&
           (!results.value.users || results.value.users.length === 0) &&
           (!results.value.topics || results.value.topics.length === 0)
  }
  if (activeTab.value === 'posts') return !results.value.posts || results.value.posts.length === 0
  if (activeTab.value === 'users') return !results.value.users || results.value.users.length === 0
  if (activeTab.value === 'topics') return !results.value.topics || results.value.topics.length === 0
  return true
})

const doSearch = async () => {
  query.value = route.query.q
  if (!query.value) return
  
  loading.value = true
  try {
    const res = await api.get('/search', { 
      params: { 
        q: query.value,
        type: activeTab.value
      } 
    })
    results.value = res.data
  } catch (e) { 
    console.error(e) 
  } finally { 
    loading.value = false 
  }
}

const switchTab = (tab) => {
  activeTab.value = tab
  doSearch()
}

onMounted(() => doSearch())
watch(() => route.query.q, () => {
  activeTab.value = 'all'
  doSearch()
})
</script>

<style scoped>
.header { 
  padding: 12px 16px; 
  border-bottom: 1px solid #eff3f4; 
  display: flex; 
  align-items: center;
  position: sticky;
  top: 0;
  background: white;
  z-index: 10;
}

.back-btn { 
  font-size: 20px; 
  margin-right: 20px; 
  cursor: pointer; 
}

.back-btn:hover {
  color: #1da1f2;
}

.search-meta { 
  padding: 12px 16px; 
  color: #536471; 
  background: #f7f9f9;
  border-bottom: 1px solid #eff3f4;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #eff3f4;
  position: sticky;
  top: 53px;
  background: white;
  z-index: 10;
}

.tab {
  flex: 1;
  padding: 16px 0;
  text-align: center;
  cursor: pointer;
  color: #536471;
  font-weight: 500;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab:hover {
  background: rgba(0, 0, 0, 0.03);
}

.tab.active {
  color: #0f1419;
  border-bottom-color: #1da1f2;
  font-weight: 700;
}

.tab .count {
  font-size: 12px;
  color: #536471;
}

.loading {
  padding: 40px;
  text-align: center;
  color: #1da1f2;
}

.no-results { 
  padding: 40px; 
  text-align: center; 
}

.section {
  border-bottom: 8px solid #eff3f4;
}

.section-title {
  padding: 12px 16px;
  margin: 0;
  font-size: 18px;
  font-weight: 800;
  color: #0f1419;
  border-bottom: 1px solid #eff3f4;
}

.show-more {
  padding: 16px;
  color: #1da1f2;
  cursor: pointer;
  text-align: center;
}

.show-more:hover {
  background: rgba(29, 161, 242, 0.1);
}
</style>
