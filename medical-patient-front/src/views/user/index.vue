<template>
  <div class="user-container">
    <el-row :gutter="20">
      <!-- 右侧内容区域 -->
      <el-col :span="24">
        <el-tabs v-model="activeTab" class="user-tabs">
          <!-- 个人信息标签页 -->
          <el-tab-pane label="个人信息" name="info">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>基本信息</span>
                  <el-button 
                    type="primary" 
                    plain 
                    size="small" 
                    @click="startEditInfo"
                    v-if="!isEditing"
                  >
                    编辑信息
                  </el-button>
                </div>
              </template>
              
              <el-form 
                :model="userForm" 
                :rules="userFormRules" 
                ref="userFormRef" 
                label-width="100px"
                :disabled="!isEditing"
              >
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="userForm.realName" disabled />
                </el-form-item>
                
                <el-form-item label="身份证号" prop="idCard">
                  <el-input v-model="userForm.idCard" disabled />
                </el-form-item>
                
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="userForm.phone" placeholder="请输入手机号码" />
                </el-form-item>
                
                <el-form-item label="性别" prop="gender">
                  <el-radio-group v-model="userForm.gender">
                    <el-radio :label="1">男</el-radio>
                    <el-radio :label="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
                
                <el-form-item label="地址" prop="address">
                  <el-input v-model="userForm.address" placeholder="请输入地址" />
                </el-form-item>
                
                <el-form-item v-if="isEditing">
                  <el-button type="primary" @click="submitUserForm">保存</el-button>
                  <el-button @click="cancelEdit">取消</el-button>
                </el-form-item>
              </el-form>
            </el-card>
            
            <el-card class="mt-20">
              <template #header>
                <div class="card-header">
                  <span>账号安全</span>
                </div>
              </template>
              
              <div class="security-items">
                <div class="security-item">
                  <div class="security-info">
                    <div class="security-title">登录密码</div>
                    <div class="security-desc">定期修改密码可以保护账号安全</div>
                  </div>
                  <el-button type="primary" plain @click="showChangePasswordDialog">修改密码</el-button>
                </div>
                
                <el-divider />
                
                <div class="security-item">
                  <div class="security-info">
                    <div class="security-title">实名认证</div>
                    <div class="security-desc">
                      {{ userInfo.isVerified ? '您已完成实名认证' : '完成实名认证后可使用更多功能' }}
                    </div>
                  </div>
                  <el-button 
                    type="primary" 
                    plain 
                    @click="goToVerification"
                    :disabled="userInfo.isVerified"
                  >
                    {{ userInfo.isVerified ? '已认证' : '去认证' }}
                  </el-button>
                </div>
              </div>
            </el-card>
          </el-tab-pane>
          
          <!-- 就诊人管理标签页 -->
          <el-tab-pane label="就诊人管理" name="patients">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>就诊人列表</span>
                  <el-button type="primary" plain size="small" @click="showPatientDialog('add')">添加就诊人</el-button>
                </div>
              </template>
              
              <div v-loading="patientsLoading">
                <el-empty v-if="patientsList.length === 0" description="暂无就诊人信息" />
                
                <div v-else class="patients-list">
                  <el-card
                    v-for="patient in patientsList"
                    :key="patient.id"
                    class="patient-card"
                    :class="{ 'is-default': patient.isMaster === 1 }"
                  >
                    <div class="patient-info">
                      <div class="patient-name">
                        <span>{{ patient.realName || patient.real_name }}</span>
                        <el-tag v-if="patient.isMaster === 1" type="success" size="small">主就诊人</el-tag>
                        <el-tag v-if="patient.nickname" type="primary" size="small">{{ patient.nickname }}</el-tag>
                      </div>
                    </div>
                    
                    <div class="patient-actions">
                      <el-popconfirm
                        v-if="patient.isMaster !== 1"
                        title="确定删除该就诊人信息吗？"
                        @confirm="deletePatient(patient.id)"
                      >
                        <template #reference>
                          <el-button type="danger" link>删除</el-button>
                        </template>
                      </el-popconfirm>
                    </div>
                  </el-card>
                </div>
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-col>
    </el-row>
    
    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px">
      <el-form
        :model="passwordForm"
        :rules="passwordRules"
        ref="passwordFormRef"
        label-width="120px"
      >
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input 
            v-model="passwordForm.oldPassword" 
            type="password" 
            placeholder="请输入当前密码" 
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password" 
            placeholder="请输入新密码" 
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码" 
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPasswordForm" :loading="passwordSubmitting">
          确认修改
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 就诊人信息对话框 -->
    <el-dialog 
      v-model="patientDialogVisible" 
      title="添加就诊人" 
      width="500px"
    >
      <el-form
        :model="patientForm"
        :rules="patientRules"
        ref="patientFormRef"
        label-width="100px"
      >
        <el-form-item label="姓名" prop="real_name">
          <el-input v-model="patientForm.real_name" placeholder="请输入姓名" />
        </el-form-item>
        
        <el-form-item label="身份证号" prop="id_card">
          <el-input v-model="patientForm.id_card" placeholder="请输入身份证号" />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="patientDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPatientForm" :loading="patientSubmitting">
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserStorage from '../../utils/userStorage'
import request from '../../api/request'

