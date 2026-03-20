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

// 根据病历ID获取处方详情
export function getPrescriptionInfoByMedicalRecordId(medicalRecordId) {
  return request({
    url: '/front/patient/medicalRecord/getPrescriptionInfoByMedicalRecordId',
    method: 'get',
    params: {
      medicalRecordId
    }
  })
}
