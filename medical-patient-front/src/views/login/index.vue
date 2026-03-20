<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <img src="../../assets/logo.svg" alt="医院logo" class="logo" />
        <h2>医院在线预约挂号系统</h2>
      </div>
      
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入账号"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item prop="captchaCode">
          <div class="captcha-container">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="请输入验证码"
              prefix-icon="Key"
              clearable
            />
            <div class="captcha-img" @click="refreshCaptcha">
              <img :src="captchaImg" alt="验证码" />
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" class="login-button" @click="handleLogin">
            登录
          </el-button>
        </el-form-item>

        <el-form-item v-if="showDevQuickLogin">
          <el-button plain class="login-button" :loading="loading" @click="handleLocalLogin">
            本地直连
          </el-button>
        </el-form-item>
        
        <div class="login-options">
          <el-link type="primary" @click="goToRegister">注册账号</el-link>
          <el-link type="primary" @click="goToForget">忘记密码</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import UserStorage from '../../utils/userStorage'
import { login, getUserInfo, getDevToken } from '../../api/user'
import { generateCaptcha } from '../../utils'

const router = useRouter()
const showDevQuickLogin = import.meta.env.DEV

// 登录表单
const loginFormRef = ref(null)
const loginForm = reactive({
  username: '',
  password: '',
  captchaKey: '',
  captchaCode: ''
})

// 验证规则
const loginRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 1, max: 20, message: '账号长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码长度不正确', trigger: 'blur' }
  ]
}

// 加载状态
const loading = ref(false)

// 验证码图片
const captchaImg = ref('')

// 刷新验证码
const refreshCaptcha = async () => {
  try {
    // 调用后端接口获取验证码
    const res = await generateCaptcha()
    if (res.code === 200 && res.data) {
      // 设置验证码图片URL和key
      captchaImg.value = res.data.code // code字段包含图片URL
      loginForm.captchaKey = res.data.key // key字段是验证码的标识
      loginForm.captchaCode = '' // 清空验证码输入
      console.log('验证码已更新，key:', loginForm.captchaKey)
    } else {
      ElMessage.error('获取验证码失败')
    }
  } catch (error) {
    console.error('获取验证码错误:', error)
    ElMessage.error('获取验证码失败，请刷新页面重试')
  }
}

const completeLogin = async (token, successMessage) => {
  console.log('登录成功，获取到token:', token)
  UserStorage.setToken(token)
  console.log('登录后localStorage中的token:', UserStorage.getToken())

  try {
    console.log('=== 开始获取用户信息 ===')
    console.log('当前token:', UserStorage.getToken())
    await new Promise(resolve => setTimeout(resolve, 100))

    const userInfoRes = await getUserInfo()
    console.log('获取用户信息响应:', userInfoRes)
    console.log('响应状态码:', userInfoRes.code)
    console.log('响应数据:', userInfoRes.data)

    if (userInfoRes.code === 200) {
      const userData = userInfoRes.data || {}
      console.log('准备保存的用户数据:', userData)
      console.log('用户数据中的ID:', userData.id)

      if (!userData.id) {
        console.error('警告：从后端获取的用户信息中没有ID字段！')
        console.log('完整的用户数据:', JSON.stringify(userData))
      }

      UserStorage.setUserInfo(userData)
      console.log('用户信息已保存到localStorage')
      console.log('验证：localStorage中的用户信息:', UserStorage.getUserInfo())
      console.log('验证：localStorage中的用户ID:', UserStorage.getUserId())
    } else {
      console.error('获取用户信息失败，状态码:', userInfoRes.code)
      console.error('错误信息:', userInfoRes.message)
    }
  } catch (userInfoError) {
    console.error('获取用户信息失败:', userInfoError)
    console.error('错误详情:', userInfoError.message)
  }

  console.log('=== 登录流程完成 ===')
  ElMessage.success(successMessage)

  const redirect = router.currentRoute.value.query.redirect
  if (redirect) {
    console.log('检测到重定向参数，跳转到:', redirect)
    await router.push(redirect)
    return
  }
  await router.push('/')
}

// 登录处理
const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    if (!loginForm.captchaKey) {
      ElMessage.error('请先获取验证码')
      refreshCaptcha()
      return
    }
    
    const loginData = {
      username: loginForm.username,
      password: loginForm.password,
      captcha: {
        key: loginForm.captchaKey,
        code: loginForm.captchaCode
      }
    }
    
    console.log('登录表单数据:', JSON.stringify(loginData))
    
    loading.value = true
    
    try {
      const res = await login(loginData)
      console.log('登录响应:', res)
      if (res.code === 200) {
        const token = typeof res.data === 'string' ? res.data : res.data?.token || ''
        await completeLogin(token, '登录成功')
      } else {
        ElMessage.error(res.message || '登录失败')
        refreshCaptcha()
      }
    } catch (error) {
      console.error('登录错误:', error)
      ElMessage.error('登录失败，请稍后重试')
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

const handleLocalLogin = async () => {
  if (!showDevQuickLogin || loading.value) return

  loading.value = true
  try {
    const res = await getDevToken({ type: 1 })
    if (res.code !== 200 || !res.data) {
      ElMessage.error(res.message || '获取本地登录令牌失败')
      return
    }
    await completeLogin(res.data, '已使用本地患者账号登录')
  } catch (error) {
    console.error('本地直连失败:', error)
    ElMessage.error(error.message || '本地直连失败')
  } finally {
    loading.value = false
  }
}

// 跳转到注册页
const goToRegister = () => {
  router.push('/register')
}

// 跳转到忘记密码页
const goToForget = () => {
  router.push('/forget')
}

// 页面加载时生成验证码
onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px 12px;
  background: transparent;
  position: relative;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(900px 420px at 20% 18%, rgb(var(--primary-200-rgb) / 0.24), transparent 60%),
    radial-gradient(820px 420px at 80% 82%, rgb(var(--primary-300-rgb) / 0.18), transparent 55%);
  pointer-events: none;
}

.login-box {
  width: 400px;
  padding: 30px;
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(14px);
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-xl);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.35);
  position: relative;
  z-index: 1;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 30px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.login-header h2 {
  font-size: 24px;
  color: var(--neutral-800);
  font-weight: 700;
  margin: 0 0 20px 0;
}

.login-form {
  margin-top: 20px;
}

.captcha-container {
  display: flex;
  align-items: center;
}

.captcha-img {
  margin-left: 10px;
  cursor: pointer;
  height: 40px;
  width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgb(var(--primary-50-rgb) / 0.7);
  border-radius: var(--radius-lg);
  overflow: hidden;
  border: 1px solid rgb(var(--primary-200-rgb) / 0.45);
  transition: all 0.3s ease;
}

.captcha-img:hover {
  box-shadow: var(--shadow);
  border-color: rgb(var(--primary-300-rgb) / 0.6);
}

.captcha-img img {
  width: 100%;
  height: 100%;
}

.login-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
}

.login-options {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}
</style>
