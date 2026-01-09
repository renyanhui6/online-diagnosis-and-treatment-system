# Online-Diagnosis-and-Treatment-System

#### 介绍
基于java的在线诊疗系统

#### 软件架构
软件架构说明


#### 安装教程
**后端启动（推荐本地 profile）**
1. 安装 JDK 17（或以上）与 Maven（也可使用 `./mvnw`）。
2. 进入后端目录：`cd medical-back`
3. 配置 DeepSeek（可选，但 AI 功能需要）：
   - Linux/macOS：`export DEEPSEEK_API_KEY=你的key`
   - Windows PowerShell：`setx DEEPSEEK_API_KEY "你的key"`
4. 本地启动（不依赖 MySQL 也可启动）：`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`
   - 访问：`http://localhost:8080/treat`

**说明**
- `local` profile 使用 H2 内存库用于“可启动/可联调”，但业务数据表需要你自行导入/初始化，否则接口会在查询阶段报错。
- 如需完整功能（预约/支付/消息队列/Redis 过期事件等），请准备 MySQL + Redis + RabbitMQ 并修改配置或通过环境变量覆盖。

#### 使用说明
- DeepSeek 配置项：
  - `DEEPSEEK_API_KEY`：API Key（不要提交到仓库）
  - `DEEPSEEK_MODEL`：默认 `deepseek-chat`
  - `DEEPSEEK_BASE_URL`：默认 `https://api.deepseek.com`
  - `DEEPSEEK_TIMEOUT_SECONDS`：默认 `20`

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


#### 特技

1.  使用 Readme\_XXX.md 来支持不同的语言，例如 Readme\_en.md, Readme\_zh.md
2.  Gitee 官方博客 [blog.gitee.com](https://blog.gitee.com)
3.  你可以 [https://gitee.com/explore](https://gitee.com/explore) 这个地址来了解 Gitee 上的优秀开源项目
4.  [GVP](https://gitee.com/gvp) 全称是 Gitee 最有价值开源项目，是综合评定出的优秀开源项目
5.  Gitee 官方提供的使用手册 [https://gitee.com/help](https://gitee.com/help)
6.  Gitee 封面人物是一档用来展示 Gitee 会员风采的栏目 [https://gitee.com/gitee-stars/](https://gitee.com/gitee-stars/)
