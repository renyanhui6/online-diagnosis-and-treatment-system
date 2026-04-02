# 测试执行记录

> 说明：本文件用于按阶段记录真实测试执行情况。每完成一段测试，都要补充记录，禁止只写“通过/失败”而不写上下文。

---

## 记录模板

### 记录编号
- 日期：
- 阶段：
- 模块：
- 执行人：

### 环境信息
- 后端：
- 患者端：
- 医生/管理端：
- MySQL：
- Redis：
- RabbitMQ：
- MinIO：

### 测试数据
- 账号/用户：
- 医生：
- 患者：
- 排班/挂号/房间：
- 其他说明：

### 执行步骤
1. 
2. 
3. 

### 预期结果
- 

### 实际结果
- 

### 结论
- 是否通过：
- 问题编号：
- 是否需要回归：

### 证据
- 命令：
- 接口：
- 页面：
- 日志：

---

## 2026-03-31 / 阶段 0 / 测试计划建立

### 环境信息
- 当前仅完成测试计划和记录模板建立

### 执行步骤
1. 读取 `PLAN.md`、`start.md`、前 3 期文档，确认项目范围和运行依赖。
2. 盘点当前仓库已有测试文件与脚本。
3. 生成 `test-plan.md` 与本记录文件。

### 预期结果
- 形成可执行的测试总计划
- 形成统一测试记录模板

### 实际结果
- 已生成 [test-plan.md](/mnt/d/AAAA/medical/test-plan.md)
- 已生成 [test-execution-log.md](/mnt/d/AAAA/medical/test-execution-log.md)
- 已确认当前存在少量后端测试，但尚不成体系

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：否

### 证据
- 计划文件：`test-plan.md`
- 记录文件：`test-execution-log.md`

---

## 2026-03-31 / 阶段 A / 环境基线预检查（第一次）

### 环境信息
- 后端：未启动
- 患者端：未启动
- 医生/管理端：未启动
- MySQL：已启动
- Redis：未启动
- RabbitMQ：未启动
- MinIO：未启动

### 测试数据
- 数据库实例：`127.0.0.1:3306`
- 已发现数据库：`OnlineTreat`、`onlinetreat`

### 执行步骤
1. 检查关键端口监听情况。
2. 检查 Docker 容器运行情况。
3. 检查 MySQL 是否可连通。
4. 检查后端 AI 状态接口是否可访问。

### 预期结果
- 至少能确认当前测试环境中哪些依赖已就绪、哪些依赖缺失。

### 实际结果
- `3306` 端口正常监听，MySQL 可访问。
- `3000`、`5173`、`8080`、`6379`、`5672`、`15672`、`9000`、`9002` 均未发现监听。
- Docker 当前无运行中的容器。
- 访问 `http://127.0.0.1:8080/treat/ai/status` 失败，说明后端未启动。

### 结论
- 是否通过：部分通过
- 问题编号：ENV-001
- 是否需要回归：是

### 问题说明
- 当前环境不足以进入真实接口测试和集成测试阶段。
- 下一步需要先启动 Redis、RabbitMQ、MinIO、后端服务，随后再复做环境基线验证。

### 证据
- 端口检查：`ss -ltnp`
- 容器检查：`docker ps`
- 数据库检查：`mysql -h127.0.0.1 -P3306 -uroot -p123456 -e 'SHOW DATABASES;'`
- 后端接口检查：`curl http://127.0.0.1:8080/treat/ai/status`

---

## 2026-03-31 / 阶段 A / 环境基线复检（第二次）

### 环境信息
- 后端：已启动（`local` profile）
- 患者端：未启动
- 医生/管理端：未启动
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 数据库：`onlinetreat`
- 患者测试账号：`userId=3`
- 医生测试账号：`userId=5`

### 执行步骤
1. 启动 `medical-redis`、`medical-rabbit`、`medical-minio` 三个容器。
2. 以 `local` 配置启动后端服务。
3. 复检关键端口、AI 状态接口和 dev token 接口。

### 预期结果
- 真实测试依赖和后端均可用，满足进入单元测试和接口测试阶段的前提条件。

### 实际结果
- `3306`、`6379`、`5672`、`15672`、`9000`、`9002`、`8080` 均已监听。
- Docker 容器 `medical-redis`、`medical-rabbit`、`medical-minio` 均为 `Up` 状态。
- `GET /treat/ai/status` 返回 `code=200`，当前 `deepSeekEnabled=false`，说明在线模型未启用，本地回退链路可用。
- `GET /treat/front/loginAndOut/devToken?userId=3` 和 `userId=5` 均返回 `code=200` 和有效 token。

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：否

### 证据
- 端口检查：`ss -ltnp | rg ':(8080|6379|5672|15672|9000|9002|3306)\b'`
- 容器检查：`docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'`
- AI 状态：`curl -s http://127.0.0.1:8080/treat/ai/status`
- 患者 token：`curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=3'`
- 医生 token：`curl -s 'http://127.0.0.1:8080/treat/front/loginAndOut/devToken?userId=5'`

---

## 2026-03-31 / 阶段 B / 现有后端测试修复与回归（第一轮）

### 环境信息
- 后端：本地服务运行中（另有测试进程独立拉起 Spring 容器）
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 数据库：`onlinetreat`
- 患者：`userId=3`
- 医生：`userId=5`
- 既有处方病历：`medicalRecordId=52`

### 执行步骤
1. 执行现有测试集 `./mvnw test`，定位首轮失败原因。
2. 修复测试环境问题：新增 `src/test/resources/application.properties`，补齐 MySQL/Redis/RabbitMQ 测试配置，并关闭测试期的排班生成、挂号对账、预约过期扫描等后台任务。
3. 修复测试本身问题：把原来的打印型和破坏型测试改成断言型、安全型测试；对新增病历测试增加事务回滚；移除删除真实账号的行为。
4. 回归执行 `./mvnw -Dtest=DrugTest test` 验证修复方向。
5. 新增纯逻辑单元测试：
   - `PatientIdentityUtilTest`
   - `ScheduleTimePolicyTest`
   - `AppointmentReservationKeysTest`
   - `AppointmentReservationStateTest`
6. 执行新增测试子集，确认新增逻辑测试通过。
7. 重新执行全量 `./mvnw test`。

### 预期结果
- 现有后端测试在真实本地依赖环境下全部通过
- 测试不再依赖错误的默认数据源密码
- 测试不再对真实业务数据做破坏性删除

### 实际结果
- 首轮失败原因已确认：
  - `SpringBootTest` 默认读取 `application.yml`，测试期 MySQL 密码为空，导致 `Access denied for user 'root'@'localhost'`
  - 旧测试存在破坏性操作：`SystemUserTest` 直接删除 `userId=3`
  - 旧测试大量依赖 `System.out.println`，缺少有效断言
- 修复后，`DrugTest` 单独回归通过。
- 新增逻辑测试结果：
  - `Tests run: 16`
  - `Failures: 0`
  - `Errors: 0`
  - `Skipped: 0`
  - `BUILD SUCCESS`
