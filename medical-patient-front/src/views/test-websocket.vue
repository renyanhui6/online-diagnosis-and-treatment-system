<template>
  <div class="test-websocket">
    <h2>WebSocket连接测试</h2>
    
    <div class="status-section">
      <h3>连接状态</h3>
      <p>患者端连接状态: <span :class="patientStatusClass">{{ patientConnectionStatus }}</span></p>
      <p>用户登录状态: <span :class="loginStatusClass">{{ loginStatus }}</span></p>
      <p>用户ID: {{ userId }}</p>
      <p>Token: {{ tokenPreview }}</p>
    </div>
    
    <div class="user-info-section">
      <h3>用户信息</h3>
      <pre>{{ userInfoJson }}</pre>
    </div>
    
    <div class="actions-section">
      <el-button @click="checkConnection">检查连接</el-button>
      <el-button @click="connectPatient">连接患者端</el-button>
      <el-button @click="disconnectPatient">断开连接</el-button>
      <el-button @click="clearLogs">清空日志</el-button>
    </div>
    
    <div class="logs-section">
      <h3>连接日志</h3>
      <div class="logs-container">
        <div v-for="(log, index) in logs" :key="index" class="log-item" :class="log.type">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-message">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import WebSocketService from '../utils/websocket'
import UserStorage from '../utils/userStorage'

const logs = ref([])
const patientConnectionStatus = ref('未知')
const loginStatus = ref('未知')
const userId = ref('')
const tokenPreview = ref('')
const userInfoJson = ref('')

const patientStatusClass = computed(() => {
  switch (patientConnectionStatus.value) {
    case '已连接': return 'status-connected'
    case '连接中': return 'status-connecting'
    case '连接失败': return 'status-error'
    default: return 'status-unknown'
  }
})

const loginStatusClass = computed(() => {
  switch (loginStatus.value) {
    case '已登录': return 'status-connected'
    case '未登录': return 'status-error'
    default: return 'status-unknown'
  }
})

const addLog = (message, type = 'info') => {
  const time = new Date().toLocaleTimeString()
  logs.value.unshift({ time, message, type })
  if (logs.value.length > 50) {
    logs.value = logs.value.slice(0, 50)
  }
}

const checkConnection = () => {
  addLog('=== 开始检查连接状态 ===', 'info')
  
  // 检查登录状态
  const isLoggedIn = UserStorage.isLoggedIn()
  loginStatus.value = isLoggedIn ? '已登录' : '未登录'
  addLog(`登录状态: ${loginStatus.value}`, isLoggedIn ? 'success' : 'error')
  
  // 检查用户信息
  const userInfo = UserStorage.getUserInfo()
  const currentUserId = UserStorage.getUserId()
  userId.value = currentUserId
  userInfoJson.value = JSON.stringify(userInfo, null, 2)
  
  addLog(`用户ID: ${currentUserId}`, currentUserId ? 'success' : 'error')
  addLog(`用户信息: ${JSON.stringify(userInfo)}`, 'info')
  
  // 检查token
  const token = UserStorage.getToken()
  tokenPreview.value = token ? `${token.substring(0, 20)}...` : '无'
  addLog(`Token: ${tokenPreview.value}`, token ? 'success' : 'error')
  
  // 检查WebSocket连接状态
  const wsStatus = WebSocketService.getPatientConnectionStatus()
  patientConnectionStatus.value = wsStatus
  addLog(`WebSocket连接状态: ${wsStatus}`, 'info')
  
  addLog('=== 连接状态检查完成 ===', 'info')
}

const connectPatient = () => {
  addLog('=== 开始连接患者端WebSocket ===', 'info')
  
  if (!UserStorage.isLoggedIn()) {
    addLog('❌ 用户未登录，无法建立连接', 'error')
    return
  }
  
  const currentUserId = UserStorage.getUserId()
  if (!currentUserId) {
    addLog('❌ 无法获取用户ID，无法建立连接', 'error')
    return
  }
  
  addLog(`✅ 开始连接患者端，用户ID: ${currentUserId}`, 'success')
  
  // 添加WebSocket事件监听
  WebSocketService.onConnection(() => {
    addLog('✅ 患者端WebSocket连接成功', 'success')
    patientConnectionStatus.value = '已连接'
  })
  
  WebSocketService.onError((error) => {
    addLog(`❌ 患者端WebSocket连接错误: ${error}`, 'error')
    patientConnectionStatus.value = '连接失败'
  })
  
  WebSocketService.onClose((event) => {
    addLog(`⚠️ 患者端WebSocket连接关闭: ${event.code}`, 'warning')
    patientConnectionStatus.value = '已断开'
  })
  
  // 建立连接
  WebSocketService.connectAsPatient()
  patientConnectionStatus.value = '连接中'
}

const disconnectPatient = () => {
  addLog('=== 断开患者端WebSocket连接 ===', 'info')
  WebSocketService.closePatientConnection()
  patientConnectionStatus.value = '已断开'
  addLog('✅ 患者端WebSocket连接已断开', 'success')
}

const clearLogs = () => {
  logs.value = []
  addLog('日志已清空', 'info')
}

onMounted(() => {
  addLog('WebSocket测试页面已加载', 'info')
  checkConnection()
})
</script>

<style scoped>
.test-websocket {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.status-section, .user-info-section, .actions-section, .logs-section {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
  background: white;
}

.status-connected {
  color: #67c23a;
  font-weight: bold;
}

.status-connecting {
  color: #e6a23c;
  font-weight: bold;
}

.status-error {
  color: #f56c6c;
  font-weight: bold;
}

.status-unknown {
  color: #909399;
  font-weight: bold;
}

.actions-section {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.logs-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 10px;
  background: #f5f5f5;
}

.log-item {
  margin-bottom: 5px;
  font-family: monospace;
  font-size: 12px;
}

.log-time {
  color: #666;
  margin-right: 10px;
}

.log-message {
  color: #333;
}

.log-item.info .log-message {
  color: #409eff;
}

.log-item.success .log-message {
  color: #67c23a;
}

.log-item.error .log-message {
  color: #f56c6c;
}

.log-item.warning .log-message {
  color: #e6a23c;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  font-size: 12px;
}
</style> 