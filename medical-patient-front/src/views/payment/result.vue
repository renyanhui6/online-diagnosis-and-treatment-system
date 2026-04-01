<template>
  <div class="payment-result-page">
    <el-card class="payment-result-card">
      <div class="result-header">
        <el-result
          :icon="resultIcon"
          :title="resultTitle"
          :sub-title="resultSubTitle"
        >
          <template #extra>
            <div class="result-actions">
              <el-button type="primary" @click="fetchResult" :loading="loading">刷新状态</el-button>
              <el-button
                v-if="canOperatePending"
                type="success"
                @click="confirmMockPayment"
                :loading="actionLoading"
              >
                模拟支付成功
              </el-button>
              <el-button
                v-if="canOperatePending"
                type="danger"
                plain
                @click="cancelMockPayment"
                :loading="actionLoading"
              >
                取消支付
              </el-button>
              <el-button @click="goToAppointmentList">我的预约</el-button>
            </div>
          </template>
        </el-result>
      </div>

      <el-descriptions v-if="result" :column="1" border class="payment-result-descriptions">
        <el-descriptions-item label="挂号编号">{{ result.registrationId }}</el-descriptions-item>
        <el-descriptions-item label="支付单号">{{ result.outTradeNo }}</el-descriptions-item>
        <el-descriptions-item label="支付状态">{{ result.paymentStatus }}</el-descriptions-item>
        <el-descriptions-item label="挂号状态">{{ getAppointmentStatusText(result.registrationStatus) }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">¥{{ formatMoney(result.paymentAmount) }}</el-descriptions-item>
        <el-descriptions-item label="支付时间">{{ formatDate(result.paymentTime) || '未支付' }}</el-descriptions-item>
        <el-descriptions-item label="支付截止">{{ formatDate(result.expireTime) || '-' }}</el-descriptions-item>
        <el-descriptions-item label="渠道流水">{{ result.gatewayTradeNo || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getAppointmentPaymentResult,
  mockAppointmentPaymentCancel,
  mockAppointmentPaymentSuccess
} from '../../api/appointment'
import { formatDate, formatMoney, getAppointmentStatusText } from '../../utils'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const actionLoading = ref(false)
const result = ref(null)

const canOperatePending = computed(() => result.value?.paymentStatus === 'PENDING' && !result.value?.paid)

const resultIcon = computed(() => {
  if (!result.value) return 'info'
  if (result.value.paid) return 'success'
  if (result.value.paymentStatus === 'CLOSED' || result.value.paymentStatus === 'FAILED') return 'error'
  return 'warning'
})

const resultTitle = computed(() => {
  if (!result.value) return '正在查询支付结果'
  if (result.value.paid) return '支付成功'
  if (result.value.paymentStatus === 'CLOSED') return '支付已关闭'
  if (result.value.paymentStatus === 'FAILED') return '支付失败'
  return '待支付'
})

const resultSubTitle = computed(() => {
  if (!result.value) return '正在查询支付结果'
  if (result.value.paymentStatus === 'PENDING') return result.value.message || '请在当前页面完成模拟支付确认'
  return result.value.message || '正在查询支付结果'
})

const outTradeNo = computed(() => route.query.out_trade_no || route.query.outTradeNo || '')

const fetchResult = async () => {
  if (!outTradeNo.value) {
    ElMessage.error('缺少支付单号，无法查询支付结果')
    return
  }
  loading.value = true
  try {
    const res = await getAppointmentPaymentResult(outTradeNo.value)
    if (res.code === 200) {
      result.value = res.data
      return
    }
    ElMessage.error(res.message || '查询支付结果失败')
  } catch (error) {
    console.error('查询支付结果失败:', error)
    ElMessage.error(error.message || '查询支付结果失败')
  } finally {
    loading.value = false
  }
}

const confirmMockPayment = async () => {
  if (!outTradeNo.value) {
    return
  }
  try {
    actionLoading.value = true
    const res = await mockAppointmentPaymentSuccess(outTradeNo.value)
    if (res.code !== 200) {
      ElMessage.error(res.message || '模拟支付失败')
      return
    }
    result.value = res.data
    ElMessage.success(res.data?.message || '模拟支付成功')
  } catch (error) {
    console.error('模拟支付失败:', error)
    ElMessage.error(error.message || '模拟支付失败')
  } finally {
    actionLoading.value = false
  }
}

const cancelMockPayment = async () => {
  if (!outTradeNo.value) {
    return
  }
  try {
    actionLoading.value = true
    const res = await mockAppointmentPaymentCancel(outTradeNo.value)
    if (res.code !== 200) {
      ElMessage.error(res.message || '取消支付失败')
      return
    }
    result.value = res.data
    ElMessage.success(res.data?.message || '已取消支付')
  } catch (error) {
    console.error('取消支付失败:', error)
    ElMessage.error(error.message || '取消支付失败')
  } finally {
    actionLoading.value = false
  }
}

const goToAppointmentList = () => {
  router.push('/appointment/list')
}

onMounted(() => {
  fetchResult()
})
</script>

<style scoped>
.payment-result-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 12px;
}

.payment-result-card {
  border-radius: 20px;
}

.result-header {
  padding: 8px 0 24px;
}

.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  flex-wrap: wrap;
}

.payment-result-descriptions {
  margin-top: 12px;
}
</style>
