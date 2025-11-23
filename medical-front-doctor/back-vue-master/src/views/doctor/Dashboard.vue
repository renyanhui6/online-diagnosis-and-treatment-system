<template>
  <div class="dashboard-container">
    <div class="welcome-section">
      <div class="welcome-card">
        <div class="welcome-info">
          <img :src="userInfo.avatar" alt="Avatar" class="avatar" />
          <div class="welcome-text">
            <h2>{{ greeting }}，{{ userInfo.name }}</h2>
            <p>今天是 {{ currentDate }}，{{ weatherText }}</p>
          </div>
        </div>
        <div class="quick-actions">
          <el-button type="primary" @click="router.push('/doctor/appointments')">
            <el-icon><Calendar /></el-icon>
            预约管理
          </el-button>
          <el-button type="success" @click="router.push('/doctor/consultation')">
            <el-icon><ChatDotRound /></el-icon>
            在线问诊
          </el-button>
          <el-button type="warning" @click="router.push('/doctor/prescription')">
            <el-icon><Tickets /></el-icon>
            处方管理
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
            <div class="data-title">今日预约</div>
            <div class="data-value">{{ dashboardData.todayAppointments }}</div>
            <div class="data-compare" :class="{ 'up': dashboardData.appointmentsChange > 0, 'down': dashboardData.appointmentsChange < 0 }">
              <el-icon v-if="dashboardData.appointmentsChange > 0"><ArrowUp /></el-icon>
              <el-icon v-else-if="dashboardData.appointmentsChange < 0"><ArrowDown /></el-icon>
              <span>{{ Math.abs(dashboardData.appointmentsChange) }}% 较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon waiting-consultations">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">待处理问诊</div>
            <div class="data-value">{{ dashboardData.waitingConsultations }}</div>
            <div class="data-compare" :class="{ 'up': dashboardData.consultationsChange > 0, 'down': dashboardData.consultationsChange < 0 }">
              <el-icon v-if="dashboardData.consultationsChange > 0"><ArrowUp /></el-icon>
              <el-icon v-else-if="dashboardData.consultationsChange < 0"><ArrowDown /></el-icon>
              <span>{{ Math.abs(dashboardData.consultationsChange) }}% 较昨日</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon prescriptions">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">处方数量</div>
            <div class="data-value">{{ dashboardData.prescriptions }}</div>
            <div class="data-compare" :class="{ 'up': dashboardData.prescriptionsChange > 0, 'down': dashboardData.prescriptionsChange < 0 }">
              <el-icon v-if="dashboardData.prescriptionsChange > 0"><ArrowUp /></el-icon>
              <el-icon v-else-if="dashboardData.prescriptionsChange < 0"><ArrowDown /></el-icon>
              <span>{{ Math.abs(dashboardData.prescriptionsChange) }}% 较上周</span>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon satisfaction">
            <el-icon><StarFilled /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">满意度评分</div>
            <div class="data-value">{{ dashboardData.satisfaction }}</div>
            <div class="data-compare" :class="{ 'up': dashboardData.satisfactionChange > 0, 'down': dashboardData.satisfactionChange < 0 }">
              <el-icon v-if="dashboardData.satisfactionChange > 0"><ArrowUp /></el-icon>
              <el-icon v-else-if="dashboardData.satisfactionChange < 0"><ArrowDown /></el-icon>
              <span>{{ Math.abs(dashboardData.satisfactionChange) }}% 较上月</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-section">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>近7天预约趋势</span>
              <el-button type="text" @click="refreshCharts">刷新</el-button>
            </div>
          </template>
          <div class="chart-container" ref="appointmentChartRef"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>问诊类型分布</span>
              <el-button type="text" @click="refreshCharts">刷新</el-button>
            </div>
          </template>
          <div class="chart-container" ref="consultationChartRef"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="bottom-section">
      <el-col :xs="24" :lg="16">
        <el-card class="schedule-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>今日日程安排</span>
              <el-button type="text" @click="router.push('/doctor/schedule')">查看更多</el-button>
            </div>
          </template>
          <el-empty v-if="scheduleData.length === 0" description="今日暂无日程安排" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="(activity, index) in scheduleData"
              :key="index"
              :timestamp="activity.time"
              :type="activity.type"
              :color="activity.color"
            >
              {{ activity.content }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="8">
        <el-card class="notification-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>通知公告</span>
              <el-button type="text">全部标为已读</el-button>
            </div>
          </template>
          <el-empty v-if="notificationData.length === 0" description="暂无通知" />
          <div v-else class="notification-list">
            <div 
              v-for="(notification, index) in notificationData" 
              :key="index"
              class="notification-item"
              :class="{ 'unread': !notification.read }"
            >
              <div class="notification-icon" :class="notification.type">
                <el-icon><component :is="getNotificationIcon(notification.type)" /></el-icon>
              </div>
              <div class="notification-content">
                <div class="notification-title">{{ notification.title }}</div>
                <div class="notification-time">{{ notification.time }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useUserStore } from '@/stores/user';
import * as echarts from 'echarts/core';
import { BarChart, PieChart, LineChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import {
  Calendar, ChatDotRound, Tickets, StarFilled,
  ArrowUp, ArrowDown, Bell, Message, Warning, InfoFilled
} from '@element-plus/icons-vue';

// 注册 ECharts 组件
echarts.use([
  BarChart,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  CanvasRenderer
]);

const router = useRouter();
const userStore = useUserStore();
const userInfo = computed(() => userStore.userInfo);

const appointmentChartRef = ref(null);
const consultationChartRef = ref(null);
let appointmentChart = null;
let consultationChart = null;

// 获取当前时间和问候语
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

const weatherText = '天气晴朗，适合出行';

// 模拟仪表盘数据
const dashboardData = reactive({
  todayAppointments: 12,
  appointmentsChange: 8.5,
  waitingConsultations: 5,
  consultationsChange: -3.2,
  prescriptions: 28,
  prescriptionsChange: 12.7,
  satisfaction: 4.8,
  satisfactionChange: 2.1
});

// 模拟日程数据
const scheduleData = [
  {
    time: '08:30 - 10:00',
    content: '门诊',
    type: 'primary',
    color: '#409EFF'
  },
  {
    time: '10:30 - 11:30',
    content: '科室会议',
    type: 'warning',
    color: '#E6A23C'
  },
  {
    time: '14:00 - 16:30',
    content: '门诊',
    type: 'primary',
    color: '#409EFF'
  },
  {
    time: '17:00 - 18:00',
    content: '查房',
    type: 'success',
    color: '#67C23A'
  }
];

// 模拟通知数据
const notificationData = [
  {
    title: '您有3个新的预约请求',
    time: '10分钟前',
    read: false,
    type: 'info'
  },
  {
    title: '张医生邀请您参加病例讨论',
    time: '1小时前',
    read: false,
    type: 'message'
  },
  {
    title: '系统维护通知',
    time: '昨天',
    read: true,
    type: 'warning'
  }
];

function getNotificationIcon(type) {
  switch (type) {
    case 'info': return 'InfoFilled';
    case 'message': return 'Message';
    case 'warning': return 'Warning';
    default: return 'Bell';
  }
}

// 初始化图表
function initCharts() {
  // 预约趋势图表
  if (appointmentChartRef.value) {
    appointmentChart = echarts.init(appointmentChartRef.value);
    const appointmentOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '预约数量',
          type: 'bar',
          data: [10, 15, 12, 8, 7, 11, 13],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#83bff6' },
              { offset: 0.5, color: '#188df0' },
              { offset: 1, color: '#188df0' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#2378f7' },
                { offset: 0.7, color: '#2378f7' },
                { offset: 1, color: '#83bff6' }
              ])
            }
          }
        }
      ],
      animationDuration: 1000
    };
    appointmentChart.setOption(appointmentOption);
  }
  
  // 问诊类型分布图表
  if (consultationChartRef.value) {
    consultationChart = echarts.init(consultationChartRef.value);
    const consultationOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 10,
        data: ['普通问诊', '复诊随访', '慢病管理', '专家会诊', '其他']
      },
      series: [
        {
          name: '问诊类型',
          type: 'pie',
          radius: ['50%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: '18',
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: 35, name: '普通问诊' },
            { value: 20, name: '复诊随访' },
            { value: 15, name: '慢病管理' },
            { value: 10, name: '专家会诊' },
            { value: 5, name: '其他' }
          ],
          animationType: 'scale',
          animationEasing: 'elasticOut',
          animationDelay: function (idx) {
            return Math.random() * 200;
          }
        }
      ]
    };
    consultationChart.setOption(consultationOption);
  }
}