const router = useRouter()
const route = useRoute()



// 用户信息
const userInfo = ref({
  id: '',
  username: '',
  phone: '',
  gender: 1,
  address: '',
  isVerified: false
})

// 标签页
const activeTab = ref(route.query.tab === 'patients' ? 'patients' : 'info')

// 编辑状态
const isEditing = ref(false)

// 用户表单
const userFormRef = ref(null)
const userForm = reactive({
  realName: '',
  idCard: '',
  phone: '',
  gender: 1,
  address: ''
})

// 用户表单验证规则
const userFormRules = {
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' }
  ]
}

// 修改密码对话框
const passwordDialogVisible = ref(false)
const passwordSubmitting = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码表单验证规则
const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 就诊人列表
const patientsList = ref([])
const patientsLoading = ref(false)

// 就诊人对话框
const patientDialogVisible = ref(false)
const patientDialogType = ref('add') // 'add' 或 'edit'
const patientSubmitting = ref(false)
const patientFormRef = ref(null)
const patientForm = reactive({
  id: '',
  real_name: '',
  id_card: ''
})

// 就诊人表单验证规则
const patientRules = {
  real_name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符之间', trigger: 'blur' }
  ],
  id_card: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  gender: [
    { required: true, message: '请选择性别', trigger: 'change' }
  ],
  home_address: [
    { required: true, message: '请输入地址', trigger: 'blur' }
  ]
}

// 格式化身份证号显示（中间用*号隐藏）
const formatIdCard = (idCard) => {
  if (!idCard || typeof idCard !== 'string') return '未填写'
  if (idCard.length < 8) return idCard
  return idCard.substring(0, 6) + '********' + idCard.substring(14)
}

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 获取就诊人列表，从中找到主就诊人信息
    const response = await request({
      url: 'http://localhost:8080/treat/front/patient/attendant/getPatientList',
      method: 'get'
    })
    
    if (response.code === 200) {
      const patientsList = response.data || []
      
      // 查找主就诊人（isMaster == 1）
      const masterPatient = patientsList.find(patient => patient.isMaster === 1)
      
      if (masterPatient) {
        // 设置用户信息为主就诊人信息
        Object.assign(userForm, {
          realName: masterPatient.realName || '',
          idCard: masterPatient.idCard || '',
          phone: masterPatient.phone || '',
          gender: masterPatient.gender || 1,
          address: masterPatient.homeAddress || ''
        })
        
        // 判断是否已实名认证（主就诊人必须同时具备身份证号和真实姓名）
        userInfo.value.isVerified = !!(masterPatient.idCard && masterPatient.idCard.trim() !== '' && 
                                       masterPatient.realName && masterPatient.realName.trim() !== '')
        
        console.log('获取主就诊人信息成功:', masterPatient)
        console.log('实名认证状态:', userInfo.value.isVerified)
        console.log('身份证号:', masterPatient.idCard)
        console.log('真实姓名:', masterPatient.realName)
      } else {
        console.log('未找到主就诊人')
        userInfo.value.isVerified = false
      }
    } else {
      ElMessage.error(response.message || '获取用户信息失败')
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    ElMessage.error('获取用户信息失败，请稍后重试')
  }
}

