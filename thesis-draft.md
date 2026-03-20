# 基于 Spring Boot 与 Vue 的在线诊疗系统设计与实现

## 摘要
随着互联网医疗的发展，传统线下就诊流程在挂号分流、诊前沟通、诊中协作与诊后追踪等环节逐渐暴露出效率低、排队时间长、信息流转不连续等问题。针对这一类问题，本文围绕“预约挂号、在线问诊、病历留存、处方管理、后台治理”构建了一套在线诊疗系统，并从工程实现与业务稳定性两个层面展开设计。系统以后端 Spring Boot 为核心，结合 MyBatis-Plus 实现业务数据访问，采用 Vue 构建患者端与医生/管理端界面，使用 Redis 与 RabbitMQ 实现挂号高并发预占及异步落库，基于 Netty WebSocket 实现医患问诊实时通信，借助 MinIO 实现图片消息与附件对象存储。

本文的研究重点不只是完成一个可用的 Web 系统，更强调在医疗场景下对关键业务链路进行稳定性治理。针对排班管理，系统设计了“排班模板 + 按日生成排班 + 手动补偿生成”的机制，并提供“仅补缺失”和“补缺失并清理未来无效排班”两种治理模式，以降低定时任务漏执行、模板变更和重复排班带来的数据污染风险。针对挂号环节，提出了基于 Redis Lua 脚本的号源预占方案，将库存扣减、重复提交校验、请求记录写入和过期集合登记合并到原子操作中，再通过 RabbitMQ 异步完成挂号持久化，从而缓解高并发下超卖与重复挂号问题。针对问诊环节，系统统一收敛为 Netty WebSocket 长连接实现，并在房间状态、挂号状态和前端交互之间建立一致性的状态推进机制。

在实现层面，系统完成了三类用户角色的主要业务功能。患者端支持注册登录、就诊人管理、科室选择、排班查看、在线挂号、问诊确认、在线聊天以及就诊记录查看；医生端支持挂号列表查看、发起问诊、实时聊天、病历填写与处方开具；管理端支持医生管理、患者管理、药品管理、排班模板配置以及排班补偿生成。本文在系统联调过程中对多项真实问题进行了修复，包括排班与挂号字段口径不一致、医生端历史消息字段兼容问题、病历提交时医生身份来源不稳定、患者端刷新后无法继续接受问诊、问诊弹窗显示占位医生信息等，并在最终版本中通过真实接口和页面联调完成验证。

测试结果表明，该系统能够稳定支撑在线诊疗核心闭环。实际联调中已完成患者挂号成功创建挂号记录、医生发起问诊、患者确认接诊、医患双向消息通信、医生填写病历、医生开具处方以及患者端查看病历和处方结果等完整流程；管理端医生/患者/排班模板 CRUD 及排班补偿生成也已通过真实接口验证。研究结果说明，将模板化排班治理、基于 Redis 与消息队列的预约控制、以及统一长连接问诊机制结合到同一系统中，可以有效提升在线诊疗业务链路的一致性与可用性。

关键词：在线诊疗系统；预约挂号；排班模板；Redis；RabbitMQ；Netty WebSocket

## Abstract
With the rapid development of Internet healthcare, conventional offline outpatient processes gradually expose problems such as low scheduling efficiency, long waiting time, fragmented information flow, and weak continuity between registration, consultation, and follow-up. To address these issues, this thesis designs and implements an online diagnosis and treatment system covering appointment registration, online consultation, medical record management, prescription management, and administrative governance. The backend is implemented with Spring Boot and MyBatis-Plus, the patient side and doctor/admin side are developed with Vue, Redis and RabbitMQ are used to support high-concurrency reservation and asynchronous persistence, Netty WebSocket is employed for real-time consultation, and MinIO is used for object storage of image messages and attachments.

The research focus of this work is not limited to building a functional information system, but further emphasizes the consistency and stability of critical medical workflows. For schedule management, a template-driven generation mechanism is designed, together with compensation generation strategies for missed tasks and future invalid schedule cleanup. For appointment booking, an atomic Redis Lua-based reservation mechanism is introduced to integrate stock deduction, duplicate-submission detection, reservation record creation, and expiration tracking, followed by RabbitMQ-based asynchronous persistence to reduce overselling and duplicate registrations. For online consultation, the system unifies real-time communication on Netty WebSocket and establishes a consistent state transition mechanism among room status, registration status, and frontend interactions.

