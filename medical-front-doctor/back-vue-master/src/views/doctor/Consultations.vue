<template>
  <div class="consultations-container">
    <div class="page-header">
      <h2>问诊管理</h2>
      <div class="header-actions">
        <el-select v-model="filterStatus" placeholder="问诊状态" clearable @change="handleFilterChange" style="width: 150px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :disabled="activeTab === 'pending'"
          @change="handleDateChange"
          style="width: 260px"
        />
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索患者姓名/问诊ID"
          clearable
          @input="handleSearch"
          style="width: 200px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-button type="primary" @click="refreshConsultations">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>

      </div>
    </div>
    
    <el-card shadow="hover" class="consultation-card">
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="待处理问诊" name="pending">
          <div v-if="pendingConsultations.length === 0 && !loading" class="empty-state">
            <el-empty description="暂无待处理问诊" />
          </div>
          <div v-else class="pending-consultations">
            <div 
              v-for="consultation in pendingConsultations" 
              :key="consultation.id"
              class="consultation-item"
              @click="viewConsultationDetail(consultation)"
            >
              <div class="consultation-header">
                <div class="patient-info">
                  <el-avatar :size="40" :src="consultation.patientAvatar" />
                  <div class="patient-detail">
                    <h4>{{ consultation.patientName }}</h4>
                    <p>{{ consultation.patientPhone }}</p>
                  </div>
                </div>
                <el-tag :type="getStatusType(consultation.registrationStatus)">
                  {{ getStatusText(consultation.registrationStatus) }}
                </el-tag>
              </div>
              <div class="consultation-content">
                <p><strong>开始时间：</strong>{{ consultation.createTime }}</p>
              </div>
              <div class="consultation-actions">
                <el-button 
                  v-if="consultation.registrationStatus === 2"
                  type="primary" 
                  size="small"
                  @click.stop="acceptConsultation(consultation)"
                >
                  接诊
                </el-button>
                <el-button
                  v-else-if="consultation.registrationStatus === 6"
                  type="primary"
                  size="small"
                  @click.stop="reAcceptConsultation(consultation)"
                >
                  重新接诊
                </el-button>
                <el-button
                  v-else-if="consultation.registrationStatus === 3"
                  type="success"
                  size="small"
                  @click.stop="enterConsultation(consultation)"
                >
                  进入问诊
                </el-button>
                <div v-else-if="consultation.registrationStatus === 7" class="waiting-status">
                  <el-tag type="warning" size="small">
                    等待响应 {{ getWaitingTime(consultation.id) }}
                  </el-tag>
                  <el-button
                    type="warning"
                    size="small"
                    @click.stop="viewWaitingStatus(consultation)"
                  >
                    查看状态
                  </el-button>
                </div>
                <el-button 
                  type="info" 
                  size="small"
                  @click.stop="viewConsultationDetail(consultation)"
                >
                  查看详情
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
        
        <el-tab-pane label="全部问诊" name="all">
          <el-table
            v-loading="loading"
            :data="paginatedConsultations"
            style="width: 100%"
            :row-class-name="getRowClassName"
            @row-click="handleRowClick"
          >
            <el-table-column prop="id" label="问诊ID" width="200" />
            <el-table-column prop="patientName" label="患者姓名" width="260">
              <template #default="{ row }">
                <div class="patient-info">
                  <el-avatar :size="24" :src="row.patientAvatar" />
                  <span>{{ row.patientName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="开始时间" width="400" sortable />
            <el-table-column prop="registrationStatus" label="状态" width="160">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.registrationStatus)">
                  {{ getStatusText(row.registrationStatus) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="240" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button 
                    v-if="row.registrationStatus === 2"
                    type="primary" 
                    size="small"
                    @click.stop="acceptConsultation(row)"
                  >
                    接诊
                  </el-button>
                  <el-button
                    v-else-if="row.registrationStatus === 6"
                    type="primary"
                    size="small"
                    @click.stop="reAcceptConsultation(row)"
                  >
                    重新接诊
                  </el-button>
                  <el-button
                    v-else-if="row.registrationStatus === 3"
                    type="success"
                    size="small"
                    @click.stop="enterConsultation(row)"
                  >
                    进入问诊
                  </el-button>
                  <el-button 
                    v-else-if="row.registrationStatus === 7"
                    type="warning"
                    size="small"
                    @click.stop="viewWaitingStatus(row)"
                  >
                    等待响应
                  </el-button>
                  <el-button
                    v-else-if="row.registrationStatus === 4"
                    type="info"
                    size="small"
                    @click.stop="viewConsultationDetail(row)"
                  >
                    查看
                  </el-button>
                  <el-button 
                    v-if="row.registrationStatus === 3"
                    type="success" 
                    size="small"
                    @click.stop="finishConsultation(row)"
                  >
                    结束问诊
                  </el-button>
                  <el-button 
                    type="info" 
                    size="small"
                    @click.stop="viewConsultationDetail(row)"
                  >
                    详情
                  </el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              :total="total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-tab-pane>
        

      </el-tabs>
    </el-card>
    
    <!-- 问诊详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="问诊详情"
      direction="rtl"
      size="30%"
    >
      <div v-if="selectedConsultation" class="consultation-detail">
        <div class="detail-header">
          <el-avatar :size="64" :src="selectedConsultation.patientAvatar" />
          <div class="patient-detail">
            <h3>{{ selectedConsultation.patientName }}</h3>
            <p>{{ selectedConsultation.patientGender }} · {{ selectedConsultation.patientAge }}岁</p>
            <div class="tag-group">
              <el-tag :type="getStatusType(selectedConsultation.registrationStatus)" size="small">
                {{ getStatusText(selectedConsultation.registrationStatus) }}
              </el-tag>
            </div>
          </div>
        </div>
        
        <el-divider />
        
        <div class="detail-content">
          <div class="detail-item">
            <span class="item-label">问诊ID：</span>
            <span class="item-value">{{ selectedConsultation.id }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">开始时间：</span>
            <span class="item-value">{{ selectedConsultation.createTime }}</span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.updateTime">
            <span class="item-label">更新时间：</span>
            <span class="item-value">{{ selectedConsultation.updateTime }}</span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.relatedAppointment">
            <span class="item-label">关联预约：</span>
            <span class="item-value">
              <el-link type="primary" @click="viewRelatedAppointment">{{ selectedConsultation.relatedAppointment }}</el-link>
            </span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.diagnosis">
            <span class="item-label">诊断结果：</span>
            <span class="item-value">{{ selectedConsultation.diagnosis }}</span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.prescription">
            <span class="item-label">处方信息：</span>
            <span class="item-value">
              <el-link type="primary" @click="viewPrescription">查看处方详情</el-link>
            </span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.satisfactionScore">
            <span class="item-label">满意度评分：</span>
            <span class="item-value">
              <el-rate 
                v-model="selectedConsultation.satisfactionScore" 
                disabled 
                show-score 
                text-color="#ff9900"
              />
            </span>
          </div>
          <div class="detail-item" v-if="selectedConsultation.patientFeedback">
            <span class="item-label">患者反馈：</span>
            <span class="item-value">{{ selectedConsultation.patientFeedback }}</span>
          </div>
        </div>
        
        <el-divider />
        
        <div class="detail-actions">
          <el-button 
            v-if="selectedConsultation.registrationStatus === 2"
            type="primary"
            @click="acceptConsultation(selectedConsultation)"
          >
            接诊
          </el-button>
          <el-button
            v-if="selectedConsultation.registrationStatus === 6"
            type="primary"
            @click="reAcceptConsultation(selectedConsultation)"
          >
            重新接诊
          </el-button>
          <el-button
            v-if="selectedConsultation.registrationStatus === 3"
            type="primary" 
            @click="enterConsultation(selectedConsultation)"
          >
            进入问诊
          </el-button>
          <el-button 
            v-if="selectedConsultation.registrationStatus === 3"
            type="success" 
            @click="finishConsultation(selectedConsultation)"
          >
            结束问诊
          </el-button>
          <el-button 
            v-if="selectedConsultation.registrationStatus === 3 && !selectedConsultation.prescription"
            type="warning" 
            @click="createPrescription(selectedConsultation)"
          >
            开具处方
          </el-button>
          <el-button
            v-if="selectedConsultation.registrationStatus === 5"
            type="info"
            @click="resumeConsultation(selectedConsultation)"
          >
            恢复问诊
          </el-button>
          <el-button 
            type="info" 
            @click="drawerVisible = false"
          >
            关闭
          </el-button>
        </div>
      </div>
    </el-drawer>
    
    <!-- 结束问诊对话框 -->
    <el-dialog
      v-model="finishDialogVisible"
      title="结束问诊"
      width="40%"
    >
      <el-form :model="diagnosisForm" label-width="100px" :rules="diagnosisRules" ref="diagnosisFormRef">
        <el-form-item label="诊断结果" prop="diagnosis">
          <el-input 
            v-model="diagnosisForm.diagnosis" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入诊断结果"
          />
        </el-form-item>
        <el-form-item label="治疗建议" prop="treatment">
          <el-input 
            v-model="diagnosisForm.treatment" 
            type="textarea" 
            :rows="3" 
            placeholder="请输入治疗建议"
          />
        </el-form-item>
        <el-form-item label="注意事项">
          <el-input 
            v-model="diagnosisForm.notes" 
            type="textarea" 
            :rows="2" 
            placeholder="请输入注意事项"
          />
        </el-form-item>
        <el-form-item label="是否开处方">
          <el-switch v-model="diagnosisForm.needPrescription" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="finishDialogVisible = false">取消</el-button>
          <el-button type="success" @click="submitDiagnosis">提交并结束问诊</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';

import { useRouter } from 'vue-router';
import { Search, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getRegistrationList, getAllRegistrationInfoList, changeStatusToCompleted } from '@/api/doctor';
import { initiateConsultation, getChatRoom } from '@/api/chat';
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

// 状态和数据
const activeTab = ref('pending');
const loading = ref(false);
const statisticsLoading = ref(false);
const consultations = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterStatus = ref('');
const selectedDate = ref('');
const searchQuery = ref('');
const total = ref(0); // 添加总数

// 抽屉和对话框
const drawerVisible = ref(false);
const selectedConsultation = ref(null);
const finishDialogVisible = ref(false);
const diagnosisFormRef = ref(null);
const diagnosisForm = ref({
  id: '',
  diagnosis: '',
  treatment: '',
  notes: '',
  needPrescription: false
});

const diagnosisRules = {
  diagnosis: [{ required: true, message: '请输入诊断结果', trigger: 'blur' }],
  treatment: [{ required: true, message: '请输入治疗建议', trigger: 'blur' }]
};

// 统计数据
const statistics = ref({
  totalCount: 0,
  activeCount: 0,
  avgResponseTime: 0,
  satisfactionScore: 0
});

// 响应定时器存储
const responseTimers = ref({});

// 等待开始时间存储
const waitingStartTimes = ref({});

// 倒计时定时器
const countdownTimer = ref(null);

// 状态选项 - 根据registrationStatus字段
const statusOptions = [
  { value: 2, label: '排队中' },
  { value: 3, label: '问诊中' },
  { value: 4, label: '已完成' },
  { value: 5, label: '暂时挂起' },
  { value: 6, label: '已回归' },
  { value: 7, label: '等待患者确认' }
];

// 计算属性
const filteredConsultations = computed(() => {
  let result = [...consultations.value];
  
  // 状态筛选（仅适用于全部问诊）
  if (filterStatus.value !== '' && filterStatus.value !== null && filterStatus.value !== undefined) {
    result = result.filter(item => item.registrationStatus === filterStatus.value);
  }
  
  // 日期筛选 - 根据开始时间的年月日查询（仅适用于全部问诊）
  if (selectedDate.value) {
    const targetDate = new Date(selectedDate.value);
    const targetYear = targetDate.getFullYear();
    const targetMonth = targetDate.getMonth();
    const targetDay = targetDate.getDate();

    result = result.filter(item => {
      if (!item.createTime) {
        return false;
      }

      // 处理日期格式问题：将 02-35 转换为 02:35
      let processedTime = item.createTime;
      if (processedTime && processedTime.includes('-')) {
        const parts = processedTime.split(' ');
        if (parts.length === 2) {
          const datePart = parts[0];
          const timePart = parts[1].replace(/-/g, ':');
          processedTime = datePart + ' ' + timePart;
        }
      }

      const itemDate = new Date(processedTime);
      const itemYear = itemDate.getFullYear();
      const itemMonth = itemDate.getMonth();
      const itemDay = itemDate.getDate();

      return itemYear === targetYear && itemMonth === targetMonth && itemDay === targetDay;
    });
  }
  
  // 搜索筛选（仅适用于全部问诊）
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item => 
      (item.patientName && item.patientName.toLowerCase().includes(query)) ||
      (item.id && item.id.toString().includes(query))
    );
  }
  
  return result;
});

const paginatedConsultations = computed(() => {
  return filteredConsultations.value;
});

const pendingConsultations = computed(() => {
  let result = [];

  // 待处理问诊的状态筛选
  if (filterStatus.value !== '' && filterStatus.value !== null && filterStatus.value !== undefined) {
    // 如果选择了特定状态，只显示该状态的问诊
    result = consultations.value.filter(item => item.registrationStatus === filterStatus.value);
  } else {
    // 如果没有选择状态，显示所有待处理问诊（状态为2、3、5、6）
    result = consultations.value.filter(item =>
      item.registrationStatus === 2 || item.registrationStatus === 3 ||
      item.registrationStatus === 5 || item.registrationStatus === 6
    );
  }

  // 待处理问诊的搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item =>
      (item.patientName && item.patientName.toLowerCase().includes(query)) ||
      (item.id && item.id.toString().includes(query))
    );
  }

  // 待处理问诊的日期筛选
  if (selectedDate.value) {
    const targetDate = new Date(selectedDate.value);
    const targetYear = targetDate.getFullYear();
    const targetMonth = targetDate.getMonth();
    const targetDay = targetDate.getDate();

    result = result.filter(item => {
      if (!item.createTime) {
        return false;
      }

      // 处理日期格式问题：将 02-35 转换为 02:35
      let processedTime = item.createTime;
      if (processedTime && processedTime.includes('-')) {
        const parts = processedTime.split(' ');
        if (parts.length === 2) {
          const datePart = parts[0];
          const timePart = parts[1].replace(/-/g, ':');
          processedTime = datePart + ' ' + timePart;
        }
      }

      const itemDate = new Date(processedTime);
      const itemYear = itemDate.getFullYear();
      const itemMonth = itemDate.getMonth();
      const itemDay = itemDate.getDate();

      return itemYear === targetYear && itemMonth === targetMonth && itemDay === targetDay;
    });
  }

  return result;
});