// 获取就诊人列表
const fetchPatientsList = async () => {
  patientsLoading.value = true
  
  try {
    // 从后端获取就诊人列表
    const response = await request({
      url: 'http://localhost:8080/treat/front/patient/attendant/getPatientList',
      method: 'get'
    })
    
    if (response.code === 200) {
      const allPatients = response.data || []
      
      // 显示所有就诊人（包括主就诊人和非主就诊人）
      patientsList.value = allPatients
      
      console.log('获取就诊人列表成功:', patientsList.value)
    } else {
      ElMessage.error(response.message || '获取就诊人列表失败')
      patientsList.value = []
    }
    
    patientsLoading.value = false
  } catch (error) {
    console.error('获取就诊人列表失败:', error)
    ElMessage.error('获取就诊人列表失败，请稍后重试')
    patientsList.value = []
    patientsLoading.value = false
  }
}


// 开始编辑信息
const startEditInfo = () => {
  isEditing.value = true
}

// 取消编辑
const cancelEdit = () => {
  userFormRef.value.resetFields()
  isEditing.value = false
}

// 提交用户信息表单（更新主就诊人信息）
const submitUserForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 获取就诊人列表，找到主就诊人
        const response = await request({
          url: 'http://localhost:8080/treat/front/patient/attendant/getPatientList',
          method: 'get'
        })
        
        if (response.code === 200) {
          const patientsList = response.data || []
          const masterPatient = patientsList.find(patient => patient.isMaster === 1)
          
          if (masterPatient) {
            // 准备要更新的主就诊人数据
            const updateData = {
              id: masterPatient.id,
              systemUserId: masterPatient.systemUserId,
              realName: userForm.realName,
              idCard: userForm.idCard,
              gender: userForm.gender,
              phone: userForm.phone,
              homeAddress: userForm.address,
              isMaster: 1
            }
            
            console.log('提交更新主就诊人信息:', updateData)
            
            // 调用API更新主就诊人信息
            const updateResponse = await request({
              url: 'http://localhost:8080/treat/front/patient/attendant/updateInfo',
              method: 'post',
              data: updateData
            })
            
            if (updateResponse.code === 200) {
              ElMessage.success('个人信息更新成功')
              isEditing.value = false
              
              // 重新获取用户信息
              await fetchUserInfo()
            } else {
              ElMessage.error(updateResponse.message || '更新个人信息失败')
            }
          } else {
            ElMessage.error('未找到主就诊人信息')
          }
        } else {
          ElMessage.error('获取就诊人列表失败')
        }
      } catch (error) {
        console.error('更新个人信息失败:', error)
        ElMessage.error('更新个人信息失败，请稍后重试')
      }
    }
  })
}

// 显示修改密码对话框
const showChangePasswordDialog = () => {
  passwordDialogVisible.value = true
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// 提交修改密码表单
const submitPasswordForm = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (valid) {
      passwordSubmitting.value = true
      
      try {
        // 调用后端接口修改密码
        const response = await request({
          url: 'http://localhost:8080/treat/front/loginAndOut/modifyPassword',
          method: 'post',
          data: {
            password: passwordForm.oldPassword,  // 后端期望的字段名是 password
            newPassword: passwordForm.newPassword
          }
        })
        
        if (response.code === 200) {
          ElMessage.success('密码修改成功，请重新登录')
          passwordDialogVisible.value = false
          UserStorage.clearUserData()
          router.push('/login')
        } else {
          ElMessage.error(response.message || '修改密码失败')
        }
      } catch (error) {
        console.error('修改密码失败:', error)
        ElMessage.error('修改密码失败，请稍后重试')
      } finally {
        passwordSubmitting.value = false
      }
    }
  })
}

// 跳转到实名认证页面
const goToVerification = () => {
  // 如果已经实名认证，不允许重复认证
  if (userInfo.value.isVerified) {
    ElMessage.warning('您已完成实名认证，无需重复认证')
    return
  }
  
  // 跳转到实名认证页面
  router.push('/user/verification')
}

