# 启动指南（本地开发）

> 注意：不要把任何 API Key/密码写进仓库并提交到 Git。本文只给出“放置位置”和“启动命令”，Key 请你在自己电脑上粘贴到**本地私有文件**中。

## 最新进度（已更新到当前仓库实现）
- AI：已接入 DeepSeek（需配置 `DEEPSEEK_API_KEY`），并提供自检接口 `GET /treat/ai/status`
- 支付：已恢复支付宝沙盒页支付链路，挂号创建后会先进入 `待支付`，再由患者端拉起沙盒支付页并回跳 `/payment/result`
- 上传：已改为本地 MinIO（S3 兼容），默认 `http://localhost:9000`，bucket `medical`
- local 模式：使用本地 MySQL 数据库；默认关闭严格鉴权，但会优先按请求中的 dev token 识别当前用户，无 token 时回退到 `application-local.yml` 中的 dev 用户；排班生成/挂号状态推进为“补偿式”；挂号创建已切换为 `Redis 预占 + RabbitMQ 异步落库`，因此 **Redis 和 RabbitMQ 都必须启动**
- 排班/挂号：排班生成与挂号状态推进改为“补偿式”，不会因为错过某个定时点就永久卡死
- 一键脚本（Windows + Docker）：`scripts/start-redis.bat`、`scripts/start-rabbitmq.bat`、`scripts/start-minio.bat`

## 快速启动（推荐）

### Windows PowerShell（后端 + 两套前端）
```powershell
# 0) 依赖服务（建议先启动；至少 MinIO 用于上传）
.\scripts\start-minio.bat
.\scripts\start-redis.bat
.\scripts\start-rabbitmq.bat

# 1) 后端（local：需要 MySQL + Redis + RabbitMQ；AI 需要 DeepSeek Key）
cd medical-back
$env:SPRING_PROFILES_ACTIVE="local"
# 如果你已配置到系统环境变量，这行可以不写
$env:DEEPSEEK_API_KEY="你的apikey"
$env:ALIPAY_ENABLED="true"
$env:ALIPAY_APP_ID="你的沙盒APPID"
$env:ALIPAY_PRIVATE_KEY="你的应用私钥"
$env:ALIPAY_PUBLIC_KEY="支付宝沙盒公钥"
$env:ALIPAY_RETURN_URL="http://127.0.0.1:5173/payment/result"
.\mvnw.cmd spring-boot:run

# 2) 患者端（另开一个终端）
cd medical-patient-front
npm ci
npm run dev

# 3) 医生/管理员端（另开一个终端）
cd medical-front-doctor/back-vue-master
npm ci
npm run dev
```

### macOS/Linux（后端 + 两套前端）
```bash
# 0) 依赖服务（Docker 方式，至少 MinIO 用于上传）
# docker run -d --name medical-minio -p 9000:9000 -p 9002:9001 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -v medical-minio-data:/data minio/minio server /data --console-address ":9001"

# 1) 后端（local：仍需要 MySQL）
cd medical-back
export SPRING_PROFILES_ACTIVE=local
# 如果你已配置到系统环境变量，这行可以不写
export DEEPSEEK_API_KEY='你的apikey'
export ALIPAY_ENABLED='true'
export ALIPAY_APP_ID='你的沙盒APPID'
export ALIPAY_PRIVATE_KEY='你的应用私钥'
export ALIPAY_PUBLIC_KEY='支付宝沙盒公钥'
export ALIPAY_RETURN_URL='http://127.0.0.1:5173/payment/result'
./mvnw spring-boot:run

# 2) 患者端（另开一个终端）
cd medical-patient-front
npm ci
npm run dev

# 3) 医生/管理员端（另开一个终端）
cd medical-front-doctor/back-vue-master
npm ci
npm run dev
```

## 0. 项目结构
- 后端（Spring Boot）：`medical-back`
- 患者端前端（Vue3 + Element Plus）：`medical-patient-front`
- 医生/管理员前端（Vue3 + Element Plus）：`medical-front-doctor/back-vue-master`

## 完整业务需要的依赖服务（Redis/RabbitMQ/MySQL）
如果你要把当前仓库里的“登录验证码、token 刷新、挂号预占、异步落库、消息队列”等流程完整跑通，本机必须启动：
- Redis（6379）
- RabbitMQ（5672/15672）
- MySQL（3306，且需要导入项目所需表结构/数据）
- MinIO（文件上传对象存储，API 9000 / 控制台 9002；如你要用“上传图片/科室图片/聊天图片”）

## RabbitMQ 常见问题：启动后日志“无限循环刷屏”
如果你启动 RabbitMQ 后，后端日志反复刷类似异常：
- `ListenerExecutionFailedException`
- `AppointmentException: 排班不存在或不合法`

