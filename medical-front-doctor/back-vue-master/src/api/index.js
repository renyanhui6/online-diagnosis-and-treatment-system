import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: '/api', // 使用代理路径
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器 - 启用token验证
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token');
    if (token) {
      // 使用access-key作为请求头
      config.headers['access-key'] = token;
    }
    return config;
  },
  error => {
    return Promise.reject(error);
  }
);

// 响应拦截器 - 启用401错误处理和未登录检查
api.interceptors.response.use(
  response => {
    // 检查响应头中是否有新的token
    const newToken = response.headers['new-access-key'] || response.headers['new-access-token'];
    if (newToken) {
      // 更新本地存储的token
      localStorage.setItem('token', newToken);
      console.log('✅ 更新token:', newToken);
    }
    
    // 检查响应数据中的message是否为"未登录"或"未登陆"
    const responseData = response.data;
    if (responseData && responseData.message) {
      console.log('🔍 响应数据message:', responseData.message);
      if (responseData.message.includes('未登录') || responseData.message.includes('未登陆')) {
        console.log('🚨 检测到未登录，准备跳转到登录页面');
        console.log('📍 当前路径:', window.location.pathname);
        
        // 清除本地存储
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        localStorage.removeItem('userRole');
        localStorage.removeItem('username');
        console.log('🧹 已清除本地存储');
        
        // 使用更可靠的方式跳转到登录页面
        if (window.location.pathname !== '/login' && window.location.pathname !== '/doctor/login' && window.location.pathname !== '/admin/login') {
          console.log('🔄 准备跳转到登录页面...');
          
          // 根据当前路径判断跳转到哪个登录页面
          let loginPath = '/login';
          if (window.location.pathname.startsWith('/doctor')) {
            loginPath = '/doctor/login';
          } else if (window.location.pathname.startsWith('/admin')) {
            loginPath = '/admin/login';
          }
          
          // 保存当前路径用于登录后重定向
          const currentPath = window.location.pathname + window.location.search;
          const redirectQuery = currentPath !== '/' ? `?redirect=${encodeURIComponent(currentPath)}` : '';
          
          console.log('🎯 跳转目标:', loginPath + redirectQuery);
          window.location.replace(loginPath + redirectQuery);
        } else {
          console.log('ℹ️ 已经在登录页面，无需跳转');
        }
        
        return Promise.reject(new Error('未登录'));
      }
    }
    
    // 返回统一的数据格式 Result<T>
    return response.data;
  },
  error => {
    console.error('❌ 请求错误:', error);
    if (error.response) {
      // 处理401未授权错误
      if (error.response.status === 401) {
        console.log('🚨 检测到401未授权错误');
        localStorage.removeItem('token');
        localStorage.removeItem('userInfo');
        localStorage.removeItem('userRole');
        localStorage.removeItem('username');
        
        if (window.location.pathname !== '/login' && window.location.pathname !== '/doctor/login' && window.location.pathname !== '/admin/login') {
          // 根据当前路径判断跳转到哪个登录页面
          let loginPath = '/login';
          if (window.location.pathname.startsWith('/doctor')) {
            loginPath = '/doctor/login';
          } else if (window.location.pathname.startsWith('/admin')) {
            loginPath = '/admin/login';
          }
          
          // 保存当前路径用于登录后重定向
          const currentPath = window.location.pathname + window.location.search;
          const redirectQuery = currentPath !== '/' ? `?redirect=${encodeURIComponent(currentPath)}` : '';
          
          console.log('🎯 401错误跳转目标:', loginPath + redirectQuery);
          window.location.replace(loginPath + redirectQuery);
        }
      }
    }
    return Promise.reject(error);
  }
);

export default api;

// 获取验证码
export const getCaptcha = () => api.get('/front/loginAndOut/captchaCode');

// 登录
export const login = (userLogin) => api.post('/front/loginAndOut/login', userLogin);

// 获取医生个人信息
export const getDoctorInfo = () => api.get('/front/doctor/doctorDetail/getInfo');