The system supports three major roles. The patient side provides login, attendant management, department browsing, schedule selection, appointment booking, consultation confirmation, online chat, and record review. The doctor side supports registration viewing, consultation initiation, real-time communication, medical record writing, and prescription generation. The administrative side supports doctor management, patient management, medicine management, schedule template configuration, and compensation generation. During implementation, several practical issues were discovered and fixed, including inconsistent doctor/department data sources, historical-message field incompatibility, unstable doctor identity sources during medical record submission, inability to continue consultation after page refresh, and placeholder doctor information in consultation notifications.

Real integration tests show that the system can support the core end-to-end workflow of online diagnosis and treatment. The verified flow includes successful appointment creation, consultation initiation, patient acceptance, bidirectional chat, medical record submission, prescription creation, and patient-side record review. Administrative CRUD and schedule compensation generation were also validated through real APIs. The results demonstrate that combining template-based schedule governance, Redis and message-queue-based booking control, and unified WebSocket consultation can significantly improve consistency and robustness in online medical service systems.

**Keywords:** online diagnosis and treatment system; appointment registration; schedule template; Redis; RabbitMQ; Netty WebSocket

## 第1章 绪论

### 1.1 研究背景
医疗信息化建设已经从单纯的院内管理逐步演进到面向患者、医生和医院管理者的全流程协同。传统门诊模式下，患者往往需要先线下排队挂号，再根据现场秩序等待问诊，诊疗过程中病历、处方和沟通信息分散在多个环节，导致诊前准备不足、诊中协作效率有限、诊后回溯成本较高。尤其在复诊、常见病咨询、图文问诊和慢病管理等场景中，线下模式无法充分发挥互联网平台在信息聚合、流程自动化和实时交互方面的优势。

与此同时，在线医疗系统的实现并不是简单地把线下业务搬到线上。挂号业务涉及排班模板、号源控制、重复提交与高并发一致性；问诊业务涉及医生发起、患者确认、消息推送、聊天持久化和状态回收；后台治理则涉及模板调整、定时任务补偿和脏数据清理。因此，一个真正可运行的在线诊疗系统，必须在业务流程设计与系统工程实现之间建立紧密联系。

### 1.2 研究意义
本文的研究意义主要体现在以下三个方面。

第一，在应用层面，在线诊疗系统可以改善患者预约体验，缩短传统门诊中的低效等待环节，使挂号、问诊与病历留存形成更连续的服务闭环。第二，在工程层面，医疗系统中的挂号与问诊流程天然带有状态机特征和并发一致性要求，适合作为后端业务治理、缓存设计、消息驱动架构与长连接通信的综合实践对象。第三，在研究层面，本文尝试把“排班治理”“预约并发控制”“诊疗状态一致性”作为系统设计核心问题，而不是将系统停留在页面和表结构层面，从而使毕业设计具备一定的工程研究属性。

### 1.3 国内外研究现状
从公开的高校毕业设计与教学案例来看，围绕预约挂号、医院管理、门诊排班和医疗问诊的系统设计已经较为常见，但多数实现仍然聚焦于“管理系统式”的增删改查与基础流程演示，较少深入处理高并发预约、状态补偿、长连接收敛以及异常恢复等问题。另一方面，工业界互联网医疗平台在在线问诊、电子处方、患者画像和医生服务调度方面已经形成较成熟的产品形态，但其架构和治理策略通常不完全公开，不适合直接照搬到本科毕业设计中。

基于此，本文选取在线诊疗系统作为研究对象，既保留患者端、医生端、管理端三类角色的完整业务流程，又针对排班生成、Redis 预占挂号、RabbitMQ 异步落库、Netty 实时问诊等关键机制进行工程化设计，以实现“功能可运行、逻辑可解释、方案可复现”的研究目标。

### 1.4 研究内容
本文围绕在线诊疗场景，完成以下研究与实现工作：

