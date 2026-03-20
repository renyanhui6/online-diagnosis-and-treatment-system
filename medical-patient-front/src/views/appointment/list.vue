<template>
  <div class="appointment-list-container">
    <el-card class="appointment-list-card">
      <template #header>
        <div class="card-header">
          <h2>我的预约</h2>
          <el-button type="primary" @click="goToAppointment">预约挂号</el-button>
        </div>
      </template>
      
      <!-- 提示信息 -->
      <el-alert
        title="预约说明"
        type="info"
        description="预约页面用于查看预约状态并进入在线问诊。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
      
      <!-- 筛选条件 -->
      <div class="filter-container">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="预约日期">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 预约列表 -->
      <el-table
        v-loading="loading"
        :data="appointmentList"
        style="width: 100%"
        border
        stripe
      >
        <el-table-column prop="id" label="预约编号" width="120" />
        <el-table-column prop="departmentName" label="科室" width="150" />
        <el-table-column prop="doctorName" label="医生" width="120" />
        <el-table-column prop="patientName" label="就诊人" width="100" />
        <el-table-column prop="scheduleDate" label="预约日期" width="120">
          <template #default="{ row }">
            {{ formatDate(row.scheduleDate) }}
          </template>
        </el-table-column>
        <el-table-column prop="timePeriod" label="时间段" width="100">
          <template #default="{ row }">
            <el-tag :type="row.timePeriod === '上午' ? 'primary' : 'success'" size="small">
              {{ row.timePeriod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="registrationStatus" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.registrationStatus)">
              {{ getStatusText(row.registrationStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button 
                v-if="row.registrationStatus === APPOINTMENT_STATUS.CONSULTING"
                type="primary" 
                size="small" 
                @click="enterConsultation(row)"
              >
                进入问诊
              </el-button>
              <template v-if="row.registrationStatus === APPOINTMENT_STATUS.WAITING_CONFIRM">
                <el-button
                  type="success"
                  size="small"
                  @click="handleConsultationResponse(row, 'accept')"
                  :loading="row.consultationLoading"
                >
                  接受问诊
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="handleConsultationResponse(row, 'reject')"
                  :loading="row.consultationLoading"
                >
                  拒绝
                </el-button>
              </template>
              <el-button 
                v-if="canResumeAppointment(row.registrationStatus)"
                type="primary" 
                size="small" 
                @click="handleResumeAppointment(row)"
                :loading="row.resumeLoading"
              >
                恢复预约
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
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
    </el-card>
    

    

    

    

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserAppointmentOrders, changeStatusToResumed } from '../../api/appointment'
import { getAppointmentStatusText, getAppointmentStatusType, APPOINTMENT_STATUS, canResumeAppointment } from '../../utils'
import { getRoomStatus, respondToConsultation } from '../../api/chat'
import UserStorage from '../../utils/userStorage'

const router = useRouter()

// 筛选表单
const filterForm = reactive({
  dateRange: []
})

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 预约列表数据
const appointmentList = ref([])
const loading = ref(false)





// 获取预约状态文本
const getStatusText = (status) => {
  return getAppointmentStatusText(status)
}

// 获取预约状态类型（用于标签颜色）
const getStatusType = (status) => {
  return getAppointmentStatusType(status)
}

// 跳转到预约挂号页面
const goToAppointment = () => {
  router.push('/appointment')
}

// 处理筛选
const handleFilter = () => {
  pagination.currentPage = 1
  fetchAppointmentList()
}

// 重置筛选
const resetFilter = () => {
  filterForm.dateRange = []
  handleFilter()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchAppointmentList()
}

// 处理每页条数变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchAppointmentList()
}

// 进入问诊（问诊中状态）
const enterConsultation = async (row) => {
  try {
    // 通过挂号ID查询房间信息
    const res = await getRoomStatus(row.id)
    if (res.code === 200 && res.data) {
      const roomId = res.data.roomId || res.data.id
      if (roomId) {
        router.push(`/appointment/chat/${row.id}`)
        return
      }
    }
    ElMessage.error('未找到正在进行中的房间，无法进入问诊')
  } catch (e) {
    console.error('进入问诊失败:', e)
    ElMessage.error('进入问诊失败，请稍后重试')
  }
}

const handleConsultationResponse = async (row, response) => {
  try {
    row.consultationLoading = true

    const res = await respondToConsultation({
      registrationId: row.id,
      response
    })

    if (res.code !== 200) {
      ElMessage.error(res.message || '处理问诊请求失败')
      return
    }

    if (response === 'accept') {
      ElMessage.success('已同意开始问诊')
      await fetchAppointmentList()
      await enterConsultation({
        ...row,
        registrationStatus: APPOINTMENT_STATUS.CONSULTING
      })
      return
    }

    ElMessage.info('已拒绝问诊请求')
    await fetchAppointmentList()
  } catch (error) {
    console.error('处理问诊请求失败:', error)
    ElMessage.error('处理问诊请求失败，请稍后重试')
  } finally {
    row.consultationLoading = false
  }
}

// 恢复预约
const handleResumeAppointment = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定要恢复这个预约吗？恢复后预约状态将变为已回归。',
      '确认恢复',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 设置加载状态
    row.resumeLoading = true
    
    // 调用恢复预约API
    const res = await changeStatusToResumed(row.id)
    
    if (res.code === 200) {
      ElMessage.success('预约恢复成功')
      // 更新本地状态
      row.registrationStatus = APPOINTMENT_STATUS.RESUMED
      // 刷新列表
      await fetchAppointmentList()
    } else {
      ElMessage.error(res.message || '恢复预约失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('恢复预约失败:', error)
      ElMessage.error('恢复预约失败，请稍后重试')
    }
  } finally {
    row.resumeLoading = false
  }
}





// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`
}

