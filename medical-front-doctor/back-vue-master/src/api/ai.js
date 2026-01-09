import api from './index';

// 医生端 AI 辅助（占位接口）
export function getDoctorAssist(payload) {
  return api.post('/ai/doctor/assist', payload);
}
