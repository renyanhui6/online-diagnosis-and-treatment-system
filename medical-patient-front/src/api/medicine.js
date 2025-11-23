import request from './request'

// 获取药品列表
export function getDrugList(params) {
  const requestParams = {
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10
  }
  
  // 如果有分类ID且不是'all'，添加到请求参数
  if (params.categoryId && params.categoryId !== 'all') {
    requestParams.categoryId = params.categoryId
  }
  
  return request({
    url: '/back/admin/drug/getDrugList',
    method: 'post',
    params: requestParams
  })
}

// 搜索药品列表
export function searchDrugList(params) {
  const requestParams = {
    pageNum: params.page || 1,
    pageSize: params.pageSize || 10,
    search: params.keyword || ''
  }
  
  // 如果有分类ID且不是'all'，添加到请求参数
  if (params.categoryId && params.categoryId !== 'all') {
    requestParams.categoryId = params.categoryId
  }
  
  return request({
    url: '/back/admin/drug/searchDrugList',
    method: 'post',
    params: requestParams
  })
}

export function createByPrescription(consultationRecordId) {
  return request({
    url: '/front/patient/medicine/createByPrescription',
    method: 'post',
    // 对于@RequestParam参数，需要用params传递（查询参数）
    params: {
      id: consultationRecordId  // 后端接口要求的参数名为"id"
    }
  })
}


// 通过购物车创建药品订单 - 使用正确的API端点
export function createOrder(cartItems) {
  return request({
    url: '/front/patient/medicine/createOrder',
    method: 'post',
    data: cartItems 
  })
}

// 完成药品订单支付
export function completeMedicineOrderPayment(data) {
  return request({
    url: '/front/patient/medicine/completeOrder',
    method: 'post',
    data: {
      id: data.id,
      paymentStatus: data.paymentStatus,
      paymentTime: data.paymentTime,
      paymentMethod: data.paymentMethod,
      paymentGateway: data.paymentGateway
    }
  })
}

// 取消药品订单（未付款时）
export function cancelMedicineOrder(id) {
  return request({
    url: '/front/patient/medicine/cancel',
    method: 'post',
    params: {
      id: id
    }
  })
}

// 药品订单退款（支付后）
export function refundMedicineOrder(id) {
  return request({
    url: '/front/patient/medicine/refund',
    method: 'post',
    params: {
      id: id
    }
  })
}

// 药品订单核销（已支付状态时可核销）
export function verifyMedicineOrder(id) {
  return request({
    url: '/front/patient/medicine/vertify',
    method: 'post',
    params: {
      id: id
    }
  })
}

// 获取药品订单列表
export function getMedicineOrderList(params) {
  return request({
    url: '/front/patient/medicine/getOrders',
    method: 'get',
    params: {
      pageNum: params.pageNum,
      pageSize: params.pageSize,
      createDate: params.createDate
    }
  })
}

// 获取药品订单详情
export function getMedicineOrderDetail(id, pageNum = 1, pageSize = 10) {
  return request({
    url: `/front/patient/medicine/getDrugs`,
    method: 'get',
    params:{
     id: id,
     pageNum: pageNum,
     pageSize: pageSize
    }
  })
}

// 获取药品订单列表（别名）
export function getDrugOrderList(params) {
  return getMedicineOrderList(params)
}

// 获取药品订单详情（别名）
export function getDrugOrderDetail(id) {
  return getMedicineOrderDetail(id)
}

// 支付药品订单（别名）
export function payDrugOrder(id, data) {
  return completeMedicineOrderPayment({ id, ...data })
}

// 支付药品订单（别名）
export function payMedicineOrder(id, data) {
  return completeMedicineOrderPayment({ id, ...data })
}

// 申请退款（别名）
export function refundDrugOrder(id, data) {
  return refundMedicineOrder(id)
}

// 支付订单（通用，别名）
export function payOrder(id, data) {
  return completeMedicineOrderPayment({ id, ...data })
}

// 保留旧的API函数名以保持兼容性
export function createMedicineOrderByPrescription(consultationRecordId) {
  return createByPrescription(consultationRecordId)
}

export function createMedicineOrderByCart(cartItems) {
  return createOrder(cartItems)
}