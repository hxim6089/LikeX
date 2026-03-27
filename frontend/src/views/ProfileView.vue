<template>
  <Layout>
    <div class="profile-container" :class="{ 'insights-expanded': activeTab === 'insights' }">
        <!-- Header / Banner -->
        <div class="profile-header">
            <div class="back-btn" @click="$router.back()">
                <el-icon><ArrowLeft /></el-icon>
            </div>
            <div class="header-info">
                <h3>{{ persona.username || 'Profile' }}</h3>
                <div class="subtitle">{{ posts.length }} posts</div>
            </div>
        </div>

        <div class="banner">
             <!-- Placeholder banner -->
        </div>

        <div class="profile-info-section">
            <div class="avatar-row">
                 <el-avatar :size="134" :src="persona.avatarUrl" class="profile-avatar-big" />
                 <div class="actions">
                     <el-button v-if="isOwnProfile" class="edit-btn" round @click="showEditModal = true">Edit profile</el-button>
                     <el-button 
                        v-else 
                        class="follow-btn" 
                        :type="isFollowing ? 'default' : 'primary'"
                        round 
                        @click="toggleFollow"
                     >
                        {{ isFollowing ? 'Following' : 'Follow' }}
                     </el-button>
                     <el-button class="message-btn" circle @click="goToMessage">
                        <el-icon><Message /></el-icon>
                     </el-button>
                 </div>
            </div>
            
            <div class="info-block">
                <div class="name-block">
                    <span class="name">{{ persona.username }}</span>
                    <span class="handle">{{ persona.handle }}</span>
                </div>
                <div class="bio">{{ persona.bio }}</div>
                
                <div class="meta-row">
                    <span class="meta-item"><el-icon><Calendar /></el-icon> Joined {{ formatJoinDate(persona.createdAt) }}</span>
                </div>

                <div class="follow-stats">
                    <span><strong>{{ persona.followingCount || 0 }}</strong> Following</span>
                    <span><strong>{{ persona.followerCount || 0 }}</strong> Followers</span>
                    <span><strong>{{ persona.totalLikes || 0 }}</strong> Likes</span>
                </div>
            </div>
        </div>

        <!-- Tabs -->
        <div class="tabs">
            <div class="tab" :class="{ active: activeTab === 'posts' }" @click="switchTab('posts')">Posts</div>
            <div class="tab" :class="{ active: activeTab === 'replies' }" @click="switchTab('replies')">Replies</div>
            <div class="tab" :class="{ active: activeTab === 'likes' }" @click="switchTab('likes')">Likes</div>
            <div class="tab" :class="{ active: activeTab === 'insights' }" @click="switchTab('insights')">📊 Insights</div>
        </div>

        <!-- Feed -->
        <div class="profile-feed" v-show="activeTab !== 'insights'">
             <div v-for="tweet in posts" :key="tweet.id" class="feed-item">
                 <TweetCard :tweet="tweet" />
             </div>
             <el-empty v-if="posts.length === 0" description="No posts yet" />
        </div>
        
        <!-- Insights Panel (Phase 27: 答辩展示) -->
        <div class="insights-panel" v-show="activeTab === 'insights'">
            <div class="insights-header">
                <h3>🎯 用户画像分析</h3>
                <p class="persona-title">{{ persona.personaTitle }}</p>
            </div>

            <!-- 概览仪表盘 -->
            <div class="overview-dashboard animate-in" style="animation-delay: 0s">
                <div class="overview-item">
                    <span class="ov-icon">🧬</span>
                    <span class="ov-value">{{ persona.userTypeDetail?.label || persona.userTypeDetail?.type || '—' }}</span>
                    <span class="ov-label">用户类型</span>
                </div>
                <div class="overview-item">
                    <span class="ov-icon">⚡</span>
                    <span class="ov-value">{{ persona.activityLevel || '—' }}<small v-if="persona.activityScore">({{ persona.activityScore }}分)</small></span>
                    <span class="ov-label">活跃度</span>
                </div>
                <div class="overview-item">
                    <span class="ov-icon">📏</span>
                    <span class="ov-value">{{ readPrefLabel }}</span>
                    <span class="ov-label">阅读偏好</span>
                </div>
                <div class="overview-item">
                    <span class="ov-icon">🖼️</span>
                    <span class="ov-value">{{ Math.round((persona.contentPreference?.imagePreference || 0) * 100) }}%</span>
                    <span class="ov-label">图片偏好</span>
                </div>
                <div class="overview-item">
                    <span class="ov-icon">🌐</span>
                    <span class="ov-value">{{ Math.round((persona.contentPreference?.topicDiversity || 0) * 100) }}%</span>
                    <span class="ov-label">话题多样性</span>
                </div>
                <div class="overview-item">
                    <span class="ov-icon">🦉</span>
                    <span class="ov-value">{{ Math.round((persona.nightOwlIndex || 0) * 100) }}%</span>
                    <span class="ov-label">夜猫子指数</span>
                </div>
            </div>
            
            <!-- 兴趣词云 -->
            <div class="animate-in" style="animation-delay: 0.1s">
              <InterestWordCloud :data="persona.wordCloudData || []" />
            </div>
            
            <!-- 行为雷达图 -->
            <div class="animate-in" style="animation-delay: 0.2s">
              <BehaviorRadarChart :stats="persona.behaviorStats || {}" />
            </div>
            
            <!-- 分类偏好 -->
            <div class="animate-in" style="animation-delay: 0.3s">
              <MatchRateBar 
                  :data="persona.categoryDistribution || []" 
                  :matchRate="persona.recommendationMatchRate" 
              />
            </div>

            <!-- Phase 29: 精细化画像 -->
            <div class="animate-in" style="animation-delay: 0.4s">
              <PersonaDetailCard :persona="persona" />
            </div>
        </div>

        <!-- Edit Modal (Simplified reuse) -->
         <el-dialog v-model="showEditModal" title="Edit Profile" width="500px">
            <el-form label-position="top">
                <el-form-item label="Avatar">
                     <input type="file" @change="handleFileChange" accept="image/*" />
                </el-form-item>
                <el-form-item label="Handle">
                    <el-input v-model="editForm.handle" />
                </el-form-item>
                <el-form-item label="Bio">
                    <el-input v-model="editForm.bio" type="textarea" />
                </el-form-item>
            </el-form>
             <template #footer>
                <el-button @click="showEditModal = false">Cancel</el-button>
                <el-button type="primary" @click="saveProfile">Save</el-button>
            </template>
         </el-dialog>

    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import TweetCard from '../components/TweetCard.vue'
