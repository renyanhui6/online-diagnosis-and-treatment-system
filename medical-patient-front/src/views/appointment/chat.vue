<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button icon="ArrowLeft" circle plain @click="goBack" />
            <h2>{{ appointmentInfo.doctorName || '医生' }}问诊</h2>
          </div>
          <el-tag :type="getStatusType(appointmentInfo.status)">
            {{ getStatusText(appointmentInfo.status) }}
          </el-tag>
        </div>
      </template>
      
      <!-- 聊天区域 -->
      <div class="chat-content" ref="chatContentRef">
        <!-- 等待提示 -->
        <div v-if="roomInfo.room_status === 1" class="waiting-tip">
          <el-result icon="info" title="等待医生接诊" sub-title="您已在候诊队列中，请耐心等待医生呼叫">
            <template #extra>
              <div class="queue-info">
                <p>{{ roomInfo.message || '请耐心等待医生接诊' }}</p>
              </div>
            </template>
          </el-result>
        </div>
        
        <!-- 聊天消息 -->
        <div v-else class="message-list">
          <!-- 系统消息 -->
          <div class="system-message" v-if="roomInfo.room_status === 2">
            <p>{{ formatDate(new Date()) }} 问诊开始</p>
          </div>
          
          <!-- 消息列表 -->
          <div 
            v-for="(message, index) in messageList" 
            :key="index"
            :class="['message-item', message.sender === 'doctor' ? 'doctor-message' : 'patient-message']"
          >
            <div class="message-avatar">
              <el-avatar 
                :size="40" 
                :src="message.sender === 'doctor' ? doctorAvatar : patientAvatar"
              />
            </div>
            <div class="message-content">
              <div class="message-info">
                <span class="message-sender">{{ message.sender === 'doctor' ? appointmentInfo.doctorName : '我' }}</span>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
              <div class="message-body">
                <!-- 文本消息 -->
                <div v-if="message.type === 'text'" class="text-message">
                  {{ message.content }}
                </div>
                <!-- 图片消息 -->
                <div v-else-if="message.type === 'image'" class="image-message">
                  <el-image 
                    :src="message.content" 
                    :preview-src-list="[message.content]"
                    fit="cover"
                    :z-index="9999"
                  />
                </div>
              </div>
            </div>
          </div>
          
          <!-- 问诊结束提示 -->
          <div v-if="roomInfo.room_status === 3" class="system-message">
            <p>{{ formatDate(new Date()) }} 问诊结束</p>
          </div>
        </div>
      </div>
      
      <!-- 输入区域 -->
      <div class="chat-input" v-if="roomInfo.room_status === 2">
        <div class="input-toolbar">
          <el-upload
            action="#"
            :auto-upload="false"
            :show-file-list="false"
            :on-change="handleImageUpload"
            accept="image/*"
          >
            <el-button icon="Picture" circle plain />
          </el-upload>
        </div>
        <div class="input-box">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入消息..."
            resize="none"
            @keyup.enter.ctrl="sendMessage"
          />
        </div>
        <div class="input-actions">
          <el-button type="primary" @click="sendMessage" :disabled="!inputMessage.trim()">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture, ArrowLeft } from '@element-plus/icons-vue'
import { getChatMessages, uploadChatImage } from '../../api/chat'
import { getAppointmentStatusText, getAppointmentStatusType, formatDate as formatDateUtil } from '../../utils'
import { WebSocketService } from '../../utils/websocket'
import UserStorage from '../../utils/userStorage'

const route = useRoute()
const router = useRouter()

// 聊天内容区域引用，用于滚动到底部
const chatContentRef = ref(null)

// 预约信息
const appointmentInfo = reactive({
  id: '',
  departmentName: '',
  subDepartmentName: '',
  doctorName: '',
  doctorTitle: '',
  appointmentDate: '',
  timeSlot: '',
  patientName: '',
  status: 2, // 默认为排队中
  startTime: null,
  endTime: null,
  registrationId: '', // 挂号ID，用于关联聊天室
  patientId: '', // 患者ID
  doctorId: '' // 医生ID
})

