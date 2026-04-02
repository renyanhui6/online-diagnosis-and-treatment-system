# C3/C4 接口执行清单（排班与挂号）

> 前置：已有 `${PATIENT_TOKEN}`。

## C3 排班查询

### C3-01 查询排班
```bash
curl -s 'http://127.0.0.1:8080/treat/front/patient/schedule/findList?subDepartmentId=1&scheduleDate=2026-04-01'
```
断言：`code=200`，返回至少一个 `scheduleId`。

## C4 挂号创建与状态

### C4-01 创建挂号
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/appointment/create' \
  -H "token: ${PATIENT_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"scheduleId":2746,"patientId":1}'
```
断言：返回 `token`，初始状态为 `PROCESSING` 或 `PAYING`。

### C4-02 轮询挂号状态
```bash
curl -s "http://127.0.0.1:8080/treat/front/patient/appointment/status?token=${APPOINT_TOKEN}" \
  -H "token: ${PATIENT_TOKEN}"
```
断言：状态可收敛为 `PAYING/SUCCESS/FAILED` 之一。

### C4-03 重复挂号拦截
- 同一个 `scheduleId + patientId` 再次创建。
- 断言：返回重复拦截错误。

### C4-04 库存不足异常
- 对无余号排班发起创建。
- 断言：返回库存不足错误。
