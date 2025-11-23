<template>
  <div class="departments-container">
    <div class="page-header">
      <h2>科室管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshDepartments">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        
        <el-button type="success" @click="addDepartment">
          <el-icon><Plus /></el-icon>
          添加科室
        </el-button>
      </div>
    </div>
    
    <el-row :gutter="20">
      <!-- 科室列表 -->
      <el-col :xs="24" :sm="24" :md="8" :lg="6" :xl="6">
        <el-card shadow="hover" class="department-list-card">
          <template #header>
            <div class="card-header">
              <span>科室列表</span>
              <el-input
                v-model="searchQuery"
                placeholder="搜索科室"
                clearable
                @input="handleSearch"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
          </template>
          
          <div v-loading="loading" class="department-list">
            <el-scrollbar height="calc(100vh - 280px)">
              <div
                v-for="item in filteredDepartments"
                :key="item.id"
                :class="['department-item', { active: currentDepartment && currentDepartment.id === item.id }]"
                @click="selectDepartment(item)"
              >
                <div class="department-item-content">
                  <el-avatar :size="40" :icon="OfficeBuilding" :style="{ backgroundColor: item.color }" />
                  <div class="department-info">
                    <div class="department-name">{{ item.name }}</div>
                    <div class="department-count">{{ item.doctorCount }}名医生</div>
                  </div>
                </div>
              </div>
              
              <el-empty v-if="filteredDepartments.length === 0" description="暂无科室数据" />
            </el-scrollbar>
          </div>
        </el-card>
      </el-col>
      
      <!-- 科室详情 -->
      <el-col :xs="24" :sm="24" :md="16" :lg="18" :xl="18">
        <el-card v-if="currentDepartment" shadow="hover" class="department-detail-card">
          <template #header>
            <div class="card-header">
              <span>科室详情</span>
              <div class="header-actions">
                <el-button type="primary" @click="editDepartment">
                  <el-icon><Edit /></el-icon>
                  编辑
                </el-button>
                <el-button type="danger" @click="deleteDepartment">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>
          </template>
          
          <div class="department-detail">
            <div class="department-header">
              <el-avatar :size="60" :icon="OfficeBuilding" :style="{ backgroundColor: currentDepartment.color }" />
              <div class="department-title">
                <h3>{{ currentDepartment.name }}</h3>
                <div class="department-meta">
                  <el-tag size="small">{{ currentDepartment.type }}</el-tag>
                  <span class="department-code">科室代码: {{ currentDepartment.code }}</span>
                </div>
              </div>
            </div>
            
            <el-divider />
            
            <div class="department-info-section">
              <div class="info-item">
                <span class="info-label">成立时间</span>
                <span class="info-value">{{ currentDepartment.establishDate }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">科室位置</span>
                <span class="info-value">{{ currentDepartment.location }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">联系电话</span>
                <span class="info-value">{{ currentDepartment.phone }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">科室主任</span>
                <span class="info-value">{{ currentDepartment.director }}</span>
              </div>
              <div class="info-item full-width">
                <span class="info-label">科室简介</span>
                <span class="info-value description">{{ currentDepartment.description }}</span>
              </div>
            </div>
            
            <el-divider />
            
            <div class="department-stats">
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="stat-card">
                    <div class="stat-value">{{ currentDepartment.doctorCount }}</div>
                    <div class="stat-label">医生数量</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-card">
                    <div class="stat-value">{{ currentDepartment.stats.appointmentsMonthly }}</div>
                    <div class="stat-label">月均预约量</div>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="stat-card">
                    <div class="stat-value">{{ currentDepartment.stats.consultationsMonthly }}</div>
                    <div class="stat-label">月均问诊量</div>
                  </div>
                </el-col>
              </el-row>
            </div>
            
            <el-divider />
            
            <div class="department-charts">
              <el-tabs v-model="activeTab">
                <el-tab-pane label="预约趋势" name="appointments">
                  <div class="chart-container" ref="appointmentChartRef"></div>
                </el-tab-pane>
                <el-tab-pane label="医生分布" name="doctors">
                  <div class="chart-container" ref="doctorChartRef"></div>
                </el-tab-pane>
                <el-tab-pane label="收入分析" name="revenue">
                  <div class="chart-container" ref="revenueChartRef"></div>
                </el-tab-pane>
              </el-tabs>
            </div>
            
            <el-divider />
            
            <div class="department-doctors">
              <h4>科室医生 ({{ currentDepartment.doctorCount }})</h4>
              <el-table :data="currentDepartment.doctors" style="width: 100%">
                <el-table-column label="医生信息" width="200">
                  <template #default="{ row }">
                    <div class="doctor-info">
                      <el-avatar :size="40" :src="row.avatar" />
                      <div class="doctor-name-info">
                        <div class="doctor-name">{{ row.name }}</div>
                        <div class="doctor-gender">{{ row.gender }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="title" label="职称" width="120" />
                <el-table-column prop="phone" label="联系电话" width="140" />
                <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)">
                      {{ row.status }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-card>
        
        <el-empty v-else description="请选择科室查看详情" />
      </el-col>
    </el-row>
    
    <!-- 添加/编辑科室对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑科室信息' : '添加新科室'"
      width="50%"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="departmentFormRef"
        :model="departmentForm"
        :rules="departmentRules"
        label-width="100px"
        class="department-form"
      >
        <el-form-item label="科室名称" prop="name">
          <el-input v-model="departmentForm.name" placeholder="请输入科室名称" />
        </el-form-item>
        
        <el-form-item label="科室代码" prop="code">
          <el-input v-model="departmentForm.code" placeholder="请输入科室代码" :disabled="isEdit" />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科室类型" prop="type">
              <el-select v-model="departmentForm.type" placeholder="请选择科室类型" style="width: 100%">
                <el-option label="临床科室" value="临床科室" />
                <el-option label="医技科室" value="医技科室" />
                <el-option label="行政科室" value="行政科室" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成立时间" prop="establishDate">
              <el-date-picker
                v-model="departmentForm.establishDate"
                type="date"
                placeholder="选择成立时间"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科室位置" prop="location">
              <el-input v-model="departmentForm.location" placeholder="请输入科室位置" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="departmentForm.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科室主任" prop="director">
              <el-input v-model="departmentForm.director" placeholder="请输入科室主任姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室颜色" prop="color">
              <el-color-picker v-model="departmentForm.color" show-alpha />
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="科室简介" prop="description">
          <el-input
            v-model="departmentForm.description"
            type="textarea"
            :rows="4"
            placeholder="请输入科室简介"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, nextTick, watch } from 'vue';
import { Search, Refresh, Plus, Edit, Delete, OfficeBuilding } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent
} from 'echarts/components';
import { LabelLayout, UniversalTransition } from 'echarts/features';
import { CanvasRenderer } from 'echarts/renderers';

// 注册 ECharts 组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  BarChart,
  LineChart,
  PieChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
]);

