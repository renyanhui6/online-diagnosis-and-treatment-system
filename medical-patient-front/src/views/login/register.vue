<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <img src="../../assets/logo.svg" alt="医院logo" class="logo" />
        <h2>用户注册</h2>
      </div>
      
      <el-form ref="registerFormRef" :model="registerForm" :rules="registerRules" class="register-form">
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="请输入账号"
            prefix-icon="User"
            clearable
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>

        
        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="请输入邮箱"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>
       
        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-button" @click="handleRegister">
            注册
          </el-button>
        </el-form-item>
        
        <div class="register-options">
          <span>已有账号？</span>
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message, Key } from '@element-plus/icons-vue'
import { register } from '../../api/user'
import { generateCaptcha, validateEmail } from '../../utils'

const router = useRouter()

// 注册表单
const registerFormRef = ref(null)
const registerForm = reactive({
  username: '',
  password: '',
  email: '',
})
const registerRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度应为3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (!validateEmail(value)) {
        callback(new Error('请输入正确的邮箱格式'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ]
}

// 加载状态
const loading = ref(false)




// 注册处理
const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    
    try {
      // 调用注册接口
      const res = await register({
        username: registerForm.username,
        password: registerForm.password,
        email: registerForm.email,
        registerType: 1 // 后端期望的字段名是 registerType
      })
      
      if (res.code === 200) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(res.message || '注册失败')
      }
    } catch (error) {
      console.error('注册错误:', error)
      ElMessage.error('注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

// 跳转到登录页
const goToLogin = () => {
  router.push('/login')
}


</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px 12px;
  background: transparent;
  position: relative;
}

.register-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(900px 420px at 20% 18%, rgb(var(--primary-200-rgb) / 0.24), transparent 60%),
    radial-gradient(820px 420px at 80% 82%, rgb(var(--primary-300-rgb) / 0.18), transparent 55%);
  pointer-events: none;
}

.register-box {
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

.register-header {
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

.register-header h2 {
  font-size: 24px;
  color: var(--primary-700);
  margin: 0;
}

.register-form {
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
  background-color: var(--primary-100);
  border-radius: 4px;
  overflow: hidden;
}

.captcha-img img {
  width: 100%;
  height: 100%;
}

.register-button {
  width: 100%;
  height: 40px;
  font-size: 16px;
}

.register-options {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 16px;
}

.register-options span {
  margin-right: 8px;
  color: var(--neutral-600);
}
</style>
