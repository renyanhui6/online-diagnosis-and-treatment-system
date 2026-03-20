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
                    {{ record.isPurchasable === 0 ? '已开具处方' : '无处方' }}
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
          <el-descriptions-item label="处方状态">
            <el-tag :type="currentRecord.isPurchasable === 0 ? 'success' : 'info'">
              {{ currentRecord.isPurchasable === 0 ? '已开具处方' : '无处方' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <el-divider content-position="left">医生诊断</el-divider>
        
        <div class="doctor-description-detail">
          <div class="description-content">
            {{ currentRecord.doctorDescription || '暂无诊断记录' }}
          </div>
        </div>

        <template v-if="currentRecord.isPurchasable === 0">
          <el-divider content-position="left">处方明细</el-divider>

          <div v-if="prescriptionLoading" class="prescription-loading">
            <el-skeleton :rows="3" animated />
          </div>
          <el-empty v-else-if="prescriptionList.length === 0" description="暂无处方明细" />
          <el-table v-else :data="prescriptionList" border>
            <el-table-column prop="drugName" label="药品名称" min-width="180" />
            <el-table-column prop="drugQuantity" label="数量" width="100" />
            <el-table-column prop="minimumSalesUnit" label="单位" width="100" />
            <el-table-column prop="price" label="单价" width="120">
              <template #default="{ row }">
                {{ formatPrice(row.price) }}
              </template>
            </el-table-column>
            <el-table-column prop="isPrescription" label="类型" width="120" />
          </el-table>
        </template>
        
        <div class="record-detail-actions">
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
import { getMedicalRecordList, getPrescriptionInfoByMedicalRecordId } from '../../api/record'
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
const prescriptionList = ref([])
const prescriptionLoading = ref(false)

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

const formatPrice = (price) => {
  if (price === null || price === undefined || price === '') return '-'
  return Number(price).toFixed(2)
}

// 查看就诊记录详情
const viewRecordDetail = async (record) => {
  currentRecord.value = record
  recordDetailVisible.value = true
  prescriptionList.value = []

  if (record.isPurchasable !== 0 || !record.medicalRecordId) {
    return
  }

  prescriptionLoading.value = true
  try {
    const res = await getPrescriptionInfoByMedicalRecordId(record.medicalRecordId)
    if (res.code === 200) {
      prescriptionList.value = Array.isArray(res.data) ? res.data : []
      return
    }
    ElMessage.error(res.message || '获取处方明细失败')
  } catch (error) {
    console.error('获取处方明细失败:', error)
    ElMessage.error('获取处方明细失败，请稍后重试')
  } finally {
    prescriptionLoading.value = false
  }
}

// 导入接口函数
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
  padding: 12px;
}

.record-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--neutral-800);
}

.header-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

.record-list {
  min-height: 520px;
  padding: 6px 0;
}

.record-item-card {
  margin-bottom: 10px;
}

.record-item-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.record-item-title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 6px;
}

.patient-name {
  font-weight: 700;
  font-size: 16px;
  color: var(--neutral-800);
}

.doctor-name {
  color: var(--neutral-600);
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
  font-weight: 600;
  color: var(--neutral-700);
  margin-bottom: 5px;
}

.description-content {
  color: var(--neutral-600);
  line-height: 1.6;
}

.record-info {
  margin-top: 10px;
}

.record-id {
  font-weight: 600;
  color: var(--neutral-700);
}

.doctor-description-detail {
  background: rgb(var(--primary-50-rgb) / 0.6);
  padding: 15px;
  border-radius: 10px;
  border: 1px solid rgb(var(--primary-200-rgb) / 0.35);
  border-left: 4px solid var(--primary-500);
  margin: 15px 0;
}

.doctor-description-detail .description-content {
  color: var(--neutral-700);
  line-height: 1.8;
  font-size: 14px;
}

.record-detail-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
  margin-top: 20px;
}

.record-item-footer {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 就诊记录详情样式 */
.record-detail {
  padding: 10px;
}
</style>