// 状态和数据
const loading = ref(false);
const departments = ref([]);
const currentDepartment = ref(null);
const searchQuery = ref('');
const activeTab = ref('appointments');

// 图表引用
const appointmentChartRef = ref(null);
const doctorChartRef = ref(null);
const revenueChartRef = ref(null);
let appointmentChart = null;
let doctorChart = null;
let revenueChart = null;

// 对话框
const dialogVisible = ref(false);
const isEdit = ref(false);
const departmentFormRef = ref(null);

// 表单数据
const departmentForm = reactive({
  id: '',
  name: '',
  code: '',
  type: '临床科室',
  establishDate: '',
  location: '',
  phone: '',
  director: '',
  color: '#409EFF',
  description: ''
});

// 表单验证规则
const departmentRules = {
  name: [
    { required: true, message: '请输入科室名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入科室代码', trigger: 'blur' },
    { pattern: /^[A-Z0-9]+$/, message: '科室代码只能包含大写字母和数字', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择科室类型', trigger: 'change' }
  ],
  establishDate: [
    { required: true, message: '请选择成立时间', trigger: 'change' }
  ],
  location: [
    { required: true, message: '请输入科室位置', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^\d{3,4}-\d{7,8}$|^\d{7,12}$/, message: '请输入正确的电话号码', trigger: 'blur' }
  ],
  director: [
    { required: true, message: '请输入科室主任姓名', trigger: 'blur' }
  ],
  color: [
    { required: true, message: '请选择科室颜色', trigger: 'change' }
  ],
  description: [
    { required: true, message: '请输入科室简介', trigger: 'blur' },
    { max: 500, message: '科室简介不能超过500字', trigger: 'blur' }
  ]
};

// 计算属性
const filteredDepartments = computed(() => {
  if (!searchQuery.value) {
    return departments.value;
  }
  
  const query = searchQuery.value.toLowerCase();
  return departments.value.filter(item => 
    item.name.toLowerCase().includes(query) || 
    item.code.toLowerCase().includes(query)
  );
});

// 方法
function getStatusType(status) {
  switch (status) {
    case '在职': return 'success';
    case '休假': return 'warning';
    case '离职': return 'info';
    default: return '';
  }
}

async function fetchDepartments() {
  loading.value = true;
  try {
    // 模拟API调用
    // const response = await getDepartments();
    // departments.value = response.data;
    
    // 模拟数据
    setTimeout(() => {
      const mockDepartments = [
        {
          id: 1,
          name: '内科',
          code: 'NK',
          type: '临床科室',
          establishDate: '1990-05-15',
          location: '门诊楼2层',
          phone: '010-12345678',
          director: '张主任',
          color: '#409EFF',
          description: '内科是临床医学的一个专科，内科学是研究内脏器官疾病的病因、病理、诊断及治疗的一门学科。内科涵盖了心脏病学、肺病学、消化病学、肾脏病学、内分泌学、血液学、风湿病学、传染病学等多个亚专科。',
          doctorCount: 15,
          stats: {
            appointmentsMonthly: 450,
            consultationsMonthly: 380,
            revenueMonthly: 125000
          },
          doctors: generateMockDoctors(15, '内科')
        },
        {
          id: 2,
          name: '外科',
          code: 'WK',
          type: '临床科室',
          establishDate: '1990-06-20',
          location: '门诊楼3层',
          phone: '010-12345679',
          director: '李主任',
          color: '#67C23A',
          description: '外科是研究外科疾病的发生、发展规律及其临床表现，诊断、预防和治疗的科学。外科包括普通外科、骨科、神经外科、心胸外科、泌尿外科、整形外科等多个亚专科。',
          doctorCount: 18,
          stats: {
            appointmentsMonthly: 520,
            consultationsMonthly: 420,
            revenueMonthly: 180000
          },
          doctors: generateMockDoctors(18, '外科')
        },
        {
          id: 3,
          name: '妇产科',
          code: 'FCK',
          type: '临床科室',
          establishDate: '1991-03-10',
          location: '门诊楼4层',
          phone: '010-12345680',
          director: '王主任',
          color: '#E6A23C',
          description: '妇产科是专门研究女性生殖器官疾病和诊治妊娠、分娩的科室。妇产科包括妇科和产科两部分，妇科主要研究女性生殖系统疾病，产科主要研究妊娠和分娩相关问题。',
          doctorCount: 12,
          stats: {
            appointmentsMonthly: 380,
            consultationsMonthly: 320,
            revenueMonthly: 135000
          },
          doctors: generateMockDoctors(12, '妇产科')
        },
        {
          id: 4,
          name: '儿科',
          code: 'EK',
          type: '临床科室',
          establishDate: '1991-05-18',
          location: '门诊楼1层',
          phone: '010-12345681',
          director: '赵主任',
          color: '#F56C6C',
          description: '儿科是专门研究儿童时期身心发育、疾病防治的医学专科。儿科的服务对象是从出生到18岁的儿童和青少年，主要研究小儿生长发育规律、营养需求以及疾病的诊断和治疗。',
          doctorCount: 14,
          stats: {
            appointmentsMonthly: 420,
            consultationsMonthly: 350,
            revenueMonthly: 110000
          },
          doctors: generateMockDoctors(14, '儿科')
        },
        {
          id: 5,
          name: '眼科',
          code: 'YK',
          type: '临床科室',
          establishDate: '1992-09-25',
          location: '门诊楼5层',
          phone: '010-12345682',
          director: '钱主任',
          color: '#909399',
          description: '眼科是研究眼睛及其附属器官的生理、病理以及诊断、治疗和预防眼科疾病的专科。眼科主要处理各种眼部疾病，如白内障、青光眼、视网膜疾病、角膜疾病等。',
          doctorCount: 8,
          stats: {
            appointmentsMonthly: 280,
            consultationsMonthly: 230,
            revenueMonthly: 95000
          },
          doctors: generateMockDoctors(8, '眼科')
        },
        {
          id: 6,
          name: '耳鼻喉科',
          code: 'EBHK',
          type: '临床科室',
          establishDate: '1992-11-15',
          location: '门诊楼5层',
          phone: '010-12345683',
          director: '孙主任',
          color: '#9B59B6',
          description: '耳鼻喉科是研究耳、鼻、咽、喉及颈部疾病的诊断与治疗的专科。耳鼻喉科主要处理听力障碍、鼻窦炎、咽喉炎、声带疾病等问题。',
          doctorCount: 7,
          stats: {
            appointmentsMonthly: 250,
            consultationsMonthly: 210,
            revenueMonthly: 85000
          },
          doctors: generateMockDoctors(7, '耳鼻喉科')
        },
        {
          id: 7,
          name: '口腔科',
          code: 'KQK',
          type: '临床科室',
          establishDate: '1993-04-20',
          location: '门诊楼6层',
          phone: '010-12345684',
          director: '周主任',
          color: '#3498DB',
          description: '口腔科是研究牙齿、口腔颌面部疾病的诊断与治疗的专科。口腔科主要处理龋齿、牙周病、口腔粘膜疾病、颞下颌关节疾病等问题。',
          doctorCount: 10,
          stats: {
            appointmentsMonthly: 320,
            consultationsMonthly: 270,
            revenueMonthly: 150000
          },
          doctors: generateMockDoctors(10, '口腔科')
        },
        {
          id: 8,
          name: '皮肤科',
          code: 'PFK',
          type: '临床科室',
          establishDate: '1993-08-12',
          location: '门诊楼2层',
          phone: '010-12345685',
          director: '吴主任',
          color: '#1ABC9C',
          description: '皮肤科是研究皮肤及其附属器官疾病的诊断与治疗的专科。皮肤科主要处理各种皮肤病，如湿疹、银屑病、痤疮、荨麻疹等。',
          doctorCount: 6,
          stats: {
            appointmentsMonthly: 220,
            consultationsMonthly: 190,
            revenueMonthly: 75000
          },
          doctors: generateMockDoctors(6, '皮肤科')
        },
        {
          id: 9,
          name: '神经科',
          code: 'SJK',
          type: '临床科室',
          establishDate: '1994-03-05',
          location: '门诊楼3层',
          phone: '010-12345686',
          director: '郑主任',
          color: '#E74C3C',
          description: '神经科是研究神经系统疾病的诊断与治疗的专科。神经科主要处理脑血管疾病、癫痫、帕金森病、多发性硬化症等神经系统疾病。',
          doctorCount: 9,
          stats: {
            appointmentsMonthly: 290,
            consultationsMonthly: 240,
            revenueMonthly: 105000
          },
          doctors: generateMockDoctors(9, '神经科')
        },
        {
          id: 10,
          name: '精神科',
          code: 'JSK',
          type: '临床科室',
          establishDate: '1994-09-18',
          location: '门诊楼7层',
          phone: '010-12345687',
          director: '陈主任',
          color: '#2ECC71',
          description: '精神科是研究精神疾病的诊断与治疗的专科。精神科主要处理抑郁症、焦虑症、精神分裂症、双相情感障碍等精神疾病。',
          doctorCount: 7,
          stats: {
            appointmentsMonthly: 180,
            consultationsMonthly: 150,
            revenueMonthly: 65000
          },
          doctors: generateMockDoctors(7, '精神科')
        },
        {
          id: 11,
          name: '中医科',
          code: 'ZYK',
          type: '临床科室',
          establishDate: '1995-05-10',
          location: '门诊楼1层',
          phone: '010-12345688',
          director: '林主任',
          color: '#F39C12',
          description: '中医科是运用中医理论和方法诊断与治疗疾病的专科。中医科主要采用中药、针灸、推拿等传统疗法治疗各种疾病。',
          doctorCount: 12,
          stats: {
            appointmentsMonthly: 350,
            consultationsMonthly: 300,
            revenueMonthly: 120000
          },
          doctors: generateMockDoctors(12, '中医科')
        },
        {
          id: 12,
          name: '康复科',
          code: 'KFK',
          type: '医技科室',
          establishDate: '1996-02-28',
          location: '医技楼1层',
          phone: '010-12345689',
          director: '黄主任',
          color: '#16A085',
          description: '康复科是运用物理疗法、作业疗法、言语疗法等手段帮助患者恢复功能的专科。康复科主要处理各种运动功能障碍、言语障碍等问题。',
          doctorCount: 8,
          stats: {
            appointmentsMonthly: 200,
            consultationsMonthly: 170,
            revenueMonthly: 70000
          },
          doctors: generateMockDoctors(8, '康复科')
        },
        {
          id: 13,
          name: '影像科',
          code: 'YXK',
          type: '医技科室',
          establishDate: '1996-08-15',
          location: '医技楼2层',
          phone: '010-12345690',
          director: '徐主任',
          color: '#8E44AD',
          description: '影像科是运用各种影像技术诊断疾病的专科。影像科主要包括X线、CT、MRI、超声等检查方法，为临床诊断提供影像学依据。',
          doctorCount: 10,
          stats: {
            appointmentsMonthly: 0,
            consultationsMonthly: 0,
            revenueMonthly: 200000
          },
          doctors: generateMockDoctors(10, '影像科')
        },
        {
          id: 14,
          name: '检验科',
          code: 'JYK',
          type: '医技科室',
          establishDate: '1997-03-20',
          location: '医技楼3层',
          phone: '010-12345691',
          director: '马主任',
          color: '#D35400',
          description: '检验科是运用各种实验室技术检测人体样本的专科。检验科主要进行血液、尿液、粪便等各种生物样本的检测，为临床诊断提供实验室依据。',
          doctorCount: 9,
          stats: {
            appointmentsMonthly: 0,
            consultationsMonthly: 0,
            revenueMonthly: 180000
          },
          doctors: generateMockDoctors(9, '检验科')
        },
        {
          id: 15,
          name: '急诊科',
          code: 'JZK',
          type: '临床科室',
          establishDate: '1997-11-08',
          location: '急诊楼1层',
          phone: '010-12345692',
          director: '胡主任',
          color: '#C0392B',
          description: '急诊科是为急危重症患者提供紧急救治的专科。急诊科全天候运作，主要处理各种急症，如创伤、急性心肌梗死、脑卒中、急性腹症等。',
          doctorCount: 16,
          stats: {
            appointmentsMonthly: 0,
            consultationsMonthly: 650,
            revenueMonthly: 220000
          },
          doctors: generateMockDoctors(16, '急诊科')
        }
      ];
      
      departments.value = mockDepartments;
      loading.value = false;
      
      // 如果没有选中的科室，默认选择第一个
      if (!currentDepartment.value && mockDepartments.length > 0) {
        selectDepartment(mockDepartments[0]);
      }
    }, 800);
  } catch (error) {
    console.error('获取科室列表失败:', error);
    ElMessage.error('获取科室列表失败');
    loading.value = false;
  }
}

function generateMockDoctors(count, department) {
  const doctors = [];
  const titles = ['主任医师', '副主任医师', '主治医师', '住院医师', '医士'];
  const statuses = ['在职', '在职', '在职', '在职', '休假', '离职'];
  
  for (let i = 1; i <= count; i++) {
    const gender = Math.random() > 0.5 ? '男' : '女';
    const titleIndex = Math.floor(Math.random() * titles.length);
    const statusIndex = Math.floor(Math.random() * statuses.length);
    
    doctors.push({
      id: `D${String(Math.floor(Math.random() * 90000) + 10000)}`,
      name: `${gender === '男' ? '张' : '李'}医生${Math.floor(Math.random() * 100) + 1}`,
      gender: gender,
      title: titles[titleIndex],
      phone: `1${Math.floor(Math.random() * 9 + 1)}${String(Math.floor(Math.random() * 1000000000)).padStart(9, '0')}`,
      email: `doctor${Math.floor(Math.random() * 1000)}@hospital.com`,
      avatar: `https://randomuser.me/api/portraits/${gender === '男' ? 'men' : 'women'}/${Math.floor(Math.random() * 100)}.jpg`,
      department: department,
      status: statuses[statusIndex]
    });
  }
  
  return doctors;
}

function refreshDepartments() {
  fetchDepartments();
  ElMessage.success('科室列表已刷新');
}

function handleSearch() {
  // 如果搜索结果中没有当前选中的科室，取消选中
  if (currentDepartment.value && !filteredDepartments.value.some(item => item.id === currentDepartment.value.id)) {
    currentDepartment.value = null;
  }
}

function selectDepartment(department) {
  currentDepartment.value = department;
  activeTab.value = 'appointments';
  
  // 在下一个 tick 初始化图表
  nextTick(() => {
    initCharts();
  });
}

function initCharts() {
  if (!currentDepartment.value) return;
  
  // 预约趋势图表
  if (appointmentChartRef.value) {
    if (!appointmentChart) {
      appointmentChart = echarts.init(appointmentChartRef.value);
    }
    
    const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
    const appointmentData = months.map(() => Math.floor(Math.random() * 200) + 100);
    
    const option = {
      title: {
        text: '近12个月预约趋势',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: months
      },
      yAxis: {
        type: 'value',
        name: '预约数量'
      },
      series: [{
        name: '预约数量',
        type: 'line',
        data: appointmentData,
        smooth: true,
        lineStyle: {
          width: 3,
          color: currentDepartment.value.color
        },
        itemStyle: {
          color: currentDepartment.value.color
        },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [{
              offset: 0,
              color: currentDepartment.value.color + 'AA' // 透明度 0.67
            }, {
              offset: 1,
              color: currentDepartment.value.color + '11' // 透明度 0.07
            }]
          }
        }
      }]
    };
    
    appointmentChart.setOption(option);
  }
  
  // 医生职称分布图表
  if (doctorChartRef.value) {
    if (!doctorChart) {
      doctorChart = echarts.init(doctorChartRef.value);
    }
    
    // 统计医生职称分布
    const titleCounts = {};
    currentDepartment.value.doctors.forEach(doctor => {
      if (!titleCounts[doctor.title]) {
        titleCounts[doctor.title] = 0;
      }
      titleCounts[doctor.title]++;
    });
    
    const titles = Object.keys(titleCounts);
    const counts = titles.map(title => titleCounts[title]);
    
    const option = {
      title: {
        text: '医生职称分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{a} <br/>{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        data: titles
      },
      series: [{
        name: '职称分布',
        type: 'pie',
        radius: ['40%', '70%'],
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
        data: titles.map((title, index) => ({
          value: counts[index],
          name: title
        }))
      }]
    };
    
    doctorChart.setOption(option);
  }
  
  // 收入分析图表
  if (revenueChartRef.value) {
    if (!revenueChart) {
      revenueChart = echarts.init(revenueChartRef.value);
    }
    
    const categories = ['门诊收入', '住院收入', '手术收入', '药品收入', '检查收入'];
    const revenueData = categories.map(() => Math.floor(Math.random() * 50000) + 10000);
    
    const option = {
      title: {
        text: '收入构成分析',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      xAxis: {
        type: 'category',
        data: categories
      },
      yAxis: {
        type: 'value',
        name: '金额 (元)'
      },
      series: [{
        name: '收入金额',
        type: 'bar',
        data: revenueData,
        itemStyle: {
          color: function(params) {
            const colorList = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399'];
            return colorList[params.dataIndex];
          }
        },
        label: {
          show: true,
          position: 'top',
          formatter: '{c} 元'
        }
      }]
    };
    
    revenueChart.setOption(option);
  }
}

