<template>
  <div class="medicine-payment-container">
    <el-card class="payment-card">
      <template #header>
        <div class="card-header">
          <h2>药品支付记录</h2>
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
      
      <div v-loading="loading" class="payment-list">
        <el-empty v-if="paymentList.length === 0" description="暂无支付记录" />
        <el-table v-else :data="paymentList" style="width: 100%" border>
          <el-table-column prop="id" label="订单编号" width="180" />
          <el-table-column label="医疗记录ID" width="120">
            <template #default="{ row }">
              <span>{{ row.medicalRecordId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="paymentAmount" label="支付金额" width="120">
            <template #default="{ row }">
              <span class="price">¥{{ (row.paymentAmount || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="支付方式" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.paymentMethod === 1" type="success">
                <el-icon><ChatDotRound /></el-icon>
                微信
              </el-tag>
              <el-tag v-else-if="row.paymentMethod === 2" type="primary">
                <el-icon><Wallet /></el-icon>
                支付宝
              </el-tag>
              <el-tag v-else-if="row.paymentMethod === 3" type="warning">
                <el-icon><CreditCard /></el-icon>
                银行卡
              </el-tag>
              <el-tag v-else type="info">其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentStatus" label="支付状态" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.paymentStatus === 0" type="warning">待支付</el-tag>
              <el-tag v-else-if="row.paymentStatus === 1" type="success">已支付</el-tag>
              <el-tag v-else-if="row.paymentStatus === 2" type="info">已退款</el-tag>
              <el-tag v-else-if="row.paymentStatus === 3" type="primary">已核销</el-tag>
              <el-tag v-else type="info">未知状态</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="orderSource" label="订单来源" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.orderSource === 1" type="primary">处方购药</el-tag>
              <el-tag v-else-if="row.orderSource === 2" type="success">购物车</el-tag>
              <el-tag v-else type="info">其他</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="paymentGateway" label="支付网关" width="120">
            <template #default="{ row }">
              <span>{{ row.paymentGateway || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" />
          <el-table-column prop="paymentTime" label="支付时间" width="180">
            <template #default="{ row }">
              <span v-if="row.paymentStatus >= 1 && row.paymentTime">{{ formatDateTime(row.paymentTime) }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <!-- 未付款时显示支付和取消按钮 -->
                <el-button v-if="row.paymentStatus === 0" type="primary" size="small" @click="handlePay(row)">支付</el-button>
                <el-button v-if="row.paymentStatus === 0" type="warning" size="small" @click="handleCancel(row)">取消</el-button>
                
                <!-- 已支付时显示退款和核销按钮 -->
                <el-button v-if="row.paymentStatus === 1" type="danger" size="small" @click="handleRefund(row)">退款</el-button>
                <el-button v-if="row.paymentStatus === 1" type="success" size="small" @click="handleVerify(row)">核销</el-button>
                
                <!-- 所有状态都显示详情按钮 -->
                <el-button type="info" size="small" @click="viewDetail(row)">详情</el-button>
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
    <el-dialog v-model="detailDialogVisible" title="订单详情" width="700px">
      <div v-if="currentOrder" class="order-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单编号">{{ currentOrder.id }}</el-descriptions-item>
          <el-descriptions-item label="医疗记录ID">{{ currentOrder.medicalRecordId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag v-if="currentOrder.paymentStatus === 0" type="warning">待支付</el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 1" type="success">已支付</el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 2" type="info">已退款</el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 3" type="primary">已核销</el-tag>
            <el-tag v-else type="info">未知状态</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单来源">
            <el-tag v-if="currentOrder.orderSource === 1" type="primary">处方购药</el-tag>
            <el-tag v-else-if="currentOrder.orderSource === 2" type="success">购物车</el-tag>
            <el-tag v-else type="info">其他</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(currentOrder.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">
            <span v-if="currentOrder.paymentStatus >= 1 && currentOrder.paymentTime">{{ formatDateTime(currentOrder.paymentTime) }}</span>
            <span v-else>-</span>
          </el-descriptions-item>
          <el-descriptions-item label="支付方式">
            <el-tag v-if="currentOrder.paymentMethod === 1" type="success">
              <el-icon><ChatDotRound /></el-icon>
              微信
            </el-tag>
            <el-tag v-else-if="currentOrder.paymentMethod === 2" type="primary">
              <el-icon><Wallet /></el-icon>
              支付宝
            </el-tag>
            <el-tag v-else-if="currentOrder.paymentMethod === 3" type="warning">
              <el-icon><CreditCard /></el-icon>
              银行卡
            </el-tag>
            <el-tag v-else-if="currentOrder.paymentStatus === 0" type="info">待选择</el-tag>
            <el-tag v-else type="info">其他</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="支付网关">{{ currentOrder.paymentGateway || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付金额">
            <span class="price">¥{{ (currentOrder.paymentAmount || 0).toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="order-items">
          <h3>药品详情</h3>
          <el-alert
            title="说明"
            type="info"
            description="以下为本次订单的药品详细信息，包含药品ID、药品名称、数量和金额。"
            show-icon
            :closable="false"
            style="margin-bottom: 20px"
          />
          
          <!-- 药品详情列表 -->
          <div v-if="currentOrder.loading" class="loading-container">
            <el-skeleton :rows="3" animated />
          </div>
          <div v-else-if="currentOrder.drug && currentOrder.drug.length > 0" class="drug-details-list">
            <el-table :data="currentOrder.drug" border style="width: 100%">
              <el-table-column prop="drugId" label="药品ID" width="100" align="center" />
              <el-table-column prop="drugName" label="药品名称" min-width="200" />
              <el-table-column prop="quantity" label="数量" width="100" align="center">
                <template #default="{ row }">
                  <span>{{ row.quantity }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="金额" width="120" align="right">
                <template #default="{ row }">
                  <span class="price">¥{{ (row.amount || 0).toFixed(2) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
          
          <!-- 如果没有药品详情数据，显示提示信息 -->
          <div v-else class="no-drug-details">
            <el-empty description="暂无药品详情数据" />
          </div>
        </div>
        
        <div class="order-actions">
          <!-- 未付款时显示支付按钮 -->
          <el-button v-if="currentOrder.paymentStatus === 0" type="primary" @click="handlePay(currentOrder)">立即支付</el-button>
          
          <!-- 已支付时显示退款和核销按钮 -->
          <el-button v-if="currentOrder.paymentStatus === 1" type="danger" @click="handleRefund(currentOrder)">申请退款</el-button>
          <el-button v-if="currentOrder.paymentStatus === 1" type="success" @click="handleVerify(currentOrder)">核销</el-button>
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
                <el-radio :label="1">
                  <el-icon><ChatDotRound /></el-icon>
                  微信
                </el-radio>
                <el-radio :label="2">
                  <el-icon><Wallet /></el-icon>
                  支付宝
                </el-radio>
                <el-radio :label="3">
                  <el-icon><CreditCard /></el-icon>
                  银行卡
                </el-radio>
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
            <span class="price">¥{{ refundForm.amount.toFixed(2) }}</span>
          </el-form-item>
          <el-form-item label="退款原因" prop="reason">
            <el-select v-model="refundForm.reasonType" placeholder="请选择退款原因" style="width: 100%">
              <el-option label="药品过期/变质" value="expired" />
              <el-option label="药品包装破损" value="damaged" />
              <el-option label="药品信息与描述不符" value="mismatch" />
              <el-option label="重复下单" value="duplicate" />
              <el-option label="不想要了" value="unwanted" />
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
import { ChatDotRound, Wallet, CreditCard } from '@element-plus/icons-vue'
import UserStorage from '../../utils/userStorage'
import { getMedicineOrderList, getMedicineOrderDetail, payMedicineOrder, refundMedicineOrder, verifyMedicineOrder, cancelMedicineOrder } from '../../api/medicine'

// 计算药品总金额
const calculateTotalAmount = (drugList) => {
  if (!drugList || !Array.isArray(drugList)) return 0
  return drugList.reduce((total, drug) => total + (drug.amount || 0), 0)
}

// 日期格式化函数
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 过滤条件
const selectedDate = ref(null)

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
    // 显示加载状态
    currentOrder.value = { ...order, loading: true }
    detailDialogVisible.value = true
    
    // 获取药品详情数据
    const orderDetail = await fetchOrderDetail(order.id)
    
    if (orderDetail) {
      // 合并订单基本信息和药品详情
      currentOrder.value = {
        ...order,
        ...orderDetail,
        loading: false
      }
    } else {
      // 如果获取详情失败，至少显示基本信息
      currentOrder.value = { ...order, loading: false, drug: [] }
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    currentOrder.value = { ...order, loading: false, drug: [] }
    ElMessage.error('获取订单详情失败')
  }
}

// 处理支付
const handlePay = (order) => {
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
    const res = await payMedicineOrder(payingOrderId.value, {
      paymentMethod: paymentMethod.value
    })
    ElMessage.success('支付成功')
    paymentDialogVisible.value = false
    await fetchPaymentList()
  } catch (error) {
    ElMessage.error('支付失败')
  }
}

// 处理核销
const handleVerify = async (order) => {
  // 只有已支付状态的订单可以核销
  if (order.paymentStatus !== 1) {
    ElMessage.warning('只有已支付的订单可以核销')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '确认核销该订单？核销后将无法退款。',
      '确认核销',
      {
        confirmButtonText: '确认核销',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    // 调用核销订单的接口
    const res = await verifyMedicineOrder(order.id)
    
    if (res.code === 200) {
      ElMessage.success('订单核销成功')
      await fetchPaymentList()
    } else {
      ElMessage.error(res.message || '订单核销失败')
    }
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消操作，不做处理
      return
    }
    console.error('核销订单失败:', error)
    ElMessage.error('订单核销失败')
  }
}

// 处理取消订单
const handleCancel = async (order) => {
  // 只有未支付的订单可以取消
  if (order.paymentStatus !== 0) {
    ElMessage.warning('只有未支付的订单可以取消')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      '确认取消该订单？取消后不可恢复。',
      '确认取消',
      {
        confirmButtonText: '确认取消',
        cancelButtonText: '返回',
        type: 'warning',
      }
    )
    
    // 调用取消订单的接口
    const res = await cancelMedicineOrder(order.id)
    
    if (res.code === 200) {
      ElMessage.success('订单取消成功')
      await fetchPaymentList()
    } else {
      ElMessage.error(res.message || '订单取消失败')
    }
  } catch (error) {
    if (error === 'cancel') {
      // 用户取消操作，不做处理
      return
    }
    console.error('取消订单失败:', error)
    ElMessage.error('订单取消失败')
  }
}

// 处理退款
const handleRefund = (order) => {
  // 只有已支付状态的订单可以退款
  if (order.paymentStatus !== 1) {
    ElMessage.warning('只有已支付的订单可以退款')
    return
  }
  
  // 已核销的订单不能退款
  if (order.paymentStatus === 3) {
    ElMessage.warning('已核销的订单无法退款')
    return
  }
  
  refundForm.orderId = order.id
  refundForm.amount = order.paymentAmount || 0
  refundForm.reasonType = ''
  refundForm.reasonDetail = ''
  refundDialogVisible.value = true
}

// 确认退款
const confirmRefund = async () => {
  if (!refundForm.reasonType) {
    ElMessage.warning('请选择退款原因')
    return
  }
  
  refunding.value = true
  
  try {
    // 调用退款API，只传递订单ID
    const res = await refundMedicineOrder(refundForm.orderId)
    
    if (res.code === 200) {
      ElMessage.success('退款申请提交成功')
      refundDialogVisible.value = false
      await fetchPaymentList()
    } else {
      ElMessage.error(res.message || '退款申请失败')
    }
  } catch (error) {
    console.error('退款申请失败:', error)
    ElMessage.error('退款申请失败')
  } finally {
    refunding.value = false
  }
}

// 获取订单详情
const fetchOrderDetail = async (orderId) => {
  try {
    // 使用分页参数获取药品详情，默认获取第一页，每页100条记录
    const res = await getMedicineOrderDetail(orderId, 1, 100)
    
    // 根据后端返回的数据结构处理
    if (res.data && res.data.records) {
      // 如果返回的是分页数据结构
      return {
        ...res.data,
        drug: res.data.records.map(item => ({
          drugId: item.drugId,
          drugName: item.genericName,  // 后端字段名为genericName
          quantity: item.drugQuantity, // 后端字段名为drugQuantity
          amount: item.amount
        }))
      }
    } else if (res.data && Array.isArray(res.data)) {
      // 如果直接返回数组
      return {
        drug: res.data.map(item => ({
          drugId: item.drugId,
          drugName: item.genericName,
          quantity: item.drugQuantity,
          amount: item.amount
        }))
      }
    } else {
      return res.data
    }
  } catch (error) {
    console.error('获取订单详情失败:', error)
    ElMessage.error('获取订单详情失败')
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
    
    console.log('药品支付记录请求参数:', params)
    const res = await getMedicineOrderList(params)
    console.log('药品支付记录API响应:', res.data)
    
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
    
    console.log('药品支付记录列表:', paymentList.value)
    console.log('分页总数:', pagination.total)
  } catch (error) {
    console.error('获取药品支付记录失败:', error)
    ElMessage.error('获取订单列表失败')
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
.medicine-payment-container {
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

.medicine-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.medicine-count {
  font-size: 14px;
  color: #4a6fa5;
}

.medicine-detail-list {
  max-height: 300px;
  overflow-y: auto;
}

.medicine-detail-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(135, 206, 250, 0.1);
}

.medicine-detail-item:last-child {
  border-bottom: none;
}

.medicine-detail-info {
  flex: 1;
  margin: 0 10px;
}

.medicine-detail-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c5aa0;
}

.medicine-detail-spec {
  font-size: 12px;
  color: #4a6fa5;
}

.medicine-detail-price {
  font-size: 14px;
  font-weight: 500;
  color: #2c5aa0;
  margin-right: 10px;
}

.medicine-detail-quantity {
  font-size: 14px;
  color: #4a6fa5;
  width: 40px;
  text-align: right;
}

.price {
  font-weight: 600;
  color: #2c5aa0;
}

.payment-method-select .el-icon {
  margin-right: 5px;
}

.price {
  color: #e6a23c;
  font-weight: 600;
}

.drug-total-row {
  margin-top: 15px;
}

.total-info {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
}

.total-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.total-amount {
  font-size: 18px;
  font-weight: 700;
  color: #e6a23c;
}

.loading-container {
  padding: 20px;
}

.action-buttons {
  display: flex;
  gap: 5px;
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

.order-items {
  margin-top: 20px;
}

.order-items h3 {
  font-size: 16px;
  font-weight: 600;
  color: #2c5aa0;
  margin: 0 0 15px 0;
  text-shadow: 0 1px 2px rgba(135, 206, 250, 0.1);
}

.drug-details-list {
  margin-top: 15px;
}

.no-drug-details {
  text-align: center;
  padding: 40px 0;
}

.medicine-info-row {
  display: flex;
  align-items: center;
}

.medicine-info-detail {
  margin-left: 10px;
}

.medicine-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c5aa0;
}

.medicine-spec {
  font-size: 12px;
  color: #4a6fa5;
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