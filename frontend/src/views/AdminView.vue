<template>
  <Layout>
    <div class="admin-container">
        <h2>Admin Dashboard</h2>
        
        <el-table :data="users" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column label="User">
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
            <el-table-column label="Actions" width="260">
                <template #default="scope">
                    <el-button size="small" @click="viewPersona(scope.row)">View Persona</el-button>
                    <el-button size="small" type="primary" @click="viewDetail(scope.row)">📊 详情</el-button>
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
import api from '../api'
import html2canvas from 'html2canvas'

const users = ref([])
const showPersonaModal = ref(false)
const showDetailModal = ref(false)
const currentPersona = ref(null)
const detailPersona = ref(null)
const cardRef = ref(null)

const fetchUsers = async () => {
    try {
        const res = await api.get('/user/all');
        users.value = res.data;
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
})
</script>

<style scoped>
.admin-container {
    padding: 20px;
}
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
