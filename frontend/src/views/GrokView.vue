<template>
  <Layout>
    <div class="grok-container">
        <!-- Header -->
        <div class="grok-header">
            <h3>Grok</h3>
            <span class="beta-badge">Early Access</span>
            <button v-if="messages.length > 0" class="clear-btn" @click="clearChat">清空对话</button>
        </div>

        <!-- Chat History -->
        <div class="chat-history" ref="chatContainer">
            <div v-if="messages.length === 0" class="welcome-box">
                <div class="grok-logo-large">𝕏</div>
                <h2>Ask Grok anything</h2>
                <p>Fun, witty, and questionable advice.</p>
            </div>

            <div v-for="(msg, index) in messages" :key="index" class="message-row" :class="msg.role">
                <div class="avatar">
                    <el-avatar v-if="msg.role === 'user'" :size="32" :src="currentUser?.avatarUrl" />
                    <div v-else class="grok-avatar">G</div>
                </div>
                <div class="bubble">
                    <div v-html="formatMessage(msg.content)"></div>
                </div>
            </div>

            <div v-if="loading" class="message-row assistant">
                <div class="avatar"><div class="grok-avatar">G</div></div>
                <div class="bubble typing">
                    <span>.</span><span>.</span><span>.</span>
                </div>
            </div>
        </div>

        <!-- Input Area -->
        <div class="input-area">
            <div class="input-wrapper">
                <textarea 
                    v-model="inputMessage" 
                    placeholder="Ask Grok..." 
                    @keydown.enter.prevent="sendMessage"
                ></textarea>
                <button class="send-btn" @click="sendMessage" :disabled="!inputMessage.trim() || loading">
                    <el-icon><Position /></el-icon>
                </button>
            </div>
            <div class="disclaimer">Grok can make mistakes. Verify important info.</div>
        </div>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import { ref, onMounted, nextTick } from 'vue'
import { Position } from '@element-plus/icons-vue'
import api from '../api'

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const chatStorageKey = currentUser ? `grok_chat_${currentUser.id}` : 'grok_chat_guest';

// Load persisted chat history
const savedChat = localStorage.getItem(chatStorageKey);
const messages = ref(savedChat ? JSON.parse(savedChat) : [])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref(null)

const saveMessages = () => {
    localStorage.setItem(chatStorageKey, JSON.stringify(messages.value));
}

const clearChat = () => {
    if (!confirm('确定清空所有对话记录？')) return;
    messages.value = [];
    localStorage.removeItem(chatStorageKey);
}

// Markdown-ish formatter (simple)
const formatMessage = (text) => {
    return text.replace(/\n/g, '<br>');
}

const scrollToBottom = async () => {
    await nextTick();
    if (chatContainer.value) {
        chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
}

const sendMessage = async () => {
    const text = inputMessage.value.trim();
    if (!text || loading.value) return;

    // 1. Add User Message
    messages.value.push({ role: 'user', content: text });
    inputMessage.value = '';
    loading.value = true;
    saveMessages();
    scrollToBottom();

    try {
        // 2. Call API with full conversation history
        const res = await api.post('/ai/chat', { 
            message: text,
            history: messages.value.filter(m => m.role === 'user' || m.role === 'assistant')
                .slice(-20) // 最近20条上下文
        });
        
        // 3. Add Assistant Message
        if (res.data && res.data.reply) {
            messages.value.push({ role: 'assistant', content: res.data.reply });
        } else {
            messages.value.push({ role: 'assistant', content: 'Connection error.' });
        }

    } catch (e) {
        messages.value.push({ role: 'assistant', content: 'Grok is currently offline. (Check console)' });
        console.error(e);
    } finally {
        loading.value = false;
        saveMessages();
        scrollToBottom();
    }
}

onMounted(() => {
    if (messages.value.length > 0) {
        scrollToBottom();
    }
})
</script>

<style scoped>
.grok-container {
    height: 100vh;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #fab005; /* Fun border implementation detail */
    border-right: 1px solid #eff3f4;
    max-width: 600px;
    position: relative;
    background: #fff;
}

.grok-header {
    padding: 10px 16px;
    border-bottom: 1px solid #eff3f4;
    display: flex;
    align-items: center;
    gap: 10px;
    height: 53px;
    background: rgba(255, 255, 255, 0.8);
    backdrop-filter: blur(10px);
    z-index: 10;
}
.grok-header h3 { margin: 0; font-weight: 800; font-size: 20px; }
.beta-badge {
    font-size: 11px;
    background: #000;
    color: #fff;
    padding: 2px 6px;
    border-radius: 4px;
    font-weight: bold;
}
.clear-btn {
    margin-left: auto;
    background: transparent;
    border: 1px solid #cfd9de;
    color: #536471;
    padding: 4px 12px;
    border-radius: 16px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
}
.clear-btn:hover {
    background: #f4212e;
    color: #fff;
    border-color: #f4212e;
}

.chat-history {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding-bottom: 150px; /* Space for input */
}

.welcome-box {
    text-align: center;
    margin-top: 20vh;
    opacity: 0.5;
}
.grok-logo-large {
    font-size: 80px;
    font-weight: bold;
    margin-bottom: 20px;
}

.message-row {
    display: flex;
    gap: 12px;
}
.message-row.user {
    flex-direction: row-reverse;
}

.avatar {
    flex-shrink: 0;
}
.grok-avatar {
    width: 32px;
    height: 32px;
    background: #000;
    color: white;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 4px;
}

.bubble {
    background: #eff3f4;
    padding: 12px 16px;
    border-radius: 20px;
    border-top-left-radius: 4px;
    font-size: 15px;
    line-height: 1.5;
    max-width: 80%;
    word-wrap: break-word;
}
.message-row.user .bubble {
    background: #1d9bf0;
    color: white;
    border-top-left-radius: 20px;
    border-top-right-radius: 4px;
}

.typing span {
    animation: blink 1.4s infinite both;
    margin: 0 2px;
    font-weight: bold;
    font-size: 20px;
}
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
    0% { opacity: 0.2; }
    20% { opacity: 1; }
    100% { opacity: 0.2; }
}

.input-area {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 10px 16px 20px;
    background: #fff;
    border-top: 1px solid #eff3f4;
}
.input-wrapper {
    background: #eff3f4;
    border-radius: 16px;
    padding: 8px;
    display: flex;
    align-items: flex-end;
}
textarea {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    resize: none;
    padding: 8px;
    font-size: 15px;
    max-height: 100px;
    font-family: inherit;
}
.send-btn {
    background: #1d9bf0;
    color: white;
    border: none;
    width: 32px;
    height: 32px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    margin-bottom: 2px;
}
.send-btn:disabled {
    opacity: 0.5;
    cursor: default;
}

.disclaimer {
    text-align: center;
    font-size: 11px;
    color: #536471;
    margin-top: 8px;
}
</style>