function addDepartment() {
  resetForm();
  isEdit.value = false;
  dialogVisible.value = true;
}

function editDepartment() {
  if (!currentDepartment.value) return;
  
  resetForm();
  isEdit.value = true;
  
  // 复制科室信息到表单
  Object.keys(departmentForm).forEach(key => {
    if (key in currentDepartment.value) {
      departmentForm[key] = currentDepartment.value[key];
    }
  });
  
  dialogVisible.value = true;
}

function deleteDepartment() {
  if (!currentDepartment.value) return;
  
  ElMessageBox.confirm(
    `确认删除科室 ${currentDepartment.value.name} 吗？此操作不可逆。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 模拟API调用
    // await deleteDepartmentById(currentDepartment.value.id);
    
    // 模拟成功响应
    setTimeout(() => {
      // 从列表中移除
      departments.value = departments.value.filter(item => item.id !== currentDepartment.value.id);
      
      // 重置当前选中
      currentDepartment.value = departments.value.length > 0 ? departments.value[0] : null;
      
      ElMessage.success('科室删除成功');
      
      // 如果还有科室，重新初始化图表
      if (currentDepartment.value) {
        nextTick(() => {
          initCharts();
        });
      }
    }, 500);
  }).catch(() => {});
}

function resetForm() {
  // 重置表单数据
  Object.assign(departmentForm, {
    id: '',
    name: '',
    code: '',
    type: '临床科室',
    establishDate: '',
    location: '',
    phone: '',
    director: '',
    color: '#409EFF',
    description: ''
  });
  
  // 如果表单引用存在，重置验证
  if (departmentFormRef.value) {
    departmentFormRef.value.resetFields();
  }
}

function handleDialogClose(done) {
  ElMessageBox.confirm(
    '确认关闭？未保存的数据将会丢失',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    resetForm();
    done();
  }).catch(() => {});
}

function submitForm() {
  departmentFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // 如果是编辑模式，调用更新API
        if (isEdit.value) {
          // await updateDepartment(departmentForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 更新本地数据
            const index = departments.value.findIndex(item => item.id === departmentForm.id);
            if (index !== -1) {
              // 保留原有的其他属性
              const updatedDepartment = { ...departments.value[index] };
              
              // 更新表单中的属性
              Object.keys(departmentForm).forEach(key => {
                updatedDepartment[key] = departmentForm[key];
              });
              
              departments.value[index] = updatedDepartment;
              
              // 更新当前选中的科室
              if (currentDepartment.value && currentDepartment.value.id === updatedDepartment.id) {
                currentDepartment.value = updatedDepartment;
              }
            }
            
            ElMessage.success('科室信息更新成功');
            dialogVisible.value = false;
            loading.value = false;
            
            // 重新初始化图表
            nextTick(() => {
              initCharts();
            });
          }, 1000);
        } else {
          // 如果是添加模式，调用创建API
          // await createDepartment(departmentForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 创建新科室对象
            const newId = Math.max(...departments.value.map(item => item.id)) + 1;
            const newDepartment = {
              ...departmentForm,
              id: newId,
              doctorCount: 0,
              stats: {
                appointmentsMonthly: 0,
                consultationsMonthly: 0,
                revenueMonthly: 0
              },
              doctors: []
            };
            
            // 添加到列表
            departments.value.push(newDepartment);
            
            // 选中新创建的科室
            currentDepartment.value = newDepartment;
            
            ElMessage.success('科室添加成功');
            dialogVisible.value = false;
            loading.value = false;
            
            // 初始化图表
            nextTick(() => {
              initCharts();
            });
          }, 1000);
        }
      } catch (error) {
        console.error('提交表单失败:', error);
        ElMessage.error('操作失败，请重试');
        loading.value = false;
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
      return false;
    }
  });
}

// 监听窗口大小变化，重新调整图表大小
function handleResize() {
  if (appointmentChart) appointmentChart.resize();
  if (doctorChart) doctorChart.resize();
  if (revenueChart) revenueChart.resize();
}

// 监听标签页切换，初始化相应图表
watch(activeTab, () => {
  nextTick(() => {
    initCharts();
  });
});

// 生命周期钩子
onMounted(() => {
  fetchDepartments();
  window.addEventListener('resize', handleResize);
});

// 组件卸载前移除事件监听
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (appointmentChart) appointmentChart.dispose();
  if (doctorChart) doctorChart.dispose();
  if (revenueChart) revenueChart.dispose();
});
</script>

<style scoped>
.departments-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 500;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.department-list-card {
  height: calc(100vh - 180px);
}

.department-list {
  margin-top: 10px;
}

.department-item {
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: all 0.3s;
}

.department-item:hover {
  background-color: #f5f7fa;
  transform: translateY(-2px);
}

.department-item.active {
  background-color: #ecf5ff;
  border-left: 3px solid #409EFF;
}

.department-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
}

.department-info {
  flex: 1;
}

.department-name {
  font-weight: 500;
  font-size: 16px;
  margin-bottom: 4px;
}

.department-count {
  font-size: 12px;
  color: #909399;
}

.department-detail-card {
  height: calc(100vh - 180px);
  overflow-y: auto;
}

.department-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.department-title {
  flex: 1;
}

.department-title h3 {
  margin: 0 0 10px 0;
  font-size: 22px;
  font-weight: 500;
}

.department-meta {
  display: flex;
  align-items: center;
  gap: 15px;
}

.department-code {
  font-size: 14px;
  color: #606266;
}

.department-info-section {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
}

.info-item {
  width: calc(50% - 8px);
  display: flex;
  flex-direction: column;
}

.info-item.full-width {
  width: 100%;
}

.info-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.info-value {
  font-size: 16px;
  color: #303133;
}

.info-value.description {
  line-height: 1.6;
  white-space: pre-line;
}

.department-stats {
  margin-bottom: 20px;
}

.stat-card {
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  color: #909399;
}

.department-charts {
  margin-bottom: 20px;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.department-doctors {
  margin-bottom: 20px;
}

.department-doctors h4 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 18px;
  font-weight: 500;
}

.doctor-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.doctor-name-info {
  display: flex;
  flex-direction: column;
}

.doctor-name {
  font-weight: 500;
}

.doctor-gender {
  font-size: 12px;
  color: #909399;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .info-item {
    width: 100%;
  }
  
  .department-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

/* 动画效果 */
.el-table :deep(tbody tr) {
  transition: all 0.3s;
}

.el-table :deep(tbody tr:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1;
  position: relative;
}
</style>