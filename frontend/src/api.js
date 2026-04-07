import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8888/api',
    timeout: 5000
});

api.interceptors.request.use(config => {
    // Content-Type
    if (config.data instanceof FormData) {
        config.headers['Content-Type'] = 'multipart/form-data';
    } else {
        config.headers['Content-Type'] = 'application/json';
    }

    // JWT Token（有则加，无则不加）
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
});

// 全局错误提示（只弹 toast，不踢人）
api.interceptors.response.use(
    response => response,
    error => {
        const msg = error.response?.data?.message
            || error.response?.data?.error
            || error.message
            || '请求失败';
        // 动态导入 ElMessage 避免循环依赖
        import('element-plus').then(({ ElMessage }) => {
            ElMessage.error(msg);
        });
        return Promise.reject(error);
    }
);

export default api;