- 全量回归结果：
  - `Tests run: 33`
  - `Failures: 0`
  - `Errors: 0`
  - `Skipped: 1`
  - `BUILD SUCCESS`

### 结论
- 是否通过：通过
- 问题编号：UT-001（已修复）
- 是否需要回归：后续新增单元测试后需再次全量回归

### 证据
- 首轮执行：`./mvnw test`
- 定向回归：`./mvnw -Dtest=DrugTest test`
- 全量回归：`./mvnw test`
- 修复文件：
  - [application.properties](/mnt/d/AAAA/medical/medical-back/src/test/resources/application.properties)
  - [DrugTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/DrugTest.java)
  - [MedicalRecordImplTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/MedicalRecordImplTest.java)
  - [RedisTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/RedisTest.java)
  - [RegistrationTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/RegistrationTest.java)
  - [SystemUserTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/SystemUserTest.java)
  - [PatientIdentityUtilTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/utils/PatientIdentityUtilTest.java)
  - [ScheduleTimePolicyTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/utils/ScheduleTimePolicyTest.java)
  - [AppointmentReservationKeysTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/registration/AppointmentReservationKeysTest.java)
  - [AppointmentReservationStateTest.java](/mnt/d/AAAA/medical/medical-back/src/test/java/cn/edu/ncu/medical/registration/AppointmentReservationStateTest.java)

---

## 2026-03-31 / 阶段 C / 登录、就诊人与基础排班接口测试（第一轮）

### 环境信息
- 后端：已启动（`local` profile）
- 患者端：未启动
- 医生/管理端：未启动
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 患者账号：`userId=3`
- 医生账号：`userId=5`
- 患者主就诊人：`patientAttendantId=1`
- 临时就诊人：`patientAttendantId=10`（测试后已删除）
- 可挂号排班样本日期：`2026-04-01`、`2026-04-02`、`2026-04-03`

### 执行步骤
1. 通过 `GET /front/loginAndOut/devToken?userId=3` 与 `userId=5` 获取患者、医生 token。
2. 用患者 token 调用 `GET /front/loginAndOut/getUserInfo`，验证登录态上下文。
3. 调用 `GET /front/patient/attendant/getPatientList` 和 `GET /front/patient/attendant/getInfo`，验证就诊人列表与主就诊人信息。
4. 生成一个临时合法身份证号，调用 `POST /front/patient/attendant/addPatientAttendant` 新增就诊人，再次查询列表确认数量加一。
5. 对同一身份证再次发起 `addPatientAttendant`，验证重复绑定被拦截。
6. 调用 `GET /front/patient/attendant/removePatientAttendant` 删除临时就诊人，再次查询列表确认数量回滚。
7. 调用 `POST /front/patient/attendant/updateInfo` 做一次等值更新，再用非法手机号做异常分支验证。
8. 调用 `GET /front/patient/schedule/findList` 查询当天与未来日期排班，确认 `canBook` 口径随日期变化正常。

### 预期结果
- dev token 和登录上下文可用。
- 就诊人列表、主就诊人信息返回正确。
- 新增临时就诊人后列表数量加一，删除后恢复原值。
- 重复就诊人和非法手机号均被正确拦截。
- 同日过期排班 `canBook=false`，未来排班 `canBook=true`。

### 实际结果
- `getUserInfo` 成功返回 `userId=3`、`username=user1`。
- 患者 `userId=3` 初始就诊人数为 `3`，新增临时就诊人后变为 `4`，删除后恢复为 `3`。
- 临时就诊人新增返回 `code=200`，重复新增返回 `code=202`、`message=参数不正确`。
- `updateInfo` 等值更新返回成功，非法手机号返回 `code=202`。
- `scheduleDate=2026-03-31` 的排班全部 `canBook=false`；`2026-04-01`、`2026-04-02`、`2026-04-03` 的未来排班返回 `canBook=true`。

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：后续前端集成时需抽样回归

### 证据
- token 与登录态：
  - `GET /treat/front/loginAndOut/devToken?userId=3`
  - `GET /treat/front/loginAndOut/getUserInfo`
- 就诊人接口：
  - `GET /treat/front/patient/attendant/getPatientList`
  - `GET /treat/front/patient/attendant/getInfo`
  - `POST /treat/front/patient/attendant/addPatientAttendant`
  - `GET /treat/front/patient/attendant/removePatientAttendant`
  - `POST /treat/front/patient/attendant/updateInfo`
- 排班接口：
  - `GET /treat/front/patient/schedule/findList?scheduleDate=2026-04-02`

---

## 2026-03-31 / 阶段 C / 挂号创建、支付状态与异常分支测试（第一轮）

### 环境信息
- 后端：已启动（`local` profile）
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 患者账号：`userId=3`
- 就诊人 A：`patientAttendantId=2`（取消支付链路）
- 就诊人 B：`patientAttendantId=3`（支付成功链路）
- 排班 A：`scheduleId=2773`
- 排班 B：`scheduleId=2774`
- 取消链路挂号：`registrationId=102`
- 成功链路挂号：`registrationId=103`
- 取消链路 token：`2773.db629ef0ae9b49acb44b855b7e844611`
- 成功链路 token：`2774.5f0f384f48834175b4099454c9ccfd64`

### 执行步骤
1. 以 `patientId=2, scheduleId=2773` 调用 `POST /front/patient/appointment/create`，随后轮询 `GET /front/patient/appointment/status`，等待从 `PROCESSING` 收敛到 `PAYING`。
2. 调用 `GET /front/patient/appointment/payment/form` 获取支付单信息，再调用 `POST /front/patient/appointment/payment/mock/cancel` 模拟取消，最后用 `GET /front/patient/appointment/payment/result` 验证结果。
3. 以 `patientId=3, scheduleId=2774` 再次走创建与状态轮询流程，等待进入 `PAYING`。
4. 在支付前对同一就诊人和同一排班再次调用 `create`，验证重复提交返回已有支付单状态，而不是重复创建。
5. 调用 `POST /front/patient/appointment/payment/mock/success` 模拟支付成功，再用 `GET /front/patient/appointment/payment/result` 验证最终状态。
6. 调用 `POST /front/patient/registration/getRegistrationInfoList`，验证患者侧挂号列表已回填取消与成功两条新记录。
7. 直接核对 `registration`、`registration_payment_order`、`registration_person_lock`、`schedule` 四张表，确认状态最终一致。
8. 补测异常分支：非法 `patientId`、非法 `scheduleId`、越权就诊人（`patientId=5`）三种情况。

### 预期结果
- 创建挂号先返回 `PROCESSING`，随后收敛到 `PAYING`。
- 模拟取消后，支付单变为 `CLOSED`，挂号状态变为 `8`，库存与锁记录回收。
- 模拟支付成功后，支付单变为 `PAID`，挂号状态变为 `1`，库存正式占用，锁记录保留。
- 重复提交不产生第二条挂号，而是返回已有支付中的同一单据。
- 非法或越权参数被正确拦截。

