import api from './index';

// 获取问诊记录列表
export function getRegistrationList() {
  return api.post('/front/doctor/registration/getRegistrationList');
}

// 获取全部问诊记录列表
export function getAllRegistrationInfoList(params) {
  // 构建查询字符串
  const queryString = params ? `?${new URLSearchParams(params).toString()}` : '';
  return api.post(`/front/doctor/registration/getAllRegistrationinfoList${queryString}`);
}

// 根据挂号ID获取单条挂号信息
export function getRegistrationById(registrationId) {
  return api.get('/front/doctor/registration/getRegistrationById', { params: { registrationId } });
}

// 获取就诊记录列表
export function getMedicalRecordList(params) {
  // 构建查询字符串
  const queryString = params ? `?${new URLSearchParams(params).toString()}` : '';
  return api.post(`/front/doctor/medicalRecord/getMedicalRecordList${queryString}`);
}

// 获取处方信息
export function getPrescriptionInfoByMedicalRecordId(medicalRecordId) {
  return api.get(`/front/doctor/medicalRecord/getPrescriptionInfoByMedicalRecordId?medicalRecordId=${medicalRecordId}`);
}

// 添加就诊记录
export function addMedicalRecord(data) {
  return api.post('/front/doctor/medicalRecord/addMedicalRecord', data);
}

// 获取所有药品
export function getAllDrugs() {
  return api.get('/front/doctor/drug/getAllDrug');
}

// 添加处方
export function addPrescription(medicines, medicalRecordId) {
  // 确保 medicines 是数组格式
  if (!Array.isArray(medicines)) {
    console.warn('medicines 不是数组格式，强制转换为数组');
    medicines = [medicines];
  }
  
  // 构建查询参数
  const params = { medicalRecordId };
  
  return api.post('/front/doctor/prescription/addPrescription', medicines, { params });
}

// 修改挂号状态为挂起（患者拒绝或超时）
export function changeStatusToSuspended(registrationId) {
  return api.post('/front/doctor/registration/changeStatusToSuspended', null, {
    params: { registrationId }
  });
}

// 修改挂号状态为问诊中（患者同意问诊）
export function changeStatusToInProgress(registrationId) {
  return api.post('/front/doctor/registration/changeStatusToInProgress', null, {
    params: { registrationId }
  });
}

// 修改挂号状态为已完成（结束问诊）
export function changeStatusToCompleted(registrationId) {
  return api.post('/front/doctor/registration/changeStatusToCompleted', null, {
    params: { registrationId }
  });
}