// 提交就诊人表单（仅支持添加）
const submitPatientForm = async () => {
  if (!patientFormRef.value) return
  
  await patientFormRef.value.validate(async (valid) => {
    if (valid) {
      patientSubmitting.value = true
      
      try {
        // 准备要添加的就诊人数据（后端仅需要姓名+身份证）
        const patientData = {
          realName: patientForm.real_name,
          idCard: patientForm.id_card
        }
        
        console.log('提交添加就诊人信息:', patientData)
        
        // 调用API添加就诊人
        const response = await request({
          url: 'http://localhost:8080/treat/front/patient/attendant/addPatientAttendant',
          method: 'post',
          data: patientData
        })
        
        if (response.code === 200) {
          ElMessage.success('添加就诊人成功')
          // 重新获取就诊人列表
          await fetchPatientsList()
          patientDialogVisible.value = false
        } else {
          ElMessage.error(response.message || '添加就诊人失败')
        }
      } catch (error) {
        console.error('提交就诊人信息失败:', error)
        ElMessage.error('提交就诊人信息失败，请稍后重试')
      } finally {
        patientSubmitting.value = false
      }
    }
  })
}

// 显示就诊人对话框（仅用于添加）
const showPatientDialog = (type, patient = null) => {
  if (type !== 'add') return // 只允许添加操作
  
  patientDialogType.value = type
  patientDialogVisible.value = true
  
  // 重置表单
  if (patientFormRef.value) {
    patientFormRef.value.resetFields()
  }
  
  // 添加就诊人，重置表单
  Object.assign(patientForm, {
    id: '',
    real_name: '',
    gender: 1,
    id_card: '',
    phone: '',
    home_address: ''
  })
}

// 删除就诊人
const deletePatient = async (id) => {
  try {
    // 调用后端接口删除就诊人
    const response = await request({
      url: `http://localhost:8080/treat/front/patient/attendant/removePatientAttendant?patientAttendantId=${id}`,
      method: 'get'
    })
    
    if (response.code === 200) {
      ElMessage.success('删除就诊人成功')
      // 重新获取就诊人列表
      await fetchPatientsList()
    } else {
      ElMessage.error(response.message || '删除就诊人失败')
    }
  } catch (error) {
    console.error('删除就诊人失败:', error)
    ElMessage.error('删除就诊人失败，请稍后重试')
  }
}


// 初始化
onMounted(async () => {
  await Promise.all([fetchUserInfo(), fetchPatientsList()])

  const tab = route.query.tab
  if (tab === 'info' || tab === 'patients') {
    activeTab.value = tab
  }

  if (route.query.action === 'addPatient') {
    activeTab.value = 'patients'
    await nextTick()
    showPatientDialog('add')
  }
})
</script>

<style scoped>
.user-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px;
}

.user-card {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
}

.user-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-upload {
  margin-top: 10px;
}

.user-info {
  width: 100%;
  text-align: center;
}

.username {
  margin: 10px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--neutral-800);
}

.user-id {
  color: var(--neutral-600);
  font-size: 14px;
  margin-bottom: 15px;
}

.user-status {
  margin-bottom: 20px;
}

.user-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  border-top: 1px solid rgb(var(--primary-200-rgb) / 0.35);
  padding-top: 20px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: var(--primary-700);
}

.stat-label {
  font-size: 14px;
  color: var(--neutral-600);
  margin-top: 5px;
}

.user-tabs {
  margin-top: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.security-items {
  padding: 10px 0;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid rgb(var(--primary-200-rgb) / 0.25);
}

.security-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--neutral-800);
  margin-bottom: 5px;
}

.security-desc {
  font-size: 14px;
  color: var(--neutral-600);
}

.patients-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.patient-card {
  border-radius: 12px;
  overflow: hidden;
}

.patient-card :deep(.el-card__body) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.patient-card.is-default {
  border-left: 4px solid var(--primary-500);
}

.patient-card.is-default :deep(.el-card__body) {
  background: rgb(var(--primary-50-rgb) / 0.55);
}

.patient-info {
  flex: 1;
}

.patient-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--neutral-800);
  margin-bottom: 5px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.patient-detail {
  color: var(--neutral-600);
  font-size: 14px;
  margin-bottom: 5px;
  display: flex;
  gap: 15px;
}

.patient-contact {
  color: var(--neutral-500);
  font-size: 14px;
}

.patient-actions {
  display: flex;
  gap: 10px;
}

.mt-20 {
  margin-top: 20px;
}
</style>