### 实际结果
- 取消链路：
  - 第一次轮询 `PROCESSING`，第二次轮询收敛为 `PAYING`
  - 生成挂号 `registrationId=102`
  - 生成支付单 `outTradeNo=REG10213292742B17E43B4`
  - 模拟取消后返回 `paymentStatus=CLOSED`、`registrationStatus=8`
  - 数据库中 `scheduleId=2773` 的 `current_appointment_count=0`
  - 对应 `registration_person_lock` 已删除
- 成功链路：
  - 第一次轮询即收敛为 `PAYING`
  - 生成挂号 `registrationId=103`
  - 生成支付单 `outTradeNo=REG1034ACE738A42254EE0`
  - 重复创建返回同一 token、同一挂号单和同一支付单状态
  - 模拟支付成功后返回 `paymentStatus=PAID`、`registrationStatus=1`
  - 数据库中 `scheduleId=2774` 的 `current_appointment_count=1`
  - 对应 `registration_person_lock` 保留 1 条
- 异常分支：
  - 非法 `patientId=999999` 返回 `code=719`
  - 非法 `scheduleId=999999` 返回 `code=708`
  - 越权就诊人 `patientId=5` 返回 `code=205`

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：后续前端挂号与支付页面联调时需复测

### 证据
- 创建与轮询：
  - `POST /treat/front/patient/appointment/create`
  - `GET /treat/front/patient/appointment/status`
- 支付链路：
  - `GET /treat/front/patient/appointment/payment/form`
  - `POST /treat/front/patient/appointment/payment/mock/cancel`
  - `POST /treat/front/patient/appointment/payment/mock/success`
  - `GET /treat/front/patient/appointment/payment/result`
- 挂号列表：
  - `POST /treat/front/patient/registration/getRegistrationInfoList`
- 数据库核对：
  - `registration`
  - `registration_payment_order`
  - `registration_person_lock`
  - `schedule`

---

## 2026-03-31 / 阶段 C / 问诊、AI、病历与处方链路测试（第一轮）

### 环境信息
- 后端：已启动（`local` profile）
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 患者账号：`userId=3`
- 医生账号：`userId=5`
- 已支付挂号：`registrationId=103`
- 房间：`roomId=187`
- 新增病历：`medicalRecordId=55`
- 新增处方明细：`prescriptionId=34,35`

### 执行步骤
1. 以医生账号调用 `POST /chat/initiate-consultation`，对已支付挂号 `registrationId=103` 发起问诊。
2. 通过 `GET /chat/room/103` 查询房间，确认房间创建成功且初始状态为等待患者确认。
3. 以患者账号调用 `POST /chat/respond-consultation`，响应 `accept`，再查询房间确认状态推进。
4. 患者、医生分别调用 `POST /chat/send-message` / `POST /chat/message` 发送文本消息，再用 `GET /chat/messages/{roomId}` 查询消息列表。
5. 以医生账号调用 `POST /ai/doctor/assist`，验证医生协作 Agent 在真实房间和挂号下返回结构化结果。
6. 调用 `POST /front/doctor/medicalRecord/addMedicalRecord` 新增病历。
7. 调用 `POST /front/doctor/drug/getDrugList` 获取药品，再调用 `POST /front/doctor/prescription/addPrescription` 为新病历开具两条处方。
8. 分别从医生端、患者端调用病历列表和处方明细接口，验证诊后结果可被两端回看。
9. 以医生账号调用 `POST /front/doctor/registration/changeStatusToCompleted`，将挂号状态推进为已完成。
10. 补测患者侧 AI：
  - `POST /ai/patient/triage`
  - `POST /ai/patient/triage/chat` 两轮对话

### 预期结果
- 医生可对自己的已支付挂号发起问诊，患者接受后房间状态和挂号状态同步推进。
- 聊天消息可以持久化并按房间拉取。
- 医生协作 Agent 能返回缺失项、风险提醒、追问建议和病历草稿等结构化结果。
- 病历和处方新增成功，处方状态正确回写到病历。
- 患者与医生都能查询到新增处方明细。
- 患者侧单轮分诊能直接给出导诊建议，多轮分诊能先追问再收敛推荐。

### 实际结果
- `initiate-consultation` 成功创建 `roomId=187`，房间初始 `roomStatus=1`。
- 患者接受问诊后，房间状态变为 `2`，最终挂号经医生手动完结后变为 `registrationStatus=4`。
- 房间 `187` 成功写入 2 条新聊天消息，接口查询返回顺序与数据库一致。
- 医生侧 AI 返回 `source=local-doctor-copilot`，命中：
  - `highRisk=true`
  - 缺失项列表
  - 追问建议
  - 风险提醒
  - 结构化病历草稿
- 新增病历 `medicalRecordId=55` 成功，新增处方 2 条：
  - `drugId=1, quantity=1`
  - `drugId=8, quantity=2`
- 病历 `55` 的 `is_purchasable` 已回写为 `0`，医生端和患者端均可通过 `medicalRecordId=55` 查询到一致的处方明细。
- 患者侧 AI：
  - 单轮分诊直接返回 `recommendedDepartments=["呼吸内科","心血管内科"]`
  - 多轮第 1 轮返回 `needMoreInfo=true`
  - 多轮第 2 轮收敛到 `recommendedSubDepartments=["呼吸内科","心血管内科"]`
- 观察项：
  - 医生端与患者端病历列表 SQL 当前按 `mr.create_time ASC` 排序，因此最新病历出现在第 2 页；数据已正确写入和可查询，但列表默认不是“最新在前”。

### 结论
- 是否通过：通过
- 问题编号：OBS-001（排序观察项，非阻塞）
- 是否需要回归：前端病历列表分页展示时需关注排序体验

### 证据
- 问诊与聊天：
  - `POST /treat/chat/initiate-consultation`
  - `POST /treat/chat/respond-consultation`
  - `GET /treat/chat/room/103`
  - `POST /treat/chat/send-message`
  - `POST /treat/chat/message`
  - `GET /treat/chat/messages/187`
- AI：
  - `POST /treat/ai/doctor/assist`
  - `POST /treat/ai/patient/triage`
  - `POST /treat/ai/patient/triage/chat`
- 病历与处方：
  - `POST /treat/front/doctor/medicalRecord/addMedicalRecord`
  - `POST /treat/front/doctor/prescription/addPrescription`
  - `GET /treat/front/doctor/medicalRecord/getPrescriptionInfoByMedicalRecordId`
  - `GET /treat/front/patient/medicalRecord/getPrescriptionInfoByMedicalRecordId`
- 数据库核对：
  - `room`
  - `chat_message`
  - `medical_record`
  - `prescription`
  - `registration`

---

## 2026-03-31 / 阶段 D / 前后端集成测试（第一轮）

### 环境信息
- 后端：已启动（`local` profile）
- 患者端：`http://127.0.0.1:5173`
- 医生端：`http://127.0.0.1:3000`
- 浏览器：Chrome DevTools 自动化

### 测试数据
- 患者账号：`userId=3`
- 医生账号（页面本地直连默认）：`userId=4 / 张心明`
- 患者端历史真实病历：包含 `medicalRecordId=55`
- 本轮补充医生账号 `userId=4` 的真实链路数据：
  - 新挂号：`registrationId=104`
  - 问诊房间：`roomId=188`
  - 新病历：`medicalRecordId=56`

