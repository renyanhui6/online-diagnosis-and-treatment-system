<template>
  <div class="profile-container">
    <div class="profile-header">
      <h2>个人中心</h2>
    </div>
    
    <el-row :gutter="20">
      
      
   
    
      <el-tabs v-model="activeTab" class="profile-tabs">
        <!-- 基本信息标签页 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>基本信息</span>
              </div>
            </template>
            
            <el-form label-width="100px" class="profile-form">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="姓名">
                    <div class="form-content">{{ doctorInfo.name }}</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="科室ID">
                    <div class="form-content">{{ doctorInfo.subDepartmentId }}</div>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="身份证号">
                    <div class="form-content">{{ maskIdCard(doctorInfo.idCard) }}</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="职业资格证号">
                    <div class="form-content">{{ doctorInfo.professionalLicenseNumber }}</div>
                  </el-form-item>
                </el-col>
              </el-row>
              
              <el-form-item label="个人简介">
                <div class="form-content biography">{{ doctorInfo.biography }}</div>
              </el-form-item>
            </el-form>
          </el-card>
        </el-tab-pane>
        
        <!-- 账号安全标签页 -->
        <el-tab-pane label="账号安全" name="security">
          <el-card shadow="hover">
            <template #header>
              <div class="card-header">
                <span>账号安全</span>
              </div>
            </template>
            
            <div class="security-list">
              <div class="security-item">
                <div class="security-info">
                  <div class="security-title">
                    <el-icon><Lock /></el-icon>
                    <span>登录密码</span>
                  </div>
                  <div class="security-desc">定期修改密码可以保护账号安全</div>
                </div>
                <div class="security-action">
                  <el-button type="primary" @click="changePassword">修改密码</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-tab-pane>
      </el-tabs>
    </el-row>
    
    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="passwordDialogVisible"
      title="修改密码"
      width="500px"
    >
      <el-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        label-width="100px"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input
            v-model="passwordForm.currentPassword"
            type="password"
            show-password
            placeholder="请输入当前密码"
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="passwordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitPasswordChange">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { Lock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getDoctorInfo } from '@/api';

// 状态变量
const activeTab = ref('basic');
const doctorInfo = ref({});

// 对话框状态
const passwordDialogVisible = ref(false);

// 表单数据
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

// 移除系统设置相关代码

// 表单验证规则
const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

// 方法
function maskIdCard(idCard) {
  if (!idCard) return '';
  return idCard.replace(/^(.{6})(.*)(.{4})$/, '$1********$3');
}

function maskPhone(phone) {
  if (!phone) return '';
  return phone.replace(/^(.{3})(.*)(.{4})$/, '$1****$3');
}

function maskEmail(email) {
  if (!email) return '';
  const parts = email.split('@');
  if (parts.length !== 2) return email;
  
  let name = parts[0];
  const domain = parts[1];
  
  if (name.length <= 2) {
    name = name.charAt(0) + '*';
  } else {
    name = name.charAt(0) + '*'.repeat(name.length - 2) + name.charAt(name.length - 1);
  }
  
  return `${name}@${domain}`;
}

// 移除了getSkillColor和formatTooltip函数

function handleAvatarChange(file) {
  // 实际项目中应该上传到服务器
  // 这里模拟上传成功
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = () => {
    doctorInfo.value.avatar = reader.result;
    ElMessage.success('头像上传成功');
  };
}

function changePassword() {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordDialogVisible.value = true;
}

function submitPasswordChange() {
  // 实际项目中应该调用API修改密码
  // 这里模拟修改成功
  setTimeout(() => {
    ElMessage.success('密码修改成功');
    passwordDialogVisible.value = false;
  }, 1000);
}

// 移除了专业技能相关函数

// 移除了changePhone、changeEmail、manageDevices和deactivateAccount函数// 移除工作统计相关函数

// 移除系统设置相关函数

