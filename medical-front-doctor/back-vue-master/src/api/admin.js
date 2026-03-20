import api from './index';

export function getScheduleTemplates(params) {
  return api.get('/back/admin/scheduleTemplate/findByPage', { params });
}

export function addScheduleTemplate(data) {
  return api.post('/back/admin/scheduleTemplate/add', data);
}

export function updateScheduleTemplate(data) {
  return api.post('/back/admin/scheduleTemplate/update', data);
}

export function removeScheduleTemplate(templateId) {
  return api.get('/back/admin/scheduleTemplate/remove', {
    params: { templateId }
  });
}

export function getDoctorDetailList() {
  return api.get('/back/admin/doctor/detailList');
}

export function generateSchedules(days, mode = 'fill_missing') {
  return api.post('/back/admin/schedule/generate', null, {
    params: {
      days,
      mode
    }
  });
}

export function getDepartments() {
  return api.get('/back/admin/department/findList');
}

export function getSubDepartments(departmentId) {
  return api.get('/back/admin/department/findSubList', {
    params: { departmentId }
  });
}

export function addDepartment(data) {
  return api.post('/back/admin/department/add', data);
}

export function addSubDepartment(formData) {
  return api.post('/back/admin/department/addSub', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

export function removeDepartment(departmentId) {
  return api.get('/back/admin/department/remove', {
    params: { departmentId }
  });
}

export function removeSubDepartment(subDepartmentId) {
  return api.get('/back/admin/department/removeSub', {
    params: { subDepartmentId }
  });
}

export function getDoctorList(params, data) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : '';
  return api.post(`/back/admin/doctor/findAll${query}`, data || {});
}

export function getDoctorDetailByUserId(id) {
  return api.get('/back/admin/doctor/findById', { params: { id } });
}

export function createDoctor(data) {
  return api.post('/back/admin/doctor/create', data);
}

export function updateDoctorDetail(data) {
  return api.post('/back/admin/doctor/updateDetail', data);
}

export function removeDoctor(id) {
  return api.delete('/back/admin/doctor/removeById', { params: { id } });
}

export function updateDoctorStatus(id, status) {
  return api.put('/back/admin/doctor/modifyStatusById', null, {
    params: { id, status }
  });
}

export function getPatientList(params, data) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : '';
  return api.post(`/back/admin/patient/findAll${query}`, data || {});
}

export function getPatientDetailByUserId(id) {
  return api.get('/back/admin/patient/findById', { params: { id } });
}

export function createPatient(data) {
  return api.post('/back/admin/patient/create', data);
}

export function updatePatientDetail(data) {
  return api.post('/back/admin/patient/updateDetail', data);
}

export function removePatient(id) {
  return api.delete('/back/admin/patient/removeById', { params: { id } });
}

export function updatePatientStatus(id, status) {
  return api.put('/back/admin/patient/modifyStatusById', null, {
    params: { id, status }
  });
}

export function getDrugList(params) {
  const query = params ? `?${new URLSearchParams(params).toString()}` : '';
  return api.post(`/back/admin/drug/getDrugList${query}`);
}

export function searchDrugList(params, search) {
  const merged = { ...(params || {}), search: search || '' };
  const query = `?${new URLSearchParams(merged).toString()}`;
  return api.post(`/back/admin/drug/searchDrugList${query}`);
}

export function addDrug(data) {
  return api.post('/back/admin/drug/addDrug', data);
}

export function updateDrug(data) {
  return api.post('/back/admin/drug/modifyDrug', data);
}

export function deleteDrug(id) {
  return api.post('/back/admin/drug/deleteDrug', null, {
    params: { id }
  });
}

export function getDrugDetail(id) {
  return api.post('/back/admin/drug/getDrug', null, {
    params: { id }
  });
}
