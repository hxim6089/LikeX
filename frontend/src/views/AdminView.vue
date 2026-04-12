<template>
  <Layout>
    <div class="admin-container">
        <h2>Admin Dashboard</h2>

        <!-- 平台数据概览 -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon">👥</div>
                <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
                <div class="stat-label">总用户数</div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">📝</div>
                <div class="stat-value">{{ stats.totalPosts || 0 }}</div>
                <div class="stat-label">总帖子数</div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">⚡</div>
                <div class="stat-value">{{ stats.totalBehaviors || 0 }}</div>
                <div class="stat-label">总互动数</div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">🆕</div>
                <div class="stat-value">{{ stats.todayNewPosts || 0 }}</div>
                <div class="stat-label">今日新增</div>
            </div>
        </div>

        <!-- 操作按钮组 -->
        <div class="action-bar">
            <el-button type="warning" @click="batchTagAll" :loading="tagging">
                🤖 AI 批量打标
            </el-button>
            <span v-if="tagResult" class="tag-result">
                ✅ 已处理 {{ tagResult.tagged }} / {{ tagResult.total }} 条帖子
            </span>
        </div>

        <!-- 用户管理表格 -->
        <el-table :data="users" style="width: 100%" stripe>
            <el-table-column prop="id" label="ID" width="70" />
            <el-table-column label="用户" min-width="180">
                <template #default="scope">
                    <div style="display:flex;align-items:center;gap:10px">
                        <el-avatar :src="scope.row.avatarUrl" />
                        <div>
                            <div>{{ scope.row.username }}</div>
                            <div style="font-size:12px;color:#888">{{ scope.row.handle }}</div>
                        </div>
                    </div>
                </template>
            </el-table-column>
            <el-table-column label="角色" width="150">
                <template #default="scope">
                    <el-switch
                        :model-value="scope.row.role === 'ADMIN'"
                        active-text="管理员"
                        inactive-text="用户"
                        @change="val => toggleRole(scope.row, val)"
                        :disabled="scope.row.id === currentUser?.id"
                    />
                </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
                <template #default="scope">
                    <el-tag :type="scope.row.banned ? 'danger' : 'success'" size="small">
                        {{ scope.row.banned ? '已封禁' : '正常' }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="操作" width="320">
                <template #default="scope">
                    <el-button size="small" @click="viewPersona(scope.row)">画像</el-button>
                    <el-button size="small" type="primary" @click="viewDetail(scope.row)">📊 详情</el-button>
                    <el-button
                        size="small"
                        :type="scope.row.banned ? 'success' : 'danger'"
                        @click="toggleBan(scope.row)"
                        :disabled="scope.row.id === currentUser?.id"
                    >
                        {{ scope.row.banned ? '解封' : '封禁' }}
                    </el-button>
                </template>
            </el-table-column>
        </el-table>

        <!-- 简版画像卡片 Modal -->
        <el-dialog v-model="showPersonaModal" title="User Persona" width="450px">
            <div class="modal-body" v-if="currentPersona">
                <PersonaCard :persona="currentPersona" ref="cardRef" />
            </div>
            <template #footer>
                <el-button type="primary" @click="exportCard">Export Image</el-button>
            </template>
        </el-dialog>

        <!-- 画像详情 Modal -->
        <el-dialog v-model="showDetailModal" title="📊 画像详情" width="820px" destroy-on-close>
            <div class="detail-body" v-if="detailPersona">
                <!-- 用户头部 -->
                <div class="detail-user-header">
                    <el-avatar :size="56" :src="detailPersona.avatarUrl" />
                    <div>
                        <div class="detail-name">{{ detailPersona.username }}</div>
                        <div class="detail-handle">{{ detailPersona.handle }}</div>
                        <div class="detail-title">{{ detailPersona.personaTitle }}</div>
                    </div>
                </div>

                <!-- 概览仪表盘 -->
                <div class="overview-dashboard">
                    <div class="overview-item">
                        <span class="ov-icon">🧬</span>
                        <span class="ov-value">{{ detailPersona.userTypeDetail?.label || detailPersona.userTypeDetail?.type || '—' }}</span>
                        <span class="ov-label">用户类型</span>
                    </div>
                    <div class="overview-item">
                        <span class="ov-icon">⚡</span>
                        <span class="ov-value">{{ detailPersona.activityLevel || '—' }}<small v-if="detailPersona.activityScore">({{ detailPersona.activityScore }}分)</small></span>
                        <span class="ov-label">活跃度</span>
                    </div>
                    <div class="overview-item">
                        <span class="ov-icon">📏</span>
                        <span class="ov-value">{{ getReadPref(detailPersona) }}</span>
                        <span class="ov-label">阅读偏好</span>
                    </div>
                    <div class="overview-item">
                        <span class="ov-icon">🖼️</span>
                        <span class="ov-value">{{ Math.round((detailPersona.contentPreference?.imagePreference || 0) * 100) }}%</span>
                        <span class="ov-label">图片偏好</span>
                    </div>
                    <div class="overview-item">
                        <span class="ov-icon">🌐</span>
                        <span class="ov-value">{{ Math.round((detailPersona.contentPreference?.topicDiversity || 0) * 100) }}%</span>
                        <span class="ov-label">话题多样性</span>
                    </div>
                    <div class="overview-item">
                        <span class="ov-icon">🦉</span>
                        <span class="ov-value">{{ Math.round((detailPersona.nightOwlIndex || 0) * 100) }}%</span>
                        <span class="ov-label">夜猫子指数</span>
                    </div>
                </div>

                <!-- 子组件 -->
                <InterestWordCloud :data="detailPersona.wordCloudData || []" />
                <BehaviorRadarChart :stats="detailPersona.behaviorStats || {}" />
                <MatchRateBar 
                    :data="detailPersona.categoryDistribution || []" 
                    :matchRate="detailPersona.recommendationMatchRate" 
                />
                <PersonaDetailCard :persona="detailPersona" />
            </div>
        </el-dialog>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import PersonaCard from '../components/PersonaCard.vue'
import InterestWordCloud from '../components/InterestWordCloud.vue'
import BehaviorRadarChart from '../components/BehaviorRadarChart.vue'
import MatchRateBar from '../components/MatchRateBar.vue'
import PersonaDetailCard from '../components/PersonaDetailCard.vue'
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'
import html2canvas from 'html2canvas'

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const users = ref([])
const stats = ref({})
const showPersonaModal = ref(false)
const showDetailModal = ref(false)
const currentPersona = ref(null)
const detailPersona = ref(null)
const cardRef = ref(null)
const tagging = ref(false)
const tagResult = ref(null)

const fetchUsers = async () => {
    try {
        const res = await api.get('/user/all');
        users.value = res.data;
    } catch(e) { console.error(e); }
}

const fetchStats = async () => {
    try {
        const res = await api.get('/admin/stats');
        stats.value = res.data;
    } catch(e) { console.error(e); }
}

const viewPersona = async (user) => {
    try {
        const res = await api.get(`/user/${user.id}/persona`);
        currentPersona.value = res.data;
        showPersonaModal.value = true;
    } catch(e) { console.error(e); }
}

const viewDetail = async (user) => {
    try {
        const res = await api.get(`/user/${user.id}/persona`);
        detailPersona.value = res.data;
        showDetailModal.value = true;
    } catch(e) { console.error(e); }
}

const toggleRole = async (user, isAdmin) => {
    try {
        const newRole = isAdmin ? 'ADMIN' : 'USER';
        const res = await api.put(`/user/${user.id}/role`, { role: newRole });
        user.role = res.data.role;
        ElMessage.success(`${user.username} 角色已切换为 ${newRole}`);
    } catch (e) {
        console.error(e);
    }
}

const toggleBan = async (user) => {
    const action = user.banned ? '解封' : '封禁';
    if (!confirm(`确定要${action} ${user.username} 吗？`)) return;
    try {
        const res = await api.put(`/user/${user.id}/ban`);
        user.banned = res.data.banned;
        ElMessage.success(`${user.username} 已${action}`);
    } catch (e) {
        console.error(e);
    }
}

const batchTagAll = async () => {
    tagging.value = true;
    tagResult.value = null;
    try {
        const res = await api.post('/ai/tag-all');
        tagResult.value = res.data;
        ElMessage.success('AI 批量打标完成');
    } catch (e) {
        ElMessage.error('打标失败，请确认 Ollama 服务已启动');
    } finally {
        tagging.value = false;
    }
}

const getReadPref = (persona) => {
    const len = persona?.contentPreference?.avgReadLength
    if (len === 'long') return '长文爱好者'
    if (len === 'medium') return '中等篇幅'
    if (len === 'short') return '快餐阅读'
    return '未知'
}

const exportCard = async () => {
    await nextTick();
    const el = document.querySelector('.persona-card');
    if (!el) return;
    
    try {
        const canvas = await html2canvas(el, { scale: 2, backgroundColor: null });
        const link = document.createElement('a');
        link.download = `persona-${currentPersona.value.handle}.png`;
        link.href = canvas.toDataURL();
        link.click();
    } catch(e) { console.error(e); }
}

onMounted(() => {
    fetchUsers();
    fetchStats();
})
</script>

<style scoped>
.admin-container {
    padding: 20px;
}

/* 统计卡片 */
.stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    margin-bottom: 20px;
}