1. 建立患者端、医生端、管理端三类角色的业务模型，明确挂号、问诊、病历、处方和后台治理的业务边界。
2. 设计排班模板驱动的排班生成方案，构建支持补偿生成和未来无效排班清理的排班治理机制。
3. 设计基于 Redis Lua 脚本与 RabbitMQ 的挂号请求处理机制，实现号源预占、重复提交控制、异步落库与失败回滚。
4. 设计基于 Netty WebSocket 的实时问诊机制，统一问诊通知、房间聊天和消息持久化的技术口径。
5. 完成病历与处方的后续流转，实现患者端对问诊结果的可视化查看。
6. 在本地真实运行环境中完成核心接口与页面联调，对实际暴露的问题进行修复与回归验证。

### 1.5 论文结构安排
全文共分为七章。第一章介绍研究背景、意义、研究内容与论文结构。第二章介绍系统实现所依赖的关键技术。第三章对系统功能和非功能需求进行分析。第四章给出系统总体设计，包括架构设计、数据设计和关键状态机设计。第五章说明系统关键模块的具体实现。第六章给出测试方案与测试结果分析。第七章对全文工作进行总结，并提出后续改进方向。

## 第2章 关键技术与理论基础

### 2.1 Spring Boot 与 MyBatis-Plus
Spring Boot 提供了统一的依赖管理、自动配置和快速启动能力，适合构建中小规模但模块完整的后端业务系统。本文以后端服务为中心，将登录认证、排班管理、挂号服务、问诊服务、病历与处方服务统一构建在 Spring Boot 框架之上。MyBatis-Plus 在保留 SQL 可控性的同时提供了常用 CRUD 能力和 Lambda 查询封装，便于在复杂联表场景与简单管理接口之间取得平衡。

在线诊疗系统中存在大量“管理类接口 + 复杂联表查询”并存的场景。例如，医生端问诊列表既需要按登录医生筛选挂号记录，又需要联表查询患者年龄、联系方式和科室信息；患者端病历列表既需要展示病历摘要，又要在详情中进一步查询处方明细。基于此，系统采用“实体 CRUD 走 MyBatis-Plus，复杂展示走 Mapper XML 联表”的实现策略，以保证查询口径的一致性和可维护性。

### 2.2 Vue 前端框架
患者端和医生/管理端前端均采用 Vue 技术栈实现。Vue 的组件化开发模式适合将预约挂号页、问诊页、管理页等复杂界面拆分为职责清晰的视图模块。对于本系统而言，患者端更强调交互连续性和状态提示，例如挂号轮询状态、问诊通知弹窗、病历与处方详情展示；医生/管理端则更强调业务表格、筛选、状态操作和后台治理功能。

### 2.3 Redis 与 RabbitMQ
Redis 在本系统中承担两类职责。第一类是登录态与缓存管理，用于开发调试和业务数据加速；第二类是挂号高并发控制，用于实现号源预占、重复提交判定、预约处理中间态记录以及超时释放。为了保证多个操作的一致性，系统把“库存扣减 + 重复提交判定 + 预占记录写入 + 过期集合登记”组合为 Lua 脚本的一次原子执行。

RabbitMQ 用于挂号持久化的异步解耦。挂号接口接收到请求后，并不直接同步写入数据库，而是先在 Redis 中完成预占，再将预约消息投递到消息队列，由消费者完成 registration 落库和 schedule 已预约数同步。这样做的主要目的是降低同步请求链路的写放大问题，并为失败重试和异常回滚预留补偿空间。

### 2.4 Netty WebSocket
Netty 是一个高性能异步事件驱动网络框架，适合构建长连接和实时消息系统。在线问诊本质上属于双向消息交互场景，HTTP 轮询不仅交互延迟高，而且难以处理医生发起问诊、患者确认响应和实时聊天广播等需求。因此本文采用 Netty WebSocket 作为统一的实时通信方案，房间路径固定为 `ws://host:9001/netty/ws/chat/{roomId}`，患者和医生的长连接通知则使用 `patient_{userId}` 与 `doctor_{userId}` 标识。

### 2.5 MinIO 对象存储
问诊过程中存在图片消息、上传附件等非结构化数据。若将这类文件直接存入数据库，不仅会增加数据库负担，也不利于后续扩展。系统采用 MinIO 作为对象存储服务，将上传文件存入对象存储，再将文件 URL 作为消息内容或附件引用写回业务表中，实现结构化数据与非结构化数据的解耦。

