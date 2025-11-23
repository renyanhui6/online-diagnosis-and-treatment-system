/**
 * WebSocket连接诊断工具
 */

// 检查后端服务器是否可访问
export async function checkBackendServer() {
  const backendHost = import.meta.env.DEV ? 'localhost:8080' : window.location.host
  const testUrl = `http://${backendHost}/common/chat/waiting-patients`
  
  try {
    const response = await fetch(testUrl, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json'
      }
    })
    
    return {
      accessible: response.ok,
      status: response.status,
      statusText: response.statusText,
      url: testUrl
    }
  } catch (error) {
    return {
      accessible: false,
      error: error.message,
      url: testUrl
    }
  }
}

// 检查网络连接
export function checkNetworkStatus() {
  return {
    online: navigator.onLine,
    connectionType: navigator.connection ? navigator.connection.effectiveType : 'unknown',
    downlink: navigator.connection ? navigator.connection.downlink : 'unknown'
  }
}

// 检查浏览器WebSocket支持
export function checkWebSocketSupport() {
  return {
    supported: typeof WebSocket !== 'undefined',
    protocol: window.location.protocol,
    secure: window.location.protocol === 'https:'
  }
}

// 检查环境配置
export function checkEnvironment() {
  return {
    mode: import.meta.env.MODE,
    dev: import.meta.env.DEV,
    prod: import.meta.env.PROD,
    baseUrl: import.meta.env.BASE_URL,
    host: window.location.host,
    hostname: window.location.hostname,
    port: window.location.port
  }
}

// 检查用户状态
export function checkUserStatus() {
  const token = localStorage.getItem('token')
  const userInfo = localStorage.getItem('userInfo')
  
  return {
    hasToken: !!token,
    tokenLength: token ? token.length : 0,
    hasUserInfo: !!userInfo,
    userInfoValid: userInfo ? (userInfo !== 'undefined' && userInfo !== 'null') : false
  }
}

// 完整的诊断报告
export async function generateDiagnosticReport() {
  const report = {
    timestamp: new Date().toISOString(),
    environment: checkEnvironment(),
    network: checkNetworkStatus(),
    websocket: checkWebSocketSupport(),
    user: checkUserStatus(),
    backend: await checkBackendServer()
  }
  
  console.log('WebSocket连接诊断报告:', report)
  return report
}

// 格式化诊断报告为可读文本
export function formatDiagnosticReport(report) {
  const lines = []
  
  lines.push('=== WebSocket连接诊断报告 ===')
  lines.push(`时间: ${report.timestamp}`)
  lines.push('')
  
  lines.push('--- 环境信息 ---')
  lines.push(`模式: ${report.environment.mode}`)
  lines.push(`开发环境: ${report.environment.dev}`)
  lines.push(`主机: ${report.environment.host}`)
  lines.push(`协议: ${report.environment.hostname}`)
  lines.push('')
  
  lines.push('--- 网络状态 ---')
  lines.push(`在线: ${report.network.online}`)
  lines.push(`连接类型: ${report.network.connectionType}`)
  lines.push(`下行速度: ${report.network.downlink}`)
  lines.push('')
  
  lines.push('--- WebSocket支持 ---')
  lines.push(`支持: ${report.websocket.supported}`)
  lines.push(`协议: ${report.websocket.protocol}`)
  lines.push(`安全连接: ${report.websocket.secure}`)
  lines.push('')
  
  lines.push('--- 用户状态 ---')
  lines.push(`有Token: ${report.user.hasToken}`)
  lines.push(`Token长度: ${report.user.tokenLength}`)
  lines.push(`有用户信息: ${report.user.hasUserInfo}`)
  lines.push(`用户信息有效: ${report.user.userInfoValid}`)
  lines.push('')
  
  lines.push('--- 后端服务器 ---')
  lines.push(`可访问: ${report.backend.accessible}`)
  if (report.backend.status) {
    lines.push(`状态码: ${report.backend.status}`)
    lines.push(`状态文本: ${report.backend.statusText}`)
  }
  if (report.backend.error) {
    lines.push(`错误: ${report.backend.error}`)
  }
  lines.push(`测试URL: ${report.backend.url}`)
  lines.push('')
  
  lines.push('=== 诊断完成 ===')
  
  return lines.join('\n')
} 