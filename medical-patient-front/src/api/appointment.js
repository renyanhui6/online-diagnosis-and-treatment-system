import request from './request'

// 获取预约列表
export function getAppointmentList(params = {}) {
  return request({
    url: '/front/patient/appointment/list',
    method: 'get',
    params
  })
}

// 获取预约详情
export function getAppointmentDetail(id) {
  return request({
    url: `/front/patient/appointment/detail/${id}`,
    method: 'get'
  })
}

// 取消预约
export function cancelAppointment(id) {
  return request({
    url: `/front/patient/appointment/cancel?id=${id}`,
    method: 'get'
  })
}

// 获取科室列表
export function getDepartmentList() {
  return request({
    url: '/back/admin/department/findList',
    method: 'get'
  })
}

// 获取子科室列表
export function getSubDepartmentList(departmentId) {
  return request({
    url: `/back/admin/department/findSubList`,
    method: 'get',
    params: {
      departmentId: departmentId
    }
  })
}

// 获取医生列表
export function getDoctorList(params = {}) {
  return request({
    url: '/front/doctor/list',
    method: 'get',
    params
  })
}

// 获取医生排班
export function getDoctorSchedule(params = {}) {
  return request({
    url: '/front/schedule/list',
    method: 'get',
    params
  })
}

// 获取排班列表
export function getScheduleList(params = {}) {
  return request({
    url: '/front/patient/schedule/findList',
    method: 'get',
    params
  })
}

// 获取患者列表
export function getPatientList() {
  return request({
    url: '/front/patient/list',
    method: 'get'
  })
}

// 创建预约挂号
export function createAppointment(data) {
  return request({
    url: '/front/patient/appointment/create',
    method: 'post',
    data: {
      doctorId: data.doctorId,
      patientId: data.patientId,
      scheduleId: data.scheduleId,
      registrationStatus: data.registrationStatus || 0 // 0-待支付
    }
  })
}

// 获取用户预约订单列表
export function getUserAppointmentOrders(params) {
  return request({
    url: '/front/patient/registration/getRegistrationInfoList',
    method: 'post',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    },
    data: {
      startDate: params.startDate,
      endDate: params.endDate
    }
  })
}

// 获取预约订单详情
export function getAppointmentOrder(id) {
  return request({
    url: `/front/patient/appointment/getOrder`,
    method: 'get',
    params: {
      id: id
    }
  })
}

// 完成预约支付
export function finishAppointmentPayment(data) {
  return request({
    url: '/front/patient/appointment/finish',
    method: 'post',
    data: {
      id: data.id,
      paymentMethod: data.paymentMethod,
      paymentGateway: data.paymentGateway || 'wechat'
    }
  })
}

// 退款预约
export function refundAppointment(id) {
  return request({
    url: `/front/patient/appointment/refund`,
    method: 'get',
    params: {
      id: id
    }
  })
}

// 获取支付记录列表
export function getPaymentOrders(params) {
  return request({
    url: '/front/patient/appointment/getOrders',
    method: 'get',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10,
      createDate: params.createDate
    }
  })
}

// 获取聊天记录
export function getChatMessages(appointmentId) {
  return request({
    url: `/front/patient/chat/getChatMessages/${appointmentId}`,
    method: 'get'
  })
}

// 发送聊天消息
export function sendChatMessage(appointmentId, data) {
  return request({
    url: `/front/patient/chat/sendChatMessage/${appointmentId}`,
    method: 'post',
    data
  })
}

// 上传聊天图片
export function uploadChatImage(appointmentId, formData) {
  return request({
    url: `/front/patient/chat/uploadChatImage/${appointmentId}`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 将挂起状态的预约变为已回归状态
export function changeStatusToResumed(registrationId) {
  return request({
    url: '/front/patient/registration/changeStatusToResumed',
    method: 'post',
    params: {
      registrationId: registrationId
    }
  })
}

// 测试导出，确保模块正常工作
export const APPOINTMENT_API = {
  getDepartmentList,
  getSubDepartmentList,
  getScheduleList,
  createAppointment,
  getUserAppointmentOrders
}