// 方法
function getStatusType(status) {
  switch (status) {
    case 2: return 'warning'; // 排队中
    case 3: return 'primary'; // 问诊中
    case 4: return 'success'; // 已完成
    case 5: return 'info'; // 暂时挂起
    case 6: return 'info'; // 已回归
    case 7: return 'warning'; // 等待患者确认
    default: return 'info';
  }
}

function getStatusText(status) {
  switch (status) {
    case 2: return '排队中';
    case 3: return '问诊中';
    case 4: return '已完成';
    case 5: return '暂时挂起';
    case 6: return '已回归';
    case 7: return '等待患者确认';
    default: return '未知';
  }
}

function getTypeTag(type) {
  switch (type) {
    case '图文问诊': return 'success';
    case '视频问诊': return 'danger';
    case '电话问诊': return 'warning';
    case '复诊随访': return 'info';
    default: return 'info';
  }
}

function getRowClassName({ row }) {
  if (row.registrationStatus === 2) {
    return 'pending-row';
  } else if (row.registrationStatus === 3) {
    return 'active-row';
  }
  return '';
}

async function fetchConsultations() {
  loading.value = true;
  consultations.value = [];

  try {
    let response;

    if (activeTab.value === 'all') {
      // 构建分页参数
      const params = {
        pageNum: currentPage.value,
        pageSize: pageSize.value
      };
      response = await getAllRegistrationInfoList(params);
    } else {
      response = await getRegistrationList();
    }

    if (response.code === 200) {
      if (response.data && Array.isArray(response.data)) {
        consultations.value = response.data;
        total.value = response.data.length; // 如果后端没有返回总数，使用当前数据长度
      } else if (response.data && Array.isArray(response.data.list)) {
        consultations.value = response.data.list;
        total.value = response.data.total || response.data.list.length;
      } else if (response.data && Array.isArray(response.data.records)) {
        consultations.value = response.data.records;
        total.value = response.data.total || response.data.records.length;
      } else {
        consultations.value = [];
        total.value = 0;
      }
    } else {
      ElMessage.error(`获取问诊记录失败: ${response.message || '未知错误'}`);
      consultations.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取问诊记录失败:', error);
    ElMessage.error(`获取问诊记录失败: ${error.message}`);
    consultations.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function refreshConsultations() {
  filterStatus.value = '';
  selectedDate.value = '';
  searchQuery.value = '';
  currentPage.value = 1;

  nextTick(() => {
    fetchConsultations();
  });
  ElMessage.success('问诊列表已刷新');
}

function handleTabClick(tab) {
  if (tab.props.name === 'all' || tab.props.name === 'pending') {
    nextTick(() => {
      fetchConsultations();
    });
  }
}

function handleFilterChange() {
  currentPage.value = 1;
  if (activeTab.value === 'all') {
    fetchConsultations();
  }
}

function handleDateChange() {
  currentPage.value = 1;
  if (activeTab.value === 'all') {
    fetchConsultations();
  }
}

function handleSearch() {
  currentPage.value = 1;
  if (activeTab.value === 'all') {
    fetchConsultations();
  }
}

function handleSizeChange(val) {
  pageSize.value = val;
  currentPage.value = 1;
  if (activeTab.value === 'all') {
    fetchConsultations();
  }
}

function handleCurrentChange(val) {
  currentPage.value = val;
  if (activeTab.value === 'all') {
    fetchConsultations();
  }
}

function handleRowClick(row) {
  try {
    if (!row) {
      ElMessage.error('行数据不完整，无法查看详情');
      return;
    }
    viewConsultationDetail(row);
  } catch (error) {
    console.error('点击行失败:', error);
    ElMessage.error('点击行失败，请稍后重试');
  }
}

function viewConsultationDetail(consultation) {
  try {
    if (!consultation) {
      ElMessage.error('问诊信息不完整，无法查看详情');
      return;
    }
    selectedConsultation.value = { ...consultation };
    drawerVisible.value = true;
  } catch (error) {
    console.error('查看问诊详情失败:', error);
    ElMessage.error('查看问诊详情失败，请稍后重试');
  }
}

// 接诊患者
async function acceptConsultation(consultation) {
  try {
    // 防止重复调用
    if (consultation.isProcessing) {
      console.log('正在处理中，跳过重复请求');
      return;
    }

    // 设置处理中状态
    consultation.isProcessing = true;

    console.log('=== 开始接诊流程 ===');
    console.log('接诊患者，咨询信息:', consultation);
    console.log('当前用户信息:', userStore.userInfo);
    console.log('当前token:', userStore.token);
    console.log('当前用户角色:', userStore.userRole);
    console.log('用户是否已登录:', userStore.isLoggedIn);

    // 检查登录状态
    if (!userStore.isLoggedIn) {
      ElMessage.error('未登录，请先登录');
      return;
    }

    // 确保必要字段存在
    if (!consultation.patientId) {
      ElMessage.error('患者ID缺失，无法接诊');
      return;
    }

    // 从用户状态获取医生ID
    let doctorId = userStore.userInfo?.userId;
    console.log('初始获取到的医生ID:', doctorId);

    // 如果医生ID不存在，尝试重新获取用户信息
    if (!doctorId) {
      console.log('医生ID缺失，尝试重新获取用户信息');
      try {
        const userInfo = await userStore.fetchUserInfo();
        console.log('重新获取的用户信息:', userInfo);

        if (userInfo && userInfo.userId) {
          doctorId = userInfo.userId;
          console.log('重新获取到的医生ID:', doctorId);
        } else {
          console.error('重新获取用户信息失败，用户信息为空或缺少ID');
          ElMessage.error('获取医生信息失败，请重新登录');
          return;
        }
      } catch (error) {
        console.error('重新获取用户信息时出错:', error);
        ElMessage.error('获取医生信息失败，请重新登录');
        return;
      }
    }

    // 最终检查医生ID
    if (!doctorId) {
      console.error('最终检查：医生ID仍然缺失');
      ElMessage.error('医生ID缺失，请重新登录');
      return;
    }

    console.log('最终使用的医生ID:', doctorId);
    await performAcceptConsultation(consultation, doctorId);

  } catch (error) {
    console.error('接诊失败:', error);
    ElMessage.error('接诊失败: ' + error.message);
  } finally {
    // 重置处理状态
    consultation.isProcessing = false;
  }
}

// 执行接诊操作
async function performAcceptConsultation(consultation, doctorId) {
  try {
    // 防止重复调用
    if (consultation.isPerforming) {
      console.log('正在执行接诊操作，跳过重复请求');
      return;
    }

    // 设置执行中状态
    consultation.isPerforming = true;

    // 先检查是否已存在房间
    console.log('检查是否已存在房间，预约ID:', consultation.id);

    try {
      const existingRoomResponse = await getChatRoom(consultation.id);
      if (existingRoomResponse.code === 200 && existingRoomResponse.data) {
        // 房间已存在，直接使用现有房间
        const existingRoom = existingRoomResponse.data;
        console.log('✅ 找到现有房间:', existingRoom);

        ElMessage.success('使用现有房间，已发送接诊通知...');

        // 更新本地状态为等待患者确认
        const index = consultations.value.findIndex(item => item.id === consultation.id);
        if (index !== -1) {
          consultations.value[index].registrationStatus = 7; // 7: 等待患者确认
        }

        // 启动3分钟倒计时
        startResponseTimer(consultation.id);

        // 跳转到现有聊天房间
        router.push(`/doctor/chat/${existingRoom.id}`);
        return;
      }
    } catch (roomCheckError) {
      console.log('房间不存在或查询失败，将创建新房间:', roomCheckError.message);
    }

    // 房间不存在，创建新房间
    console.log('创建新房间，参数:', {
      registrationId: consultation.id,
      doctorId: doctorId,
      patientId: consultation.patientId,
      patientName: consultation.patientName
    });

    const response = await initiateConsultation({
      registrationId: consultation.id,
      doctorId: doctorId,
      patientId: consultation.patientId,
      patientName: consultation.patientName
    });

    console.log('接诊响应:', response);

    if (response.code === 200) {
      ElMessage.success('已发送接诊通知，等待患者确认...');

      // 更新本地状态为等待患者确认
      const index = consultations.value.findIndex(item => item.id === consultation.id);
      if (index !== -1) {
        consultations.value[index].registrationStatus = 7; // 7: 等待患者确认
      }

      // 启动3分钟倒计时
      startResponseTimer(consultation.id);

      // 获取创建的房间ID，跳转到聊天页面
      const roomId = response.data.roomId;
      if (roomId) {
        router.push(`/doctor/chat/${roomId}`);
      }

    } else {
      ElMessage.error(response.message || '接诊失败');
    }
  } catch (error) {
    console.error('执行接诊操作失败:', error);
    ElMessage.error('接诊失败: ' + error.message);
  } finally {
    // 重置执行状态
    consultation.isPerforming = false;
  }
}

// 查看等待响应状态
function viewWaitingStatus(consultation) {
  ElMessage.info(`正在等待患者 ${consultation.patientName} 确认接诊请求`);
}

// 启动响应倒计时
function startResponseTimer(consultationId) {
  // 记录开始时间
  waitingStartTimes.value[consultationId] = Date.now();

  const timer = setTimeout(() => {
    // 3分钟后，如果患者仍未响应，将状态改为挂起
    const index = consultations.value.findIndex(item => item.id === consultationId);
    if (index !== -1 && consultations.value[index].registrationStatus === 7) {
      consultations.value[index].registrationStatus = 5; // 5: 暂时挂起
      ElMessage.warning('患者超时未响应，问诊已挂起');
    }
  }, 3 * 60 * 1000); // 3分钟

  // 保存定时器引用，以便后续清理
  responseTimers.value[consultationId] = timer;

  // 启动倒计时更新
  startCountdown();
}

// 获取等待时间
function getWaitingTime(consultationId) {
  const startTime = waitingStartTimes.value[consultationId];
  if (!startTime) return '';

  const elapsed = Math.floor((Date.now() - startTime) / 1000);
  const remaining = Math.max(0, 180 - elapsed); // 3分钟 = 180秒

  const minutes = Math.floor(remaining / 60);
  const seconds = remaining % 60;

  return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

// 启动倒计时更新
function startCountdown() {
  if (countdownTimer.value) return;

  countdownTimer.value = setInterval(() => {
    // 强制更新组件，显示最新的倒计时
    consultations.value = [...consultations.value];
  }, 1000);
}

// 清理响应定时器
function clearResponseTimer(consultationId) {
  if (responseTimers.value[consultationId]) {
    clearTimeout(responseTimers.value[consultationId]);
    delete responseTimers.value[consultationId];
  }

  // 清理等待开始时间
  delete waitingStartTimes.value[consultationId];

  // 如果没有其他等待的问诊，停止倒计时
  if (Object.keys(waitingStartTimes.value).length === 0 && countdownTimer.value) {
    clearInterval(countdownTimer.value);
    countdownTimer.value = null;
  }
}

// 处理患者响应
function handlePatientResponse(consultationId, response) {
  const index = consultations.value.findIndex(item => item.id === consultationId);
  if (index === -1) return;

  // 清理定时器
  clearResponseTimer(consultationId);

  if (response === 'accept') {
    // 患者接受问诊
    consultations.value[index].registrationStatus = 3; // 3: 问诊中
    ElMessage.success('患者已接受问诊，可以开始聊天');

    // 刷新问诊列表
  fetchConsultations();
  } else if (response === 'reject') {
    // 患者拒绝问诊
    consultations.value[index].registrationStatus = 5; // 5: 暂时挂起
    ElMessage.warning('患者拒绝问诊');

    // 刷新问诊列表
    fetchConsultations();
  }
}

// 恢复问诊（将挂起状态改为已回归）
async function resumeConsultation(consultation) {
  try {
    const index = consultations.value.findIndex(item => item.id === consultation.id);
    if (index === -1) return;

    // 更新状态为已回归
    consultations.value[index].registrationStatus = 6; // 6: 已回归

    ElMessage.success('问诊已恢复为已回归状态');
  } catch (error) {
    console.error('恢复问诊失败:', error);
    ElMessage.error('恢复问诊失败');
  }
}

// 重新接诊已回归的患者
async function reAcceptConsultation(consultation) {
  try {
    console.log('=== 开始重新接诊流程 ===');
    console.log('重新接诊患者，咨询信息:', consultation);

    // 检查登录状态
    if (!userStore.isLoggedIn) {
      ElMessage.error('未登录，请先登录');
      return;
    }

    // 确保必要字段存在
    if (!consultation.patientId) {
      ElMessage.error('患者ID缺失，无法重新接诊');
      return;
    }

    // 从用户状态获取医生ID
    let doctorId = userStore.userInfo?.userId;
    console.log('初始获取到的医生ID:', doctorId);

    // 如果医生ID不存在，尝试重新获取用户信息
    if (!doctorId) {
      console.log('医生ID缺失，尝试重新获取用户信息');
      try {
        const userInfo = await userStore.fetchUserInfo();
        console.log('重新获取的用户信息:', userInfo);

        if (userInfo && userInfo.userId) {
          doctorId = userInfo.userId;
          console.log('重新获取到的医生ID:', doctorId);
        } else {
          console.error('重新获取用户信息失败，用户信息为空或缺少ID');
          ElMessage.error('获取医生信息失败，请重新登录');
          return;
        }
      } catch (error) {
        console.error('重新获取用户信息时出错:', error);
        ElMessage.error('获取医生信息失败，请重新登录');
        return;
      }
    }

    // 最终检查医生ID
    if (!doctorId) {
      console.error('最终检查：医生ID仍然缺失');
      ElMessage.error('医生ID缺失，请重新登录');
      return;
    }

    console.log('最终使用的医生ID:', doctorId);

    // 确认重新接诊
    await ElMessageBox.confirm(
      `确定要重新接诊患者 ${consultation.patientName} 吗？`,
      '重新接诊确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    // 执行重新接诊操作
    await performReAcceptConsultation(consultation, doctorId);

  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新接诊失败:', error);
      ElMessage.error('重新接诊失败: ' + error.message);
    }
  }
}

// 执行重新接诊操作
async function performReAcceptConsultation(consultation, doctorId) {
  try {
    // 始终重新创建问诊房间（与首次接诊一致）
    console.log('重新创建问诊房间，参数:', {
      registrationId: consultation.id,
      doctorId: doctorId,
      patientId: consultation.patientId,
      patientName: consultation.patientName
    });

    const response = await initiateConsultation({
      registrationId: consultation.id,
      doctorId: doctorId,
      patientId: consultation.patientId,
      patientName: consultation.patientName
    });

    console.log('重新接诊响应:', response);

    if (response.code === 200) {
      ElMessage.success('已发送重新接诊通知，等待患者确认...');

      // 更新本地状态为等待患者确认
      const index = consultations.value.findIndex(item => item.id === consultation.id);
      if (index !== -1) {
        consultations.value[index].registrationStatus = 7; // 7: 等待患者确认
      }

      // 启动3分钟倒计时
      startResponseTimer(consultation.id);

      // 获取创建的房间ID，跳转到聊天页面
      const roomId = response.data.roomId;
      if (roomId) {
        router.push(`/doctor/chat/${roomId}`);
      }

    } else {
      ElMessage.error(response.message || '重新接诊失败');
    }
  } catch (error) {
    console.error('执行重新接诊操作失败:', error);
    ElMessage.error('重新接诊失败: ' + error.message);
  }
}

async function enterConsultation(consultation) {
  try {
    console.log('进入问诊，咨询信息:', consultation);

    // 确保必要字段存在
    if (!consultation.patientId) {
      ElMessage.error('患者ID缺失，无法进入问诊');
      return;
    }

    // 只有问诊中状态才能进入聊天
    if (consultation.registrationStatus === 3) {
      try {
        // 通过挂号ID获取已有房间信息并用房间ID进入
        const roomResp = await getChatRoom(consultation.id);
        if (roomResp.code === 200 && roomResp.data && roomResp.data.id) {
          router.push(`/doctor/chat/${roomResp.data.id}`);
          return;
        }
        ElMessage.error('未找到正在进行中的房间，请稍后再试或重新接诊');
        return;
      } catch (err) {
        console.error('获取房间信息失败，无法进入问诊:', err);
        ElMessage.error('进入问诊失败，无法获取房间信息');
        return;
      }
    }

    // 其他状态提示用户
    ElMessage.warning('当前问诊状态不允许进入聊天');
  } catch (error) {
    console.error('进入问诊失败:', error);
    ElMessage.error('进入问诊失败: ' + error.message);
  }
}

function finishConsultation(consultation) {
  try {
    if (!consultation || !consultation.id) {
      ElMessage.error('问诊信息不完整，无法结束问诊');
      return;
    }
  selectedConsultation.value = consultation;
  diagnosisForm.value = {
    id: consultation.id,
    diagnosis: '',
    treatment: '',
    notes: '',
    needPrescription: false
  };
  finishDialogVisible.value = true;
  } catch (error) {
    console.error('结束问诊失败:', error);
    ElMessage.error('结束问诊失败，请稍后重试');
  }
}

function createPrescription(consultation) {
  router.push(`/doctor/prescriptions/new?consultationId=${consultation.id}`);
}

function viewPrescription() {
  if (selectedConsultation.value && selectedConsultation.value.prescription) {
    router.push(`/doctor/prescriptions/${selectedConsultation.value.prescription.id}`);
  }
}

function viewRelatedAppointment() {
  if (selectedConsultation.value && selectedConsultation.value.relatedAppointment) {
    // 这里可以跳转到预约详情页或者打开预约详情抽屉
    ElMessage({ type: 'info', message: `查看预约 ${selectedConsultation.value.relatedAppointment} 的详情` });
  }
}

async function submitDiagnosis() {
  if (!diagnosisFormRef.value) return;
  
  await diagnosisFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 调用后端接口，将状态设置为已完成
        try {
          const registrationId = diagnosisForm.value.id;
          if (registrationId) {
            console.log('🔌 调用changeStatusToCompleted，registrationId:', registrationId);
            await changeStatusToCompleted(registrationId);
            console.log('✅ 状态已更新为已完成');
          } else {
            console.error('❌ 无法获取registrationId');
          }
        } catch (error) {
          console.error('❌ 更新状态为已完成失败:', error);
          ElMessage.error('更新状态失败');
        }
        
        // 模拟API调用
        // await submitConsultationDiagnosis(diagnosisForm.value);
        
        // 模拟成功响应
        setTimeout(() => {
          // 更新本地数据
          const index = consultations.value.findIndex(item => item.id === diagnosisForm.value.id);
          if (index !== -1) {
            consultations.value[index].registrationStatus = 4; // 4: 已完成
            consultations.value[index].diagnosis = diagnosisForm.value.diagnosis;
            consultations.value[index].endTime = new Date().toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          }
          
          // 如果抽屉打开且显示的是当前问诊，也更新抽屉中的数据
          if (drawerVisible.value && selectedConsultation.value && selectedConsultation.value.id === diagnosisForm.value.id) {
            selectedConsultation.value.registrationStatus = 4;
            selectedConsultation.value.diagnosis = diagnosisForm.value.diagnosis;
            selectedConsultation.value.endTime = new Date().toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit'
            });
          }
          
          finishDialogVisible.value = false;
          ElMessage.success('问诊已结束，诊断结果已提交');
          
          // 如果需要开处方，跳转到处方页面
          if (diagnosisForm.value.needPrescription) {
            router.push(`/doctor/prescriptions/new?consultationId=${diagnosisForm.value.id}`);
          }
          
          // 更新统计数据
          // updateStatistics(); // 移除此行
        }, 500);
      } catch (error) {
        console.error('提交诊断结果失败:', error);
        ElMessage.error('提交诊断结果失败');
      }
    }
  });
}

