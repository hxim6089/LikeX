<template>
  <div class="auth-container">
    <div class="auth-box">
        <h1 class="logo">𝕏</h1>
        <h2>Join X today.</h2>
        
        <input v-model="username" placeholder="Username" class="auth-input" />
        <input v-model="password" type="password" placeholder="Password" class="auth-input" />
        
        <button class="auth-btn" @click="handleRegister" :disabled="loading">
            {{ loading ? 'Creating account...' : 'Sign Up' }}
        </button>

        <p class="switch-auth">
            Already have an account? <router-link to="/login">Sign in</router-link>
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

const handleRegister = async () => {
    if (!username.value || !password.value) return;
    loading.value = true;
    try {
        const res = await api.post('/auth/register', {
            username: username.value,
            password: password.value
        });
        localStorage.setItem('user', JSON.stringify(res.data));
        ElMessage.success('Account created!');
        router.push('/');
    } catch (e) {
        ElMessage.error('Username may already exist');
    } finally {
        loading.value = false;
    }
}
</script>

<style scoped>
/* Same styles as Login */
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