// 聊天室信息
const roomInfo = reactive({
  id: '',
  room_status: 1, // 1-等待中, 2-进行中, 3-已结束
  message: ''
})

// 聊天消息列表
const messageList = ref([])

// 输入消息
const inputMessage = ref('')

// 医生和患者头像
const doctorAvatar = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')
const patientAvatar = ref('https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

// WebSocket服务
let wsService = null

// 获取预约状态文本
const getStatusText = (status) => {
  return getAppointmentStatusText(status)
}

// 获取预约状态类型（用于标签颜色）
const getStatusType = (status) => {
  return getAppointmentStatusType(status)
}

// 格式化日期
const formatDate = (date) => {
  return formatDateUtil(date, 'YYYY-MM-DD HH:mm:ss')
}

// 格式化时间
const formatTime = (date) => {
  const d = new Date(date)
  const hours = d.getHours().toString().padStart(2, '0')
  const minutes = d.getMinutes().toString().padStart(2, '0')
  return `${hours}:${minutes}`
}

// 返回上一页
const goBack = () => {
  router.push('/appointment/list')
}

// 发送消息
const sendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  try {
    const messageData = {
      roomId: roomInfo.id,
      senderId: appointmentInfo.patientId,
      messageType: 1, // 1-文本, 2-图片
      content: inputMessage.value.trim(),
      senderType: 1 // 1-患者, 2-医生
    }
    
    console.log('发送消息:', messageData)
    
    // 通过WebSocket发送 - 使用新的格式
    if (wsService && wsService.getConnectedStatus()) {
      wsService.sendChatMessage(
        roomInfo.id,
        appointmentInfo.patientId,
        1, // 文本消息
        inputMessage.value.trim(),
        1 // 1-患者
      )
    } else {
      console.error('WebSocket未连接，无法发送消息')
    }
    
    // 清空输入框
    inputMessage.value = ''
    
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
  }
}

// 处理图片上传
const handleImageUpload = async (file) => {
  if (!file) return
  
  try {
    const formData = new FormData()
    formData.append('file', file.raw) // 修改字段名为'file'以匹配后端期望
    formData.append('room_id', roomInfo.id)
    formData.append('sender_id', appointmentInfo.patientId)
    
    // 上传图片到七牛云
    const response = await uploadChatImage(formData)
    
    if (response.code === 200) {
      // 图片已经通过后端上传并保存到数据库，直接显示
      ElMessage.success('图片发送成功')
    } else {
      ElMessage.error(response.message || '图片上传失败')
    }
    
  } catch (error) {
    console.error('图片上传失败:', error)
    ElMessage.error('图片上传失败')
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (chatContentRef.value) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight
  }
}

