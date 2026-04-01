<template>
  <div class="home-container">
    <!-- 轮播图 -->
    <div class="banner-section animate__animated animate__fadeIn">
      <el-carousel height="350px" indicator-position="outside" :interval="5000" arrow="always">
        <el-carousel-item v-for="(item, index) in banners" :key="index">
          <div class="banner-item" :style="{ backgroundImage: `url(${item.image})` }">
            <div class="banner-content animate__animated" :class="[`animate__delay-${index}s`, 'animate__fadeInLeft']">
              <h2>{{ item.title }}</h2>
              <p>{{ item.description }}</p>
            </div>
          </div>
        </el-carousel-item>
      </el-carousel>
    </div>

    <div class="ai-triage-section animate__animated animate__fadeIn animate__delay-1s">
      <div class="ai-triage-card">
        <div class="ai-triage-content">
          <div class="ai-triage-badge">独立模块</div>
          <h2>不确定挂哪个科室，先做 AI 智能分诊</h2>
          <p>
            先描述症状，系统会结合院内真实科室、子科室和近期排班给出挂号建议，再带你进入预约挂号。
          </p>
          <div class="ai-triage-actions">
            <el-button type="primary" size="large" @click="goToAiTriage">进入 AI 分诊</el-button>
            <el-button size="large" @click="goToAppointment">直接手动挂号</el-button>
          </div>
        </div>
        <div class="ai-triage-side">
          <div class="ai-triage-tip">
            <h3>适合场景</h3>
            <ul>
              <li>不知道该挂哪个科室</li>
              <li>多个科室看起来都像</li>
              <li>想先缩小挂号范围</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 科室信息 -->
    <div class="department-section animate__animated animate__fadeIn animate__delay-1s">
      <div class="section-header">
        <h2><i class="el-icon-Plus"></i> 科室导航</h2>
        <el-button type="primary" @click="goToAppointment" class="appointment-btn" icon="el-icon-Right">立即预约</el-button>
      </div>
      
      <!-- 嵌套展示主科室和子科室 -->
      <div class="nested-departments">
        <div v-for="(dept, deptIndex) in allDepartments" :key="'dept-' + dept.id" 
             class="department-group animate__animated animate__fadeInUp" 
             :style="{'animation-delay': `${deptIndex * 0.2}s`}">
          
          <!-- 主科室卡片 -->
          <div class="main-department-card">
            <div class="main-dept-header">
              <div class="dept-title-section">
                <h3 class="dept-title">{{ dept.departmentName }}</h3>
                <el-tag size="small" type="primary" effect="dark" class="dept-tag">主科室</el-tag>
              </div>
              <div class="dept-actions">
                <el-button type="primary" size="small" @click="handleDeptClick(dept.id)" class="main-dept-btn">
                  <i class="el-icon-date"></i> 预约科室
                </el-button>
              </div>
            </div>
            <div class="main-dept-content">
              <p class="dept-description">{{ dept.description }}</p>
            </div>
            
            <!-- 子科室展示区域 -->
            <div class="sub-departments-container" v-if="getSubDepartmentsByParent(dept.id).length > 0">
              <div class="sub-dept-header">
                <h4><i class="el-icon-arrow-right"></i> 下属科室</h4>
                <span class="sub-dept-count">{{ getSubDepartmentsByParent(dept.id).length }}个</span>
              </div>
              
              <div class="sub-departments-grid">
                <div v-for="(subDept, subIndex) in getSubDepartmentsByParent(dept.id)" 
                     :key="'sub-' + subDept.id"
                     class="sub-department-item animate__animated animate__fadeInRight"
                     :style="{'animation-delay': `${(deptIndex * 0.2) + (subIndex * 0.1)}s`}">
                  
                  <div class="sub-dept-card">
                    <!-- 子科室图片 -->
                    <div class="sub-dept-image">
                      <img :src="getSubDeptImage(subDept)" :alt="subDept.departmentName" class="animated-image">
                      <div class="image-overlay">
                        <i class="el-icon-view"></i>
                      </div>
                    </div>
                    
                    <!-- 子科室信息 -->
                    <div class="sub-dept-info">
                      <div class="sub-dept-title-section">
                        <h5 class="sub-dept-title">{{ subDept.departmentName }}</h5>
                        <el-tag size="mini" type="success" effect="plain">子科室</el-tag>
                      </div>
                      
                      <div class="sub-dept-details">
                        <div class="detail-item" v-if="subDept.description">
                          <span class="detail-label"><i class="el-icon-document"></i> 科室描述</span>
                          <p class="detail-value">{{ subDept.description }}</p>
                        </div>
                        <div class="detail-item" v-if="subDept.treatmentScope">
                          <span class="detail-label"><i class="el-icon-medal"></i> 诊疗范围</span>
                          <p class="detail-value">{{ subDept.treatmentScope }}</p>
                        </div>
                        <div class="detail-item" v-if="subDept.departmentFeatures">
                          <span class="detail-label"><i class="el-icon-star-on"></i> 科室特色</span>
                          <p class="detail-value">{{ subDept.departmentFeatures }}</p>
                        </div>
                      </div>
                      
                      <div class="sub-dept-actions">
                        <el-button type="success" size="mini" @click="handleSubDeptClick(subDept.parentId, subDept.id)" 
                                   class="sub-dept-btn">
                          <i class="el-icon-check"></i> 预约此科室
                        </el-button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
            <!-- 无子科室提示 -->
            <div v-else class="no-sub-departments">
              <i class="el-icon-info"></i>
              <span>该科室暂无下属子科室</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDepartmentList, getSubDepartmentList } from '../../api/appointment'

