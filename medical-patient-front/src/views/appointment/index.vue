<template>
  <div class="appointment-container">
    <el-card class="appointment-card">
      <template #header>
        <div class="card-header">
          <h2>预约挂号</h2>
          <el-button type="primary" @click="goToAppointmentList">我的预约</el-button>
          <el-button type="info" plain @click="triageDialogVisible = true">AI 推荐科室</el-button>
        </div>
      </template>
      
      <!-- 提示信息 -->
      <el-alert
        title="预约说明"
        type="info"
        description="预约成功后，请在支付记录页面完成支付操作。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
      
      <el-steps :active="currentStep" finish-status="success" simple class="appointment-steps">
        <el-step title="选择科室" />
        <el-step title="选择日期和医生" />
        <el-step title="选择就诊人" />
        <el-step title="确认预约" />
      </el-steps>
      
      <!-- 步骤1：选择科室 -->
      <div v-if="currentStep === 0" class="step-content">
        <el-form :model="appointmentForm" label-width="100px">
          <el-form-item label="主科室">
            <el-select 
              v-model="appointmentForm.departmentId" 
              placeholder="请选择主科室"
              @change="handleDepartmentChange"
              style="width: 100%"
            >
              <el-option 
                v-for="dept in departments" 
                :key="dept.id" 
                :label="dept.departmentName" 
                :value="dept.id" 
              />
            </el-select>
          </el-form-item>
          
          <el-form-item label="子科室">
            <el-select 
              v-model="appointmentForm.subDepartmentId" 
              placeholder="请选择子科室"
              style="width: 100%"
              :disabled="!appointmentForm.departmentId"
            >
              <el-option 
                v-for="subDept in subDepartments" 
                :key="subDept.id" 
                :label="subDept.departmentName" 
                :value="subDept.id" 
              >
                <div class="sub-dept-option">
                  <div class="sub-dept-image" v-if="subDept.imagePath">
                    <img :src="subDept.imagePath" :alt="subDept.departmentName" />
                  </div>
                  <div class="sub-dept-icon" v-else>
                    <el-icon><OfficeBuilding /></el-icon>
                  </div>
                  <span class="sub-dept-name">{{ subDept.departmentName }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <div class="step-buttons">
              <el-button type="primary" @click="nextStep" :disabled="!appointmentForm.departmentId || !appointmentForm.subDepartmentId">下一步</el-button>
            </div>
          </el-form-item>
        </el-form>
        
        <!-- 科室介绍 -->
        <div v-if="selectedDepartment" class="department-info">
          <h3>{{ selectedDepartment.name }}</h3>
          
          <div v-if="selectedSubDepartment" class="sub-department-info">
            <h4>{{ selectedSubDepartment.name }}</h4>
          </div>
        </div>
      </div>
      
      <!-- 步骤2：选择日期和医生 -->
      <div v-if="currentStep === 1" class="step-content">
        <!-- 日期选择 -->
        <div class="date-selection">
          <h3>选择就诊日期</h3>
          <div class="date-list">
            <div 
              v-for="date in availableDates" 
              :key="date.date" 
              class="date-item" 
              :class="{ 'date-selected': appointmentForm.appointmentDate === date.date, 'date-disabled': date.isWeekend }"
              @click="!date.isWeekend && selectDate(date.date)"
              style="cursor: pointer;"
            >
              <div class="date-day">{{ date.day }}</div>
              <div class="date-value">{{ formatDateDisplay(date.date) }}</div>
              <div v-if="date.isWeekend" class="date-status">无排班</div>
            </div>
          </div>
        </div>
        
        <!-- 医生选择 -->
        <div class="doctor-selection" v-if="appointmentForm.appointmentDate">
          <h3>选择医生和时段</h3>
          <el-empty v-if="schedules.length === 0" description="当前日期没有可用排班"></el-empty>
          <div v-else>
            <!-- 上午排班 -->
            <div v-if="morningSchedules.length > 0" class="time-period-section">
              <h4 class="time-period-title">上午</h4>
              <el-row :gutter="20">
                <el-col :span="8" v-for="schedule in morningSchedules" :key="schedule.id">
                  <div 
                    class="doctor-card" 
                    :class="{ 
                      'doctor-selected': appointmentForm.scheduleId === schedule.id,
                      'doctor-disabled': schedule.status === 0 || schedule.currentAppointmentCount >= schedule.appointmentLimit
                    }"
                    @click="schedule.status === 1 && schedule.currentAppointmentCount < schedule.appointmentLimit && selectSchedule(schedule)"
                    style="cursor: pointer;"
                  >
                    <div class="doctor-info">
                      <div class="doctor-details">
                        <h4>{{ schedule.doctorName }}</h4>
                        <p>上午 · ¥{{ schedule.price || '0.00' }}</p>
                      </div>
                    </div>
                    <div class="schedule-info">
                      <div class="schedule-remaining">
                        <span v-if="schedule.status === 0" class="status-unavailable">停诊</span>
                        <span v-else-if="schedule.currentAppointmentCount >= schedule.appointmentLimit" class="status-full">已满</span>
                        <span v-else>
                          剩余号源: <span :class="{ 'few-remaining': (schedule.appointmentLimit - schedule.currentAppointmentCount) < 5 }">
                            {{ schedule.appointmentLimit - schedule.currentAppointmentCount }}
                          </span>
                        </span>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
            
            <!-- 下午排班 -->
            <div v-if="afternoonSchedules.length > 0" class="time-period-section">
              <h4 class="time-period-title">下午</h4>
              <el-row :gutter="20">
                <el-col :span="8" v-for="schedule in afternoonSchedules" :key="schedule.id">
                  <div 
                    class="doctor-card" 
                    :class="{ 
                      'doctor-selected': appointmentForm.scheduleId === schedule.id,
                      'doctor-disabled': schedule.status === 0 || schedule.currentAppointmentCount >= schedule.appointmentLimit
                    }"
                    @click="schedule.status === 1 && schedule.currentAppointmentCount < schedule.appointmentLimit && selectSchedule(schedule)"
                    style="cursor: pointer;"
                  >
                    <div class="doctor-info">
                      <div class="doctor-details">
                        <h4>{{ schedule.doctorName }}</h4>
                        <p>下午 · ¥{{ schedule.price || '0.00' }}</p>
                      </div>
                    </div>
                    <div class="schedule-info">
                      <div class="schedule-remaining">
                        <span v-if="schedule.status === 0" class="status-unavailable">停诊</span>
                        <span v-else-if="schedule.currentAppointmentCount >= schedule.appointmentLimit" class="status-full">已满</span>
                        <span v-else>
                          剩余号源: <span :class="{ 'few-remaining': (schedule.appointmentLimit - schedule.currentAppointmentCount) < 5 }">
                            {{ schedule.appointmentLimit - schedule.currentAppointmentCount }}
                          </span>
                        </span>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>
          </div>
        </div>
        
        <div class="step-buttons">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!appointmentForm.scheduleId">下一步</el-button>
        </div>
      </div>
      
      <!-- 步骤3：选择就诊人 -->
      <div v-if="currentStep === 2" class="step-content">
        <h3>选择就诊人</h3>
        
        <!-- 如果没有就诊人或所有就诊人都未填写身份证，显示实名认证提示 -->
        <div v-if="!hasValidPatients" class="no-patients-tip">
          <el-empty description="暂无可用就诊人">
            <template #image>
              <el-icon :size="60" color="var(--primary-600)"><User /></el-icon>
            </template>
            <template #description>
              <p>您还没有完成实名认证的就诊人</p>
              <p>完成实名认证后才能进行预约挂号</p>
            </template>
            <el-button type="primary" @click="goToVerification">
              <el-icon><Checked /></el-icon>
              去实名认证
            </el-button>
          </el-empty>
        </div>
        
        <!-- 有可用就诊人时显示选择界面 -->
        <div v-else class="patient-selection">
          <el-radio-group v-model="appointmentForm.patientId">
            <el-row :gutter="20">
              <el-col :span="8" v-for="patient in validPatients" :key="patient.id">
                <el-radio :label="patient.id" class="patient-card" style="width: 100%; height: 100%; display: block; padding: 15px; cursor: pointer;">
                  <div class="patient-info">
                    <h4>{{ patient.realName || patient.name }}</h4>
                    <p>{{ patient.idCard ? patient.idCard.replace(/^(.{6})(?:\d+)(.{4})$/, '$1****$2') : '未填写身份证' }}</p>
                  </div>
                </el-radio>
              </el-col>
              
              <!-- 添加就诊人按钮 -->
              <el-col :span="8">
                <div class="add-patient-card" @click="goToAddPatient" style="cursor: pointer;">
                  <el-icon :size="30"><Plus /></el-icon>
                  <p>添加就诊人</p>
                </div>
              </el-col>
            </el-row>
          </el-radio-group>
        </div>
        
        <div class="step-buttons">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="nextStep" :disabled="!appointmentForm.patientId">下一步</el-button>
        </div>
      </div>
      
      <!-- 步骤4：确认预约 -->
      <div v-if="currentStep === 3" class="step-content">
        <h3>确认预约信息</h3>
        
        <div class="appointment-summary">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="科室">
              {{ selectedDepartment?.departmentName }} - {{ selectedSubDepartment?.departmentName }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊日期">
              {{ appointmentForm.appointmentDate }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊医生">
              {{ selectedSchedule?.doctorName }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊时段">
              {{ getScheduleTimeSlot(selectedSchedule) }}
            </el-descriptions-item>
            <el-descriptions-item label="就诊人">
              {{ selectedPatient?.realName || selectedPatient?.name }}
            </el-descriptions-item>
            <el-descriptions-item label="挂号费用">
              <span class="fee">¥{{ selectedSchedule?.price || '0.00' }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div class="step-buttons">
          <el-button @click="prevStep">上一步</el-button>
          <el-button type="primary" @click="submitAppointment" :loading="submitting">确认预约</el-button>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="triageDialogVisible"
      title="AI 科室推荐（仅供参考）"
      width="520px"
    >
      <el-form :model="triageForm" label-width="90px">
        <el-form-item label="症状描述">
          <el-input
            v-model="triageForm.description"
            type="textarea"
            :rows="4"
            placeholder="请描述主要症状，例如：持续咳嗽三天伴低热、轻度胸闷"
          />
        </el-form-item>
      </el-form>
      <el-alert
        title="AI 推荐仅供参考，如症状严重请直接就医。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />
      <div v-if="triageResult.recommendedDepartments.length" class="triage-result">
        <p>推荐科室：<strong>{{ triageResult.recommendedDepartments.join('，') }}</strong></p>
        <small>{{ triageResult.rationale }}</small>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="triageDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="triageLoading" @click="fetchTriageSuggestion">获取推荐</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      v-model="aiUnavailableDialogVisible"
      title="AI 服务不可用：模型 & 价格"
      width="720px"
    >
      <pre class="ai-unavailable-pre">{{ aiUnavailableText }}</pre>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="aiUnavailableDialogVisible = false">我知道了</el-button>
        </span>
      </template>
    </el-dialog>
    

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, User, Checked, OfficeBuilding } from '@element-plus/icons-vue'
import { getDepartmentList, getSubDepartmentList, getScheduleList, createAppointment } from '../../api/appointment'
import { getCaseList } from '../../api/user'
import { getTriageSuggestion } from '../../api/ai'
import { getFutureDates, formatDate } from '../../utils'

const router = useRouter()
const route = useRoute()

// 当前步骤
const currentStep = ref(0)

// 预约表单
const appointmentForm = reactive({
  departmentId: '',
  departmentName:'',
  subDepartmentId: '',
  subDepartmentName:'',
  appointmentDate: '',
  scheduleId: '',
  patientId: ''
})

// 科室数据
const departments = ref([])
const subDepartments = ref([])

// 日期数据
const availableDates = ref([])

// 排班数据
const schedules = ref([])

// 就诊人数据
const patients = ref([])

// 加载状态
const submitting = ref(false)
const triageLoading = ref(false)
const triageDialogVisible = ref(false)
const aiUnavailableDialogVisible = ref(false)
const aiUnavailableText = ref('')

const AI_UNAVAILABLE_FALLBACK = `AI 服务不可用：模型 & 价格

模型 & 价格
下表所列模型价格以“百万 tokens”为单位。Token 是模型用来表示自然语言文本的的最小单位，可以是一个词、一个数字或一个标点符号等。我们将根据模型输入和输出的总 token 数进行计量计费。

模型细节
模型\tdeepseek-chat\tdeepseek-reasoner\tdeepseek-reasoner(1)
BASE URL\thttps://api.deepseek.com\thttps://api.deepseek.com/\tv3.2_speciale_expires_on_20251215
模型版本\tDeepSeek-V3.2（非思考模式）\tDeepSeek-V3.2（思考模式）\tDeepSeek-V3.2-Speciale（只支持思考模式）
上下文长度\t128K
输出长度\t默认 4K，最大 8K\t默认 32K，最大 64K\t默认 128K，最大 128K
功能\tJson Output\t支持\t支持\t不支持
\tTool Calls\t支持\t支持\t不支持
\t对话前缀续写（Beta）\t支持\t支持\t不支持
\tFIM 补全（Beta）\t支持\t不支持\t不支持
价格\t百万tokens输入（缓存命中）\t0.2元
\t百万tokens输入（缓存未命中）\t2元
\t百万tokens输出\t3元

(1) 用户可以通过设置 base_url="https://api.deepseek.com/v3.2_speciale_expires_on_20251215" 访问 DeepSeek-V3.2-Speciale 模型。该模型只支持思考模式，支持时间截止至北京时间 2025-12-15 23:59。

扣费规则
扣减费用 = token 消耗量 × 模型单价，对应的费用将直接从充值余额或赠送余额中进行扣减。当充值余额与赠送余额同时存在时，优先扣减赠送余额。

产品价格可能发生变动，DeepSeek 保留修改价格的权利。请您依据实际用量按需充值，定期查看此页面以获知最新价格信息。`
const triageForm = reactive({
  description: ''
})
const triageResult = reactive({
  recommendedDepartments: [],
  rationale: ''
})

// 计算属性：上午排班
const morningSchedules = computed(() => {
  return schedules.value.filter(schedule => schedule.isMorning === 1)
})

// 计算属性：下午排班
const afternoonSchedules = computed(() => {
  return schedules.value.filter(schedule => schedule.isAfternoon === 1)
})

// 计算属性：选中的科室
const selectedDepartment = computed(() => {
  return departments.value.find(dept => dept.id === appointmentForm.departmentId) || null
})

// 计算属性：选中的子科室
const selectedSubDepartment = computed(() => {
  return subDepartments.value.find(subDept => subDept.id === appointmentForm.subDepartmentId) || null
})

// 计算属性：选中的排班
const selectedSchedule = computed(() => {
  return schedules.value.find(schedule => schedule.id === appointmentForm.scheduleId) || null
})

// 计算属性：选中的就诊人
const selectedPatient = computed(() => {
  return validPatients.value.find(patient => patient.id === appointmentForm.patientId) || null
})

// 计算属性：有效的就诊人（已填写身份证和真实姓名的）
const validPatients = computed(() => {
  return patients.value.filter(patient => 
    patient.idCard && patient.idCard.trim() !== '' &&
    patient.realName && patient.realName.trim() !== ''
  )
})

// 计算属性：是否有有效的就诊人
const hasValidPatients = computed(() => {
  return validPatients.value.length > 0
})

// 获取排班时段显示文本
const getScheduleTimeSlot = (schedule) => {
  if (!schedule) return ''
  
  const timeSlots = []
  if (schedule.isMorning === 1) timeSlots.push('上午')
  if (schedule.isAfternoon === 1) timeSlots.push('下午')
  
  return timeSlots.join('、') || '未知时段'
}

// 格式化日期显示
const formatDateDisplay = (dateStr) => {
  const date = new Date(dateStr)
  return `${date.getMonth() + 1}月${date.getDate()}日`
}

// AI 推荐科室
const fetchTriageSuggestion = async () => {
  if (!triageForm.description || !triageForm.description.trim()) {
    ElMessage.warning('请先填写症状描述')
    return
  }
  triageLoading.value = true
  try {
    triageResult.recommendedDepartments = []
    triageResult.rationale = ''
    const resp = await getTriageSuggestion({
      description: triageForm.description
    })
    if (resp.code === 200 && resp.data) {
      triageResult.recommendedDepartments = resp.data.recommendedDepartments || []
      triageResult.rationale = resp.data.rationale || ''
    } else {
      ElMessage.warning(resp.message || '获取推荐失败')
    }
  } catch (error) {
    console.error('AI 推荐失败', error)
    if (error && error.code === 9001) {
      aiUnavailableText.value = error.message || AI_UNAVAILABLE_FALLBACK
      aiUnavailableDialogVisible.value = true
      return
    }
    ElMessage.error(error.message || 'AI 推荐失败')
  } finally {
    triageLoading.value = false
  }
}

// 下一步
const nextStep = () => {
  if (currentStep.value === 0) {
    if (!appointmentForm.departmentId || !appointmentForm.subDepartmentId) {
      ElMessage.warning('请选择科室和子科室')
      return
    }
  } else if (currentStep.value === 1) {
    if (!appointmentForm.appointmentDate || !appointmentForm.scheduleId) {
      ElMessage.warning('请选择就诊日期和医生')
      return
    }
  } else if (currentStep.value === 2) {
    if (!appointmentForm.patientId) {
      ElMessage.warning('请选择就诊人')
      return
    }
    // 验证选中的就诊人是否有身份证和真实姓名
    const selectedPatientInfo = validPatients.value.find(p => p.id === appointmentForm.patientId)
    if (!selectedPatientInfo || !selectedPatientInfo.idCard || !selectedPatientInfo.realName) {
      ElMessage.warning('所选就诊人信息不完整，请先完成实名认证')
      return
    }
  }
  
  currentStep.value++
}

// 上一步
const prevStep = () => {
  currentStep.value--
}

// 科室变更处理
const handleDepartmentChange = async () => {
  appointmentForm.subDepartmentId = ''
  subDepartments.value = []
  
  if (!appointmentForm.departmentId) return
  
  try {
    // 调用获取子科室列表的接口
    const res = await getSubDepartmentList(appointmentForm.departmentId)
    if (res.code === 200) {
      subDepartments.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取子科室列表失败')
    }
  } catch (error) {
    console.error('获取子科室列表失败:', error)
    ElMessage.error('获取子科室列表失败')
  }
}

// 选择日期
const selectDate = async (date) => {
  appointmentForm.appointmentDate = date
  appointmentForm.scheduleId = ''
  schedules.value = []
  
  try {
    // 调用获取排班列表的接口
    const res = await getScheduleList({
      subDepartmentId: appointmentForm.subDepartmentId,
      scheduleDate: appointmentForm.appointmentDate
    })
    
    if (res.code === 200) {
      schedules.value = res.data || []
      console.log('获取到排班信息:', schedules.value)
      
      // 检查是否有上次保存的排班ID，如果有则自动选择
      const lastAppointmentInfoStr = localStorage.getItem('lastAppointmentInfo')
      if (lastAppointmentInfoStr) {
        try {
          const lastAppointmentInfo = JSON.parse(lastAppointmentInfoStr)
          // 如果数据不超过30分钟，则使用保存的排班ID
          const now = new Date().getTime()
          const savedTime = lastAppointmentInfo.updatedAt || 0
          if (now - savedTime < 30 * 60 * 1000) { // 30分钟内的数据有效
            console.log('使用保存的排班信息')
          }
        } catch (e) {
          console.error('解析保存的排班信息失败:', e)
        }
      }
    } else {
      ElMessage.error(res.message || '获取排班信息失败')
    }
  } catch (error) {
    console.error('获取排班信息失败:', error)
    ElMessage.error('获取排班信息失败，请稍后重试')
  }
}

// 选择排班
const selectSchedule = (schedule) => {
  appointmentForm.scheduleId = schedule.id
  console.log('选择排班:', schedule, '当前步骤:', currentStep.value)
  
  // 更新UI显示，确保选中状态正确
  schedules.value = schedules.value.map(item => {
    if (item.id === schedule.id) {
      return { ...item, selected: true }
    }
    return { ...item, selected: false }
  })
  
  // 选择排班后自动跳转到下一步（选择就诊人）
  nextStep()
}

// 跳转到添加就诊人页面
const goToAddPatient = () => {
  router.push({ path: '/user', query: { tab: 'patients', action: 'addPatient' } })
}

// 跳转到实名认证页面
const goToVerification = () => {
  router.push('/user/verification')
}

// 跳转到我的预约列表
const goToAppointmentList = () => {
  router.push('/appointment/list')
}

// 提交预约
const submitAppointment = async () => {
  submitting.value = true
  
  try {
    // 调用创建预约订单的接口
    const res = await createAppointment({
      doctorId: selectedSchedule.value?.doctorId,
      patientId: appointmentForm.patientId,
      scheduleId: appointmentForm.scheduleId
    })
    
    if (res.code === 200) {
      // 更新当前选中排班的剩余号源数量
      if (selectedSchedule.value) {
        // 增加已预约数量
        selectedSchedule.value.currentAppointmentCount += 1
        console.log('更新后的排班信息:', selectedSchedule.value)
      }
      
      // 保存当前选择的日期和科室信息，用于在返回时恢复
      localStorage.setItem('lastAppointmentInfo', JSON.stringify({
        departmentId: appointmentForm.departmentId,
        subDepartmentId: appointmentForm.subDepartmentId,
        appointmentDate: appointmentForm.appointmentDate,
        updatedAt: new Date().getTime() // 添加时间戳，用于判断数据是否过期
      }))
      
      ElMessage.success('预约创建成功，请在支付记录中完成支付')
      
      // 清空所有选择状态
      appointmentForm.departmentId = ''
      appointmentForm.subDepartmentId = ''
      appointmentForm.appointmentDate = ''
      appointmentForm.scheduleId = ''
      appointmentForm.patientId = ''
      currentStep.value = 0
      schedules.value = []
      subDepartments.value = []
      
      router.push('/payment/appointment')
    } else {
      ElMessage.error(res.message || '预约失败')
    }
  } catch (error) {
    console.error('创建预约失败:', error)
    ElMessage.error('预约失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}



// 初始化数据
onMounted(async () => {
  // 获取可用日期（当天及未来3天，排除周末）
  availableDates.value = getFutureDates(4)
  
  try {
    // 获取科室列表
    const deptRes = await getDepartmentList()
    if (deptRes.code === 200) {
      departments.value = deptRes.data || []
    }
    
    // 获取就诊人列表
    const patientRes = await getCaseList()
    if (patientRes.code === 200) {
      patients.value = patientRes.data || []
    }
    
    // 检查是否有上次保存的预约信息
    const lastAppointmentInfoStr = localStorage.getItem('lastAppointmentInfo')
    if (lastAppointmentInfoStr) {
      try {
        const lastAppointmentInfo = JSON.parse(lastAppointmentInfoStr)
        
        // 恢复上次选择的科室
        if (lastAppointmentInfo.departmentId) {
          appointmentForm.departmentId = lastAppointmentInfo.departmentId
          await handleDepartmentChange()
          
          // 恢复上次选择的子科室
          if (lastAppointmentInfo.subDepartmentId) {
            appointmentForm.subDepartmentId = lastAppointmentInfo.subDepartmentId
            
            // 恢复上次选择的日期并刷新排班信息
            if (lastAppointmentInfo.appointmentDate) {
              // 自动跳转到第二步（选择日期和医生）
              currentStep.value = 1
              
              // 选择日期并刷新排班信息
              await selectDate(lastAppointmentInfo.appointmentDate)
              
              // 显示提示信息
              ElMessage({
                message: '已恢复上次的预约信息，剩余号数已更新',
                type: 'success',
                duration: 5000
              })
              
              // 清除本地存储的预约信息，避免下次再自动恢复
              localStorage.removeItem('lastAppointmentInfo')
            }
          }
        }
      } catch (e) {
        console.error('恢复上次预约信息失败:', e)
        localStorage.removeItem('lastAppointmentInfo')
      }
    }
    
    // 如果URL中有科室和子科室参数，自动选择（URL参数优先级高于本地存储）
    if (route.query.deptId) {
      appointmentForm.departmentId = parseInt(route.query.deptId)
      await handleDepartmentChange()
      
      if (route.query.subDeptId) {
        appointmentForm.subDepartmentId = parseInt(route.query.subDeptId)
        // 如果有子科室参数，自动跳转到第二步
        currentStep.value = 1
      }
    }
  } catch (error) {
    console.error('初始化数据失败:', error)
    ElMessage.error('加载数据失败，请刷新页面重试')
  }
})
</script>

<style scoped>
.appointment-container {
  min-height: 70vh;
  background: transparent;
  padding: 8px;
  position: relative;
  overflow-x: hidden;
}

/* 日期选择样式 */
.date-selection {
  margin-bottom: 30px;
}

.date-list {
  display: flex;
  gap: 15px;
  overflow-x: auto;
  padding: 10px 0;
  margin-bottom: 20px;
}

.date-item {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-50) 100%);
  border: 2px solid rgb(var(--primary-500-rgb) / 0.18);
  border-radius: 12px;
  padding: 15px 20px;
  min-width: 100px;
  text-align: center;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.date-item:not(.date-disabled):hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgb(var(--primary-500-rgb) / 0.25);
  border-color: rgb(var(--primary-500-rgb) / 0.35);
}

.date-selected {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: #ffffff;
  border-color: var(--primary-700);
  box-shadow: 0 6px 20px rgb(var(--primary-500-rgb) / 0.35);
}

.date-disabled {
  background: linear-gradient(135deg, var(--neutral-100) 0%, var(--neutral-200) 100%);
  color: var(--neutral-400);
  cursor: not-allowed !important;
  border-color: var(--neutral-200);
}

.date-disabled:hover {
  transform: none;
  box-shadow: none;
}

.date-day {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 5px;
}

.date-selected .date-day {
  color: #ffffff;
}

.date-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--primary-800);
}

.date-selected .date-value {
  color: #ffffff;
}

.date-status {
  font-size: 12px;
  color: var(--neutral-400);
  margin-top: 5px;
}

/* 无就诊人提示样式 */
.no-patients-tip {
  margin-bottom: 30px;
  text-align: center;
  padding: 40px 20px;
}

.no-patients-tip .el-empty__description p {
  color: var(--neutral-600);
  font-size: 14px;
  margin: 5px 0;
}

.no-patients-tip .el-button {
  margin-top: 20px;
  padding: 12px 24px;
  font-size: 16px;
  border-radius: 8px;
}

/* 就诊人卡片样式 */
.patient-selection {
  margin-bottom: 30px;
}

.patient-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-50) 100%);
  border: 2px solid rgb(var(--primary-500-rgb) / 0.18);
  border-radius: 12px;
  transition: all 0.3s ease;
  height: 100%;
  display: block;
}

