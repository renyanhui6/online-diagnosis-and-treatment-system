<template>
  <div class="app-container">
    <app-sidebar :is-collapse="isCollapse" />
    
    <div class="main-container">
      <app-header v-model:is-collapse="isCollapse" />
      
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive>
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import AppSidebar from '@/components/layout/AppSidebar.vue';
import AppHeader from '@/components/layout/AppHeader.vue';

const isCollapse = ref(false);
</script>

<style scoped>
.app-container {
  display: flex;
  width: 100%;
  height: 100vh;
  overflow: hidden;
  position: relative;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.app-main {
  flex: 1;
  padding: 24px 28px 28px;
  overflow-y: auto;
  background-color: transparent;
  position: relative;
}

.app-main::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at 12% 8%, rgba(37, 99, 235, 0.08) 0%, transparent 28%),
    radial-gradient(circle at 90% 16%, rgba(59, 130, 246, 0.07) 0%, transparent 26%);
}

.app-main :deep(> *) {
  position: relative;
  z-index: 1;
}

/* 页面切换动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

@media (max-width: 1024px) {
  .app-main {
    padding: 20px;
  }
}
</style>
