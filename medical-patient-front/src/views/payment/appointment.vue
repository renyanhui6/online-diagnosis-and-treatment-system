<template>
  <div class="appointment-payment-container">
    <el-card class="payment-card">
      <template #header>
        <div class="card-header">
          <h2>挂号支付记录</h2>
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
      
      <!-- 提示信息 -->
      <el-alert
        title="支付说明"
        type="info"
        description="在此页面可以完成预约的支付、取消和退款操作。预约成功后请及时完成支付。"
        show-icon
        :closable="false"
        style="margin-bottom: 20px"
      />
      
      <div v-loading="loading" class="payment-list">
        <el-empty v-if="paymentList.length === 0" description="暂无支付记录" />
        <el-table v-else :data="paymentList" style="width: 100%" border>
          <el-table-column prop="id" label="订单编号" width="120" />
          <el-table-column prop="payerId" label="支付人ID" width="120" />
          <el-table-column prop="paymentStatus" label="支付状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.paymentStatus === 0" type="warning">待支付</el-tag>
              <el-tag v-else-if="row.paymentStatus === 1" type="success">已支付</el-tag>
              <el-tag v-else-if="row.paymentStatus === 2" type="info">已退款</el-tag>
              <el-tag v-else type="info">未知状态</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentAmount" label="支付金额" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ (row.paymentAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="paymentTime" label="支付时间" width="180">
            <template #default="{ row }">
              <span v-if="row.paymentTime">{{ row.paymentTime }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="支付方式" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.paymentMethod === 1" type="success">微信支付</el-tag>
              <el-tag v-else-if="row.paymentMethod === 2" type="primary">支付宝</el-tag>
              <el-tag v-else-if="row.paymentMethod === 3" type="warning">银行卡</el-tag>
              <el-tag v-else type="info">其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button v-if="row.paymentStatus === 0" type="primary" size="small" @click="handlePay(row)">支付</el-button>
                <el-button v-if="row.paymentStatus === 1" type="danger" size="small" @click="handleRefund(row)">退款</el-button>
                <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
                <el-button v-if="row.paymentStatus === 0" type="warning" size="small" @click="handleCancel(row)">取消</el-button>
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
      </div>
    </el-card>
    
    <!-- 订单详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="挂号详情" width="600px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="支付人ID">{{ currentOrder.payerId }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag v-if="currentOrder.paymentStatus === 0" type="warning">待支付</el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 1" type="success">已支付</el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 2" type="info">已退款</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付金额">
            <span class="price">¥{{ (currentOrder.paymentAmount || 0).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付时间">
            <span v-if="currentOrder.paymentTime">{{ currentOrder.paymentTime }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            <el-tag v-if="currentOrder.paymentMethod === 1" type="success">微信支付</el-tag>
            <el-tag v-else-if="currentOrder.paymentMethod === 2" type="primary">支付宝</el-tag>
            <el-tag v-else-if="currentOrder.paymentMethod === 3" type="warning">银行卡</el-tag>
            <el-tag v-else type="info">其他</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentOrder.createTime }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="order-actions">
          <el-button v-if="currentOrder.paymentStatus === 0" type="primary" @click="handlePay(currentOrder)">立即支付</el-button>
          <el-button v-if="currentOrder.paymentStatus === 1" type="danger" @click="handleRefund(currentOrder)">申请退款</el-button>
        </div>
      </div>
    </el-dialog>
    
    <!-- 支付对话框 -->
    <el-dialog v-model="paymentDialogVisible" title="订单支付" width="400px" center>
      <div class="payment-dialog-content">
        <el-result icon="info" title="确认支付" sub-title="请选择支付方式完成支付">
          <template #extra>
            <div class="payment-method-select">
              <el-radio-group v-model="paymentMethod">
                <el-radio :label="1">微信支付</el-radio>
                <el-radio :label="2">支付宝</el-radio>
                <el-radio :label="3">银行卡</el-radio>
              </el-radio-group>
            </div>
            <div class="payment-amount">
              <p>支付金额: <span class="price">¥{{ (paymentAmount || 0).toFixed(2) }}</span></p>
            </div>
            <el-button type="primary" @click="confirmPayment" :loading="paying">确认支付</el-button>
            <el-button @click="paymentDialogVisible = false">取消</el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>
    
    <!-- 退款对话框 -->
    <el-dialog v-model="refundDialogVisible" title="申请退款" width="500px">
      <div class="refund-dialog-content">
        <el-alert
          title="退款说明"
          type="warning"
          description="请填写退款原因，审核通过后将退款至原支付账户，预计1-3个工作日到账。"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <el-form :model="refundForm" label-width="100px">
          <el-form-item label="订单编号">
            <span>{{ refundForm.orderId }}</span>
          </el-form-item>
          <el-form-item label="退款金额">
            <span class="price">¥{{ (refundForm.amount || 0).toFixed(2) }}</span>
          </el-form-item>
          <el-form-item label="退款原因" prop="reason">
            <el-select v-model="refundForm.reasonType" placeholder="请选择退款原因" style="width: 100%">
              <el-option label="临时有事，无法就诊" value="busy" />
              <el-option label="选错科室/医生" value="wrong_selection" />
              <el-option label="选错就诊时间" value="wrong_time" />
              <el-option label="重复预约" value="duplicate" />
              <el-option label="其他原因" value="other" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细说明">
            <el-input v-model="refundForm.reasonDetail" type="textarea" :rows="3" placeholder="请详细描述退款原因（选填）" />
          </el-form-item>
        </el-form>
        
        <div class="dialog-footer">
          <el-button @click="refundDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRefund" :loading="refunding">提交申请</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import UserStorage from '../../utils/userStorage'
import { getAppointmentOrder, finishAppointmentPayment, refundAppointment, getPaymentOrders, cancelAppointment } from '../../api/appointment'

// 过滤条件
const selectedDate = ref(null)
const searchAppointmentId = ref('')

// 路由
const router = useRouter()

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
})

// 支付记录列表
const paymentList = ref([])
const loading = ref(false)

// 订单详情
const currentOrder = ref(null)
const detailDialogVisible = ref(false)

// 支付相关
const paymentDialogVisible = ref(false)
const paymentMethod = ref(1)
const paymentAmount = ref(0)
const payingOrderId = ref('')
const paying = ref(false)

// 退款相关
const refundDialogVisible = ref(false)
const refundForm = reactive({
  orderId: '',
  amount: 0,
  reasonType: '',
  reasonDetail: ''
})
const refunding = ref(false)

// 处理过滤条件变化
const handleFilterChange = () => {
  pagination.currentPage = 1
  fetchPaymentList()
}

// 处理日期变化
const handleDateChange = () => {
  // 日期变化时不自动查询，需要点击查询按钮
}

// 重置筛选
const resetFilter = () => {
  selectedDate.value = null
  searchAppointmentId.value = ''
  pagination.currentPage = 1
  fetchPaymentList()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchPaymentList()
}

// 处理每页条数变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchPaymentList()
}