// 窗口大小变化时重新调整图表大小
function handleResize() {
  // 如果需要图表功能，可以在这里添加
  // 目前Consultations.vue不需要图表功能
}

// 生命周期钩子
onMounted(async () => {
  // 获取用户信息
  await userStore.fetchUserInfo();

  // 检查是否有从预约管理跳转过来的appointmentId
  const appointmentId = router.currentRoute.value.query.appointmentId;
  if (appointmentId) {
    // 如果有appointmentId，创建新的问诊记录
    createConsultationFromAppointment(appointmentId);
  }
  
  fetchConsultations();
  window.addEventListener('resize', handleResize);
  
  // 监听问诊状态更新事件
  window.addEventListener('registrationStatusUpdated', handleRegistrationStatusUpdate);
  
  // 监听localStorage变化
  window.addEventListener('storage', handleStorageChange);
});

// 从预约创建问诊
function createConsultationFromAppointment(appointmentId) {
  // 这里应该调用API创建问诊记录
  // 暂时使用模拟数据
  const newConsultation = {
    id: `C${Date.now()}`,
    patientName: '患者姓名',
    patientAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNGNUY1RjUiLz4KPHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4PSI4IiB5PSI4Ij4KPHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDEyQzE0LjIwOTEgMTIgMTYgMTAuMjA5MSAxNiA4QzE2IDUuNzkwODYgMTQuMjA5MSA0IDEyIDRDOS43OTA4NiA0IDggNS43OTA4NiA4IDhDOCAxMC4yMDkxIDkuNzkwODYgMTIgMTJaIiBmaWxsPSIjOTk5OTk5Ii8+CjxwYXRoIGQ9Ik0xMiAxNEM5LjMzIDEzLjk5IDcuMDEgMTUuNjIgNiAxOEMxMC4wMSAyMCAxMy45OSAyMCAxOCAxOEMxNi45OSAxNS42MiAxNC42NyAxMy45OSAxMiAxNFoiIGZpbGw9IiM5OTk5OTkiLz4KPC9zdmc+Cjwvc3ZnPgo8L3N2Zz4K',
    patientGender: '男',
    patientAge: 35,
    type: '图文问诊',
    title: '从预约转入的问诊',
    startTime: new Date().toLocaleString('zh-CN'),
    lastReplyTime: new Date().toLocaleString('zh-CN'),
    messageCount: 0,
    status: '进行中',
    relatedAppointment: appointmentId
  };
  
  // 添加到问诊列表
  consultations.value.unshift(newConsultation);
  
  // 自动进入问诊聊天界面
  setTimeout(() => {
    enterConsultation(newConsultation);
  }, 500);
}

