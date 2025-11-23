// 通知工具类
class NotificationService {
  constructor() {
    this.permission = 'default'
    this.init()
  }

  // 初始化通知服务
  async init() {
    if ('Notification' in window) {
      this.permission = await this.requestPermission()
    } else {
      console.warn('浏览器不支持通知功能')
    }
  }

  // 请求通知权限
  async requestPermission() {
    if (Notification.permission === 'granted') {
      return 'granted'
    } else if (Notification.permission !== 'denied') {
      const permission = await Notification.requestPermission()
      return permission
    }
    return Notification.permission
  }

  // 显示通知
  showNotification(title, options = {}) {
    if (this.permission !== 'granted') {
      console.warn('没有通知权限')
      return null
    }

    const defaultOptions = {
      icon: '/favicon.ico',
      badge: '/favicon.ico',
      tag: 'consultation-request',
      requireInteraction: true, // 需要用户交互才能关闭
      silent: false,
      ...options
    }

    const notification = new Notification(title, defaultOptions)

    // 设置点击事件
    notification.onclick = () => {
      window.focus()
      notification.close()
      if (options.onClick) {
        options.onClick()
      }
    }

    // 自动关闭（如果没有设置requireInteraction）
    if (!defaultOptions.requireInteraction && options.autoClose !== false) {
      setTimeout(() => {
        notification.close()
      }, options.duration || 5000)
    }

    return notification
  }

  // 显示问诊请求通知
  showConsultationRequest(doctorName, options = {}) {
    return this.showNotification(
      '医生问诊请求',
      {
        body: `${doctorName}医生邀请您开始问诊，请及时响应`,
        icon: '/favicon.ico',
        tag: 'consultation-request',
        requireInteraction: true,
        // 移除actions，因为浏览器通知API不支持
        ...options
      }
    )
  }

  // 播放提示音
  playNotificationSound() {
    try {
      // 创建音频上下文
      const audioContext = new (window.AudioContext || window.webkitAudioContext)()
      
      // 创建振荡器
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      
      // 连接节点
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      
      // 设置音频参数
      oscillator.frequency.setValueAtTime(800, audioContext.currentTime)
      oscillator.frequency.setValueAtTime(600, audioContext.currentTime + 0.1)
      oscillator.frequency.setValueAtTime(800, audioContext.currentTime + 0.2)
      
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3)
      
      // 播放音频
      oscillator.start(audioContext.currentTime)
      oscillator.stop(audioContext.currentTime + 0.3)
    } catch (error) {
      console.warn('无法播放提示音:', error)
    }
  }

  // 检查是否支持通知
  isSupported() {
    return 'Notification' in window
  }

  // 获取当前权限状态
  getPermission() {
    return this.permission
  }
}

// 创建单例
const notificationService = new NotificationService()

export default notificationService