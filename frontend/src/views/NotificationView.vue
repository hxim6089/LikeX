<template>
  <div class="notification-view">
    <div class="header">
      <h2>Notifications</h2>
    </div>

    <div v-if="loading" class="loading">Loading...</div>
    
    <div v-else class="notification-list">
      <div v-if="notifications.length === 0" class="empty-state">
        No notifications yet.
      </div>
      
      <div 
        v-for="notif in notifications" 
        :key="notif.id" 
        class="notification-item"
        :class="{ 'unread': !notif.read }"
      >
        <div class="avatar">
          <img :src="notif.actor?.avatarUrl || 'https://placehold.co/50'" alt="Avatar" />
        </div>
        <div class="content">
          <div class="text">
            <strong>{{ notif.actor?.username }}</strong>
            <span v-if="notif.type === 'LIKE'"> liked your tweet.</span>
            <span v-if="notif.type === 'COMMENT'"> commented on your tweet.</span>
            <span v-if="notif.type === 'FOLLOW'"> followed you.</span>
          </div>
          <div class="time">{{ formatTime(notif.createdAt) }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';

const notifications = ref([]);
const loading = ref(true);
const currentUser = JSON.parse(localStorage.getItem('user') || '{}');

const fetchNotifications = async () => {
  if (!currentUser.id) return;
  try {
    const response = await axios.get(`http://localhost:8888/api/notifications?userId=${currentUser.id}`);
    notifications.value = response.data.content;
  } catch (error) {
    console.error("Failed to fetch notifications", error);
  } finally {
    loading.value = false;
  }
};

const formatTime = (time) => {
  if (!time) return '';
  return new Date(time).toLocaleString();
};

onMounted(() => {
  fetchNotifications();
});
</script>

<style scoped>
.notification-view {
  padding: 0;
}
.header {
  padding: 15px;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}
.header h2 {
  margin: 0;
  font-size: 20px;
}
.notification-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}
.notification-item:hover {
  background-color: #f9f9f9;
}
.notification-item.unread {
  background-color: #f0f7ff;
}
.avatar {
  margin-right: 15px;
}
.avatar img {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
}
.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.text {
  font-size: 15px;
  margin-bottom: 4px;
}
.time {
  font-size: 13px;
  color: #888;
}
.empty-state {
  padding: 40px;
  text-align: center;
  color: #888;
}
</style>
