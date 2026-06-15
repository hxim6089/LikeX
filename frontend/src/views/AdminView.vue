<template>
  <Layout wide title="管理后台">
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

        <!-- 推荐算法引擎切换 -->
        <div class="strategy-section">
            <h3 class="section-title">推荐算法引擎</h3>
            <div class="strategy-cards">
                <div
                    class="strategy-card"
                    :class="{ active: strategyInfo.current === 'traditional' }"
                    @click="switchStrategy('traditional')"
                >
                    <div class="strategy-icon">⚙️</div>
                    <div class="strategy-name">传统多因子算法</div>
                    <div class="strategy-desc">基于用户行为画像的多因子加权打分：互动热度、TF-IDF、话题亲和度、作者亲密度、动态权重</div>
                    <el-tag v-if="strategyInfo.current === 'traditional'" type="success" size="small" class="strategy-badge">当前使用</el-tag>
                </div>
                <div
                    class="strategy-card"
                    :class="{ active: strategyInfo.current === 'ai', disabled: !strategyInfo.aiAvailable && strategyInfo.current !== 'ai' }"
                    @click="switchStrategy('ai')"
                >
                    <div class="strategy-icon">🧠</div>
                    <div class="strategy-name">AI 大模型推荐</div>
                    <div class="strategy-desc">Ollama Qwen 8B 大语言模型驱动，智能分析内容语义并排序，生成个性化推荐理由</div>
                    <div class="strategy-status-row">
                        <el-tag v-if="strategyInfo.current === 'ai'" type="success" size="small" class="strategy-badge">当前使用</el-tag>
                        <el-tag
                            :type="strategyInfo.aiAvailable ? 'success' : 'danger'"
                            size="small"
                            effect="plain"
                        >
                            Ollama {{ strategyInfo.aiAvailable ? '在线' : '离线' }}
                        </el-tag>
                    </div>
                </div>
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

        <!-- Kaggle 数据导入 -->
        <div class="crawl-section">
            <h3 class="section-title">📊 Kaggle 数据导入</h3>
            <div class="crawl-batch-row">
                <el-button type="danger" size="large" @click="startBatchImport" :loading="batchImporting">
                    一键导入 50 条帖文
                </el-button>
                <span class="crawl-batch-hint">从 Kaggle 公开数据集 (HuffPost News 21万条) 自动导入分类帖文，Kaggle 不可用时使用内置内容库</span>
            </div>
            <div class="crawl-form">
                <div class="crawl-divider">或指定 Kaggle 数据集导入</div>
                <div class="crawl-input-row">
                    <el-input
                        v-model="datasetSlug"
                        placeholder="输入数据集路径，如 rmisra/news-category-dataset"
                        clearable
                        style="flex:1"
                        @keyup.enter="startDatasetImport"
                    >
                        <template #prepend>kaggle.com/</template>
                    </el-input>
                    <el-button type="primary" @click="startDatasetImport" :loading="importing" :disabled="!datasetSlug?.trim()">
                        导入
                    </el-button>
                </div>
            </div>
            <div v-if="importResult" class="crawl-result" :class="{ success: importResult.success, fail: !importResult.success }">
                <div class="crawl-result-header">
                    <span class="crawl-result-icon">{{ importResult.success ? '✅' : '❌' }}</span>
                    <span class="crawl-result-msg">{{ importResult.message }}</span>
                </div>
                <div v-if="importResult.success" class="crawl-result-stats">
                    <span>导入: <strong>{{ importResult.importedCount }}</strong> 条</span>
                    <span>去重跳过: <strong>{{ importResult.skippedDuplicate }}</strong> 条</span>
                </div>
            </div>
            <div v-if="importHistory.length > 0" class="crawl-history">
                <div class="crawl-history-title">导入记录</div>
                <div v-for="(h, i) in importHistory" :key="i" class="crawl-history-item">
                    <span class="crawl-history-icon">{{ h.success ? '✅' : '❌' }}</span>
                    <span class="crawl-history-name">{{ h.source || 'Kaggle 批量' }}</span>
                    <span class="crawl-history-count" v-if="h.success">+{{ h.importedCount }} 条</span>
                    <span class="crawl-history-msg" v-else>{{ h.message }}</span>
                </div>
            </div>
        </div>

        <!-- 用户管理表格 -->
        <section class="user-management">
            <div class="user-management-header">
                <div>
                    <h3 class="section-title">用户管理</h3>
                    <p>查看用户画像、调整角色并管理账号状态</p>
                </div>
                <el-tag type="info" effect="plain">{{ users.length }} 位用户</el-tag>
            </div>

            <div class="users-table-scroll">
                <el-table :data="users" class="users-table" stripe table-layout="auto">
                    <el-table-column prop="id" label="ID" width="72" />
                    <el-table-column label="用户信息" min-width="230">
                        <template #default="scope">
                            <div class="user-cell">
                                <el-avatar :src="scope.row.avatarUrl" />
                                <div class="user-cell-text">
                                    <div class="user-name">{{ scope.row.username }}</div>
                                    <div class="user-handle">{{ scope.row.handle }}</div>
                                </div>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column label="角色" width="210">
                        <template #default="scope">
                            <div class="role-cell">
                                <el-tag
                                    :type="scope.row.role === 'ADMIN' ? '' : 'info'"
                                    effect="plain"
                                    size="small"
                                >
                                    {{ scope.row.role === 'ADMIN' ? '管理员' : '普通用户' }}
                                </el-tag>
                                <el-button
                                    link
                                    type="primary"
                                    size="small"
                                    @click="toggleRole(scope.row, scope.row.role !== 'ADMIN')"
                                    :disabled="scope.row.id === currentUser?.id"
                                >
                                    {{ scope.row.role === 'ADMIN' ? '移除管理员' : '设为管理员' }}
                                </el-button>
                            </div>
                        </template>
                    </el-table-column>
                    <el-table-column label="状态" width="110">
                        <template #default="scope">
                            <el-tag
                                :type="scope.row.banned ? 'danger' : 'success'"
                                effect="plain"
                                size="small"
                            >
                                {{ scope.row.banned ? '已封禁' : '正常' }}
                            </el-tag>
                        </template>
                    </el-table-column>
                    <el-table-column label="操作" min-width="290" align="right" header-align="right">
                        <template #default="scope">
                            <div class="user-actions">
                                <el-button size="small" plain :icon="User" @click="viewPersona(scope.row)">画像</el-button>
                                <el-button size="small" type="primary" plain :icon="DataAnalysis" @click="viewDetail(scope.row)">详情</el-button>
                                <el-button
                                    size="small"
                                    plain
                                    :icon="scope.row.banned ? Unlock : Lock"
                                    :type="scope.row.banned ? 'success' : 'danger'"
                                    @click="toggleBan(scope.row)"
                                    :disabled="scope.row.id === currentUser?.id"
                                >
                                    {{ scope.row.banned ? '解封' : '封禁' }}
                                </el-button>
                            </div>
                        </template>
                    </el-table-column>
                </el-table>
            </div>
        </section>

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
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, DataAnalysis, Lock, Unlock } from '@element-plus/icons-vue'
import api from '../api'
import html2canvas from 'html2canvas'

