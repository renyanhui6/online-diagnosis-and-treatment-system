import request from './request'

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

// 获取排班列表
export function getScheduleList(params = {}) {
  return request({
    url: '/front/patient/schedule/findList',
    method: 'get',
    params
  })
}

// 创建预约挂号
export function createAppointment(data) {
  return request({
    url: '/front/patient/appointment/create',
    method: 'post',
    data: {
      patientId: data.patientId,
      scheduleId: data.scheduleId
    }
  })
}

// 查询预约创建状态
export function getAppointmentStatus(token) {
  return request({
    url: '/front/patient/appointment/status',
    method: 'get',
    params: {
      token
    }
  })
}

// 获取支付宝支付表单
export function getAppointmentPaymentForm(registrationId) {
  return request({
    url: '/front/patient/appointment/payment/form',
    method: 'get',
    params: {
      registrationId
    }
  })
}

// 查询支付结果
export function getAppointmentPaymentResult(outTradeNo) {
  return request({
    url: '/front/patient/appointment/payment/result',
    method: 'get',
    params: {
      outTradeNo
    }
  })
}

// 模拟支付成功
export function mockAppointmentPaymentSuccess(outTradeNo) {
  return request({
    url: '/front/patient/appointment/payment/mock/success',
    method: 'post',
    params: {
      outTradeNo
    }
  })
}

// 模拟取消支付
export function mockAppointmentPaymentCancel(outTradeNo) {
  return request({
    url: '/front/patient/appointment/payment/mock/cancel',
    method: 'post',
    params: {
      outTradeNo
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
  getAppointmentStatus,
  getAppointmentPaymentForm,
  getAppointmentPaymentResult,
  mockAppointmentPaymentSuccess,
  mockAppointmentPaymentCancel,
  getUserAppointmentOrders
}
