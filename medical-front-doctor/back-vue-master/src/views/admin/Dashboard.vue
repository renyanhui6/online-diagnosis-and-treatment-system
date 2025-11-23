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
        <div class="system-status">
          <div class="status-item">
            <div class="status-label">系统状态</div>
            <div class="status-value normal">正常运行</div>
          </div>
          <div class="status-item">
            <div class="status-label">在线医生</div>
            <div class="status-value">{{ dashboardData.onlineDoctors }}/{{ dashboardData.totalDoctors }}</div>
          </div>
          <div class="status-item">
            <div class="status-label">系统负载</div>
            <el-progress :percentage="dashboardData.systemLoad" :color="loadColor" :stroke-width="8" />
          </div>
        </div>
      </div>
    </div>
    
    <el-row :gutter="20" class="data-overview">
      <el-col :xs="24" :sm="12" :md="6">
        <el-card class="data-card" shadow="hover">
          <div class="data-icon total-appointments">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">今日预约总数</div>
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
          <div class="data-icon total-consultations">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">今日问诊总数</div>
            <div class="data-value">{{ dashboardData.todayConsultations }}</div>
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
          <div class="data-icon total-income">
            <el-icon><Money /></el-icon>
          </div>
          <div class="data-info">
            <div class="data-title">今日收入</div>
            <div class="data-value">¥{{ dashboardData.todayIncome.toLocaleString() }}</div>
            <div class="data-compare" :class="{ 'up': dashboardData.incomeChange > 0, 'down': dashboardData.incomeChange < 0 }">
              <el-icon v-if="dashboardData.incomeChange > 0"><ArrowUp /></el-icon>
              <el-icon v-else-if="dashboardData.incomeChange < 0"><ArrowDown /></el-icon>
              <span>{{ Math.abs(dashboardData.incomeChange) }}% 较昨日</span>
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
            <div class="data-title">平均满意度</div>
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
              <span>近30天收入趋势</span>
              <el-button type="text" @click="refreshCharts">刷新</el-button>
            </div>
          </template>
          <div class="chart-container" ref="incomeChartRef"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>各科室预约分布</span>
              <el-button type="text" @click="refreshCharts">刷新</el-button>
            </div>
          </template>
          <div class="chart-container" ref="departmentChartRef"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="charts-section">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>医生工作量排名</span>
              <el-button type="text" @click="router.push('/admin/statistics')">查看详情</el-button>
            </div>
          </template>
          <div class="chart-container" ref="doctorWorkloadChartRef"></div>
        </el-card>
      </el-col>
      
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>收入构成分析</span>
              <el-button type="text" @click="router.push('/admin/statistics')">查看详情</el-button>
            </div>
          </template>
          <div class="chart-container" ref="incomeCompositionChartRef"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <el-row :gutter="20" class="bottom-section">
      <el-col :xs="24">
        <el-card class="alert-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>系统告警</span>
              <el-button type="text">全部标为已处理</el-button>
            </div>
          </template>
          <el-table :data="alertData" style="width: 100%" :row-class-name="alertRowClassName">
            <el-table-column prop="level" label="级别" width="100">
              <template #default="scope">
                <el-tag :type="getAlertLevelType(scope.row.level)" effect="dark">
                  {{ scope.row.level }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="time" label="时间" width="180" />
            <el-table-column prop="source" label="来源" width="150" />
            <el-table-column prop="message" label="告警内容" />
            <el-table-column prop="status" label="状态" width="120">
              <template #default="scope">
                <el-tag :type="scope.row.status === '已处理' ? 'success' : 'warning'">
                  {{ scope.row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button 
                  type="text" 
                  size="small"
                  :disabled="scope.row.status === '已处理'"
                >
                  处理
                </el-button>
                <el-button type="text" size="small">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, onUnmounted } from 'vue';
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
  Calendar, ChatDotRound, Money, StarFilled,
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

const incomeChartRef = ref(null);
const departmentChartRef = ref(null);
const doctorWorkloadChartRef = ref(null);
const incomeCompositionChartRef = ref(null);

let incomeChart = null;
let departmentChart = null;
let doctorWorkloadChart = null;
let incomeCompositionChart = null;

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
  onlineDoctors: 18,
  totalDoctors: 25,
  systemLoad: 65,
  todayAppointments: 128,
  appointmentsChange: 12.5,
  todayConsultations: 86,
  consultationsChange: 8.3,
  todayIncome: 15680,
  incomeChange: 5.7,
  satisfaction: 4.7,
  satisfactionChange: 1.5
});

