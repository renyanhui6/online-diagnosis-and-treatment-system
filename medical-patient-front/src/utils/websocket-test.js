/**
 * WebSocket连接测试工具
 */

// 测试WebSocket连接
export function testWebSocketConnection(roomId, token) {
  return new Promise((resolve, reject) => {
    // WebSocket连接直接连接到后端，不通过Vite代理
    let backendHost
    if (import.meta.env.DEV) {
      // 开发环境：直接连接到 Netty 服务
      backendHost = 'localhost:9001'
    } else {
      // 生产环境：使用当前域名
      backendHost = window.location.host
    }

    const wsUrl = `ws://${backendHost}/netty/ws/chat/${roomId}?token=${token}`
    console.log('测试WebSocket连接URL:', wsUrl)
    console.log('当前环境:', import.meta.env.MODE)
    console.log('后端主机:', backendHost)
    
    const ws = new WebSocket(wsUrl)
    
    const timeout = setTimeout(() => {
      ws.close()
      reject(new Error('WebSocket连接超时'))
    }, 10000) // 10秒超时
    
    ws.onopen = () => {
      console.log('WebSocket连接测试成功')
      clearTimeout(timeout)
      ws.close()
      resolve(true)
    }
    
    ws.onerror = (error) => {
      console.error('WebSocket连接测试失败:', error)
      console.error('连接URL:', wsUrl)
      clearTimeout(timeout)
      reject(error)
    }
    
    ws.onclose = (event) => {
      console.log('WebSocket连接测试关闭:', event)
      console.log('关闭代码:', event.code)
      clearTimeout(timeout)
      if (event.code !== 1000) {
        reject(new Error(`WebSocket连接异常关闭，代码: ${event.code}`))
      }
    }
    
    ws.onmessage = (event) => {
      console.log('收到测试消息:', event.data)
    }
  })
}

// 检查WebSocket支持
export function checkWebSocketSupport() {
  if (typeof WebSocket === 'undefined') {
    return {
      supported: false,
      message: '浏览器不支持WebSocket'
    }
  }
  
  return {
    supported: true,
    message: 'WebSocket支持正常'
  }
}

// 检查网络连接
export function checkNetworkConnection() {
  if (!navigator.onLine) {
    return {
      online: false,
      message: '网络连接已断开'
    }
  }
  
  return {
    online: true,
    message: '网络连接正常'
  }
}

// 获取当前页面URL信息
export function getCurrentUrlInfo() {
  return {
    protocol: window.location.protocol,
    host: window.location.host,
    hostname: window.location.hostname,
    port: window.location.port,
    pathname: window.location.pathname
  }
} 
