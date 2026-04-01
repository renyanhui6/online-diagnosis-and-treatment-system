import api from './index';

// 医生端 AI 问诊协作 Agent
export function getDoctorAssist(payload) {
  return api.post('/ai/doctor/assist', payload);
}
