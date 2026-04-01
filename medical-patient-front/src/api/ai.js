import request from './request'

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