const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : null;

const users = ref([])
const stats = ref({})
const strategyInfo = ref({ current: 'traditional', aiAvailable: false, strategies: [] })
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

const fetchStrategy = async () => {
    try {
        const res = await api.get('/admin/rec-strategy');
        strategyInfo.value = res.data;
    } catch(e) { console.error(e); }
}

const switchStrategy = async (type) => {
    if (strategyInfo.value.current === type) return;
    if (type === 'ai' && !strategyInfo.value.aiAvailable) {
        ElMessage.warning('Ollama 服务未启动，请先启动 Ollama 再切换到 AI 推荐');
        return;
    }
    try {
        await api.put('/admin/rec-strategy', { strategy: type });
        strategyInfo.value.current = type;
        ElMessage.success(`推荐算法已切换为：${type === 'ai' ? 'AI 大模型推荐' : '传统多因子算法'}`);
    } catch(e) {
        ElMessage.error('切换失败');
    }
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
    const newRole = isAdmin ? 'ADMIN' : 'USER';
    const roleLabel = isAdmin ? '管理员' : '普通用户';
    try {
        await ElMessageBox.confirm(
            `确定将 ${user.username} 的角色调整为${roleLabel}吗？`,
            '调整用户角色',
            {
                confirmButtonText: '确认调整',
                cancelButtonText: '取消',
                type: 'warning'
            }
        );
        const res = await api.put(`/user/${user.id}/role`, { role: newRole });
        user.role = res.data.role;
        ElMessage.success(`${user.username} 已调整为${roleLabel}`);
    } catch (e) {
        if (e !== 'cancel' && e !== 'close') {
            console.error(e);
            ElMessage.error('角色调整失败');
        }
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

const datasetSlug = ref('')
const importing = ref(false)
const batchImporting = ref(false)
const importResult = ref(null)
const importHistory = ref([])

const startBatchImport = async () => {
    batchImporting.value = true
    importResult.value = null
    try {
        const res = await api.post('/admin/import-kaggle-batch', { target: 50 })
        importResult.value = res.data
        importHistory.value.unshift(res.data)
        if (importHistory.value.length > 10) importHistory.value.pop()
        if (res.data.success) {
            ElMessage.success(`批量导入完成，导入 ${res.data.importedCount} 条帖文`)
            fetchStats()
        } else {
            ElMessage.warning(res.data.message)
        }
    } catch (e) {
        importResult.value = { success: false, message: '批量导入请求失败: ' + (e.response?.data?.message || e.message) }
        ElMessage.error('批量导入请求失败')
    } finally {
        batchImporting.value = false
    }
}

const startDatasetImport = async () => {
    const slug = datasetSlug.value?.trim()
    if (!slug) return
    importing.value = true
    importResult.value = null
    try {
        const res = await api.post('/admin/import-kaggle', { datasetSlug: slug })
        importResult.value = res.data
        importHistory.value.unshift(res.data)
        if (importHistory.value.length > 10) importHistory.value.pop()
        if (res.data.success) {
            ElMessage.success(`成功导入 ${res.data.importedCount} 条帖文`)
            fetchStats()
        } else {
            ElMessage.warning(res.data.message)
        }
    } catch (e) {
        importResult.value = { success: false, message: '请求失败: ' + (e.response?.data?.message || e.message) }
        ElMessage.error('导入请求失败')
    } finally {
        importing.value = false
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
    fetchStrategy();
})
</script>

<style scoped>
.admin-container {
    width: 100%;
    max-width: 1100px;
    margin: 0 auto;
    padding: 20px 24px 40px;
    box-sizing: border-box;
}

.user-management {
    margin-top: 28px;
}

.user-management-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 12px;
}

.user-management-header .section-title {
    margin: 0 0 4px;
}

.user-management-header p {
    margin: 0;
    color: #536471;
    font-size: 13px;
}

.users-table-scroll {
    width: 100%;
    overflow-x: auto;
    border-top: 1px solid #eff3f4;
}

.users-table {
    width: 100%;
    min-width: 900px;
}

.users-table :deep(.cell) {
    white-space: nowrap;
}

.users-table :deep(th.el-table__cell) {
    color: #536471;
    font-weight: 600;
    background: #fff;
}

.users-table :deep(td.el-table__cell) {
    padding: 13px 0;
}

.user-cell,
.role-cell,
.user-actions {
    display: flex;
    align-items: center;
}

.user-cell {
    gap: 10px;
    min-width: 0;
}

.user-cell-text {
    min-width: 0;
}

.user-name {
    color: #0f1419;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
}

.user-handle {
    color: #536471;
    font-size: 12px;
    overflow: hidden;
    text-overflow: ellipsis;
}

.role-cell {
    gap: 10px;
}

.user-actions {
    justify-content: flex-end;
    gap: 8px;
}

.user-actions :deep(.el-button + .el-button) {
    margin-left: 0;
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

/* 推荐算法引擎 */
.strategy-section {
    margin-bottom: 20px;
}

.section-title {
    font-size: 16px;
    font-weight: 700;
    color: #0f1419;
    margin-bottom: 12px;
}

.strategy-cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
}

.strategy-card {
    border: 2px solid #eff3f4;
    border-radius: 16px;
    padding: 20px;
    cursor: pointer;
    transition: all 0.25s;
    background: #fff;
    position: relative;
}

.strategy-card:hover {
    border-color: #1da1f2;
    box-shadow: 0 4px 16px rgba(29, 161, 242, 0.12);
}

.strategy-card.active {
    border-color: #1da1f2;
    background: linear-gradient(135deg, #f0f9ff 0%, #fff 100%);
    box-shadow: 0 4px 16px rgba(29, 161, 242, 0.15);
}

.strategy-card.disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.strategy-icon {
    font-size: 32px;
    margin-bottom: 10px;
}

.strategy-name {
    font-size: 16px;
    font-weight: 700;
    color: #0f1419;
    margin-bottom: 6px;
}

.strategy-desc {
    font-size: 13px;
    color: #536471;
    line-height: 1.5;
    margin-bottom: 10px;
}

.strategy-badge {
    margin-right: 6px;
}

.strategy-status-row {
    display: flex;
    align-items: center;
    gap: 8px;
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

/* X 爬取面板 */
.crawl-section {
    margin-bottom: 24px;
    padding: 20px;
    background: white;
    border: 1px solid #eff3f4;
    border-radius: 16px;
}
.crawl-batch-row {
    display: flex;
    align-items: center;
    gap: 16px;
    margin-top: 12px;
    margin-bottom: 16px;
}
.crawl-batch-hint {
    font-size: 12px;
    color: #536471;
}
.crawl-divider {
    font-size: 12px;
    color: #aab8c2;
    text-align: center;
    margin: 12px 0;
    position: relative;
}
.crawl-divider::before, .crawl-divider::after {
    content: '';
    position: absolute;
    top: 50%;
    width: 40%;
    height: 1px;
    background: #eff3f4;
}
.crawl-divider::before { left: 0; }
.crawl-divider::after { right: 0; }
.crawl-form { margin-top: 12px; }
.crawl-input-row {
    display: flex;
    gap: 12px;
    align-items: center;
}
.crawl-hint {
    margin-top: 8px;
    font-size: 12px;
    color: #536471;
}
.crawl-result {
    margin-top: 16px;
    padding: 14px 16px;
    border-radius: 12px;
    border: 1px solid #e1e8ed;
}
.crawl-result.success { background: #f0fdf4; border-color: #bbf7d0; }
.crawl-result.fail { background: #fef2f2; border-color: #fecaca; }
.crawl-result-header {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: 600;
}
.crawl-result-stats {
    margin-top: 8px;
    display: flex;
    gap: 20px;
    font-size: 13px;
    color: #536471;
}
.crawl-result-stats strong { color: #0f1419; }
.crawl-history {
    margin-top: 16px;
    border-top: 1px solid #eff3f4;
    padding-top: 12px;
}
.crawl-history-title {
    font-size: 13px;
    font-weight: 600;
    color: #536471;
    margin-bottom: 8px;
}
.crawl-history-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px 0;
    font-size: 13px;
    color: #0f1419;
}
.crawl-history-name { font-weight: 600; color: #1d9bf0; }
.crawl-history-count { color: #00ba7c; }
.crawl-history-msg { color: #f4212e; font-size: 12px; }

@media (max-width: 1100px) {
    .admin-container {
        padding: 16px 18px 32px;
    }

    .stats-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .strategy-cards {
        grid-template-columns: 1fr;
    }
}
</style>
