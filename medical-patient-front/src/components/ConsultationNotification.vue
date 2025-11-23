<template>
  <div class="consultation-notification" v-if="showNotification">
    <el-card class="notification-card">
      <template #header>
        <div class="notification-header">
          <el-icon class="notification-icon"><Bell /></el-icon>
          <span class="notification-title">医生问诊请求</span>
        </div>
      </template>
      
      <div class="notification-content">
        <div class="doctor-info">
          <el-avatar :size="50" :src="notification.doctorAvatar" />
          <div class="doctor-details">
            <h3>{{ notification.doctorName }}</h3>
            <p>{{ notification.departmentName }} · {{ notification.doctorTitle }}</p>
          </div>
        </div>
        
        <div class="consultation-info">
          <p><strong>预约时间：</strong>{{ formatDateTime(notification.appointmentTime) }}</p>
          <p><strong>问诊类型：</strong>{{ notification.consultationType }}</p>
          <p class="consultation-message">{{ notification.message }}</p>
        </div>
        
        <div class="notification-actions">
          <el-button 
            type="primary" 
            size="large" 
            @click="acceptConsultation"
            :loading="responding"
          >
            同意开始问诊
          </el-button>
          <el-button 
            type="danger" 
            size="large" 
            @click="rejectConsultation"
            :loading="responding"
          >
            拒绝
          </el-button>
        </div>
        
        <div class="countdown" v-if="showCountdown">
          <p>自动拒绝倒计时：{{ countdown }}秒</p>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Bell } from '@element-plus/icons-vue'
import WebSocketService from '../utils/websocket'

const router = useRouter()

// 状态数据
const showNotification = ref(false)
const responding = ref(false)
const showCountdown = ref(false)
const countdown = ref(180) // 3分钟倒计时

// 通知数据
const notification = ref({
  id: '',
  doctorId: '',
  doctorName: '',
  doctorAvatar: '',
  departmentName: '',
  doctorTitle: '',
  appointmentTime: '',
  consultationType: '图文问诊',
  message: '医生请求开始问诊，请确认是否同意。',
  roomId: '',
  registrationId: ''
})

// 定时器
let countdownTimer = null

// 显示通知
function showConsultationNotification(data) {
  notification.value = { ...data }
  showNotification.value = true
  showCountdown.value = true
  countdown.value = 180
  
  // 开始倒计时
  startCountdown()
}

// 开始倒计时
function startCountdown() {
  countdownTimer = setInterval(() => {
    countdown.value--
    
    if (countdown.value <= 0) {
      // 自动拒绝
      rejectConsultation()
    }
  }, 1000)
}

// 停止倒计时
function stopCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

// 同意问诊
async function acceptConsultation() {
  responding.value = true
  try {
    // 调用后端API响应问诊请求
    const { respondToConsultation } = await import('../api/chat')
    const response = await respondToConsultation({
      registrationId: notification.value.registrationId || notification.value.id,
      response: 'accept'
    })
    
    if (response.code === 200) {
      ElMessage.success('已同意开始问诊')
      hideNotification()
      
      // 跳转到聊天页面
      router.push(`/appointment/chat/${notification.value.registrationId}`)
      
      // 立即发送状态更新消息给医生端
      if (WebSocketService && WebSocketService.getConnectedStatus()) {
        try {
          // 先获取房间信息，确保使用正确的房间ID
          const { getRoomStatus } = await import('../api/chat')
          const roomResponse = await getRoomStatus(notification.value.registrationId)
          
          let chatRoomId = notification.value.registrationId // 默认使用预约ID
          
          if (roomResponse.code === 200 && roomResponse.data) {
            // 优先使用后端返回的房间ID
            if (roomResponse.data.roomId) {
              chatRoomId = roomResponse.data.roomId
            } else if (roomResponse.data.id) {
              chatRoomId = roomResponse.data.id
            }
          }
          
          console.log('发送患者同意状态更新到房间:', chatRoomId)
          
          // 发送状态更新消息
          WebSocketService.sendStatusUpdate(chatRoomId, 2) // 2-问诊中
          
          // 发送系统消息
          WebSocketService.sendChatMessage(
            chatRoomId,
            notification.value.patientId || 3, // 患者ID
            1, // 文本消息
            '患者已同意开始问诊，可以开始聊天了。'
          )
        } catch (error) {
          console.error('获取房间信息失败:', error)
          // 如果获取房间信息失败，使用默认的预约ID
          const chatRoomId = notification.value.registrationId
          console.log('使用默认房间ID发送消息:', chatRoomId)
          
          // 发送状态更新消息
          WebSocketService.sendStatusUpdate(chatRoomId, 2) // 2-问诊中
          
          // 发送系统消息
          WebSocketService.sendChatMessage(
            chatRoomId,
            notification.value.patientId || 3, // 患者ID
            1, // 文本消息
            '患者已同意开始问诊，可以开始聊天了。'
          )
        }
      }
      
      // 延迟跳转，确保消息发送完成
      setTimeout(() => {
        router.push(`/appointment/chat/${notification.value.registrationId}`)
      }, 500)
    } else {
      ElMessage.error(response.message || '响应失败')
    }
  } catch (error) {
    console.error('响应问诊请求失败:', error)
    ElMessage.error('响应失败，请重试')
  } finally {
    responding.value = false
  }
}

// 拒绝问诊
async function rejectConsultation() {
  responding.value = true
  try {
    // 调用后端API响应问诊请求
    const { respondToConsultation } = await import('../api/chat')
    const response = await respondToConsultation({
      registrationId: notification.value.registrationId || notification.value.id,
      response: 'reject'
    })
    
    if (response.code === 200) {
      ElMessage.info('已拒绝问诊请求')
      hideNotification()
    } else {
      ElMessage.error(response.message || '响应失败')
    }
  } catch (error) {
    console.error('响应问诊请求失败:', error)
    ElMessage.error('响应失败，请重试')
  } finally {
    responding.value = false
  }
}

// 隐藏通知
function hideNotification() {
  showNotification.value = false
  showCountdown.value = false
  stopCountdown()
}

// 格式化日期时间
function formatDateTime(dateTime) {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 组件卸载时清理定时器
onUnmounted(() => {
  stopCountdown()
})

// 暴露方法给父组件
defineExpose({
  showConsultationNotification,
  hideNotification
})
</script>

<style scoped>
.consultation-notification {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  width: 400px;
  max-width: 90vw;
}

.notification-card {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  border-radius: 8px;
}

.notification-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-icon {
  color: #409eff;
  font-size: 18px;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.notification-content {
  padding: 10px 0;
}

.doctor-info {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background-color: #f8f9fa;
  border-radius: 6px;
}

.doctor-details h3 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.doctor-details p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.consultation-info {
  margin-bottom: 20px;
}

.consultation-info p {
  margin: 8px 0;
  line-height: 1.5;
}

.consultation-message {
  color: #666;
  font-style: italic;
  margin-top: 12px;
  padding: 8px;
  background-color: #f0f9ff;
  border-radius: 4px;
  border-left: 3px solid #409eff;
}

.notification-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.notification-actions .el-button {
  flex: 1;
}

.countdown {
  text-align: center;
  padding: 8px;
  background-color: #fff2e8;
  border-radius: 4px;
  border: 1px solid #ffd591;
}

.countdown p {
  margin: 0;
  color: #d46b08;
  font-size: 14px;
  font-weight: 500;
}

/* 动画效果 */
.consultation-notification {
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style> 