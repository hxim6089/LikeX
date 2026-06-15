<template>
  <div class="user-card" @click="goToProfile">
    <el-avatar :src="user.avatarUrl || defaultAvatar" :size="48" />
    <div class="info">
      <div class="name-row">
        <span class="name">{{ user.username }}</span>
        <span class="handle">{{ user.handle || '@' + user.username }}</span>
      </div>
      <p class="bio" v-if="user.bio">{{ user.bio }}</p>
    </div>
    <el-button 
      v-if="showFollowButton && !isCurrentUser"
      :type="isFollowing ? 'default' : 'primary'" 
      size="small"
      round
      @click.stop="toggleFollow"
    >
      {{ isFollowing ? '已关注' : '关注' }}
    </el-button>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'

const props = defineProps({
  user: {
    type: Object,
    required: true
  },
  showFollowButton: {
    type: Boolean,
    default: true
  }
})

const router = useRouter()
const isFollowing = ref(false)
const defaultAvatar = 'https://api.dicebear.com/7.x/avataaars/svg?seed=default'

const currentUserId = computed(() => {
  const stored = localStorage.getItem('user')
  return stored ? JSON.parse(stored).id : null
})

const isCurrentUser = computed(() => {
  return currentUserId.value && currentUserId.value === props.user.id
})

onMounted(async () => {
  if (currentUserId.value && !isCurrentUser.value) {
    try {
      const res = await api.get('/relation/status', {
        params: { 
          followerId: currentUserId.value, 
          followeeId: props.user.id 
        }
      })
      isFollowing.value = res.data
    } catch (e) {
      console.error('Failed to check follow status', e)
    }
  }
})

const goToProfile = () => {
  router.push(`/profile/${props.user.id}`)
}

const toggleFollow = async () => {
  if (!currentUserId.value) {
    router.push('/login')
    return
  }
  
  try {
    if (isFollowing.value) {
      await api.post('/relation/unfollow', { 
        followerId: currentUserId.value, 
        followeeId: props.user.id 
      })
      isFollowing.value = false
    } else {
      await api.post('/relation/follow', { 
        followerId: currentUserId.value, 
        followeeId: props.user.id 
      })
      isFollowing.value = true
    }
  } catch (e) {
    console.error('Failed to toggle follow', e)
  }
}
</script>

<style scoped>
.user-card {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #eff3f4;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-card:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.info {
  flex: 1;
  margin-left: 12px;
  min-width: 0;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.name {
  font-weight: 700;
  color: #0f1419;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.handle {
  color: #536471;
  font-size: 14px;
}

.bio {
  margin: 4px 0 0 0;
  color: #0f1419;
  font-size: 14px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
