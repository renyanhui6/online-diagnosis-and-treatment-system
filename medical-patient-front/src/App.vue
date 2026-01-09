<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { WebSocketService } from './utils/websocket'
import ConsultationNotification from './components/ConsultationNotification.vue'
import UserStorage from './utils/userStorage'

const router = useRouter()

// 全局WebSocket服务
let globalWsService = null

// 通知组件引用
const notificationRef = ref(null)

// 初始化患者端长连接
const initPatientWebSocket = () => {
  console.log('=== 开始初始化患者端WebSocket连接 ===')
  
  // 检查用户是否已登录
  if (!UserStorage.isLoggedIn()) {
    console.log('❌ 用户未登录，跳过患者端WebSocket初始化')
    console.log('当前token:', UserStorage.getToken())
    return
  }

  const userId = UserStorage.getUserId()
  console.log('当前用户信息:', UserStorage.getUserInfo())
  console.log('解析出的用户ID:', userId)
  
  if (!userId) {
    console.log('❌ 无法获取用户ID，跳过患者端WebSocket初始化')
    console.log('用户信息详情:', JSON.stringify(UserStorage.getUserInfo()))
    return
  }

  console.log('✅ 初始化患者端长连接，用户ID:', userId)
  
  // 创建患者端长连接
  globalWsService = new WebSocketService()
  
  // 建立患者端长连接
  globalWsService.connectAsPatient()
  
  // 监听问诊请求
  globalWsService.onConsultationRequest((data) => {
    console.log('App.vue收到医生问诊请求:', data)
    handleConsultationRequest(data)
  })
  
  // 监听其他通知
  globalWsService.onNotification((data) => {
    console.log('收到通知:', data)
    handleSystemNotification(data)
  })
  
  // 监听连接状态
  globalWsService.onConnection(() => {
    console.log('患者端长连接成功')
  })
  
  globalWsService.onError((error) => {
    console.error('患者端长连接错误:', error)
  })
  
  globalWsService.onClose((event) => {
    console.log('患者端长连接关闭:', event)
    // 自动重连逻辑已在WebSocket服务中实现
  })
}

// 处理问诊请求
const handleConsultationRequest = (data) => {
  console.log('收到问诊请求:', data)
  
  // 显示通知组件
  if (notificationRef.value) {
    notificationRef.value.showConsultationNotification({
      id: data.consultationId || data.roomId,
      doctorId: data.doctorId,
      doctorName: data.doctorName,
      doctorAvatar: data.doctorAvatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png',
      departmentName: data.departmentName,
      doctorTitle: data.doctorTitle,
      appointmentTime: data.appointmentTime,
      consultationType: data.consultationType || '图文问诊',
      message: data.message || '医生请求开始问诊，请确认是否同意。',
      roomId: data.roomId,
      registrationId: data.registrationId
    })
  }
  
  // 显示浏览器通知
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('医生问诊请求', {
      body: `${data.doctorName}医生请求开始问诊`,
      icon: '/favicon.ico'
    })
  }
}

// 处理预约状态更新
const handleAppointmentUpdate = (data) => {
  console.log('收到预约状态更新:', data)
  
  ElMessage.info(`预约状态已更新: ${data.statusText}`)
  
  // 如果当前在预约列表页面，可以刷新数据
  if (router.currentRoute.value.path === '/appointment/list') {
    // 触发页面刷新
    window.location.reload()
  }
}

// 处理系统通知
const handleSystemNotification = (data) => {
  console.log('收到系统通知:', data)
  
  ElMessage({
    message: data.message,
    type: data.type || 'info',
    duration: data.duration || 3000
  })
}

// 请求浏览器通知权限
const requestNotificationPermission = async () => {
  if ('Notification' in window && Notification.permission === 'default') {
    const permission = await Notification.requestPermission()
    console.log('通知权限状态:', permission)
  }
}

// 组件挂载时
onMounted(() => {
  // 请求通知权限
  requestNotificationPermission()
  
  // 延迟初始化患者端长连接，确保用户信息已加载
  setTimeout(() => {
    initPatientWebSocket()
  }, 1000)
})

// 组件卸载时
onUnmounted(() => {
  // 关闭患者端长连接
  if (globalWsService) {
    globalWsService.closePatientConnection()
  }
})
</script>

<template>
  <div class="tech-background">
    <div class="particles-overlay"></div>
    <div class="wave-animation"></div>
    <router-view />
    
    <!-- 全局问诊通知组件 -->
    <ConsultationNotification ref="notificationRef" />
  </div>
</template>

<style>
html, body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: var(--neutral-800);
  background-color: var(--primary-50);
  margin: 0;
  padding: 0;
  min-height: 100vh;
  overflow-x: hidden;
}