const loadColor = computed(() => {
  const load = dashboardData.systemLoad;
  if (load < 60) return '#67C23A';
  if (load < 80) return '#E6A23C';
  return '#F56C6C';
});

// 模拟告警数据
const alertData = [
  {
    level: '严重',
    time: '2023-05-20 08:15:32',
    source: '数据库服务器',
    message: '数据库连接池达到最大限制，可能影响系统性能',
    status: '未处理'
  },
  {
    level: '警告',
    time: '2023-05-20 07:30:15',
    source: '预约系统',
    message: '预约系统响应时间超过阈值',
    status: '未处理'
  },
  {
    level: '信息',
    time: '2023-05-19 22:45:10',
    source: '系统监控',
    message: '系统例行维护完成',
    status: '已处理'
  },
  {
    level: '严重',
    time: '2023-05-19 16:20:05',
    source: '网络设备',
    message: '主路由器负载过高',
    status: '已处理'
  }
];

function getAlertLevelType(level) {
  switch (level) {
    case '严重': return 'danger';
    case '警告': return 'warning';
    case '信息': return 'info';
    default: return '';
  }
}

function alertRowClassName({ row }) {
  if (row.status === '未处理' && row.level === '严重') {
    return 'alert-row-danger';
  }
  return '';
}

// 初始化图表
function initCharts() {
  // 收入趋势图表
  if (incomeChartRef.value) {
    incomeChart = echarts.init(incomeChartRef.value);
    const incomeOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'cross',
          label: {
            backgroundColor: '#6a7985'
          }
        }
      },
      legend: {
        data: ['总收入', '挂号收入', '药品收入']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          boundaryGap: false,
          data: Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
        }
      ],
      yAxis: [
        {
          type: 'value',
          axisLabel: {
            formatter: '¥{value}'
          }
        }
      ],
      series: [
        {
          name: '总收入',
          type: 'line',
          stack: 'Total',
          areaStyle: {},
          emphasis: {
            focus: 'series'
          },
          data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 5000) + 10000)
        },
        {
          name: '挂号收入',
          type: 'line',
          stack: 'Total',
          areaStyle: {},
          emphasis: {
            focus: 'series'
          },
          data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 2000) + 3000)
        },
        {
          name: '药品收入',
          type: 'line',
          stack: 'Total',
          areaStyle: {},
          emphasis: {
            focus: 'series'
          },
          data: Array.from({ length: 30 }, () => Math.floor(Math.random() * 3000) + 7000)
        }
      ]
    };
    incomeChart.setOption(incomeOption);
  }
  
  // 各科室预约分布图表
  if (departmentChartRef.value) {
    departmentChart = echarts.init(departmentChartRef.value);
    const departmentOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 10,
        data: ['内科', '外科', '妇产科', '儿科', '眼科', '口腔科', '其他']
      },
      series: [
        {
          name: '预约分布',
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
            { value: 35, name: '内科' },
            { value: 25, name: '外科' },
            { value: 18, name: '妇产科' },
            { value: 15, name: '儿科' },
            { value: 12, name: '眼科' },
            { value: 10, name: '口腔科' },
            { value: 5, name: '其他' }
          ]
        }
      ]
    };
    departmentChart.setOption(departmentOption);
  }
  
  // 医生工作量排名图表
  if (doctorWorkloadChartRef.value) {
    doctorWorkloadChart = echarts.init(doctorWorkloadChartRef.value);
    const doctorWorkloadOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['接诊人数', '问诊次数']
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'value'
      },
      yAxis: {
        type: 'category',
        data: ['张医生', '李医生', '王医生', '赵医生', '刘医生']
      },
      series: [
        {
          name: '接诊人数',
          type: 'bar',
          data: [45, 37, 34, 30, 28]
        },
        {
          name: '问诊次数',
          type: 'bar',
          data: [32, 28, 26, 22, 18]
        }
      ]
    };
    doctorWorkloadChart.setOption(doctorWorkloadOption);
  }
  
  // 收入构成分析图表
  if (incomeCompositionChartRef.value) {
    incomeCompositionChart = echarts.init(incomeCompositionChartRef.value);
    const incomeCompositionOption = {
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: ¥{c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 10,
        data: ['挂号费', '检查费', '药品费', '治疗费', '其他']
      },
      series: [
        {
          name: '收入构成',
          type: 'pie',
          radius: '70%',
          center: ['50%', '50%'],
          data: [
            { value: 25000, name: '挂号费' },
            { value: 80000, name: '检查费' },
            { value: 120000, name: '药品费' },
            { value: 65000, name: '治疗费' },
            { value: 30000, name: '其他' }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    };
    incomeCompositionChart.setOption(incomeCompositionOption);
  }
}

// 刷新图表
function refreshCharts() {
  // 模拟数据刷新
  if (incomeChart) {
    const newData1 = Array.from({ length: 30 }, () => Math.floor(Math.random() * 5000) + 10000);
    const newData2 = Array.from({ length: 30 }, () => Math.floor(Math.random() * 2000) + 3000);
    const newData3 = Array.from({ length: 30 }, () => Math.floor(Math.random() * 3000) + 7000);
    
    incomeChart.setOption({
      series: [
        { data: newData1 },
        { data: newData2 },
        { data: newData3 }
      ]
    });
  }
  
  if (departmentChart) {
    const newData = [
      { value: Math.floor(Math.random() * 20) + 25, name: '内科' },
      { value: Math.floor(Math.random() * 15) + 20, name: '外科' },
      { value: Math.floor(Math.random() * 10) + 15, name: '妇产科' },
      { value: Math.floor(Math.random() * 10) + 10, name: '儿科' },
      { value: Math.floor(Math.random() * 8) + 8, name: '眼科' },
      { value: Math.floor(Math.random() * 5) + 8, name: '口腔科' },
      { value: Math.floor(Math.random() * 5) + 3, name: '其他' }
    ];
    
    departmentChart.setOption({
      series: [{ data: newData }]
    });
  }
}

// 窗口大小变化时重新调整图表大小
function handleResize() {
  incomeChart && incomeChart.resize();
  departmentChart && departmentChart.resize();
  doctorWorkloadChart && doctorWorkloadChart.resize();
  incomeCompositionChart && incomeCompositionChart.resize();
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
  incomeChart && incomeChart.dispose();
  departmentChart && departmentChart.dispose();
  doctorWorkloadChart && doctorWorkloadChart.dispose();
  incomeCompositionChart && incomeCompositionChart.dispose();
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
  background: linear-gradient(135deg, #1890ff, #722ed1);
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

.system-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 200px;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-label {
  font-size: 14px;
  opacity: 0.9;
}

.status-value {
  font-weight: 500;
}

.status-value.normal {
  color: #52c41a;
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

.total-appointments {
  background-color: #1890ff;
}

.total-consultations {
  background-color: #52c41a;
}

.total-income {
  background-color: #722ed1;
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

.alert-card {
  margin-bottom: 20px;
}

.alert-row-danger {
  background-color: #fff1f0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .welcome-card {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .system-status {
    margin-top: 16px;
    width: 100%;
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

.alert-card :deep(.el-table__row) {
  transition: all 0.3s;
}

.alert-card :deep(.el-table__row:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1;
  position: relative;
}
</style>