const router = useRouter()

// 轮播图数据
const banners = ref([
  {
    title: '专业医疗团队',
    description: '汇聚国内外顶尖医疗专家，为您提供专业诊疗服务',
    image: 'https://img.freepik.com/free-photo/team-young-specialist-doctors-standing-corridor-hospital_1303-21199.jpg?w=2000&t=st=1686747631~exp=1686748231~hmac=4d4d5bc9e5e5f5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5'
  },
  {
    title: '先进医疗设备',
    description: '引进国际先进医疗设备，提供精准诊断和治疗',
    image: 'https://img.freepik.com/free-photo/doctor-with-stethoscope-hands-hospital-background_1423-1.jpg?w=2000&t=st=1686747631~exp=1686748231~hmac=4d4d5bc9e5e5f5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5'
  },
  {
    title: '舒适就医环境',
    description: '温馨舒适的就医环境，让您的就医体验更加愉悦',
    image: 'https://img.freepik.com/free-photo/hospital-corridor-with-empty-chairs_1339-4725.jpg?w=2000&t=st=1686747631~exp=1686748231~hmac=4d4d5bc9e5e5f5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5b5'
  }
])

// 科室分类
const departmentCategories = ref([])

// 科室数据
const allDepartments = ref([])
const allSubDepartments = ref([])

// 获取子科室图片
const getSubDeptImage = (subDept) => {
  // 如果有原始图片路径，使用原始图片
  if (subDept.imagePath) {
    return subDept.imagePath
  }
}

// 获取指定父科室的子科室
const getSubDepartmentsByParent = (parentId) => {
  return allSubDepartments.value.filter(subDept => subDept.parentId === parentId)
}

// 获取科室数据
const fetchDepartments = async () => {
  try {
    const res = await getDepartmentList()
    if (res.code === 200) {
      allDepartments.value = res.data || []
      // 获取所有子科室
      await fetchAllSubDepartments()
    }
  } catch (error) {
    console.error('获取科室列表失败:', error)
  }
}

// 获取所有子科室数据
const fetchAllSubDepartments = async () => {
  try {
    const allSubs = []
    // 遍历所有主科室
    for (const dept of allDepartments.value) {
      const res = await getSubDepartmentList(dept.id)
      if (res.code === 200) {
        const subDepts = (res.data || []).map(subDept => ({
          id: subDept.id,
          parentId: dept.id,
          departmentName: subDept.departmentName,
          description: subDept.description,
          treatmentScope: subDept.treatmentScope,
          departmentFeatures: subDept.departmentFeatures,
          imagePath: subDept.imagePath
        }))
        allSubs.push(...subDepts)
      }
    }
    allSubDepartments.value = allSubs
  } catch (error) {
    console.error('获取子科室列表失败:', error)
  }
}

