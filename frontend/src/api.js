import axios from 'axios';
import { useAuthStore } from './store/auth';
import { message } from 'ant-design-vue';

const api = axios.create({
  baseURL: '/api'
});

api.interceptors.request.use(config => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
}, error => {
  console.error('请求错误:', error);
  return Promise.reject(error);
});

api.interceptors.response.use(response => {
  // 直接返回响应，让调用方处理
  return response;
}, error => {
  console.error('响应错误:', error);
  if (error.response) {
    // 服务器返回了错误状态码
    const status = error.response.status;
    if (status === 401) {
      // 未授权，清除token并跳转到登录页
      const auth = useAuthStore();
      auth.clear();
      window.location.href = '/login';
    } else if (status === 403) {
      message.error('没有权限访问该资源');
    } else if (status >= 500) {
      message.error('服务器错误，请稍后重试');
    }
  } else if (error.request) {
    // 请求已发出但没有收到响应
    message.error('网络错误，请检查网络连接');
  } else {
    // 其他错误
    message.error('请求失败：' + error.message);
  }
  return Promise.reject(error);
});

export default api;

