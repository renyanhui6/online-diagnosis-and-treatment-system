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
    this.isConnecting = false
    this.isConnected = false
    this.heartbeatInterval = null
    this.heartbeatTimeout = null
    
    // 新增：患者端长连接相关
    this.patientConnection = null
    this.isPatientConnected = false
    this.consultationCallbacks = [] // 问诊请求回调
    this.notificationCallbacks = [] // 通知回调
  }

  // 连接WebSocket（聊天室连接）
  connect(roomId) {
    if (this.isConnecting) {
      console.log('WebSocket正在连接中，跳过重复连接')
      return
    }

    this.roomId = roomId
    const token = localStorage.getItem('token')
    if (!token) {
      console.error('未登录，无法建立WebSocket连接')
      return
    }

    // 关闭之前的连接
    if (this.ws) {
      this.close()
    }

    this.isConnecting = true
    console.log('开始建立WebSocket连接，roomId:', roomId)

    // 创建新的WebSocket连接 - 支持环境开关切换 Netty 路径
    const isHttps = window.location.protocol === 'https:'
    const backendHost = import.meta.env.VITE_WS_BASE || (import.meta.env.DEV ? 'localhost:9001' : window.location.host)
    const wsPath = import.meta.env.VITE_WS_PATH || '/netty/ws/chat'
    const wsUrl = `${isHttps ? 'wss' : 'ws'}://${backendHost}${wsPath}/${roomId}?token=${token}`
    console.log('WebSocket连接URL:', wsUrl)
    
    this.ws = new WebSocket(wsUrl)

    // 连接打开事件
    this.ws.onopen = () => {
      console.log('WebSocket连接已建立')
      this.isConnecting = false
      this.isConnected = true
      this.reconnectAttempts = 0
      
      // 启动心跳检测
      this.startHeartbeat()
      
      this.connectionCallbacks.forEach(callback => callback())
    }

    // 接收消息事件
    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('收到WebSocket消息:', data)
        console.log('消息详情:', JSON.stringify(data, null, 2))
        
        // 处理心跳响应
        if (data.type === 'pong') {
          this.handleHeartbeatResponse()
          return
        }
        
        // 处理连接成功消息
        if (data.type === 'connection') {
          console.log('WebSocket连接确认:', data)
          return
        }
        
        // 处理聊天消息 - 支持多种消息类型和格式
        if (data.type === 'chat' || data.type === 'message' || 
            (data.type === undefined && data.content !== undefined)) {
          console.log('收到聊天消息:', data)
          console.log('消息类型:', data.type, '发送者类型:', data.sender_type || data.senderType || '未知')
          
          // 标准化消息格式，确保关键字段存在
          let normalizedData = { ...data };
          
          // 检查消息结构，处理嵌套的data字段
          if (data.data) {
            console.log('消息内部data字段:', JSON.stringify(data.data, null, 2))
            console.log('内部发送者类型:', data.data.sender_type || data.data.senderType || '未知')
            
            // 将嵌套的data字段合并到顶层
            if (typeof data.data === 'object') {
              normalizedData = { ...normalizedData, ...data.data };
              
              // 确保关键字段存在
              if (!normalizedData.content && data.data.content) {
                normalizedData.content = data.data.content;
              }
              
              // 确保发送者类型字段存在（同时支持驼峰和蛇形命名）
              if (data.data.sender_type !== undefined) {
                normalizedData.sender_type = data.data.sender_type;
                normalizedData.senderType = data.data.sender_type;
              } else if (data.data.senderType !== undefined) {
                normalizedData.sender_type = data.data.senderType;
                normalizedData.senderType = data.data.senderType;
              }
              
              // 确保消息类型字段存在（同时支持驼峰和蛇形命名）
              if (data.data.message_type !== undefined) {
                normalizedData.message_type = data.data.message_type;
                normalizedData.messageType = data.data.message_type;
              } else if (data.data.messageType !== undefined) {
                normalizedData.message_type = data.data.messageType;
                normalizedData.messageType = data.data.messageType;
              }
            }
          }
          
          console.log('标准化后的消息:', normalizedData);
          
          // 将标准化后的消息转发给所有注册的回调函数
          this.messageCallbacks.forEach(callback => callback(normalizedData))
          return
        }
        
        // 处理问诊请求通知
        if (data.type === 'consultation_request') {
          console.log('收到医生问诊请求:', data)
          this.consultationCallbacks.forEach(callback => callback(data))
          return
        }
        
        // 处理患者响应通知
        if (data.type === 'patient_accepted' || data.type === 'patient_rejected' || data.type === 'patient_timeout') {
          console.log('收到患者响应通知:', data)
          this.notificationCallbacks.forEach(callback => callback(data))
          return
        }
        
        this.messageCallbacks.forEach(callback => callback(data))
      } catch (error) {
        console.error('解析WebSocket消息失败:', error, '原始数据:', event.data)
      }
    }

    // 连接错误事件
    this.ws.onerror = (error) => {
      console.error('WebSocket连接错误:', error)
      this.isConnecting = false
      this.isConnected = false
      this.errorCallbacks.forEach(callback => callback(error))
    }

    // 连接关闭事件
    this.ws.onclose = (event) => {
      console.log('WebSocket连接已关闭:', event)
      this.isConnecting = false
      this.isConnected = false
      this.stopHeartbeat()
      this.closeCallbacks.forEach(callback => callback(event))

      // 只有在非正常关闭时才尝试重新连接
      if (event.code !== 1000 && this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectAttempts++
        console.log(`准备重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
        setTimeout(() => {
          console.log(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)
          this.connect(this.roomId)
        }, this.reconnectInterval)
      } else if (event.code === 1000) {
        console.log('正常关闭连接，不进行重连')
        this.reconnectAttempts = 0
        // 清理连接状态
        this.ws = null
      } else {
        console.log('达到最大重连次数，停止重连')
        this.reconnectAttempts = 0
      }
    }
  }

  // 新增：患者端长连接（登录后建立）
  connectAsPatient() {
    console.log('=== 开始建立患者端WebSocket连接 ===')
    
    if (this.isPatientConnected) {
      console.log('⚠️ 患者端长连接已存在，跳过重复连接')
      return
    }

    const token = localStorage.getItem('token')
    console.log('当前token:', token)
    
    if (!token) {
      console.error('❌ 未登录，无法建立患者端长连接')
      return
    }

    console.log('✅ 开始建立患者端长连接')

    // 创建新的WebSocket连接 - 患者端专用
    const isHttps = window.location.protocol === 'https:'
    const backendHost = import.meta.env.VITE_WS_BASE || (import.meta.env.DEV ? 'localhost:9001' : window.location.host)
    const wsPath = import.meta.env.VITE_WS_PATH || '/netty/ws/chat'
    
    // 患者端连接到个人通知频道
    const userId = this.getUserIdFromToken(token)
    console.log('解析出的用户ID:', userId)
    console.log('用户信息详情:', JSON.stringify(JSON.parse(localStorage.getItem('userInfo') || '{}')))
    
    const wsUrl = `${isHttps ? 'wss' : 'ws'}://${backendHost}${wsPath}/patient_${userId}?token=${token}`
    console.log('患者端WebSocket连接URL:', wsUrl)
    
    this.patientConnection = new WebSocket(wsUrl)

    // 连接打开事件
    this.patientConnection.onopen = () => {
      console.log('✅ 患者端长连接已建立')
      this.isPatientConnected = true
      
      // 发送患者上线消息
      this.sendPatientMessage({
        type: 'patient_online',
        data: {
          timestamp: new Date().toISOString()
        }
      })
    }

    // 接收消息事件
    this.patientConnection.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        console.log('患者端收到消息:', data)
        console.log('消息类型:', data.type)
        console.log('问诊请求回调数量:', this.consultationCallbacks.length)
        
        // 处理问诊请求通知
        if (data.type === 'consultation_request') {
          console.log('收到医生问诊请求:', data)
          this.consultationCallbacks.forEach(callback => callback(data))
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
        console.error('解析患者端WebSocket消息失败:', error, '原始数据:', event.data)
      }
    }

    // 连接错误事件
    this.patientConnection.onerror = (error) => {
      console.error('❌ 患者端WebSocket连接错误:', error)
      console.error('错误详情:', error)
      this.isPatientConnected = false
    }

    // 连接关闭事件
    this.patientConnection.onclose = (event) => {
      console.log('患者端WebSocket连接已关闭:', event)
      this.isPatientConnected = false
      
      // 只有在非正常关闭时才尝试重新连接
      if (event.code !== 1000) {
        setTimeout(() => {
          console.log('患者端连接断开，尝试重新连接...')
          this.connectAsPatient()
        }, 5000)
      } else {
        console.log('患者端长连接正常关闭，不进行重连')
        // 清理连接状态
        this.patientConnection = null
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

  // 新增：发送患者端消息
  sendPatientMessage(message) {
    if (this.patientConnection && this.patientConnection.readyState === WebSocket.OPEN && this.isPatientConnected) {
      try {
        const messageStr = JSON.stringify(message)
        this.patientConnection.send(messageStr)
        console.log('发送患者端消息:', message)
        return true
      } catch (error) {
        console.error('发送患者端消息失败:', error)
        return false
      }
    } else {
      console.error('患者端WebSocket未连接，无法发送消息')
      return false
    }
  }

  // 新增：响应问诊请求
  respondToConsultation(consultationId, response, roomId) {
    return this.sendPatientMessage({
      type: 'consultation_response',
      data: {
        consultationId,
        response, // 'accept' 或 'reject'
        roomId,
        timestamp: new Date().toISOString()
      }
    })
  }

  // 新增：添加问诊请求监听器
  onConsultationRequest(callback) {
    this.consultationCallbacks.push(callback)
  }

  // 新增：添加通知监听器
  onNotification(callback) {
    this.notificationCallbacks.push(callback)
  }

  // 新增：移除问诊请求监听器
  removeConsultationListener(callback) {
    this.consultationCallbacks = this.consultationCallbacks.filter(cb => cb !== callback)
  }

  // 新增：移除通知监听器
  removeNotificationListener(callback) {
    this.notificationCallbacks = this.notificationCallbacks.filter(cb => cb !== callback)
  }

  // 启动心跳检测
  startHeartbeat() {
    this.heartbeatInterval = setInterval(() => {
      if (this.isConnected) {
        this.send({ type: 'ping' })
        
        // 设置心跳超时
        this.heartbeatTimeout = setTimeout(() => {
          console.log('心跳超时，关闭连接')
          this.close()
        }, 5000) // 5秒超时
      }
    }, 30000) // 30秒发送一次心跳
  }

  // 处理心跳响应
  handleHeartbeatResponse() {
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout)
      this.heartbeatTimeout = null
    }
  }

  // 停止心跳检测
  stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
    if (this.heartbeatTimeout) {
      clearTimeout(this.heartbeatTimeout)
      this.heartbeatTimeout = null
    }
  }

  // 发送消息 - 根据后端期望的格式调整
  send(message) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN && this.isConnected) {
      try {
        const messageStr = JSON.stringify(message)
        this.ws.send(messageStr)
        console.log('发送WebSocket消息:', message)
        return true
      } catch (error) {
        console.error('发送WebSocket消息失败:', error)
        return false
      }
    } else {
      console.error('WebSocket未连接，无法发送消息')
      return false
    }
  }

  // 发送聊天消息 - 根据后端格式
  sendChatMessage(roomId, senderId, messageType, content, senderType = 1) {
    // 添加详细日志
    console.log('发送聊天消息:', {
      roomId, 
      senderId, 
      messageType, 
      content: content.substring(0, 50) + (content.length > 50 ? '...' : ''),
      senderType
    });
    
    // 确保senderType是数字类型
    const senderTypeNum = parseInt(senderType);
    
    const message = {
      type: 'chat',
      roomId: roomId,
      senderId: senderId,
      sender_type: senderTypeNum, // 添加sender_type字段（后端可能使用蛇形命名）
      senderType: senderTypeNum, // 1-患者，2-医生（保留驼峰命名兼容性）
      messageType: messageType,
      message_type: messageType, // 添加message_type字段（后端可能使用蛇形命名）
      content: content,
      timestamp: new Date().toISOString()
    }
    
    console.log('最终发送的消息格式:', message);
    return this.send(message)
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

  // 关闭连接
  close() {
    console.log('WebSocket服务关闭连接')
    this.isConnecting = false
    this.isConnected = false
    this.stopHeartbeat()
    
    if (this.ws) {
      try {
        // 检查连接状态
        if (this.ws.readyState === WebSocket.OPEN) {
          console.log('WebSocket连接状态为OPEN，正在关闭...')
          this.ws.close(1000, 'Normal closure')
        } else {
          console.log('WebSocket连接状态为:', this.ws.readyState)
        }
      } catch (error) {
        console.error('关闭WebSocket连接时出错:', error)
      } finally {
        this.ws = null
      }
    }
  }

  // 新增：关闭患者端连接
  closePatientConnection() {
    console.log('关闭患者端长连接')
    this.isPatientConnected = false
    
    if (this.patientConnection) {
      try {
        // 检查连接状态
        if (this.patientConnection.readyState === WebSocket.OPEN) {
          console.log('患者端长连接状态为OPEN，正在关闭...')
          this.patientConnection.close(1000, 'Normal closure')
        } else {
          console.log('患者端长连接状态为:', this.patientConnection.readyState)
        }
      } catch (error) {
        console.error('关闭患者端长连接时出错:', error)
      } finally {
        this.patientConnection = null
      }
    }
  }

  // 获取连接状态
  getConnectionState() {
    if (!this.ws) return 'disconnected'
    switch (this.ws.readyState) {
      case WebSocket.CONNECTING:
        return 'connecting'
      case WebSocket.OPEN:
        return 'open'
      case WebSocket.CLOSING:
        return 'closing'
      case WebSocket.CLOSED:
        return 'closed'
      default:
        return 'unknown'
    }
  }

  // 检查是否已连接
  isConnected() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }

  // 获取连接状态（避免递归调用）
  getConnectedStatus() {
    return this.ws && this.ws.readyState === WebSocket.OPEN
  }

  // 新增：获取患者端连接状态
  getPatientConnectionStatus() {
    return this.isPatientConnected && this.patientConnection && this.patientConnection.readyState === WebSocket.OPEN
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

  // 清理所有监听器
  clearAllListeners() {
    this.messageCallbacks = []
    this.connectionCallbacks = []
    this.errorCallbacks = []
    this.closeCallbacks = []
    this.consultationCallbacks = []
    this.notificationCallbacks = []
  }
}

// Export both the class and a singleton instance
export { WebSocketService }

// Create singleton for backward compatibility
const webSocketService = new WebSocketService()

// 添加调试信息
console.log('WebSocketService初始化完成，导出类和单例实例');
console.log('WebSocketService类型:', typeof WebSocketService);
console.log('webSocketService实例类型:', typeof webSocketService);

// Export the singleton as default for backward compatibility
// while also allowing new instances to be created with WebSocketService
export default webSocketService
