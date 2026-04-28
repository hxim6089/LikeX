<template>
  <div class="tweet-card" ref="tweetCardRef" @click="goToDetail">
    <div class="tweet-avatar" @click.stop="goToProfile(tweet.author)">
      <el-avatar :size="48" :src="tweet.author.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
    </div>
    
    <div class="tweet-content">
      <div class="tweet-header">
        <span class="username">{{ tweet.author?.username || 'Unknown' }}</span>
        <span class="handle">{{ tweet.author?.handle || '@unknown' }}</span>
        <span class="dot">·</span>
        <span class="time">{{ formatTime(tweet.createdAt) }}</span>
        <div class="header-actions">
             <el-icon v-if="tweet.author?.id === currentUser.id || currentUser.role === 'ADMIN'" class="delete-btn" @click.stop="handleDelete" title="删除"><Delete /></el-icon>
             <el-icon class="grok-btn" @click.stop="analyzeTweet" title="Ask Grok"><Cpu /></el-icon>
        </div>
      </div>
      
      <div class="tweet-text">
        <template v-for="(part, index) in parsedContent" :key="index">
            <span v-if="part.isTag" class="hashtag" @click.stop="handleTagClick(part.text)">{{ part.text }}</span>
            <span v-else>{{ part.text }}</span>
        </template>
      </div>

      <!-- AI 智能标签展示 -->
      <div class="tweet-tags" v-if="tweet.tags && tweet.tags.length" @click.stop>
        <span class="tag-item" 
              v-for="tag in tweet.tags" 
              :key="tag.id" 
              @click="handleTagClick('#' + tag.name)">
          #{{ tag.name }}
        </span>
      </div>

      <div v-if="tweet.imageUrl" class="tweet-image">
        <img :src="tweet.imageUrl" loading="lazy" />
      </div>

      <!-- 引用的原帖 (Quote) -->
      <div v-if="tweet.quoteOf" class="quoted-tweet-display" @click.stop="goToQuotedPost">
        <div class="quoted-header">
          <el-avatar :size="20" :src="tweet.quoteOf.author?.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
          <span class="quoted-username">{{ tweet.quoteOf.author?.username || 'Unknown' }}</span>
          <span class="quoted-handle">{{ tweet.quoteOf.author?.handle || '@unknown' }}</span>
        </div>
        <div class="quoted-content">{{ tweet.quoteOf.content }}</div>
        <div v-if="tweet.quoteOf.imageUrl" class="quoted-image">
          <img :src="tweet.quoteOf.imageUrl" />
        </div>
      </div>

      <div class="tweet-actions">
        <div class="action-item" @click.stop="toggleReplyInput">
          <el-icon><ChatLineRound /></el-icon>
          <span>{{ tweet.commentCount }}</span>
        </div>
        <el-dropdown trigger="click" @command="handleRepostCommand">
          <div class="action-item" @click.stop>
            <el-icon><Refresh /></el-icon>
            <span>{{ tweet.repostCount || 0 }}</span>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="repost">转发</el-dropdown-item>
              <el-dropdown-item command="quote">引用</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <div class="action-item like-action" :class="{ 'liked': tweet.liked }" @click.stop="handleLike">
          <el-icon><StarFilled v-if="tweet.liked" /><Star v-else /></el-icon>
          <span>{{ tweet.likeCount }}</span>
        </div>
        <div class="action-item">
          <el-icon><DataAnalysis /></el-icon>
          <span>{{ tweet.viewCount }}</span>
        </div>
      </div>

      <!-- Inline Reply Input -->
      <div v-if="showReplyInput" class="inline-reply" @click.stop>
        <textarea v-model="replyContent" placeholder="Post your reply"></textarea>
        <div class="reply-actions">
           <button @click="submitReply" :disabled="!replyContent.trim()">Reply</button>
        </div>
      </div>

      <!-- Recursive Replies -->
      <div v-if="tweet.replies && tweet.replies.length > 0" class="replies-container">
         <TweetCard v-for="reply in tweet.replies" :key="reply.id" :tweet="reply" :is-reply="true" />
      </div>
      
      <!-- Score Panel (推荐评分展示) -->
      <div @click.stop>
        <ScorePanel 
          :score="scoreBreakdown" 
          :rank="rank" 
          :visible="showScore && scoreBreakdown" 
        />
      </div>
    </div>
  </div>

  <!-- Grok Analysis Modal -->
  <el-dialog v-model="showGrokModal" title="Grok Analysis" width="500px" append-to-body>
      <div class="grok-content">
          <div class="grok-avatar-box">
              <div class="grok-avatar">G</div>
          </div>
          <div class="analysis-text" v-if="analyzing">
              <span class="typing-dot">.</span><span class="typing-dot">.</span><span class="typing-dot">.</span>
          </div>
          <div class="analysis-text" v-else v-html="grokAnalysis"></div>
      </div>
  </el-dialog>

  <!-- Quote Modal -->
  <el-dialog v-model="showQuoteModal" title="引用转发" width="500px" append-to-body>
      <div class="quote-modal-content">
          <textarea v-model="quoteContent" placeholder="添加你的评论..." rows="4" style="width:100%;resize:none;border:1px solid #ddd;border-radius:8px;padding:12px;font-size:14px;"></textarea>
          <div class="quoted-tweet" style="margin-top:12px;padding:12px;border:1px solid #e1e8ed;border-radius:12px;background:#f7f9fa;">
              <div style="font-weight:bold;">{{ tweet.author?.username }}</div>
              <div style="color:#657786;font-size:13px;">{{ tweet.content?.substring(0, 100) }}{{ tweet.content?.length > 100 ? '...' : '' }}</div>
          </div>
      </div>
      <template #footer>
          <el-button @click="showQuoteModal = false">取消</el-button>
          <el-button type="primary" @click="submitQuote" :disabled="!quoteContent.trim()">发布</el-button>
      </template>
  </el-dialog>
