<template>
  <div class="notification-view">
    <div class="header">
      <h2>Notifications</h2>
      <button v-if="notifications.length > 0" class="read-all-btn" @click="markAllAsRead">全部已读</button>
    </div>

    <div v-if="loading" class="loading">Loading...</div>
    
    <div v-else class="notification-list">
      <div v-if="notifications.length === 0" class="empty-state">
        暂无通知
      </div>
      
      <div 
        v-for="notif in notifications" 
        :key="notif.id" 
        class="notification-item"
        :class="{ 'unread': !notif.read }"
        @click="handleClick(notif)"
      >
        <div class="notif-icon">
          <el-icon v-if="notif.type === 'LIKE'" class="icon-like"><Star /></el-icon>
          <el-icon v-else-if="notif.type === 'COMMENT'" class="icon-comment"><ChatLineRound /></el-icon>
          <el-icon v-else-if="notif.type === 'FOLLOW'" class="icon-follow"><UserFilled /></el-icon>
          <el-icon v-else-if="notif.type === 'REPOST'" class="icon-repost"><Refresh /></el-icon>
          <el-icon v-else-if="notif.type === 'QUOTE'" class="icon-quote"><ChatLineSquare /></el-icon>
          <el-icon v-else class="icon-default"><Bell /></el-icon>
        </div>
        <div class="avatar">
          <img :src="notif.actor?.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" alt="Avatar" />
        </div>
        <div class="content">
          <div class="text">
            <strong>{{ notif.actor?.username || '未知用户' }}</strong>
            <span v-if="notif.type === 'LIKE'"> 赞了你的帖子</span>
            <span v-else-if="notif.type === 'COMMENT'"> 评论了你的帖子</span>
            <span v-else-if="notif.type === 'FOLLOW'"> 关注了你</span>
            <span v-else-if="notif.type === 'REPOST'"> 转发了你的帖子</span>
            <span v-else-if="notif.type === 'QUOTE'"> 引用了你的帖子</span>
          </div>
          <div v-if="notif.content" class="preview">{{ notif.content.content?.substring(0, 80) }}{{ notif.content.content?.length > 80 ? '...' : '' }}</div>
          <div class="time">{{ formatTime(notif.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Star, ChatLineRound, UserFilled, Refresh, ChatLineSquare, Bell } from '@element-plus/icons-vue'
import api from '../api'
import { uiState } from '../store'

const router = useRouter()
const notifications = ref([])
const loading = ref(true)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const fetchNotifications = async () => {
  if (!currentUser.id) return
  try {
    const res = await api.get(`/notifications?userId=${currentUser.id}`)
    notifications.value = res.data.content
  } catch (e) {
    console.error('Failed to fetch notifications', e)
  } finally {
    loading.value = false
  }
}

const handleClick = async (notif) => {
  // Mark as read
  if (!notif.read) {
    try {
      await api.post(`/notifications/${notif.id}/read`)
      notif.read = true
      if (uiState.unreadNotifications > 0) uiState.unreadNotifications--
    } catch (e) { /* ignore */ }
  }
  // Navigate
  if (notif.type === 'FOLLOW') {
    router.push(`/profile?userId=${notif.actorId}`)
  } else if (notif.entityId) {
    router.push(`/tweet/${notif.entityId}`)
  }
}

const markAllAsRead = async () => {
  try {
    await api.post(`/notifications/read-all?userId=${currentUser.id}`)
    notifications.value.forEach(n => n.read = true)
    uiState.unreadNotifications = 0
  } catch (e) {
    console.error('Failed to mark all as read', e)
  }
}

const formatTime = (time) => {
  if (!time) return ''
  if (Array.isArray(time)) {
    return new Date(time[0], time[1] - 1, time[2], time[3] || 0, time[4] || 0).toLocaleString()
  }
  return new Date(time).toLocaleString()
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-view {
  padding: 0;
}
.header {
  padding: 15px;
  border-bottom: 1px solid #eff3f4;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header h2 {
  margin: 0;
  font-size: 20px;
}
.read-all-btn {
  background: transparent;
  border: 1px solid #cfd9de;
  color: #536471;
  padding: 4px 14px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.read-all-btn:hover {
  background: #1d9bf0;
  color: white;
  border-color: #1d9bf0;
}
.notification-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #eff3f4;
  transition: background-color 0.2s;
  cursor: pointer;
}
.notification-item:hover {
  background-color: rgba(0, 0, 0, 0.03);
}
.notification-item.unread {
  background-color: #f7fbff;
}
.notif-icon {
  margin-right: 10px;
  display: flex;
  align-items: flex-start;
  padding-top: 4px;
}
.notif-icon .el-icon {
  font-size: 20px;
}
.icon-like { color: #f91880; }
.icon-comment { color: #1d9bf0; }
.icon-follow { color: #7856ff; }
.icon-repost { color: #00ba7c; }
.icon-quote { color: #ff7a00; }
.icon-default { color: #536471; }
.avatar {
  margin-right: 12px;
  flex-shrink: 0;
}
.avatar img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.text {
  font-size: 15px;
  margin-bottom: 2px;
}
.preview {
  font-size: 13px;
  color: #536471;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  font-size: 13px;
  color: #536471;
}
.empty-state {
  padding: 60px 40px;
  text-align: center;
  color: #536471;
  font-size: 15px;
}
.loading {
  padding: 40px;
  text-align: center;
  color: #536471;
}
</style>
