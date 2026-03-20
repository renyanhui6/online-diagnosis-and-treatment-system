import request from './request'

// 登录
export function login(data) {
  // 打印登录请求数据，用于调试
  console.log('登录请求数据:', JSON.stringify(data))
  
  return request({
    url: '/front/loginAndOut/login',
    method: 'post',
    data,
    headers: {
      // 确保请求头中包含正确的Content-Type
      'Content-Type': 'application/json'
    }
  })
}



// 注册
export function register(data) {
  console.log('register API调用，参数:', data)
  return request({
    url: '/front/patient/loginAndOut/register',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 找回密码
export function resetPassword(data) {
  console.log('resetPassword API调用，参数:', data)
  return request({
    url: '/front/loginAndOut/findPassword',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

// 获取邮箱验证码
export function getEmailCode(data) {
  console.log('getEmailCode API调用，参数:', data)
  return request({
    url: '/front/loginAndOut/getEmailCode',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'application/json'
    },
     timeout: 30000
  })
}

// 获取用户信息
export function getUserInfo() {
  // 打印当前token，用于调试
  console.log('获取用户信息使用的token:', localStorage.getItem('token'))
  
  return request({
    url: '/front/loginAndOut/getUserInfo',
    method: 'get'
    // 不需要在这里设置Authorization头，请求拦截器会自动添加
  })
}

export function getDevToken(params = {}) {
  return request({
    url: '/front/loginAndOut/devToken',
    method: 'get',
    params
  })
}

// 获取就诊人列表
export function getPatientList() {
  return request({
    url: '/front/patient/attendant/getPatientList',
    method: 'get'
  })
}

// 获取就诊人列表（别名）
export function getCaseList() {
  return getPatientList()
}