### 执行步骤
1. 患者端登录后进入首页，检查首页导航、AI 分诊入口和科室导航区是否正常渲染。
2. 患者端进入 `/record` 页面，验证病历列表可展示真实历史数据，并切换到第 2 页查看最新新增病历 `medicalRecordId=55`。
3. 在患者端病历详情弹窗中查看处方明细，确认药品名称、数量、单位、价格与处方属性正确展示。
4. 患者端进入 `/ai-triage` 页面，通过页面输入“咳嗽三天，胸闷，低热，夜里明显一些，没有明显胸痛”，验证推荐结果和“去挂号”按钮。
5. 患者端进入 `/appointment/list` 页面，检查新增挂号 `registrationId=102/103` 的状态是否与接口结果一致。
6. 医生端进入 `/doctor/consultations` 页面，验证当前账号的待处理/全部问诊列表。
7. 使用当前登录医生 `userId=4` 新增一条真实测试链路：
   - 患者 `userId=3` 为医生 `userId=4` 的排班 `scheduleId=2745` 创建并支付挂号；
   - 医生发起问诊，生成 `roomId=188`；
   - 患者接受问诊并完成双向聊天消息写入。
8. 医生端在“全部问诊”页查看新问诊 `registrationId=104`，进入 `/doctor/chat/188` 页面。
9. 在聊天页点击“AI 协作”，验证结构化协作结果弹窗，并点击“回填到就诊记录”验证病历表单自动填充。
10. 通过接口为当前医生补充病历 `medicalRecordId=56` 与处方后，进入 `/doctor/medical-records` 页面，检查列表、详情与处方弹窗。

### 预期结果
- 患者端首页、AI 分诊、预约列表、就诊记录页面均可正常渲染真实数据。
- 患者端病历详情能展示处方明细。
- 医生端“全部问诊”可看到当前医生名下真实问诊，聊天页可加载历史消息。
- 医生端 AI 协作弹窗可返回结构化建议，并能将草稿回填到就诊记录表单。
- 医生端病历管理页可展示当前医生名下真实病历与处方详情。

### 实际结果
- 患者端首页渲染正常，已显示：
  - 顶部导航
  - AI 分诊入口
  - 科室/子科室导航与预约按钮
- 患者端 `/record` 页面成功加载真实病历数据：
  - 第 1 页展示历史病历 `39~49`
  - 第 2 页可见最新病历 `medicalRecordId=55`
- 患者端病历 `55` 的详情弹窗中，成功展示两条处方：
  - 阿莫西林胶囊 × 1 盒
  - 布洛芬缓释胶囊 × 2 盒
- 患者端 `/ai-triage` 页面发送真实症状后，页面直接显示：
  - 推荐方向：`呼吸内科，心血管内科，小儿内科`
  - 推荐依据文本
  - “去挂‘呼吸内科’”等跳转按钮
  - 点击后出现提示：`已带入推荐科室：内科 - 呼吸内科`
- 患者端 `/appointment/list` 页面正确展示：
  - `registrationId=102` 状态 `失效`
  - `registrationId=103` 状态 `已完成`
- 医生端本地直连账号实际为 `userId=4 / 张心明`。直接访问不属于该医生的房间 `roomId=187` 时，聊天页点击“AI 协作”返回 `code=403`、提示“当前医生无权访问该问诊上下文”；该现象与后端权限设计一致，属于正常鉴权结果，不记为缺陷。
- 针对当前医生 `userId=4` 补充真实测试链路后：
  - 新建挂号 `registrationId=104`
  - 新建房间 `roomId=188`
  - 写入聊天消息 2 条
  - 医生端 `/doctor/consultations` 的“全部问诊”页可看到 `registrationId=104`
  - 点击“进入问诊”后可进入 `/doctor/chat/188`
- 医生端 `/doctor/chat/188` 页面成功展示真实患者信息与历史聊天消息。
- 点击“AI 协作”后，成功弹出结构化协作结果，包含：
  - 风险提醒
  - 缺失项检查
  - 重点排查方向
  - 建议继续追问
  - 历史病历线索
  - 处方前注意
  - 主诉草稿 / 现病史草稿 / 结构化病历草稿
- 点击“回填到就诊记录”后，页面弹出“填写就诊记录”对话框，并自动填入 AI 生成的结构化病历草稿，同时出现提示：`AI 草稿已回填到就诊记录`
- 为当前医生补充病历 `medicalRecordId=56` 和处方后，医生端 `/doctor/medical-records` 页面成功展示：
  - 记录总数 `1`
  - 病历 `56`
  - 患者姓名 `任焱辉`
  - 状态 `未使用`
  - 详情弹窗与处方弹窗可正确展示药品 `阿莫西林胶囊 × 1`

### 结论
- 是否通过：通过
- 问题编号：
  - OBS-001：病历列表默认按 `create_time ASC` 排序，最新病历出现在第 2 页
  - OBS-002：医生端“待处理问诊”页不显示已接受并进入问诊中的房间，需切换到“全部问诊”查看（当前表现与页面语义一致，非阻塞）
- 是否需要回归：后续论文整理前可对患者端和医生端页面再做一次冒烟回归

### 证据
- 患者端页面：
  - `/home`
  - `/record`
  - `/ai-triage`
  - `/appointment/list`
- 医生端页面：
  - `/doctor/consultations`
  - `/doctor/chat/188`
  - `/doctor/medical-records`
- 真实数据补充接口：
  - `POST /treat/front/patient/appointment/create`
  - `POST /treat/front/patient/appointment/payment/mock/success`
  - `POST /treat/chat/initiate-consultation`
  - `POST /treat/chat/respond-consultation`
  - `POST /treat/chat/send-message`
  - `POST /treat/chat/message`
  - `POST /treat/front/doctor/medicalRecord/addMedicalRecord`
  - `POST /treat/front/doctor/prescription/addPrescription`

---

## 2026-03-31 / 阶段 E / 论文正文整理与最终回归

### 环境信息
- 后端：已启动（`local` profile）
- 患者端：已启动
- 医生/管理端：已启动
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 执行步骤
1. 按 `PLAN.md`、四期阶段文档与真实测试结果重写论文正文。
2. 恢复并重建 `thesis-draft.md`，纠正旧稿中过时或不准确表述。
3. 发起最终回归测试 `./mvnw test`，确认论文中的测试结论与当前仓库一致。

### 实际结果
- `thesis-draft.md` 已恢复，且已改写为正式论文结构（摘要、Abstract、绪论、技术基础、需求分析、总体设计、详细实现、系统测试、总结与展望、参考文献）。
- 论文内容已对齐以下事实：
  - WebSocket 采用 Spring 原生 WebSocket。
  - 患者端 Agent 采用院内业务检索增强推荐，不虚构向量库部署。
  - 医生端 Agent 采用 Java + LangChain4j 实现，表述为 LangGraph 风格工作流思想，而非原生 LangGraph runtime。
  - 长期记忆描述为历史病历与处方检索支撑，而非未实现的独立画像表。
