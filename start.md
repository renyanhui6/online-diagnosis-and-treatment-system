# 启动指南（本地开发）

> 注意：不要把任何 API Key/密码写进仓库并提交到 Git。本文只给出“放置位置”和“启动命令”，Key 请你在自己电脑上粘贴到**本地私有文件**中。

## 最新进度（已更新到当前仓库实现）
- AI：已接入 DeepSeek（需配置 `DEEPSEEK_API_KEY`），并提供自检接口 `GET /treat/ai/status`
- 上传：已改为本地 MinIO（S3 兼容），默认 `http://localhost:9000`，bucket `medical`
- local 模式：MySQL 数据库 + 默认关闭鉴权并注入 dev 登录态；排班生成/挂号状态推进为“补偿式”；RabbitMQ 监听默认关闭；Redis 未启动时常用操作会走内存兜底（便于联调）
- 排班/挂号：排班生成与挂号状态推进改为“补偿式”，不会因为错过某个定时点就永久卡死
- 一键脚本（Windows + Docker）：`scripts/start-redis.bat`、`scripts/start-rabbitmq.bat`、`scripts/start-minio.bat`

## 快速启动（推荐）

### Windows PowerShell（后端 + 两套前端）
```powershell
# 0) 依赖服务（建议先启动；至少 MinIO 用于上传）
.\scripts\start-minio.bat
.\scripts\start-redis.bat
.\scripts\start-rabbitmq.bat

# 1) 后端（local：可不依赖 MySQL/Redis/RabbitMQ；AI 需要 DeepSeek Key）
cd medical-back
$env:SPRING_PROFILES_ACTIVE="local"
# 如果你已配置到系统环境变量，这行可以不写
$env:DEEPSEEK_API_KEY="你的apikey"
.\mvnw.cmd spring-boot:run

# 2) 患者端（另开一个终端）
cd medical-patient-front
npm i
npm run dev

# 3) 医生/管理员端（另开一个终端）
cd medical-front-doctor/back-vue-master
npm i
npm run dev
```

### macOS/Linux（后端 + 两套前端）
```bash
# 0) 依赖服务（Docker 方式，至少 MinIO 用于上传）
# docker run -d --name medical-minio -p 9000:9000 -p 9001:9001 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -v medical-minio-data:/data minio/minio server /data --console-address ":9001"

# 1) 后端（local）
cd medical-back
export SPRING_PROFILES_ACTIVE=local
# 如果你已配置到系统环境变量，这行可以不写
export DEEPSEEK_API_KEY='你的apikey'
./mvnw spring-boot:run

# 2) 患者端（另开一个终端）
cd medical-patient-front
npm i
npm run dev

# 3) 医生/管理员端（另开一个终端）
cd medical-front-doctor/back-vue-master
npm i
npm run dev
```

## 0. 项目结构
- 后端（Spring Boot）：`medical-back`
- 患者端前端（Vue3 + Element Plus）：`medical-patient-front`
- 医生/管理员前端（Vue3 + Element Plus）：`medical-front-doctor/back-vue-master`

## 完整业务需要的依赖服务（Redis/RabbitMQ/MySQL）
如果你要把“登录验证码、token 刷新、支付/订单延迟处理、消息队列”等流程完整跑通，建议你本机启动：
- Redis（6379）
- RabbitMQ（5672/15672）
- MySQL（3306，且需要导入项目所需表结构/数据）
- MinIO（文件上传对象存储，9000/9001；如你要用“上传图片/科室图片/聊天图片”）

## RabbitMQ 常见问题：启动后日志“无限循环刷屏”
如果你启动 RabbitMQ 后，后端日志反复刷类似异常：
- `ListenerExecutionFailedException`
- `AppointmentException: 排班不存在或不合法`

通常表示 **RabbitMQ 队列里堆积了历史消息**（比如之前没启动 RabbitMQ/后端消费端，导致旧的挂号创建消息积压；这些消息对应的排班日期已经过期，所以业务校验会失败）。

目前后端已做处理：**业务异常不再回队列重试**，不会再出现“同一条消息无限循环重试”的情况。  
如果你想立刻清空历史消息（推荐），可以在 RabbitMQ 管理台直接 Purge：

1) 打开管理台：`http://localhost:15672`（默认账号密码 `guest/guest`）  
2) 进入 `Queues`  
3) 找到并 Purge：`order.appointment.create.queue`（以及你不需要的其他队列）

