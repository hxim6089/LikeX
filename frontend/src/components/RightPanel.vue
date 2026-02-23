<template>
  <div class="right-panel">
    <!-- 搜索框 -->
    <div class="search-box">
      <el-icon><Search /></el-icon>
      <input 
        type="text" 
        placeholder="搜索" 
        v-model="keyword" 
        @keyup.enter="handleSearch"
        @input="handleInput"
        @focus="showSuggestions = true"
        @blur="hideSuggestions"
      />
      <!-- 搜索建议下拉 -->
      <div v-if="showSuggestions && searchSuggestions.length > 0" class="suggestions">
        <div 
          v-for="(item, index) in searchSuggestions" 
          :key="index" 
          class="suggestion-item"
          @mousedown.prevent="selectSuggestion(item)"
        >
          <el-avatar v-if="item.icon" :size="32" :src="item.icon" />
          <div v-else class="suggestion-icon">
            {{ item.type === 'topic' ? '#' : '@' }}
          </div>
          <div class="suggestion-info">
            <span class="suggestion-value">{{ item.value }}</span>
            <span class="suggestion-subtext">{{ item.subtext }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门话题 -->
    <div class="trends-box">
      <h3>热门话题</h3>
      <div v-if="loadingTrending" class="loading-text">加载中...</div>
      <div v-else-if="trendingTopics.length === 0" class="empty-text">暂无热门话题</div>
      <div 
        v-else
        v-for="(topic, index) in trendingTopics" 
        :key="topic.name" 
        class="trend-item"
        @click="goToTopic(topic.name)"
      >
        <div class="trend-meta">热门 · 第 {{ index + 1 }} 名</div>
        <div class="trend-name">#{{ topic.name }}</div>
        <div class="trend-count">{{ formatCount(topic.postCount) }} 帖子</div>
      </div>
      <div v-if="trendingTopics.length > 0" class="show-more" @click="router.push('/search?q=%23')">
        查看更多
      </div>
    </div>

    <!-- 推荐关注 -->
    <div class="follow-box">
      <h3>推荐关注</h3>
      <div v-for="user in suggestions" :key="user.id" class="follow-item">
        <el-avatar :size="40" :src="user.avatarUrl" class="avatar-placeholder" @click="goToProfile(user.id)" />
        <div class="user-info" @click="goToProfile(user.id)">
          <div class="name">{{ user.username }}</div>
          <div class="handle">{{ user.handle }}</div>
        </div>
        <button class="follow-btn" @click="followUser(user.id)">关注</button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import api from '../api'

const router = useRouter()
const suggestions = ref([])
const keyword = ref('')
const showSuggestions = ref(false)
const searchSuggestions = ref([])
const trendingTopics = ref([])
const loadingTrending = ref(false)

const userStr = localStorage.getItem('user')
const currentUser = userStr ? JSON.parse(userStr) : { id: 1 }

// 搜索相关
const handleSearch = () => {
  if (keyword.value.trim()) {
    showSuggestions.value = false
    router.push(`/search?q=${encodeURIComponent(keyword.value)}`)
  }
}

let debounceTimer = null
const handleInput = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(async () => {
    if (keyword.value.length >= 2) {
      try {
        const res = await api.get('/search/suggest', { params: { q: keyword.value } })
        searchSuggestions.value = res.data
      } catch (e) {
        console.error(e)
        searchSuggestions.value = []
      }
    } else {
      searchSuggestions.value = []
    }
  }, 300)
}

const hideSuggestions = () => {
  setTimeout(() => {
    showSuggestions.value = false
  }, 200)
}

const selectSuggestion = (item) => {
  if (item.type === 'user') {
    keyword.value = item.subtext // @handle
  } else if (item.type === 'topic') {
    keyword.value = item.value // #topicname
  }
  showSuggestions.value = false
  handleSearch()
}

