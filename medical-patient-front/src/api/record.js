import request from './request'

// 获取就诊记录列表
export function getMedicalRecordList(params) {
  return request({
    url: '/front/patient/medicalRecord/getMedicalRecordByUserId',
    method: 'post',
    params: {
      pageNum: params.pageNum || 1,
      pageSize: params.pageSize || 10
    },
    data: {
      createTime: params.createTime
    }
  })
}

// 获取就诊记录详情
export function getMedicalRecordDetail(id) {
  return request({
    url: `/front/patient/medicalRecord/getPrescriptionInfoByMedicalRecordId/${id}`,
    method: 'get'
  })
}

// 获取处方详情
export function getPrescriptionInfo(id) {
  return request({
    url: `/front/patient/prescription/getPrescriptionInfo/${id}`,
    method: 'get'
  })
}

// 获取处方详情 (别名)
export function getPrescriptionDetail(id) {
  return getPrescriptionInfo(id)
}

// 获取支付记录列表 (别名)
export function getPaymentRecordList(params) {
  return request({
    url: '/front/patient/payment/getPaymentRecordList',
    method: 'post',
    data: params
  })
}

// 获取支付记录详情 (别名)
export function getPaymentRecordDetail(id) {
  return request({
    url: `/front/patient/payment/getPaymentRecordDetail/${id}`,
    method: 'get'
  })
}

// 支付订单 (别名)
export function payOrder(data) {
  return request({
    url: '/front/patient/payment/payOrder',
    method: 'post',
    data
  })
}

// 退款订单 (别名)
export function refundOrder(data) {
  return request({
    url: '/front/patient/payment/refundOrder',
    method: 'post',
    data
  })
}