.patient-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgb(var(--primary-500-rgb) / 0.25);
  border-color: rgb(var(--primary-500-rgb) / 0.35);
}

.patient-info {
  padding: 5px 0;
}

.patient-info h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-800);
  margin-bottom: 8px;
}

.patient-info p {
  font-size: 14px;
  color: var(--neutral-600);
  margin-bottom: 5px;
}

.add-patient-card {
  background: linear-gradient(135deg, var(--primary-50) 0%, var(--primary-100) 100%);
  border: 2px dashed rgb(var(--primary-500-rgb) / 0.35);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
}

.add-patient-card:hover {
  background: linear-gradient(135deg, var(--primary-100) 0%, var(--primary-200) 100%);
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgb(var(--primary-500-rgb) / 0.18);
}

.add-patient-card p {
  margin-top: 10px;
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-600);
}

/* 医生卡片样式 */
.doctor-selection {
  margin-bottom: 30px;
}

.doctor-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-50) 100%);
  border: 2px solid rgb(var(--primary-500-rgb) / 0.18);
  border-radius: 12px;
  padding: 15px;
  transition: all 0.3s ease;
  height: 100%;
  margin-bottom: 15px;
}

.doctor-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgb(var(--primary-500-rgb) / 0.25);
  border-color: rgb(var(--primary-500-rgb) / 0.35);
}