</template>

<script setup>
import { ChatLineRound, Star, StarFilled, Refresh, DataAnalysis, Cpu, Delete } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import ScorePanel from './ScorePanel.vue'

const router = useRouter()

const props = defineProps({
  tweet: Object,
  isReply: Boolean,
  scoreBreakdown: Object,
  rank: Number,
  showScore: Boolean
})

const emit = defineEmits(['deleted'])

// ===== 浏览时长追踪 =====
const tweetCardRef = ref(null)
let viewStartTime = null
let viewTimer = null
let hasReportedView = false

const userStr = localStorage.getItem('user')
const currentUser = userStr ? JSON.parse(userStr) : { id: 1 }

const startViewTracking = () => {
  if (hasReportedView || !props.tweet?.id) return
  viewStartTime = Date.now()
}

const stopViewTracking = () => {
  if (!viewStartTime || hasReportedView || !props.tweet?.id) return
  const duration = Math.round((Date.now() - viewStartTime) / 1000)
  viewStartTime = null
  if (duration >= 2) {
    hasReportedView = true
    api.post('/behavior/view', {
      userId: currentUser.id,
      contentId: props.tweet.id,
      duration: duration
    }).catch(() => {})
  }
}

onMounted(() => {
  if (typeof IntersectionObserver !== 'undefined' && tweetCardRef.value) {
    const observer = new IntersectionObserver((entries) => {
      for (const entry of entries) {
        if (entry.isIntersecting) {
          startViewTracking()
        } else {
          stopViewTracking()
        }
      }
    }, { threshold: 0.5 })
    observer.observe(tweetCardRef.value)
    onBeforeUnmount(() => {
      observer.disconnect()
      stopViewTracking()
    })
  }
})

const showReplyInput = ref(false)
const replyContent = ref('')

const showGrokModal = ref(false)
const grokAnalysis = ref('')
const analyzing = ref(false)

const showQuoteModal = ref(false)
const quoteContent = ref('')

const analyzeTweet = async () => {
    showGrokModal.value = true;
    
    // Check cache first
    const cacheKey = `grok_analysis_${props.tweet.id}`;
    const cached = localStorage.getItem(cacheKey);
    if (cached) {
        grokAnalysis.value = cached;
        analyzing.value = false;
        return;
    }
    
    analyzing.value = true;
    grokAnalysis.value = '';
    
    const t = props.tweet;
    const tagsStr = t.tags && t.tags.length ? t.tags.map(tag => '#' + tag.name).join(' ') : '无标签';
    const prompt = `你是社交媒体分析专家 Grok，请从以下维度分析这条推文：
1. 内容解读：核心观点或意图
2. 传播力评估：基于互动数据判断内容质量
3. 受众画像：推测目标受众

推文信息：
- 作者：${t.author?.username || 'Unknown'} (${t.author?.handle || '@unknown'})
- 内容："${t.content}"
- 标签：${tagsStr}
- 分类：${t.category || '未分类'}
- 互动数据：${t.likeCount} 点赞 / ${t.commentCount} 评论 / ${t.repostCount || 0} 转发 / ${t.viewCount} 浏览
- 发布时间：${formatTime(t.createdAt)}

请用简洁、有洞察力的风格回答，适当加入幽默感。`;
    
    try {
        const res = await api.post('/ai/chat', { message: prompt });
        if(res.data && res.data.reply) {
            grokAnalysis.value = res.data.reply;
            localStorage.setItem(cacheKey, res.data.reply);
        } else {
            grokAnalysis.value = "Grok seems to be sleeping.";
        }
    } catch(e) {
        grokAnalysis.value = "Failed to connect to Grok.";
    } finally {
        analyzing.value = false;
    }
}

