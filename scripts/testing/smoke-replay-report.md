# Smoke Replay Report

- Generated at: 2026-04-02T10:40:37.795005Z
- Total requests: **22**

| Checklist | Command | HTTP | Result | Notes |
|---|---|---:|---|---|
| c1-c2-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=3'` | 000 | WARN |  |
| c1-c2-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=5'` | 000 | WARN |  |
| c1-c2-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/front/patient/attendant/getPatientList'` | 000 | WARN |  |
| c1-c2-api-checklist.md | `curl -s -H "token: DUMMY_PATIENT_TOKEN" 'http://127.0.0.1:8080/treat/front/patient/atte...` | 000 | WARN |  |
| c1-c2-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/attendant/addPatientAttendan...` | 000 | WARN | /bin/sh: 1: Syntax error: Unterminated quoted string |
| c3-c4-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/front/patient/schedule/findList?subDepartmentId=1&...` | 000 | WARN |  |
| c3-c4-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/appointment/create' -H "toke...` | 000 | WARN |  |
| c3-c4-api-checklist.md | `curl -s "http://127.0.0.1:8080/treat/front/patient/appointment/status?token=DUMMY_APPOI...` | 000 | WARN |  |
| c5-c6-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/chat/initiate-consultation' -H "token: DUM...` | 000 | WARN |  |
| c5-c6-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/chat/respond-consultation' -H "token: DUMM...` | 000 | WARN |  |
| c5-c6-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/chat/message' -H "token: DUMMY_PATIENT_TOK...` | 000 | WARN |  |
| c5-c6-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/chat/message' -H "token: DUMMY_DOCTOR_TOKE...` | 000 | WARN |  |
| c5-c6-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/chat/messages/189'` | 000 | WARN |  |
| c7-c8-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/medicalRecord/addMedicalRecor...` | 000 | WARN | /bin/sh: 1: Syntax error: Unterminated quoted string |
| c7-c8-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/medicalRecord/getMedicalRecor...` | 000 | WARN |  |
| c7-c8-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/patient/medicalRecord/getMedicalReco...` | 000 | WARN |  |
| c7-c8-api-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/front/doctor/prescription/addPrescription'...` | 000 | WARN | /bin/sh: 1: Syntax error: Unterminated quoted string |
| c7-c8-api-checklist.md | `curl -s 'http://127.0.0.1:8080/treat/front/patient/medicalRecord/getPrescriptionInfoByM...` | 000 | WARN |  |
| c9-c10-d-e-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/ai/doctor/assist' -H "token: DUMMY_DOCTOR_...` | 000 | WARN |  |
| c9-c10-d-e-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/ai/patient/triage' -H 'Content-Type: appli...` | 000 | WARN |  |
| c9-c10-d-e-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/back/admin/drug/getDrugList?pageNum=1&page...` | 000 | WARN |  |
| c9-c10-d-e-checklist.md | `curl -s -X POST 'http://127.0.0.1:8080/treat/back/admin/doctor/findAll?pageNum=1&pageSi...` | 000 | WARN |  |