<template>
  <div class="patients-container">
    <div class="page-header">
      <h2>患者管理</h2>
      <div class="header-actions">
        <el-select v-model="filterGender" placeholder="性别" clearable @change="handleFilterChange">
          <el-option label="男" value="男" />
          <el-option label="女" value="女" />
        </el-select>
        
        <el-select v-model="filterAgeGroup" placeholder="年龄段" clearable @change="handleFilterChange">
          <el-option v-for="item in ageGroupOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleFilterChange">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索姓名/手机号/身份证"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-button type="primary" @click="refreshPatients">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        
        <el-button type="success" @click="addPatient">
          <el-icon><Plus /></el-icon>
          添加患者
        </el-button>
        
        <el-button type="info" @click="exportPatients">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>
    
    <el-card shadow="hover" class="patients-card">
      <el-table
        v-loading="loading"
        :data="paginatedPatients"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="patient-expand">
              <div class="expand-section">
                <h4>患者详情</h4>
                <div class="expand-content">
                  <div class="expand-item">
                    <span class="expand-label">身份证号：</span>
                    <span>{{ props.row.idCard }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">出生日期：</span>
                    <span>{{ props.row.birthDate }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">婚姻状况：</span>
                    <span>{{ props.row.maritalStatus }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">职业：</span>
                    <span>{{ props.row.occupation }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">联系地址：</span>
                    <span>{{ props.row.address }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">紧急联系人：</span>
                    <span>{{ props.row.emergencyContact }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">紧急联系电话：</span>
                    <span>{{ props.row.emergencyPhone }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">过敏史：</span>
                    <span>{{ props.row.allergies || '无' }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">既往病史：</span>
                    <span>{{ props.row.medicalHistory || '无' }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">家族病史：</span>
                    <span>{{ props.row.familyHistory || '无' }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">备注：</span>
                    <span>{{ props.row.notes || '无' }}</span>
                  </div>
                </div>
              </div>
              
              <div class="expand-section">
                <h4>就诊记录</h4>
                <el-table :data="props.row.visitRecords" style="width: 100%">
                  <el-table-column prop="date" label="就诊日期" width="120" />
                  <el-table-column prop="department" label="科室" width="120" />
                  <el-table-column prop="doctor" label="医生" width="120" />
                  <el-table-column prop="diagnosis" label="诊断结果" min-width="200" show-overflow-tooltip />
                  <el-table-column prop="type" label="就诊类型" width="100">
                    <template #default="scope">
                      <el-tag :type="scope.row.type === '门诊' ? 'success' : 'warning'">
                        {{ scope.row.type }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100" fixed="right">
                    <template #default="scope">
                      <el-button type="primary" size="small" @click.stop="viewVisitDetail(scope.row)">
                        详情
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="患者信息" min-width="200">
          <template #default="{ row }">
            <div class="patient-info">
              <div class="patient-avatar">
                <el-avatar :size="50" :src="row.avatar">
                  {{ row.name.charAt(0) }}
                </el-avatar>
              </div>
              <div class="patient-detail">
                <div class="patient-name">{{ row.name }}</div>
                <div class="patient-id">患者ID: {{ row.patientId }}</div>
                <div class="patient-tags">
                  <el-tag size="small" effect="plain" type="info">{{ row.gender }}</el-tag>
                  <el-tag size="small" effect="plain" type="info">{{ row.age }}岁</el-tag>
                  <el-tag 
                    v-if="row.tags && row.tags.length > 0" 
                    v-for="tag in row.tags" 
                    :key="tag"
                    size="small" 
                    effect="plain" 
                    type="warning"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="registrationDate" label="注册日期" width="120" sortable />
        <el-table-column prop="lastVisit" label="最近就诊" width="120" sortable />
        <el-table-column prop="visitCount" label="就诊次数" width="100" sortable />
        <el-table-column prop="insuranceType" label="医保类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getInsuranceType(row.insuranceType)" effect="plain">
              {{ row.insuranceType }}
            </el-tag>
          </template>
        </el-table-column>
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
                @click.stop="editPatient(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="success" 
                size="small"
                @click.stop="addVisitRecord(row)"
              >
                添加就诊
              </el-button>
              <el-dropdown 
                trigger="click" 
                @command="(command) => handleCommand(command, row)"
                @click.stop
              >
                <el-button size="small">
                  更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="view">查看详情</el-dropdown-item>
                    <el-dropdown-item command="records">就诊记录</el-dropdown-item>
                    <el-dropdown-item command="prescriptions">处方记录</el-dropdown-item>
                    <el-dropdown-item 
                      command="status" 
                      :disabled="row.status === '已注销'"
                    >
                      {{ row.status === '正常' ? '禁用账号' : '启用账号' }}
                    </el-dropdown-item>
                    <el-dropdown-item 
                      command="delete" 
                      divided 
                      style="color: #F56C6C;"
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          :total="filteredPatients.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑患者对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑患者信息' : '添加新患者'"
      width="60%"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="patientFormRef"
        :model="patientForm"
        :rules="patientRules"
        label-width="100px"
        class="patient-form"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" prop="name">
                  <el-input v-model="patientForm.name" placeholder="请输入姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="patientForm.gender">
                    <el-radio label="男">男</el-radio>
                    <el-radio label="女">女</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="身份证号" prop="idCard">
                  <el-input 
                    v-model="patientForm.idCard" 
                    placeholder="请输入身份证号" 
                    @blur="handleIdCardBlur"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="出生日期" prop="birthDate">
                  <el-date-picker
                    v-model="patientForm.birthDate"
                    type="date"
                    placeholder="选择出生日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="patientForm.phone" placeholder="请输入手机号码" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="婚姻状况" prop="maritalStatus">
                  <el-select v-model="patientForm.maritalStatus" placeholder="请选择婚姻状况" style="width: 100%">
                    <el-option label="未婚" value="未婚" />
                    <el-option label="已婚" value="已婚" />
                    <el-option label="离异" value="离异" />
                    <el-option label="丧偶" value="丧偶" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="职业" prop="occupation">
                  <el-input v-model="patientForm.occupation" placeholder="请输入职业" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="医保类型" prop="insuranceType">
                  <el-select v-model="patientForm.insuranceType" placeholder="请选择医保类型" style="width: 100%">
                    <el-option label="城镇职工医保" value="城镇职工医保" />
                    <el-option label="城乡居民医保" value="城乡居民医保" />
                    <el-option label="新农合" value="新农合" />
                    <el-option label="商业保险" value="商业保险" />
                    <el-option label="自费" value="自费" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="联系地址" prop="address">
              <el-input v-model="patientForm.address" placeholder="请输入联系地址" />
            </el-form-item>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="紧急联系人" prop="emergencyContact">
                  <el-input v-model="patientForm.emergencyContact" placeholder="请输入紧急联系人" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="紧急电话" prop="emergencyPhone">
                  <el-input v-model="patientForm.emergencyPhone" placeholder="请输入紧急联系电话" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleAvatarChange"
              >
                <img v-if="patientForm.avatar" :src="patientForm.avatar" class="avatar-image" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
              </el-upload>
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane label="健康信息" name="health">
            <el-form-item label="过敏史" prop="allergies">
              <el-input
                v-model="patientForm.allergies"
                type="textarea"
                :rows="3"
                placeholder="请输入过敏史，如无请留空"
              />
            </el-form-item>
            
            <el-form-item label="既往病史" prop="medicalHistory">
              <el-input
                v-model="patientForm.medicalHistory"
                type="textarea"
                :rows="3"
                placeholder="请输入既往病史，如无请留空"
              />
            </el-form-item>
            
            <el-form-item label="家族病史" prop="familyHistory">
              <el-input
                v-model="patientForm.familyHistory"
                type="textarea"
                :rows="3"
                placeholder="请输入家族病史，如无请留空"
              />
            </el-form-item>
            
            <el-form-item label="标签">
              <el-select
                v-model="patientForm.tags"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="请选择或创建标签"
                style="width: 100%"
              >
                <el-option
                  v-for="item in tagOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
              <div class="form-tip">可添加如"高血压"、"糖尿病"等健康标签</div>
            </el-form-item>
            
            <el-form-item label="备注" prop="notes">
              <el-input
                v-model="patientForm.notes"
                type="textarea"
                :rows="3"
                placeholder="请输入备注信息"
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
    
    <!-- 添加就诊记录对话框 -->
    <el-dialog
      v-model="visitDialogVisible"
      title="添加就诊记录"
      width="50%"
    >
      <el-form
        ref="visitFormRef"
        :model="visitForm"
        :rules="visitRules"
        label-width="100px"
      >
        <el-form-item label="患者姓名">
          <div>{{ visitForm.patientName }}</div>
        </el-form-item>
        
        <el-form-item label="就诊日期" prop="date">
          <el-date-picker
            v-model="visitForm.date"
            type="date"
            placeholder="选择就诊日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="就诊类型" prop="type">
          <el-radio-group v-model="visitForm.type">
            <el-radio label="门诊">门诊</el-radio>
            <el-radio label="急诊">急诊</el-radio>
            <el-radio label="住院">住院</el-radio>
            <el-radio label="复诊">复诊</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="科室" prop="department">
              <el-select v-model="visitForm.department" placeholder="请选择科室" style="width: 100%">
                <el-option
                  v-for="item in departmentOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="医生" prop="doctor">
              <el-select 
                v-model="visitForm.doctor" 
                placeholder="请选择医生"
                filterable
                style="width: 100%"
              >
                <el-option
                  v-for="item in doctorOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        
        <el-form-item label="主诉" prop="chiefComplaint">
          <el-input
            v-model="visitForm.chiefComplaint"
            type="textarea"
            :rows="2"
            placeholder="请输入主诉"
          />
        </el-form-item>
        
        <el-form-item label="诊断结果" prop="diagnosis">
          <el-input
            v-model="visitForm.diagnosis"
            type="textarea"
            :rows="3"
            placeholder="请输入诊断结果"
          />
        </el-form-item>
        
        <el-form-item label="治疗方案" prop="treatment">
          <el-input
            v-model="visitForm.treatment"
            type="textarea"
            :rows="3"
            placeholder="请输入治疗方案"
          />
        </el-form-item>
        
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="visitForm.notes"
            type="textarea"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="visitDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitVisitForm">确认</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 就诊详情对话框 -->
    <el-dialog
      v-model="visitDetailDialogVisible"
      title="就诊详情"
      width="60%"
    >
      <div v-if="selectedVisit" class="visit-detail">
        <div class="detail-header">
          <div class="detail-title">
            <h3>{{ selectedVisit.type }}记录</h3>
            <el-tag>{{ selectedVisit.date }}</el-tag>
          </div>
          <div class="detail-info">
            <span>科室：{{ selectedVisit.department }}</span>
            <span>医生：{{ selectedVisit.doctor }}</span>
          </div>
        </div>
        
        <el-divider />
        
        <div class="detail-section">
          <h4>主诉</h4>
          <p>{{ selectedVisit.chiefComplaint }}</p>
        </div>
        
        <div class="detail-section">
          <h4>诊断结果</h4>
          <p>{{ selectedVisit.diagnosis }}</p>
        </div>
        
        <div class="detail-section">
          <h4>治疗方案</h4>
          <p>{{ selectedVisit.treatment }}</p>
        </div>
        
        <div v-if="selectedVisit.notes" class="detail-section">
          <h4>备注</h4>
          <p>{{ selectedVisit.notes }}</p>
        </div>
        
        <el-divider />
        
        <div class="detail-section">
          <h4>处方信息</h4>
          <div v-if="selectedVisit.prescriptions && selectedVisit.prescriptions.length > 0">
            <el-table :data="selectedVisit.prescriptions" style="width: 100%">
              <el-table-column prop="code" label="处方编号" width="120" />
              <el-table-column prop="date" label="开具日期" width="120" />
              <el-table-column prop="type" label="处方类型" width="100">
                <template #default="scope">
                  <el-tag :type="scope.row.type === '西药' ? 'primary' : 'success'">
                    {{ scope.row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="scope">
                  <el-tag :type="getPrescriptionStatusType(scope.row.status)">
                    {{ scope.row.status }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="viewPrescription(scope.row)">
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无处方信息" />
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { Search, Refresh, Plus, Download, ArrowDown } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

// 状态和数据
const loading = ref(false);
const patients = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterGender = ref('');
const filterAgeGroup = ref('');
const filterStatus = ref('');
const searchQuery = ref('');

// 对话框
const dialogVisible = ref(false);
const visitDialogVisible = ref(false);
const visitDetailDialogVisible = ref(false);
const isEdit = ref(false);
const activeTab = ref('basic');
const patientFormRef = ref(null);
const visitFormRef = ref(null);
const selectedVisit = ref(null);

// 表单数据
const patientForm = reactive({
  id: '',
  patientId: '',
  name: '',
  gender: '男',
  idCard: '',
  birthDate: '',
  phone: '',
  maritalStatus: '',
  occupation: '',
  address: '',
  emergencyContact: '',
  emergencyPhone: '',
  insuranceType: '',
  allergies: '',
  medicalHistory: '',
  familyHistory: '',
  tags: [],
  notes: '',
  avatar: '',
  status: '正常'
});

// 就诊记录表单
const visitForm = reactive({
  patientId: '',
  patientName: '',
  date: new Date().toISOString().split('T')[0],
  type: '门诊',
  department: '',
  doctor: '',
  chiefComplaint: '',
  diagnosis: '',
  treatment: '',
  notes: ''
});

// 表单验证规则
const patientRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],
  birthDate: [
    { required: true, message: '请选择出生日期', trigger: 'change' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  maritalStatus: [
    { required: true, message: '请选择婚姻状况', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入联系地址', trigger: 'blur' }
  ],
  insuranceType: [
    { required: true, message: '请选择医保类型', trigger: 'change' }
  ],
  emergencyContact: [
    { required: true, message: '请输入紧急联系人', trigger: 'blur' }
  ],
  emergencyPhone: [
    { required: true, message: '请输入紧急联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
};

// 就诊记录表单验证规则
const visitRules = {
  date: [
    { required: true, message: '请选择就诊日期', trigger: 'change' }
  ],
  type: [
    { required: true, message: '请选择就诊类型', trigger: 'change' }
  ],
  department: [
    { required: true, message: '请选择科室', trigger: 'change' }
  ],
  doctor: [
    { required: true, message: '请选择医生', trigger: 'change' }
  ],
  chiefComplaint: [
    { required: true, message: '请输入主诉', trigger: 'blur' }
  ],
  diagnosis: [
    { required: true, message: '请输入诊断结果', trigger: 'blur' }
  ],
  treatment: [
    { required: true, message: '请输入治疗方案', trigger: 'blur' }
  ]
};

// 选项数据
const ageGroupOptions = [
  { value: '0-18', label: '0-18岁' },
  { value: '19-30', label: '19-30岁' },
  { value: '31-45', label: '31-45岁' },
  { value: '46-60', label: '46-60岁' },
  { value: '60+', label: '60岁以上' }
];

const statusOptions = [
  { value: '正常', label: '正常' },
  { value: '已禁用', label: '已禁用' },
  { value: '已注销', label: '已注销' }
];

const tagOptions = [
  '高血压', '糖尿病', '心脏病', '哮喘', '过敏体质', '孕妇', '儿童', '老年人', '慢性病', '术后康复'
];

const departmentOptions = [
  '内科', '外科', '妇产科', '儿科', '眼科', '耳鼻喉科', '口腔科', '皮肤科', '神经内科', '神经外科',
  '心血管内科', '呼吸内科', '消化内科', '泌尿外科', '骨科', '中医科', '康复科', '精神科', '肿瘤科', '急诊科'
];

const doctorOptions = [
  { value: '张医生', label: '张医生 - 主任医师' },
  { value: '李医生', label: '李医生 - 副主任医师' },
  { value: '王医生', label: '王医生 - 主治医师' },
  { value: '赵医生', label: '赵医生 - 住院医师' },
  { value: '刘医生', label: '刘医生 - 主任医师' },
  { value: '陈医生', label: '陈医生 - 副主任医师' },
  { value: '杨医生', label: '杨医生 - 主治医师' },
  { value: '黄医生', label: '黄医生 - 住院医师' }
];

// 计算属性
const filteredPatients = computed(() => {
  let result = [...patients.value];
  
  // 性别筛选
  if (filterGender.value) {
    result = result.filter(item => item.gender === filterGender.value);
  }
  
  // 年龄段筛选
  if (filterAgeGroup.value) {
    const [minAge, maxAge] = filterAgeGroup.value.split('-');
    if (maxAge) {
      result = result.filter(item => item.age >= parseInt(minAge) && item.age <= parseInt(maxAge));
    } else {
      // 处理 60+ 的情况
      const min = parseInt(minAge);
      result = result.filter(item => item.age >= min);
    }
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
      item.phone.includes(query) || 
      item.idCard.includes(query) ||
      item.patientId.toLowerCase().includes(query)
    );
  }
  
  return result;
});

const paginatedPatients = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredPatients.value.slice(start, end);
});

// 方法
function getStatusType(status) {
  switch (status) {
    case '正常': return 'success';
    case '已禁用': return 'warning';
    case '已注销': return 'info';
    default: return '';
  }
}

function getInsuranceType(type) {
  switch (type) {
    case '城镇职工医保': return 'primary';
    case '城乡居民医保': return 'success';
    case '新农合': return 'warning';
    case '商业保险': return 'danger';
    case '自费': return 'info';
    default: return '';
  }
}

function getPrescriptionStatusType(status) {
  switch (status) {
    case '已开具': return 'primary';
    case '已发药': return 'success';
    case '已完成': return 'info';
    case '已作废': return 'danger';
    default: return '';
  }
}

async function fetchPatients() {
  loading.value = true;
  try {
    // 模拟API调用
    // const response = await getPatients();
    // patients.value = response.data;
    
    // 模拟数据
    setTimeout(() => {
      const mockPatients = [];
      const statuses = statusOptions.map(item => item.value);
      const insuranceTypes = ['城镇职工医保', '城乡居民医保', '新农合', '商业保险', '自费'];
      const maritalStatuses = ['未婚', '已婚', '离异', '丧偶'];
      const occupations = ['教师', '工程师', '医生', '律师', '会计', '销售', '学生', '退休', '自由职业', '其他'];
      
      for (let i = 1; i <= 50; i++) {
        const gender = Math.random() > 0.5 ? '男' : '女';
        const birthYear = 1950 + Math.floor(Math.random() * 70);
        const birthMonth = Math.floor(Math.random() * 12) + 1;
        const birthDay = Math.floor(Math.random() * 28) + 1;
        const birthDate = `${birthYear}-${String(birthMonth).padStart(2, '0')}-${String(birthDay).padStart(2, '0')}`;
        const age = new Date().getFullYear() - birthYear;
        
        // 生成随机身份证号
        const idPrefix = `${Math.floor(Math.random() * 900) + 100}${birthYear}${String(birthMonth).padStart(2, '0')}${String(birthDay).padStart(2, '0')}`;
        const idSuffix = `${Math.floor(Math.random() * 10000).toString().padStart(4, '0')}`;
        const idCard = `${idPrefix}${idSuffix}`;
        
        // 生成随机注册日期（近3年内）
        const today = new Date();
        const registrationDate = new Date(today);
        registrationDate.setDate(today.getDate() - Math.floor(Math.random() * 1095)); // 最多3年前
        const regDateStr = registrationDate.toISOString().split('T')[0];
        
        // 生成随机最近就诊日期（近1年内）
        const lastVisitDate = new Date(today);
        lastVisitDate.setDate(today.getDate() - Math.floor(Math.random() * 365)); // 最多1年前
        const lastVisitStr = lastVisitDate.toISOString().split('T')[0];
        
        // 生成随机就诊记录
        const visitRecords = [];
        const visitCount = Math.floor(Math.random() * 8) + 1; // 1-8次就诊记录
        
        for (let j = 0; j < visitCount; j++) {
          const visitDate = new Date(lastVisitDate);
          visitDate.setDate(lastVisitDate.getDate() - Math.floor(Math.random() * 180) * (j + 1)); // 递增时间间隔
          const visitDateStr = visitDate.toISOString().split('T')[0];
          
          const departmentIndex = Math.floor(Math.random() * departmentOptions.length);
          const doctorIndex = Math.floor(Math.random() * doctorOptions.length);
          const visitType = ['门诊', '急诊', '住院', '复诊'][Math.floor(Math.random() * 4)];
          
          // 生成随机处方
          const prescriptions = [];
          const hasPrescription = Math.random() > 0.3; // 70%概率有处方
          
          if (hasPrescription) {
            const prescriptionCount = Math.floor(Math.random() * 2) + 1; // 1-2个处方
            for (let k = 0; k < prescriptionCount; k++) {
              const prescriptionType = Math.random() > 0.5 ? '西药' : '中药';
              const prescriptionStatus = ['已开具', '已发药', '已完成', '已作废'][Math.floor(Math.random() * 4)];
              
              prescriptions.push({
                id: `P${Math.floor(Math.random() * 10000)}`,
                code: `RX${String(Math.floor(Math.random() * 100000)).padStart(6, '0')}`,
                date: visitDateStr,
                type: prescriptionType,
                status: prescriptionStatus,
                doctor: doctorOptions[doctorIndex].value,
                department: departmentOptions[departmentIndex],
                items: []
              });
            }
          }
          
          visitRecords.push({
            id: `V${Math.floor(Math.random() * 10000)}`,
            date: visitDateStr,
            type: visitType,
            department: departmentOptions[departmentIndex],
            doctor: doctorOptions[doctorIndex].value,
            chiefComplaint: `患者${['感到', '出现', '反映'][Math.floor(Math.random() * 3)]}${['头痛', '发热', '咳嗽', '腹痛', '胸闷', '乏力', '恶心', '皮疹', '关节痛', '眩晕'][Math.floor(Math.random() * 10)]}${Math.random() > 0.5 ? '，伴有' + ['食欲不振', '呕吐', '腹泻', '失眠', '心悸', '气短'][Math.floor(Math.random() * 6)] : ''}。`,
            diagnosis: `${['急性', '慢性', '复发性', '轻度', '中度', '重度'][Math.floor(Math.random() * 6)]}${['上呼吸道感染', '胃炎', '结膜炎', '皮炎', '高血压', '糖尿病', '冠心病', '肺炎', '肠炎', '关节炎', '偏头痛', '焦虑症'][Math.floor(Math.random() * 12)]}。`,
            treatment: `建议${['口服药物治疗', '静脉输液', '肌肉注射', '局部用药', '物理治疗', '手术治疗', '饮食调理', '休息观察'][Math.floor(Math.random() * 8)]}，${['定期复查', '注意休息', '多饮水', '清淡饮食', '避免劳累', '遵医嘱服药'][Math.floor(Math.random() * 6)]}。`,
            notes: Math.random() > 0.7 ? `患者${['依从性好', '病情稳定', '需要进一步检查', '建议转诊', '需要长期随访'][Math.floor(Math.random() * 5)]}。` : '',
            prescriptions: prescriptions
          });
        }
        
        // 随机选择一些健康标签
        const tags = [];
        if (Math.random() > 0.7) { // 30%的患者有健康标签
          const tagCount = Math.floor(Math.random() * 3) + 1; // 1-3个标签
          const shuffledTags = [...tagOptions].sort(() => 0.5 - Math.random());
          for (let j = 0; j < tagCount; j++) {
            if (j < shuffledTags.length) {
              tags.push(shuffledTags[j]);
            }
          }
        }
        
        mockPatients.push({
          id: i,
          patientId: `P${String(i).padStart(6, '0')}`,
          name: `${gender === '男' ? ['张', '李', '王', '赵', '刘', '陈', '杨', '黄'][Math.floor(Math.random() * 8)] : ['王', '李', '张', '刘', '陈', '杨', '赵', '黄'][Math.floor(Math.random() * 8)]}${['伟', '芳', '娜', '强', '军', '杰', '敏', '静', '磊', '丽'][Math.floor(Math.random() * 10)]}${['', '强', '杰', '敏', '静', '磊', '丽'][Math.floor(Math.random() * 7)]}`,
          gender: gender,
          age: age,
          idCard: idCard,
          birthDate: birthDate,
          phone: `1${Math.floor(Math.random() * 9) + 1}${Math.floor(Math.random() * 10000000000).toString().padStart(9, '0')}`,
          maritalStatus: maritalStatuses[Math.floor(Math.random() * maritalStatuses.length)],
          occupation: occupations[Math.floor(Math.random() * occupations.length)],
          address: `${['北京市', '上海市', '广州市', '深圳市', '成都市', '杭州市', '南京市', '武汉市'][Math.floor(Math.random() * 8)]}${['朝阳区', '海淀区', '浦东新区', '天河区', '福田区', '武侯区', '西湖区', '鼓楼区', '江汉区'][Math.floor(Math.random() * 9)]}${['中关村', '望京', '陆家嘴', '珠江新城', '车公庙', '锦江', '西溪', '新街口', '汉口'][Math.floor(Math.random() * 9)]}${Math.floor(Math.random() * 100) + 1}号`,
          emergencyContact: `${['张', '李', '王', '赵', '刘', '陈', '杨', '黄'][Math.floor(Math.random() * 8)]}${['伟', '芳', '娜', '强', '军', '杰', '敏', '静', '磊', '丽'][Math.floor(Math.random() * 10)]}`,
          emergencyPhone: `1${Math.floor(Math.random() * 9) + 1}${Math.floor(Math.random() * 10000000000).toString().padStart(9, '0')}`,
          insuranceType: insuranceTypes[Math.floor(Math.random() * insuranceTypes.length)],
          allergies: Math.random() > 0.8 ? `对${['青霉素', '磺胺类药物', '阿司匹林', '海鲜', '花粉', '尘螨', '乳制品'][Math.floor(Math.random() * 7)]}过敏` : '',
          medicalHistory: Math.random() > 0.7 ? `${Math.floor(Math.random() * 10) + 1}年前曾患${['高血压', '糖尿病', '冠心病', '哮喘', '胃炎', '肝炎', '肾炎', '关节炎'][Math.floor(Math.random() * 8)]}` : '',
          familyHistory: Math.random() > 0.8 ? `家族中有${['高血压', '糖尿病', '冠心病', '癌症', '哮喘', '精神疾病'][Math.floor(Math.random() * 6)]}病史` : '',
          tags: tags,
          notes: Math.random() > 0.8 ? `患者${['性格温和', '配合治疗', '经济条件一般', '对治疗有顾虑', '需要心理疏导'][Math.floor(Math.random() * 5)]}` : '',
          avatar: Math.random() > 0.3 ? `https://randomuser.me/api/portraits/${gender === '男' ? 'men' : 'women'}/${Math.floor(Math.random() * 100)}.jpg` : '',
          registrationDate: regDateStr,
          lastVisit: lastVisitStr,
          visitCount: visitCount,
          status: statuses[Math.floor(Math.random() * (statuses.length - 1))], // 排除已注销状态
          visitRecords: visitRecords
        });
      }
      
      patients.value = mockPatients;
      loading.value = false;
    }, 800);
  } catch (error) {
    console.error('获取患者列表失败:', error);
    ElMessage.error('获取患者列表失败');
    loading.value = false;
  }
}

function refreshPatients() {
  fetchPatients();
  ElMessage.success('患者列表已刷新');
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

function handleCommand(command, row) {
  switch (command) {
    case 'view':
      // 查看详情，可以实现为打开抽屉或对话框
      ElMessage({ type: 'info', message: `查看患者详情: ${row.name}` });
      break;
    case 'records':
      // 查看就诊记录
      ElMessage({ type: 'info', message: `查看就诊记录: ${row.name}` });
      break;
    case 'prescriptions':
      // 查看处方记录
      ElMessage({ type: 'info', message: `查看处方记录: ${row.name}` });
      break;
    case 'status':
      // 更改状态
      changeStatus(row);
      break;
    case 'delete':
      // 删除患者
      deletePatient(row);
      break;
  }
}

function addPatient() {
  resetForm();
  isEdit.value = false;
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function editPatient(patient) {
  resetForm();
  isEdit.value = true;
  
  // 复制患者信息到表单
  Object.keys(patientForm).forEach(key => {
    if (key in patient) {
      patientForm[key] = patient[key];
    }
  });
  
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function addVisitRecord(patient) {
  // 设置就诊记录表单
  visitForm.patientId = patient.id;
  visitForm.patientName = patient.name;
  visitForm.date = new Date().toISOString().split('T')[0];
  visitForm.type = '门诊';
  visitForm.department = '';
  visitForm.doctor = '';
  visitForm.chiefComplaint = '';
  visitForm.diagnosis = '';
  visitForm.treatment = '';
  visitForm.notes = '';
  
  visitDialogVisible.value = true;
}

function viewVisitDetail(visit) {
  selectedVisit.value = visit;
  visitDetailDialogVisible.value = true;
}

function viewPrescription(prescription) {
  // 查看处方详情
  ElMessage({ type: 'info', message: `查看处方详情: ${prescription.code}` });
}

function changeStatus(patient) {
  const newStatus = patient.status === '正常' ? '已禁用' : '正常';
  const statusText = newStatus === '正常' ? '启用' : '禁用';
  
  ElMessageBox.confirm(
    `确认${statusText}患者 ${patient.name} 的账号吗？`,
    `${statusText}确认`,
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: newStatus === '已禁用' ? 'warning' : 'info'
    }
  ).then(() => {
    // 模拟API调用
    // await updatePatientStatus(patient.id, newStatus);
    
    // 模拟成功响应
    setTimeout(() => {
      // 更新本地数据
      const index = patients.value.findIndex(item => item.id === patient.id);
      if (index !== -1) {
        patients.value[index].status = newStatus;
      }
      
      ElMessage.success(`已${statusText}患者 ${patient.name} 的账号`);
    }, 500);
  }).catch(() => {});
}

function deletePatient(patient) {
  ElMessageBox.confirm(
    `确认删除患者 ${patient.name} 吗？此操作不可逆。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 模拟API调用
    // await deletePatient(patient.id);
    
    // 模拟成功响应
    setTimeout(() => {
      // 从列表中移除
      patients.value = patients.value.filter(item => item.id !== patient.id);
      
      ElMessage.success('患者删除成功');
    }, 500);
  }).catch(() => {});
}

function handleAvatarChange(file) {
  // 实际项目中应该上传到服务器
  // 这里模拟上传成功
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = () => {
    patientForm.avatar = reader.result;
  };
}

function handleIdCardBlur() {
  // 从身份证号提取出生日期
  if (patientForm.idCard && patientForm.idCard.length === 18) {
    const year = patientForm.idCard.substring(6, 10);
    const month = patientForm.idCard.substring(10, 12);
    const day = patientForm.idCard.substring(12, 14);
    patientForm.birthDate = `${year}-${month}-${day}`;
  }
}

function resetForm() {
  // 重置表单数据
  Object.assign(patientForm, {
    id: '',
    patientId: '',
    name: '',
    gender: '男',
    idCard: '',
    birthDate: '',
    phone: '',
    maritalStatus: '',
    occupation: '',
    address: '',
    emergencyContact: '',
    emergencyPhone: '',
    insuranceType: '',
    allergies: '',
    medicalHistory: '',
    familyHistory: '',
    tags: [],
    notes: '',
    avatar: '',
    status: '正常'
  });
  
  // 如果表单引用存在，重置验证
  if (patientFormRef.value) {
    patientFormRef.value.resetFields();
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
  patientFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // 如果是编辑模式，调用更新API
        if (isEdit.value) {
          // await updatePatient(patientForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 更新本地数据
            const index = patients.value.findIndex(item => item.id === patientForm.id);
            if (index !== -1) {
              // 复制表单数据到患者对象
              const updatedPatient = { ...patients.value[index] };
              Object.keys(patientForm).forEach(key => {
                updatedPatient[key] = patientForm[key];
              });
              
              // 计算年龄
              if (patientForm.birthDate) {
                const birthYear = new Date(patientForm.birthDate).getFullYear();
                updatedPatient.age = new Date().getFullYear() - birthYear;
              }
              
              patients.value[index] = updatedPatient;
            }
            
            ElMessage.success('患者信息更新成功');
            dialogVisible.value = false;
            loading.value = false;
          }, 1000);
        } else {
          // 如果是添加模式，调用创建API
          // await createPatient(patientForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 创建新患者对象并添加到列表
            const newId = Math.max(...patients.value.map(item => item.id)) + 1;
            const patientId = `P${String(newId).padStart(6, '0')}`;
            
            // 计算年龄
            let age = 0;
            if (patientForm.birthDate) {
              const birthYear = new Date(patientForm.birthDate).getFullYear();
              age = new Date().getFullYear() - birthYear;
            }
            
            const newPatient = {
              ...patientForm,
              id: newId,
              patientId: patientId,
              age: age,
              registrationDate: new Date().toISOString().split('T')[0],
              lastVisit: '',
              visitCount: 0,
              visitRecords: [],
              status: '正常'
            };
            
            patients.value.unshift(newPatient);
            
            ElMessage.success('患者添加成功');
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

function submitVisitForm() {
  visitFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // await createVisitRecord(visitForm);
        
        // 模拟成功响应
        setTimeout(() => {
          // 更新本地数据
          const patientIndex = patients.value.findIndex(item => item.id === visitForm.patientId);
          if (patientIndex !== -1) {
            // 创建新就诊记录
            const newVisit = {
              id: `V${Math.floor(Math.random() * 10000)}`,
              date: visitForm.date,
              type: visitForm.type,
              department: visitForm.department,
              doctor: visitForm.doctor,
              chiefComplaint: visitForm.chiefComplaint,
              diagnosis: visitForm.diagnosis,
              treatment: visitForm.treatment,
              notes: visitForm.notes,
              prescriptions: []
            };
            
            // 添加到患者的就诊记录中
            patients.value[patientIndex].visitRecords.unshift(newVisit);
            
            // 更新患者的最近就诊日期和就诊次数
            patients.value[patientIndex].lastVisit = visitForm.date;
            patients.value[patientIndex].visitCount += 1;
          }
          
          ElMessage.success('就诊记录添加成功');
          visitDialogVisible.value = false;
          loading.value = false;
        }, 1000);
      } catch (error) {
        console.error('提交就诊记录失败:', error);
        ElMessage.error('操作失败，请重试');
        loading.value = false;
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
      return false;
    }
  });
}

function exportPatients() {
  // 模拟导出过程
  loading.value = true;
  setTimeout(() => {
    ElMessage.success('患者数据导出成功');
    loading.value = false;
  }, 1000);
}

// 生命周期钩子
onMounted(() => {
  fetchPatients();
});
</script>

<style scoped>
.patients-container {
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
  flex-wrap: wrap;
  gap: 12px;
}

.patients-card {
  margin-bottom: 20px;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.patient-avatar {
  width: 50px;
  height: 50px;
}

.patient-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.patient-name {
  font-weight: 500;
}

.patient-id {
  font-size: 12px;
  color: #606266;
}

.patient-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
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
.patient-expand {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.expand-section {
  width: 100%;
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
  width: calc(25% - 8px);
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

/* 对话框样式 */
.patient-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
}

.avatar-uploader {
  width: 100px;
  height: 100px;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
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

.avatar-image {
  width: 100px;
  height: 100px;
  display: block;
  object-fit: cover;
  border-radius: 50%;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 就诊详情对话框样式 */
.visit-detail {
  padding: 0 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.detail-info {
  display: flex;
  gap: 20px;
  color: #606266;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.detail-section p {
  margin: 0;
  line-height: 1.6;
  color: #606266;
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