// 热门话题
const fetchTrending = async () => {
  loadingTrending.value = true
  try {
    const res = await api.get('/trending', { params: { limit: 5 } })
    trendingTopics.value = res.data
  } catch (e) {
    console.error('Failed to fetch trending:', e)
  } finally {
    loadingTrending.value = false
  }
}

const goToTopic = (name) => {
  router.push(`/search?q=${encodeURIComponent('#' + name)}`)
}

const formatCount = (count) => {
  if (!count) return '0'
  if (count >= 10000) {
    return (count / 10000).toFixed(1) + '万'
  } else if (count >= 1000) {
    return (count / 1000).toFixed(1) + 'k'
  }
  return count
}

// 推荐关注
const fetchSuggestions = async () => {
  try {
    const res = await api.get('/relation/suggestions', { params: { userId: currentUser.id } })
    suggestions.value = res.data
  } catch (e) {
    console.error(e)
  }
}

const followUser = async (targetId) => {
  try {
    await api.post('/relation/follow', { followerId: currentUser.id, followeeId: targetId })
    suggestions.value = suggestions.value.filter(u => u.id !== targetId)
  } catch (e) {
    console.error(e)
  }
}

const goToProfile = (userId) => {
  router.push(`/profile/${userId}`)
}

onMounted(() => {
  fetchSuggestions()
  fetchTrending()
})
</script>

<style scoped>
.search-box {
  position: relative;
  margin-bottom: 20px;
}

.search-box input {
  width: 100%;
  padding: 12px 20px 12px 45px;
  border-radius: 25px;
  border: none;
  background-color: #eff3f4;
  font-size: 15px;
}

.search-box input:focus {
  outline: none;
  background-color: white;
  border: 1px solid #1da1f2;
}

.search-box .el-icon {
  position: absolute;
  left: 15px;
  top: 50%;
  transform: translateY(-50%);
  color: #536471;
  z-index: 1;
}

.suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  max-height: 300px;
  overflow-y: auto;
  z-index: 100;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
}

.suggestion-item:hover {
  background: #f7f9f9;
}

.suggestion-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1da1f2 0%, #0d8bd9 100%);
  color: white;
  font-size: 16px;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
}

.suggestion-info {
  margin-left: 12px;
  display: flex;
  flex-direction: column;
}

.suggestion-value {
  font-weight: 600;
  color: #0f1419;
}

.suggestion-subtext {
  font-size: 13px;
  color: #536471;
}

.trends-box, .follow-box {
  background-color: #f7f9f9;
  border-radius: 16px;
  padding: 15px;
  margin-bottom: 20px;
}

h3 {
  margin: 0 0 15px 0;
  font-size: 20px;
  font-weight: 800;
}

.loading-text, .empty-text {
  color: #536471;
  font-size: 14px;
  padding: 10px 0;
}

.trend-item {
  padding: 10px 0;
  cursor: pointer;
  border-radius: 8px;
  transition: background 0.2s;
}

.trend-item:hover {
  background: rgba(0, 0, 0, 0.03);
}

.trend-meta { color: #536471; font-size: 13px; }
.trend-name { font-weight: bold; margin: 2px 0; color: #0f1419; }
.trend-count { color: #536471; font-size: 13px; }

.show-more {
  padding: 12px 0;
  color: #1da1f2;
  cursor: pointer;
  text-align: center;
  border-radius: 8px;
}

.show-more:hover {
  background: rgba(29, 161, 242, 0.1);
}

.follow-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.avatar-placeholder {
  cursor: pointer;
}

.user-info { 
  flex: 1; 
  margin-left: 10px;
  cursor: pointer;
}

.name { font-weight: bold; font-size: 15px; }
.handle { color: #536471; font-size: 13px; }

.follow-btn {
  background: black;
  color: white;
  border: none;
  border-radius: 20px;
  padding: 6px 16px;
  font-weight: bold;
  cursor: pointer;
  transition: background 0.2s;
}

.follow-btn:hover {
  background: #333;
}
</style>
