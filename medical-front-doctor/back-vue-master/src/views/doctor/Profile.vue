<template>
  <div class="profile-container">
    <section class="profile-hero">
      <div class="hero-main">
        <div class="hero-avatar">{{ doctorInitial }}</div>
        <div class="hero-copy">
          <span class="hero-eyebrow">个人中心</span>
          <h2>{{ doctorInfo.name || '医生档案' }}</h2>
          <p>
            统一维护医生身份、执业信息与联系方式，保证接诊页面、病历记录和管理端数据口径一致。
          </p>
          <div class="hero-tags">
            <span class="info-pill">{{ displayDepartment }}</span>
            <span class="info-pill">{{ displayTitle }}</span>
            <span class="info-pill">执业编号 {{ doctorInfo.professionalLicenseNumber || '待补充' }}</span>
          </div>
        </div>
      </div>

      <div class="hero-side">
        <div class="hero-stat-card">
          <span class="stat-label">联系电话</span>
          <strong>{{ maskPhone(doctorInfo.phone) || '未填写' }}</strong>
          <small>用于患者回访与平台通知</small>
        </div>
        <div class="hero-stat-card">
          <span class="stat-label">邮箱地址</span>
          <strong>{{ maskEmail(doctorInfo.email) || '未填写' }}</strong>
          <small>用于账号安全与消息确认</small>
        </div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="profile-tabs">
      <el-tab-pane label="基本信息" name="basic">
        <div class="info-grid">
          <el-card shadow="hover" class="info-card">
            <template #header>
              <div class="card-header">
                <div>
                  <span>身份与执业信息</span>
                  <small>展示医生基础身份信息和执业资质</small>
                </div>
              </div>
            </template>

            <div class="info-list">
              <div class="info-item">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ doctorInfo.name || '未填写' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">所属科室</span>
                <span class="info-value">{{ displayDepartment }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">职称</span>
                <span class="info-value">{{ displayTitle }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">身份证号</span>
                <span class="info-value">{{ maskIdCard(doctorInfo.idCard) || '未填写' }}</span>
              </div>
              <div class="info-item info-item-full">
                <span class="info-label">职业资格证号</span>
                <span class="info-value">{{ doctorInfo.professionalLicenseNumber || '未填写' }}</span>
              </div>
            </div>
          </el-card>

          <el-card shadow="hover" class="info-card">
            <template #header>
              <div class="card-header">
                <div>
                  <span>联系与资料信息</span>
                  <small>用于患者沟通、平台通知与信息校验</small>
                </div>
              </div>
            </template>

            <div class="info-list">
              <div class="info-item">
                <span class="info-label">手机号码</span>
                <span class="info-value">{{ maskPhone(doctorInfo.phone) || '未填写' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">邮箱地址</span>
                <span class="info-value">{{ maskEmail(doctorInfo.email) || '未填写' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">医生编号</span>
                <span class="info-value">{{ doctorInfo.id || '未分配' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">子科室编号</span>
                <span class="info-value">{{ doctorInfo.subDepartmentId || '未分配' }}</span>
              </div>
            </div>
          </el-card>
        </div>

        <el-card shadow="hover" class="intro-card">
          <template #header>
            <div class="card-header">
              <div>
                <span>个人简介</span>
                <small>面向患者展示的专业背景与接诊方向</small>
              </div>
            </div>
          </template>

          <p class="biography-text">
            {{ doctorInfo.biography || '暂未填写个人简介。' }}
          </p>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="账号安全" name="security">
        <el-card shadow="hover" class="security-card">
          <div class="security-panel">
            <div class="security-copy">
              <div class="security-title">
                <el-icon><Lock /></el-icon>
                <span>登录密码管理</span>
              </div>
              <p>建议定期更换密码，并避免在多端复用同一组口令，降低账号被盗用的风险。</p>
              <ul class="security-tips">
                <li>密码长度不少于 6 位，建议包含字母与数字。</li>
                <li>如发现异常登录，请立即修改密码并重新登录。</li>
                <li>联系方式应保持有效，便于平台发送安全提醒。</li>
              </ul>
            </div>
            <div class="security-action">
              <el-button type="primary" size="large" @click="changePassword">修改密码</el-button>
            </div>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

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
import { computed, onMounted, reactive, ref } from 'vue';
import { Lock } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getDoctorInfo, modifyPassword } from '@/api';

const activeTab = ref('basic');
const passwordDialogVisible = ref(false);
const passwordFormRef = ref(null);

const doctorInfo = ref({
  id: '',
  name: '',
  phone: '',
  email: '',
  department: '',
  title: '',
  idCard: '',
  biography: '',
  subDepartmentId: '',
  professionalLicenseNumber: ''
});

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
});

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

const doctorInitial = computed(() => doctorInfo.value.name?.charAt(0) || '医');
const displayDepartment = computed(() => doctorInfo.value.department || '未分配科室');
const displayTitle = computed(() => doctorInfo.value.title || '未设置职称');

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

function changePassword() {
  passwordForm.currentPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordDialogVisible.value = true;
}

async function submitPasswordChange() {
  if (!passwordFormRef.value) return;

  try {
    await passwordFormRef.value.validate();
    const response = await modifyPassword({
      password: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword
    });

    if (response.code === 200) {
      ElMessage.success('密码修改成功');
      passwordDialogVisible.value = false;
      return;
    }

    ElMessage.error(response.message || '密码修改失败');
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message);
      return;
    }
    ElMessage.error('密码修改失败，请稍后重试');
  }
}

async function fetchDoctorInfo() {
  try {
    const response = await getDoctorInfo();
    if (response.code === 200 && response.data) {
      doctorInfo.value = {
        id: response.data.id || '',
        name: response.data.realName || '',
        phone: response.data.phone || '',
        email: response.data.email || '',
        department: response.data.departmentName || '',
        title: response.data.title || '',
        idCard: response.data.idCard || '',
        biography: response.data.introduction || '',
        subDepartmentId: response.data.subDepartmentId || '',
        professionalLicenseNumber: response.data.professionalLicenseNumber || ''
      };
      return;
    }

    ElMessage.error('获取医生信息失败');
  } catch (error) {
    console.error('获取医生信息失败:', error);
    ElMessage.error('获取医生信息失败，请稍后重试');
  }
}

onMounted(() => {
  fetchDoctorInfo();
});
</script>

<style scoped>
.profile-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 1fr);
  gap: 20px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 38%),
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.12), transparent 32%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.1);
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.hero-avatar {
  width: 88px;
  height: 88px;
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #eff6ff;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.25);
  flex-shrink: 0;
}

