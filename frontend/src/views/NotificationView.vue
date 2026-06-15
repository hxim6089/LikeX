<template>
  <Layout title="通知">
    <div class="notification-view">
      <div class="notification-toolbar">
        <div>
          <div class="toolbar-title">最新动态</div>
          <div class="toolbar-subtitle">
            {{ unreadCount > 0 ? `${unreadCount} 条通知尚未查看` : '所有通知均已查看' }}
          </div>
        </div>
        <el-button
          v-if="unreadCount > 0"
          link
          type="primary"
          :icon="Check"
          @click="markAllAsRead"
        >
          全部已读
        </el-button>
      </div>

      <div v-if="loading" class="notification-skeleton">
        <div v-for="index in 5" :key="index" class="skeleton-row">
          <el-skeleton animated>
            <template #template>
              <div class="skeleton-content">
                <el-skeleton-item variant="circle" class="skeleton-avatar" />
                <div class="skeleton-lines">
                  <el-skeleton-item variant="text" class="skeleton-title" />
                  <el-skeleton-item variant="text" class="skeleton-preview" />
                  <el-skeleton-item variant="text" class="skeleton-type" />
                </div>
              </div>
            </template>
          </el-skeleton>
        </div>
      </div>

      <div v-else class="notification-list">
        <div v-if="notifications.length === 0" class="empty-state">
          <el-icon><Bell /></el-icon>
          <h3>暂无通知</h3>
          <p>新的点赞、评论和关注会显示在这里</p>
        </div>

        <div
          v-for="notif in notifications"
          :key="notif.id"
          class="notification-item"
          :class="{ unread: !notif.read }"
          @click="handleClick(notif)"
        >
          <el-avatar
            :size="44"
            :src="notif.actor?.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'"
          />

          <div class="notification-content">
            <div class="notification-meta">
              <div class="notification-text">
                <strong>{{ notif.actor?.username || '未知用户' }}</strong>
                <span>{{ getActionText(notif.type) }}</span>
              </div>
              <div class="notification-status">
                <span class="notification-time">{{ formatTime(notif.createdAt) }}</span>
                <span v-if="!notif.read" class="unread-dot" aria-label="未读通知"></span>
              </div>
            </div>

            <div v-if="notif.content?.content" class="notification-preview">
              {{ notif.content.content }}
            </div>

            <div class="notification-type" :class="`type-${notif.type?.toLowerCase() || 'default'}`">
              <el-icon v-if="notif.type === 'LIKE'"><Star /></el-icon>
              <el-icon v-else-if="notif.type === 'COMMENT'"><ChatLineRound /></el-icon>
              <el-icon v-else-if="notif.type === 'FOLLOW'"><UserFilled /></el-icon>
              <el-icon v-else-if="notif.type === 'REPOST'"><Refresh /></el-icon>
              <el-icon v-else-if="notif.type === 'QUOTE'"><ChatLineSquare /></el-icon>
              <el-icon v-else><Bell /></el-icon>
              <span>{{ getTypeLabel(notif.type) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Star, ChatLineRound, UserFilled, Refresh, ChatLineSquare, Bell, Check } from '@element-plus/icons-vue'
import api from '../api'
import { uiState } from '../store'

const router = useRouter()
const notifications = ref([])
const loading = ref(true)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const unreadCount = computed(() => notifications.value.filter(notif => !notif.read).length)

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

const getActionText = (type) => {
  const actions = {
    LIKE: '赞了你的帖子',
    COMMENT: '评论了你的帖子',
    FOLLOW: '关注了你',
    REPOST: '转发了你的帖子',
    QUOTE: '引用了你的帖子'
  }
  return actions[type] || '与你产生了新的互动'
}

const getTypeLabel = (type) => {
  const labels = {
    LIKE: '点赞',
    COMMENT: '评论',
    FOLLOW: '新关注',
    REPOST: '转发',
    QUOTE: '引用'
  }
  return labels[type] || '通知'
}

const parseTime = (time) => {
  if (Array.isArray(time)) {
    return new Date(time[0], time[1] - 1, time[2], time[3] || 0, time[4] || 0, time[5] || 0)
  }
  return new Date(time)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = parseTime(time)
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < 2 * day) return '昨天'
  if (diff < 7 * day) return `${Math.floor(diff / day)} 天前`
  return date.toLocaleDateString()
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-view {
  min-height: calc(100vh - 53px);
}

.notification-toolbar {
  min-height: 66px;
  padding: 12px 16px;
  border-bottom: 1px solid #eff3f4;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-sizing: border-box;
}

.toolbar-title {
  color: #0f1419;
  font-size: 15px;
  font-weight: 700;
}

.toolbar-subtitle {
  margin-top: 3px;
  color: #536471;
  font-size: 12px;
}

.notification-skeleton {
  width: 100%;
}

.skeleton-row {
  padding: 16px;
  border-bottom: 1px solid #eff3f4;
}

.skeleton-content {
  display: flex;
  gap: 12px;
}

.skeleton-avatar {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
}

.skeleton-lines {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding-top: 2px;
}

.skeleton-title {
  width: 58%;
}

.skeleton-preview {
  width: 90%;
}

.skeleton-type {
  width: 18%;
}

.notification-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #eff3f4;
  transition: background-color 0.18s ease;
  cursor: pointer;
}

.notification-item:hover {
  background-color: #f7f9f9;
}

.notification-item.unread {
  background-color: #f5faff;
}

.notification-item.unread:hover {
  background-color: #edf7ff;
}

.notification-content {
  min-width: 0;
  flex: 1;
}

.notification-meta {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.notification-text {
  min-width: 0;
  color: #0f1419;
  font-size: 14px;
  line-height: 1.45;
}

.notification-text strong {
  margin-right: 5px;
  font-weight: 700;
}

.notification-status {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.notification-time {
  color: #536471;
  font-size: 12px;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1d9bf0;
  flex-shrink: 0;
}

.notification-preview {
  display: -webkit-box;
  margin-top: 8px;
  padding: 9px 11px;
  overflow: hidden;
  color: #536471;
  font-size: 13px;
  line-height: 1.5;
  background: #f7f9f9;
  border-left: 3px solid #cfd9de;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-type {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 9px;
  color: #536471;
  font-size: 12px;
  font-weight: 600;
}

.notification-type .el-icon {
  font-size: 14px;
}

.type-like { color: #f91880; }
.type-comment { color: #1d9bf0; }
.type-follow { color: #7856ff; }
.type-repost { color: #00ba7c; }
.type-quote { color: #f7812b; }

.empty-state {
  min-height: 360px;
  padding: 48px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  box-sizing: border-box;
}

.empty-state .el-icon {
  color: #536471;
  font-size: 34px;
}

.empty-state h3 {
  margin: 14px 0 5px;
  color: #0f1419;
  font-size: 18px;
}

.empty-state p {
  margin: 0;
  color: #536471;
  font-size: 13px;
}

@media (max-width: 720px) {
  .notification-item {
    padding: 14px 12px;
  }

  .notification-meta {
    gap: 8px;
  }

  .notification-time {
    max-width: 68px;
    text-align: right;
  }
}
</style>
