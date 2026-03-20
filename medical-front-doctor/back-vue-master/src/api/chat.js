import api from './index';

// 获取排队中的患者列表
export function getWaitingPatients(params) {
  return api.get('/chat/waiting-patients', { params });
}

// 发起问诊请求
export function initiateConsultation(data) {
  return api.post('/chat/initiate-consultation', data);
}

// 获取聊天室信息（通过预约ID）
export function getChatRoom(registrationId) {
  return api.get(`/chat/room/${registrationId}`);
}

// 获取聊天室信息（通过房间ID）
export function getChatRoomById(roomId) {
  return api.get(`/chat/room-by-id/${roomId}`);
}


// 获取聊天消息列表
export function getChatMessages(roomId, params) {
  return api.get(`/chat/messages/${roomId}`, { params });
}

// 发送聊天消息
export function sendChatMessage(data) {
  return api.post('/chat/message', data);
}

// 上传聊天图片
export function uploadChatImage(formData) {
  return api.post('/chat/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

// 更新聊天室状态
export function updateRoomStatus(roomId, status) {
  return api.put(`/chat/room/${roomId}/status`, { status });
}

// 结束问诊
export function endConsultation(roomId) {
  return api.post(`/chat/room/${roomId}/end`);
}

 