// 格式化日期时间
const formatDateTime = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return `${date.getFullYear()}-${(date.getMonth() + 1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

// 获取预约列表
const fetchAppointmentList = async () => {
  console.log('=== 开始获取预约列表 ===')
  
  // 首先检查登录状态
  if (!UserStorage.isLoggedIn()) {
    console.error('用户未登录')
    ElMessage.error('请先登录')
    router.push('/login')
    return
  }
  
  console.log('用户已登录，检查用户信息...')
  console.log('Token:', UserStorage.getToken())
  console.log('用户信息:', UserStorage.getUserInfo())
  
  // 检查用户信息是否存在且有效
  const userId = UserStorage.getUserId()
  
  if (!UserStorage.isUserInfoValid() || !userId) {
    console.log("用户信息有效性:", UserStorage.isUserInfoValid())
    console.log("用户ID:", userId)
    ElMessage.error('用户信息失效，请重新登录')
    UserStorage.clearUserData()
    router.push('/login')
    return
  }
  
  console.log('=== 用户信息验证通过，开始获取预约列表 ===')
  console.log('使用的用户ID:', userId)

  loading.value = true
  
  try {
    console.log('发送请求获取用户所有预约订单')
    
    // 准备请求参数
    const requestParams = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    
    // 添加日期范围参数
    if (filterForm.dateRange && filterForm.dateRange.length === 2) {
      requestParams.startDate = filterForm.dateRange[0]
      requestParams.endDate = filterForm.dateRange[1]
    }
    
    console.log('请求参数:', requestParams)
    console.log('当前分页设置 - 页码:', pagination.currentPage, '每页条数:', pagination.pageSize)
    const res = await getUserAppointmentOrders(requestParams)
    console.log('API响应:', res)
    
    if (res.code === 200) {
      // 检查返回数据的结构
      console.log('返回数据结构:', res.data)
      console.log('返回数据类型:', typeof res.data)
      console.log('是否为数组:', Array.isArray(res.data))
      
      if (res.data) {
        console.log('res.data的所有属性:', Object.keys(res.data))
        if (res.data.list) console.log('res.data.list长度:', res.data.list.length)
        if (res.data.records) console.log('res.data.records长度:', res.data.records.length)
        if (res.data.total) console.log('res.data.total:', res.data.total)
      }
      
      // 尝试不同的数据字段
      if (res.data && Array.isArray(res.data)) {
        // 如果直接返回数组
        appointmentList.value = res.data
        pagination.total = res.data.length
        console.log('使用直接数组数据')
      } else if (res.data && res.data.list) {
        // 如果有list字段
        appointmentList.value = res.data.list
        pagination.total = res.data.total || res.data.list.length
        console.log('使用list字段数据')
      } else if (res.data && res.data.records) {
        // 如果有records字段
        appointmentList.value = res.data.records
        pagination.total = res.data.total || res.data.records.length
        console.log('使用records字段数据')
      } else if (res.data && res.data.data) {
        // 如果有嵌套的data字段
        appointmentList.value = res.data.data
        pagination.total = res.data.total || res.data.data.length
        console.log('使用嵌套data字段数据')
      } else {
        // 默认情况
        appointmentList.value = []
        pagination.total = 0
        console.log('使用默认空数据')
      }
      
      console.log('预约列表:', appointmentList.value)
      console.log('预约列表获取成功，数量:', appointmentList.value.length)
      console.log('分页总数:', pagination.total)
      console.log('当前页码:', pagination.currentPage)
      console.log('每页条数:', pagination.pageSize)
      
      // 检查表格数据绑定
      console.log('表格数据长度:', appointmentList.value.length)
      if (appointmentList.value.length > 0) {
        console.log('第一条记录:', appointmentList.value[0])
        console.log('第一条记录的字段:', Object.keys(appointmentList.value[0]))
        
        // 检查关键字段是否存在
        const firstRecord = appointmentList.value[0]
        console.log('id字段:', firstRecord.id)
        console.log('departmentName字段:', firstRecord.departmentName)
        console.log('doctorName字段:', firstRecord.doctorName)
        console.log('registrationStatus字段:', firstRecord.registrationStatus)
      }
    } else {
      ElMessage.error(res.message || '获取预约列表失败')
    }
  } catch (error) {
    console.error('获取预约列表失败:', error)
    ElMessage.error('获取预约列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  fetchAppointmentList()
})
</script>

<style scoped>
.appointment-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px;
}

.appointment-list-card {
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
  font-weight: 600;
  color: var(--neutral-800);
}

.filter-container {
  margin-bottom: 20px;
}

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-buttons .el-button {
  margin: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 详情页面中的操作按钮 */
.detail-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

</style>