// 刷新图表
function refreshCharts() {
  // 模拟数据刷新
  if (appointmentChart) {
    const newData = Array.from({ length: 7 }, () => Math.floor(Math.random() * 20) + 5);
    appointmentChart.setOption({
      series: [{
        data: newData
      }]
    });
  }
  
  if (consultationChart) {
    const newData = [
      { value: Math.floor(Math.random() * 40) + 20, name: '普通问诊' },
      { value: Math.floor(Math.random() * 30) + 10, name: '复诊随访' },
      { value: Math.floor(Math.random() * 20) + 10, name: '慢病管理' },
      { value: Math.floor(Math.random() * 15) + 5, name: '专家会诊' },
      { value: Math.floor(Math.random() * 10) + 1, name: '其他' }
    ];
    consultationChart.setOption({
      series: [{
        data: newData
      }]
    });
  }
}

// 窗口大小变化时重新调整图表大小
function handleResize() {
  appointmentChart && appointmentChart.resize();
  consultationChart && consultationChart.resize();
}

onMounted(() => {
  // 初始化图表
  setTimeout(() => {
    initCharts();
    window.addEventListener('resize', handleResize);
  }, 100);
});

// 组件卸载时移除事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  appointmentChart && appointmentChart.dispose();
  consultationChart && consultationChart.dispose();
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
  background: linear-gradient(135deg, #1890ff, #52c41a);
  border-radius: 8px;
  padding: 20px;
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
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
  gap: 10px;
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
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
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
  background-color: #1890ff;
}