// 跳转到预约页面
const goToAppointment = () => {
  router.push('/appointment')
}

const goToAiTriage = () => {
  router.push('/ai-triage')
}

// 主科室点击处理
const handleDeptClick = (deptId) => {
  router.push({
    path: '/appointment',
    query: {
      deptId
    }
  })
}

// 子科室点击处理
const handleSubDeptClick = (parentId, subDeptId) => {
  router.push({
    path: '/appointment',
    query: {
      deptId: parentId,
      subDeptId
    }
  })
}

// 获取科室数据
onMounted(async () => {
  try {
    await fetchDepartments()
  } catch (error) {
    console.error('获取科室列表失败:', error)
  }
})
</script>

<style scoped>
.home-container {
  min-height: 100vh;
  background: transparent;
  padding: 12px;
  position: relative;
  overflow-x: hidden;
}

.home-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background:
    radial-gradient(circle at 25% 25%, rgb(var(--primary-200-rgb) / 0.18) 0%, transparent 55%),
    radial-gradient(circle at 75% 75%, rgb(var(--primary-300-rgb) / 0.12) 0%, transparent 55%),
    radial-gradient(circle at 50% 50%, rgb(var(--primary-100-rgb) / 0.12) 0%, transparent 70%);
  pointer-events: none;
  animation: backgroundPulse 10s ease-in-out infinite;
}

@keyframes backgroundPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

/* 轮播图样式 */
.banner-section {
  margin-bottom: 40px;
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.2);
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  z-index: 2;
  border: 2px solid rgba(59, 130, 246, 0.2);
}

.banner-item {
  height: 100%;
  background-size: cover;
  background-position: center;
  position: relative;
  overflow: hidden;
  transition: transform 0.6s ease;
}

.banner-item:hover {
  transform: scale(1.02);
}

.banner-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(to right, rgba(30, 64, 175, 0.8) 0%, rgba(37, 99, 235, 0.4) 50%, rgba(59, 130, 246, 0.1) 100%);
  transition: background 0.4s ease;
}

.banner-content {
  position: absolute;
  left: 60px;
  top: 50%;
  transform: translateY(-50%);
  color: #fff;
  max-width: 500px;
  z-index: 2;
}

.banner-content h2 {
  font-size: 36px;
  margin-bottom: 16px;
  font-weight: 700;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  animation: slideInLeft 1s ease-out;
  color: #ffffff;
}

.banner-content p {
  font-size: 18px;
  line-height: 1.6;
  margin-bottom: 24px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
  animation: slideInLeft 1s ease-out 0.2s both;
  color: rgb(var(--primary-50-rgb) / 0.95);
}

.banner-btn {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 30px;
  transition: all 0.4s ease;
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.3);
  animation: slideInLeft 1s ease-out 0.4s both;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  border: none;
  color: #ffffff;
}

.banner-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.4);
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
}

.ai-triage-section {
  margin-bottom: 28px;
  position: relative;
  z-index: 2;
}

.ai-triage-card {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(240px, 0.9fr);
  gap: 18px;
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.94) 0%, rgb(var(--primary-100-rgb) / 0.7) 100%);
  border: 1px solid rgb(var(--primary-300-rgb) / 0.32);
  box-shadow: var(--shadow-xl);
}

.ai-triage-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgb(var(--primary-600-rgb) / 0.12);
  color: var(--primary-700);
  font-size: 13px;
  font-weight: 700;
}

.ai-triage-content h2 {
  margin: 16px 0 10px;
  font-size: 30px;
  color: #1e40af;
  line-height: 1.25;
}

.ai-triage-content p {
  margin: 0;
  font-size: 15px;
  color: var(--neutral-700);
  line-height: 1.8;
}