.stat-card {
    background: linear-gradient(135deg, #f8f9fa 0%, #fff 100%);
    border: 1px solid #eff3f4;
    border-radius: 16px;
    padding: 20px;
    text-align: center;
    transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(29, 161, 242, 0.12);
}

.stat-icon {
    font-size: 28px;
    margin-bottom: 8px;
}

.stat-value {
    font-size: 28px;
    font-weight: 800;
    color: #0f1419;
}

.stat-label {
    font-size: 13px;
    color: #536471;
    margin-top: 4px;
}

/* 操作栏 */
.action-bar {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding: 12px 16px;
    background: #f7f9fa;
    border-radius: 12px;
}

.tag-result {
    font-size: 14px;
    color: #00ba7c;
    font-weight: 500;
}

/* Modal */
.modal-body {
    display: flex;
    justify-content: center;
}

/* 画像详情弹窗 */
.detail-body {
    max-height: 70vh;
    overflow-y: auto;
    padding: 0 4px;
}

.detail-user-header {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid #eff3f4;
}

.detail-name {
    font-size: 20px;
    font-weight: 800;
    color: #0f1419;
}

.detail-handle {
    font-size: 14px;
    color: #536471;
}

.detail-title {
    font-size: 14px;
    color: #1da1f2;
    font-weight: 600;
    margin-top: 2px;
}

/* 概览仪表盘 */
.overview-dashboard {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
    gap: 10px;
    margin-bottom: 16px;
    padding: 16px;
    background: #f7f9fa;
    border-radius: 16px;
}

.overview-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px 6px;
    border-radius: 10px;
    background: white;
    transition: transform 0.2s;
}

.overview-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(29,161,242,0.12);
}

.ov-icon { font-size: 22px; }
.ov-value { font-size: 13px; font-weight: 700; color: #0f1419; text-align: center; }
.ov-value small { font-size: 10px; font-weight: 500; color: #536471; margin-left: 2px; }
.ov-label { font-size: 10px; color: #536471; }
</style>