通常表示 **RabbitMQ 队列里堆积了历史消息**（比如之前没启动 RabbitMQ/后端消费端，导致旧的挂号创建消息积压；这些消息对应的排班日期已经过期，所以业务校验会失败）。

目前后端已做处理：**业务异常不再回队列重试**，不会再出现“同一条消息无限循环重试”的情况。  
如果你想立刻清空历史消息（推荐），可以在 RabbitMQ 管理台直接 Purge：

1) 打开管理台：`http://localhost:15672`（默认账号密码 `guest/guest`）  
2) 进入 `Queues`  
3) 找到并 Purge：`appointment.registration.create.queue`（以及你不需要的其他队列）

### 用 Docker Desktop 启动（推荐）
Windows PowerShell：
```powershell
# Redis（无密码）
docker run -d --name medical-redis -p 6379:6379 redis:7-alpine

# RabbitMQ（带管理台 http://localhost:15672，默认 guest/guest）
docker run -d --name medical-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# MinIO（API:9000, 控制台:9002，默认 minioadmin/minioadmin；控制台端口映射到容器 9001）
docker run -d --name medical-minio -p 9000:9000 -p 9002:9001 -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin -v medical-minio-data:/data minio/minio server /data --console-address ":9001"
```

也可以直接双击运行（Windows）：
- `scripts/start-redis.bat`
- `scripts/start-rabbitmq.bat`
- `scripts/start-minio.bat`
- `scripts/check-services.bat`（检查三个容器是否 running，失败会打印 logs）

日志位置：
- 以上 bat 执行日志会写到 `scripts/logs/*.log`
- 如果你不想脚本执行完 `pause`，可以在命令行中运行：`scripts\\start-minio.bat --no-pause`

> 说明：当前 `local` profile 仍可关闭严格鉴权做本地联调，但**挂号链路不再支持脱离 Redis/RabbitMQ 运行**。

## 1. 环境准备

### 1.1 后端必需
- JDK：`17`（建议 17/21）
- Maven：可选（仓库自带 `medical-back/mvnw`）

### 1.2 前端必需
- Node.js：建议 `18 LTS` 或 `20 LTS`
- npm：随 Node 自带（也可用 pnpm/yarn，但本项目以 npm 为例）

### 1.3 完整功能（必需）
如果你要把当前“挂号/队列/Redis 预占/超时回收”等完整跑起来，还需要：
- MySQL 8+
- Redis
- RabbitMQ

> 如果你只是先验证页面和非挂号接口，仍然可以先按下面的 `local profile` 跑；但要验证挂号创建，Redis 和 RabbitMQ 必须可用。

## 2. 后端启动（local profile）

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
export ALIPAY_ENABLED='true'
export ALIPAY_APP_ID='你的沙盒APPID'
export ALIPAY_PRIVATE_KEY='你的应用私钥'
export ALIPAY_PUBLIC_KEY='支付宝沙盒公钥'
export ALIPAY_RETURN_URL='http://127.0.0.1:5173/payment/result'
```

Windows PowerShell（当前窗口生效）：
```powershell
$env:DEEPSEEK_API_KEY="你的apikey"
$env:DEEPSEEK_BASE_URL="https://api.deepseek.com"
$env:DEEPSEEK_MODEL="deepseek-chat"
$env:ALIPAY_ENABLED="true"
$env:ALIPAY_APP_ID="你的沙盒APPID"
$env:ALIPAY_PRIVATE_KEY="你的应用私钥"
$env:ALIPAY_PUBLIC_KEY="支付宝沙盒公钥"
$env:ALIPAY_RETURN_URL="http://127.0.0.1:5173/payment/result"
```

Windows（永久写入用户环境变量，需重开终端/IDE）：
```bat
setx DEEPSEEK_API_KEY "你的apikey"
setx DEEPSEEK_BASE_URL "https://api.deepseek.com"
setx DEEPSEEK_MODEL "deepseek-chat"
setx ALIPAY_ENABLED "true"
setx ALIPAY_APP_ID "你的沙盒APPID"
setx ALIPAY_PRIVATE_KEY "你的应用私钥"
setx ALIPAY_PUBLIC_KEY "支付宝沙盒公钥"
setx ALIPAY_RETURN_URL "http://127.0.0.1:5173/payment/result"
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

