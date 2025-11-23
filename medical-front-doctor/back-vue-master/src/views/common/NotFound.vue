<template>
  <div class="not-found-container">
    <div class="not-found-content">
      <div class="error-code">404</div>
      <div class="error-title">页面不存在</div>
      <div class="error-desc">抱歉，您访问的页面不存在或已被删除</div>
      <el-button type="primary" class="back-button" @click="goBack">
        <el-icon><Back /></el-icon>
        返回上一页
      </el-button>
      <el-button type="default" class="home-button" @click="goHome">
        <el-icon><HomeFilled /></el-icon>
        返回首页
      </el-button>
    </div>
    
    <div class="not-found-image">
      <div class="astronaut">
        <div class="astronaut-body"></div>
        <div class="astronaut-head"></div>
        <div class="astronaut-arm-left"></div>
        <div class="astronaut-arm-right"></div>
        <div class="astronaut-leg-left"></div>
        <div class="astronaut-leg-right"></div>
      </div>
      <div class="planet"></div>
      <div class="stars">
        <div v-for="i in 20" :key="i" class="star" :style="getRandomStarStyle()"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { Back, HomeFilled } from '@element-plus/icons-vue';

const router = useRouter();
const userStore = useUserStore();

function goBack() {
  router.go(-1);
}

function goHome() {
  const isDoctor = userStore.isDoctor;
  const isAdmin = userStore.isAdmin;
  const isLoggedIn = userStore.isLoggedIn;
  
  if (!isLoggedIn) {
    router.push('/login');
  } else if (isDoctor) {
    router.push('/doctor');
  } else if (isAdmin) {
    router.push('/admin');
  } else {
    router.push('/login');
  }
}

function getRandomStarStyle() {
  const size = Math.floor(Math.random() * 4) + 1;
  const left = Math.floor(Math.random() * 100);
  const top = Math.floor(Math.random() * 100);
  const animationDuration = Math.floor(Math.random() * 3) + 1;
  const animationDelay = Math.random() * 2;
  
  return {
    width: `${size}px`,
    height: `${size}px`,
    left: `${left}%`,
    top: `${top}%`,
    animationDuration: `${animationDuration}s`,
    animationDelay: `${animationDelay}s`
  };
}
</script>

<style scoped>
.not-found-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  width: 100%;
  background-color: #1a1a2e;
  overflow: hidden;
  position: relative;
}

.not-found-content {
  z-index: 10;
  text-align: center;
  color: #fff;
  padding: 0 20px;
  max-width: 500px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 20px;
  background: linear-gradient(45deg, #1890ff, #52c41a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

.error-title {
  font-size: 32px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #f0f0f0;
}

.error-desc {
  font-size: 16px;
  color: #a0a0a0;
  margin-bottom: 30px;
}

.back-button,
.home-button {
  margin: 0 10px;
  transition: all 0.3s;
}

.back-button:hover,
.home-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.not-found-image {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.stars {
  position: absolute;
  width: 100%;
  height: 100%;
}

.star {
  position: absolute;
  background-color: #fff;
  border-radius: 50%;
  animation: twinkle ease-in-out infinite;
}

@keyframes twinkle {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.planet {
  position: absolute;
  bottom: -100px;
  right: -100px;
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle at 30% 30%, #4b6cb7, #182848);
  box-shadow: 0 0 50px rgba(75, 108, 183, 0.5);
  animation: rotate 100s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.astronaut {
  position: absolute;
  top: 50%;
  right: 20%;
  animation: float 6s ease-in-out infinite;
}

@keyframes float {
  0% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
  100% { transform: translateY(0) rotate(0deg); }
}

.astronaut-body {
  width: 50px;
  height: 80px;
  background-color: #f0f0f0;
  border-radius: 20px;
  position: relative;
}

.astronaut-head {
  width: 40px;
  height: 40px;
  background-color: #f0f0f0;
  border-radius: 50%;
  position: absolute;
  top: -30px;
  left: 5px;
  border: 5px solid #d0d0d0;
}

.astronaut-arm-left,
.astronaut-arm-right {
  width: 20px;
  height: 60px;
  background-color: #f0f0f0;
  border-radius: 10px;
  position: absolute;
}

.astronaut-arm-left {
  left: -15px;
  top: 10px;
  transform: rotate(20deg);
  animation: wave 2s ease-in-out infinite;
}

@keyframes wave {
  0% { transform: rotate(20deg); }
  50% { transform: rotate(40deg); }
  100% { transform: rotate(20deg); }
}

.astronaut-arm-right {
  right: -15px;
  top: 10px;
  transform: rotate(-20deg);
}

.astronaut-leg-left,
.astronaut-leg-right {
  width: 20px;
  height: 40px;
  background-color: #f0f0f0;
  border-radius: 10px;
  position: absolute;
  bottom: -30px;
}

.astronaut-leg-left {
  left: 5px;
}

.astronaut-leg-right {
  right: 5px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .error-code {
    font-size: 100px;
  }
  
  .error-title {
    font-size: 28px;
  }
  
  .astronaut {
    right: 10%;
    transform: scale(0.8);
  }
  
  .planet {
    width: 200px;
    height: 200px;
  }
}

@media (max-width: 576px) {
  .error-code {
    font-size: 80px;
  }
  
  .error-title {
    font-size: 24px;
  }
  
  .back-button,
  .home-button {
    display: block;
    margin: 10px auto;
    width: 80%;
  }
  
  .astronaut {
    display: none;
  }
}
</style>