// 组件卸载时移除事件监听
watch(() => activeTab.value, (newVal) => {
  if (newVal === 'statistics') {
    statisticsLoading.value = true;
    setTimeout(() => {
      // 如果需要统计功能，可以在这里添加
      statisticsLoading.value = false;
    }, 500);
  }
});

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  // 清理所有响应定时器
  Object.keys(responseTimers.value).forEach(consultationId => {
    clearResponseTimer(consultationId);
  });

  // 清理倒计时定时器
  if (countdownTimer.value) {
    clearInterval(countdownTimer.value);
    countdownTimer.value = null;
  }

  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize);
  
  // 移除问诊状态更新事件监听器
  window.removeEventListener('registrationStatusUpdated', handleRegistrationStatusUpdate);
  
  // 移除localStorage变化监听器
  window.removeEventListener('storage', handleStorageChange);
});

// 处理问诊状态更新事件
function handleRegistrationStatusUpdate(event) {
  console.log('收到问诊状态更新事件:', event.detail);
  const { registrationId, newStatus } = event.detail;
  
  // 更新本地状态
  const index = consultations.value.findIndex(item => item.id === registrationId);
  if (index !== -1) {
    consultations.value[index].registrationStatus = newStatus;
    console.log('✅ 已更新本地问诊状态:', registrationId, '新状态:', newStatus);
  }
  
  // 刷新问诊列表
  fetchConsultations();
}

