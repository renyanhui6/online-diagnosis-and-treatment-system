<template>
  <el-aside :width="isCollapse ? '64px' : '220px'" class="app-sidebar">
    <div class="logo-container">
      <img src="@/assets/logo.svg" alt="Logo" class="logo" />
      <h1 class="title" v-show="!isCollapse">{{ title }}</h1>
    </div>
    
    <el-scrollbar>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <template v-for="(menu, index) in menus" >
          <!-- 无子菜单 -->
          <el-menu-item v-if="!menu.children" :key="index" :index="menu.path">
            <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
            <template #title>
              <span>{{ menu.title }}</span>
            </template>
          </el-menu-item>
          
          <!-- 有子菜单 -->
          <el-sub-menu v-else :index="menu.path">
            <template #title>
              <el-icon v-if="menu.icon"><component :is="menu.icon" /></el-icon>
              <span>{{ menu.title }}</span>
            </template>
            
            <el-menu-item 
              v-for="(subMenu, subIndex) in menu.children" 
              :key="subIndex"
              :index="subMenu.path"
            >
              <el-icon v-if="subMenu.icon"><component :is="subMenu.icon" /></el-icon>
              <template #title>
                <span>{{ subMenu.title }}</span>
              </template>
            </el-menu-item>
          </el-sub-menu>
        </template>
      </el-menu>
    </el-scrollbar>
    
    <div class="sidebar-footer" v-if="!isCollapse">
      <div class="version">v1.0.0</div>
      <div class="copyright">© {{ new Date().getFullYear() }} 医院管理系统</div>
    </div>
  </el-aside>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useUserStore } from '@/stores/user';
import {
  HomeFilled, User, DataLine, Calendar, ChatDotRound, Document
} from '@element-plus/icons-vue';

const props = defineProps({
  isCollapse: {
    type: Boolean,
    default: false
  }
});

const route = useRoute();
const userStore = useUserStore();

const isDoctor = computed(() => userStore.isDoctor);
const isAdmin = computed(() => userStore.isAdmin);

const title = computed(() => {
  return isDoctor.value ? '医生工作站' : '管理员控制台';
});

// 医生菜单
const doctorMenus = [
  {
    path: '/doctor/appointments',
    title: '我的排班',
    icon: 'Calendar'
  },
  {
    path: '/doctor/consultations',
    title: '在线问诊',
    icon: 'ChatDotRound'
  },
  {
    path: '/doctor/medical-records',
    title: '就诊记录管理',
    icon: 'Document'
  },
  {
    path: '/doctor/profile',
    title: '个人信息',
    icon: 'User'
  }
];

// 管理员菜单
const adminMenus = [
  {
    path: '/admin',
    title: '仪表盘',
    icon: 'HomeFilled'
  },
  {
    path: '/admin/statistics',
    title: '统计报表',
    icon: 'DataLine'
  }
];

const menus = computed(() => {
  return isDoctor.value ? doctorMenus : adminMenus;
});

const activeMenu = computed(() => {
  return route.path;
});
</script>

<style scoped>
.app-sidebar {
  background-color: #001529;
  height: 100vh;
  transition: width 0.3s;
  position: relative;
  overflow: hidden;
  box-shadow: 2px 0 6px rgba(0, 21, 41, 0.35);
  z-index: 20;
}

.logo-container {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  overflow: hidden;
  background-color: #002140;
}

.logo {
  height: 32px;
  width: 32px;
  margin-right: 12px;
}

.title {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  margin: 0;
}

.sidebar-menu {
  border-right: none;
  background-color: #001529;
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.65);
  height: 50px;
  line-height: 50px;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background-color: #1890ff;
}

.sidebar-menu :deep(.el-menu-item:hover),
.sidebar-menu :deep(.el-sub-menu__title:hover) {
  color: #fff;
  background-color: #001528;
}

.sidebar-footer {
  position: absolute;
  bottom: 0;
  width: 100%;
  padding: 16px;
  text-align: center;
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
  background-color: #00101f;
}

.version {
  margin-bottom: 4px;
}

/* 动画效果 */
.logo {
  transition: all 0.3s;
}

.logo-container:hover .logo {
  transform: rotate(360deg);
}

.sidebar-menu :deep(.el-menu-item),
.sidebar-menu :deep(.el-sub-menu__title) {
  position: relative;
  overflow: hidden;
}

.sidebar-menu :deep(.el-menu-item)::before,
.sidebar-menu :deep(.el-sub-menu__title)::before {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 0;
  height: 2px;
  background-color: #1890ff;
  transition: width 0.3s ease;
}

.sidebar-menu :deep(.el-menu-item:hover)::before,
.sidebar-menu :deep(.el-sub-menu__title:hover)::before,
.sidebar-menu :deep(.el-menu-item.is-active)::before {
  width: 100%;
}
</style>