## 第3章 系统需求分析

### 3.1 角色需求分析

#### 3.1.1 患者端需求
患者端需要完成注册登录、就诊人管理、科室浏览、排班查看、挂号提交、挂号状态查询、问诊确认、在线聊天以及病历与处方查看等功能。患者对系统最核心的需求并不是“能看到界面”，而是“挂号是否真实成功、医生是否真实发起问诊、诊后信息是否能够回看”。

#### 3.1.2 医生端需求
医生端需要根据挂号列表开展接诊工作，包括查看当日与历史问诊、发起问诊、等待患者确认、进入聊天房间、编写病历、开具处方和结束问诊。医生端的业务重心在于“围绕挂号记录开展处理”，而不是单独维护排班视图，因此本文在功能收敛阶段移除了医生端独立排班页，统一以挂号列表和问诊房间作为医生侧主要工作入口。

#### 3.1.3 管理端需求
管理端负责基础资料和业务规则治理，包括医生管理、患者管理、药品管理、科室管理、排班模板管理以及补偿生成。管理端需求中最具治理特征的是排班模板与补偿生成，因为这部分决定了前台是否有排班可挂以及错过定时任务后如何恢复。

### 3.2 功能需求分析
结合项目实现，系统主要功能可划分为五类：

1. 账户与身份功能：注册、登录、获取用户信息、找回密码。
2. 排班与挂号功能：排班模板维护、排班生成、号源展示、挂号提交、预约状态轮询。
3. 在线问诊功能：医生发起、患者确认、房间聊天、图片消息、超时处理。
4. 诊疗结果功能：病历保存、处方生成、患者记录查看。
5. 后台治理功能：医生/患者/药品 CRUD、补偿生成、未来无效排班清理。

### 3.3 非功能需求分析
本系统除功能需求外，还需要满足以下非功能要求：

1. 一致性要求：挂号状态、问诊房间状态和前端按钮显示必须保持一致。
2. 并发安全要求：同一真实就诊人对同一排班不能重复成功挂号，号源不能超卖。
3. 可恢复要求：当系统错过定时任务或服务中断时，应能通过补偿机制恢复排班和挂号状态。
4. 可维护要求：医生、患者、科室、模板等后台数据应支持真实 CRUD，而非仅提供展示壳子。
5. 可测试要求：系统核心链路应能在本地真实环境中完成编译、启动、接口联调和页面联调。

### 3.4 业务流程分析
系统的核心业务流程可概括为：患者登录后选择科室与医生，查看某日排班和剩余号源，提交挂号请求；系统完成预约预占与落库后，医生端在适当时机发起问诊；患者确认后进入在线聊天；问诊结束后医生填写病历和处方，患者最终在记录页查看诊疗结果。与传统“页面跳转型”系统不同，本系统在挂号、发起问诊和房间状态之间引入了显式状态机，这也是系统设计的关键。

## 第4章 系统总体设计

### 4.1 系统总体架构设计
系统采用前后端分离架构。前端分为患者端与医生/管理端两套 Vue 应用，后端采用 Spring Boot 统一提供 REST 接口与 WebSocket 入口，底层配合 MySQL、Redis、RabbitMQ 与 MinIO 构成完整运行环境。

从架构职责划分看，患者端负责预约和就诊结果查看，医生端负责问诊执行，管理端负责数据治理；后端则按模块拆分为认证与用户模块、排班与挂号模块、问诊与聊天模块、病历与处方模块以及后台管理模块。Redis 主要服务于挂号预占和缓存，RabbitMQ 主要服务于预约异步持久化，Netty 主要服务于实时消息推送，MinIO 主要服务于文件对象存储。

### 4.2 数据设计
结合本系统的实现范围，核心数据实体包括：