.hero-copy {
  min-width: 0;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  margin-bottom: 10px;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.88);
}

.hero-copy h2 {
  margin: 0;
  font-size: 32px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.hero-copy p {
  margin: 12px 0 16px;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.75;
  color: #475569;
}

.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.info-pill {
  padding: 10px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(148, 163, 184, 0.24);
}

.hero-side {
  display: grid;
  gap: 14px;
}

.hero-stat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.stat-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.hero-stat-card strong {
  font-size: 18px;
  color: #0f172a;
}

.hero-stat-card small {
  color: #64748b;
  line-height: 1.6;
}

.profile-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.profile-tabs :deep(.el-tabs__header) {
  margin-bottom: 18px;
}

.profile-tabs :deep(.el-tabs__item) {
  height: 42px;
  padding: 0 18px;
  font-weight: 600;
  color: #64748b;
}

.profile-tabs :deep(.el-tabs__item.is-active) {
  color: #1d4ed8;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.info-card,
.intro-card,
.security-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header span {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.card-header small {
  display: block;
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.info-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 92px;
  padding: 18px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.96));
  border: 1px solid rgba(226, 232, 240, 0.9);
}

.info-item-full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.info-value {
  font-size: 16px;
  line-height: 1.6;
  color: #0f172a;
  word-break: break-word;
}

.intro-card {
  margin-top: 20px;
}

.biography-text {
  margin: 0;
  padding: 2px 4px 6px;
  font-size: 15px;
  line-height: 1.95;
  color: #334155;
  white-space: pre-line;
}

.security-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.security-copy {
  flex: 1;
}

.security-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.security-title :deep(.el-icon) {
  color: #2563eb;
}

.security-copy p {
  margin: 0 0 14px;
  line-height: 1.8;
  color: #475569;
}

.security-tips {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.9;
}

.security-action {
  flex-shrink: 0;
}

@media (max-width: 1100px) {
  .profile-hero,
  .info-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .profile-hero {
    padding: 22px;
    border-radius: 24px;
  }

  .hero-main {
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-copy h2 {
    font-size: 28px;
  }

  .info-list {
    grid-template-columns: 1fr;
  }

  .security-panel {
    flex-direction: column;
    align-items: stretch;
  }

  .security-action {
    width: 100%;
  }

  .security-action :deep(.el-button) {
    width: 100%;
  }
}
</style>