### 用 Docker Desktop 启动（推荐）
Windows PowerShell：
```powershell
# Redis（无密码）
docker run -d --name medical-redis -p 6379:6379 redis:7-alpine

# RabbitMQ（带管理台 http://localhost:15672，默认 guest/guest）
docker run -d --name medical-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# MinIO（API:9000, 控制台:9001，默认 minioadmin/minioadmin）
docker run -d --name medical-minio -p 9000:9000 -p 9001:9001 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -v medical-minio-data:/data minio/minio server /data --console-address ":9001"
```

也可以直接双击运行（Windows）：
- `scripts/start-redis.bat`
- `scripts/start-rabbitmq.bat`
- `scripts/start-minio.bat`
- `scripts/check-services.bat`（检查三个容器是否 running，失败会打印 logs）

日志位置：
- 以上 bat 执行日志会写到 `scripts/logs/*.log`
- 如果你不想脚本执行完 `pause`，可以在命令行中运行：`scripts\\start-minio.bat --no-pause`

> 说明：`local` profile 允许你不启动 Redis/RabbitMQ 也能“跑起来联调”，但这会牺牲一部分业务能力；要完整业务就需要把依赖服务也启动并配置到后端。

## 1. 环境准备

### 1.1 后端必需
- JDK：`17`（建议 17/21）
- Maven：可选（仓库自带 `medical-back/mvnw`）

### 1.2 前端必需
- Node.js：建议 `18 LTS` 或 `20 LTS`
- npm：随 Node 自带（也可用 pnpm/yarn，但本项目以 npm 为例）

### 1.3 完整功能（可选）
如果你要把“挂号/支付/队列/Redis 过期事件”等完整跑起来，还需要：
- MySQL 8+
- Redis
- RabbitMQ

> 如果你只是想先“能启动 + 页面能打开 + 接口能通”，可以先按下面的“最小可启动（local profile）”跑。

## 2. 后端启动（最小可启动：local profile）

### 2.1 进入后端目录
```bash
cd medical-back
```

### 2.2 配置 DeepSeek（AI 功能需要）

#### 方式 A：环境变量（推荐）
Linux/macOS（当前终端会话生效）：
```bash
export DEEPSEEK_API_KEY='你的apikey'
export DEEPSEEK_BASE_URL='https://api.deepseek.com'
export DEEPSEEK_MODEL='deepseek-chat'
```

Windows PowerShell（当前窗口生效）：
```powershell
$env:DEEPSEEK_API_KEY="你的apikey"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_MODEL="deepseek-chat"
```

Windows（永久写入用户环境变量，需重开终端/IDE）：
```bat
setx DEEPSEEK_API_KEY "你的apikey"
setx DEEPSEEK_BASE_URL "https://api.deepseek.com"
setx DEEPSEEK_MODEL "deepseek-chat"
```

#### 方式 B：本地私有配置文件（不推荐但可用）
我不会替你把 Key 写进仓库文件；你可以在本地新建一个**不会提交**的私有配置文件：

1) 新建文件：`medical-back/src/main/resources/application-local.secret.yml`  
2) 写入（示例，Key 你自己填）：
```yml
ai:
  deepseek:
    base-url: https://api.deepseek.com
    api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxx
    model: deepseek-chat
```

> 该文件会被 `application-local.yml` 以 `spring.config.import` 方式可选加载；没有这个文件也能启动，但 AI 接口会返回 `code=9001`（服务不可用）。

### 2.3 启动命令
#### 推荐方式（跨平台最稳）：使用 `SPRING_PROFILES_ACTIVE`
Windows PowerShell：
```powershell
$env:SPRING_PROFILES_ACTIVE="local"
.\mvnw.cmd spring-boot:run
```

Windows CMD：
```bat
set SPRING_PROFILES_ACTIVE=local
mvnw spring-boot:run
```

Linux/macOS：
```bash
export SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

#### 备用方式：Maven 参数指定 profile
> 如果你遇到 `Unknown lifecycle phase ".run.profiles=local"`，请按下面写法**加引号**或改用上面的 `SPRING_PROFILES_ACTIVE`。

Windows PowerShell：
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

Linux/macOS：
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 2.4 验证后端启动
- 访问：`http://localhost:8080/treat`
- AI 接口：`POST /treat/ai/patient/triage`、`POST /treat/ai/doctor/assist`

