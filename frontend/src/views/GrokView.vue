<template>
  <Layout>
    <div class="grok-wrapper">
      <!-- 对话列表侧栏 -->
      <div class="conv-sidebar" :class="{ collapsed: !showSidebar }">
        <div class="conv-sidebar-header">
          <span v-if="showSidebar" class="conv-sidebar-title">对话列表</span>
          <button class="toggle-sidebar-btn" @click="showSidebar = !showSidebar">
            <el-icon><Operation /></el-icon>
          </button>
        </div>
        <div v-if="showSidebar" class="conv-sidebar-body">
          <button class="new-conv-btn" @click="createConversation">+ 新对话</button>
          <div class="conv-list">
            <div
              v-for="conv in conversations"
              :key="conv.id"
              class="conv-item"
              :class="{ active: conv.id === activeConvId }"
              @click="switchConversation(conv.id)"
            >
              <div class="conv-item-title">{{ conv.title || '新对话' }}</div>
              <div class="conv-item-meta">{{ conv.messageCount || 0 }} 条消息</div>
              <button class="conv-delete-btn" @click.stop="deleteConversation(conv.id)" title="删除">×</button>
            </div>
            <div v-if="conversations.length === 0" class="conv-empty">暂无对话</div>
          </div>
        </div>
      </div>

      <!-- 主聊天区 -->
      <div class="grok-container">
        <!-- Header -->
        <div class="grok-header">
            <button class="toggle-sidebar-btn mobile-toggle" @click="showSidebar = !showSidebar" v-if="!showSidebar">
              <el-icon><Operation /></el-icon>
            </button>
            <h3>Grok</h3>
            <span class="beta-badge">Early Access</span>
            <span v-if="activeConv" class="conv-name">{{ activeConv.title || '新对话' }}</span>
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
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import { ref, computed, onMounted, nextTick } from 'vue'
import { Position, Operation } from '@element-plus/icons-vue'
import api from '../api'

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;
const uid = currentUser ? currentUser.id : 'guest';

// ---- Storage keys ----
const convListKey = `grok_convs_${uid}`;
const convMsgKey = (convId) => `grok_msg_${uid}_${convId}`;

// ---- State ----
const showSidebar = ref(true)
const conversations = ref([])   // [{ id, title, messageCount, createdAt }]
const activeConvId = ref(null)
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const chatContainer = ref(null)

const activeConv = computed(() => conversations.value.find(c => c.id === activeConvId.value))

// ---- Persistence helpers ----
const saveConvList = () => {
  localStorage.setItem(convListKey, JSON.stringify(conversations.value))
}
const saveMessages = () => {
  if (!activeConvId.value) return
  localStorage.setItem(convMsgKey(activeConvId.value), JSON.stringify(messages.value))
  // Update messageCount in conv list
  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (conv) {
    conv.messageCount = messages.value.length
    saveConvList()
  }
}
const loadMessages = (convId) => {
  const raw = localStorage.getItem(convMsgKey(convId))
  return raw ? JSON.parse(raw) : []
}

// ---- Conversation management ----
const createConversation = () => {
  const id = Date.now().toString(36) + Math.random().toString(36).slice(2, 6)
  const conv = { id, title: '新对话', messageCount: 0, createdAt: Date.now() }
  conversations.value.unshift(conv)
  saveConvList()
  switchConversation(id)
}

const switchConversation = (convId) => {
  activeConvId.value = convId
  messages.value = loadMessages(convId)
  localStorage.setItem(`grok_active_${uid}`, convId)
  nextTick(() => scrollToBottom())
}

const deleteConversation = (convId) => {
  conversations.value = conversations.value.filter(c => c.id !== convId)
  localStorage.removeItem(convMsgKey(convId))
  saveConvList()
  if (activeConvId.value === convId) {
    if (conversations.value.length > 0) {
      switchConversation(conversations.value[0].id)
    } else {
      createConversation()
    }
  }
}

const clearChat = () => {
  if (!confirm('确定清空当前对话记录？')) return
  messages.value = []
  saveMessages()
  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (conv) { conv.title = '新对话'; conv.messageCount = 0; saveConvList() }
}

// ---- Chat logic ----
const formatMessage = (text) => text.replace(/\n/g, '<br>')

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) chatContainer.value.scrollTop = chatContainer.value.scrollHeight
}

const sendMessage = async () => {
  const text = inputMessage.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', content: text })
  inputMessage.value = ''
  loading.value = true

  // Auto-title: use first user message as title
  const conv = conversations.value.find(c => c.id === activeConvId.value)
  if (conv && conv.title === '新对话') {
    conv.title = text.length > 20 ? text.slice(0, 20) + '...' : text
    saveConvList()
  }

  saveMessages()
  scrollToBottom()

  try {
    const res = await api.post('/ai/chat', {
      message: text,
      history: messages.value.filter(m => m.role === 'user' || m.role === 'assistant').slice(-20)
    })
    if (res.data && res.data.reply) {
      messages.value.push({ role: 'assistant', content: res.data.reply })
    } else {
      messages.value.push({ role: 'assistant', content: 'Connection error.' })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: 'Grok is currently offline. (Check console)' })
    console.error(e)
  } finally {
    loading.value = false
    saveMessages()
    scrollToBottom()
  }
}