// 处理localStorage变化
function handleStorageChange(event) {
  if (event.key === 'registrationStatusUpdate') {
    try {
      const statusUpdate = JSON.parse(event.newValue);
      if (statusUpdate && statusUpdate.type === 'registration_status_update') {
        console.log('收到localStorage状态更新:', statusUpdate);
        handleRegistrationStatusUpdate({ detail: statusUpdate });
      }
    } catch (error) {
      console.error('解析localStorage状态更新失败:', error);
    }
  }
}
</script>

<style scoped>
.consultations-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 18px;
  background: var(--app-surface);
  border: 1px solid var(--app-border);
  border-radius: var(--app-radius);
  box-shadow: var(--app-shadow-sm);
  backdrop-filter: blur(10px);
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.consultation-card {
  border-radius: var(--app-radius);
}

.empty-state {
  text-align: center;
  padding: 48px 0;
}

.pending-consultations {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.consultation-item {
  cursor: pointer;
  border-radius: var(--app-radius);
  border: 1px solid var(--app-border);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--app-shadow-sm);
  padding: 14px;
  transition: transform var(--app-transition), box-shadow var(--app-transition), border-color var(--app-transition);
}

.consultation-item:hover {
  transform: translateY(-1px);
  box-shadow: var(--app-shadow);
  border-color: rgba(37, 99, 235, 0.35);
}