- 最终回归执行 `./mvnw test` 结果：
  - `Tests run: 33`
  - `Failures: 0`
  - `Errors: 0`
  - `Skipped: 1`
  - `BUILD SUCCESS`
- 服务存活冒烟检查结果：
  - `GET /treat/ai/status` 返回 `code=200`
  - 患者端首页 `http://127.0.0.1:5173/` 返回 `HTTP/1.1 200 OK`
  - 医生/管理端首页 `http://127.0.0.1:3000/` 返回 `HTTP/1.1 200 OK`

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：当前阶段否

### 证据
- 论文文件：[thesis-draft.md](/mnt/d/AAAA/medical/thesis-draft.md)
- 后端回归：`cd medical-back && ./mvnw test`
- 后端状态：`curl -s http://127.0.0.1:8080/treat/ai/status`
- 患者端冒烟：`curl -I -s http://127.0.0.1:5173/`
- 医生端冒烟：`curl -I -s http://127.0.0.1:3000/`

---

## 2026-03-31 / 阶段 F / 真实全流程复核（第二轮）

### 环境信息
- 后端：已启动（`local` profile）
- 患者端：已启动
- 医生/管理端：已启动
- MySQL：已启动
- Redis：已启动
- RabbitMQ：已启动
- MinIO：已启动

### 测试数据
- 患者用户：`userId=3`
- 医生用户：`userId=4`
- 就诊人：`patientId=1`
- 排班：`scheduleId=2746`（`2026-04-01`，医生 `张心明`）
- 新增挂号：`registrationId=105`
- 支付单：`outTradeNo=REG10584C6853B34DD4982`
- 问诊房间：`roomId=189`
- 新增病历：`medicalRecordId=58`
- 新增处方：`prescriptionId=37,38`

### 执行步骤
1. 使用真实患者 token 调用 `/front/patient/appointment/create`，创建 `scheduleId=2746` 的新挂号请求。
2. 轮询 `/front/patient/appointment/status`，确认请求经 `Redis + RabbitMQ` 处理后进入 `PAYING`，生成真实 `registrationId=105` 与支付单。
3. 调用 `/front/patient/appointment/payment/form`，确认后端返回项目当前正式实现的支付页面表单。
4. 调用项目内置的支付确认接口 `/front/patient/appointment/payment/mock/success`，使挂号进入已支付状态。
5. 使用真实医生 token 调用 `/chat/initiate-consultation` 发起问诊，创建 `roomId=189`。
6. 使用真实患者 token 调用 `/chat/respond-consultation` 接诊，再通过 `/chat/message` 写入患者与医生聊天消息。
7. 调用 `/ai/doctor/assist`，验证医生端协作 Agent 基于真实房间上下文返回结构化结果。
8. 调用 `/front/doctor/medicalRecord/addMedicalRecord` 与 `/front/doctor/prescription/addPrescription`，写入病历与处方。
9. 调用 `/chat/room/189/status` 结束房间，验证挂号状态推进。
10. 通过患者端和医生端病历/处方查询接口回看新增结果。

### 实际结果
- 挂号创建返回：
  - `token=2746.c509dc0425a347cfae17b74b679df573`
  - 初始状态 `PROCESSING`
- 状态轮询返回：
  - `status=PAYING`
  - `registrationId=105`
  - `outTradeNo=REG10584C6853B34DD4982`
- 支付表单接口 `/payment/form` 返回 `200`，且 `formHtml` 中包含系统内置的模拟支付跳转页。
- 支付确认后：
  - `registration_payment_order.payment_status=1`
  - `registration.id=105` 的 `registration_status=1(已支付)`，随后在问诊流转后更新为 `5(已完成)`
- 医生发起问诊成功：
  - `roomId=189`
- 患者接诊成功后：
  - `room.room_status=2`
  - `registration.registration_status=3(问诊中)`
- 聊天消息已真实写入 `chat_message`：
  - 患者消息：“医生您好，我今天下午开始心慌，晚上有一点胸闷。”
  - 医生消息：“请补充持续时间、是否伴随胸痛、气短，以及既往心脏病史。”
- 医生端 AI 协作接口返回：
  - `source=local-doctor-copilot`
  - 包含 `riskAlerts`、`missingInfoItems`、`followUpQuestions`、`structuredRecordDraft`
- 病历与处方落库成功：
  - `medicalRecordId=58`
  - `prescription` 两条明细：`drugId=1 qty=1`、`drugId=2 qty=1`
  - 对应病历 `isPurchasable=0`
- 结束问诊后：
  - `room.id=189` 的 `room_status=4`
  - `registration.id=105` 的 `registration_status=5(已完成)`
- 患者端与医生端查询接口均能查到新增病历 `58` 及其处方：
  - 药品 `阿莫西林胶囊 × 1`
  - 药品 `头孢克肟分散片 × 1`

### 结论
- 是否通过：通过
- 问题编号：无
- 是否需要回归：否

### 说明
- 本轮测试未使用 Mockito、MockBean 或外部桩服务。
- 单元测试与集成测试均建立在真实 MySQL、Redis、RabbitMQ、MinIO、本地后端服务与双前端页面之上。
- 需要单独说明的是：**支付步骤调用的是项目当前正式实现的内置模拟支付模块**，其业务实现位于 `RegistrationPaymentService`，并非测试期间临时注入的 mock/stub。当前项目本身未接入第三方真实支付网关，因此“真实全流程测试”的边界为：除第三方支付清结算外，其余核心业务流程均已在真实运行环境中完成验证。

### 证据
- 挂号创建：`POST /treat/front/patient/appointment/create`
- 状态轮询：`GET /treat/front/patient/appointment/status`
- 支付表单：`GET /treat/front/patient/appointment/payment/form`
- 支付确认：`POST /treat/front/patient/appointment/payment/mock/success`
- 发起问诊：`POST /treat/chat/initiate-consultation`
- 患者接诊：`POST /treat/chat/respond-consultation`
- 聊天消息：`POST /treat/chat/message`
- AI 协作：`POST /treat/ai/doctor/assist`
- 病历新增：`POST /treat/front/doctor/medicalRecord/addMedicalRecord`
- 处方新增：`POST /treat/front/doctor/prescription/addPrescription`
- 患者回看：`POST /treat/front/patient/medicalRecord/getMedicalRecordByUserId`
- 处方明细：`GET /treat/front/patient/medicalRecord/getPrescriptionInfoByMedicalRecordId`

---

## 2026-04-01 / 阶段 B / 批次 1（B1 AI 单元测试）首次执行

### 环境信息
- 后端：未拉起（本批次为后端单元测试）
- MySQL：未验证（本批次主要是纯逻辑单元测试）
- Redis：未验证
- RabbitMQ：未验证
- MinIO：未验证
- Maven：可执行，但外网仓库访问受限

### 测试数据
- 测试类：
  - `cn.edu.ncu.medical.ai.AiRequestSanitizerTest`
  - `cn.edu.ncu.medical.ai.AiRateLimiterTest`
  - `cn.edu.ncu.medical.ai.AiRequestContextTest`