.ai-triage-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.ai-triage-tip {
  height: 100%;
  padding: 20px;
  border-radius: 18px;
  background: rgb(var(--primary-500-rgb) / 0.08);
  border: 1px solid rgb(var(--primary-500-rgb) / 0.14);
}

.ai-triage-tip h3 {
  margin: 0 0 12px;
  font-size: 18px;
  color: #1e40af;
}

.ai-triage-tip ul {
  margin: 0;
  padding-left: 18px;
  color: var(--neutral-700);
  line-height: 1.9;
}

@keyframes slideInLeft {
  from {
    opacity: 0;
    transform: translateX(-50px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 科室信息样式 */
.department-section {
  margin-bottom: 60px;
  position: relative;
  z-index: 2;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 0 10px;
}

.section-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: #1e40af;
  margin: 0;
  position: relative;
  padding-left: 15px;
  text-shadow: 0 2px 4px rgba(30, 64, 175, 0.2);
}

.section-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 5px;
  height: 24px;
  background: linear-gradient(to bottom, #2563eb, #1d4ed8);
  border-radius: 3px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.3);
}

.appointment-btn {
  padding: 10px 20px;
  border-radius: 30px;
  font-weight: 600;
  transition: all 0.4s ease;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.2);
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #ffffff;
  border: 2px solid rgba(59, 130, 246, 0.3);
}

.appointment-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.3);
  background: linear-gradient(135deg, #1d4ed8, #1e40af);
  border-color: rgba(59, 130, 246, 0.5);
}

.nested-departments {
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 250, 252, 0.9));
  backdrop-filter: blur(15px);
  border-radius: 20px;
  padding: 35px;
  box-shadow: 0 12px 40px rgba(37, 99, 235, 0.1);
  border: 2px solid rgba(59, 130, 246, 0.2);
  position: relative;
  overflow: hidden;
}

.nested-departments::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 20%, rgba(59, 130, 246, 0.05) 0%, transparent 50%),
              radial-gradient(circle at 80% 80%, rgba(37, 99, 235, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

.department-group {
  margin-bottom: 40px;
  position: relative;
}

.department-group:last-child {
  margin-bottom: 0;
}

/* 主科室卡片样式 */
.main-department-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 18px;
  padding: 32px;
  box-shadow: 0 8px 32px rgba(37, 99, 235, 0.08);
  border: 2px solid rgba(59, 130, 246, 0.15);
  position: relative;
  overflow: hidden;
  transition: all 0.5s ease;
}

.main-department-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 6px;
  background: linear-gradient(to right, #2563eb, #1d4ed8, #1e40af, #3b82f6);
  background-size: 300% 100%;
  animation: lightGradientMove 4s ease-in-out infinite;
}

@keyframes lightGradientMove {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

.main-department-card:hover {
  transform: translateY(-10px);
  box-shadow: 0 16px 48px rgba(37, 99, 235, 0.15);
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border-color: rgba(59, 130, 246, 0.25);
}

.main-dept-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid rgba(59, 130, 246, 0.15);
}

.dept-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dept-title {
  font-size: 24px;
  font-weight: 700;
  color: #1e40af;
  margin: 0;
  position: relative;
  text-shadow: 0 2px 4px rgba(30, 64, 175, 0.1);
}

.dept-tag {
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.main-dept-btn {
  border-radius: 25px;
  padding: 8px 20px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.main-dept-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.4);
}

.main-dept-content {
  margin-bottom: 25px;
}

.dept-description {
  font-size: 16px;
  line-height: 1.8;
  color: #475569;
  margin: 0;
  padding: 15px 20px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 12px;
  border-left: 4px solid #2563eb;
}

/* 子科室容器样式 */
.sub-departments-container {
  margin-top: 28px;
  padding: 24px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.8), rgba(240, 249, 255, 0.6));
  border-radius: 16px;
  border: 2px solid rgba(59, 130, 246, 0.1);
  position: relative;
  overflow: hidden;
}

.sub-departments-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 30% 30%, rgba(59, 130, 246, 0.03) 0%, transparent 60%);
  pointer-events: none;
}

