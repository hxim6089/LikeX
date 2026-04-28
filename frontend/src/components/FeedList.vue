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
            />
        </div>
    </div>

    <!-- 自定义算法参数状态条 -->
    <div class="custom-weights-bar" v-if="isPersonalized && hasCustomWeights">
      <span class="custom-weights-text">⚡ 自定义算法参数生效中</span>
      <el-button size="small" text type="warning" @click="resetWeights">恢复默认</el-button>
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
        <div class="ai-tag-hint">
          <transition name="fade">
            <span v-if="aiTagging" class="ai-analyzing">✨ AI 正在分析标签...</span>
            <span v-else>🤖 AI 将自动分析内容并添加智能标签</span>
          </transition>
        </div>
      </div>
    </div>
    
    <div v-if="loading" class="loading-spinner">
        <el-icon class="is-loading"><Loading /></el-icon>
    </div>
    
    <template v-for="(item, index) in items" :key="item.id || item.content?.id">
        <TweetCard 
            :tweet="isPersonalized ? (item.content || item) : item" 
            :scoreBreakdown="isPersonalized ? item.scoreBreakdown : null"
            :rank="isPersonalized ? item.rank : 0"
            :showScore="isPersonalized && (debugMode || index < 5) && item.scoreBreakdown"
            @deleted="handlePostDeleted"
        />
        <!-- 按配置频率插入广告 -->
        <AdCard 
            v-if="adEnabled && adInterval > 0 && (index + 1) % adInterval === 0 && ads[Math.floor(index / adInterval)]" 
            :ad="ads[Math.floor(index / adInterval)].ad" 
            :matchedTags="ads[Math.floor(index / adInterval)].matchedTags || []"
            :userId="userId"
        />
    </template>

    <!-- 加载更多 / 底部状态 -->
    <div v-if="loadingMore" class="loading-more">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载更多...</span>
    </div>
    <div v-else-if="!hasMore && items.length > 0" class="no-more">
        已经到底了
    </div>

    <!-- 滚动探测哨兵 -->
    <div ref="scrollSentinel" class="scroll-sentinel"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { Loading, Picture, Location } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import TweetCard from './TweetCard.vue'
import AdCard from './AdCard.vue'
import api from '../api'
import { useRoute } from 'vue-router'

const items = ref([])
const ads = ref([])
const loading = ref(false)
const loadingMore = ref(false)
const isPersonalized = ref(true)
const debugMode = ref(false)
const tweetContent = ref('')
const fileInput = ref(null)
const selectedFile = ref(null)
const previewImage = ref(null)
const aiTagging = ref(false)
const hasCustomWeights = ref(false)
const adInterval = ref(5)
const adEnabled = ref(true)

const currentPage = ref(0)
const pageSize = 10
const hasMore = ref(true)
const scrollSentinel = ref(null)
let observer = null

const route = useRoute()

watch(() => route.query.refresh, () => {
    if (isPersonalized.value) {
        resetAndFetch();
    }
})

const userStr = localStorage.getItem('user');
const user = userStr ? JSON.parse(userStr) : null;
const userId = user ? user.id : null;

const triggerFileInput = () => fileInput.value.click();

// 处理帖子被删除
const handlePostDeleted = (postId) => {
    items.value = items.value.filter(item => {
        const id = item.content?.id || item.id;
        return id !== postId;
    });
}

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
        
        // AI 打标签动态提示
        aiTagging.value = true;
        setTimeout(() => {
            aiTagging.value = false;
            resetAndFetch();
        }, 3000);
    } catch (e) {
        console.error(e);
        ElMessage.error('发布失败');
    }
}

const switchTab = (personalized) => {
    isPersonalized.value = personalized;
    resetAndFetch();
}

const resetAndFetch = () => {
    items.value = [];
    currentPage.value = 0;
    hasMore.value = true;
    fetchFeed();
}