.waiting-consultations {
  background-color: #52c41a;
}

.prescriptions {
  background-color: #faad14;
}

.satisfaction {
  background-color: #f5222d;
}

.data-info {
  flex: 1;
}

.data-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}

.data-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.data-compare {
  font-size: 12px;
  display: flex;
  align-items: center;
}

.data-compare.up {
  color: #52c41a;
}

.data-compare.down {
  color: #f5222d;
}

.charts-section {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.schedule-card,
.notification-card {
  margin-bottom: 20px;
  height: 400px;
  overflow-y: auto;
}

.notification-list {
  max-height: 320px;
  overflow-y: auto;
}

.notification-item {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #f0f7ff;
}

.notification-icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  color: #fff;
}

.notification-icon.info {
  background-color: #1890ff;
}

.notification-icon.message {
  background-color: #52c41a;
}

.notification-icon.warning {
  background-color: #faad14;
}

.notification-content {
  flex: 1;
}

.notification-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.notification-time {
  font-size: 12px;
  color: #909399;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .quick-actions {
    margin-top: 16px;
    width: 100%;
    justify-content: space-between;
  }
  
  .data-card {
    height: auto;
  }
}

/* 动画效果 */
.data-card {
  overflow: hidden;
  position: relative;
}

.data-card::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 70%);
  opacity: 0;
  transform: scale(0.5);
  transition: transform 0.5s, opacity 0.5s;
}

.data-card:hover::after {
  opacity: 1;
  transform: scale(1);
}

.data-icon {
  transition: all 0.3s;
}

.data-card:hover .data-icon {
  transform: scale(1.1) rotate(10deg);
}

.notification-item {
  position: relative;
  overflow: hidden;
}

.notification-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  width: 3px;
  background-color: transparent;
  transition: background-color 0.3s;
}

.notification-item.unread::before {
  background-color: #1890ff;
}
</style>