### 执行步骤
1. 先尝试使用 Maven Wrapper 执行 AI 单元测试子集。
2. 由于 wrapper 下载失败，切换到系统 `mvn` 再次执行。
3. 记录失败原因与阻塞点，纳入下一轮执行前置条件。

### 预期结果
- 完成批次 1 的 AI 单元测试首轮执行并获得通过/失败结果。

### 实际结果
- `./mvnw` 触发下载 Maven 发行包时失败（无法拉取 `apache-maven-3.9.11-bin.zip`）。
- 使用系统 `mvn` 后，构建在解析父 POM 时被远程仓库 `403 Forbidden` 阻断，未进入测试执行阶段。

### 结论
- 是否通过：未通过（环境阻塞）
- 问题编号：ENV-MVN-001
- 是否需要回归：是（网络/仓库可访问后立即重跑批次 1）

### 证据
- 命令：`cd medical-back && ./mvnw -Dtest=cn.edu.ncu.medical.ai.AiRequestSanitizerTest,cn.edu.ncu.medical.ai.AiRateLimiterTest,cn.edu.ncu.medical.ai.AiRequestContextTest test`
- 命令：`cd medical-back && mvn -Dtest=cn.edu.ncu.medical.ai.AiRequestSanitizerTest,cn.edu.ncu.medical.ai.AiRateLimiterTest,cn.edu.ncu.medical.ai.AiRequestContextTest test`
- 关键报错：`Could not transfer artifact ... status code: 403, reason phrase: Forbidden (403)`

---

## 2026-04-01 / 阶段 B / 批次 1（B1 AI 单元测试）阻塞定位补充

### 环境信息
- Maven：3.9.10（系统安装）
- 网络：可访问命令行，但 Maven 远程仓库请求被网关拦截

### 执行步骤
1. 使用 `curl -I` 直接验证 Maven 中央仓库 POM 地址可达性。
2. 尝试 Aliyun 镜像地址做对照验证。
3. 检查本机 `~/.m2/repository` 是否已有可复用离线依赖缓存。

### 预期结果
- 明确当前阻塞发生在“项目配置”还是“网络出口策略”。

### 实际结果
- `repo.maven.apache.org` 返回 `HTTP/1.1 403 Forbidden`。
- `maven.aliyun.com` 同样返回 `HTTP/1.1 403 Forbidden`。
- 本机不存在 `~/.m2/repository` 缓存，无法离线构建。

### 结论
- 是否通过：未通过（环境阻塞持续）
- 问题编号：ENV-MVN-001（延续）
- 是否需要回归：是

### 证据
- `curl -I -s https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/3.4.7/spring-boot-starter-parent-3.4.7.pom | head`
- `curl -I -s https://maven.aliyun.com/repository/public/org/springframework/boot/spring-boot-starter-parent/3.4.7/spring-boot-starter-parent-3.4.7.pom | head`
- `ls -la ~/.m2/repository/org/springframework/boot/spring-boot-starter-parent/3.4.7`

---

## 2026-04-01 / 6小时连续执行 / N1（环境与依赖可用性复核）

### 计划目标
- 复核后续连续执行所需的基础运行条件（端口、容器、后端状态）。

### 实际执行命令
1. `ss -ltnp | rg ':(3306|6379|5672|15672|9000|9002|8080)\b' || true`
2. `docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'`
3. `curl -s -m 3 http://127.0.0.1:8080/treat/ai/status || true`

### 实际结果
- `ss` 命令不可用（`command not found`）。
- `docker` 命令不可用（`command not found`）。
- `curl` 在 3 秒超时窗口内未返回可用结果。

### 达成情况
- 是否达成：部分达成（明确了环境工具缺失，但未拿到完整依赖状态）。

### 反思
- 本节点主要阻塞：执行环境缺少网络/容器观测工具，无法直接验证服务存活。
- 对后续节点影响：接口级与集成级测试需改为“可执行命令 + 错误证据”模式推进，先完成离线可执行内容。
- 下一节点调整动作：先进行功能拆解与优先级细化，不等待环境恢复。

---

## 2026-04-01 / 6小时连续执行 / N2（功能点拆解与优先级确认）

### 计划目标
- 将测试目标细化到模块规模，形成可连续推进的优先级队列。

### 实际执行命令
1. `rg --files medical-back/src/main/java | rg '/controller/' | wc -l`
2. `rg --files medical-back/src/main/java | rg '/service/' | wc -l`
3. `rg --files medical-back/src/main/java | rg '/service/impl/' | wc -l`
4. `rg --files medical-back/src/test/java | wc -l`
5. `rg --files medical-patient-front/src/views | wc -l`
6. `rg --files medical-front-doctor/back-vue-master/src/views | wc -l`
7. `rg --files medical-back/src/main/java/cn/edu/ncu/medical/controller | sed -n '1,200p'`

### 实际结果
- 后端 Controller：22 个
- Service 接口：30 个
- ServiceImpl：15 个
- 后端测试类：14 个
- 患者端视图：14 个
- 医生/管理端视图：15 个
- 已整理患者/医生/管理/通用四类控制器清单，确认后续接口测试可按角色分批推进。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：无功能阻塞，主要是后续执行环境仍未恢复。
- 对后续节点影响：可先把 B1/B2 用例细化到可直接转 JUnit 的粒度，避免空等。
- 下一节点调整动作：进入 N3，补齐每个用例的输入样例与断言口径。

---

## 2026-04-01 / 6小时连续执行 / N3（B1/B2 参数细化）

### 计划目标
- 把 B1/B2 的用例细化成可直接转 JUnit 的“输入+断言”结构。

### 实际执行操作
1. 在 `continuous-6h-test-plan.md` 新增 `N3 产出（B1/B2 参数与断言口径）`。
2. 给 `AiRequestSanitizer`、`AiRateLimiter`、`AiRequestContext`、`AppointmentReservationState`、`AppointmentReservationKeys`、`PatientIdentityUtil` 分别补充输入样例与断言口径。

### 实际结果
- 已形成可直接用于下一步写测试代码的断言模板，避免后续执行出现“只有方向没有可测断言”的问题。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：仍未恢复 Maven 依赖下载，无法立即把用例转成可运行测试。
- 对后续节点影响：N4/N5 执行时将继续先尝试构建；若失败则记录阻塞并切换接口链路验证准备。
- 下一节点调整动作：进入 N4，重试 B1 执行并抓取阻塞证据。

---

## 2026-04-01 / 6小时连续执行 / N4（执行 B1 AI 单元测试）

### 计划目标
- 实际执行 `AiRequestSanitizerTest`，至少拿到 1 个可运行测试结果。

### 实际执行命令
1. `cd medical-back && ./mvnw -Dtest=cn.edu.ncu.medical.ai.AiRequestSanitizerTest test`
2. `cd medical-back && sh ./mvnw -Dtest=cn.edu.ncu.medical.ai.AiRequestSanitizerTest test`
3. `cd medical-back && mvn -Dtest=cn.edu.ncu.medical.ai.AiRequestSanitizerTest test`