const fetchFeed = async () => {
    if (currentPage.value === 0) {
        loading.value = true;
    } else {
        loadingMore.value = true;
    }
    try {
        if (isPersonalized.value) {
            const res = await api.get('/content/feed', { 
                params: { 
                    personalized: true, 
                    userId,
                    debug: true,
                    page: currentPage.value,
                    size: pageSize
                } 
            });
            const newItems = res.data.content || [];
            if (currentPage.value === 0) {
                items.value = newItems;
            } else {
                items.value = [...items.value, ...newItems];
            }
            const totalPages = res.data.totalPages || 1;
            hasMore.value = currentPage.value + 1 < totalPages;
        } else {
            const res = await api.get('/content/following', { 
                params: { userId, page: currentPage.value, size: pageSize } 
            });
            const newItems = res.data.content || [];
            if (currentPage.value === 0) {
                items.value = newItems;
            } else {
                items.value = [...items.value, ...newItems];
            }
            const totalPages = res.data.totalPages || 1;
            hasMore.value = currentPage.value + 1 < totalPages;
        }
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
        loadingMore.value = false;
    }
}

const loadNextPage = () => {
    if (loadingMore.value || loading.value || !hasMore.value) return;
    currentPage.value++;
    fetchFeed();
}

const fetchAds = async () => {
    if (!userId) return;
    try {
        // 先获取广告配置
        const cfgRes = await api.get('/ads/config');
        const cfg = cfgRes.data || {};
        adInterval.value = cfg.adInterval || 5;
        adEnabled.value = cfg.globalEnabled !== false;
        const maxAds = cfg.maxAdsPerPage || 3;

        if (!adEnabled.value) { ads.value = []; return; }

        const res = await api.get('/ads/relevant', { params: { userId, count: maxAds } });
        ads.value = res.data || [];
    } catch (e) {
        console.log('Ads load skipped');
    }
}

onMounted(() => {
    fetchFeed();
    fetchAds();
    checkCustomWeights();

    if (typeof IntersectionObserver !== 'undefined') {
        observer = new IntersectionObserver((entries) => {
            if (entries[0].isIntersecting && hasMore.value) {
                loadNextPage();
            }
        }, { rootMargin: '200px' });
        if (scrollSentinel.value) {
            observer.observe(scrollSentinel.value);
        }
    }
})

onBeforeUnmount(() => {
    if (observer) {
        observer.disconnect();
        observer = null;
    }
})

// 检查用户是否有自定义权重
const checkCustomWeights = async () => {
    if (!userId) return;
    try {
        const res = await api.get(`/weights/${userId}`);
        hasCustomWeights.value = res.data.isCustom || false;
    } catch (e) {
        hasCustomWeights.value = false;
    }
}

// 恢复默认权重
const resetWeights = async () => {
    if (!userId) return;
    try {
        await api.delete(`/weights/${userId}`);
        hasCustomWeights.value = false;
        ElMessage.success('已恢复默认算法参数');
        resetAndFetch();
    } catch (e) {
        ElMessage.error('恢复失败');
    }
}
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

.custom-weights-bar {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 8px 16px;
    background: linear-gradient(135deg, #fff7e6 0%, #fff3cd 100%);
    border-bottom: 1px solid #ffeeba;
}

.custom-weights-text {
    font-size: 13px;
    font-weight: 600;
    color: #856404;
}

.ai-tag-hint {
    padding: 6px 0 2px;
    font-size: 12px;
    color: #9ca3af;
    display: flex;
    align-items: center;
    gap: 4px;
}

.ai-analyzing {
    color: #1DA1F2;
    animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

.fade-enter-active, .fade-leave-active {
    transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
    opacity: 0;
}

.loading-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    padding: 20px;
    color: #1d9bf0;
    font-size: 14px;
}

.no-more {
    text-align: center;
    padding: 20px;
    color: #536471;
    font-size: 14px;
    border-top: 1px solid #eff3f4;
}

.scroll-sentinel {
    height: 1px;
}
</style>
