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
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-main {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background-color: transparent;
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
</style>