.doctor-selected {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: #ffffff;
  border-color: var(--primary-700);
  box-shadow: 0 6px 20px rgb(var(--primary-500-rgb) / 0.35);
}

.doctor-info {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.doctor-details {
  margin-left: 15px;
}

.doctor-details h4 {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-800);
  margin-bottom: 5px;
}

.doctor-selected .doctor-details h4 {
  color: #ffffff;
}

.doctor-details p {
  font-size: 14px;
  color: var(--neutral-600);
  margin: 0;
}

.doctor-selected .doctor-details p {
  color: rgb(255 255 255 / 0.82);
}

.schedule-info {
  border-top: 1px solid rgb(var(--primary-500-rgb) / 0.18);
  padding-top: 10px;
}

.doctor-selected .schedule-info {
  border-top-color: rgba(255, 255, 255, 0.2);
}

.schedule-time {
  font-size: 14px;
  font-weight: 600;
  color: var(--primary-800);
  margin-bottom: 5px;
}

.doctor-selected .schedule-time {
  color: #ffffff;
}

.schedule-remaining {
  font-size: 13px;
  color: var(--neutral-600);
  margin-bottom: 5px;
}

.doctor-selected .schedule-remaining {
  color: rgb(255 255 255 / 0.82);
}

.few-remaining {
  color: var(--error);
  font-weight: 600;
}

