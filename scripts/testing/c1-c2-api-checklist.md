# C1/C2 接口执行清单（登录与就诊人）

## C1 登录与鉴权

### C1-01 devToken 获取（患者）
```bash
curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=3'
```
断言：`code=200` 且 `data.token` 非空。

### C1-02 devToken 获取（医生）
```bash
curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=5'
```
断言：`code=200` 且 `data.token` 非空。

### C1-03 未携带 token 访问受限接口
```bash
curl -s 'http://127.0.0.1:8080/treat/front/patient/attendant/getPatientList'
```
断言：返回鉴权失败（401/业务失败码）。

## C2 就诊人管理

> 先替换 `${PATIENT_TOKEN}` 为 C1-01 获取到的 token。

### C2-01 查询就诊人列表
```bash
curl -s -H "token: ${PATIENT_TOKEN}" \
  'http://127.0.0.1:8080/treat/front/patient/attendant/getPatientList'
```
断言：`code=200`，`data` 为数组。

### C2-02 新增就诊人（样例）
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/attendant/addPatientAttendant' \
  -H "token: ${PATIENT_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "realName":"自动化测试样本A",
    "idCard":"360102199901011239"
  }'
```
断言：`code=200`，返回新增主键或成功标记。

### C2-03 重复身份证新增（异常分支）
- 使用与 C2-02 相同身份证再次新增。
- 断言：返回重复约束失败码或失败消息。