// 初始化WebSocket连接
const initWebSocket = () => {
  if (!roomInfo.id) {
    console.log('聊天室ID不存在，无法初始化WebSocket')
    return
  }
  
  console.log('初始化聊天页面WebSocket连接，roomId:', roomInfo.id)
  
  wsService = new WebSocketService()
  
  // 连接WebSocket
  wsService.connect(roomInfo.id)
  
  // 如果患者刚同意问诊，发送状态更新消息
  if (roomInfo.room_status === 2) {
    console.log('患者已同意问诊，发送状态更新消息到房间:', roomInfo.id)
    setTimeout(() => {
      if (wsService && wsService.getConnectedStatus()) {
        wsService.sendStatusUpdate(roomInfo.id, 2) // 2-问诊中
      }
    }, 500)
  }
  
  // 监听消息
wsService.onMessage((data) => {
  console.log('患者端聊天页面收到WebSocket消息:', data)
  console.log('消息详细信息:', JSON.stringify(data, null, 2))
  console.log('消息类型:', data.type, '发送者类型:', data.sender_type || data.senderType || '未知')
  
  // 检查消息结构
  if (data.data) {
    console.log('消息内部data字段:', JSON.stringify(data.data, null, 2))
    console.log('内部发送者类型:', data.data.sender_type || data.data.senderType || '未知')
  }
  
  // 处理各种格式的聊天消息
  if (data.type === 'message' || data.type === 'chat' || 
      (data.type === undefined && data.content !== undefined)) {
    console.log('开始处理聊天消息，原始数据:', JSON.stringify(data, null, 2));
    
    // 添加新消息到列表
    let messageData = data.data || data;
    
    console.log('提取的消息数据:', JSON.stringify(messageData, null, 2));
    
    // 检查消息是否有效
    if (!messageData.content) {
      console.warn('收到无效消息，缺少content字段:', messageData)
      // 尝试从其他可能的位置获取内容
      if (data.content) {
        console.log('从顶层数据获取content:', data.content);
        messageData.content = data.content;
      } else {
        console.warn('无法找到消息内容，跳过此消息');
        return;
      }
    }
    
    // 如果messageData是数组，取第一个元素
    if (Array.isArray(messageData)) {
      console.log('消息数据是数组，取第一个元素');
      messageData = messageData[0];
    }
    
    // 获取发送者ID，优先从messageData中获取，如果没有则从原始data中获取
    const senderId = messageData.senderId || messageData.sender_id || data.senderId || data.sender_id;
    
    // 获取发送者类型，优先根据senderId判断
    let senderType;
    
    // 如果发送者ID是医生ID，则设置为医生消息
    if (senderId && appointmentInfo.doctorId && senderId === appointmentInfo.doctorId) {
      console.log('根据senderId判断为医生消息');
      senderType = 2; // 强制设置为医生
    } else {
      // 否则从消息中获取发送者类型
      senderType = messageData.sender_type || messageData.senderType || 
                  (data.sender_type || data.senderType || 1); // 默认为患者(1)
    }
    
    console.log('原始发送者类型信息:', {
      'senderId': senderId,
      'messageData.sender_type': messageData.sender_type,
      'messageData.senderType': messageData.senderType,
      'data.sender_type': data.sender_type,
      'data.senderType': data.senderType,
      'appointmentInfo.doctorId': appointmentInfo.doctorId,
      'appointmentInfo.patientId': appointmentInfo.patientId,
      '最终使用的senderType': senderType
    });
    
    // 构建标准消息对象
    const message = {
      id: messageData.id || Date.now(),
      content: messageData.content,
      type: (messageData.message_type || messageData.messageType || data.message_type || data.messageType || 1) === 1 ? 'text' : 'image',
      // 1-患者，2-医生
      sender: senderType === 2 ? 'doctor' : 'patient',
      timestamp: new Date(messageData.created_at || messageData.createTime || data.timestamp || new Date()),
      time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    
    console.log('消息发送者类型:', senderType, '解析后的发送者:', message.sender)
    
    console.log('患者端处理聊天消息，添加到列表:', message)
    messageList.value.push(message)
    
    // 滚动到底部
    nextTick(() => {
      scrollToBottom()
    })
  } else if (data.type === 'status') {
      // 更新聊天室状态
      roomInfo.room_status = data.roomStatus || data.data?.room_status
      roomInfo.message = data.message || data.data?.message
      
      console.log('收到状态更新消息，新状态:', roomInfo.room_status)
      
      // 如果状态变为问诊中，更新预约状态
      if (roomInfo.room_status === 2 && appointmentInfo.status === 2) {
        appointmentInfo.status = 3 // 问诊中
        appointmentInfo.startTime = new Date()
      }
      
      // 如果状态变为已结束，主动断开WebSocket连接
      if (roomInfo.room_status === 3) {
        console.log('问诊状态已结束，主动断开WebSocket连接')
        if (wsService && wsService.getConnectedStatus()) {
          wsService.close()
        }
      }
    } else if (data.type === 'consultation_start') {
      // 医生开始问诊
      appointmentInfo.status = 3 // 问诊中
      appointmentInfo.startTime = new Date()
      roomInfo.room_status = 2 // 问诊中
      
      // 添加系统消息
      messageList.value.push({
        type: 'text',
        content: `您好，我是${appointmentInfo.doctorName}医生，很高兴为您服务。请问您有什么不适？`,
        sender: 'doctor',
        timestamp: new Date()
      })
      
      // 滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
    } else if (data.type === 'consultation_end') {
      // 医生结束问诊
      appointmentInfo.status = 4 // 已完成
      appointmentInfo.endTime = new Date()
      roomInfo.room_status = 3 // 已结束
      
      // 添加系统消息
      messageList.value.push({
        type: 'text',
        content: '问诊已结束，感谢您的配合。',
        sender: 'system',
        timestamp: new Date()
      })
      
      // 滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
      
      // 主动断开WebSocket连接
      if (wsService && wsService.getConnectedStatus()) {
        console.log('问诊已结束，主动断开WebSocket连接')
        try {
          // 先发送离开消息
          wsService.sendStatusUpdate(roomInfo.id, 3) // 3-已结束
          
          // 等待一小段时间后关闭连接
          setTimeout(() => {
            wsService.close()
            console.log('WebSocket连接已关闭')
          }, 500)
        } catch (error) {
          console.error('关闭WebSocket连接失败:', error)
          // 强制关闭连接
          if (wsService.ws) {
            wsService.ws.close()
            wsService.ws = null
          }
        }
      }
      
      // 显示问诊结束提示
      ElMessage.success('问诊已结束，感谢您的配合')
    } else if (data.type === 'room_status_update') {
      // 房间状态更新消息
      console.log('收到房间状态更新:', data)
      const status = data.room_status || data.roomStatus
      if (status === 2) {
        roomInfo.room_status = 2 // 问诊中
        appointmentInfo.status = 3 // 问诊中
        appointmentInfo.startTime = new Date()
      } else if (status === 3) {
        roomInfo.room_status = 3 // 已结束
        appointmentInfo.status = 4 // 已完成
        appointmentInfo.endTime = new Date()
        
        // 添加系统消息
        messageList.value.push({
          type: 'text',
          content: '问诊已结束，感谢您的配合。',
          sender: 'system',
          timestamp: new Date()
        })
        
        // 滚动到底部
        nextTick(() => {
          scrollToBottom()
        })
        
        // 主动断开WebSocket连接
        if (wsService && wsService.getConnectedStatus()) {
          console.log('房间状态已结束，主动断开WebSocket连接')
          try {
            // 先发送离开消息
            wsService.sendStatusUpdate(roomInfo.id, 3) // 3-已结束
            
            // 等待一小段时间后关闭连接
            setTimeout(() => {
              wsService.close()
              console.log('WebSocket连接已关闭')
            }, 500)
          } catch (error) {
            console.error('关闭WebSocket连接失败:', error)
            // 强制关闭连接
            if (wsService.ws) {
              wsService.ws.close()
              wsService.ws = null
            }
          }
        }
        
        // 显示问诊结束提示
        ElMessage.success('问诊已结束，感谢您的配合')
      }
    } else if (data.type === 'disconnect') {
      // 收到断开连接消息
      console.log('收到断开连接消息:', data)
      
      // 更新房间状态
      roomInfo.room_status = 3 // 已结束
      appointmentInfo.status = 4 // 已完成
      appointmentInfo.endTime = new Date()
      
      // 添加系统消息
      messageList.value.push({
        type: 'text',
        content: '问诊已结束，连接已断开。',
        sender: 'system',
        timestamp: new Date()
      })
      
      // 滚动到底部
      nextTick(() => {
        scrollToBottom()
      })
      
      // 主动断开WebSocket连接
      if (wsService && wsService.getConnectedStatus()) {
        console.log('收到断开连接消息，主动断开WebSocket连接')
        try {
          // 先发送离开消息
          wsService.sendStatusUpdate(roomInfo.id, 3) // 3-已结束
          
          // 等待一小段时间后关闭连接
          setTimeout(() => {
            wsService.close()
            console.log('WebSocket连接已关闭')
          }, 500)
        } catch (error) {
          console.error('关闭WebSocket连接失败:', error)
          // 强制关闭连接
          if (wsService.ws) {
            wsService.ws.close()
            wsService.ws = null
          }
        }
      }
      
      // 显示问诊结束提示
      ElMessage.success('问诊已结束，连接已断开')
    }
  })
  
  // 监听连接状态
  wsService.onConnection(() => {
    console.log('聊天页面WebSocket连接成功')
    
    // 发送状态更新消息，表示患者已准备就绪
    if (wsService && wsService.getConnectedStatus()) {
      wsService.sendStatusUpdate(roomInfo.id, 1) // 1-等待患者确认
    }
  })
  
  wsService.onError((error) => {
    console.error('聊天页面WebSocket连接错误:', error)
    ElMessage.error('连接异常，请刷新页面重试')
  })
  
  wsService.onClose((event) => {
    console.log('聊天页面WebSocket连接关闭:', event)
  })
}

// 获取聊天历史记录
const loadChatHistory = async () => {
  try {
    console.log('=== 患者端开始获取聊天记录 ===');
    console.log('roomInfo.id:', roomInfo.id);
    
    if (!roomInfo.id) {
      console.warn('房间ID为空，无法获取聊天记录');
      return;
    }
    
    const { getChatMessages } = await import('../../api/chat')
    const response = await getChatMessages(roomInfo.id)
    
    console.log('患者端聊天记录API响应:', response);
      console.log('聊天记录数据详情:', JSON.stringify(response.data, null, 2));
    
    if (response.code === 200 && response.data && Array.isArray(response.data)) {
      // 映射消息数据
      messageList.value = response.data.map(msg => {
        // 检查消息内容是否有效
        if (!msg.content) {
          console.warn('历史消息没有内容，跳过:', msg);
          return null;
        }
        
        // 提取发送者ID
        const senderId = msg.sender_id || msg.senderId;
        
        // 确保正确解析发送者类型
        // 1-患者，2-医生
        let senderType = msg.sender_type || msg.senderType;
        
        // 如果发送者ID是医生ID，则设置为医生消息
        if (senderId && appointmentInfo.doctorId && senderId === appointmentInfo.doctorId) {
          console.log('历史消息：根据senderId判断为医生消息');
          senderType = 2; // 强制设置为医生
        }
        
        const sender = senderType === 2 ? 'doctor' : 'patient';
        
        console.log('历史消息发送者详情:', {
          'msg.id': msg.id,
          'msg.content': msg.content.substring(0, 20) + (msg.content.length > 20 ? '...' : ''),
          'msg.sender_type': msg.sender_type,
          'msg.senderType': msg.senderType,
          'msg.sender_id': msg.sender_id,
          'msg.senderId': msg.senderId,
          'appointmentInfo.doctorId': appointmentInfo.doctorId,
          '最终使用的senderType': senderType,
          '解析后的sender': sender
        });
        
        console.log('历史消息发送者类型:', senderType, '解析后的发送者:', sender, '消息内容:', msg.content);
        
        return {
          id: msg.id,
          content: msg.content,
          type: (msg.message_type || msg.messageType || 1) === 1 ? 'text' : 'image',
          sender: sender,
          timestamp: new Date(msg.create_time || msg.createTime),
          time: new Date(msg.create_time || msg.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
        };
      }).filter(msg => msg !== null); // 过滤掉无效消息
      
      console.log('✅ 患者端成功加载聊天记录，消息数量:', messageList.value.length);
      console.log('历史消息详情:', messageList.value);
      
      // 确保DOM更新后滚动到底部
      await nextTick();
      setTimeout(() => {
        scrollToBottom();
      }, 100);
    } else {
      console.warn('患者端获取聊天记录失败或无数据:', response);
    }
  } catch (error) {
    console.error('患者端获取聊天记录失败:', error)
    ElMessage.error('获取聊天记录失败，请刷新页面重试')
  }
}

// 获取预约详情
const fetchAppointmentDetail = async () => {
  const registrationId = route.params.id
  if (!registrationId) {
    ElMessage.error('预约ID不存在')
    router.push('/appointment/list')
    return
  }
  
  console.log('聊天页面预约ID:', registrationId, '类型:', typeof registrationId)
  
  try {
    // 使用新的API获取房间信息
    const { getRoomStatus } = await import('../../api/chat')
    const response = await getRoomStatus(registrationId)
    
    if (response.code === 200 && response.data) {
      const roomData = response.data
      Object.assign(roomInfo, roomData)
      
      // 使用后端返回的房间ID（room表的id字段）
      if (roomData.roomId) {
        roomInfo.id = roomData.roomId
        console.log('使用后端返回的房间ID:', roomData.roomId)
      } else if (roomData.id) {
        // 如果后端直接返回id字段
        roomInfo.id = roomData.id
        console.log('使用后端返回的房间ID:', roomData.id)
      } else {
        // 如果没有返回房间ID，使用registrationId作为临时房间ID
        roomInfo.id = parseInt(registrationId)
        console.log('使用预约ID作为临时房间ID:', roomInfo.id)
      }
      
      // 确保房间ID是数字类型
      roomInfo.id = parseInt(roomInfo.id)
      console.log('最终房间ID:', roomInfo.id)
      
      // 设置预约信息（从房间信息中获取）
      appointmentInfo.id = registrationId
      appointmentInfo.doctorName = roomData.doctorName || '医生'
      appointmentInfo.patientName = roomData.patientName || '患者'
      appointmentInfo.patientId = roomData.patientId || UserStorage.getUserId() // 设置患者ID
      appointmentInfo.doctorId = roomData.doctorId // 设置医生ID
      appointmentInfo.status = 3 // 问诊中
      
      // 如果患者刚同意问诊，直接设置为问诊中状态，不等待医生确认
      if (roomData.room_status === 1 || roomData.room_status === undefined) {
        roomInfo.room_status = 2 // 问诊中
        console.log('患者已同意问诊，直接设置为问诊中状态')
        
        // 发送状态更新消息给医生端
        setTimeout(() => {
          if (wsService && wsService.getConnectedStatus()) {
            console.log('发送患者同意问诊状态更新到房间:', roomInfo.id)
            wsService.sendStatusUpdate(roomInfo.id, 2) // 2-问诊中
            
            // 同时发送一条系统消息
            wsService.sendChatMessage(
              roomInfo.id,
              appointmentInfo.patientId || UserStorage.getUserId(),
              1, // 文本消息
              '患者已同意开始问诊，可以开始聊天了。'
            )
          }
        }, 1000)
      } else {
        roomInfo.room_status = roomData.room_status
      }
      
      // 获取历史聊天记录（使用房间ID）
      await loadChatHistory()
      
      // 初始化WebSocket
      initWebSocket()
    } else {
      ElMessage.warning('医生尚未发起问诊，请稍后再试')
      router.push('/appointment/list')
      return
    }
    
  } catch (error) {
    console.error('获取预约详情失败:', error)
    ElMessage.error('获取预约详情失败，请稍后重试')
    router.push('/appointment/list')
  }
}

// 组件挂载时
onMounted(() => {
  fetchAppointmentDetail();
  
  // 添加WebSocket连接状态监听
  if (wsService) {
    wsService.onConnection(() => {
      console.log('WebSocket连接成功事件触发');
    });
    
    wsService.onError((error) => {
      console.error('WebSocket连接错误:', error);
    });
    
    wsService.onClose((event) => {
      console.log('WebSocket连接关闭:', event);
    });
  } else {
    console.error('WebSocket服务未初始化');
  }
})

// 组件卸载前
onBeforeUnmount(() => {
  console.log('患者端聊天页面组件卸载，清理WebSocket连接')
  
  // 发送状态更新消息，表示患者离开
  if (wsService && wsService.getConnectedStatus()) {
    try {
      wsService.sendStatusUpdate(roomInfo.id, 3) // 3-已结束
    } catch (error) {
      console.error('发送离开状态失败:', error)
    }
  }
  
  // 清理WebSocket监听器
  if (wsService) {
    wsService.clearAllListeners()
  }
  
  // 主动断开WebSocket连接
  if (wsService && wsService.getConnectedStatus()) {
    try {
      console.log('组件卸载，主动断开WebSocket连接')
      wsService.close()
    } catch (error) {
      console.error('组件卸载时关闭WebSocket连接失败:', error)
      // 强制关闭连接
      if (wsService.ws) {
        wsService.ws.close()
        wsService.ws = null
      }
    }
  }
  
  // 清理患者端长连接（如果存在）
  if (wsService && wsService.getPatientConnectionStatus()) {
    try {
      console.log('组件卸载，关闭患者端长连接')
      wsService.closePatientConnection()
    } catch (error) {
      console.error('组件卸载时关闭患者端长连接失败:', error)
    }
  }
})
</script>

<style scoped>
.chat-container {
  max-width: 980px;
  margin: 0 auto;
  padding: 12px;
}

.chat-card {
  border-radius: var(--radius-2xl);
  box-shadow: var(--shadow-lg);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.header-left h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--neutral-800);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-content {
  height: min(62vh, 640px);
  overflow-y: auto;
  padding: 14px;
  background:
    linear-gradient(180deg, rgb(var(--primary-50-rgb) / 0.55), rgba(255, 255, 255, 0.55));
  border-radius: var(--radius-xl);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.22);
}

.waiting-tip {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.queue-info {
  text-align: center;
  margin-top: 20px;
  color: var(--neutral-600);
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message-item {
  display: flex;
  margin-bottom: 10px;
}

.doctor-message {
  flex-direction: row;
}

.patient-message {
  flex-direction: row-reverse;
}

.message-avatar {
  margin: 0 10px;
}

.message-content {
  max-width: min(72%, 560px);
}

.doctor-message .message-content {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 14px 14px 14px 6px;
  padding: 10px 12px;
  box-shadow: var(--shadow-sm);
  border: 1px solid rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
}

.patient-message .message-content {
  background: linear-gradient(135deg, var(--primary-600), var(--primary-700));
  color: #fff;
  border-radius: 14px 14px 6px 14px;
  padding: 10px 12px;
  box-shadow: var(--shadow-sm);
}

.message-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
  font-size: 12px;
  color: var(--neutral-500);
}

.patient-message .message-info {
  color: rgba(255, 255, 255, 0.85);
}

.message-body {
  word-break: break-word;
}

.text-message {
  line-height: 1.5;
}

.patient-message .text-message {
  color: rgba(255, 255, 255, 0.95);
}

.image-message .el-image {
  max-width: 200px;
  border-radius: 10px;
  overflow: hidden;
}

.system-message {
  text-align: center;
  margin: 10px 0;
  color: var(--neutral-500);
  font-size: 12px;
}

.chat-input {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 12px;
  border-radius: var(--radius-xl);
  background: rgba(255, 255, 255, 0.75);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.22);
  box-shadow: var(--shadow-md);
  backdrop-filter: blur(10px);
}

.input-toolbar {
  display: flex;
  gap: 10px;
}

.input-box {
  flex: 1;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}

@media (max-width: 768px) {
  .chat-container {
    padding: 8px;
  }
  .chat-content {
    height: min(70vh, 620px);
    padding: 12px;
  }
  .message-content {
    max-width: 84%;
  }
}

.medical-record {
  margin-top: 20px;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 5px;
}

.record-content {
  padding: 10px 0;
}

.prescription-info {
  margin-top: 20px;
}

.prescription-info h3 {
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
}

.prescription-total {
  margin-top: 10px;
  text-align: right;
  font-weight: 500;
}

.fee {
  color: var(--warning);
  font-size: 16px;
}
</style>
