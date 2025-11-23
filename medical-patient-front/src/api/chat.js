import request from './request'

// 医生发起问诊
export function initiateConsultation(data) {
  return request({
    url: '/chat/initiate-consultation',
    method: 'post',
    data
  })
}

// 患者响应问诊请求
export function respondToConsultation(data) {
  return request({
    url: '/chat/respond-consultation',
    method: 'post',
    data
  })
}

// 恢复挂起的问诊
export function resumeConsultation(data) {
  return request({
    url: '/chat/resume-consultation',
    method: 'post',
    data
  })
}

// 获取聊天记录
export function getChatMessages(registrationId) {
  return request({
    url: `/chat/messages/${registrationId}`,
    method: 'get'
  })
}

// 获取房间状态
export function getRoomStatus(registrationId) {
  return request({
    url: `/chat/room/${registrationId}`,
    method: 'get'
  })
}

// 发送聊天消息
export function sendChatMessage(data) {
  return request({
    url: '/chat/send-message',
    method: 'post',
    data
  })
}

// 上传聊天图片
export function uploadChatImage(formData) {
  return request({
    url: '/chat/upload-image',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}