.doctor-selected .few-remaining {
  color: rgb(255 255 255 / 0.9);
}

.schedule-fee {
  font-size: 13px;
  color: var(--neutral-600);
}

.doctor-selected .schedule-fee {
  color: rgb(255 255 255 / 0.82);
}

.appointment-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 25% 25%, rgb(var(--primary-500-rgb) / 0.22) 0%, transparent 50%),
    radial-gradient(circle at 75% 75%, rgb(var(--primary-700-rgb) / 0.16) 0%, transparent 50%),
    radial-gradient(circle at 50% 50%, rgb(var(--primary-400-rgb) / 0.12) 0%, transparent 70%);
  pointer-events: none;
  animation: backgroundPulse 10s ease-in-out infinite;
}

@keyframes backgroundPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.appointment-card {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-100) 100%);
  border-radius: 20px;
  padding: 35px;
  box-shadow: var(--shadow-xl);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.45);
  position: relative;
  z-index: 2;
  overflow: hidden;
}

.appointment-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgb(var(--primary-500-rgb) / 0.06) 0%, transparent 50%);
  pointer-events: none;
}

.card-header {
  text-align: center;
  margin-bottom: 35px;
  position: relative;
}

.card-header h2 {
  font-size: 32px;
  font-weight: 700;
  color: var(--primary-800);
  margin-bottom: 15px;
  text-shadow: 0 2px 8px rgb(var(--primary-800-rgb) / 0.22);
}