.sub-dept-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 3px solid rgba(59, 130, 246, 0.2);
  position: relative;
}

.sub-dept-header::after {
  content: '';
  position: absolute;
  bottom: -3px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(to right, #2563eb, #1d4ed8);
  border-radius: 2px;
}

.sub-dept-header h4 {
  font-size: 20px;
  font-weight: 600;
  color: #1e40af;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  text-shadow: 0 1px 3px rgba(30, 64, 175, 0.1);
}

.sub-dept-count {
  background: linear-gradient(135deg, #16a34a, #15803d);
  color: white;
  padding: 4px 12px;
  border-radius: 15px;
  font-size: 12px;
  font-weight: 600;
}

.sub-departments-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

/* 子科室卡片样式 */
.sub-department-item {
  position: relative;
}

.sub-dept-card {
  background: linear-gradient(135deg, #ffffff 0%, #fafcff 100%);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.06);
  border: 2px solid rgba(59, 130, 246, 0.1);
  transition: all 0.4s ease;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
}

.sub-dept-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  background: linear-gradient(to right, #2563eb, #1d4ed8);
  transform: scaleX(0);
  transition: transform 0.4s ease;
}

.sub-dept-card:hover::before {
  transform: scaleX(1);
}

.sub-dept-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.12);
  background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
  border-color: rgba(59, 130, 246, 0.2);
}

.sub-dept-image {
  position: relative;
  height: 150px;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
  background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

.animated-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  transition: all 0.5s ease;
  filter: brightness(1) saturate(1);
  padding: 8px;
  background: transparent;
  margin-left: 0;
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(37, 99, 235, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
}

.image-overlay i {
  font-size: 24px;
  color: white;
}

.sub-dept-image:hover .animated-image {
  transform: scale(1.05);
  filter: brightness(1.1) saturate(1.2);
  padding: 6px;
}

.sub-dept-image:hover .image-overlay {
  opacity: 1;
}

.sub-dept-info {
  padding: 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.sub-dept-title-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.sub-dept-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e40af;
  margin: 0;
  text-shadow: 0 1px 2px rgba(30, 64, 175, 0.1);
}

.sub-dept-details {
  flex: 1;
  margin-bottom: 15px;
}

.detail-item {
  margin-bottom: 12px;
}

.detail-item:last-child {
  margin-bottom: 0;
}

.detail-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #16a34a;
  font-size: 14px;
  margin-bottom: 5px;
}

.detail-value {
  color: #475569;
  font-size: 14px;
  line-height: 1.6;
  margin: 0;
  padding-left: 20px;
}

.sub-dept-actions {
  text-align: center;
}

.sub-dept-btn {
  border-radius: 20px;
  padding: 6px 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  width: 100%;
}

.sub-dept-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(22, 163, 74, 0.3);
}

/* 无子科室提示 */
.no-sub-departments {
  text-align: center;
  color: #64748b;
  font-style: italic;
  padding: 24px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.5), rgba(240, 249, 255, 0.3));
  border-radius: 12px;
  border: 2px dashed rgba(59, 130, 246, 0.2);
  font-size: 15px;
  margin-top: 20px;
}

.no-sub-departments i {
  margin-right: 8px;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .home-container {
    padding: 15px;
  }

  .ai-triage-card {
    grid-template-columns: 1fr;
    padding: 22px;
  }

  .ai-triage-content h2 {
    font-size: 24px;
  }
  
  .nested-departments {
    padding: 20px;
  }
  
  .main-department-card {
    padding: 20px;
  }
  
  .main-dept-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .sub-departments-grid {
    grid-template-columns: 1fr;
  }
  
  .sub-dept-image {
    height: 120px;
  }
  
  .animated-image {
    padding: 6px;
  }
  
  .dept-title {
    font-size: 20px;
  }
  
  .banner-content {
    left: 30px;
    max-width: 400px;
  }
  
  .banner-content h2 {
    font-size: 28px;
  }
  
  .banner-content p {
    font-size: 16px;
  }
}
</style>
