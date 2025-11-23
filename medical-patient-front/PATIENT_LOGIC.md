# 患者端逻辑说明

## 概述

根据新的Java后端实现，患者端的逻辑已经进行了优化，实现了以下功能：

1. **患者端登录成功后维持长连接**：等待医生发起问诊请求
2. **医生选择接诊时创建房间**：并发起通知提醒患者
3. **患者收到弹窗选择**：可以选择接收或拒绝问诊
4. **超时或拒绝处理**：预约记录变为挂起状态
5. **恢复功能**：提供操作将挂起状态变为已回归状态

## 主要修改

### 1. WebSocket服务优化 (`src/utils/websocket.js`)

- 适配新的后端WebSocket路径：`/common/ws/chat/{roomId}`
- 优化消息格式以匹配后端期望的格式
- 增强患者端长连接逻辑
- 新增用户ID解析功能

### 2. API接口更新 (`src/api/chat.js`)

- 更新为新的后端API路径：`/chat/*`
- 新增 `initiateConsultation()` - 医生发起问诊
- 新增 `respondToConsultation()` - 患者响应问诊
- 新增 `resumeConsultation()` - 恢复挂起的问诊
- 更新 `getChatMessages()` 和 `getRoomStatus()` 接口

### 3. 预约状态更新 (`src/utils/index.js`)

- 更新预约状态常量以匹配后端：
  - **0 - 待支付**
  - **1 - 已支付**
  - **2 - 排队中**
  - **3 - 问诊中**
  - **4 - 已完成**
  - **5 - 暂时挂起**
  - **6 - 已回归**
  - **7 - 等待患者确认**

### 4. 通知组件优化 (`src/components/ConsultationNotification.vue`)

- 使用新的后端API进行问诊响应
- 优化错误处理和用户反馈

### 5. 预约列表页面更新 (`src/views/appointment/list.vue`)

- 使用新的恢复问诊API
- 更新状态显示逻辑

### 6. 聊天页面优化 (`src/views/appointment/chat.vue`)

- 适配新的房间状态API
- 更新聊天记录获取逻辑
- 优化消息发送格式

## 工作流程

### 1. 患者登录
```
患者登录成功 → 建立WebSocket长连接 → 等待医生问诊请求
```

### 2. 医生发起问诊
```
医生选择患者 → 调用 /chat/initiate API → 创建房间 → 发送WebSocket通知 → 患者收到弹窗
```

### 3. 患者响应
```
患者收到通知 → 选择同意/拒绝 → 调用 /chat/respond API → 更新预约状态
```

### 4. 超时处理
```
3分钟内无响应 → 后端自动处理 → 预约状态变为暂时挂起 → 患者可手动恢复
```

### 5. 恢复问诊
```
患者点击恢复 → 调用 /chat/resume API → 状态变为已回归
```

## 技术实现

### WebSocket连接
- 聊天室连接：`ws://host/treat/common/ws/chat/{roomId}?token=xxx`
- 患者端专用连接：`ws://host/treat/common/ws/chat/patient_{userId}?token=xxx`

### API接口
```javascript
// 医生发起问诊
POST /chat/initiate
{
  "registrationId": "xxx",
  "doctorId": "xxx",
  "patientId": "xxx",
  "patientName": "xxx"
}

// 患者响应问诊
POST /chat/respond
{
  "registrationId": "xxx",
  "response": "accept" | "reject"
}

// 恢复问诊
POST /chat/resume
{
  "registrationId": "xxx"
}
```

### 消息格式
```javascript
// 问诊请求通知
{
  type: 'consultation_request',
  roomId: 'xxx',
  doctorId: 'xxx',
  patientId: 'xxx',
  registrationId: 'xxx'
}

// 患者响应通知
{
  type: 'patient_accepted' | 'patient_rejected' | 'patient_timeout',
  registrationId: 'xxx'
}

// 聊天消息
{
  type: 'chat',
  roomId: 'xxx',
  senderId: 'xxx',
  messageType: 1 | 2, // 1-文本, 2-图片
  content: 'xxx'
}
```

## 状态流转

```
待支付(0) → 已支付(1) → 排队中(2) → 等待患者确认(7) → 问诊中(3) → 已完成(4)
                                    ↓
                              暂时挂起(5) → 已回归(6)
```

## 注意事项

1. **WebSocket连接维护**：患者端长连接会在连接断开时自动重连
2. **状态同步**：预约状态变更会实时同步到前端
3. **错误处理**：网络异常时会自动重试，确保连接稳定性
4. **超时处理**：后端自动处理3分钟超时，无需前端干预
5. **API兼容性**：所有API调用都使用新的后端接口

## 后续开发

1. 完善WebSocket消息处理逻辑
2. 添加更多的通知类型
3. 优化用户体验和界面设计
4. 添加更多的预约状态管理功能
5. 实现更复杂的聊天功能（图片、文件等） 