import InterestWordCloud from '../components/InterestWordCloud.vue'
import BehaviorRadarChart from '../components/BehaviorRadarChart.vue'
import MatchRateBar from '../components/MatchRateBar.vue'
import PersonaDetailCard from '../components/PersonaDetailCard.vue'
import { ref, onMounted, reactive, watch, computed } from 'vue'
import { ArrowLeft, Calendar, Message } from '@element-plus/icons-vue'
import api from '../api'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const persona = ref({})
const posts = ref([])
const showEditModal = ref(false)

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;
const isOwnProfile = ref(true);

const editForm = reactive({ handle: '', bio: '', avatarUrl: '' })

const isFollowing = ref(false);
const activeTab = ref('posts');

const readPrefLabel = computed(() => {
    const len = persona.value?.contentPreference?.avgReadLength
    if (len === 'long') return '长文爱好者'
    if (len === 'medium') return '中等篇幅'
    if (len === 'short') return '快餐阅读'
    return '未知'
})

const formatJoinDate = (dateStr) => {
    if (!dateStr) return 'Unknown'
    try {
        const d = new Date(dateStr)
        const months = ['January','February','March','April','May','June','July','August','September','October','November','December']
        return months[d.getMonth()] + ' ' + d.getFullYear()
    } catch { return dateStr }
}

