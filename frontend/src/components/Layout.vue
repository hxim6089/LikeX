<template>
  <div class="layout-container">
    <!-- Left Sidebar -->
    <aside class="sidebar">
      <Sidebar />
    </aside>

    <!-- Center Feed -->
    <main class="main-feed">
      <div class="feed-header">
        <h2>Home</h2>
      </div>
      <slot />
    </main>

    <!-- Right Widgets -->
    <aside class="right-panel">
      <RightPanel />
    </aside>
  </div>
</template>

<script setup>
import Sidebar from './Sidebar.vue'
import RightPanel from './RightPanel.vue'
import { onMounted, onUnmounted } from 'vue'
import { connectWebSocket, disconnect } from '../utils/websocket'
import { uiState } from '../store'
import { ElNotification } from 'element-plus'
import api from '../api'

const userStr = localStorage.getItem('user')
const currentUser = userStr ? JSON.parse(userStr) : null

const fetchUnreadCount = async () => {
    if (!currentUser?.id) return
    try {
        const res = await api.get(`/notifications/unread-count?userId=${currentUser.id}`)
        uiState.unreadNotifications = res.data
    } catch (e) {
        console.error('Failed to fetch unread count', e)
    }
}

onMounted(() => {
    if (!currentUser?.id) return
    fetchUnreadCount()
    connectWebSocket(
        currentUser.id,
        (notif) => {
            // Real-time notification received
            uiState.unreadNotifications++
            const typeText = {
                'LIKE': '赞了你的帖子',
                'COMMENT': '评论了你的帖子',
                'FOLLOW': '关注了你',
                'REPOST': '转发了你的帖子',
                'QUOTE': '引用了你的帖子'
            }
            ElNotification({
                title: notif.actorName || '新通知',
                message: typeText[notif.type] || '与你互动了',
                type: 'info',
                duration: 4000,
                position: 'bottom-right'
            })
        },
        (msg) => {
            ElNotification({
                title: '新私信',
                message: msg.content || '你收到了一条私信',
                type: 'success',
                duration: 4000,
                position: 'bottom-right'
            })
        }
    )
})

onUnmounted(() => {
    disconnect()
})
</script>

<style scoped>
.layout-container {
  display: flex;
  justify-content: center;
  min-height: 100vh;
  background-color: #fff; /* or black for dark mode */
}

/* 3-Column Widths */
.sidebar {
  width: 275px;
  padding: 0 10px;
  border-right: 1px solid #eff3f4;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.main-feed {
  width: 600px;
  border-right: 1px solid #eff3f4;
}

.right-panel {
  width: 350px;
  padding-left: 30px;
}

.feed-header {
  height: 53px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid #eff3f4;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  z-index: 100;
}
</style>
