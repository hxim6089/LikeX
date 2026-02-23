<template>
  <Layout>
    <div class="detail-container" v-if="tweet">
      <!-- Header -->
      <div class="header">
        <el-icon class="back-btn" @click="$router.back()"><ArrowLeft /></el-icon>
        <h2>Tweet</h2>
      </div>

      <!-- Main Tweet -->
      <div class="main-tweet">
        <div class="user-row">
            <el-avatar :src="tweet.author.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
            <div class="user-meta">
                <div class="name">{{ tweet.author.username }}</div>
                <div class="handle">{{ tweet.author.handle }}</div>
            </div>
        </div>
        <div class="tweet-content">
            <template v-for="(part, index) in parsedContent" :key="index">
                <span v-if="part.isTag" class="hashtag" @click.stop="handleTagClick(part.text)">{{ part.text }}</span>
                <span v-else>{{ part.text }}</span>
            </template>
        </div>
        <div class="tweet-time">{{ new Date(tweet.createdAt).toLocaleString() }}</div>
        
        <div class="stats-border">
            <span class="stat"><strong>{{ tweet.likeCount }}</strong> Likes</span>
            <span class="stat"><strong>{{ tweet.commentCount }}</strong> Comments</span>
        </div>
      </div>

      <!-- Reply Box -->
      <div class="reply-area">
        <el-avatar :size="40" :src="currentUser?.avatarUrl" />
        <div class="input-wrapper">
            <textarea v-model="replyText" placeholder="Post your reply" rows="2"></textarea>
            <button class="reply-btn" @click="postReply" :disabled="!replyText.trim()">Reply</button>
        </div>
      </div>

      <!-- Comments List -->
      <div class="comments-list">
        <TweetCard v-for="comment in comments" :key="comment.id" :tweet="comment" />
      </div>

    </div>
    <div v-else class="loading">Loading...</div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import TweetCard from '../components/TweetCard.vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../api'

const route = useRoute()
const tweet = ref(null)
const comments = ref([])
const replyText = ref('')
const router = useRouter()

import { computed } from 'vue'
import { useRouter } from 'vue-router'

const parsedContent = computed(() => {
    if (!tweet.value || !tweet.value.content) return [];
    const text = tweet.value.content;
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

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const fetchTweet = async () => {
    try {
        const id = route.params.id;
        const res = await api.get(`/content/${id}`);
        tweet.value = res.data;
    } catch (e) { console.error(e) }
}

const fetchComments = async () => {
    try {
        const id = route.params.id;
        const res = await api.get(`/content/${id}/comments`);
        comments.value = res.data;
    } catch (e) { console.error(e) }
}

const postReply = async () => {
    if(!currentUser) return alert("Please login");
    try {
        await api.post(`/content/${tweet.value.id}/comment`, {
            authorId: currentUser.id,
            content: replyText.value
        });
        replyText.value = '';
        fetchComments(); // Refresh list
        tweet.value.commentCount++; // Optimistic update
    } catch (e) { console.error(e) }
}

onMounted(() => {
    fetchTweet();
    fetchComments();
})
</script>

<style scoped>
.detail-container { width: 600px; border-right: 1px solid #eff3f4; min-height: 100vh; }
.header { display: flex; align-items: center; padding: 10px; cursor: pointer; }
.back-btn { font-size: 20px; margin-right: 20px; }

.main-tweet { padding: 15px; border-bottom: 1px solid #eff3f4; }
.user-row { display: flex; margin-bottom: 15px; }
.user-meta { margin-left: 10px; }
.name { font-weight: bold; }
.handle { color: #536471; }
.tweet-content { font-size: 20px; line-height: 1.5; margin-bottom: 15px; }
.tweet-time { color: #536471; margin-bottom: 15px; }
.stats-border { border-top: 1px solid #eff3f4; border-bottom: 1px solid #eff3f4; padding: 15px 0; }
.stat { margin-right: 20px; }

.reply-area { display: flex; padding: 15px; border-bottom: 1px solid #eff3f4; }
.input-wrapper { flex: 1; margin-left: 10px; }
textarea { width: 100%; border: none; outline: none; font-size: 18px; resize: none; font-family: inherit; }
.reply-btn { background: #1d9bf0; color: white; border: none; padding: 8px 16px; border-radius: 20px; font-weight: bold; float: right; cursor: pointer; }
.reply-btn:disabled { opacity: 0.5; }

.hashtag {
    color: #1d9bf0;
    cursor: pointer;
}
.hashtag:hover {
    text-decoration: underline;
}
</style>
