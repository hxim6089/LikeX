<template>
  <div class="feed-wrapper">
    <!-- Top Tabs (For You / Following) -->
    <div class="feed-tabs">
        <div class="tab" :class="{ active: isPersonalized }" @click="switchTab(true)">
            <span>推荐</span>
            <div class="indicator" v-if="isPersonalized"></div>
        </div>
        <div class="tab" :class="{ active: !isPersonalized }" @click="switchTab(false)">
            <span>关注</span>
            <div class="indicator" v-if="!isPersonalized"></div>
        </div>
        <!-- Debug Mode Toggle (答辩展示用) -->
        <div class="debug-toggle" v-if="isPersonalized">
            <el-switch 
                v-model="debugMode" 
                active-text="📊" 
                inactive-text="" 
                size="small"
                @change="fetchFeed"
            />
        </div>
    </div>

    <!-- Twitter Post Box (Simplified) -->
    <div class="compose-box">
        <div class="avatar">
           <el-avatar :size="40" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
        </div>
        <div class="tweet-box">
        <textarea v-model="tweetContent" placeholder="有什么新鲜事?!"></textarea>
        
        <div v-if="previewImage" class="image-preview">
            <img :src="previewImage" />
            <button class="remove-img-btn" @click="removeImage">×</button>
        </div>

        <div class="box-footer">
          <div class="icons">
            <el-icon class="icon-btn" @click="triggerFileInput"><Picture /></el-icon>
            <input type="file" ref="fileInput" @change="handleFileChange" style="display: none" accept="image/*" />
            <el-icon class="icon-btn"><Location /></el-icon>
          </div>
          <button class="post-btn" @click="publishTweet" :disabled="!tweetContent && !selectedFile">发布</button>
        </div>
      </div>
    </div>
    
    <div v-if="loading" class="loading-spinner">
        <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    
    <template v-for="(item, index) in items" :key="item.id || item.content?.id">
        <TweetCard 
            :tweet="debugMode ? item.content : item" 
            :scoreBreakdown="debugMode ? item.scoreBreakdown : null"
            :rank="debugMode ? item.rank : 0"
            :showScore="debugMode"
        />
        <!-- 每5条帖子插入1条广告 -->
        <AdCard 
            v-if="(index + 1) % 5 === 0 && ads[Math.floor(index / 5)]" 
            :ad="ads[Math.floor(index / 5)].ad" 
            :matchedTags="ads[Math.floor(index / 5)].matchedTags || []"
            :userId="userId"
        />
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { Loading, Picture, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TweetCard from './TweetCard.vue'
import AdCard from './AdCard.vue'
import api from '../api'
import { useRoute } from 'vue-router'

// Mock User ID for demo
// Mock User ID removed, using real user from storage below

const items = ref([])
const ads = ref([])
const loading = ref(false)
const isPersonalized = ref(true)
const debugMode = ref(false)  // Debug 模式 (答辩展示用)
const tweetContent = ref('')
const fileInput = ref(null)
const selectedFile = ref(null)
const previewImage = ref(null)

const route = useRoute()

// Watch for "Home" button clicks (refresh param)
watch(() => route.query.refresh, () => {
    if (isPersonalized.value) {
        fetchFeed();
    }
})

// Get User from Storage
const userStr = localStorage.getItem('user');
const user = userStr ? JSON.parse(userStr) : null;
const userId = user ? user.id : null;

const triggerFileInput = () => fileInput.value.click();

const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
        selectedFile.value = file;
        previewImage.value = URL.createObjectURL(file);
    }
}

const removeImage = () => {
    selectedFile.value = null;
    previewImage.value = null;
    if(fileInput.value) fileInput.value.value = ''; // Reset input
}

const publishTweet = async () => {
    if ((!tweetContent.value.trim() && !selectedFile.value) || !userId) return;
    
    try {
        let imageUrl = null;
        // 1. Upload Image content if exists
        if (selectedFile.value) {
            const formData = new FormData();
            formData.append('file', selectedFile.value);
            const uploadRes = await api.post('/upload', formData);
            // Append BaseURL for local serving
            imageUrl = 'http://localhost:8888' + uploadRes.data.url;
        }

        // 2. Publish Content
        await api.post('/content/publish', {
            authorId: userId,
            content: tweetContent.value,
            imageUrl: imageUrl
        });
        
        ElMessage.success('发布成功!');
        tweetContent.value = '';
        removeImage();
        fetchFeed();
    } catch (e) {
        console.error(e);
        ElMessage.error('发布失败');
    }
}

const switchTab = (personalized) => {
    isPersonalized.value = personalized;
    fetchFeed();
}

const fetchFeed = async () => {
    loading.value = true;
    try {
        if (isPersonalized.value) {
            // "For You" - 支持 debug 模式
            const res = await api.get('/content/feed', { 
                params: { 
                    personalized: true, 
                    userId,
                    debug: debugMode.value  // Debug 参数
                } 
            });
            // debug 模式返回结构不同
            if (debugMode.value && res.data.debug) {
                items.value = res.data.content;
            } else {
                items.value = res.data.content;
            }
        } else {
            // "Following"
            const res = await api.get('/content/following', { params: { userId } });
            items.value = res.data.content;
        }
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
}

const fetchAds = async () => {
    if (!userId) return;
    try {
        const res = await api.get('/ads/relevant', { params: { userId, count: 3 } });
        ads.value = res.data || [];
    } catch (e) {
        console.log('Ads load skipped');
    }
}

onMounted(() => {
    fetchFeed();
    fetchAds();
})
</script>

<style scoped>
.feed-tabs {
    display: flex;
    border-bottom: 1px solid #eff3f4;
    height: 53px;
}
.tab {
    flex: 1;
    display: flex;
    justify-content: center;
    align-items: center;
    font-weight: bold;
    color: #536471;
    cursor: pointer;
    position: relative;
    transition: background 0.2s;
}
.tab:hover { background: rgba(0,0,0,0.03); }
.tab.active { color: #0f1419; }
.indicator {
    position: absolute;
    bottom: 0;
    width: 56px;
    height: 4px;
    background: #1d9bf0;
    border-radius: 9999px;
}

.compose-box {
    display: flex;
    padding: 16px;
    border-top: 1px solid #eff3f4;
}

.image-preview {
    margin: 10px 0;
    position: relative;
    max-width: 100%;
}
.image-preview img {
    max-width: 100%;
    max-height: 300px;
    border-radius: 15px;
}
.remove-img-btn {
    position: absolute;
    top: 5px;
    left: 5px;
    background: rgba(0,0,0,0.7);
    color: white;
    border: none;
    border-radius: 50%;
    width: 30px;
    height: 30px;
    cursor: pointer;
    font-size: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.icons {
    display: flex;
    gap: 10px;
}

.compose-box .avatar { margin-right: 12px; }
.compose-box .input-area { flex: 1; }
.compose-box input {
    width: 100%;
    border: none;
    font-size: 20px;
    outline: none;
    margin-top: 8px;
    margin-bottom: 20px;
}
.compose-actions { text-align: right; border-top: 1px solid #eff3f4; padding-top: 10px; }
.post-btn { font-weight: bold; padding: 18px 24px; cursor: pointer; }
.post-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background-color: #8ecdf8;
}

.loading-spinner {
    text-align: center;
    padding: 20px;
    font-size: 24px;
    color: #1d9bf0;
}

.debug-toggle {
    display: flex;
    align-items: center;
    padding: 0 16px;
    border-left: 1px solid #eff3f4;
}
</style>
