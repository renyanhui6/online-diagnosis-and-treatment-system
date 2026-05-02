import request from './request'

// 查询 AI 后端运行状态
export function getAiStatus() {
  return request({
    url: '/ai/status',
    method: 'get'
  })
}

// 患者端 AI 科室推荐（占位接口）
export function getTriageSuggestion(data) {
  return request({
    url: '/ai/patient/triage',
    method: 'post',
    data
  })
}

// 患者端 AI 多轮分诊客服
export function chatTriageAgent(data) {
  return request({
    url: '/ai/patient/triage/chat',
    method: 'post',
    data
  })
}

// 创建患者端 AI 分诊会话
export function startTriageSession(data = {}) {
  return request({
    url: '/ai/patient/triage/session/start',
    method: 'post',
    data
  })
}

// 关闭患者端 AI 分诊会话
export function closeTriageSession(sessionId) {
  return request({
    url: '/ai/patient/triage/session/close',
    method: 'post',
    params: {
      sessionId
    }
  })
}

// 查询患者端 AI 分诊历史会话
export function getTriageSessionList() {
  return request({
    url: '/ai/patient/triage/session/list',
    method: 'get'
  })
}

// 查询某个 AI 分诊会话的消息
export function getTriageSessionMessages(sessionId) {
  return request({
    url: '/ai/patient/triage/session/messages',
    method: 'get',
    params: {
      sessionId
    }
  })
}