// 查看订单详情
const viewDetail = async (order) => {
  try {
    // 获取最新的订单详情
    const orderDetail = await fetchOrderDetail(order.id)
    if (orderDetail) {
      currentOrder.value = orderDetail
      detailDialogVisible.value = true
    } else {
      // 如果获取详情失败，使用当前订单信息
      currentOrder.value = order
      detailDialogVisible.value = true
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    // 使用当前订单信息作为备选
    currentOrder.value = order
    detailDialogVisible.value = true
  }
}

// 处理支付
const handlePay = (order) => {
  console.log('处理支付订单:', order)
  payingOrderId.value = order.id
  paymentAmount.value = order.paymentAmount || 0
  paymentMethod.value = order.paymentMethod || 1
  paymentDialogVisible.value = true
}

// 确认支付
const confirmPayment = async () => {
  if (!payingOrderId.value) {
    ElMessage.error('订单信息有误，请刷新后重试')
    return
  }
  
  paying.value = true
  
  try {
    const res = await finishAppointmentPayment({
      id: payingOrderId.value,
      paymentMethod: paymentMethod.value
    })
    
    if (res.code === 200) {
      ElMessage.success('支付成功')
      paymentDialogVisible.value = false
      await fetchPaymentList()
    } else {
      ElMessage.error(res.message || '支付失败')
    }
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败，请稍后重试')
  } finally {
    paying.value = false
  }
}

// 处理退款
const handleRefund = (order) => {
  console.log('处理退款订单:', order)
  refundForm.orderId = order.id
  refundForm.amount = order.paymentAmount || 0
  refundForm.reasonType = ''
  refundForm.reasonDetail = ''
  refundDialogVisible.value = true
}

// 处理取消预约
const handleCancel = async (order) => {
  try {
    await ElMessageBox.confirm(
      '确定要取消这个预约吗？取消后无法恢复。',
      '确认取消',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 添加取消中的提示
    const loadingMsg = ElMessage({
      message: '正在处理取消请求...',
      type: 'info',
      duration: 0
    })
    
    try {
      const res = await cancelAppointment(order.id)
      // 关闭加载提示
      loadingMsg.close()
      
      if (res.code === 200) {
        ElMessage.success('预约取消成功')
        await fetchPaymentList()
      } else if (res.code === 201) {
        // 201错误码特殊处理
        ElMessage({
          message: '无法取消预约，可能已超过可取消时间',
          type: 'warning',
          duration: 5000
        })
      } else {
        ElMessage.error(res.message || '取消预约失败')
      }
    } catch (error) {
      // 关闭加载提示
      loadingMsg.close()
      console.error('取消预约失败:', error)
      ElMessage({
        message: '取消预约失败，请稍后重试',
        type: 'error',
        duration: 5000
      })
    }
  } catch (error) {
    // 用户取消确认框
    if (error !== 'cancel') {
      console.error('取消预约操作异常:', error)
      ElMessage.error('操作异常，请稍后重试')
    }
  }
}

// 确认退款
const confirmRefund = async () => {
  if (!refundForm.reasonType) {
    ElMessage.warning('请选择退款原因')
    return
  }
  
  refunding.value = true
  
  try {
    const res = await refundAppointment(refundForm.orderId)
    
    if (res.code === 200) {
      ElMessage.success('退款申请提交成功')
      refundDialogVisible.value = false
      await fetchPaymentList()
    } else {
      ElMessage.error(res.message || '退款申请失败')
    }
  } catch (error) {
    console.error('退款申请失败:', error)
    ElMessage.error('退款申请失败，请稍后重试')
  } finally {
    refunding.value = false
  }
}

// 获取订单详情
const fetchOrderDetail = async (orderId) => {
  try {
    const res = await getAppointmentOrder(orderId)
    if (res.code === 200) {
      return res.data
    } else {
      console.error('获取订单详情失败:', res.message)
      return null
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    return null
  }
}

// 获取支付记录列表
const fetchPaymentList = async () => {
  loading.value = true
  
  try {
    // 检查登录状态
    if (!UserStorage.isLoggedIn()) {
      ElMessage.error('请先登录')
      router.push('/login')
      return
    }
    
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    }
    
    // 添加日期筛选条件
    if (selectedDate.value) {
      params.createDate = selectedDate.value
    }
    
    console.log('支付记录请求参数:', params)
    const res = await getPaymentOrders(params)
    console.log('支付记录API响应:', res)
    
    if (res.code === 200) {
      if (res.data && res.data.records) {
        paymentList.value = res.data.records
        pagination.total = res.data.total || res.data.records.length
      } else if (res.data && Array.isArray(res.data)) {
        paymentList.value = res.data
        pagination.total = res.data.length
      } else {
        paymentList.value = []
        pagination.total = 0
      }
      
      // 打印每条记录的状态信息
      paymentList.value.forEach((item, index) => {
        console.log(`记录 ${index + 1}:`, {
          id: item.id,
          payerId: item.payerId,
          paymentStatus: item.paymentStatus,
          paymentAmount: item.paymentAmount,
          paymentTime: item.paymentTime,
          paymentMethod: item.paymentMethod,
          createTime: item.createTime
        })
      })
    } else {
      console.error('获取支付记录失败:', res.message)
      paymentList.value = []
      pagination.total = 0
      ElMessage.error(res.message || '获取支付记录失败')
    }
    
    console.log('支付记录列表:', paymentList.value)
    console.log('分页总数:', pagination.total)
  } catch (error) {
    console.error('获取支付记录失败:', error)
    ElMessage.error('获取支付记录失败，请稍后重试')
    paymentList.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  fetchPaymentList()
})
</script>

<style scoped>
.appointment-payment-container {
  max-width: 1200px;
  margin: 0 auto;
}

.payment-card {
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

.payment-list {
  min-height: 500px;
}



.price {
  font-weight: 600;
  color: #2c5aa0;
}

.action-buttons {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 订单详情样式 */
.order-detail {
  padding: 10px;
}

.order-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 15px;
}

/* 支付对话框样式 */
.payment-method-select {
  margin-bottom: 20px;
}

.payment-amount {
  margin-bottom: 20px;
  font-size: 16px;
  color: #2c5aa0;
}

/* 退款对话框样式 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>