.card-header::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 100px;
  height: 4px;
  background: linear-gradient(90deg, var(--primary-500) 0%, var(--primary-600) 100%);
  border-radius: 2px;
}

.form-section {
  margin-bottom: 30px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--primary-800);
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-shadow: 0 1px 3px rgb(var(--primary-800-rgb) / 0.14);
}

.section-title::before {
  content: '';
  width: 4px;
  height: 20px;
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  border-radius: 2px;
}

.department-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 15px;
  margin-bottom: 25px;
}

.department-item {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-50) 100%);
  border: 2px solid rgb(var(--primary-500-rgb) / 0.18);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.department-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgb(var(--primary-500-rgb) / 0.08) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.department-item:hover::before {
  opacity: 1;
}

.department-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgb(var(--primary-500-rgb) / 0.25);
  border-color: rgb(var(--primary-500-rgb) / 0.35);
}

.department-item.selected {
  background: linear-gradient(135deg, var(--primary-500) 0%, var(--primary-600) 100%);
  color: #ffffff;
  border-color: var(--primary-700);
  box-shadow: 0 6px 20px rgb(var(--primary-500-rgb) / 0.35);
}

.department-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--primary-800);
  margin-bottom: 8px;
  text-shadow: 0 1px 2px rgb(var(--primary-800-rgb) / 0.12);
}