1. `system_user`：存储账号、密码、类型、邮箱和状态等信息。
2. `doctor_detail`：存储医生真实姓名、职称、科室、简介和价格等信息。
3. `patient_attendant`：存储就诊人实名信息、手机号、家庭住址等信息。
4. `sub_department`：存储子科室名称及描述，用于医生归属和患者选科。
5. `schedule_template`：存储医生每周出诊模板、上午/下午号源上限及启用状态。
6. `schedule`：存储实际日期排班、当前已预约数、排班快照等执行态信息。
7. `registration`：存储挂号记录、挂号状态、请求令牌与真实就诊人标识。
8. `room`：存储问诊房间及其状态。
9. `chat_message`：存储文本消息、图片消息及发送者信息。
10. `medical_record` 与 `prescription`：存储问诊结果。

### 4.3 排班生成机制设计
排班模块采用“模板定义规则，排班存储执行结果”的设计。`schedule_template` 描述医生每周几出诊以及上午、下午各开放多少号；`schedule` 则是面向患者和挂号系统可直接使用的排班实例。这样做有两个好处：一是前台查排班时不需要每次动态计算；二是挂号库存、预约计数和后续统计都可以直接围绕 `scheduleId` 进行。

在此基础上，系统增加了补偿生成机制。普通补偿模式仅补充缺失排班，不删除任何未来计划；增强模式则在补缺失之前先清理“未来无效排班”，包括重复排班、失效模板对应排班和未来快照不一致但尚未被挂号使用的排班。该设计的核心思想是：未来排班属于可收敛计划，过去排班属于业务历史，不能简单按当前模板反向删除。

### 4.4 预约状态机设计
系统中存在两层状态机。第一层是预约预占状态机，用于解决高并发一致性问题，包括 `PENDING`、`CONFIRMED` 和 `ROLLED_BACK` 三个状态；第二层是业务挂号状态机，用于表达患者可见和医生可见的业务流程，包括 `PAID(1)`、`QUEUING(2)`、`WAITING_CONFIRM(7)`、`IN_PROGRESS(3)`、`COMPLETED(4)`，以及 `SUSPENDED(5)`、`RESUMED(6)`、`INVALID(8)` 等异常与恢复状态。

将技术状态机与业务状态机拆开设计的原因在于，两者解决的问题不同。前者关注缓存、消息投递、回滚和重试；后者关注医生接诊、患者确认和页面按钮语义。如果将二者混合，状态语义会同时承载业务含义与基础设施异常，导致系统复杂度急剧增加。

### 4.5 问诊通信设计
问诊通信统一基于 Netty WebSocket 实现。医生发起问诊后，系统根据挂号记录创建房间，将房间状态置为等待确认，并向患者长连接发送 `consultation_request` 消息。患者接受后，房间状态推进到“问诊中”，挂号状态同步推进到 `IN_PROGRESS`；若患者拒绝或超时未响应，则进入挂起或失效分支。聊天消息统一走“消息解析 -> 数据库存储 -> 房间广播”的处理路径，文本和图片消息在前后端都走同一条归口逻辑。

## 第5章 系统实现

### 5.1 认证与用户模块实现
系统支持前后端分离的登录认证。前端登录成功后将令牌写入本地存储，并通过请求拦截器放入 `access-key` 请求头。后端根据登录态返回当前用户信息。为了适配本地联调，系统还提供了开发模式下的 DevToken 能力，用于快速切换不同测试用户并完成真实多角色调试。

### 5.2 管理端真实 CRUD 实现
管理端实现了医生管理、患者管理、药品管理与排班模板管理等真实 CRUD 功能。以医生管理为例，创建医生并不是单表插入，而是先创建 `system_user`，再创建对应的 `doctor_detail`；删除时则需要同步清理登录态并逻辑删除医生资料。患者管理同样包括账号信息与主就诊人信息的联合维护。实际联调中，本文已通过接口验证医生与患者的创建、详情查询、更新、状态修改和删除闭环均可执行成功。

### 5.3 排班模板与补偿生成实现
排班模板接口支持分页查询、创建、更新、删除和按医生筛选。生成器根据日期范围扫描启用模板，并以“医生 + 日期 + 时段”为幂等粒度判断是否已存在排班。若不存在则创建 `schedule`，若存在则跳过。对于补偿模式，生成器可根据管理员选择分别执行“仅补缺失”和“补缺失并清理未来无效”。

