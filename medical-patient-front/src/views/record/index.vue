<template>
  <div class="record-container">
    <el-card class="record-card">
      <template #header>
        <div class="card-header">
          <h2>就诊记录</h2>
          <div class="header-actions">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
            />
            <el-button type="primary" @click="handleFilterChange">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </div>
        </div>
      </template>
      
      <div v-loading="loading" class="record-list">
        <el-empty v-if="recordList.length === 0" description="暂无就诊记录" />
        <el-timeline v-else>
          <el-timeline-item
            v-for="record in recordList"
            :key="record.medicalRecordId"
            :timestamp="formatDateTime(record.createTime)"
            :type="getTimelineItemType(record)"
            :hollow="record.isPurchasable !== 1"
            placement="top"
          >
            <el-card class="record-item-card">
              <div class="record-item-header">
                <div class="record-item-title">
                  <span class="patient-name">{{ record.patientName }}</span>
                  <span class="doctor-name">- {{ record.doctorName }}</span>
                </div>
                <div class="record-item-status">
                  <el-tag :type="record.isPurchasable === 0 ? 'success' : 'info'" size="small">
                    {{ record.isPurchasable === 0 ? '可购买药品' : '不可购买' }}
                  </el-tag>
                </div>
              </div>
              
              <div class="record-item-content">
                <div class="doctor-description" v-if="record.doctorDescription">
                  <div class="description-label">医生诊断：</div>
                  <div class="description-content">{{ record.doctorDescription }}</div>
                </div>
                
                <div class="record-info">
                  <span class="record-id">记录编号：{{ record.medicalRecordId }}</span>
                </div>
              </div>
              
              <div class="record-item-footer">
                <el-button type="primary" @click="viewRecordDetail(record)">查看详情</el-button>
                <el-button 
                  v-if="record.isPurchasable === 0" 
                  type="success" 
                  @click="purchaseMedicine(record)"
                >
                  购买药品
                </el-button>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
        
        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 30, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="pagination.total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 就诊记录详情对话框 -->
    <el-dialog v-model="recordDetailVisible" title="就诊记录详情" width="700px">
      <div v-if="currentRecord" class="record-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="就诊人">{{ currentRecord.patientName }}</el-descriptions-item>
          <el-descriptions-item label="就诊医生">{{ currentRecord.doctorName }}</el-descriptions-item>
          <el-descriptions-item label="记录编号">{{ currentRecord.medicalRecordId }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentRecord.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="药品购买状态">
            <el-tag :type="currentRecord.isPurchasable === 0 ? 'success' : 'info'">
              {{ currentRecord.isPurchasable === 0 ? '可购买药品' : '不可购买' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider content-position="left">医生诊断</el-divider>
        
        <div class="doctor-description-detail">
          <div class="description-content">
            {{ currentRecord.doctorDescription || '暂无诊断记录' }}
          </div>
        </div>
        
        <div class="record-detail-actions">
          <el-button 
            v-if="currentRecord.isPurchasable === 0" 
            type="success" 
            @click="purchaseMedicine(currentRecord)"
          >
            购买药品
          </el-button>
          <el-button type="primary" @click="recordDetailVisible = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMedicalRecordList } from '../../api/record'
import UserStorage from '../../utils/userStorage'

const router = useRouter()

// 过滤条件
const selectedDate = ref(null)

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 就诊记录列表
const recordList = ref([])
const loading = ref(false)

// 就诊记录详情
const currentRecord = ref(null)
const recordDetailVisible = ref(false)

// 处理过滤条件变化
const handleFilterChange = () => {
  pagination.currentPage = 1
  fetchRecordList()
}

// 处理日期变化
const handleDateChange = () => {
  // 日期变化时不自动查询，需要点击查询按钮
}

// 重置筛选
const resetFilter = () => {
  selectedDate.value = null
  pagination.currentPage = 1
  fetchRecordList()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchRecordList()
}

// 处理每页条数变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchRecordList()
}

// 获取时间线项目类型
const getTimelineItemType = (record) => {
  return record.isPurchasable === 0 ? 'success' : 'info'
}

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 查看就诊记录详情
const viewRecordDetail = (record) => {
  currentRecord.value = record
  recordDetailVisible.value = true
}

// 导入接口函数
import { createByPrescription } from '../../api/medicine'
import { ElLoading } from 'element-plus'

// 创建订单ID
const createdOrderId = ref(null)

// 购买药品（通过处方直接创建订单）
const purchaseMedicine = async (record) => {
  // 显示加载状态
  const loading = ElLoading.service({
    lock: true,
    text: '正在创建订单...',
    background: 'rgba(0, 0, 0, 0.7)'
  })

  try {
    // 调用处方创建订单接口
    const res = await createByPrescription(record.medicalRecordId)
    
    if (res.code === 200) {
      ElMessage.success('订单创建成功！即将跳转到支付页面')
      // 保存订单ID，用于后续支付
      createdOrderId.value = res.data
      // 跳转到支付页面或显示支付弹窗
      router.push({
  path: '/payment/medicine',
  query: { orderId: res.data }
})
    } else {
      ElMessage.error(res.message || '创建订单失败，请重试')
    }
  } catch (error) {
    console.error('通过处方创建订单失败:', error)
    ElMessage.error('网络错误，创建订单失败')
  } finally {
    // 关闭加载状态
    loading.close()
  }
}

// 获取就诊记录列表
const fetchRecordList = async () => {
  console.log('=== 开始获取就诊记录列表 ===')
  
  // 检查登录状态
  if (!UserStorage.isLoggedIn()) {
    console.error('用户未登录')
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  
  const userId = UserStorage.getUserId()
  if (!UserStorage.isUserInfoValid() || !userId) {
    console.log("用户信息有效性:", UserStorage.isUserInfoValid())
    console.log("用户ID:", userId)
    ElMessage.error('用户信息失效，请重新登录')
    UserStorage.clearUserData()
    router.push('/login')
    return
  }
  
  console.log('=== 用户信息验证通过，开始获取就诊记录列表 ===')
  console.log('使用的用户ID:', userId)

  loading.value = true
  
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    
    // 添加日期筛选条件
    if (selectedDate.value) {
      params.createTime = selectedDate.value
    }
    
    console.log('发送请求参数:', params)
    const res = await getMedicalRecordList(params)
    console.log('API响应:', res)
    
    if (res.code === 200) {
      recordList.value = res.data.list || res.data.records || []
      pagination.total = res.data.total || 0
      console.log('就诊记录列表获取成功，数量:', recordList.value.length)
    } else {
      ElMessage.error(res.message || '获取就诊记录列表失败')
    }
  } catch (error) {
    console.error('获取就诊记录列表失败:', error)
    ElMessage.error('获取就诊记录列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  fetchRecordList()
})
</script>

<style scoped>
.record-container {
  max-width: 1200px;
  margin: 0 auto;
}

.record-card {
  margin-bottom: 20px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(135, 206, 250, 0.2);
  box-shadow: 0 4px 20px rgba(135, 206, 250, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #2c5aa0;
  text-shadow: 0 1px 2px rgba(135, 206, 250, 0.1);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.record-list {
  min-height: 500px;
  padding: 10px 0;
}

.record-item-card {
  margin-bottom: 10px;
  background: linear-gradient(135deg, #ffffff 0%, #f8fbff 100%);
  border: 1px solid rgba(135, 206, 250, 0.2);
  box-shadow: 0 2px 12px rgba(135, 206, 250, 0.1);
  transition: all 0.3s ease;
}

.record-item-card:hover {
  box-shadow: 0 4px 20px rgba(135, 206, 250, 0.2);
  background: linear-gradient(135deg, #f0f8ff 0%, #e6f3ff 100%);
}

.record-item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.record-item-title {
  display: flex;
  align-items: center;
  gap: 5px;
}

.patient-name {
  font-weight: 600;
  font-size: 16px;
  color: #2c5aa0;
}

.doctor-name {
  color: #4a6fa5;
  font-size: 14px;
}

.record-item-status {
  display: flex;
  align-items: center;
  gap: 5px;
}

.record-item-content {
  margin-bottom: 15px;
}

.doctor-description {
  margin-bottom: 10px;
}

.description-label {
  font-weight: 500;
  color: #2c5aa0;
  margin-bottom: 5px;
}

.description-content {
  color: #4a6fa5;
  line-height: 1.6;
}

.record-info {
  margin-top: 10px;
}

.record-id {
  font-weight: 500;
  color: #2c5aa0;
}

.doctor-description-detail {
  background: #f8fbff;
  padding: 15px;
  border-radius: 8px;
  border-left: 4px solid #0ea5e9;
  margin: 15px 0;
}

.doctor-description-detail .description-content {
  color: #334155;
  line-height: 1.8;
  font-size: 14px;
}

.record-detail-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.record-item-footer {
  display: flex;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 就诊记录详情样式 */
.record-detail {
  padding: 10px;
}
</style>