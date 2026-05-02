# C9/C10 + D/E 执行清单（AI、管理端、一致性、集成）

## C9 AI 接口

### C9-01 医生 AI 协作
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/ai/doctor/assist' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"roomId":189,"registrationId":105,"summary":"胸闷心慌待排心律失常","conversationSnippet":"患者主诉胸闷1天","symptoms":["胸闷","心慌"]}'
```
断言：`code=200`，返回结构化建议。

### C9-02 患者 AI 分诊
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/ai/patient/triage' \
  -H 'Content-Type: application/json' \
  -d '{"description":"胸闷心慌1天","age":27,"gender":"男","symptoms":["胸闷","心慌"]}'
```
断言：`code=200`，返回推荐科室。

## C10 管理端基础 CRUD

### C10-01 药品列表
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/back/admin/drug/getDrugList?pageNum=1&pageSize=10' \
  -H "token: ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}'
```
断言：`code=200`。

### C10-02 医生列表
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/back/admin/doctor/findAll?pageNum=1&pageSize=10' \
  -H "token: ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}'
```
断言：`code=200`。

## D 一致性校验

1. 校验挂号状态与房间状态：`registration_status` 与 `room_status` 匹配。
2. 校验病历与处方关联：`medical_record.id` 能查到处方明细。
3. 校验异步收敛：挂号 token 最终进入成功/失败终态。

## E 三端集成回归

1. 患者端：登录 -> 挂号 -> 接诊 -> 查询病历处方。
2. 医生端：登录 -> 发起问诊 -> 聊天 -> 病历 -> 处方。
3. 管理端：登录 -> 药品/医生维护 -> 回查前台可见性。
