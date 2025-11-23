# 医院挂号预约系统 - 患者端

## 项目介绍
医院挂号预约系统患者端前端代码，基于Vue3 + Element Plus构建的现代化医疗服务平台。

## 技术栈
- **前端框架**: Vue 3
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **路由管理**: Vue Router
- **HTTP客户端**: Axios
- **构建工具**: Vite
- **开发语言**: JavaScript

## 主要功能
- 用户注册/登录
- 科室医生查询
- 在线挂号预约
- 药品购买
- 在线问诊聊天
- 支付管理
- 订单管理
- 个人信息管理

## 安装运行

### 环境要求
- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖
```bash
npm install
```

### 开发环境运行
```bash
npm run dev
```

### 生产环境构建
```bash
npm run build
```

### 预览构建结果
```bash
npm run preview
```

## 项目结构
```
src/
├── api/           # API接口
├── components/    # 公共组件
├── layouts/       # 布局组件
├── router/        # 路由配置
├── stores/        # 状态管理
├── utils/         # 工具函数
├── views/         # 页面组件
└── main.js        # 入口文件
```

## 开发规范
- 使用ES6+语法
- 组件采用组合式API
- 遵循Vue3官方风格指南
- 使用Element Plus组件库
