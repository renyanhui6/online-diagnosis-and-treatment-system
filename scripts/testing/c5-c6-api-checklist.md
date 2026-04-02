# C5/C6 接口执行清单（问诊房间与聊天）

> 前置：`PATIENT_TOKEN`、`DOCTOR_TOKEN`、有效 `registrationId`。

## C5 问诊房间流程

### C5-01 医生发起问诊
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/chat/initiate-consultation' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"registrationId":105}'
```
断言：`code=200`，返回 `roomId`。

### C5-02 患者接诊
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/chat/respond-consultation' \
  -H "token: ${PATIENT_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"roomId":189,"accept":true}'
```
断言：`code=200`，房间状态进入问诊中。

### C5-03 患者拒绝分支
- `accept=false` 再测一轮。
- 断言：返回拒绝态，挂号状态同步更新。

## C6 聊天消息

### C6-01 患者发送消息
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/chat/message' \
  -H "token: ${PATIENT_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"roomId":189,"messageType":1,"content":"医生您好，我今天胸闷。"}'
```
断言：`code=200`，消息写入成功。

### C6-02 医生发送消息
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/chat/message' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"roomId":189,"messageType":1,"content":"请补充持续时间与伴随症状。"}'
```
断言：`code=200`。

### C6-03 查询房间状态
```bash
curl -s 'http://127.0.0.1:8080/treat/chat/messages/{roomId}'
```
断言：返回消息数组或空数组，接口可用（执行时将 `{roomId}` 替换成真实值）。