.consultation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.patient-detail {
  min-width: 0;
}

.patient-detail h4 {
  margin: 0 0 4px 0;
  font-size: 15px;
  font-weight: 700;
  color: var(--app-text);
}

.patient-detail p {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.consultation-content {
  margin-bottom: 10px;
}

.consultation-content p {
  margin: 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.consultation-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.waiting-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: flex-end;
}

.waiting-status .el-tag {
  font-size: 12px;
  padding: 4px 8px;
}

.table-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 问诊详情（抽屉） */
.consultation-detail {
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.patient-detail h3 {
  margin: 0 0 6px 0;
  font-size: 18px;
  font-weight: 800;
}

.patient-detail p {
  margin: 0 0 6px 0;
  color: var(--app-text-muted);
  font-size: 13px;
}

.tag-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-content {
  margin-bottom: 16px;
}

.detail-item {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
}

.item-label {
  min-width: 96px;
  color: var(--app-text-muted);
  font-size: 13px;
}

.item-value {
  color: var(--app-text);
  font-size: 13px;
  flex: 1;
}

.detail-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 16px;
}

/* 统计卡片样式 */
.statistics-container {
  padding: 16px 0 0;
}

.stat-card {
  height: 120px;
  display: flex;
  align-items: center;
}

@media (max-width: 960px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
  .header-actions {
    justify-content: flex-start;
  }
  .pending-consultations {
    grid-template-columns: 1fr;
  }
  .waiting-status {
    align-items: flex-start;
  }
}
</style>
