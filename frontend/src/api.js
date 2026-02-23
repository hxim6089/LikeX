import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8888/api',
    timeout: 5000
    // headers will be set dynamically
});

api.interceptors.request.use(config => {
    // Determine content type
    if (config.data instanceof FormData) {
        config.headers['Content-Type'] = 'multipart/form-data';
    } else {
        config.headers['Content-Type'] = 'application/json';
    }
    return config;
});

export default api;
