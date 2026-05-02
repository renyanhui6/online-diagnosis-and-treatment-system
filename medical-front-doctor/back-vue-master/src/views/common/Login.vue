<template>
  <div class="login-container">
    <div class="login-background">
      <div class="circles">
        <div v-for="i in 10" :key="i" class="circle" :style="getRandomStyle()"></div>
      </div>
    </div>
    
    <div class="login-content">
      <div class="login-header">
        <img src="@/assets/logo.svg" alt="Logo" class="logo" />
        <h1 class="title">医院管理系统</h1>
      </div>
      
      <div class="login-form-container">
        <h2 class="welcome">欢迎登录</h2>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="用户名"
              prefix-icon="User"
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              prefix-icon="Lock"
              show-password
              clearable
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item prop="captcha">
            <div class="captcha-container">
              <el-input
                v-model="loginForm.captcha"
                placeholder="验证码"
                prefix-icon="Picture"
                clearable
                maxlength="4"
                @keyup.enter="handleLogin"
                style="flex: 1; margin-right: 10px;"
              />
              <div class="captcha-image" @click="refreshCaptcha">
                <img v-if="captchaInfo.image" :src="captchaInfo.image" alt="验证码" />
                <span v-else>点击获取</span>
              </div>
            </div>
          </el-form-item>
          
          <el-form-item class="login-type">
            <el-radio-group v-model="loginForm.type">
              <el-radio label="doctor">医生登录</el-radio>
              <el-radio label="admin">管理员登录</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item class="login-options">
            <el-checkbox v-model="loginForm.remember">记住我</el-checkbox>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form-item>

          <el-form-item v-if="showDevQuickLogin">
            <el-button
              plain
              class="login-button"
              :loading="loading"
              @click="handleLocalLogin"
            >
              本地直连
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="login-tips">
          <p v-if="showDevQuickLogin">本地开发环境可直接使用“本地直连”进入当前端。</p>
          <p v-else>请使用数据库中的真实账号登录。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Picture } from '@element-plus/icons-vue';
import { useUserStore } from '@/stores/user';
import api, { getCaptcha } from '@/api';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const loginFormRef = ref(null);
const loading = ref(false);
const showDevQuickLogin = import.meta.env.DEV;

const loginForm = reactive({
  username: '',
  password: '',
  type: 'doctor',
  remember: false,
  captcha: '',
  captchaKey: ''
});

// 验证码信息
const captchaInfo = reactive({
  image: '',
  key: ''
});

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码长度为4位', trigger: 'blur' }
  ]
};

// 获取验证码
async function fetchCaptcha() {
  try {
    const response = await getCaptcha();
    if (response.code === 200 && response.data) {
      captchaInfo.key = response.data.key;
      captchaInfo.image = response.data.code;
      loginForm.captchaKey = response.data.key;
    } else {
      ElMessage.error('获取验证码失败');
    }
  } catch (error) {
    console.error('获取验证码失败:', error);
    ElMessage.error('获取验证码失败，请稍后重试');
  }
}

// 刷新验证码
function refreshCaptcha() {
  loginForm.captcha = '';
  fetchCaptcha();
}

// 组件挂载时获取验证码
onMounted(() => {
  syncLoginTypeFromRoute();
  fetchCaptcha();
});

function syncLoginTypeFromRoute() {
  if (route.path.startsWith('/admin')) {
    loginForm.type = 'admin';
    return;
  }
  if (route.path.startsWith('/doctor')) {
    loginForm.type = 'doctor';
  }
}

function getRandomStyle() {
  const size = Math.floor(Math.random() * 100) + 50;
  const left = Math.floor(Math.random() * 100);
  const top = Math.floor(Math.random() * 100);
  const opacity = Math.random() * 0.6 + 0.1;
  const animationDuration = Math.floor(Math.random() * 20) + 10;
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    opacity: opacity,
    animationDuration: `${animationDuration}s`
  };
}

