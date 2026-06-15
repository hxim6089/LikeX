<template>
  <Layout>
    <div class="messages-container">
      <!-- Left: Conversations List -->
      <div class="conversations-list">
        <div class="list-header">
            <h3>Messages</h3>
            <el-button circle size="small" @click="startNewChat"><el-icon><Plus /></el-icon></el-button>
        </div>
        
        <div v-if="loadingList" class="loading-state">
            <el-icon class="is-loading"><Loading /></el-icon>
        </div>

        <div 
            v-for="conv in conversations" 
            :key="conv.partner.id" 
            class="conversation-item" 
            :class="{ active: currentPartner && currentPartner.id === conv.partner.id }"
            @click="selectConversation(conv.partner)"
        >
            <el-avatar :size="40" :src="conv.partner.avatarUrl || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
            <div class="conv-info">
                <div class="top-row">
                    <span class="name">{{ conv.partner.username }}</span>
                    <span class="date">{{ formatTime(conv.timestamp) }}</span>
                </div>
                <div class="last-msg">{{ conv.latestMessage }}</div>
            </div>
        </div>
        
        <el-empty v-if="conversations.length === 0 && !loadingList" description="No conversations" />
      </div>

      <!-- Right: Chat Window -->
      <div class="chat-window">
        <div v-if="currentPartner" class="chat-content">
            <div class="chat-header">
                <span class="partner-name">{{ currentPartner.username }}</span>
            </div>

            <div class="messages-scroll" ref="scrollContainer">
                <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ 'mine': msg.senderId === currentUser.id }">
                     <div class="msg-bubble">
                        {{ msg.content }}
                     </div>
                     <div class="msg-time">{{ formatTime(msg.createdAt, true) }}</div>
                </div>
            </div>

            <div class="input-area">
                <input 
                    v-model="inputContent" 
                    placeholder="Start a new message" 
                    @keydown.enter="sendMessage"
                />
                <button class="send-btn" @click="sendMessage" :disabled="!inputContent.trim()">
                    <el-icon><Position /></el-icon>
                </button>
            </div>
        </div>

        <div v-else class="no-selection">
            <h2>Select a message</h2>
            <p>Choose from your existing conversations, start a new one, or just keep swimming.</p>
            <el-button type="primary" size="large" @click="startNewChat">New Message</el-button>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { Plus, Position, Loading } from '@element-plus/icons-vue'
import api from '../api'
import { ElMessage, ElMessageBox } from 'element-plus'

const conversations = ref([])
const messages = ref([])
const currentPartner = ref(null)
const inputContent = ref('')
const loadingList = ref(false)
const scrollContainer = ref(null)

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

// Polling Interval
let pollTimer = null;

const loadConversations = async () => {
    if (!currentUser) return;
    try {
        const res = await api.get('/messages/conversations', { params: { userId: currentUser.id } });
        conversations.value = res.data;
    } catch(e) { console.error(e); }
}

const selectConversation = async (partner) => {
    currentPartner.value = partner;
    await loadHistory();
    scrollToBottom();
}

const loadHistory = async () => {
    if (!currentUser || !currentPartner.value) return;
    try {
        const res = await api.get('/messages/history', { 
            params: { userId: currentUser.id, targetId: currentPartner.value.id } 
        });
        messages.value = res.data;
    } catch(e) { console.error(e); }
}

const sendMessage = async () => {
    const text = inputContent.value.trim();
    if (!text || !currentPartner.value) return;

    try {
        await api.post('/messages', {
            senderId: currentUser.id,
            recipientId: currentPartner.value.id,
            content: text
        });
        inputContent.value = '';
        await loadHistory();
        scrollToBottom();
        // Refresh list to update last message preview
        loadConversations();
    } catch (e) {
        ElMessage.error('Failed to send');
    }
}

const scrollToBottom = async () => {
    await nextTick();
    if (scrollContainer.value) {
        scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight;
    }
}

const startNewChat = async () => {
    try {
        // Quick & Dirty solution: Ask for User ID to chat with. 
        // Ideal: Open a User Search Modal.
        // For MVP: Prompt or just navigate to a user profile to "Message"
        const { value } = await ElMessageBox.prompt('Enter User ID to chat with:', 'New Message');
        if (value) {
            // Check if user exists (rough check by fetching profile or just listing)
            // Simplified: Just set partner manually for now, usually you select from search
            // Let's rely on backend to handle if ID is invalid, or better, 
            // implemented "Message" button on Profile view to push router to this view with params.
            ElMessage.info('Feature: Find user via Search then click Message');
        }
    } catch(e) {}
}

