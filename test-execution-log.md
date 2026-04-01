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
