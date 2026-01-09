<template>
  <div class="verification-container">
    <el-card class="verification-card">
      <template #header>
        <div class="card-header">
          <h2>实名认证</h2>
        </div>
      </template>
      
      <div class="verification-content">
        <div class="verification-status" v-if="userInfo.isVerified">
          <el-result
            icon="success"
            title="实名认证已完成"
            sub-title="您已完成实名认证，可以使用所有功能"
          >
            <template #extra>
              <el-button type="primary" @click="goToUserCenter">返回个人中心</el-button>
            </template>
          </el-result>
        </div>
        
        <div v-else>
          <el-steps :active="activeStep" finish-status="success" simple>
            <el-step title="填写身份信息" />
            <el-step title="完成认证" />
          </el-steps>
          
          <div class="step-content">
            <!-- 步骤一：填写身份信息 -->
            <div v-if="activeStep === 0" class="step-form">
              <el-form
                ref="identityFormRef"
                :model="identityForm"
                :rules="identityRules"
                label-width="100px"
              >
                <el-form-item label="真实姓名" prop="realName">
                  <el-input v-model="identityForm.realName" placeholder="请输入真实姓名" />
                </el-form-item>
                
                <el-form-item label="身份证号" prop="idCard">
                  <el-input v-model="identityForm.idCard" placeholder="请输入身份证号" />
                </el-form-item>
                
                <el-form-item>
                  <el-button type="primary" @click="nextStep">下一步</el-button>
                </el-form-item>
              </el-form>
            </div>
            
            <!-- 步骤二：完成认证 -->
            <div v-else-if="activeStep === 1" class="step-form">
              <div v-if="verificationStatus === 'pending'" class="verification-pending">
                <el-result
                  icon="info"
                  title="正在提交认证信息..."
                  sub-title="请稍候，正在处理您的实名认证申请"
                >
                  <template #extra>
                    <el-button type="primary" loading>处理中...</el-button>
                  </template>
                </el-result>
              </div>
              
              <div v-else-if="verificationStatus === 'success'" class="verification-success">
                <el-result
                  icon="success"
                  title="实名认证成功"
                  sub-title="您已完成实名认证，可以使用所有功能"
                >
                  <template #extra>
                    <el-button type="primary" @click="goToUserCenter">返回个人中心</el-button>
                  </template>
                </el-result>
              </div>
              
              <div v-else-if="verificationStatus === 'failed'" class="verification-failed">
                <el-result
                  icon="error"
                  title="实名认证失败"
                  sub-title="认证信息审核未通过，请检查信息后重新提交"
                >
                  <template #extra>
                    <el-button @click="resetVerification">重新认证</el-button>
                  </template>
                </el-result>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import UserStorage from '../../utils/userStorage'
import request from '../../api/request'

const router = useRouter()

// 用户信息
const userInfo = ref({
  isVerified: false
})

// 当前步骤
const activeStep = ref(0)

// 身份信息表单
const identityFormRef = ref(null)
const identityForm = reactive({
  realName: '',
  idCard: ''
})

// 身份信息表单验证规则
const identityRules = {
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度在 2 到 20 个字符之间', trigger: 'blur' }
  ],
  idCard: [
    { required: true, message: '请输入身份证号', trigger: 'blur' },
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号', trigger: 'blur' }
  ]
}

// 认证状态
const verificationStatus = ref('') // 'pending', 'success', 'failed'

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    // 从后端获取就诊人列表来判断实名认证状态
    const response = await request({
      url: 'http://localhost:8080/treat/front/patient/attendant/getPatientList',
      method: 'get'
    })
    
    if (response.code === 200) {
      const patientsList = response.data || []
      
      // 查找主就诊人（isMaster == 1）
      const masterPatient = patientsList.find(patient => patient.isMaster === 1)
      
      if (masterPatient) {
        // 判断是否已实名认证（主就诊人必须同时具备身份证号和真实姓名）
        userInfo.value.isVerified = !!(masterPatient.idCard && masterPatient.idCard.trim() !== '' && 
                                       masterPatient.realName && masterPatient.realName.trim() !== '')
        
        if (userInfo.value.isVerified) {
          // 如果已经实名认证，直接显示认证成功
          activeStep.value = 1
          verificationStatus.value = 'success'
          
          console.log('用户已实名认证，主就诊人信息:', masterPatient)
        } else {
          console.log('用户未实名认证，主就诊人信息不完整:', masterPatient)
        }
      } else {
        // 没有主就诊人表示未实名认证
        userInfo.value.isVerified = false
        console.log('用户未实名认证')
      }
    } else {
      console.error('获取就诊人列表失败:', response.message)
      userInfo.value.isVerified = false
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    userInfo.value.isVerified = false
  }
}

