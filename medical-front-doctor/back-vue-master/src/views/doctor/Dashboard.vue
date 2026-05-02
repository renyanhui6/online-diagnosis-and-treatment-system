<template>
  <div class="dashboard-container">
    <div class="hero-grid">
      <div class="welcome-card">
        <div class="welcome-info">
          <img :src="avatarUrl" alt="Avatar" class="avatar" />
          <div class="welcome-text">
            <div class="hero-tag">临床工作台</div>
            <h2>{{ greeting }}，{{ displayName }}</h2>
            <p>今天是 {{ currentDate }}，重点查看挂号队列、正在接诊的患者和病历收口情况。</p>
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

      <div class="hero-side-card">
        <div class="hero-side-label">今日重点</div>
        <div class="hero-side-value">{{ dashboardData.pendingCount }} 位待接诊</div>
        <div class="hero-side-hint">问诊中 {{ dashboardData.inProgressCount }} 人，已完成 {{ dashboardData.completedCount }} 人。</div>
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
const avatarUrl = computed(() => userInfo.value.avatar || '/thesis-assets/avatars/doctor.svg');

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

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(260px, 0.9fr);
  gap: 18px;
  margin-bottom: 22px;
}

.welcome-card {
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(16, 185, 129, 0.9));
  border-radius: 24px;
  padding: 26px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: var(--app-shadow-lg);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.welcome-info {
  display: flex;
  align-items: center;
}

.hero-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
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
  margin: 12px 0 8px 0;
  font-size: 28px;
  font-weight: 700;
}

.welcome-text p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.92;
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.hero-side-card {
  border-radius: 24px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.72));
  border: 1px solid var(--app-border);
  box-shadow: var(--app-shadow-sm);
}

.hero-side-label {
  font-size: 13px;
  color: var(--app-text-muted);
}

.hero-side-value {
  margin-top: 10px;
  font-size: 26px;
  font-weight: 700;
  color: var(--app-text);
}

.hero-side-hint {
  margin-top: 12px;
  color: var(--app-text-muted);
  line-height: 1.7;
  font-size: 14px;
}

.data-overview {
  margin-bottom: 20px;
}

.data-card {
  height: 132px;
  display: flex;
  align-items: center;
  padding: 22px;
  margin-bottom: 20px;
  transition: all 0.3s;
  border-radius: 22px;
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

@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
  }

  .quick-actions {
    justify-content: flex-start;
  }
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