#app {
  min-height: 100vh;
  width: 100%;
}

/* 科技背景 */
.tech-background {
  position: relative;
  min-height: 100vh;
  width: 100%;
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--primary-100) 50%, var(--primary-200) 100%);
  overflow: hidden;
}

/* 粒子效果覆盖层 */
.particles-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('./assets/particles-bg.svg');
  background-size: cover;
  background-position: center;
  opacity: 0.5;
  pointer-events: none;
  z-index: 0;
}

/* 波浪动画 */
.wave-animation {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  height: 240px;
  background-image: url('./assets/wave-animation.svg');
  background-size: cover;
  background-position: center bottom;
  pointer-events: none;
  z-index: 0;
}

/* 页面过渡动画 */
.page-enter-active, 
.page-leave-active {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

/* 按钮样式增强 */
.el-button {
  border-radius: var(--radius-lg);
  font-weight: 500;
  transition: var(--transition);
  box-shadow: var(--shadow);
  border: none;
  position: relative;
  overflow: hidden;
}

.el-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: all 0.6s;
}

.el-button:hover::before {
  left: 100%;
}

/* Plain / link buttons (restore Element Plus semantics) */
.el-button.is-plain {
  background: rgba(255, 255, 255, 0.65);
  border: 1px solid rgb(var(--primary-300-rgb) / 0.55);
  box-shadow: var(--shadow-sm);
  color: var(--neutral-800);
}

.el-button--primary.is-plain {
  border-color: rgb(var(--primary-400-rgb) / 0.6);
  color: var(--primary-700);
}

.el-button--info.is-plain {
  border-color: rgb(var(--primary-300-rgb) / 0.45);
  color: var(--neutral-700);
}

.el-button.is-plain:hover {
  background: rgba(255, 255, 255, 0.85);
  border-color: rgb(var(--primary-400-rgb) / 0.7);
  box-shadow: var(--shadow);
  transform: translateY(-1px);
}

.el-button.is-link,
.el-button.is-text {
  background: transparent;
  box-shadow: none;
  border: none;
}

.el-button.is-link::before,
.el-button.is-text::before {
  display: none;
}

.el-button.is-link:hover,
.el-button.is-text:hover {
  transform: none;
  box-shadow: none;
}

.el-button--primary {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: white;
  font-weight: 600;
}

.el-button--primary:hover {
  background: linear-gradient(135deg, var(--primary-600) 0%, var(--primary-700) 100%);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.el-button--success {
  background: linear-gradient(135deg, var(--success) 0%, #0d9488 100%);
  color: white;
}

.el-button--success:hover {
  background: linear-gradient(135deg, #0d9488 0%, #0f766e 100%);
  box-shadow: var(--shadow-md);
}

.el-button--warning {
  background: linear-gradient(135deg, var(--warning) 0%, #d97706 100%);
  color: white;
}

.el-button--warning:hover {
  background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  box-shadow: var(--shadow-md);
}

.el-button--danger {
  background: linear-gradient(135deg, var(--error) 0%, #dc2626 100%);
  color: white;
}

.el-button--danger:hover {
  background: linear-gradient(135deg, #dc2626 0%, #b91c1c 100%);
  box-shadow: var(--shadow-md);
}

.el-button--info {
  background: linear-gradient(135deg, var(--info) 0%, #2563eb 100%);
  color: white;
}

.el-button--info:hover {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  box-shadow: var(--shadow-md);
}

/* 卡片样式增强 */
.el-card {
  border-radius: var(--radius-xl);
  overflow: hidden;
  transition: var(--transition);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.3);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(10px);
  background-color: rgba(255, 255, 255, 0.7);
}

.el-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

.el-card__header {
  padding: 15px 20px;
  border-bottom: 1px solid rgb(var(--primary-200-rgb) / 0.3);
  font-weight: 600;
  color: var(--primary-700);
  background-color: rgb(var(--primary-100-rgb) / 0.35);
}

/* 输入框样式 */
.el-input__inner {
  border-radius: var(--radius-lg);
  border: 1px solid var(--primary-200);
  transition: var(--transition);
}

.el-input__inner:focus {
  border-color: var(--primary-500);
  box-shadow: 0 0 0 2px rgb(var(--primary-500-rgb) / 0.2);
}

/* 表格样式 */
.el-table {
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow);
  background-color: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(10px);
}

.el-table th {
  background-color: var(--primary-100);
  color: var(--primary-800);
  font-weight: 600;
}

.el-table tr:hover > td {
  background-color: var(--primary-50);
}

/* 磨砂玻璃效果 */
.glass-effect {
  background-color: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-md);
  border-radius: var(--radius-xl);
}
</style>
