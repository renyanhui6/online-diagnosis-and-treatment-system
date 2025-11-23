<template>
  <div class="doctors-container">
    <div class="page-header">
      <h2>医生管理</h2>
      <div class="header-actions">
        <el-select v-model="filterDepartment" placeholder="科室" clearable @change="handleFilterChange">
          <el-option v-for="item in departmentOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-select v-model="filterTitle" placeholder="职称" clearable @change="handleFilterChange">
          <el-option v-for="item in titleOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleFilterChange">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索医生姓名/工号"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-button type="primary" @click="refreshDoctors">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        
        <el-button type="success" @click="addDoctor">
          <el-icon><Plus /></el-icon>
          添加医生
        </el-button>
      </div>
    </div>
    
    <el-card shadow="hover" class="doctors-card">
      <el-table
        v-loading="loading"
        :data="paginatedDoctors"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="doctor-expand">
              <div class="expand-section">
                <h4>个人信息</h4>
                <div class="expand-content">
                  <div class="expand-item">
                    <span class="expand-label">出生日期：</span>
                    <span>{{ props.row.birthDate }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">身份证号：</span>
                    <span>{{ maskIdCard(props.row.idCard) }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">手机号码：</span>
                    <span>{{ props.row.phone }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">邮箱地址：</span>
                    <span>{{ props.row.email }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">家庭住址：</span>
                    <span>{{ props.row.address }}</span>
                  </div>
                </div>
              </div>
              
              <div class="expand-section">
                <h4>工作信息</h4>
                <div class="expand-content">
                  <div class="expand-item">
                    <span class="expand-label">入职日期：</span>
                    <span>{{ props.row.joinDate }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">专业特长：</span>
                    <span>
                      <el-tag 
                        v-for="(skill, index) in props.row.skills" 
                        :key="index"
                        size="small"
                        class="skill-tag"
                      >
                        {{ skill }}
                      </el-tag>
                    </span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">个人简介：</span>
                    <span>{{ props.row.biography }}</span>
                  </div>
                </div>
              </div>
              
              <div class="expand-section">
                <h4>工作统计</h4>
                <div class="expand-content">
                  <div class="expand-item">
                    <span class="expand-label">问诊数量：</span>
                    <span>{{ props.row.stats.consultations }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">处方数量：</span>
                    <span>{{ props.row.stats.prescriptions }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">预约数量：</span>
                    <span>{{ props.row.stats.appointments }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">平均评分：</span>
                    <span>
                      <el-rate 
                        v-model="props.row.stats.rating" 
                        disabled 
                        show-score 
                        text-color="#ff9900"
                        score-template="{value}"
                      />
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="id" label="工号" width="100" />
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
        <el-table-column prop="department" label="科室" width="120" />
        <el-table-column prop="title" label="职称" width="120" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="joinDate" label="入职日期" width="120" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button 
                type="primary" 
                size="small"
                @click.stop="editDoctor(row)"
              >
                编辑
              </el-button>
              <el-button 
                v-if="row.status === '在职'"
                type="warning" 
                size="small"
                @click.stop="changeStatus(row, '休假')"
              >
                休假
              </el-button>
              <el-button 
                v-if="row.status === '休假'"
                type="success" 
                size="small"
                @click.stop="changeStatus(row, '在职')"
              >
                复职
              </el-button>
              <el-button 
                v-if="row.status !== '离职'"
                type="danger" 
                size="small"
                @click.stop="changeStatus(row, '离职')"
              >
                离职
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
          :total="filteredDoctors.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑医生对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑医生信息' : '添加新医生'"
      width="60%"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="doctorFormRef"
        :model="doctorForm"
        :rules="doctorRules"
        label-width="100px"
        class="doctor-form"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="doctorForm.name" placeholder="请输入医生姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="doctorForm.gender">
                    <el-radio label="男">男</el-radio>
                    <el-radio label="女">女</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker
                    v-model="doctorForm.birthDate"
                    type="date"
                    placeholder="选择出生日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="身份证号" prop="idCard">
                  <el-input v-model="doctorForm.idCard" placeholder="请输入身份证号" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="doctorForm.phone" placeholder="请输入手机号码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="doctorForm.email" placeholder="请输入邮箱地址" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="家庭住址" prop="address">
              <el-input v-model="doctorForm.address" placeholder="请输入家庭住址" />
            </el-form-item>
            
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleAvatarChange"
              >
                <img v-if="doctorForm.avatar" :src="doctorForm.avatar" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane label="工作信息" name="work">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="工号" prop="id">
                  <el-input 
                    v-model="doctorForm.id" 
                    placeholder="请输入工号" 
                    :disabled="isEdit"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="入职日期" prop="joinDate">
                  <el-date-picker
                    v-model="doctorForm.joinDate"
                    type="date"
                    placeholder="选择入职日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="科室" prop="department">
                  <el-select v-model="doctorForm.department" placeholder="请选择科室" style="width: 100%">
                    <el-option
                      v-for="item in departmentOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职称" prop="title">
                  <el-select v-model="doctorForm.title" placeholder="请选择职称" style="width: 100%">
                    <el-option
                      v-for="item in titleOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="状态" prop="status">
              <el-select v-model="doctorForm.status" placeholder="请选择状态" style="width: 100%">
                <el-option
                  v-for="item in statusOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="专业特长" prop="skills">
              <el-select
                v-model="doctorForm.skills"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="请输入专业特长"
                style="width: 100%"
              >
                <el-option
                  v-for="item in skillOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
            
            <el-form-item label="个人简介" prop="biography">
              <el-input
                v-model="doctorForm.biography"
                type="textarea"
                :rows="4"
                placeholder="请输入个人简介"
              />
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane label="账号信息" name="account">
            <el-alert
              v-if="isEdit"
              title="修改密码将会重置医生的登录密码，请谨慎操作"
              type="warning"
              :closable="false"
              show-icon
              style="margin-bottom: 20px"
            />
            
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="doctorForm.username" 
                placeholder="请输入用户名" 
                :disabled="isEdit"
              />
            </el-form-item>
            
            <el-form-item :label="isEdit ? '新密码' : '密码'" prop="password">
              <el-input
                v-model="doctorForm.password"
                type="password"
                placeholder="请输入密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item v-if="!isEdit" label="确认密码" prop="confirmPassword">
              <el-input
                v-model="doctorForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                show-password
              />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
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
import { ref, computed, reactive, onMounted } from 'vue';
import { Search, Refresh, Plus } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

// 状态和数据
const loading = ref(false);
const doctors = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterDepartment = ref('');
const filterTitle = ref('');
const filterStatus = ref('');
const searchQuery = ref('');

// 对话框
const dialogVisible = ref(false);
const isEdit = ref(false);
const activeTab = ref('basic');
const doctorFormRef = ref(null);

// 表单数据
const doctorForm = reactive({
  id: '',
  name: '',
  gender: '男',
  birthDate: '',
  idCard: '',
  phone: '',
  email: '',
  address: '',
  avatar: '',
  department: '',
  title: '',
  joinDate: '',
  status: '在职',
  skills: [],
  biography: '',
  username: '',
  password: '',
  confirmPassword: ''
});

// 表单验证规则
const doctorRules = {
  name: [
    { required: true, message: '请输入医生姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入家庭住址', trigger: 'blur' }
  ],
  id: [
    { required: true, message: '请输入工号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]+$/, message: '工号只能包含字母和数字', trigger: 'blur' }
  ],
  joinDate: [
    { required: true, message: '请选择入职日期', trigger: 'change' }
  ],
  department: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请选择职称', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  biography: [
    { max: 500, message: '个人简介不能超过500字', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '长度在 4 到 20 个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== doctorForm.password) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 选项数据
const departmentOptions = [
  { value: '内科', label: '内科' },
  { value: '外科', label: '外科' },
  { value: '妇产科', label: '妇产科' },
  { value: '儿科', label: '儿科' },
  { value: '眼科', label: '眼科' },
  { value: '耳鼻喉科', label: '耳鼻喉科' },
  { value: '口腔科', label: '口腔科' },
  { value: '皮肤科', label: '皮肤科' },
  { value: '神经科', label: '神经科' },
  { value: '精神科', label: '精神科' },
  { value: '中医科', label: '中医科' },
  { value: '康复科', label: '康复科' },
  { value: '影像科', label: '影像科' },
  { value: '检验科', label: '检验科' },
  { value: '急诊科', label: '急诊科' }
];

const titleOptions = [
  { value: '主任医师', label: '主任医师' },
  { value: '副主任医师', label: '副主任医师' },
  { value: '主治医师', label: '主治医师' },
  { value: '住院医师', label: '住院医师' },
  { value: '医士', label: '医士' }
];

const statusOptions = [
  { value: '在职', label: '在职' },
  { value: '休假', label: '休假' },
  { value: '离职', label: '离职' }
];

const skillOptions = [
  '内科诊断', '外科手术', '妇产科诊疗', '儿科诊疗', '眼科手术',
  '耳鼻喉诊疗', '口腔治疗', '皮肤病诊断', '神经系统疾病', '精神疾病诊断',
  '中医针灸', '中医推拿', '康复治疗', '影像诊断', '急诊抢救',
  '心血管疾病', '呼吸系统疾病', '消化系统疾病', '泌尿系统疾病', '内分泌疾病',
  '血液系统疾病', '风湿免疫疾病', '传染病诊治', '肿瘤诊治', '疼痛管理'
];

// 计算属性
const filteredDoctors = computed(() => {
  let result = [...doctors.value];
  
  // 科室筛选
  if (filterDepartment.value) {
    result = result.filter(item => item.department === filterDepartment.value);
  }
  
  // 职称筛选
  if (filterTitle.value) {
    result = result.filter(item => item.title === filterTitle.value);
  }
  
  // 状态筛选
  if (filterStatus.value) {
    result = result.filter(item => item.status === filterStatus.value);
  }
  
  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item => 
      item.name.toLowerCase().includes(query) || 
      item.id.toLowerCase().includes(query)
    );
  }
  
  return result;
});

const paginatedDoctors = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredDoctors.value.slice(start, end);
});

// 方法
function maskIdCard(idCard) {
  if (!idCard) return '';
  return idCard.replace(/^(.{6})(.*)(.{4})$/, '$1********$3');
}

function getStatusType(status) {
  switch (status) {
    case '在职': return 'success';
    case '休假': return 'warning';
    case '离职': return 'info';
    default: return '';
  }
}

async function fetchDoctors() {
  loading.value = true;
  try {
    // 模拟API调用
    // const response = await getDoctors();
    // doctors.value = response.data;
    
    // 模拟数据
    setTimeout(() => {
      const mockDoctors = [];
      const departments = departmentOptions.map(item => item.value);
      const titles = titleOptions.map(item => item.value);
      const statuses = statusOptions.map(item => item.value);
      
      for (let i = 1; i <= 50; i++) {
        const gender = Math.random() > 0.5 ? '男' : '女';
        const departmentIndex = Math.floor(Math.random() * departments.length);
        const titleIndex = Math.floor(Math.random() * titles.length);
        const statusIndex = Math.floor(Math.random() * statuses.length);
        
        // 生成随机出生日期（1970-1995年之间）
        const birthYear = 1970 + Math.floor(Math.random() * 25);
        const birthMonth = Math.floor(Math.random() * 12) + 1;
        const birthDay = Math.floor(Math.random() * 28) + 1;
        const birthDate = `${birthYear}-${String(birthMonth).padStart(2, '0')}-${String(birthDay).padStart(2, '0')}`;
        
        // 生成随机入职日期（2010-2023年之间）
        const joinYear = 2010 + Math.floor(Math.random() * 13);
        const joinMonth = Math.floor(Math.random() * 12) + 1;
        const joinDay = Math.floor(Math.random() * 28) + 1;
        const joinDate = `${joinYear}-${String(joinMonth).padStart(2, '0')}-${String(joinDay).padStart(2, '0')}`;
        
        // 随机选择3-5个技能
        const skillCount = Math.floor(Math.random() * 3) + 3; // 3-5个技能
        const shuffledSkills = [...skillOptions].sort(() => 0.5 - Math.random());
        const selectedSkills = shuffledSkills.slice(0, skillCount);
        
        mockDoctors.push({
          id: `D${String(i).padStart(5, '0')}`,
          name: `${gender === '男' ? '张' : '李'}医生${i}`,
          gender: gender,
          birthDate: birthDate,
          idCard: `11010119${birthYear.toString().substring(2)}${String(birthMonth).padStart(2, '0')}${String(birthDay).padStart(2, '0')}${Math.floor(Math.random() * 10000).toString().padStart(4, '0')}`,
          phone: `1${Math.floor(Math.random() * 9 + 1)}${String(Math.floor(Math.random() * 1000000000)).padStart(9, '0')}`,
          email: `doctor${i}@hospital.com`,
          address: `北京市海淀区医院路${Math.floor(Math.random() * 100) + 1}号`,
          avatar: `https://randomuser.me/api/portraits/${gender === '男' ? 'men' : 'women'}/${Math.floor(Math.random() * 100)}.jpg`,
          department: departments[departmentIndex],
          title: titles[titleIndex],
          joinDate: joinDate,
          status: statuses[statusIndex],
          skills: selectedSkills,
          biography: `${gender === '男' ? '张' : '李'}医生${i}，${titles[titleIndex]}，毕业于北京医科大学，从事${departments[departmentIndex]}临床工作${Math.floor(Math.random() * 20) + 5}年，擅长${selectedSkills.join('、')}等疾病的诊断与治疗。`,
          username: `doctor${i}`,
          stats: {
            consultations: Math.floor(Math.random() * 1000) + 100,
            prescriptions: Math.floor(Math.random() * 800) + 50,
            appointments: Math.floor(Math.random() * 1200) + 200,
            rating: (Math.random() * 1 + 4).toFixed(1) // 4.0-5.0之间的评分
          }
        });
      }
      
      doctors.value = mockDoctors;
      loading.value = false;
    }, 800);
  } catch (error) {
    console.error('获取医生列表失败:', error);
    ElMessage.error('获取医生列表失败');
    loading.value = false;
  }
}

function refreshDoctors() {
  fetchDoctors();
  ElMessage.success('医生列表已刷新');
}

function handleFilterChange() {
  currentPage.value = 1;
}

function handleSearch() {
  currentPage.value = 1;
}

function handleSizeChange(val) {
  pageSize.value = val;
  currentPage.value = 1;
}

function handleCurrentChange(val) {
  currentPage.value = val;
}

function handleRowClick(row) {
  // 行点击事件，可以展开行或者其他操作
}

function addDoctor() {
  resetForm();
  isEdit.value = false;
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function editDoctor(doctor) {
  resetForm();
  isEdit.value = true;
  
  // 复制医生信息到表单
  Object.keys(doctorForm).forEach(key => {
    if (key !== 'confirmPassword' && key in doctor) {
      doctorForm[key] = doctor[key];
    }
  });
  
  // 清空密码字段
  doctorForm.password = '';
  
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function changeStatus(doctor, newStatus) {
  const statusText = {
    '在职': '复职',
    '休假': '休假',
    '离职': '离职'
  }[newStatus];
  
  ElMessageBox.confirm(
    `确认将 ${doctor.name} 的状态修改为${newStatus}吗？`,
    `${statusText}确认`,
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: newStatus === '离职' ? 'warning' : 'info'
    }
  ).then(() => {
    // 模拟API调用
    // await updateDoctorStatus(doctor.id, newStatus);
    
    // 模拟成功响应
    setTimeout(() => {
      // 更新本地数据
      const index = doctors.value.findIndex(item => item.id === doctor.id);
      if (index !== -1) {
        doctors.value[index].status = newStatus;
      }
      
      ElMessage.success(`已将 ${doctor.name} 的状态修改为${newStatus}`);
    }, 500);
  }).catch(() => {});
}

function handleAvatarChange(file) {
  // 实际项目中应该上传到服务器
  // 这里模拟上传成功
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = () => {
    doctorForm.avatar = reader.result;
  };
}

function resetForm() {
  // 重置表单数据
  Object.assign(doctorForm, {
    id: '',
    name: '',
    gender: '男',
    birthDate: '',
    idCard: '',
    phone: '',
    email: '',
    address: '',
    avatar: '',
    department: '',
    title: '',
    joinDate: '',
    status: '在职',
    skills: [],
    biography: '',
    username: '',
    password: '',
    confirmPassword: ''
  });
  
  // 如果表单引用存在，重置验证
  if (doctorFormRef.value) {
    doctorFormRef.value.resetFields();
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
  doctorFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // 如果是编辑模式，调用更新API
        if (isEdit.value) {
          // await updateDoctor(doctorForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 更新本地数据
            const index = doctors.value.findIndex(item => item.id === doctorForm.id);
            if (index !== -1) {
              // 复制表单数据到医生对象
              const updatedDoctor = { ...doctors.value[index] };
              Object.keys(doctorForm).forEach(key => {
                if (key !== 'confirmPassword' && key !== 'password') {
                  updatedDoctor[key] = doctorForm[key];
                }
              });
              
              doctors.value[index] = updatedDoctor;
            }
            
            ElMessage.success('医生信息更新成功');
            dialogVisible.value = false;
            loading.value = false;
          }, 1000);
        } else {
          // 如果是添加模式，调用创建API
          // await createDoctor(doctorForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 创建新医生对象并添加到列表
            const newDoctor = { ...doctorForm };
            delete newDoctor.confirmPassword;
            
            // 添加统计数据
            newDoctor.stats = {
              consultations: 0,
              prescriptions: 0,
              appointments: 0,
              rating: 5.0
            };
            
            doctors.value.unshift(newDoctor);
            
            ElMessage.success('医生添加成功');
            dialogVisible.value = false;
            loading.value = false;
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

// 生命周期钩子
onMounted(() => {
  fetchDoctors();
});
</script>

<style scoped>
.doctors-container {
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

.doctors-card {
  margin-bottom: 20px;
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

.table-actions {
  display: flex;
  gap: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 展开行样式 */
.doctor-expand {
  padding: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}

.expand-section {
  flex: 1;
  min-width: 300px;
}

.expand-section h4 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.expand-content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.expand-item {
  width: calc(50% - 5px);
  display: flex;
  margin-bottom: 10px;
}

.expand-item.full-width {
  width: 100%;
}

.expand-label {
  width: 100px;
  color: #909399;
  font-size: 14px;
}

.skill-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

/* 对话框样式 */
.doctor-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
}

.avatar-uploader {
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #409EFF;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 100px;
  height: 100px;
  line-height: 100px;
  text-align: center;
}

.avatar {
  width: 100px;
  height: 100px;
  display: block;
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
  
  .expand-item {
    width: 100%;
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

/* 表格样式增强 */
.el-table :deep(th) {
  background-color: #f5f7fa !important;
}

.el-table :deep(.el-table__row:nth-child(even)) {
  background-color: #fafafa;
}
</style>