### 实际结果
- `./mvnw`：权限不足（`Permission denied`）。
- `sh ./mvnw`：wrapper 下载 Maven 发行包失败。
- `mvn`：解析 Spring Boot 父 POM 时中央仓库返回 `403 Forbidden`。

### 达成情况
- 是否达成：未达成（环境阻塞）

### 反思
- 本节点主要阻塞：运行链路被“执行权限 + 远程仓库403”双重阻塞。
- 对后续节点影响：N5（B2）在当前环境预计同样失败，应避免重复无效尝试，转向“测试资产先行（用例/脚本/证据模板）+ 接口链路前置准备”。
- 下一节点调整动作：N5 只做一次最小化重试留证据，然后立即切 N6 数据准备。

---

## 2026-04-01 / 6小时连续执行 / N5（执行 B2 状态机/Key 单元测试）

### 计划目标
- 最小化重试 B2，确认是否存在与 B1 不同的执行结果。

### 实际执行命令
- `cd medical-back && mvn -Dtest=cn.edu.ncu.medical.registration.AppointmentReservationStateTest,cn.edu.ncu.medical.registration.AppointmentReservationKeysTest test`

### 实际结果
- 与 B1 一致，构建阶段即因父 POM 拉取失败被阻断：`403 Forbidden`。

### 达成情况
- 是否达成：未达成（环境阻塞）

### 反思
- 本节点主要阻塞：Maven 依赖下载在项目启动前即失败，和具体测试类无关。
- 对后续节点影响：继续跑本地单测没有增量信息；应切换到“真实数据脚本与接口样本准备”以保持推进。
- 下一节点调整动作：进入 N6，固定测试数据来源并提取可直接使用的样本账号。

---

## 2026-04-01 / 6小时连续执行 / N6（C阶段接口测试数据准备）

### 计划目标
- 固定 C 阶段接口测试所需的数据脚本与账号样本，降低后续联调随机性。

### 实际执行命令
1. `wc -l scripts/OnlineTreat_medical_only.sql`
2. `wc -l scripts/20260312_registration_person_key.sql`
3. `wc -l scripts/20260321_cleanup_demo_data.sql`
4. `rg "INSERT INTO" scripts/OnlineTreat_medical_only.sql | head -n 40`

### 实际结果
- 数据脚本规模已确认：
  - `OnlineTreat_medical_only.sql`：1051 行
  - `20260312_registration_person_key.sql`：25 行
  - `20260321_cleanup_demo_data.sql`：73 行
- 已提取到可直接用于测试的种子样本：
  - 患者：`system_user.id=3 (user1)`
  - 医生：`system_user.id=5 (cardio_dir1)`
  - 已存在科室/子科室初始化数据（内科、外科、妇产科等）

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：无法直接连接数据库核对“当前运行库”与 SQL 样本的一致性。
- 对后续节点影响：N7/N8 若无后端服务，将先输出标准化接口用例和请求脚本模板。
- 下一节点调整动作：进入 N7，先构建 C1/C2 的 API 执行脚本与断言清单。

---

## 2026-04-01 / 6小时连续执行 / N7（C1/C2 登录+就诊人）

### 计划目标
- 执行 C1/C2，至少输出可复用命令与断言清单，环境可用时可直接运行。

### 实际执行操作
1. 新建 `scripts/testing/c1-c2-api-checklist.md`。
2. 固化 C1 登录鉴权与 C2 就诊人管理的 curl 命令、token 传递方式和断言口径。
3. 明确成功分支与异常分支（重复身份证）两类验证路径。

### 实际结果
- C1/C2 已形成“可直接复制执行”的命令级脚本文档。
- 在后端可访问时可立即执行，不需要再次设计接口用例。

### 达成情况
- 是否达成：达成（设计与脚本准备完成）

### 反思
- 本节点主要阻塞：当前无法确认本机 8080 服务状态，暂未产出实时响应证据。
- 对后续节点影响：N8/N9/N10 将继续先固化请求模板，同时保留“环境恢复后立即回放”的执行顺序。
- 下一节点调整动作：进入 N8，生成 C3/C4（排班+挂号）脚本模板与断言。

---

## 2026-04-01 / 6小时连续执行 / N8（C3/C4 排班+挂号）

### 计划目标
- 固化排班查询与挂号创建/状态轮询的可执行脚本，确保环境恢复后可直接跑核心链路。

### 实际执行操作
1. 新建 `scripts/testing/c3-c4-api-checklist.md`。
2. 定义 C3（科室/子科室/排班）查询命令。
3. 定义 C4（创建挂号、状态轮询、重复拦截、库存不足）命令与断言。

### 实际结果
- C3/C4 用例脚本已就绪，包含成功与异常分支。
- 已固定最关键的参数传递方式（`PATIENT_TOKEN`、`APPOINT_TOKEN`）。

### 达成情况
- 是否达成：达成（脚本就绪）

### 反思
- 本节点主要阻塞：未获得在线服务可用证据，暂时无法产出实时响应结果。
- 对后续节点影响：N9/N10 将继续构建问诊/病历/处方脚本，形成“环境恢复即可回放”的批量执行包。
- 下一节点调整动作：进入 N9，输出 C5/C6 脚本。

---

## 2026-04-01 / 6小时连续执行 / N9（C5/C6 问诊+聊天）

### 计划目标
- 输出问诊房间与聊天消息链路的执行脚本，并尝试在线验证。

### 实际执行操作
1. 新建 `scripts/testing/c5-c6-api-checklist.md`，覆盖发起问诊、接诊、拒绝分支、消息发送、房间状态查询。
2. 尝试调用房间状态接口：`POST /treat/chat/room/189/status`（本地 8080）。

### 实际结果
- 脚本模板完成。
- 在线接口在 5 秒窗口内无返回内容，未获得有效回包证据。

### 达成情况
- 是否达成：部分达成（脚本达成，在线验证阻塞）。

### 反思
- 本节点主要阻塞：后端服务可用性不可确认，导致无法验证问诊状态与消息落库。
- 对后续节点影响：N10/N11 继续先沉淀可执行脚本与断言，等待服务恢复后批量回放。
- 下一节点调整动作：进入 N10，输出病历/处方脚本并进行最小化在线探测。

---

## 2026-04-01 / 6小时连续执行 / N10（C7/C8 病历+处方）

### 计划目标
- 输出病历与处方链路执行脚本，并进行接口连通性探测。

### 实际执行操作
1. 新建 `scripts/testing/c7-c8-api-checklist.md`，覆盖医生新增病历、医患两侧病历查询、医生开处方、患者查处方。
2. 尝试调用病历列表接口：`POST /treat/front/doctor/medicalRecord/getMedicalRecordList?pageNum=1&pageSize=10`。

### 实际结果
- 脚本模板完成。
- 在线探测无返回内容，未形成实时接口证据。

### 达成情况
- 是否达成：部分达成（脚本达成，在线验证阻塞）。

### 反思
- 本节点主要阻塞：服务侧不可观测状态持续。
- 对后续节点影响：N11 将以“AI+管理+一致性校验模板”先行，优先保证恢复后可快速执行。
- 下一节点调整动作：进入 N11。

---

## 2026-04-01 / 6小时连续执行 / N11（C9/C10 + D）

