import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import api, { login as loginApi } from '@/api';

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '');
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));
  const userRole = ref(localStorage.getItem('userRole') || '');
  
  const isLoggedIn = computed(() => !!token.value);
  const isDoctor = computed(() => userRole.value === 'doctor');
  const isAdmin = computed(() => userRole.value === 'admin');
  
  // 登录
  async function login(credentials) {
    try {
      // 构建userLogin对象
      const userLogin = {
        username: credentials.username,
        password: credentials.password,
        captcha: {
          key: credentials.captchaKey,
          code: credentials.captcha
        },
        type: credentials.type === 'doctor' ? 2 : 3
      };
      
      const response = await loginApi(userLogin);
      
      // 检查返回的结果
      if (response.code === 200) {
        // 登录成功
        const newToken = response.data;
        
        // 更新状态
        token.value = newToken;
        userRole.value = credentials.type;
        
        // 保存到本地存储
        localStorage.setItem('token', newToken);
        localStorage.setItem('userRole', credentials.type);
        
        // 登录成功后立即获取用户信息
        await fetchUserInfo();
        
        return { success: true, role: credentials.type };
      } else {
        // 登录失败
        return { success: false, message: response.message || '登录失败' };
      }
    } catch (error) {
      console.error('登录失败:', error);
      return { success: false, message: error.response?.data?.message || error.message || '登录失败' };
    }
  }
  
  // 注销
  function logout() {
    // 清除状态
    token.value = '';
    userInfo.value = {};
    userRole.value = '';
    
    // 清除本地存储
    localStorage.removeItem('token');
    localStorage.removeItem('userInfo');
    localStorage.removeItem('userRole');
  }
  
  // 获取用户信息
  async function fetchUserInfo() {
    try {
      if (!token.value) {
        console.log('Token不存在，无法获取用户信息');
        return null;
      }
      
      console.log('开始获取用户信息，token:', token.value);
      
      // 调用后端API获取最新的用户信息
      const response = await api.get('/front/loginAndOut/getUserInfo');
      
      console.log('获取用户信息响应:', response);
      
      // 检查返回的结果
      if (response.code === 200 && response.data) {
        userInfo.value = response.data;
        localStorage.setItem('userInfo', JSON.stringify(response.data));
        console.log('用户信息已更新:', response.data);
        console.log('用户ID:', response.data.userId);
        console.log('用户名:', response.data.username);
        return response.data;
      } else {
        console.warn('获取用户信息失败，响应:', response);
        // 如果获取失败，尝试从本地存储恢复
        const localUserInfo = localStorage.getItem('userInfo');
        if (localUserInfo) {
          try {
            const parsedInfo = JSON.parse(localUserInfo);
            userInfo.value = parsedInfo;
            console.log('从本地存储恢复用户信息:', parsedInfo);
            return parsedInfo;
          } catch (e) {
            console.error('解析本地用户信息失败:', e);
          }
        }
      }
    } catch (error) {
      console.error('获取用户信息失败:', error);
      // 如果网络请求失败，尝试从本地存储恢复
      const localUserInfo = localStorage.getItem('userInfo');
      if (localUserInfo) {
        try {
          const parsedInfo = JSON.parse(localUserInfo);
          userInfo.value = parsedInfo;
          console.log('网络请求失败，从本地存储恢复用户信息:', parsedInfo);
          return parsedInfo;
        } catch (e) {
          console.error('解析本地用户信息失败:', e);
        }
      }
    }
    return null;
  }
  
  return {
    token,
    userInfo,
    userRole,
    isLoggedIn,
    isDoctor,
    isAdmin,
    login,
    logout,
    fetchUserInfo
  };
});