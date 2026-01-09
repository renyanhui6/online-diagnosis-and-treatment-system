<template>
  <div class="medicine-container">
    <el-card class="medicine-card">
      <template #header>
        <div class="card-header">
          <h2>在线购药</h2>
          <div class="header-actions">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索药品名称"
              clearable
              @keyup.enter="handleSearch"
              class="search-input"
            >
              <template #append>
                <el-button @click="handleSearch">
                  <el-icon><Search /></el-icon>
                </el-button>
              </template>
            </el-input>
            <el-button v-if="!isFromPrescription" type="primary" @click="viewCart">
              <el-badge :value="cartCount" :hidden="cartCount === 0">
                <el-icon><ShoppingCart /></el-icon>
                购物车
              </el-badge>
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 处方信息提示 -->
      <div v-if="isFromPrescription && prescriptionInfo" class="prescription-info">
        <el-alert
          :title="`处方购买 - ${prescriptionInfo.patientName} (${prescriptionInfo.doctorName}医生开具)`"
          type="info"
          description="您正在根据医生处方购买药品，请仔细核对药品信息。"
          show-icon
          :closable="false"
        />
      </div>

      
      <!-- 药品列表 -->
      <div v-loading="loading" class="medicine-list">
        <el-empty v-if="filteredDrugs.length === 0" description="暂无药品数据" />
        <el-row :gutter="20" v-else>
          <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="drug in filteredDrugs" :key="drug.id">
            <div class="medicine-item">
              <div class="medicine-image" @click="viewDrugDetail(drug)">
                <el-image :src="getDrugImage(drug)" fit="cover" :preview-src-list="[getDrugImage(drug)]">
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                      <span>暂无图片</span>
                    </div>
                  </template>
                </el-image>
                <div v-if="drug.isPrescription === 1" class="prescription-tag">处方药</div>
              </div>
              <div class="medicine-info">
                <h3 class="medicine-name" @click="viewDrugDetail(drug)">{{ drug.genericName }}</h3>
                <p class="medicine-spec">{{ drug.specification }}</p>
                <div class="medicine-price-row">
                  <span class="medicine-price">¥{{ drug.drugPrice }}</span>
                  <el-button 
                    type="primary" 
                    size="small" 
                    @click="addToCart(drug)"
                    :disabled="drug.isPrescription === 1"
                  >
                    加入购物车
                  </el-button>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[12, 24, 36, 48]"
          :total="totalDrugs"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 药品详情对话框 -->
    <el-dialog v-model="drugDetailVisible" title="药品详情" width="700px">
      <div class="medicine-detail-content" v-if="currentDrug">
          <div class="medicine-detail-header">
            <div class="medicine-detail-image">
              <el-image :src="getDrugImage(currentDrug)" fit="cover" :preview-src-list="[getDrugImage(currentDrug)]">
                <template #error>
                  <div class="image-placeholder">
                    <el-icon><Picture /></el-icon>
                    <span>暂无图片</span>
                  </div>
                </template>
              </el-image>
            </div>
          <div class="medicine-detail-info">
            <h2>{{ currentDrug.genericName }}</h2>
            <p class="medicine-detail-spec">规格：{{ currentDrug.specification }}</p>
            <p class="medicine-detail-manufacturer">生产厂家：{{ currentDrug.manufacturer }}</p>
            <p class="medicine-detail-price">价格：<span class="price">¥{{ currentDrug.drugPrice }}</span></p>
            
            <div class="purchase-controls">
              <el-input-number 
                v-model="purchaseQuantity" 
                :min="1" 
                :max="99" 
                size="small"
                :disabled="currentDrug.isPrescription === 1"
              />
              <el-button 
                type="primary" 
                @click="addToCartWithQuantity"
                :disabled="currentDrug.isPrescription === 1"
              >
                加入购物车
              </el-button>
            </div>
            
            <div v-if="currentDrug.isPrescription === 1" class="prescription-warning">
              <el-alert
                title="处方药提醒"
                description="此药品为处方药，需凭医生处方购买"
                type="warning"
                :closable="false"
              />
            </div>
          </div>
        </div>
        
        <el-tabs type="border-card">
          <el-tab-pane label="基本信息">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="药品名称">{{ currentDrug.genericName }}</el-descriptions-item>
              <el-descriptions-item label="通用名称">{{ currentDrug.genericName }}</el-descriptions-item>
              <el-descriptions-item label="规格">{{ currentDrug.specification }}</el-descriptions-item>
              <el-descriptions-item label="最小销售单位">{{ currentDrug.minimumSalesUnit }}</el-descriptions-item>
              <el-descriptions-item label="价格">¥{{ currentDrug.drugPrice }}</el-descriptions-item>
              <el-descriptions-item label="库存数量">{{ currentDrug.quantity }}</el-descriptions-item>
              <el-descriptions-item label="是否处方药">{{ currentDrug.isPrescription === 1 ? '是' : '否' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
          
          <el-tab-pane label="用药指导">
            <div class="medicine-guidance">
              <h4>用药指导</h4>
              <p>{{ currentDrug.guidance }}</p>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
    
    <!-- 购物车对话框 -->
    <el-dialog v-model="cartDialogVisible" title="购物车" width="700px">
      <div class="cart-content">
        <el-empty v-if="cartItems.length === 0" description="购物车为空" />
        <template v-else>
          <el-table :data="cartItems" style="width: 100%">
            <el-table-column label="药品信息">
              <template #default="{ row }">
                <div class="cart-item-info">
                  <el-image :src="getDrugImage(row)" fit="cover" style="width: 50px; height: 50px;">
                    <template #error>
                      <div class="image-placeholder" style="width: 50px; height: 50px; display: flex; align-items: center; justify-content: center; background: #f5f5f5;">
                        <el-icon><Picture /></el-icon>
                      </div>
                    </template>
                  </el-image>
                  <div class="cart-item-name">
                    <h4>{{ row.genericName }}</h4>
                    <p>{{ row.specification }}</p>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="单价" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ row.drugPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="150">
              <template #default="{ row }">
                <el-input-number 
                  v-model="row.quantity" 
                  :min="1" 
                  :max="99"
                  size="small"
                  @change="updateCartItemQuantity(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计" width="100">
              <template #default="{ row }">
                <span class="price">¥{{ (row.drugPrice * row.quantity).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button 
                  type="danger" 
                  size="small" 
                  circle 
                  @click="removeFromCart(row)"
                >
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          
          <div class="cart-footer">
            <div class="cart-actions">
              <el-button @click="clearCart">清空购物车</el-button>
            </div>
            <div class="cart-total">
              <p>共 <span class="count">{{ cartCount }}</span> 件商品，合计：<span class="price">¥{{ cartTotal.toFixed(2) }}</span></p>
              <el-button type="primary" @click="checkout">结算</el-button>
            </div>
          </div>
        </template>
      </div>
    </el-dialog>
    
    <!-- 结算对话框 -->
    <el-dialog v-model="checkoutDialogVisible" title="订单结算" width="500px">
      <div class="checkout-content">
        <el-alert
          :title="isFromPrescription ? '处方订单信息' : '订单信息'"
          type="info"
          :description="isFromPrescription ? '请确认您的处方订单信息无误，提交后将无法修改。' : '请确认您的订单信息无误，提交后将无法修改。'"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <!-- 处方信息显示 -->
        <div v-if="isFromPrescription && prescriptionInfo" class="prescription-details">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="就诊人">{{ prescriptionInfo.patientName }}</el-descriptions-item>
            <el-descriptions-item label="开具医生">{{ prescriptionInfo.doctorName }}</el-descriptions-item>
            <el-descriptions-item label="就诊记录ID">{{ prescriptionInfo.consultationRecordId }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <el-form :model="orderForm" label-width="100px">
          <el-form-item label="支付方式">
            <el-radio-group v-model="orderForm.paymentMethod">
              <el-radio :label="1">微信支付</el-radio>
              <el-radio :label="2">支付宝</el-radio>
              <el-radio :label="3">银行卡</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        
        <div class="order-summary">
          <p v-if="!isFromPrescription">共 <span class="count">{{ cartCount }}</span> 件商品，合计：<span class="price">¥{{ cartTotal.toFixed(2) }}</span></p>
          <p v-else>处方药品订单，金额将在提交后确定</p>
        </div>
        
        <div class="dialog-footer">
          <el-button @click="checkoutDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitOrder" :loading="submitting">提交订单</el-button>
        </div>
      </div>
    </el-dialog>
    
    <!-- 支付对话框 -->
    <el-dialog v-model="paymentDialogVisible" title="订单支付" width="400px" center>
      <div class="payment-dialog-content">
        <el-result icon="success" title="订单创建成功" sub-title="请在30分钟内完成支付，否则订单将自动取消">
          <template #extra>
            <div class="payment-amount">
              <p>支付金额: <span class="price">¥{{ cartTotal.toFixed(2) }}</span></p>
            </div>
            <el-button type="primary" @click="confirmPayment" :loading="paying">立即支付</el-button>
            <el-button @click="cancelPayment">稍后支付</el-button>
          </template>
        </el-result>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ShoppingCart, Picture, Plus, Minus, Close } from '@element-plus/icons-vue'
import { getDrugList, searchDrugList, createOrder, createByPrescription, completeMedicineOrderPayment } from '../../api/medicine'
import UserStorage from '../../utils/userStorage'

const router = useRouter()
const route = useRoute()

// 检查是否来自处方购买
const isFromPrescription = computed(() => {
  return route.query.fromPrescription === 'true'
})

// 处方信息
const prescriptionInfo = computed(() => {
  if (isFromPrescription.value) {
    return {
      consultationRecordId: route.query.consultationRecordId,
      patientName: route.query.patientName,
      doctorName: route.query.doctorName
    }
  }
  return null
})

// 搜索关键词
const searchKeyword = ref('')

// 当前选中的药品分类
const activeCategory = ref('all')

// 分页信息
const pagination = reactive({
  currentPage: 1,
  pageSize: 12,
  total: 0
})

// 药品列表数据
const drugList = ref([])
const loading = ref(false)

// 购物车数据
const cartItems = ref([])
const cartDialogVisible = ref(false)

// 药品详情
const currentDrug = ref(null)
const drugDetailVisible = ref(false)
const purchaseQuantity = ref(1)

// 结算相关
const checkoutDialogVisible = ref(false)
// 订单表单
const orderForm = reactive({
  paymentMethod: 1
})
const submitting = ref(false)

// 支付相关
const paymentDialogVisible = ref(false)
const paying = ref(false)
const createdOrderId = ref('')

// 计算属性：过滤后的药品列表
const filteredDrugs = computed(() => {
  return drugList.value
})

// 计算属性：药品总数
const totalDrugs = computed(() => {
  return pagination.total
})

// 计算属性：购物车商品数量
const cartCount = computed(() => {
  return cartItems.value.reduce((total, item) => total + item.quantity, 0)
})

// 计算属性：购物车总金额
const cartTotal = computed(() => {
  return cartItems.value.reduce((total, item) => total + (item.drugPrice * item.quantity), 0)
})

// 处理搜索
const handleSearch = () => {
  pagination.currentPage = 1
  fetchDrugList()
}

// 处理页码变化
const handleCurrentChange = (page) => {
  pagination.currentPage = page
  fetchDrugList()
}

// 处理每页条数变化
const handleSizeChange = (size) => {
  pagination.pageSize = size
  pagination.currentPage = 1
  fetchDrugList()
}

// 查看药品详情
const viewDrugDetail = (drug) => {
  currentDrug.value = drug
  purchaseQuantity.value = 1
  drugDetailVisible.value = true
}

// 添加到购物车
const addToCart = (drug) => {
  if (drug.isPrescription === 1) {
    ElMessage.warning('处方药需凭医生处方购买')
    return
  }
  
  // 检查购物车中是否已存在该药品
  const existingItem = cartItems.value.find(item => item.id === drug.id)
  if (existingItem) {
    existingItem.quantity += 1
  } else {
    cartItems.value.push({
      ...drug,
      quantity: 1
    })
  }
  ElMessage.success('已添加到购物车')
}

// 添加指定数量到购物车
const addToCartWithQuantity = () => {
  if (!currentDrug.value) return
  
  if (currentDrug.value.isPrescription === 1) {
    ElMessage.warning('处方药需凭医生处方购买')
    return
  }
  
  try {
    // 模拟添加到购物车
    const existingItem = cartItems.value.find(item => item.id === currentDrug.value.id)
    if (existingItem) {
      existingItem.quantity += purchaseQuantity.value
    } else {
      cartItems.value.push({
        ...currentDrug.value,
        quantity: purchaseQuantity.value
      })
    }
    ElMessage.success('已添加到购物车')
    drugDetailVisible.value = false
  } catch (error) {
    console.error('添加到购物车失败:', error)
    ElMessage.error('添加失败，请稍后重试')
  }
}

// 查看购物车
const viewCart = () => {
  cartDialogVisible.value = true
}

// 更新购物车商品数量
const updateCartItemQuantity = (item) => {
  // 前端直接更新，无需调用接口
  ElMessage.success('更新成功')
}

// 从购物车移除
const removeFromCart = (item) => {
  ElMessageBox.confirm(
    '确定要从购物车中移除该商品吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 从前端购物车数组中移除
    const index = cartItems.value.findIndex(cartItem => cartItem.id === item.id)
    if (index > -1) {
      cartItems.value.splice(index, 1)
      ElMessage.success('已从购物车移除')
    }
  }).catch(() => {})
}

// 清空购物车
const clearCart = () => {
  if (cartItems.value.length === 0) {
    ElMessage.info('购物车已经为空')
    return
  }
  
  ElMessageBox.confirm(
    '确定要清空购物车吗？',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 清空前端购物车数组
    cartItems.value = []
    ElMessage.success('清空成功')
  }).catch(() => {})
}

// 结算
const checkout = () => {
  if (cartItems.value.length === 0) {
    ElMessage.warning('购物车为空，无法结算')
    return
  }
  
  // 初始化订单表单
  orderForm.paymentMethod = 1
  
  cartDialogVisible.value = false
  checkoutDialogVisible.value = true
}

// 提交订单
const submitOrder = async () => {
  submitting.value = true
  
  try {
    let res
    
    if (isFromPrescription.value) {
      // 通过处方创建药品订单
      res = await createByPrescription(prescriptionInfo.value.consultationRecordId)
    } else {
      // 构建购物车商品列表
      const cartItemsTemp = cartItems.value.map(item => ({
        drugId: item.id,
        drugQuantity: item.quantity
      }))
      
      // 通过购物车创建药品订单
      res = await createOrder(cartItemsTemp)
    }
    
    if (res.code === 200) {
      createdOrderId.value = res.data
      checkoutDialogVisible.value = false
      paymentDialogVisible.value = true
      ElMessage.success('订单创建成功，请完成支付')
    } else {
      ElMessage.error(res.message || '创建订单失败')
    }
    submitting.value = false
  } catch (error) {
    console.error('创建订单失败:', error)
    ElMessage.error('创建订单失败，请稍后重试')
    submitting.value = false
  }
}

// 确认支付
const confirmPayment = async () => {
  paying.value = true
  
  try {
    const paymentData = {
      id: createdOrderId.value,
      paymentStatus: 1,
      paymentTime: new Date().toISOString(),
      paymentMethod: orderForm.paymentMethod === 1 ? '微信支付' : orderForm.paymentMethod === 2 ? '支付宝' : '银行卡',
      paymentGateway: orderForm.paymentMethod === 1 ? 'wechat' : orderForm.paymentMethod === 2 ? 'alipay' : 'bank'
    }
    
    // 调用完成药品订单支付的接口
    const res = await completeMedicineOrderPayment(paymentData)
    if (res.code === 200) {
      ElMessage.success('支付成功')
      paymentDialogVisible.value = false
      // 清空购物车
      cartItems.value = []
      // 跳转到药品订单页面
      router.push('/payment/medicine')
    } else {
      ElMessage.error(res.message || '支付失败')
    }
    paying.value = false
  } catch (error) {
    console.error('支付失败:', error)
    ElMessage.error('支付失败，请稍后重试')
    paying.value = false
  }
}

// 取消支付
const cancelPayment = () => {
  paymentDialogVisible.value = false
  ElMessage.info('您可以稍后在"药品订单"中完成支付')
  router.push('/payment/medicine')
}


// 获取药品图片
const getDrugImage = (drug) => {
  // 如果有原始图片，使用原始图片
  if (drug.image) {
    return drug.image
  }
  
  // 根据药品ID映射到对应的图片文件（0-29）
  // 药品ID为1 → 药品(0).png，药品ID为4 → 药品(3).png
  // 即药品ID减1后取模30来对应图片索引
  const imageIndex = drug.id ? ((drug.id - 1) % 30) : 0
  
  // 根据索引返回对应的图片路径
  const imageMap = {
    0: '/drug/药品(0).png',
    1: '/drug/药品(1).png',
    2: '/drug/药品(2).png',
    3: '/drug/药品 (3).png',
    4: '/drug/药品 (4).png',
    5: '/drug/药品 (5).png',
    6: '/drug/药品(6).png',
    7: '/drug/药品 (7).png',
    8: '/drug/药品 (8).png',
    9: '/drug/药品 (9).png',
    10: '/drug/药品(10).png',
    11: '/drug/药品(11).png',
    12: '/drug/药品(12).png',
    13: '/drug/药品 (13).png',
    14: '/drug/药品 (14).png',
    15: '/drug/药品(15).png',
    16: '/drug/药品 (16).png',
    17: '/drug/药品(17).png',
    18: '/drug/药品 (18).png',
    19: '/drug/药品19).png',
    20: '/drug/药品(20).png',
    21: '/drug/药品(21).png',
    22: '/drug/药品 (22).png',
    23: '/drug/药品(23).png',
    24: '/drug/药品 (24).png',
    25: '/drug/药品 (25).png',
    26: '/drug/药品 (26).png',
    27: '/drug/药品(27).png',
    28: '/drug/药品(28).png',
    29: '/drug/药品(29).png'
  }
  
  return imageMap[imageIndex] || '/drug/药品(0).png'
}

// 获取药品列表
const fetchDrugList = async () => {
  loading.value = true
  
  try {
    // 根据是否有搜索关键词决定使用哪个API
    const apiFunction = searchKeyword.value ? searchDrugList : getDrugList;
    const res = await apiFunction({
      page: pagination.currentPage,
      pageSize: pagination.pageSize,
      keyword: searchKeyword.value,
      categoryId: activeCategory.value
    })
    drugList.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    console.error('获取药品列表失败:', error)
    ElMessage.error('获取药品列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 初始化
onMounted(() => {
  fetchDrugList()
  // fetchCartList() // 前端购物车不需要初始化获取
})
</script>

<style scoped>
.medicine-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px;
}

.medicine-card {
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.search-input {
  width: 250px;
}

.medicine-categories {
  margin: 20px 0;
}

.medicine-list {
  min-height: 500px;
}

.medicine-item {
  border: 1px solid rgb(var(--primary-200-rgb) / 0.25);
  border-radius: 12px;
  overflow: hidden;
  transition: all 0.3s ease;
  margin-bottom: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgb(var(--primary-50-rgb) / 0.55) 100%);
  box-shadow: var(--shadow);
}

.medicine-item:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92) 0%, rgb(var(--primary-100-rgb) / 0.55) 100%);
}

.medicine-image {
  height: 200px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
}

.medicine-image .el-image {
  width: 100%;
  height: 100%;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.85) 0%, rgb(var(--primary-100-rgb) / 0.55) 100%);
  color: var(--primary-600);
  font-size: 30px;
}

