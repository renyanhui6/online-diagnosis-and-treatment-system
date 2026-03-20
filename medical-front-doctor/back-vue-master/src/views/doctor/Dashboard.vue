<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <div class="welcome-card">
        <div class="welcome-info">
          <img :src="avatarUrl" alt="Avatar" class="avatar" />
          <div class="welcome-text">
            <h2>{{ greeting }}，{{ displayName }}</h2>
            <p>今天是 {{ currentDate }}</p>
          </div>
        </div>
        <div class="quick-actions">
          <el-button type="primary" @click="router.push('/doctor/consultations')">
            <el-icon><Calendar /></el-icon>
            挂号列表
          </el-button>
          <el-button type="success" @click="router.push('/doctor/consultations')">
            <el-icon><ChatDotRound /></el-icon>
            在线问诊
          </el-button>
          <el-button type="warning" @click="router.push('/doctor/medical-records')">
            <el-icon><Tickets /></el-icon>
            病历管理
          </el-button>
        </div>
      </div>
    </div>

    <el-row :gutter="20" class="data-overview">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon today-appointments">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">挂号总数</div>
            <div class="data-value">{{ dashboardData.totalCount }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon waiting-consultations">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">待接诊</div>
            <div class="data-value">{{ dashboardData.pendingCount }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon prescriptions">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">问诊中</div>
            <div class="data-value">{{ dashboardData.inProgressCount }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon satisfaction">
            <el-icon><StarFilled /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">已完成</div>
            <div class="data-value">{{ dashboardData.completedCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import { Calendar, ChatDotRound, Tickets, StarFilled } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getAllRegistrationInfoList } from '@/api/doctor';

const router = useRouter();
const userStore = useUserStore();
const userInfo = computed(() => userStore.userInfo);
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '医生');
const avatarUrl = computed(() => userInfo.value.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png');

const currentDate = computed(() => {
  const now = new Date();
  const options = { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' };
  return now.toLocaleDateString('zh-CN', options);
});

const greeting = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '凌晨好';
  if (hour < 9) return '早上好';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 17) return '下午好';
  if (hour < 19) return '傍晚好';
  if (hour < 22) return '晚上好';
  return '夜深了';
});

const dashboardData = reactive({
  totalCount: 0,
  pendingCount: 0,
  inProgressCount: 0,
  completedCount: 0
});

const normalizeRegistrationList = (data) => {
  if (!data) return [];
  if (Array.isArray(data)) return data;
  if (Array.isArray(data.records)) return data.records;
  if (Array.isArray(data.list)) return data.list;
  return [];
};

const fetchDashboardData = async () => {
  try {
    const response = await getAllRegistrationInfoList({ pageNum: 1, pageSize: 1000 });
    if (response.code !== 200) {
      ElMessage.error(response.message || '获取统计失败');
      return;
    }

    const list = normalizeRegistrationList(response.data);
    dashboardData.totalCount = list.length;
    dashboardData.pendingCount = list.filter(item => item.registrationStatus === 2 || item.registrationStatus === 7).length;
    dashboardData.inProgressCount = list.filter(item => item.registrationStatus === 3).length;
    dashboardData.completedCount = list.filter(item => item.registrationStatus === 4).length;
  } catch (error) {
    console.error('获取统计失败:', error);
    ElMessage.error('获取统计失败，请稍后重试');
  }
};

onMounted(async () => {
  await userStore.fetchUserInfo();
  await fetchDashboardData();
});
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}

.welcome-section {
  margin-bottom: 20px;
}

.welcome-card {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(16, 185, 129, 0.92));
  border-radius: var(--app-radius);
  padding: 20px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--app-shadow);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.welcome-info {
  display: flex;
  align-items: center;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 3px solid rgba(255, 255, 255, 0.5);
  margin-right: 16px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
}

.welcome-text h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 500;
}

.welcome-text p {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}

.quick-actions {
  display: flex;
  gap: 12px;
}

.data-overview {
  margin-bottom: 20px;
}

.data-card {
  height: 120px;
  display: flex;
  align-items: center;
  padding: 20px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.data-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--app-shadow);
}

.data-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 24px;
  color: #fff;
}

.today-appointments {
  background-color: var(--brand-600);
}

.waiting-consultations {
  background-color: var(--el-color-success);
}

.prescriptions {
  background-color: var(--brand-700);
}

.satisfaction {
  background-color: var(--el-color-danger);
}

.data-info {
  flex: 1;
}

.data-title {
  font-size: 14px;
  color: rgba(15, 23, 42, 0.55);
  margin-bottom: 8px;
}

.data-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--app-text);
  margin-bottom: 8px;
}

@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }

  .quick-actions {
    flex-wrap: wrap;
  }
}
</style>
