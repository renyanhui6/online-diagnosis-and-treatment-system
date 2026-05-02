<template>
  <div class="dashboard-container">
    <div class="hero-grid">
      <div class="welcome-card">
        <div class="welcome-info">
          <img :src="avatarUrl" alt="Avatar" class="avatar" />
          <div class="welcome-text">
            <div class="hero-tag">管理首页</div>
            <h2>{{ greeting }}，{{ displayName }}</h2>
            <p>今天是 {{ currentDate }}，这里仅保留后台治理入口，优先处理主数据维护、账号状态控制和排班模板补偿。</p>
          </div>
        </div>
      </div>

      <div class="hero-side-card">
        <div class="hero-side-label">当前定位</div>
        <div class="hero-side-value">基础维护</div>
        <div class="hero-side-hint">不展示统计看板，不做聚合报表，只保留必要后台治理功能。</div>
      </div>
    </div>

    <el-row :gutter="20" class="module-grid">
      <el-col
        v-for="module in modules"
        :key="module.path"
        :xs="24"
        :sm="12"
        :lg="8"
      >
        <el-card class="module-card" shadow="hover" @click="goTo(module.path)">
          <div class="module-chip">{{ module.badge }}</div>
          <h3>{{ module.title }}</h3>
          <p>{{ module.description }}</p>
          <div class="module-footer">
            <span>{{ module.tip }}</span>
            <el-button type="primary" text @click.stop="goTo(module.path)">进入模块</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="notice-card" shadow="never">
      <div class="notice-title">当前收口</div>
      <ul class="notice-list">
        <li>管理首页不再拉医生、患者、药品和排班模板统计接口。</li>
        <li>所有数据查看、筛选和状态操作都下沉到对应业务页内完成。</li>
        <li>后台只覆盖科室、医生、患者、药品和排班模板这五类必要治理对象。</li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';

const router = useRouter();
const userStore = useUserStore();
const userInfo = computed(() => userStore.userInfo);
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '管理员');
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

const modules = [
  {
    path: '/admin/doctors',
    badge: '账号治理',
    title: '医生管理',
    description: '维护医生账号、详情、状态启停和删除回收。',
    tip: '处理执业信息与可用状态'
  },
  {
    path: '/admin/patients',
    badge: '就诊对象',
    title: '患者管理',
    description: '维护患者账号及主就诊人资料，支撑挂号与病历链路。',
    tip: '处理主就诊人资料与账号状态'
  },
  {
    path: '/admin/departments',
    badge: '主数据',
    title: '科室管理',
    description: '维护科室与子科室信息，保证分诊与挂号入口口径一致。',
    tip: '处理科室结构与展示信息'
  },
  {
    path: '/admin/medicines',
    badge: '处方支撑',
    title: '药品管理',
    description: '维护药品基础信息，保证医生开方与患者回看使用同一数据源。',
    tip: '处理药品主数据与库存字段'
  },
  {
    path: '/admin/schedule-templates',
    badge: '排班维护',
    title: '排班模板',
    description: '维护排班模板与补偿入口，确保后续挂号可持续生成。',
    tip: '处理模板启停与补缺补偿'
  }
];

const goTo = (path) => {
  router.push(path);
};

onMounted(async () => {
  await userStore.fetchUserInfo();
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
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.95), rgba(29, 78, 216, 0.84));
  border-radius: 24px;
  padding: 26px;
  color: #fff;
  display: flex;
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
  margin: 12px 0 8px;
  font-size: 28px;
  font-weight: 700;
}

.welcome-text p {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  opacity: 0.92;
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

.module-grid {
  margin-bottom: 20px;
}

.module-card {
  min-height: 220px;
  margin-bottom: 20px;
  cursor: pointer;
  border-radius: 22px;
  transition: all 0.3s;
  border: 1px solid rgba(15, 23, 42, 0.06);
}

.module-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--app-shadow-lg);
}

.module-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.module-chip {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--el-color-primary);
  font-size: 12px;
  font-weight: 700;
}

.module-card h3 {
  margin: 18px 0 12px;
  font-size: 22px;
  font-weight: 700;
}

.module-card p {
  margin: 0;
  color: var(--app-text-muted);
  line-height: 1.8;
  font-size: 13px;
}

.module-footer {
  margin-top: auto;
  padding-top: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.notice-card {
  border-radius: 22px;
  border: 1px dashed rgba(37, 99, 235, 0.22);
  background: rgba(255, 255, 255, 0.72);
}

.notice-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--app-text);
}

.notice-list {
  margin: 14px 0 0;
  padding-left: 18px;
  color: var(--app-text-muted);
  line-height: 1.9;
}

@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .welcome-card {
    padding: 22px;
  }

  .welcome-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .avatar {
    margin-right: 0;
    margin-bottom: 14px;
  }

  .module-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