const formatTime = (timeArray, specific = false) => {
    if(!timeArray) return '';
    const date = new Date(timeArray[0], timeArray[1]-1, timeArray[2], timeArray[3], timeArray[4]);
    if (specific) return date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
    return date.toLocaleDateString();
}

// Polling logic
const poll = async () => {
    if (currentPartner.value) {
        // Silent refresh history
        const res = await api.get('/messages/history', { 
            params: { userId: currentUser.id, targetId: currentPartner.value.id } 
        });
        if (res.data.length !== messages.value.length) {
            messages.value = res.data;
            scrollToBottom();
        }
    }
    // Refresh list
    loadConversations();
}

import { useRoute } from 'vue-router'
const route = useRoute()

onMounted(async () => {
    loadingList.value = true;
    await loadConversations();
    loadingList.value = false;
    
    // Check for deep link
    const targetUserId = route.query.userId;
    if (targetUserId) {
        // Check if already in conversation list
        const existing = conversations.value.find(c => String(c.partner.id) === String(targetUserId));
        if (existing) {
            selectConversation(existing.partner);
        } else {
            // New conversation, fetch user details
            try {
                const res = await api.get(`/user/${targetUserId}/persona`);
                currentPartner.value = res.data;
                // Optionally add to list immediately or wait for first message
                // For UI consistency, we can add a dummy entry or just let the chat window show
            } catch (e) { console.error("Failed to load target user", e); }
        }
    }

    pollTimer = setInterval(poll, 3000);
})

onUnmounted(() => {
    clearInterval(pollTimer);
})
</script>

<style scoped>
.messages-container {
    display: flex;
    height: 100vh;
    max-width: 900px; /* Wider for chat */
    border-right: 1px solid #eff3f4;
}

.conversations-list {
    width: 350px;
    border-right: 1px solid #eff3f4;
    overflow-y: auto;
}
.list-header {
    padding: 10px 16px;
    height: 53px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #eff3f4;
}
.list-header h3 { margin: 0; font-size: 20px; }

.conversation-item {
    display: flex;
    padding: 12px 16px;
    cursor: pointer;
    transition: background 0.2s;
}
.conversation-item:hover { background: #f7f9f9; }
.conversation-item.active { border-right: 2px solid #1d9bf0; background: #eff3f4; }

.conv-info {
    margin-left: 10px;
    flex: 1;
    min-width: 0;
}
.top-row { display: flex; justify-content: space-between; margin-bottom: 4px; }
.name { font-weight: bold; font-size: 15px; }
.date { color: #536471; font-size: 13px; }
.last-msg { color: #536471; font-size: 14px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

/* Chat Window */
.chat-window {
    flex: 1;
    display: flex;
    flex-direction: column;
}
.chat-header {
    height: 53px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #eff3f4;
    font-weight: bold;
    font-size: 18px;
    background: rgba(255,255,255,0.9);
}
.no-selection {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 20px;
    text-align: center;
}

.messages-scroll {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.msg-row {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    max-width: 70%;
}
.msg-row.mine {
    align-self: flex-end;
    align-items: flex-end;
}

.msg-bubble {
    padding: 10px 14px;
    background: #eff3f4;
    border-radius: 20px;
    border-bottom-left-radius: 4px;
    font-size: 15px;
    line-height: 1.4;
}
.msg-row.mine .msg-bubble {
    background: #1d9bf0;
    color: white;
    border-bottom-left-radius: 20px;
    border-bottom-right-radius: 4px;
}
.msg-time { font-size: 11px; color: #536471; margin-top: 4px; }

.input-area {
    padding: 12px;
    border-top: 1px solid #eff3f4;
    display: flex;
    align-items: center;
    gap: 10px;
}
.input-area input {
    flex: 1;
    padding: 10px;
    border-radius: 20px;
    border: 1px solid #cfd9de;
    background: #eff3f4;
    outline: none;
    font-size: 14px;
}
.input-area input:focus { border-color: #1d9bf0; background: white; }
.send-btn { 
    background: transparent; border: none; color: #1d9bf0; cursor: pointer;
    font-size: 20px; display: flex; 
}
.send-btn:disabled { color: #b9cad3; }
</style>
