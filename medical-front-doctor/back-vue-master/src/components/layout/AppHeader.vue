<template>
  <el-header class="app-header">
    <div class="header-left">
      <el-button 
        type="text" 
        class="toggle-sidebar" 
        @click="toggleSidebar"
      >
        <el-icon :size="20">
          <component :is="isCollapse ? 'Expand' : 'Fold'" />
        </el-icon>
      </el-button>
      <div class="breadcrumb" v-if="route.meta.title">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: homeRoute }">
            {{ isDoctor ? '医生工作台' : '管理员控制台' }}
          </el-breadcrumb-item>
          <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <div class="header-note">{{ isDoctor ? '当日接诊与病历协同' : '基础维护与资源治理' }}</div>
    </div>
    
    <div class="header-right">
      <el-tooltip content="全屏" placement="bottom">
        <el-button 
          type="text" 
          class="header-icon" 
          @click="toggleFullScreen"
        >
          <el-icon :size="18">
            <FullScreen />
          </el-icon>
        </el-button>
      </el-tooltip>
      
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar :size="32" :src="userInfo.avatar" />
          <span class="username">{{ userInfo.name }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="isDoctor" command="profile">
              <el-icon><User /></el-icon>
              <span>个人信息</span>
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>
              <span>退出登录</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { 
  Expand, Fold, FullScreen, 
  ArrowDown, User, SwitchButton 
} from '@element-plus/icons-vue';

const props = defineProps({
  isCollapse: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:isCollapse']);

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const userInfo = computed(() => {
  // 优先从localStorage获取用户名
  const username = localStorage.getItem('username');
  const storedUserInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
  
  return {
    name: username || storedUserInfo.realName || storedUserInfo.username || storedUserInfo.name || '用户',
    avatar: storedUserInfo.avatar || 'https://randomuser.me/api/portraits/men/32.jpg'
  };
});
const isDoctor = computed(() => userStore.isDoctor);
const isAdmin = computed(() => userStore.isAdmin);

const homeRoute = computed(() => {
  return isDoctor.value ? '/doctor' : '/admin';
});

function toggleSidebar() {
  emit('update:isCollapse', !props.isCollapse);
}

function toggleFullScreen() {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    }
  }
}

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout();
    router.push('/login');
  } else if (command === 'profile') {
    router.push('/doctor/profile');
  }
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 72px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.82) 0%, rgba(255, 255, 255, 0.68) 100%);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--app-border);
  box-shadow: var(--app-shadow-sm);
  padding: 0 22px;
  position: relative;
  z-index: 10;
  margin: 16px 20px 0;
  border-radius: 18px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.toggle-sidebar {
  margin-right: 15px;
  font-size: 20px;
}

.breadcrumb {
  margin-left: 4px;
}

.header-note {
  font-size: 12px;
  color: var(--app-text-muted);
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
}

.header-right {
  display: flex;
  align-items: center;
}

.header-icon {
  padding: 0 10px;
  font-size: 18px;
  color: var(--app-text-muted);
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 10px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.66);
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: var(--app-text);
  font-weight: 600;
}

/* 添加动画效果 */
.toggle-sidebar {
  transition: transform 0.3s;
}

.toggle-sidebar:hover {
  transform: scale(1.1);
}

.header-icon {
  transition: all var(--app-transition);
}

.header-icon:hover {
  color: var(--el-color-primary);
  transform: scale(1.1);
}

.user-info {
  transition: background-color var(--app-transition);
  border-radius: 10px;
}

.user-info:hover {
  background-color: rgba(37, 99, 235, 0.06);
}

@media (max-width: 1024px) {
  .app-header {
    margin: 12px 14px 0;
    padding: 0 16px;
  }

  .header-note {
    display: none;
  }
}
</style>
