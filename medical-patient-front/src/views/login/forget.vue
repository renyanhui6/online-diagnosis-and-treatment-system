<template>
  <div class="forget-container">
    <div class="forget-box">
      <div class="forget-header">
        <img src="../../assets/logo.svg" alt="医院logo" class="logo" />
        <h2>找回密码</h2>
      </div>
      
      <el-form ref="forgetFormRef" :model="forgetForm" :rules="forgetRules" class="forget-form">
        <el-form-item prop="email">
          <el-input
            v-model="forgetForm.email"
            placeholder="请输入注册邮箱"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>
        <el-form-item prop="emailCode">
          <div class="email-code-container">
            <el-input
              v-model="forgetForm.emailCode"
              placeholder="请输入邮箱验证码"
              prefix-icon="ChatDotRound"
              clearable
            />
            <el-button 
              type="primary" 
              :disabled="emailCodeDisabled" 
              @click="sendEmailCode"
            >
              {{ emailCodeText }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="forgetForm.password"
            type="password"
            placeholder="请输入新密码"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" class="forget-button" @click="handleForget">
            重置密码
          </el-button>
        </el-form-item>
        
        <div class="forget-options">
          <el-link type="primary" @click="goToLogin">返回登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

// 验证邮箱格式
const validateEmail = (email) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(email)
}

// 生成验证码
const generateCaptcha = () => {
  const chars = 'ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678'
  let result = ''
  for (let i = 0; i < 4; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

const router = useRouter()

// 加载状态
const loading = ref(false)


// 忘记密码表单
const forgetFormRef = ref(null)
const forgetForm = reactive({
  email: '',
  emailCode: '',
  password: ''
})

// 邮箱验证码按钮状态
const emailCodeDisabled = ref(false)
const emailCodeText = ref('获取验证码')
let emailCodeTimer = null

const forgetRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { validator: (rule, value, callback) => {
      if (!validateEmail(value)) {
        callback(new Error('请输入正确的邮箱格式'))
      } else {
        callback()
      }
    }, trigger: 'blur' }
  ],

  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { min: 6, max: 6, message: '邮箱验证码长度不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度应为6-20个字符', trigger: 'blur' }
  ]
}


// 发送邮箱验证码
const sendEmailCode = async () => {
  if (!forgetForm.email) {
    ElMessage.error('请先输入邮箱')
    return
  }
  
  if (!validateEmail(forgetForm.email)) {
    ElMessage.error('请输入正确的邮箱格式')
    return
  }
  
  try {
    // 调用获取邮箱验证码接口
    const { getEmailCode } = await import('../../api/user')
    
    // 打印请求参数用于调试
    const requestData = {
      email: forgetForm.email
    }
    console.log('获取验证码请求参数:', requestData)
    
    const res = await getEmailCode(requestData)
    
    console.log('获取验证码响应:', res)
    
    if (res.code === 200) {
      ElMessage.success('验证码已发送到您的邮箱')
      
      // 开始倒计时
      emailCodeDisabled.value = true
      let countdown = 60
      emailCodeText.value = `${countdown}秒后重新获取`
      
      emailCodeTimer = setInterval(() => {
        countdown--
        if (countdown > 0) {
          emailCodeText.value = `${countdown}秒后重新获取`
        } else {
          clearInterval(emailCodeTimer)
          emailCodeDisabled.value = false
          emailCodeText.value = '获取验证码'
        }
      }, 1000)
    } else {
      console.error('获取验证码失败:', res)
      ElMessage.error(res.message || '获取验证码失败')
    }
  } catch (error) {
    console.error('获取验证码错误:', error)
    ElMessage.error('获取验证码失败，请稍后重试')
  }
}

// 处理忘记密码
const handleForget = () => {
  forgetFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    
    try {
      // 调用找回密码接口
      const { resetPassword } = await import('../../api/user')
      
      // 打印请求参数用于调试
      const requestData = {
        email: forgetForm.email,
        email_code: forgetForm.emailCode,  // 后端期望的字段名是 email_code
        newPassword: forgetForm.password
      }
      console.log('重置密码请求参数:', requestData)
      
      const res = await resetPassword(requestData)
      
      console.log('重置密码响应:', res)
      
      if (res.code === 200) {
        ElMessage.success('密码重置成功，请使用新密码登录')
        router.push('/login')
      } else {
        console.error('密码重置失败:', res)
        ElMessage.error(res.message || '密码重置失败')
      }
    } catch (error) {
      console.error('密码重置错误:', error)
      ElMessage.error('密码重置失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

// 跳转到登录页
const goToLogin = () => {
  router.push('/login')
}

// 组件销毁时清除定时器
onBeforeUnmount(() => {
  if (emailCodeTimer) {
    clearInterval(emailCodeTimer)
  }
})
</script>

<style scoped>
.forget-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #e0f7fa 0%, #80deea 100%);
  background-size: cover;
}

.forget-box {
  width: 450px;
  padding: 30px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.forget-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 16px;
}

.forget-header h2 {
  font-size: 24px;
  color: var(--primary-700);
  margin: 0;
}

.forget-form {
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

.email-code-container {
  display: flex;
  align-items: center;
}

.email-code-container .el-input {
  flex: 1;
}

.email-code-container .el-button {
  margin-left: 10px;
  width: 120px;
}

.forget-button {
  width: 100%;
  height: 45px;
  font-size: 16px;
  margin-top: 10px;
}

.forget-options {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.success-container {
  margin: 20px 0;
}
</style>
