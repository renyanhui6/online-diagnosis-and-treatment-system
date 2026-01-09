<script setup>
// 应用主入口组件
import { onMounted } from 'vue'
import WebSocketService from './utils/websocket'

// 初始化医生端长连接
const initDoctorWebSocket = () => {
  console.log('=== 开始初始化医生端WebSocket连接 ===')
  
  // 检查是否已登录
  const token = localStorage.getItem('token')
  if (!token) {
    console.log('❌ 用户未登录，跳过医生端WebSocket初始化')
    return
  }
  
  // 检查用户信息
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  if (!userInfo.id && !userInfo.userId) {
    console.log('❌ 无法获取用户ID，跳过医生端WebSocket初始化')
    return
  }
  
  console.log('✅ 开始建立医生端长连接')
  
  // 建立医生端长连接
  WebSocketService.connectAsDoctor()
  
  // 监听患者问诊响应
  WebSocketService.onConsultationResponse((data) => {
    console.log('收到患者问诊响应:', data)
    // 这里可以添加全局的响应处理逻辑，比如显示通知
  })
}

onMounted(() => {
  // 延迟初始化，确保用户信息已加载
  setTimeout(() => {
    initDoctorWebSocket()
  }, 1000)
})
</script>

<template>
  <div class="app-container">
    <!-- 路由视图 -->
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style>
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: var(--el-font-family);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  font-size: 14px;
  color: var(--el-text-color-primary);
}

#app, .app-container {
  height: 100vh;
  width: 100%;
}

/* 路由过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 全局工具类 */
.flex-center {
  display: flex;
  justify-content: center;
  align-items: center;
}

.text-ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