这部分实现的难点不在于单次插入，而在于幂等性和治理边界。为避免模板变更和任务重跑造成重复排班，系统在生成前优先做同日同医生同时段检查；为避免后台任务隐式删除未来计划，增强清理模式被设计为管理员显式触发，而不是定时任务默认行为。

### 5.4 Redis 与 RabbitMQ 挂号链路实现
挂号接口接收到请求后，先对就诊人合法性、排班合法性和号源状态进行校验。随后系统使用真实就诊人身份生成 `personKey`，调用 Redis Lua 脚本完成库存扣减、重复提交判定、预占记录写入和过期集合登记，并返回预约请求 `token`。前端使用该 `token` 轮询挂号状态；后台消费者接收消息后完成 `registration` 落库，并将 Redis 状态从 `PENDING` 推进到 `CONFIRMED`。

与传统同步落库方案相比，这种实现方式虽然增加了 Redis 和消息队列的协调复杂度，但显著提升了高并发场景下的安全性和可恢复性。若持久化失败，系统会把状态回滚为 `ROLLED_BACK` 并释放号源；若请求长时间未完成，则通过过期扫描任务释放预占库存。

### 5.5 在线问诊模块实现
在线问诊模块由房间管理、通知推送、聊天消息和状态同步四部分构成。医生端发起问诊时，后端根据挂号记录反查真实医生和患者身份，不再信任前端传来的医生 ID 和患者 ID；患者端接收到问诊请求后，既可以通过实时弹窗操作，也可以在“我的预约”页对 `WAITING_CONFIRM` 状态记录进行接受或拒绝，从而避免刷新页面导致流程中断。

在实现过程中，系统还对通知内容做了统一治理。早期版本的重新接诊通知只推送“医生”“科室”“主治医师”等占位信息，患者端弹窗虽然能出现，但缺少真实上下文。后续通过在 `ChatController` 中统一补齐 `doctorName`、`departmentName` 和 `doctorTitle`，患者端实时通知已经能正确展示真实医生信息。

### 5.6 病历与处方模块实现
问诊结束后，医生在聊天页或问诊管理页填写病历并开具处方。病历接口在最终版本中改为根据登录态和挂号记录推导真实医生身份，避免因前端状态漂移造成病历所属医生错误。患者端就诊记录页则支持查看病历摘要和处方明细，能够展示药品名称、数量、单位、单价及处方属性，从而形成诊后可回溯闭环。

## 第6章 系统测试与结果分析

### 6.1 测试环境
系统测试环境采用本地开发模式，后端运行在 Spring Boot `local` 配置之上，MySQL 作为主数据库，Redis、RabbitMQ 与 MinIO 通过 Docker 容器启动，患者端与医生/管理端分别通过 Vite 开发服务器运行。测试方法以接口联调、页面联调和关键状态验证为主，重点检查业务闭环是否真实打通，而不是只检查页面是否可访问。

### 6.2 功能测试
本文围绕以下关键场景进行了真实测试：

| 测试编号 | 测试内容 | 预期结果 | 实测结果 |
| --- | --- | --- | --- |
| T1 | 患者端挂号创建 | 返回处理中 `token`，随后状态变为 `SUCCESS` | 已通过，`scheduleId=1868` 对应挂号成功生成 `registrationId=98` |
| T2 | 医生发起问诊与患者接受 | 患者收到通知，接受后进入房间 | 已通过，历史联调中已完成医生发起、患者接受、房间创建 |
| T3 | 医患双向聊天 | 双端消息均能持久化和广播 | 已通过，文本消息在医生端和患者端均可见 |
| T4 | 病历与处方提交 | 医生提交成功，患者端可查看结果 | 已通过，病历 `medicalRecordId=52` 与处方明细已在患者端展示 |
| T5 | 重新接诊通知 | 患者端弹窗展示真实医生信息 | 已通过，页面实测显示“韩雪梅 / 妇科 / 专家” |
| T6 | 医生 CRUD | 创建、更新、禁用、删除可执行 | 已通过 |
| T7 | 患者 CRUD | 创建、更新、禁用、删除可执行 | 已通过 |
| T8 | 排班模板 CRUD 与补偿生成 | 新增、更新、删除、补偿生成可执行 | 已通过，`fill_missing` 模式返回生成统计结果 |

