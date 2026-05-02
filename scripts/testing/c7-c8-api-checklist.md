# C7/C8 接口执行清单（病历与处方）

> 前置：已存在有效 `roomId`、`registrationId`，并准备好医生 token。

## C7 病历

### C7-01 新增病历
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/medicalRecord/addMedicalRecord' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "registrationId":105,
    "roomId":189,
    "chiefComplaint":"胸闷伴心慌1天",
    "presentIllness":"活动后加重",
    "diagnosis":"待排心律失常"
  }'
```
断言：`code=200`，返回 `medicalRecordId`。

### C7-02 医生端病历列表
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/medicalRecord/getMedicalRecordList?pageNum=1&pageSize=10' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}'
```
断言：`code=200`，`records` 中包含新增病历。

### C7-03 患者端病历列表
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/medicalRecord/getMedicalRecordByUserId?pageNum=1&pageSize=10' \
  -H "token: ${PATIENT_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}'
```
断言：`code=200`，患者可见病历。

## C8 处方

### C8-01 新增处方
```bash
curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/prescription/addPrescription' \
  -H "token: ${DOCTOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{
    "medicalRecordId":58,
    "prescriptions":[
      {"drugId":1,"drugNum":1,"drugUsage":"口服"},
      {"drugId":2,"drugNum":1,"drugUsage":"口服"}
    ]
  }'
```
断言：`code=200`。

### C8-02 患者查询处方详情
```bash
curl -s 'http://127.0.0.1:8080/treat/front/patient/medicalRecord/getPrescriptionInfoByMedicalRecordId?medicalRecordId=58' \
  -H "token: ${PATIENT_TOKEN}"
```
断言：`code=200`，可查到处方明细。