> local profile 默认关闭鉴权并注入 dev 登录态，用于“能启动/能联调”。local profile 当前使用 MySQL（见 `medical-back/src/main/resources/application-local.yml`）；请确保你配置的 MySQL 可用并已导入表结构/数据。
> local profile 下你可以不启动 Redis/RabbitMQ：项目会对常用 Redis 操作做内存兜底（用于验证码/临时 token 等），RabbitMQ 的监听与声明默认关闭；并且 local 配置会显式清空 `spring.data.redis.username/password`，避免本机 Redis 无密码时出现 AUTH 报错。
> 另外，local profile 已开启排班补齐与挂号状态对账（`app.schedule.enabled=true`、`app.registration.reconcile-enabled=true`）；如果你只想“最小启动”而不跑这些后台补偿逻辑，可以在 local 配置里把它们关掉。

### 2.5 MinIO（本地上传）
local profile 默认 MinIO 配置为：
- `endpoint=http://localhost:9000`
- `bucket=medical`
- 控制台：`http://localhost:9001`（默认 `minioadmin/minioadmin`）

建议先运行：`scripts/start-minio.bat`，或用 Docker 命令启动 MinIO（见上文）。

MinIO 环境变量（可选覆盖）：
- `MINIO_ENDPOINT`（默认 `http://localhost:9000`）
- `MINIO_ACCESS_KEY`（默认 `minioadmin`）
- `MINIO_SECRET_KEY`（默认 `minioadmin`）
- `MINIO_BUCKET`（默认 `medical`）
- `MINIO_PUBLIC_URL_PREFIX`（默认 `http://localhost:9000/medical`）

#### 2.4.1 AI 配置自检（推荐先做）
如果你已经配置了 `DEEPSEEK_API_KEY` 但接口仍返回 `code=9001`，先自检后端进程是否真的读到了环境变量：
```bash
curl "http://localhost:8080/treat/ai/status"
```
返回里关注：
- `deepSeekEnabled=true`
- `apiKeyPresent=true` 且 `apiKeyLength>0`

若 `deepSeekEnabled=false`：
- 先在你启动后端的同一个终端里确认变量是否真的存在：
  - Windows PowerShell：`echo $env:DEEPSEEK_API_KEY`
  - Windows CMD：`echo %DEEPSEEK_API_KEY%`
- Windows `setx` 设置后需要**重开一个新的终端/IDE**再启动后端（`setx` 不会影响当前已打开的窗口）
- 也可以只在当前窗口临时设置（立即生效）：
  - PowerShell：`$env:DEEPSEEK_API_KEY="你的apikey"` 后再启动后端
  - CMD：`set DEEPSEEK_API_KEY=你的apikey` 后再启动后端

## 3. 后端启动（完整模式：连接 MySQL/Redis/RabbitMQ）

### 3.1 修改配置
后端默认配置在：`medical-back/src/main/resources/application.yml`  
你需要把以下服务改成你自己的地址/账号：
- `spring.datasource.*`（MySQL）
- `spring.data.redis.*`（Redis）
- `spring.rabbitmq.*`（RabbitMQ）

### 3.2 启动命令
```bash
cd medical-back
./mvnw spring-boot:run
```

## 4. 患者端前端启动（Vite）

### 4.1 安装依赖
```bash
cd medical-patient-front
npm install
```

### 4.2 启动开发服务器
```bash
npm run dev
```

### 4.3 访问
- `http://localhost:5173`

说明：
- 患者端接口默认请求 `http://localhost:8080/treat`（见 `medical-patient-front/src/api/request.js`），所以后端需要在 `8080` 启动。

## 5. 医生/管理员前端启动（Vite）

### 5.1 安装依赖
```bash
cd medical-front-doctor/back-vue-master
npm install
```

### 5.2 启动开发服务器
```bash
npm run dev
```

### 5.3 访问
- `http://localhost:3000`

说明：
- 该前端通过 Vite 代理把 `/api/*` 转发到 `http://localhost:8080/treat`（见 `medical-front-doctor/back-vue-master/vite.config.js`）。

## 6. WebSocket（可选）
- 默认仍走旧 WS：`/treat/common/ws/chat`
- 如需切换 Netty WS：`ws://localhost:9001/netty/ws/chat`

前端可通过环境变量覆盖（可选）：
- `VITE_WS_BASE=localhost:9001`
- `VITE_WS_PATH=/netty/ws/chat`
