<template>
  <div class="auth-container">
    <div class="auth-box">
        <h1 class="logo">𝕏</h1>
        <h2>Sign in to X</h2>
        
        <input v-model="username" placeholder="Username" class="auth-input" />
        <input v-model="password" type="password" placeholder="Password" class="auth-input" />
        
        <button class="auth-btn" @click="handleLogin" :disabled="loading">
            {{ loading ? 'Signing in...' : 'Sign In' }}
        </button>

        <p class="switch-auth">
            Don't have an account? <router-link to="/register">Sign up</router-link>
        </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()
const username = ref('')
const password = ref('')
const loading = ref(false)

const handleLogin = async () => {
    if (!username.value || !password.value) return;
    loading.value = true;
    try {
        const res = await api.post('/auth/login', {
            username: username.value,
            password: password.value
        });
        // 存储用户信息（格式不变）和 JWT Token（独立 key）
        localStorage.setItem('user', JSON.stringify(res.data.user));
        localStorage.setItem('token', res.data.token);
        ElMessage.success('Welcome back, ' + res.data.user.username);
        router.push('/');
    } catch (e) {
        ElMessage.error('Invalid credentials');
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
.auth-container {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    background-color: #fff;
}
.auth-box {
    width: 350px;
    padding: 20px;
}
.logo { font-size: 50px; margin: 0 0 40px; }
h2 { font-size: 30px; margin-bottom: 30px; }

.auth-input {
    width: 100%;
    padding: 15px;
    margin-bottom: 20px;
    border: 1px solid #cfd9de;
    border-radius: 4px;
    font-size: 16px;
    outline: none;
}
.auth-input:focus { border-color: #1d9bf0; }

.auth-btn {
    width: 100%;
    padding: 12px;
    background: #0f1419;
    color: white;
    border: none;
    border-radius: 30px;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
    margin-bottom: 30px;
}
.auth-btn:disabled { opacity: 0.5; }

.switch-auth { color: #536471; font-size: 14px; }
.switch-auth a { color: #1d9bf0; margin-left: 5px; }
</style>