// 获取医生信息
async function fetchDoctorInfo() {
  try {
    const response = await getDoctorInfo();
    if (response.code === 200 && response.data) {
      // 根据后端返回的数据结构更新医生信息
      doctorInfo.value = {
        id: response.data.id || '',
        name: response.data.realName || '',
        avatar: 'https://randomuser.me/api/portraits/men/32.jpg', // 保持默认头像
        gender: response.data.gender || '',
        birthDate: response.data.birthDate || '',
        idCard: response.data.idCard || '',
        phone: response.data.phone || '',
        email: response.data.email || '',
        department: response.data.departmentName || '', // 根据科室ID可能需要额外获取科室名称
        title: response.data.title || '',
        joinDate: response.data.joinDate || '',
        address: response.data.address || '',
        biography: response.data.introduction || '',
        subDepartmentId: response.data.subDepartmentId || '',
        professionalLicenseNumber: response.data.professionalLicenseNumber || '',
        stats: {
          consultations: 1286,
          prescriptions: 952,
          rating: 4.8
        }
      };
      
      // 确保stats对象及其属性存在，设置默认值
      if (!doctorInfo.value.stats) {
        doctorInfo.value.stats = {};
      }
      
      // 为stats对象的属性设置默认值
      doctorInfo.value.stats.consultations = doctorInfo.value.stats.consultations || 0;
      doctorInfo.value.stats.prescriptions = doctorInfo.value.stats.prescriptions || 0;
      doctorInfo.value.stats.rating = doctorInfo.value.stats.rating || 0;
    } else {
      ElMessage.error('获取医生信息失败');
    }
  } catch (error) {
    console.error('获取医生信息失败:', error);
    ElMessage.error('获取医生信息失败，请稍后重试');
  }
}

// 移除工作统计相关函数

// 移除工作统计相关图表初始化函数

// 生命周期钩子
onMounted(() => {
  fetchDoctorInfo();
});
</script>

<style scoped>
.profile-container {
  padding: 0;
}

.profile-header {
  margin-bottom: 20px;
}

.profile-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: -0.2px;
}

.profile-card {
  margin-bottom: 20px;
}

.profile-avatar-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 20px;
}

.profile-upload {
  margin-top: 10px;
}

.profile-info {
  text-align: center;
  margin-bottom: 20px;
}

.profile-info h3 {
  margin: 10px 0;
  font-size: 18px;
  font-weight: 500;
}

.profile-tags {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 15px;
}

.profile-meta {
  text-align: left;
  padding: 0 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--app-text-muted);
}

.meta-item .el-icon {
  margin-right: 8px;
  font-size: 16px;
}

.profile-stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--app-border);
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 500;
  color: var(--brand-600);
  margin-bottom: 5px;
}

.stat-label {
  font-size: 12px;
  color: var(--app-text-muted);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 18px;
  font-weight: 600;
}

.skill-list {
  padding: 0 10px;
}

.skill-item {
  margin-bottom: 15px;
}

.skill-name {
  margin-bottom: 5px;
  font-size: 14px;
  font-weight: 500;
}

.profile-tabs {
  margin-bottom: 20px;
}

.profile-form {
  padding: 20px;
}

.form-content {
  padding: 8px 0;
  color: var(--app-text-muted);
}

.biography {
  white-space: pre-line;
  line-height: 1.6;
}

.security-list {
  padding: 10px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid var(--app-border);
}

.security-item:last-child {
  border-bottom: none;
}

.security-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 5px;
}

.security-title .el-icon {
  margin-right: 8px;
  font-size: 18px;
}

.security-desc {
  font-size: 12px;
  color: var(--app-text-muted);
}

/* 响应式调整 */
@media (max-width: 768px) {
  .profile-container {
    padding: 15px;
  }
  
  .profile-header h2 {
    font-size: 20px;
  }
  
  .security-item {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .security-action {
    margin-top: 10px;
  }
  
  .profile-form {
    padding: 15px;
  }
}
</style>
