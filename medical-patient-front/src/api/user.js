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

// 更新用户信息
export function updateUserInfo(data) {
  return request({
    url: '/user/info',
    method: 'put',
    data
  })
}

// 实名认证
export function userAuth(data) {
  return request({
    url: '/user/auth',
    method: 'post',
    data
  })
}

// 获取就诊人列表
export function getPatientAttendantList() {
  return request({
    url: '/front/patient/attendant/getPatientList',
    method: 'get'
  })
}

// 添加就诊人
export function addPatientAttendant(data) {
  return request({
    url: '/front/patient/attendant/addPatientAttendant',
    method: 'post',
    data
  })
}


// 删除就诊人
export function deletePatientAttendant(id) {
  return request({
    url: `/front/patient/attendant/removePatientAttendant`,
    method: 'get',
    params: { id }
  })
}

// 身份验证
export function verifyIdentity(data) {
  return request({
    url: '/user/verify-identity',
    method: 'post',
    data
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