// ---- Migration: import old single-conversation data ----
const migrateOldChat = () => {
  const oldKey = currentUser ? `grok_chat_${currentUser.id}` : 'grok_chat_guest'
  const oldData = localStorage.getItem(oldKey)
  if (oldData) {
    const oldMessages = JSON.parse(oldData)
    if (oldMessages.length > 0) {
      const id = 'migrated_' + Date.now().toString(36)
      const firstUserMsg = oldMessages.find(m => m.role === 'user')
      const title = firstUserMsg ? (firstUserMsg.content.length > 20 ? firstUserMsg.content.slice(0, 20) + '...' : firstUserMsg.content) : '旧对话'
      conversations.value.push({ id, title, messageCount: oldMessages.length, createdAt: Date.now() })
      localStorage.setItem(convMsgKey(id), JSON.stringify(oldMessages))
      saveConvList()
    }
    localStorage.removeItem(oldKey)
  }
}

// ---- Init ----
onMounted(() => {
  // Load conversation list
  const raw = localStorage.getItem(convListKey)
  conversations.value = raw ? JSON.parse(raw) : []

  // Migrate old single-chat data if exists
  migrateOldChat()

  // Restore last active or create first conversation
  if (conversations.value.length === 0) {
    createConversation()
  } else {
    const lastActive = localStorage.getItem(`grok_active_${uid}`)
    const target = conversations.value.find(c => c.id === lastActive) ? lastActive : conversations.value[0].id
    switchConversation(target)
  }
})
</script>

<style scoped>
.grok-wrapper {
    display: flex;
    height: 100vh;
    max-width: 800px;
}

/* ---- Conversation Sidebar ---- */
.conv-sidebar {
    width: 260px;
    min-width: 260px;
    border-right: 1px solid #eff3f4;
    display: flex;
    flex-direction: column;
    background: #f7f9f9;
    transition: all 0.25s ease;
}
.conv-sidebar.collapsed {
    width: 0;
    min-width: 0;
    overflow: hidden;
    border-right: none;
}
.conv-sidebar-header {
    padding: 10px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 53px;
    border-bottom: 1px solid #eff3f4;
}
.conv-sidebar-title {
    font-weight: 700;
    font-size: 16px;
    color: #0f1419;
}
.toggle-sidebar-btn {
    background: transparent;
    border: none;
    cursor: pointer;
    padding: 6px;
    border-radius: 50%;
    color: #536471;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.2s;
}
.toggle-sidebar-btn:hover {
    background: #e7e7e8;
}
.conv-sidebar-body {
    flex: 1;
    overflow-y: auto;
    padding: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;
}
.new-conv-btn {
    width: 100%;
    padding: 10px;
    background: #0f1419;
    color: white;
    border: none;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    margin-bottom: 8px;
    transition: background 0.2s;
}
.new-conv-btn:hover {
    background: #272c30;
}
.conv-list {
    display: flex;
    flex-direction: column;
    gap: 2px;
}
.conv-item {
    padding: 10px 12px;
    border-radius: 12px;
    cursor: pointer;
    position: relative;
    transition: background 0.15s;
}
.conv-item:hover {
    background: #e7e7e8;
}
.conv-item.active {
    background: #e8f5fd;
}
.conv-item-title {
    font-size: 14px;
    font-weight: 600;
    color: #0f1419;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    padding-right: 20px;
}
.conv-item-meta {
    font-size: 12px;
    color: #536471;
    margin-top: 2px;
}
.conv-delete-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    background: transparent;
    border: none;
    color: #536471;
    font-size: 16px;
    cursor: pointer;
    border-radius: 50%;
    width: 22px;
    height: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: all 0.15s;
}
.conv-item:hover .conv-delete-btn {
    opacity: 1;
}
.conv-delete-btn:hover {
    background: #f4212e;
    color: white;
}
.conv-empty {
    text-align: center;
    color: #536471;
    font-size: 13px;
    padding: 20px 0;
}

/* ---- Main Chat ---- */
.grok-container {
    flex: 1;
    height: 100vh;
    display: flex;
    flex-direction: column;
    border-right: 1px solid #eff3f4;
    position: relative;
    background: #fff;
    min-width: 0;
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
.conv-name {
    font-size: 13px;
    color: #536471;
    max-width: 200px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
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
.mobile-toggle {
    margin-right: 4px;
}

.chat-history {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding-bottom: 150px;
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
