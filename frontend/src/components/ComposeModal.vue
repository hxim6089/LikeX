<template>
  <div class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <button class="close-btn" @click="close">×</button>
        <button class="submit-btn" :disabled="!content.trim()" @click="submit">Post</button>
      </div>
      <div class="modal-body">
        <div class="avatar">
           <el-avatar :size="40" :src="currentUser.avatarUrl" />
        </div>
        <div class="input-area">
            <textarea 
                v-model="content" 
                placeholder="What is happening?!" 
                rows="5"
            ></textarea>
            <div class="image-preview" v-if="imageUrl">
                <img :src="imageUrl" />
                <button class="remove-btn" @click="removeImage">×</button>
            </div>
        </div>
      </div>
      <div class="modal-footer">
        <el-icon class="media-icon" @click="triggerUpload"><Picture /></el-icon>
        <el-icon class="media-icon"><Location /></el-icon>
        <input type="file" ref="fileInput" accept="image/*" style="display:none" @change="handleFileChange" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Picture, Location } from '@element-plus/icons-vue'
import { toggleCompose } from '../store'
import api from '../api'
import { ElMessage } from 'element-plus'

const content = ref('')
const imageUrl = ref('')
const fileInput = ref(null)
const userStr = localStorage.getItem('user');
const currentUser = userStr ? JSON.parse(userStr) : { id: 1 }; 

const close = () => {
    toggleCompose()
}

const triggerUpload = () => {
    fileInput.value.click();
}

const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const formData = new FormData();
    formData.append('file', file);

    try {
        const res = await api.post('/upload', formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        imageUrl.value = 'http://localhost:8888' + res.data.url;
    } catch (e) {
        ElMessage.error('Image upload failed');
        console.error(e);
    }
}

const removeImage = () => {
    imageUrl.value = '';
    if(fileInput.value) fileInput.value.value = '';
}

const submit = async () => {
    if (!content.value.trim() && !imageUrl.value) return;
    try {
        await api.post('/content/publish', {
            authorId: currentUser.id,
            content: content.value,
            imageUrl: imageUrl.value
        });
        ElMessage.success('Sent!');
        content.value = '';
        imageUrl.value = '';
        close();
    } catch (e) {
        ElMessage.error('Failed to send');
        console.error(e);
    }
}
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.4);
    z-index: 9999;
    display: flex;
    justify-content: center;
    align-items: flex-start;
    padding-top: 50px;
}

.modal-content {
    background: white;
    width: 600px;
    border-radius: 16px;
    padding: 16px;
    display: flex;
    flex-direction: column;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
}

.close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    line-height: 1;
}

.submit-btn {
    background: #1d9bf0;
    color: white;
    border: none;
    padding: 8px 20px;
    border-radius: 20px;
    font-weight: bold;
    cursor: pointer;
    font-size: 15px;
}
.submit-btn:disabled { opacity: 0.5; cursor: default; }

.modal-body {
    display: flex;
    gap: 12px;
}

.input-area { flex: 1; }

textarea {
    width: 100%;
    border: none;
    outline: none;
    font-size: 20px;
    font-family: inherit;
    resize: none;
}

.modal-footer {
    border-top: 1px solid #eff3f4;
    padding-top: 12px;
    margin-top: 12px;
    display: flex;
    gap: 16px;
    padding-left: 52px; /* avatar width + gap */
}

.media-icon {
    font-size: 20px;
    color: #1d9bf0;
    cursor: pointer;
}
.image-preview {
    position: relative;
    margin-top: 10px;
}
.image-preview img {
    max-width: 100%;
    max-height: 300px;
    border-radius: 12px;
}
.remove-btn {
    position: absolute;
    top: 5px;
    left: 5px;
    background: rgba(0,0,0,0.5);
    color: white;
    border: none;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    cursor: pointer;
    font-size: 16px;
    line-height: 1;
}
</style>