### 计划目标
- 输出 AI、管理端、状态一致性校验模板，并进行接口探测。

### 实际执行操作
1. 新建 `scripts/testing/c9-c10-d-e-checklist.md`，覆盖：
   - `POST /treat/ai/doctor/assist`
   - `POST /treat/ai/patient/triage`
   - `POST /treat/admin/drug/getDrugList`
   - `POST /treat/admin/doctor/findAll`
   - D 阶段一致性检查项。
2. 尝试调用 AI 分诊与管理端药品列表接口进行连通性探测。

### 实际结果
- 脚本模板完成。
- 在线接口无返回内容，实时校验未完成。

### 达成情况
- 是否达成：部分达成（模板达成，实时验证阻塞）。

### 反思
- 本节点主要阻塞：在线服务不可用导致 C9/C10 和 D 只能准备不能验收。
- 对后续节点影响：N12 将完成回归执行顺序与恢复后的一键回放步骤。
- 下一节点调整动作：进入 N12。

---

## 2026-04-01 / 6小时连续执行 / N12（E 集成回归收口）

### 计划目标
- 完成本轮 6 小时执行收口，给出恢复后回放顺序。

### 实际执行操作
1. 汇总 N1~N11 的阻塞与产出。
2. 固化恢复后推荐回放顺序：`C1/C2 -> C3/C4 -> C5/C6 -> C7/C8 -> C9/C10 -> D -> E`。
3. 将本轮状态同步到 `continuous-6h-test-plan.md` 的节点清单与结论段。

### 实际结果
- 本轮已完成完整 12 节点连续执行（含反思）。
- 受环境限制，实时在线验证未闭环，但测试资产已可复用。

### 达成情况
- 是否达成：达成（以可执行资产与完整记录为目标）。

### 反思
- 本节点主要阻塞：外部依赖可用性未恢复。
- 对后续节点影响：环境恢复后可直接按回放顺序执行，无需再设计。
- 下一节点调整动作：进入下一轮“环境恢复后的批量回放执行”。

---

## 2026-04-01 / 连续执行续轮 / N13（清单接口路径静态校验与修正）

### 计划目标
- 在服务不可用条件下，先保证所有 API 清单中的路径与后端真实映射一致，避免恢复后出现“脚本路径错误”。

### 实际执行命令
1. `python scripts/testing/validate_checklists.py`
2. `sed -n '1,260p' scripts/testing/checklist-endpoint-validation.md`
3. `sed -n '1,260p' medical-back/src/main/java/cn/edu/ncu/medical/controller/patient/PatientAttendantController.java`
4. `sed -n '1,260p' medical-back/src/main/java/cn/edu/ncu/medical/controller/patient/ScheduleController.java`
5. `sed -n '1,360p' medical-back/src/main/java/cn/edu/ncu/medical/websocket/ChatController.java | rg "@RequestMapping|@GetMapping|@PostMapping" -n`

### 实际结果
- 新增并执行了 `scripts/testing/validate_checklists.py`，自动对比 checklist URL 与后端路由。
- 根据校验结果修正了以下脚本路径：
  - 就诊人接口从 `/front/patient/patientAttendant/*` 更正为 `/front/patient/attendant/*`
  - 排班接口收敛为 `/front/patient/schedule/findList`
  - 管理端接口更正为 `/back/admin/drug/getDrugList`、`/back/admin/doctor/findAll`
  - 房间消息查询更正为 `/chat/messages/{roomId}`
- 修正后再次校验：清单中检测到的接口路径已全部匹配后端映射（✅）。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：仍缺实时服务回包，无法验证业务语义，只能先确保路径层正确。
- 对后续节点影响：环境恢复后可直接回放，不会再浪费时间在 404/路由不匹配问题上。
- 下一节点调整动作：进入续轮 N14，补充请求体字段与 DTO 的静态一致性检查。

---

## 2026-04-01 / 连续执行续轮 / N14（请求体与 DTO 字段一致性校验）

### 计划目标
- 校验 checklist 中 JSON 请求体字段与后端 DTO 定义一致，减少恢复后 400 参数错误。

### 实际执行命令
1. `sed -n '1,220p' medical-back/src/main/java/cn/edu/ncu/medical/entity/dto/IdCard.java`
2. `sed -n '1,220p' medical-back/src/main/java/cn/edu/ncu/medical/entity/dto/AppointmentCreateRequest.java`
3. `sed -n '1,220p' medical-back/src/main/java/cn/edu/ncu/medical/entity/dto/DoctorAiRequest.java`
4. `sed -n '1,220p' medical-back/src/main/java/cn/edu/ncu/medical/entity/dto/TriageRequest.java`

### 实际结果
- 已按 DTO 修正脚本请求体字段：
  - `IdCard`：改为 `realName` + `idCard`
  - `DoctorAiRequest`：改为 `summary`、`conversationSnippet`、`symptoms`
  - `TriageRequest`：改为 `description`、`age`、`gender`、`symptoms`
- `AppointmentCreateRequest` 字段保持 `patientId` + `scheduleId`，与现有脚本一致。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：无法在线提交验证，但静态字段口径已统一。
- 对后续节点影响：环境恢复后可直接进行参数级验证，减少请求体不匹配错误。
- 下一节点调整动作：进入续轮 N15，输出“恢复后一键回放执行顺序脚本”。

---

## 2026-04-01 / 连续执行续轮 / N15（恢复后一键回放脚本）

### 计划目标
- 将 C1~E 的执行顺序脚本化，恢复环境后可直接按顺序推进。

### 实际执行命令
1. `bash scripts/testing/run-all-checklists.sh`

### 实际结果
- 已新增并执行 `scripts/testing/run-all-checklists.sh`。
- 脚本按顺序输出 C1/C2 -> C3/C4 -> C5/C6 -> C7/C8 -> C9/C10+D/E 的回放步骤。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：当前仍是流程脚本级验证，未连接实时服务。
- 对后续节点影响：一旦服务恢复，可直接照顺序执行并回填日志。
- 下一节点调整动作：进入续轮 N16，做本轮资产一致性总检查并收口。

---

## 2026-04-01 / 连续执行续轮 / N16（测试资产一致性总检查）

### 计划目标
- 对本轮新增脚本做一致性总检查，确认“可执行路径、参数字段、回放顺序”三者闭环。

### 实际执行命令
1. `python scripts/testing/validate_checklists.py`
2. `tail -n 60 scripts/testing/checklist-endpoint-validation.md`

### 实际结果
- Endpoint 校验报告显示：五份 checklist 中提取的后端路径均匹配（✅）。
- 结合 N14 的 DTO 字段校验与 N15 的顺序脚本，已形成可回放的完整测试资产包。

### 达成情况
- 是否达成：达成

### 反思
- 本节点主要阻塞：仍缺在线服务回包证据，当前结论属于“资产完备、待环境回放”。
- 对后续节点影响：后续工作可直接切换到“真实环境回放执行+论文结果沉淀”。
- 下一节点调整动作：等待环境可用后，按 `run-all-checklists.sh` 实施并回填通过率、缺陷率、回归结果。