.image-placeholder span {
  font-size: 12px;
  margin-top: 5px;
  color: var(--neutral-500);
}

.prescription-tag {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: var(--error);
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.medicine-info {
  padding: 15px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.medicine-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin: 0 0 10px 0;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.medicine-name:hover {
  color: var(--primary-600);
}

.medicine-spec {
  font-size: 14px;
  color: var(--neutral-600);
  margin: 0 0 10px 0;
}

.medicine-price-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
}

.medicine-price {
  font-size: 18px;
  font-weight: 600;
  color: var(--warning);
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 药品详情样式 */
.medicine-detail-content {
  padding: 10px;
}

.medicine-detail-header {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.medicine-detail-image {
  width: 200px;
  height: 200px;
  overflow: hidden;
  border-radius: 8px;
  border: 1px solid rgb(var(--primary-200-rgb) / 0.25);
}

.medicine-detail-image .el-image {
  width: 100%;
  height: 100%;
}

.medicine-detail-info {
  flex: 1;
}

.medicine-detail-info h2 {
  font-size: 20px;
  font-weight: 600;
  color: var(--neutral-800);
  margin: 0 0 15px 0;
}

.medicine-detail-spec,
.medicine-detail-manufacturer {
  font-size: 14px;
  color: var(--neutral-600);
  margin: 0 0 10px 0;
}

.medicine-detail-price {
  font-size: 16px;
  color: var(--neutral-700);
  margin: 0 0 20px 0;
}

.price {
  font-size: 20px;
  font-weight: 600;
  color: var(--warning);
}

.medicine-detail-actions {
  display: flex;
  gap: 15px;
  align-items: center;
  margin-bottom: 15px;
}

.prescription-warning {
  margin-top: 15px;
}

.medicine-instructions,
.medicine-guidance {
  padding: 10px;
}

.medicine-guidance h3 {
  font-size: 16px;
  font-weight: 600;
  color: var(--neutral-800);
  margin: 0 0 10px 0;
}

.medicine-guidance p {
  font-size: 14px;
  color: var(--neutral-600);
  line-height: 1.6;
}

/* 购物车样式 */
.cart-item-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cart-item-name {
  display: flex;
  flex-direction: column;
}

.cart-item-name h4 {
  font-size: 14px;
  font-weight: 600;
  color: var(--neutral-800);
  margin: 0 0 5px 0;
}

.cart-item-name p {
  font-size: 12px;
  color: var(--neutral-600);
  margin: 0;
}

.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 20px;
}

.cart-total {
  display: flex;
  align-items: center;
  gap: 15px;
}

.cart-total p {
  font-size: 14px;
  color: var(--neutral-700);
  margin: 0;
}

.count {
  font-weight: 600;
  color: var(--primary-600);
}

/* 结算样式 */
.order-summary {
  margin: 20px 0;
  text-align: right;
  font-size: 16px;
  color: var(--neutral-700);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

/* 支付对话框样式 */
.payment-dialog-content {
  text-align: center;
}

.payment-amount {
  margin-bottom: 20px;
  font-size: 16px;
}

.prescription-info {
  margin-bottom: 20px;
}

.prescription-details {
  margin-bottom: 20px;
}

.prescription-details .el-descriptions {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 8px;
  padding: 15px;
}
</style>