### 6.3 问题发现与修复分析
测试过程中暴露的问题主要集中在三个方面。

第一，数据口径问题。早期部分挂号展示仍然读取 `schedule` 快照中的医生与科室名称，导致聊天、病历和挂号列表中的医生名称不一致。后续通过将展示口径收敛为 `registration.doctor_id -> doctor_detail/sub_department` 联表主数据源，解决了这一问题。

第二，前后端状态协同问题。问诊过程中，如果患者刷新页面，原有实时弹窗会丢失，导致用户看起来“有问诊请求但无法继续操作”。系统最终通过在“我的预约”页增加 `WAITING_CONFIRM` 状态操作入口解决这一问题。

第三，消息通知质量问题。重新接诊通知虽然已经能到达患者端，但医生信息显示为占位文本，不利于真实使用体验。后续通过通知体补齐真实医生信息并做页面回归测试，完成了这一修复。

### 6.4 测试结果讨论
从测试结果来看，系统已经完成了在线诊疗核心闭环和后台治理闭环。特别是在预约并发控制、状态一致性推进和问诊实时交互三个方面，系统不再停留于普通管理系统的展示逻辑，而是具备较强的业务可运行性。

但同时也应看到，系统仍存在进一步完善空间。例如，支付链路在当前版本中被收敛为“直接创建挂号”而非真实支付回调闭环；测试主要集中在功能正确性和链路打通上，尚未形成系统化的压力测试报告；论文中的“国内外研究现状”与“相关工作对比”还可以在后续版本中继续补充文献支撑。

## 第7章 总结与展望

本文围绕在线诊疗业务场景，设计并实现了一套基于 Spring Boot 与 Vue 的在线诊疗系统。相较于仅关注页面与表结构的传统课程设计，本文将研究重点放在排班治理、挂号并发控制和问诊状态一致性三个关键问题上，并通过 Redis、RabbitMQ 与 Netty 等技术手段完成了相应工程落地。系统已经实现患者端、医生端和管理端的主要功能，经过真实接口与页面联调，挂号、问诊、病历和处方等核心业务闭环可以正常运行。

本文的主要工作可以概括为以下三点：其一，构建了模板驱动的排班生成与补偿机制，解决了排班重复生成、模板变更后未来计划收敛等问题；其二，设计并实现了基于 Redis Lua 和 RabbitMQ 的挂号处理链路，增强了高并发挂号场景下的安全性与一致性；其三，统一了 Netty WebSocket 问诊实现，打通了医生发起、患者确认、房间聊天和结果留存的完整链路。

后续工作可以从以下几个方向展开。第一，补充真实支付与回调机制，使挂号状态推进更加贴合实际业务。第二，引入更完善的监控和压测手段，对 Redis 预占与消息队列链路进行量化评估。第三，将医生智能辅助、病历结构化分析等能力纳入系统，在现有在线诊疗框架基础上扩展更高层次的智能服务功能。

## 参考文献（初稿）
[1] 南昌大学软件学院. 南昌大学2025届本科生毕业论文（设计）答辩信息公示表[EB/OL]. https://soft.ncu.edu.cn/info/1028/5291.htm.  
[2] 南昌大学. 南昌大学本科生毕业论文（设计）工作条例[EB/OL]. https://spms.ncu.edu.cn/info/2031/29392.htm.  
[3] Spring. Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/docs/current/reference/html/.  
[4] Redis Labs. Redis Documentation[EB/OL]. https://redis.io/docs/.  
[5] RabbitMQ Team. RabbitMQ Documentation[EB/OL]. https://www.rabbitmq.com/documentation.html.  
[6] Netty Project. Netty User Guide[EB/OL]. https://netty.io/wiki/user-guide-for-4.x.html.  
[7] Vue Team. Vue.js Guide[EB/OL]. https://vuejs.org/guide/.  

## 后续补写建议
1. 在绪论中补充更具体的“国内外研究现状”文献引用。
2. 在系统测试章节增加接口响应耗时、并发挂号实验和异常恢复实验。
3. 将章节中的表格和业务流程图迁移到正式论文模板中。
4. 根据学校格式要求补充封面、诚信声明、致谢和附录。
