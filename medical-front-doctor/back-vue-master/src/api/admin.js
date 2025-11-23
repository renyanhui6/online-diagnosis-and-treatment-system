import api from './index';

// 获取仪表盘数据
export function getDashboardData() {
  return api.get('/admin/dashboard');
}

// 获取预约统计数据
export function getAppointmentStatistics(params) {
  return api.get('/admin/statistics/appointments', { params });
}

// 获取问诊统计数据
export function getConsultationStatistics(params) {
  return api.get('/admin/statistics/consultations', { params });
}

// 获取收入统计数据
export function getIncomeStatistics(params) {
  return api.get('/admin/statistics/income', { params });
}

// 获取医生工作量统计数据
export function getDoctorStatistics(params) {
  return api.get('/admin/statistics/doctors', { params });
}