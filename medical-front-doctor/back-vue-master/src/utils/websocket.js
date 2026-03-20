// WebSocket服务
class WebSocketService {
  constructor() {
    this.ws = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectInterval = 3000 // 3秒
    this.messageCallbacks = []
    this.connectionCallbacks = []
    this.errorCallbacks = []
    this.closeCallbacks = []
    this.roomId = null
    
    // 新增：医生端长连接相关
    this.doctorConnection = null
    this.isDoctorConnected = false
    this.consultationResponseCallbacks = [] // 问诊响应回调
    this.notificationCallbacks = [] // 通知回调
  }



  // 连接WebSocket
  connect(roomId) {
    this.roomId = roomId
    const token = localStorage.getItem('token')
    if (!token) {
      console.error('未登录，无法建立WebSocket连接')
      return
    }

    // 只关闭之前的聊天室连接，不影响医生端长连接
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      console.log('🔌 关闭之前的聊天室连接');
      this.ws.close(1000, 'Switching to new chat room');
      this.ws = null;
    }

    // 创建新的WebSocket连接，支持环境开关以适配 Netty 端点
    const isHttps = window.location.protocol === 'https:'
    const backendHost = import.meta.env.VITE_WS_BASE || (import.meta.env.DEV ? 'localhost:9001' : window.location.host)
    const wsPath = import.meta.env.VITE_WS_PATH || '/netty/ws/chat'
    const wsUrl = `${isHttps ? 'wss' : 'ws'}://${backendHost}${wsPath}/${roomId}?token=${token}`
    console.log('🔗 连接WebSocket，房间ID:', roomId, 'URL:', wsUrl);
    
    try {
      this.ws = new WebSocket(wsUrl)
    } catch (error) {
      console.error('创建WebSocket连接失败:', error);
      return;
    }

    // 连接打开事件
    this.ws.onopen = () => {
      console.log('✅ WebSocket连接已建立，房间ID:', roomId)
      this.reconnectAttempts = 0
      this.connectionCallbacks.forEach(callback => callback())
    }

    // 接收消息事件
    this.ws.onmessage = (event) => {
      console.log('📨 收到WebSocket消息:', event.data);
      try {
        const data = JSON.parse(event.data)
        this.messageCallbacks.forEach(callback => callback(data))
      } catch (error) {
        console.error('解析WebSocket消息失败:', error)
      }
    }

    // 连接错误事件
    this.ws.onerror = (error) => {
      console.error('❌ WebSocket连接错误:', error)
      this.errorCallbacks.forEach(callback => callback(error))
    }