.department-item.selected .department-name {
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.sub-department-name {
  font-size: 14px;
  color: var(--neutral-600);
  font-weight: 500;
}

.department-item.selected .sub-department-name {
  color: #e2e8f0;
}

.doctor-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 25px;
}

.doctor-item {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid rgba(59, 130, 246, 0.1);
  border-radius: 16px;
  padding: 25px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.doctor-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.doctor-item:hover::before {
  opacity: 1;
}

.doctor-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(59, 130, 246, 0.2);
  border-color: rgba(59, 130, 246, 0.3);
}

.doctor-item.selected {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  border-color: #1e40af;
  box-shadow: 0 8px 25px rgba(37, 99, 235, 0.3);
}

.doctor-name {
  font-size: 18px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
  text-shadow: 0 1px 2px rgba(30, 64, 175, 0.1);
}

.doctor-item.selected .doctor-name {
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.doctor-title {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
  font-weight: 500;
}

.doctor-item.selected .doctor-title {
  color: #e2e8f0;
}

.doctor-schedule {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.doctor-item.selected .doctor-schedule {
  color: #f1f5f9;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
  margin-bottom: 25px;
}

.schedule-item {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid rgba(59, 130, 246, 0.1);
  border-radius: 10px;
  padding: 15px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.schedule-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.schedule-item:hover::before {
  opacity: 1;
}

.schedule-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.15);
  border-color: rgba(59, 130, 246, 0.3);
}

.schedule-item.selected {
  background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
  color: #ffffff;
  border-color: #166534;
  box-shadow: 0 4px 15px rgba(22, 163, 74, 0.3);
}

.schedule-date {
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 5px;
  text-shadow: 0 1px 2px rgba(30, 64, 175, 0.1);
}

.schedule-item.selected .schedule-date {
  color: #ffffff;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.schedule-time {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}

.schedule-item.selected .schedule-time {
  color: #dcfce7;
}

.schedule-item.disabled {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #94a3b8;
  cursor: not-allowed;
  border-color: #e2e8f0;
}

.schedule-item.disabled:hover {
  transform: none;
  box-shadow: none;
}

.patient-form {
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 16px;
  padding: 25px;
  border: 2px solid rgba(59, 130, 246, 0.1);
  margin-bottom: 25px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 8px;
  text-shadow: 0 1px 2px rgba(30, 64, 175, 0.1);
}

.form-group input,
.form-group select,
.form-group textarea {
  padding: 12px 16px;
  border: 2px solid rgba(59, 130, 246, 0.1);
  border-radius: 10px;
  font-size: 14px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  color: #1e40af;
  transition: all 0.3s ease;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
  background: #ffffff;
}

.appointment-summary {
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 16px;
  padding: 25px;
  border: 2px solid rgba(59, 130, 246, 0.2);
  margin-bottom: 25px;
  position: relative;
  overflow: hidden;
}

.appointment-summary::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.summary-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 20px;
  text-shadow: 0 1px 3px rgba(30, 64, 175, 0.1);
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid rgba(59, 130, 246, 0.1);
}

.summary-item:last-child {
  border-bottom: none;
  font-weight: 600;
  font-size: 16px;
  color: #1e40af;
}

.summary-label {
  color: #475569;
  font-weight: 500;
}

.summary-value {
  color: #1e40af;
  font-weight: 600;
  text-shadow: 0 1px 2px rgba(30, 64, 175, 0.1);
}

.price {
  font-size: 20px;
  font-weight: 700;
  color: #dc2626;
  text-shadow: 0 1px 3px rgba(220, 38, 38, 0.2);
}

.submit-button {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  color: #ffffff;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(37, 99, 235, 0.3);
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.submit-button:hover {
  background: linear-gradient(135deg, #1d4ed8 0%, #1e40af 100%);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.4);
  transform: translateY(-2px);
}

.submit-button:disabled {
  background: linear-gradient(135deg, #94a3b8 0%, #64748b 100%);
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .appointment-container {
    padding: 15px;
  }
  
  .appointment-card {
    padding: 25px;
  }
  
  .card-header h2 {
    font-size: 28px;
  }
  
  .department-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }
  
  .doctor-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .schedule-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
  
  .form-row {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}

/* 时段分组样式 */
.time-period-section {
  margin-bottom: 30px;
}

.time-period-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e40af;
  margin-bottom: 15px;
  padding-bottom: 8px;
  border-bottom: 2px solid rgba(59, 130, 246, 0.2);
}

/* 医生卡片禁用状态 */
.doctor-disabled {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%) !important;
  color: #94a3b8 !important;
  cursor: not-allowed !important;
  border-color: #e2e8f0 !important;
}

.doctor-disabled:hover {
  transform: none !important;
  box-shadow: none !important;
}

.doctor-disabled .doctor-details h4 {
  color: #94a3b8 !important;
}

.doctor-disabled .doctor-details p {
  color: #cbd5e1 !important;
}

/* 状态标签样式 */
.status-unavailable {
  color: #dc2626;
  font-weight: 600;
}

.status-full {
  color: #ea580c;
  font-weight: 600;
}

/* 子科室选择器样式 */
.sub-dept-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 0;
}

.sub-dept-image {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
}

.sub-dept-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.sub-dept-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%);
  border-radius: 6px;
  color: #0284c7;
  flex-shrink: 0;
}

.sub-dept-name {
  font-size: 14px;
  color: #1e40af;
  font-weight: 500;
}

.ai-unavailable-pre {
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 60vh;
  overflow: auto;
  margin: 0;
  padding: 12px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgb(var(--primary-500-rgb) / 0.15);
  color: var(--neutral-900);
}
</style>