const toggleReplyInput = () => {
    showReplyInput.value = !showReplyInput.value
}

// currentUser already declared above for view tracking

const handleLike = async () => {
    if (props.tweet.liked) return; 
    
    // Optimistic UI update
    props.tweet.likeCount++;
    props.tweet.liked = true;
    
    try {
        await api.post('/behavior/like', { 
            userId: currentUser.id, 
            contentId: props.tweet.id 
        });
    } catch (e) {
        props.tweet.likeCount--;
        props.tweet.liked = false;
        console.error("Like failed", e);
    }
}

const submitReply = async () => {
    if (!replyContent.value.trim()) return;
    try {
        const res = await api.post(`/content/${props.tweet.id}/comment`, {
            authorId: currentUser.id,
            content: replyContent.value
        });
        
        // Optimistic UI update
        if (!props.tweet.replies) props.tweet.replies = [];
        props.tweet.replies.push(res.data);
        props.tweet.commentCount++;
        
        showReplyInput.value = false;
        replyContent.value = '';
        ElMessage.success('Reply posted');
    } catch (e) {
        ElMessage.error('Failed to reply');
    }
}

const goToProfile = (author) => {
    if (author?.id) {
        // If it's current user, go to /profile, else /profile/:id (future) or just handle logic
        // For now, let's just go to /profile?userId=... or assume global profile is own.
        // The requirement is "Enter User Homepage".
        // Use query param for now as ProfileView might be capable?
        // Actually ProfileView is currently hardcoded to current user.
        // I will add query support to ProfileView later. For now, just navigation.
        router.push(`/profile?userId=${author.id}`);
    }
}

const goToDetail = () => {
    router.push(`/tweet/${props.tweet.id}`);
}

// 跳转到被引用的原帖
const goToQuotedPost = () => {
    if (props.tweet.quoteOf?.id) {
        router.push(`/tweet/${props.tweet.quoteOf.id}`);
    }
}

const formatTime = (timeArray) => {
    if (!timeArray) return 'now';
    if (Array.isArray(timeArray)) {
        return new Date(timeArray[0], timeArray[1]-1, timeArray[2]).toLocaleDateString();
    }
    return new Date(timeArray).toLocaleDateString();
}

const parsedContent = computed(() => {
    if (!props.tweet.content) return [];
    const text = props.tweet.content;
    const parts = [];
    let lastIndex = 0;
    const regex = /#(\w+)/g;
    let match;
    
    while ((match = regex.exec(text)) !== null) {
        if (match.index > lastIndex) {
            parts.push({ text: text.slice(lastIndex, match.index), isTag: false });
        }
        parts.push({ text: match[0], isTag: true });
        lastIndex = regex.lastIndex;
    }
    if (lastIndex < text.length) {
        parts.push({ text: text.slice(lastIndex), isTag: false });
    }
    return parts;
})

const handleTagClick = (tag) => {
    router.push(`/search?q=${encodeURIComponent(tag)}`);
}

const handleRepostCommand = async (command) => {
    if (command === 'repost') {
        try {
            await api.post(`/content/${props.tweet.id}/repost`, { authorId: currentUser.id });
            props.tweet.repostCount = (props.tweet.repostCount || 0) + 1;
            ElMessage.success('转发成功');
        } catch (e) {
            ElMessage.error('转发失败');
        }
    } else if (command === 'quote') {
        showQuoteModal.value = true;
    }
}

const submitQuote = async () => {
    if (!quoteContent.value.trim()) return;
    try {
        await api.post(`/content/${props.tweet.id}/quote`, {
            authorId: currentUser.id,
            content: quoteContent.value
        });
        props.tweet.repostCount = (props.tweet.repostCount || 0) + 1;
        showQuoteModal.value = false;
        quoteContent.value = '';
        ElMessage.success('引用发布成功');
    } catch (e) {
        ElMessage.error('引用失败');
    }
}

const handleDelete = async () => {
    if (!confirm('确定要删除这条帖子吗？')) return;
    try {
        await api.delete(`/content/${props.tweet.id}?userId=${currentUser.id}`);
        ElMessage.success('已删除');
        emit('deleted', props.tweet.id);
    } catch (e) {
        // 全局拦截器已处理
    }
}
</script>