### 2.2.1 支付沙盒说明
- 本地联调路径是：创建挂号 -> 返回 `PAYING` -> 拉起支付宝页支付 -> 浏览器回跳 `http://127.0.0.1:5173/payment/result` -> 前端主动调用后端查单确认结果。
- 如果你只有本地 `127.0.0.1` 地址，没有公网回调域名，也能完成联调；这时主要依赖回跳页主动查单，不依赖支付宝异步通知。
- 真实拉起沙盒支付页至少需要：`ALIPAY_ENABLED=true`、`ALIPAY_APP_ID`、`ALIPAY_PRIVATE_KEY`、`ALIPAY_PUBLIC_KEY`。
- 如果这些参数没配，后端会明确返回“支付宝沙盒配置不完整，无法发起支付”，这是保护逻辑，不是代码异常。
- 如果你有公网域名或内网穿透地址，可以额外配置 `ALIPAY_NOTIFY_URL=https://你的域名/treat/pay/alipay/notify`，让支付成功后由支付宝异步回调落单。

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

> local profile 当前使用 MySQL（见 `medical-back/src/main/resources/application-local.yml`）；请确保你配置的 MySQL 可用并已导入表结构/数据。
> 如果你的数据库是旧版本表结构，先执行：`scripts/20260312_registration_person_key.sql`
> 如果你要验证支付宝沙盒，另外执行：`scripts/20260320_registration_payment_order.sql`
> 当前挂号创建依赖真实 Redis 和 RabbitMQ；Redis 内存兜底不覆盖这条链路。
> 另外，local profile 已开启排班补齐与挂号状态对账（`app.schedule.enabled=true`、`app.registration.reconcile-enabled=true`）；如果你只想“最小启动”而不跑这些后台补偿逻辑，可以在 local 配置里把它们关掉。
> local profile 下前端可直接走 `GET /front/loginAndOut/devToken` 获取本地 JWT。患者端建议使用 `type=1`；医生端与管理员端建议使用 `type=2`。当前种子数据没有可用的 `type=3` 管理员账号，因此管理员前端本地联调默认复用医生账号上下文。

### 2.4.1 本地登录建议
- 患者端：启动后在登录页使用“本地直连”，会取第一个启用的患者账号。
- 医生端：启动后在登录页使用“本地直连”，会取第一个启用的医生账号。
- 管理端：启动后在登录页使用“本地直连”，当前会复用第一个启用的医生账号，仅用于本地功能联调。
- 如果前端目录已有损坏或过期的 `node_modules`，优先执行 `npm ci` 再启动，不要继续沿用旧依赖树。

### 2.5 MinIO（本地上传）
local profile 默认 MinIO 配置为：
- `endpoint=http://localhost:9000`
- `bucket=medical`
- 控制台：`http://localhost:9002`（默认 `minioadmin/minioadmin`，映射容器控制台 `9001`）

建议先运行：`scripts/start-minio.bat`，或用 Docker 命令启动 MinIO（见上文）。

MinIO 环境变量（可选覆盖）：
- `MINIO_ENDPOINT`（默认 `http://localhost:9000`）
- `MINIO_ACCESS_KEY`（默认 `minioadmin`）
- `MINIO_SECRET_KEY`（默认 `minioadmin`）
- `MINIO_BUCKET`（默认 `medical`）
- `MINIO_PUBLIC_URL_PREFIX`（默认 `http://localhost:9000/medical`）

#### 2.5.1 AI 配置自检（推荐先做）
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

#### 2.5.2 AI 治理参数（可选）
后端新增 AI 请求治理（截断/脱敏/限流/审计日志），配置项在 `application.yml`：
- `ai.guard.mask-enabled`：是否开启脱敏
- `ai.guard.audit-enabled`：是否开启审计日志
- `ai.guard.rate-limit-enabled`：是否开启限流
- `ai.guard.rate-limit-window-seconds`：限流窗口（秒）
- `ai.guard.rate-limit-max-requests`：窗口内最大请求数
- `ai.guard.max-*-chars` 与 `ai.guard.max-symptom-count`：截断阈值

触发限流时会返回 `code=9002`。

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
- 默认走 Spring WebSocket：`ws://localhost:8080/treat/ws/chat/{roomId}`

前端可通过环境变量覆盖（可选）：
- `VITE_WS_BASE=localhost:8080`
- `VITE_WS_PATH=/treat/ws/chat`

后端开关：
- `app.websocket.path=/ws/chat`
- `app.websocket.patient-response-timeout-minutes=3`

验证清单（端到端）：
- 医生发起问诊 -> 患者弹窗 -> 接受 -> 正常聊天收发。
- 患者不响应等待超时（默认 3 分钟）-> 医生收到 `patient_timeout` 通知。
- 医生结束问诊 -> 患者端收到结束与断开提示。

压测建议：
- 使用 `k6`/`wrk`/`websocat` 等工具对 WS 建连数、广播延迟、断线重连进行测试。
