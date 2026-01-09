# 改造计划（Netty 聊天 & AI 辅助）

## 目标
- 将问诊/聊天长连接从 Spring WebSocket 迁移到 Netty，提升并发、可控性与可扩展性。
- 为医生聊天端提供 AI 辅助工具（基于当前会话/病情摘要给出建议），为患者挂号前提供 AI 科室推荐。
- 在不破坏现有业务（挂号、订单、支付、排班等）的前提下分阶段上线，保证可回滚。

## 现状简述
- 后端：Spring Boot 3.4，`/common/ws/chat/{roomId}` 使用 `@ServerEndpoint`；房间/长连接管理在 `ChatWebSocket`，消息持久化由 `ChatMessageService`；房间状态/问诊流程由 `ChatController`、`RoomService` 等控制。
- 前端：医生/管理员端（`medical-front-doctor`）与患者端（`medical-patient-front`）直接连接上述 WebSocket，并调用 REST API 完成发起/响应问诊、获取房间、发送消息、上传图片等。
- 认证：JWT + Redis 校验，拦截器验证 `access-key`；房间和长连接依赖 token 解析。

## 方案概览
1) **Netty 聊天替换（分阶段并存）**
   - 新增 Netty WebSocket 服务，支持房间聊天、医生/患者个人长连接（`patient_{id}` / `doctor_{id}`）和心跳。
   - 兼容现有消息模型：类型（chat/consultation_request/room_status_update 等）、房间/用户字段、持久化调用 `ChatMessageService`/`RoomService`。
   - 认证：握手时校验 JWT（与现有 `JwtUtil`/Redis token 一致），拒绝非法连接；心跳+超时剔除。
   - 迁移策略：先上线新端点（如 `/netty/ws/chat/{roomId}`），前后端可灰度切换；稳定后下线旧 `@ServerEndpoint`。
2) **医生端 AI 辅助**
   - 后端新增 `AiService` 抽象与实现（可对接内网大模型/HTTP 服务），提供 `POST /ai/doctor/assist`，输入：房间ID/上下文消息或病情摘要；输出：建议/问诊提纲/用药参考（标记“AI 生成，非诊疗结论”）。
   - 前端医生聊天界面新增“AI 辅助”入口，支持一键提取当前对话片段发送给 AI，结果以提示卡片形式展示，不自动发送给患者。
   - 安全：截断敏感信息、限流、超时降级、记录调用日志。
3) **患者挂号前 AI 科室推荐**
   - 后端新增 `POST /ai/patient/triage`，输入：主诉描述（必填）+ 可选症状/年龄/性别；输出：推荐科室列表、置信度、说明。
   - 前端在挂号科室选择前提供“AI 推荐科室”入口，提示仅供参考，用户可覆盖选择。
   - 安全/合规同上，限制字段、脱敏/截断，失败时优雅降级。

## 详细实施步骤
### A. Netty 聊天
1. **基础框架与配置**
   - 引入 Netty 依赖，新增配置类（端口、路径、线程池、SSL 可选）。
   - 定义握手/认证处理器：解析 `token` 参数 -> `JwtUtil` 校验 -> Redis 一致性检查。
2. **连接与房间管理**
   - 设计 `SessionContext`：维护 channel 与 userId/role/roomId 映射、患者/医生长连接、房间内 channel 集合。
   - 实现心跳（ping/pong）、空闲检测、自动清理。
3. **消息路由与持久化**
   - 定义统一消息模型（type/roomId/senderId/senderType/messageType/content/timestamp）。
   - 路由规则：房间广播、单播通知（patient/doctor long connection）、房间状态更新；持久化走 `ChatMessageService`、房间状态走 `RoomService`。
4. **兼容与灰度**
   - 新增端点 `/netty/ws/chat/{roomId}`；保留旧端点作为回退。
   - 前端新增可配置的 WS 基础路径（环境变量/配置开关）以便切换。
   - 压测/对齐：消息格式、重连、超时定时器（患者响应超时）在 Netty 侧实现。
5. **下线旧实现**
   - 稳定后移除/封印 `@ServerEndpoint`，清理重复逻辑，保留 REST API 不变。

### B. 医生端 AI 辅助
1. **后端接口**
   - 新建 `AiController`（或模块化 `ai/doctor`），方法 `assist(DoctorAiRequest)`：校验医生身份、房间可见性、截断上下文，调用 `AiService`。
   - `AiService` 接口 + 默认实现（占位/可配置模型 URL），支持超时、错误码、审计日志。
2. **前端集成（医生端）**
   - 在 `DoctorChat` 页面添加“AI 辅助”按钮/侧边栏，选择“提炼要点/建议话术/用药提示”等模式。
   - 调用新 API 展示结果（不可直接发送给患者，需医生确认后手动发送）。
3. **安全/治理**
   - 明示免责声明；限制请求频率；审计调用数据（不落地患者隐私全文，可存摘要/指纹）。

### C. 患者挂号前 AI 推荐
1. **后端接口**
   - `AiController.triage(TriageRequest)`：校验字段、调用 `AiService`，返回推荐科室及理由。
2. **前端集成（患者端）**
   - 挂号入口/科室选择页增加“AI 推荐”弹窗：输入症状描述 -> 调用接口 -> 展示推荐及置信度，附提示“仅供参考”。
3. **安全/治理**
   - 字段长度限制、脱敏；限流；失败回退到手动选择。

## 交付与验证
- 文档：更新 README/开发文档，描述 Netty WS 路径、AI 接口规范、前端配置开关。
- 测试：单元测试（消息路由、JWT 校验）、集成测试（WS 连接、房间广播、AI 接口回退）、前端 E2E（问诊流、AI 按钮、科室推荐）。
- 灰度：先在测试环境切 Netty 端点与 AI 接口，观察日志/资源占用，再在生产逐步放量。

## 风险与缓解
- **Netty 迁移复杂度**：阶段并存 + 开关切换；保留回退端点。
- **AI 可用性/合规**：限流/超时/降级，前端明确提示非诊疗，日志审计。
- **兼容性**：确保消息格式与前端现有逻辑一致，必要时在前端做向后兼容的解析。

## 下一步（建议的实施顺序）
1. 落地 Netty 框架骨架 + 认证/心跳 + 房间管理最小可用；前端增加 WS 开关；联调基本聊天。
2. 补齐 Netty 与业务的持久化/房间状态/超时逻辑；压测并灰度。
3. 实现医生端 AI 辅助接口与前端入口；添加开关与免责声明。
4. 实现患者科室推荐接口与前端入口；验证挂号流程不受影响。
5. 清理旧 WS 实现、完善文档与测试。