// 不再需要发送验证码函数

// 下一步（提交实名认证）
const nextStep = async () => {
  if (!identityFormRef.value) return
  
  await identityFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        verificationStatus.value = 'pending'
        
        // 调用后端API提交实名认证信息
        const response = await request({
          url: 'http://localhost:8080/treat/front/patient/attendant/addIdCard',
          method: 'post',
          data: {
            realName: identityForm.realName,
            idCard: identityForm.idCard
          }
        })
        
        if (response.code === 200) {
          // 认证成功
          activeStep.value++
          verificationStatus.value = 'success'
          ElMessage.success('实名认证成功')
          
          // 更新用户信息
          userInfo.value.isVerified = true
          
          // 更新用户信息到localStorage
          const currentUserInfo = UserStorage.getUserInfo()
          UserStorage.setUserInfo({
            ...currentUserInfo,
            isVerified: true
          })
        } else {
          // 认证失败
          verificationStatus.value = 'failed'
          ElMessage.error(response.message || '实名认证失败')
        }
      } catch (error) {
        console.error('提交认证信息失败:', error)
        verificationStatus.value = 'failed'
        ElMessage.error('提交认证信息失败，请稍后重试')
      }
    }
  })
}

// 上一步
const prevStep = () => {
  activeStep.value--
}

// 不再需要照片上传和提交认证的函数，已合并到nextStep函数中

// 重新认证
const resetVerification = () => {
  activeStep.value = 0
  verificationStatus.value = ''
  
  // 重置表单
  if (identityFormRef.value) {
    identityFormRef.value.resetFields()
  }
}

// 返回个人中心
const goToUserCenter = () => {
  router.push('/user')
}

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})

</script>

<style scoped>
.verification-container {
  max-width: 860px;
  margin: 0 auto;
  padding: 12px;
}

.verification-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
}

.verification-content {
  padding: 12px 0;
}

.step-content {
  margin-top: 20px;
}

.step-form {
  max-width: 640px;
  margin: 0 auto;
}

.verify-code-input {
  display: flex;
  gap: 10px;
}

.verify-code-input .el-input {
  flex: 1;
}

.upload-tips {
  margin-bottom: 20px;
  padding: 10px;
  background: rgb(var(--primary-50-rgb) / 0.65);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.35);
  border-radius: 10px;
}

.upload-tips p {
  margin: 5px 0;
  color: var(--neutral-600);
  font-size: 14px;
}

.id-card-upload {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 30px;
}

.upload-item {
  flex: 1;
}

.upload-item h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 500;
  color: var(--neutral-800);
}

.upload-container {
  width: 100%;
  height: 180px;
  border: 1px dashed rgb(var(--primary-200-rgb) / 0.7);
  border-radius: 12px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.55);
}

.upload-container:hover {
  border-color: var(--primary-500);
  box-shadow: var(--shadow);
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--neutral-500);
}

.upload-placeholder .el-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.upload-text {
  font-size: 14px;
  color: var(--neutral-500);
}

.preview-container {
  width: 100%;
  height: 100%;
  position: relative;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.preview-actions {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  padding: 8px;
  display: flex;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.preview-container:hover .preview-actions {
  opacity: 1;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 30px;
}

.verification-pending,
.verification-success,
.verification-failed {
  padding: 20px 0;
}
</style>
