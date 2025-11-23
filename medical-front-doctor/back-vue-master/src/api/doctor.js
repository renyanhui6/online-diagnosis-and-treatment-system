import api from './index';

// 获取预约列表
export function getAppointments(params) {
  return api.get('/doctor/appointments', { params });
}

// 更新预约状态
export function updateAppointmentStatus(id, status, remark) {
  return api.put(`/doctor/appointments/${id}`, { status, remark });
}

// 获取问诊列表
export function getConsultations(params) {
  return api.get('/doctor/consultations', { params });
}

// 获取问诊详情
export function getConsultationDetail(id) {
  return api.get(`/doctor/consultations/${id}`);
}

// 发送问诊消息
export function sendConsultationMessage(id, message) {
  return api.post(`/doctor/consultations/${id}/messages`, message);
}

// 提交诊断结果
export function submitDiagnosis(id, diagnosis) {
  return api.post(`/doctor/consultations/${id}/diagnosis`, diagnosis);
}

// 创建处方
export function createPrescription(data) {
  return api.post('/doctor/prescriptions', data);
}

// 获取处方列表
export function getPrescriptions(params) {
  return api.get('/doctor/prescriptions', { params });
}

// 获取排班信息
export function getSchedule(params) {
  return api.get('/doctor/schedule', { params });
}

// 获取个人工作统计
export function getStatistics(params) {
  return api.get('/doctor/statistics', { params });
}

// 获取药品列表
export function getMedicines(params) {
  return api.get('/medicines', { params });
}

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

// 获取排班列表
export function getScheduleList() {
  return api.get('/front/doctor/schedule/getScheduleList');
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