const loadData = async () => {
    const queryId = route.query.userId;
    const targetId = queryId || (currentUser ? currentUser.id : null);
    
    if (!targetId) return;
    
    isOwnProfile.value = (currentUser && String(currentUser.id) === String(targetId));

    try {
        // Fetch User Info (Persona)
        const res = await api.get(`/user/${targetId}/persona`);
        persona.value = res.data;
        
        editForm.handle = persona.value.handle;
        editForm.bio = persona.value.bio;
        editForm.avatarUrl = persona.value.avatarUrl;

        // Check Follow Status
        if (!isOwnProfile.value && currentUser) {
             const statusRes = await api.get('/relation/status', { 
                 params: { followerId: currentUser.id, followeeId: targetId } 
             });
             isFollowing.value = statusRes.data;
        }

        // Fetch Content based on Tab
        fetchTabContent(targetId);

    } catch (e) {
        console.error(e);
        ElMessage.error("Failed to load profile");
    }
}

const fetchTabContent = async (targetId) => {
    posts.value = [];
    try {
        let url = `/content/user/${targetId}`;
        if (activeTab.value === 'replies') url = `/content/user/${targetId}/replies`;
        if (activeTab.value === 'likes') url = `/content/user/${targetId}/likes`;
        
        const res = await api.get(url);
        posts.value = res.data;
    } catch(e) { console.error(e); }
}

const switchTab = (tab) => {
    activeTab.value = tab;
    // Reload content for target user
    const queryId = route.query.userId;
    const targetId = queryId || (currentUser ? currentUser.id : null);
    if(targetId) fetchTabContent(targetId);
}

const toggleFollow = async () => {
    if (!currentUser) return;
    const targetId = persona.value.id; // persona DTO should have ID
    // If persona.id is missing, use route param or careful check. 
    // Actually backend returns User object which has ID.
    // Wait, persona endpoint returns PersonaDTO? Let's check. 
    // Assuming backend returns User object for now or DTO has ID.
    
    try {
        if (isFollowing.value) {
            await api.post('/relation/unfollow', { followerId: currentUser.id, followeeId: targetId });
            isFollowing.value = false;
        } else {
            await api.post('/relation/follow', { followerId: currentUser.id, followeeId: targetId });
            isFollowing.value = true;
        }
    } catch(e) { ElMessage.error('Action failed'); }
}

const goToMessage = () => {
    if (persona.value.id) {
        router.push(`/messages?userId=${persona.value.id}`);
    }
}

const handleFileChange = async (e) => {
    // simplified upload logic reuse
    const file = e.target.files[0];
    if(!file) return;
    const formData = new FormData();
    formData.append('file', file);
    try {
        const res = await api.post('/upload', formData, {headers:{'Content-Type':'multipart/form-data'}});
        editForm.avatarUrl = 'http://localhost:8888' + res.data.url;
    } catch(e) { ElMessage.error('Upload failed'); }
}

const saveProfile = async () => {
    try {
        const res = await api.put(`/user/${currentUser.id}`, {
            handle: editForm.handle,
            bio: editForm.bio,
            avatarUrl: editForm.avatarUrl
        });
        
        // Update Local Storage so global state (Sidebar etc) reflects changes
        if (currentUser) {
            Object.assign(currentUser, res.data);
            localStorage.setItem('user', JSON.stringify(currentUser));
        }

        showEditModal.value = false;
        loadData();
        ElMessage.success('Profile updated');
        
        // Reload page to refresh Sidebar (simplest way without global state store)
        setTimeout(() => location.reload(), 500);
        
    } catch(e) { ElMessage.error('Save failed'); }
}

onMounted(loadData);
watch(() => route.query.userId, loadData); // reload on route change
</script>

