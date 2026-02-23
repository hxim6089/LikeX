<template>
  <div class="sidebar-content">
    <div class="logo-box">
      <!-- Twitter-like mocked logo -->
      <h1 class="logo">𝕏</h1> 
    </div>

    <nav>
      <a href="#" @click.prevent="goHome" class="nav-item" :class="{ active: route.path === '/' }">
        <el-icon><HomeFilled /></el-icon> <span>首页</span>
      </a>
      <a href="#" class="nav-item"><el-icon><Search /></el-icon> <span>发现</span></a>
      <router-link to="/grok" class="nav-item"><el-icon><Cpu /></el-icon> <span>Grok</span></router-link>
      <router-link to="/compare" class="nav-item"><el-icon><DataAnalysis /></el-icon> <span>算法对比</span></router-link>
      <router-link to="/ad-dashboard" class="nav-item"><el-icon><Coin /></el-icon> <span>广告报表</span></router-link>
      <router-link to="/notifications" class="nav-item"><el-icon><Bell /></el-icon> <span>通知</span></router-link>
      <router-link to="/messages" class="nav-item"><el-icon><Message /></el-icon> <span>私信</span></router-link>
      <router-link to="/profile" class="nav-item"><el-icon><User /></el-icon> <span>个人主页</span></router-link>
    </nav>

    <button class="tweet-btn" @click="toggleCompose">发帖</button>

    <div class="user-profile" v-if="currentUser">
        <div class="profile-info" @click="goToMyProfile">
            <el-avatar :size="40" :src="currentUser.avatarUrl" />
            <div class="user-meta">
                <div class="name">{{ currentUser.username }}</div>
                <div class="handle">{{ currentUser.handle }}</div>
            </div>
        </div>
        <div class="logout-btn" @click.stop="handleLogout">
            <el-icon><More /></el-icon>
        </div>
    </div>
  </div>
</template>

<script setup>
import { HomeFilled, Search, Bell, Message, User, More, Cpu, DataAnalysis, Coin } from '@element-plus/icons-vue'
import { useRouter, useRoute } from 'vue-router'
import { toggleCompose } from '../store'

const router = useRouter()
const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const route = useRoute() // Ensure useRoute is imported if not already, wait it calls useRouter.
// I need `useRoute` for the class binding in template
// Check imports below.

const goHome = () => {
    // Force refresh by changing query param
    router.push({ path: '/', query: { refresh: Date.now() } });
}

const goToMyProfile = () => {
    router.push('/profile');
}

const handleLogout = () => {
    if(confirm('确定要退出登录吗?')) {
        localStorage.removeItem('user');
        router.push('/login');
    }
}
</script>

<style scoped>
.sidebar-content {
  width: 100%;
  max-width: 250px;
  padding-top: 10px;
}

.logo-box {
  padding: 10px;
  font-size: 30px;
  margin-bottom: 10px;
}

.logo {
    margin: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  font-size: 20px;
  color: #0f1419;
  text-decoration: none;
  border-radius: 30px;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.nav-item:hover {
  background-color: #e8f5fd;
}

.nav-item .el-icon {
  font-size: 26px;
  margin-right: 20px;
}

.nav-item span {
    font-weight: 500;
}

.nav-item.active {
    font-weight: 700;
}

.tweet-btn {
  width: 90%;
  background-color: #1d9bf0;
  color: white;
  border: none;
  padding: 15px 0;
  border-radius: 30px;
  font-size: 17px;
  font-weight: bold;
  margin-top: 10px;
  cursor: pointer;
}
.tweet-btn:hover {
  background-color: #1a8cd8;
}

.user-profile {
    margin-top: auto; 
    display: flex;
    align-items: center;
    padding: 10px;
    border-radius: 30px;
    cursor: default;
    margin-bottom: 20px;
}
.user-profile:hover { background-color: #e8f5fd; }

.profile-info {
    display: flex;
    flex: 1;
    align-items: center;
    cursor: pointer;
}
.logout-btn {
    padding: 5px;
    border-radius: 50%;
    cursor: pointer;
}
.logout-btn:hover { background-color: rgba(0,0,0,0.1); }

.user-meta { margin-left: 10px; flex: 1; }
.name { font-weight: bold; font-size: 15px; }
.handle { font-size: 13px; color: #536471; }
</style>
