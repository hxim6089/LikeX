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

// 注意：不添加任何 401/403 响应拦截器
// 路由守卫 router.beforeEach 已负责未登录跳转

export default api;