async function handleLogin() {
  if (!loginFormRef.value) return;
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return;
    
    try {
      loading.value = true;
      
      const { success, role, message } = await userStore.login({
        username: loginForm.username,
        password: loginForm.password,
        type: loginForm.type, // 传递用户选择的登录类型
        captcha: loginForm.captcha,
        captchaKey: loginForm.captchaKey
      });
      
      if (success) {
        ElMessage.success('登录成功');
        
        // 检查是否有重定向参数
        const redirect = router.currentRoute.value.query.redirect;
        
        if (redirect) {
          // 如果有重定向参数，跳转到指定页面
          try {
            await router.push(decodeURIComponent(redirect));
          } catch (error) {
            console.error('重定向失败:', error);
            // 重定向失败时，根据用户类型跳转到默认页面
            if (loginForm.type === 'doctor') {
              router.push('/doctor');
            } else if (loginForm.type === 'admin') {
              router.push('/admin');
            }
          }
        } else {
          // 没有重定向参数时，根据用户选择的登录类型进行跳转
          if (loginForm.type === 'doctor') {
            router.push('/doctor');
          } else if (loginForm.type === 'admin') {
            router.push('/admin');
          }
        }
      } else {
        ElMessage.error(message || '登录失败');
        // 登录失败时刷新验证码
        refreshCaptcha();
      }
    } catch (error) {
      console.error('登录错误:', error);
      ElMessage.error('登录失败，请稍后重试');
      // 登录出错时也刷新验证码
      refreshCaptcha();
    } finally {
      loading.value = false;
    }
  });
}

function getRedirectTarget(role = loginForm.type) {
  const redirect = router.currentRoute.value.query.redirect;
  if (redirect) {
    return decodeURIComponent(redirect);
  }
  return role === 'admin' ? '/admin' : '/doctor';
}

async function finalizeLoginSession(token, role, successMessage) {
  userStore.token = token;
  userStore.userRole = role;
  localStorage.setItem('token', token);
  localStorage.setItem('userRole', role);

  await userStore.fetchUserInfo();
  ElMessage.success(successMessage);

  try {
    await router.push(getRedirectTarget(role));
  } catch (error) {
    console.error('跳转失败:', error);
    await router.push(role === 'admin' ? '/admin' : '/doctor');
  }
}

async function handleLocalLogin() {
  if (!showDevQuickLogin || loading.value) return;

  loading.value = true;
  try {
    const loginType = loginForm.type === 'admin' ? 3 : 2;
    const response = await api.get('/front/loginAndOut/devToken', {
      params: { type: loginType }
    });
    if (response.code !== 200 || !response.data) {
      ElMessage.error(response.message || '获取本地登录令牌失败');
      return;
    }
    const successMessage = loginForm.type === 'admin'
      ? '已使用本地管理员账号登录'
      : '已使用本地医生账号登录';
    await finalizeLoginSession(response.data, loginForm.type, successMessage);
  } catch (error) {
    console.error('本地直连失败:', error);
    ElMessage.error(error.response?.data?.message || error.message || '本地直连失败');
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  position: relative;
  overflow: hidden;
  background-color: transparent;
}

.login-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
}

.circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(45deg, var(--brand-600), var(--el-color-success));
  animation: float linear infinite;
  opacity: 0.2;
}

@keyframes float {
  0% {
    transform: translateY(0) rotate(0deg);
  }
  100% {
    transform: translateY(-100vh) rotate(360deg);
  }
}

.login-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  z-index: 1;
  width: 100%;
  max-width: 450px;
}

.login-header {
  display: flex;
  align-items: center;
  margin-bottom: 40px;
}

.logo {
  width: 48px;
  height: 48px;
  margin-right: 16px;
  animation: rotate 10s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.title {
  font-size: 28px;
  font-weight: 600;
  margin: 0;
  background: linear-gradient(45deg, var(--brand-600), var(--el-color-success));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.login-form-container {
  width: 100%;
  padding: 40px;
  background: var(--app-surface);
  border-radius: var(--app-radius);
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow);
  backdrop-filter: blur(10px);
  transition: all 0.3s;
}

.login-form-container:hover {
  box-shadow: var(--app-shadow);
  transform: translateY(-5px);
}

.welcome {
  font-size: 24px;
  font-weight: 500;
  color: var(--app-text);
  margin-top: 0;
  margin-bottom: 30px;
  text-align: center;
}

.login-form {
  width: 100%;
}

.login-type {
  margin-bottom: 20px;
}

.login-options {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 4px;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
}

.login-tips {
  margin-top: 20px;
  color: #999;
  font-size: 13px;
  text-align: center;
}

.login-tips p {
  margin: 5px 0;
}

/* 响应式设计 */
@media (max-width: 576px) {
  .login-form-container {
    padding: 30px 20px;
  }
  
  .login-content {
    max-width: 90%;
  }
}

/* 验证码容器样式 */
.captcha-container {
  display: flex;
  align-items: center;
}

.captcha-image {
  width: 100px;
  height: 40px;
  border: 1px solid var(--app-border);
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background-color: rgba(15, 23, 42, 0.03);
  transition: all 0.3s;
}

.captcha-image:hover {
  border-color: rgba(37, 99, 235, 0.55);
  background-color: rgba(37, 99, 235, 0.06);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.captcha-image span {
  font-size: 12px;
  color: #909399;
}
</style>
