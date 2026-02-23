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
            <el-table-column label="Actions">
                <template #default="scope">
                    <el-button size="small" @click="viewPersona(scope.row)">View Persona</el-button>
                </template>
            </el-table-column>
        </el-table>

        <el-dialog v-model="showPersonaModal" title="User Persona" width="450px">
            <div class="modal-body" v-if="currentPersona">
                <PersonaCard :persona="currentPersona" ref="cardRef" />
            </div>
            <template #footer>
                <el-button type="primary" @click="exportCard">Export Image</el-button>
            </template>
        </el-dialog>
    </div>
  </Layout>
</template>

<script setup>
import Layout from '../components/Layout.vue'
import PersonaCard from '../components/PersonaCard.vue'
import { ref, onMounted, nextTick } from 'vue'
import api from '../api'
import html2canvas from 'html2canvas'

const users = ref([])
const showPersonaModal = ref(false)
const currentPersona = ref(null)
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

const exportCard = async () => {
    // Need to access the DOM element inside the child component
    // Assuming PersonaCard exposes or we wrap it. 
    // Wait for render
    await nextTick();
    const el = document.querySelector('.persona-card'); // Simple selector for now since modal is active
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
</style>
