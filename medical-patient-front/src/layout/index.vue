<template>
  <div class="app-container">
    <!-- 侧边栏 -->
    <div class="sidebar glass-panel">
      <div class="logo">
        <img src="../assets/logo.svg" alt="医院logo" />
        <h2>医院预约挂号系统</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="transparent"
        text-color="#ffffff"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/home">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        
        <el-sub-menu index="/appointment">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>预约挂号</span>
          </template>
          <el-menu-item index="/appointment">
            <el-icon><Plus /></el-icon>
            <span>预约挂号</span>
          </el-menu-item>
          <el-menu-item index="/appointment/list">
            <el-icon><List /></el-icon>
            <span>我的预约</span>
          </el-menu-item>
        </el-sub-menu>
        
        <el-menu-item index="/record">
          <el-icon><Document /></el-icon>
          <span>就诊记录</span>
        </el-menu-item>
        
        <el-sub-menu index="/user">
          <template #title>
            <el-icon><User /></el-icon>
            <span>个人中心</span>
          </template>
          <el-menu-item index="/user">
            <el-icon><UserFilled /></el-icon>
            <span>个人信息</span>
          </el-menu-item>
          <el-menu-item index="/user/verification">
            <el-icon><Lock /></el-icon>
            <span>实名认证</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
      
      <!-- 装饰元素 -->
      <div class="tech-circle-container">
        <img src="../assets/tech-circle.svg" alt="" class="tech-circle" />
      </div>
    </div>
    
    <!-- 主内容区 -->
    <div class="main-container">
      <!-- 头部 -->
      <div class="header glass-panel">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" :src="userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
              <span class="username">{{ userInfo.name || userInfo.username || '用户' }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      
      <!-- 内容区 -->
      <div class="content glass-panel">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <keep-alive>
              <component :is="Component" class="animate__animated animate__fadeIn" />
            </keep-alive>
          </transition>
        </router-view>
      </div>
      
      <!-- 问诊通知组件 -->
      <ConsultationNotification />
      
      <!-- 底部 -->
      <div class="footer glass-panel">
        <p>© {{ new Date().getFullYear() }} 医院在线预约挂号系统 版权所有</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import UserStorage from '../utils/userStorage'
import { ElMessageBox } from 'element-plus'
import ConsultationNotification from '../components/ConsultationNotification.vue'
import {
  HomeFilled,
  Calendar,
  Plus,
  List,
  Document,
  User,
  UserFilled,
  Lock,
  ArrowDown,
  SwitchButton,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 用户信息 - 使用响应式数据
const userInfo = ref(UserStorage.getUserInfo())

// 监听用户信息变化（可以通过定时器或事件来更新）
const updateUserInfo = () => {
  userInfo.value = UserStorage.getUserInfo()
}

// 当前激活的菜单
const activeMenu = computed(() => {
  return route.path
})

// 下拉菜单命令处理
const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/user')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      UserStorage.clearUserData()
      router.push('/login')
    }).catch(() => {})
  }
}

// 获取用户信息
onMounted(() => {
  // 初始化时更新用户信息
  updateUserInfo()
})
</script>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
  width: 100%;
  position: relative;
  z-index: 1;
}

.main-container {
  flex: 1;
  margin-left: 250px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  padding: 15px;
  width: calc(100% - 250px);
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* 磨砂玻璃面板 */
.glass-panel {
  background-color: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-md);
  transition: var(--transition);
}

/* 头部样式 */
.header {
  height: 60px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  margin-bottom: 15px;
  width: 100%;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.86) 0%, rgb(var(--primary-100-rgb) / 0.55) 100%);
}

.header:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

/* 内容区样式 */
.content {
  flex: 1;
  border-radius: var(--radius-xl);
  padding: 20px;
  margin-bottom: 15px;
  overflow-y: auto;
  width: 100%;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.86) 0%, rgb(var(--primary-50-rgb) / 0.55) 100%);
}

.content:hover {
  box-shadow: var(--shadow-lg);
}

/* 底部样式 */
.footer {
  height: 50px;
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  color: var(--neutral-500);
  font-size: 14px;
  width: 100%;
  box-sizing: border-box;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.86) 0%, rgb(var(--primary-100-rgb) / 0.55) 100%);
}

.footer:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-2px);
}

/* 侧边栏样式 */
.sidebar {
  width: 250px;
  background: linear-gradient(180deg, var(--primary-600) 0%, var(--primary-700) 50%, var(--primary-800) 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xl);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 10;
  overflow-y: auto;
  overflow-x: hidden;
  border-radius: 0 var(--radius-xl) var(--radius-xl) 0;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(45deg, rgba(255, 255, 255, 0.1) 0%, transparent 50%);
  pointer-events: none;
}

/* Logo样式 */
.logo {
  padding: 20px;
  text-align: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;
}

.logo::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: pulse 10s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.5; }
  50% { transform: scale(1.2); opacity: 0.2; }
  100% { transform: scale(1); opacity: 0.5; }
}

.logo img {
  width: 50px;
  height: 50px;
  margin-bottom: 10px;
  filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.5));
}

.logo h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

/* 菜单样式 */
.sidebar-menu {
  border-right: none;
  background-color: transparent !important;
  flex: 1;
}

.el-menu-item, .el-sub-menu__title {
  color: rgba(255, 255, 255, 0.9) !important;
  height: 50px;
  line-height: 50px;
  position: relative;
  overflow: hidden;
}

.el-menu-item::before, .el-sub-menu__title::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: all 0.6s;
}

.el-menu-item:hover::before, .el-sub-menu__title:hover::before {
  left: 100%;
}

.el-menu-item:hover, .el-sub-menu__title:hover {
  background: rgba(255, 255, 255, 0.15) !important;
}

.el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.2) !important;
  color: #ffffff !important;
  position: relative;
}

.el-menu-item.is-active::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: #ffffff;
  box-shadow: 0 0 10px rgba(255, 255, 255, 0.8);
}

/* 用户信息样式 */
.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: var(--radius-full);
  background: rgb(var(--primary-50-rgb) / 0.2);
  transition: var(--transition);
}

.user-info:hover {
  background: rgb(var(--primary-50-rgb) / 0.3);
}

.username {
  font-weight: 500;
  color: var(--primary-800);
}

/* 装饰性圆环 */
.tech-circle-container {
  position: absolute;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 150px;
  height: 150px;
  opacity: 0.2;
  pointer-events: none;
}

.tech-circle {
  width: 100%;
  height: 100%;
}

/* 页面过渡动画 */
.page-enter-active, .page-leave-active {
  transition: all 0.3s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.page-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .sidebar {
    width: 200px;
  }
  
  .main-container {
    margin-left: 200px;
    width: calc(100% - 200px);
    padding: 10px;
  }
  
  .header, .content, .footer {
    padding: 10px;
  }
  
  .logo h2 {
    font-size: 16px;
  }
}
</style>
