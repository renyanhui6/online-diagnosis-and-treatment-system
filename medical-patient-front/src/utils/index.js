import axios from "axios"

// 格式化日期
export function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
  if (!date) return ''
  
  if (typeof date === 'string') {
    date = new Date(date)
  }
  
  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    return ''
  }
  
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()
  
  // 确保format不为null或undefined
  if (!format) {
    format = 'YYYY-MM-DD HH:mm:ss'
  }
  
  return format
    .replace('YYYY', year)
    .replace('MM', month.toString().padStart(2, '0'))
    .replace('DD', day.toString().padStart(2, '0'))
    .replace('HH', hour.toString().padStart(2, '0'))
    .replace('mm', minute.toString().padStart(2, '0'))
    .replace('ss', second.toString().padStart(2, '0'))
}

// 获取当前日期
export function getCurrentDate(format = 'YYYY-MM-DD') {
  return formatDate(new Date(), format)
}

// 获取未来几天的日期
export function getFutureDates(days) {
  const dates = []
  const today = new Date()
  
  for (let i = 0; i < days; i++) {
    const date = new Date(today)
    date.setDate(today.getDate() + i)
    dates.push({
      date: formatDate(date, 'YYYY-MM-DD'),
      day: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][date.getDay()],
      isWeekend: date.getDay() === 0 || date.getDay() === 6
    })
  }
  
  return dates
}

// 格式化金额
export function formatMoney(money) {
  if (money === undefined || money === null) return '0.00'
  return parseFloat(money).toFixed(2)
}

// 生成随机字符串
export function randomString(length = 8) {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

// 深拷贝
export function deepClone(obj) {
  if (obj === null || typeof obj !== 'object') return obj
  const clone = Array.isArray(obj) ? [] : {}
  for (const key in obj) {
    if (Object.prototype.hasOwnProperty.call(obj, key)) {
      clone[key] = deepClone(obj[key])
    }
  }
  return clone
}

// 防抖函数
export function debounce(func, wait = 300) {
  let timeout
  return function(...args) {
    clearTimeout(timeout)
    timeout = setTimeout(() => {
      func.apply(this, args)
    }, wait)
  }
}

// 节流函数
export function throttle(func, wait = 300) {
  let timeout = null
  let previous = 0
  
  return function(...args) {
    const now = Date.now()
    const remaining = wait - (now - previous)
    
    if (remaining <= 0) {
      if (timeout) {
        clearTimeout(timeout)
        timeout = null
      }
      previous = now
      func.apply(this, args)
    } else if (!timeout) {
      timeout = setTimeout(() => {
        previous = Date.now()
        timeout = null
        func.apply(this, args)
      }, remaining)
    }
  }
}

// 预约状态相关工具函数
export const APPOINTMENT_STATUS = {
  PENDING_PAYMENT: 0,    // 待支付
  PAID: 1,               // 已支付
  QUEUING: 2,            // 排队中
  CONSULTING: 3,         // 问诊中
  COMPLETED: 4,          // 已完成
  SUSPENDED: 5,          // 暂时挂起
  RESUMED: 6,            // 已回归
  WAITING_CONFIRM: 7,    // 等待患者确认
  INVALID: 8             // 失效
}

// 获取预约状态文本
export function getAppointmentStatusText(status) {
  const statusMap = {
    [APPOINTMENT_STATUS.PENDING_PAYMENT]: '待支付',
    [APPOINTMENT_STATUS.PAID]: '已支付',
    [APPOINTMENT_STATUS.QUEUING]: '排队中',
    [APPOINTMENT_STATUS.CONSULTING]: '问诊中',
    [APPOINTMENT_STATUS.COMPLETED]: '已完成',
    [APPOINTMENT_STATUS.SUSPENDED]: '暂时挂起',
    [APPOINTMENT_STATUS.RESUMED]: '已回归',
    [APPOINTMENT_STATUS.WAITING_CONFIRM]: '等待患者确认',
    [APPOINTMENT_STATUS.INVALID]: '失效'
  }
  return statusMap[status] || '未知状态'
}

// 获取预约状态类型（用于标签颜色）
export function getAppointmentStatusType(status) {
  const typeMap = {
    [APPOINTMENT_STATUS.PENDING_PAYMENT]: 'warning',
    [APPOINTMENT_STATUS.PAID]: 'success',
    [APPOINTMENT_STATUS.QUEUING]: 'info',
    [APPOINTMENT_STATUS.CONSULTING]: 'primary',
    [APPOINTMENT_STATUS.COMPLETED]: 'success',
    [APPOINTMENT_STATUS.SUSPENDED]: 'danger',
    [APPOINTMENT_STATUS.RESUMED]: 'info',
    [APPOINTMENT_STATUS.WAITING_CONFIRM]: 'warning',
    [APPOINTMENT_STATUS.INVALID]: 'info'
  }
  return typeMap[status] || 'info'
}

// 检查预约是否可以恢复
export function canResumeAppointment(status) {
  return status === APPOINTMENT_STATUS.SUSPENDED
}

// 检查预约是否可以问诊
export function canStartConsultation(status) {
  return status === APPOINTMENT_STATUS.QUEUING || status === APPOINTMENT_STATUS.CONSULTING
}

// 检查预约是否等待患者确认
export function isWaitingPatientConfirm(status) {
  return status === APPOINTMENT_STATUS.WAITING_CONFIRM
}

// 获取验证码
export function generateCaptcha() {
  // 使用完整URL并添加时间戳防止缓存
  const timestamp = new Date().getTime();
  return axios({
    url: `http://localhost:8080/treat/front/loginAndOut/captchaCode?t=${timestamp}`,
    method: "get",
    // 确保请求头中包含正确的Content-Type
    headers: {
      'Content-Type': 'application/json'
    }
  }).then(response => {
    // 打印完整响应以便调试
    console.log('验证码响应:', response);
    return response.data;
  }).catch(error => {
    console.error('获取验证码错误:', error);
    throw error;
  });
}

// 校验手机号
export function validatePhone(phone) {
  return /^1[3-9]\d{9}$/.test(phone)
}

// 校验邮箱
export function validateEmail(email) {
  return /^[\w-]+(\.[\w-]+)*@[\w-]+(\.[\w-]+)+$/.test(email)
}

// 校验身份证
export function validateIdCard(idCard) {
  return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)
}