<style scoped>
.profile-container {
    max-width: 600px;
    border-right: 1px solid #eff3f4;
    min-height: 100vh;
    transition: max-width 0.3s ease;
}
.profile-container.insights-expanded {
    max-width: 800px;
}
.profile-header {
    display: flex;
    align-items: center;
    gap: 20px;
    padding: 5px 16px;
    position: sticky;
    top: 0;
    background: rgba(255,255,255,0.9);
    backdrop-filter: blur(5px);
    z-index: 10;
}
.back-btn { cursor: pointer; padding: 8px; border-radius: 50%; }
.back-btn:hover { background: #eff3f4; }
.header-info h3 { margin: 0; font-size: 20px; }
.subtitle { font-size: 13px; color: #536471; }

.banner {
    background-color: #cfd9de;
    height: 200px;
    background-image: url('https://pbs.twimg.com/profile_banners/placeholder.jpg'); /* Optional placeholder */
    background-size: cover;
}

.profile-info-section {
    padding: 12px 16px;
    position: relative;
}
.avatar-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-top: -15%;
    margin-bottom: 12px;
}
.profile-avatar-big {
    border: 4px solid white;
    background: white;
}
.edit-btn { font-weight: bold; border-color: #cfd9de; color: #0f1419; }
.edit-btn:hover { background: #eff3f4; }
.message-btn { margin-left: 10px; border-color: #cfd9de; color: #0f1419; }
.message-btn:hover { background: #eff3f4; }

.name-block { display: flex; flex-direction: column; margin-bottom: 12px; }
.name { font-weight: 800; font-size: 20px; }
.handle { font-size: 15px; color: #536471; }
.bio { font-size: 15px; margin-bottom: 12px; }

.meta-row { color: #536471; font-size: 15px; margin-bottom: 12px; display: flex; gap: 10px; }
.meta-item { display: flex; align-items: center; gap: 4px; }

.follow-stats { display: flex; gap: 20px; font-size: 14px; color: #536471;}
.follow-stats strong { color: #0f1419; }

.tabs {
    display: flex;
    border-bottom: 1px solid #eff3f4;
    margin-top: 10px;
}
.tab {
    flex: 1;
    text-align: center;
    padding: 16px;
    font-weight: 500;
    color: #536471;
    cursor: pointer;
    position: relative;
    transition: background 0.2s;
}
.tab:hover { background: #eff3f4; }
.tab.active {
    font-weight: bold;
    color: #0f1419;
}
.tab.active::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 56px;
    height: 4px;
    background: #1d9bf0;
    border-radius: 2px;
}

.feed-item { border-bottom: 1px solid #eff3f4; }

/* Insights Panel Styles */
.insights-panel {
    padding: 20px;
    background: #f7f9fa;
}

.insights-header {
    text-align: center;
    margin-bottom: 24px;
}

.insights-header h3 {
    margin: 0 0 8px 0;
    font-size: 24px;
    color: #0f1419;
}

.insights-header .persona-title {
    margin: 0;
    font-size: 16px;
    color: #1da1f2;
    font-weight: 600;
}

/* Overview Dashboard */
.overview-dashboard {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
    gap: 12px;
    margin-bottom: 20px;
    padding: 20px;
    background: white;
    border-radius: 16px;
    box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.overview-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 12px 8px;
    border-radius: 12px;
    background: #f7f9fa;
    transition: transform 0.2s, box-shadow 0.2s;
}

.overview-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(29, 161, 242, 0.15);
}

.ov-icon { font-size: 24px; }
.ov-value { font-size: 14px; font-weight: 700; color: #0f1419; text-align: center; }
.ov-value small { font-size: 11px; font-weight: 500; color: #536471; margin-left: 2px; }
.ov-label { font-size: 11px; color: #536471; }

/* Staggered Fade-in Animation */
@keyframes fadeSlideUp {
    from {
        opacity: 0;
        transform: translateY(16px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-in {
    animation: fadeSlideUp 0.4s ease forwards;
    opacity: 0;
}
</style>