    // 连接关闭事件
    this.ws.onclose = (event) => {
      console.log('🔌 WebSocket连接已关闭')
      this.closeCallbacks.forEach(callback => callback(event))

      // 只有在非主动关闭的情况下才尝试重新连接
      if (this.roomId && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        setTimeout(() => {
          console.log(`🔄 尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)
          this.connect(this.roomId)
        }, this.reconnectInterval)
      }
    }
  }

  // 发送消息
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message))
      return true
    } else {
      console.error('WebSocket未连接，无法发送消息')
      return false
    }
  }

  // 关闭聊天室连接（不影响医生端长连接）
  close() {
    if (this.ws) {
      // 清除房间ID，防止自动重连
      this.roomId = null
      this.ws.close()
      this.ws = null
    }
  }

  // 添加消息监听器
  onMessage(callback) {
    this.messageCallbacks.push(callback)
  }

  // 添加连接成功监听器
  onConnection(callback) {
    this.connectionCallbacks.push(callback)
  }

  // 添加错误监听器
  onError(callback) {
    this.errorCallbacks.push(callback)
  }

  // 添加关闭监听器
  onClose(callback) {
    this.closeCallbacks.push(callback)
  }

  // 移除消息监听器
  removeMessageListener(callback) {
    this.messageCallbacks = this.messageCallbacks.filter(cb => cb !== callback)
  }

  // 移除连接成功监听器
  removeConnectionListener(callback) {
    this.connectionCallbacks = this.connectionCallbacks.filter(cb => cb !== callback)
  }

  // 移除错误监听器
  removeErrorListener(callback) {
    this.errorCallbacks = this.errorCallbacks.filter(cb => cb !== callback)
  }

  // 移除关闭监听器
  removeCloseListener(callback) {
    this.closeCallbacks = this.closeCallbacks.filter(cb => cb !== callback)
  }

  // 新增：医生端长连接（登录后建立）
  connectAsDoctor() {
    console.log('=== 开始建立医生端WebSocket连接 ===')
    
    if (this.isDoctorConnected) {
      console.log('⚠️ 医生端长连接已存在，跳过重复连接')
      return
    }

    const token = localStorage.getItem('token')
    console.log('当前token:', token)
    
    if (!token) {
      console.error('❌ 未登录，无法建立医生端长连接')
      return
    }

    console.log('✅ 开始建立医生端长连接')

    // 创建新的WebSocket连接 - 医生端专用
    const isHttps = window.location.protocol === 'https:'
    const backendHost = import.meta.env.VITE_WS_BASE || (import.meta.env.DEV ? 'localhost:9001' : window.location.host)
    const wsPath = import.meta.env.VITE_WS_PATH || '/netty/ws/chat'
    
    // 医生端连接到个人通知频道
    const userId = this.getUserIdFromToken(token)
    console.log('解析出的用户ID:', userId)
    console.log('用户信息详情:', JSON.stringify(JSON.parse(localStorage.getItem('userInfo') || '{}')))
    
    const wsUrl = `${isHttps ? 'wss' : 'ws'}://${backendHost}${wsPath}/doctor_${userId}?token=${token}`
    console.log('医生端WebSocket连接URL:', wsUrl)
    
    this.doctorConnection = new WebSocket(wsUrl)

    // 连接打开事件
    this.doctorConnection.onopen = () => {
      console.log('✅ 医生端长连接已建立')
      this.isDoctorConnected = true
      
      // 发送医生上线消息
      this.sendDoctorMessage({
        type: 'doctor_online',
        data: {
          timestamp: new Date().toISOString()
        }
      })
    }

    // 接收消息事件
    this.doctorConnection.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('医生端收到消息:', data)
        console.log('消息类型:', data.type)
        console.log('问诊响应回调数量:', this.consultationResponseCallbacks.length)
        
        // 处理患者问诊响应通知
        if (data.type === 'consultation_response') {
          console.log('收到患者问诊响应:', data)
          this.consultationResponseCallbacks.forEach(callback => callback(data))
          return
        }
        
        // 处理其他通知
        if (data.type === 'notification') {
          console.log('收到通知:', data)
          this.notificationCallbacks.forEach(callback => callback(data))
          return
        }
        
        // 处理心跳响应
        if (data.type === 'pong') {
          return
        }
        
        // 处理连接确认消息
        if (data.type === 'connection') {
          console.log('收到连接确认消息:', data)
          return
        }
        
        console.log('未处理的消息类型:', data.type)
        
      } catch (error) {
        console.error('解析医生端WebSocket消息失败:', error, '原始数据:', event.data)
      }
    }

    // 连接错误事件
    this.doctorConnection.onerror = (error) => {
      console.error('❌ 医生端WebSocket连接错误:', error)
      console.error('错误详情:', error)
      this.isDoctorConnected = false
    }

    // 连接关闭事件
    this.doctorConnection.onclose = (event) => {
      console.log('医生端WebSocket连接已关闭:', event)
      this.isDoctorConnected = false
      
      // 自动重连
      if (event.code !== 1000) {
        setTimeout(() => {
          console.log('医生端连接断开，尝试重新连接...')
          this.connectAsDoctor()
        }, 5000)
      }
    }
  }

  // 从token中获取用户ID
  getUserIdFromToken(token) {
    try {
      // 这里需要根据你的token格式来解析用户ID
      // 假设token是JWT格式，或者直接包含用户ID
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      return userInfo.id || userInfo.userId || 'unknown'
    } catch (error) {
      console.error('解析用户ID失败:', error)
      return 'unknown'
    }
  }

  // 发送状态更新消息
  sendStatusUpdate(roomId, roomStatus) {
    const message = {
      type: 'status',
      roomId: roomId,
      roomStatus: roomStatus,
      timestamp: new Date().toISOString()
    }
    console.log('发送状态更新消息:', message)
    return this.send(message)
  }

  // 发送聊天消息 - 根据后端格式
  sendChatMessage(roomId, senderId, messageType, content) {
    const message = {
      type: 'chat',
      roomId: roomId,
      senderId: senderId,
      senderType: 2, // 1-患者，2-医生
      messageType: messageType,
      content: content
    }
    return this.send(message)
  }

  // 新增：发送医生端消息
  sendDoctorMessage(message) {
    if (this.doctorConnection && this.doctorConnection.readyState === WebSocket.OPEN && this.isDoctorConnected) {
      try {
        const messageStr = JSON.stringify(message)
        this.doctorConnection.send(messageStr)
        console.log('发送医生端消息:', message)
        return true
      } catch (error) {
        console.error('发送医生端消息失败:', error)
        return false
      }
    } else {
      console.error('医生端WebSocket未连接，无法发送消息')
      return false
    }
  }

  // 新增：添加问诊响应监听器
  onConsultationResponse(callback) {
    this.consultationResponseCallbacks.push(callback)
  }

  // 新增：添加通知监听器
  onNotification(callback) {
    this.notificationCallbacks.push(callback)
  }

  // 新增：移除问诊响应监听器
  removeConsultationResponseListener(callback) {
    this.consultationResponseCallbacks = this.consultationResponseCallbacks.filter(cb => cb !== callback)
  }

  // 新增：移除通知监听器
  removeNotificationListener(callback) {
    this.notificationCallbacks = this.notificationCallbacks.filter(cb => cb !== callback)
  }

  // 新增：关闭医生端连接
  closeDoctorConnection() {
    if (this.doctorConnection) {
      this.isDoctorConnected = false
      this.doctorConnection.close()
      this.doctorConnection = null
    }
  }

  // 新增：获取医生端连接状态
  getDoctorConnectionStatus() {
    if (!this.doctorConnection) return 'CLOSED'
    switch (this.doctorConnection.readyState) {
      case WebSocket.CONNECTING:
        return 'CONNECTING'
      case WebSocket.OPEN:
        return 'OPEN'
      case WebSocket.CLOSING:
        return 'CLOSING'
      case WebSocket.CLOSED:
        return 'CLOSED'
      default:
        return 'UNKNOWN'
    }
  }

  // 新增：检查医生端是否连接
  isDoctorConnectionOpen() {
    return this.isDoctorConnected && this.doctorConnection && this.doctorConnection.readyState === WebSocket.OPEN
  }

  // 获取连接状态
  getConnectionState() {
    if (!this.ws) return 'CLOSED'
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING:
        return 'CONNECTING'
      case WebSocket.OPEN:
        return 'OPEN'
      case WebSocket.CLOSING:
        return 'CLOSING'
      case WebSocket.CLOSED:
        return 'CLOSED'
      default:
        return 'UNKNOWN'
    }
  }

  // 获取聊天室连接状态
  getConnectedStatus() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }
}

// 创建单例
const webSocketService = new WebSocketService()

export default webSocketService
