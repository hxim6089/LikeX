<template>
  <div class="common-layout">
    <el-container>
      <el-header>
        <el-menu mode="horizontal" router>
          <el-menu-item index="/">Home</el-menu-item>
          <el-menu-item index="/admin">Admin</el-menu-item>
        </el-menu>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <ComposeModal v-if="uiState.isComposeOpen" />
  </div>
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { ElNotification } from 'element-plus'
import ComposeModal from './components/ComposeModal.vue'
import { uiState } from './store'
import { connectWebSocket, disconnect } from './utils/websocket'

onMounted(() => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
        const user = JSON.parse(userStr)
        if (user?.id) {
            connectWebSocket(
                user.id,
                // 通知回调
                (notification) => {
                    const titleMap = {
                        'LIKE': '有人赞了你的帖子',
                        'COMMENT': '有人评论了你的帖子',
                        'FOLLOW': '有人关注了你',
                        'REPOST': '有人转发了你的帖子',
                        'QUOTE': '有人引用了你的帖子'
                    }
                    ElNotification({
                        title: titleMap[notification.type] || '新通知',
                        message: `来自 ${notification.actorName}`,
                        type: 'info',
                        duration: 5000
                    })
                },
                // 私信回调
                (message) => {
                    ElNotification({
                        title: '新私信',
                        message: `${message.senderName}: ${message.content.substring(0, 30)}...`,
                        type: 'success',
                        duration: 5000
                    })
                }
            )
        }
    }
})

onUnmounted(() => {
    disconnect()
})
</script>

<style>
body {
  margin: 0;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
  'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}
</style>