<style scoped>
.tweet-card {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #eff3f4;
  cursor: pointer;
  transition: background 0.2s;
}

.tweet-card:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.tweet-avatar {
  margin-right: 12px;
  flex-shrink: 0;
}

.tweet-content {
  flex: 1;
  min-width: 0;
}

.tweet-header {
  display: flex;
  align-items: center;
  margin-bottom: 2px;
  font-size: 15px;
}

.username {
  font-weight: bold;
  color: #0f1419;
  margin-right: 4px;
}

.handle, .time, .dot {
  color: #536471;
}

.dot { margin: 0 4px; }

.tweet-text {
  font-size: 15px;
  color: #0f1419;
  line-height: 20px;
  white-space: pre-wrap;
  margin-bottom: 12px;
}

.tweet-image img {
  width: 100%;
  border-radius: 16px;
  border: 1px solid #cfd9de;
  margin-bottom: 12px;
}

.tweet-actions {
  display: flex;
  justify-content: space-between;
  max-width: 425px;
  color: #536471;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  transition: color 0.2s;
}

.action-item:hover { color: #1d9bf0; }
.like-action:hover { color: #f91880; }
.like-action.liked { color: #f91880; }

.action-item .el-icon { font-size: 18px; }

.hashtag {
    color: #1d9bf0;
    cursor: pointer;
}
.hashtag:hover {
    text-decoration: underline;
}

/* Reply Styles */
.inline-reply {
    margin-top: 10px;
    padding: 10px;
    background: #f7f9f9;
    border-radius: 12px;
}
.inline-reply textarea {
    width: 100%;
    border: 1px solid #cfd9de;
    border-radius: 8px;
    padding: 8px;
    margin-bottom: 8px;
    resize: none;
    font-family: inherit;
}
.reply-actions { text-align: right; }
.reply-actions button {
    background: #1d9bf0;
    color: white;
    border: none;
    padding: 6px 12px;
    border-radius: 16px;
    cursor: pointer;
    font-weight: bold;
}
.reply-actions button:disabled { opacity: 0.5; }

.replies-container {
    margin-top: 10px;
    margin-left: -40px; /* Offset avatar margin */
    border-left: 2px solid #eff3f4;
    padding-left: 10px;
}

.header-actions {
    margin-left: auto;
}
.grok-btn {
    cursor: pointer;
    font-size: 16px;
    color: #536471;
    transition: color 0.2s;
}
.grok-btn:hover {
    color: #1d9bf0;
}
.delete-btn {
    cursor: pointer;
    font-size: 16px;
    color: #536471;
    transition: color 0.2s;
    margin-right: 8px;
}
.delete-btn:hover {
    color: #f4212e;
}

.grok-content {
    display: flex;
    gap: 15px;
}
.grok-avatar-box { flex-shrink: 0; }
.grok-avatar {
    width: 40px;
    height: 40px;
    background: #000;
    color: #fff;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 20px;
}
.analysis-text {
    flex: 1;
    font-size: 15px;
    line-height: 1.6;
    color: #0f1419;
    white-space: pre-wrap;
}

.typing-dot {
    animation: blink 1.4s infinite both;
    margin: 0 2px;
    font-weight: bold;
    font-size: 20px;
}
.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
    0% { opacity: 0.2; }
    20% { opacity: 1; }
    100% { opacity: 0.2; }
}

/* 引用帖子显示样式 */
.quoted-tweet-display {
    margin-top: 12px;
    padding: 12px;
    border: 1px solid #e1e8ed;
    border-radius: 16px;
    background: #f7f9fa;
    cursor: pointer;
    transition: background 0.2s;
}

.quoted-tweet-display:hover {
    background: #eff3f4;
}

.quoted-header {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 8px;
}

.quoted-username {
    font-weight: 700;
    font-size: 14px;
    color: #0f1419;
}

.quoted-handle {
    font-size: 14px;
    color: #536471;
}

.quoted-content {
    font-size: 14px;
    color: #0f1419;
    line-height: 1.4;
    word-break: break-word;
}

.quoted-image {
    margin-top: 10px;
}

.quoted-image img {
    max-width: 100%;
    max-height: 150px;
    border-radius: 12px;
    object-fit: cover;
}

/* AI 智能标签 */
.tweet-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 10px;
    margin-top: 8px;
}

.tag-item {
    color: #1DA1F2;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.15s ease;
}

.tag-item:hover {
    text-decoration: underline;
    color: #0d8bd9;
}
</style>
