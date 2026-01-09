<template>
  <div class="medical-records-container">
    <div class="page-header">
      <h2>就诊记录管理</h2>
      <div class="header-actions">
        <el-select v-model="filterStatus" placeholder="就诊状态" clearable @change="handleFilterChange">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width: 300px"
        />
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索患者姓名/就诊记录ID"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-button type="primary" @click="refreshMedicalRecords">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
    
    <el-card shadow="hover" class="medical-record-card">
      <el-table
        v-loading="loading"
        :data="paginatedMedicalRecords"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="medicalRecordId" label="就诊记录ID" width="265" />
        <el-table-column prop="patientName" label="患者姓名" width="265">
          <template #default="{ row }">
            <div class="patient-info">
              <el-avatar :size="24" :src="row.patientAvatar" />
              <span>{{ row.patientName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="isPurchasable" label="处方状态" width="295">
          <template #default="{ row }">
            <el-tag :type="getPrescriptionStatusType(row.isPurchasable)">
              {{ getPrescriptionStatusText(row.isPurchasable) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="就诊时间" width="315" sortable />
        <el-table-column label="操作" width="215" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button 
                type="primary" 
                size="small"
                @click.stop="viewMedicalRecordDetail(row)"
              >
                查看详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 就诊记录详情抽屉 -->
    <el-drawer
      v-model="medicalRecordDrawerVisible"
      title="就诊记录详情"
      direction="rtl"
      size="40%"
    >
      <div v-if="selectedMedicalRecord" class="medical-record-detail">
        <div class="detail-header">
          <div class="record-id">就诊记录编号：{{ selectedMedicalRecord.medicalRecordId }}</div>
          <el-tag :type="getPrescriptionStatusType(selectedMedicalRecord.isPurchasable)" class="status-tag">
            {{ getPrescriptionStatusText(selectedMedicalRecord.isPurchasable) }}
          </el-tag>
        </div>
        
        <el-divider content-position="left">患者信息</el-divider>
        
        <div class="patient-section">
          <el-avatar :size="64" :src="selectedMedicalRecord.patientAvatar" />
          <div class="patient-info-detail">
            <h3>{{ selectedMedicalRecord.patientName }}</h3>
            <p>{{ selectedMedicalRecord.patientGender }} · {{ selectedMedicalRecord.patientAge }}岁</p>
            <p>{{ selectedMedicalRecord.patientPhone }}</p>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="detail-item">
            <span class="item-label">医生描述：</span>
            <span class="item-value">{{ selectedMedicalRecord.doctorDescription || '无' }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">创建时间：</span>
            <span class="item-value">{{ selectedMedicalRecord.createTime }}</span>
          </div>
          <div class="detail-item" v-if="selectedMedicalRecord.updateTime">
            <span class="item-label">更新时间：</span>
            <span class="item-value">{{ selectedMedicalRecord.updateTime }}</span>
          </div>
        </div>
        
        <div class="detail-actions">
          <el-button 
            v-if="selectedMedicalRecord.isPurchasable !== 2"
            type="primary" 
            @click="viewPrescriptionFromRecord"
          >
            查看处方
          </el-button>
          <el-button 
            type="info" 
            @click="medicalRecordDrawerVisible = false"
          >
            关闭
          </el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 处方详情抽屉 -->
    <el-drawer
      v-model="prescriptionDrawerVisible"
      title="处方详情"
      direction="rtl"
      size="40%"
    >
      <div v-if="selectedPrescription" class="prescription-detail">
        <div class="detail-header">
          <div class="prescription-id">处方编号：{{ selectedPrescription.id || '暂无' }}</div>
        </div>
        
        <el-divider content-position="left">基本信息</el-divider>
        
        <div class="patient-section">
          <el-avatar :size="64" :src="selectedPrescription.patientAvatar" />
          <div class="patient-info-detail">
            <h3>{{ selectedPrescription.patientName }}</h3>
            <p>{{ selectedPrescription.patientGender }} · {{ selectedPrescription.patientAge }}岁</p>
            <p>{{ selectedPrescription.patientPhone }}</p>
          </div>
        </div>
        
        <div class="detail-content">
          <div class="detail-item">
            <span class="item-label">患者姓名：</span>
            <span class="item-value">{{ selectedPrescription.patientName || selectedMedicalRecord?.patientName || '未知患者' }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">医生描述：</span>
            <span class="item-value">{{ selectedPrescription.doctorDescription || selectedMedicalRecord?.doctorDescription || '无' }}</span>
          </div>
        </div>
        
        <el-divider content-position="left">药品信息</el-divider>
        
        <el-table :data="selectedPrescription.drugs" border style="width: 100%">
          <el-table-column prop="drugName" label="药品名称" min-width="150" />
          <el-table-column prop="drugQuantity" label="药品数量" width="100" />
          <el-table-column prop="minimumSalesUnit" label="单位" width="100" />
          <el-table-column prop="price" label="价格" width="100">
            <template #default="{ row }">
              ¥{{ (row.price || 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="isPrescription" label="处方药" width="100">
            <template #default="{ row }">
              <el-tag :type="row.isPrescription === 1 ? 'danger' : 'success'">
                {{ row.isPrescription === 1 ? '处方药' : '非处方药' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <div class="price-summary">
          <div class="price-item">
            <span>药品总数：</span>
            <span>{{ selectedPrescription.medicineCount }}种</span>
          </div>
          <div class="price-item">
            <span>总金额：</span>
            <span class="total-price">¥{{ (selectedPrescription.total_amount || 0).toFixed(2) }}</span>
          </div>
        </div>
        
        <div class="detail-actions">
          <el-button 
            type="info" 
            @click="prescriptionDrawerVisible = false"
          >
            关闭
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Search, Refresh } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getMedicalRecordList, getPrescriptionInfoByMedicalRecordId } from '@/api/doctor';

const router = useRouter();

// 状态和数据
const loading = ref(false);
const medicalRecords = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterStatus = ref('');
const selectedDate = ref('');
const searchQuery = ref('');
const total = ref(0); // 添加总数

// 抽屉
const medicalRecordDrawerVisible = ref(false);
const prescriptionDrawerVisible = ref(false);
const selectedMedicalRecord = ref(null);
const selectedPrescription = ref(null);

// 处方状态选项 - 根据isPurchasable字段
const statusOptions = [
  { value: 0, label: '未使用' },
  { value: 1, label: '已使用' },
  { value: 2, label: '未开具' }
];

// 计算属性
const filteredMedicalRecords = computed(() => {
  // 确保medicalRecords.value是数组
  const records = Array.isArray(medicalRecords.value) ? medicalRecords.value : [];
  let result = [...records];
  
  console.log('filteredMedicalRecords计算，原始数据长度:', records.length);
  console.log('当前筛选条件 - 状态:', filterStatus.value, '日期:', selectedDate.value, '搜索:', searchQuery.value);
  
  // 状态筛选
  if (filterStatus.value !== '' && filterStatus.value !== null && filterStatus.value !== undefined) {
    result = result.filter(item => item.isPurchasable === filterStatus.value);
    console.log('状态筛选后长度:', result.length);
  }
  
  // 日期筛选 - 根据就诊时间的年月日查询
  if (selectedDate.value) {
    console.log('开始日期筛选，选择的日期:', selectedDate.value);
    const targetDate = new Date(selectedDate.value);
    const targetYear = targetDate.getFullYear();
    const targetMonth = targetDate.getMonth();
    const targetDay = targetDate.getDate();
    
    console.log('目标日期:', targetYear, targetMonth, targetDay);
    
    result = result.filter(item => {
      if (!item.createTime) {
        console.log('记录没有createTime:', item);
        return false;
      }
      
      console.log('检查记录:', item.createTime);
      
      // 处理日期格式问题：将 02-35 转换为 02:35
      let processedTime = item.createTime;
      if (processedTime && processedTime.includes('-')) {
        // 将时间部分的 - 替换为 :
        const parts = processedTime.split(' ');
        if (parts.length === 2) {
          const datePart = parts[0];
          const timePart = parts[1].replace(/-/g, ':');
          processedTime = datePart + ' ' + timePart;
          console.log('处理后的时间格式:', processedTime);
        }
      }
      
      const itemDate = new Date(processedTime);
      const itemYear = itemDate.getFullYear();
      const itemMonth = itemDate.getMonth();
      const itemDay = itemDate.getDate();
      
      console.log('记录日期:', itemYear, itemMonth, itemDay);
      const isMatch = itemYear === targetYear && itemMonth === targetMonth && itemDay === targetDay;
      console.log('日期匹配:', isMatch);
      
      return isMatch;
    });
    console.log('日期筛选后长度:', result.length);
  }
  
  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item => 
      item.patientName && item.patientName.toLowerCase().includes(query) || 
      item.medicalRecordId && item.medicalRecordId.toString().includes(query) ||
      (item.doctorDescription && item.doctorDescription.toLowerCase().includes(query))
    );
    console.log('搜索筛选后长度:', result.length);
  }
  
  console.log('最终筛选结果长度:', result.length);
  return result;
});

const paginatedMedicalRecords = computed(() => {
  return filteredMedicalRecords.value;
});

// 方法
function getPrescriptionStatusType(status) {
  switch (status) {
    case 0: return 'info'; // 未使用
    case 1: return 'success'; // 已使用
    case 2: return 'warning'; // 未开具
    default: return 'info';
  }
}

function getPrescriptionStatusText(status) {
  switch (status) {
    case 0: return '未使用';
    case 1: return '已使用';
    case 2: return '未开具';
    default: return '未知';
  }
}

async function fetchMedicalRecords() {
  loading.value = true;
  // 清空之前的数据，避免显示缓存数据
  medicalRecords.value = [];
  
  try {
    // 构建分页参数
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };
    
    const response = await getMedicalRecordList(params);
    
    if (response.code === 200) {
      console.log('就诊记录API响应:', response);
      
      // 确保数据是数组格式
      if (response.data && Array.isArray(response.data)) {
        medicalRecords.value = response.data;
        total.value = response.data.length; // 如果后端没有返回总数，使用当前数据长度
        console.log('直接使用数组数据，长度:', medicalRecords.value.length);
      } else if (response.data && Array.isArray(response.data.list)) {
        // 如果数据在list字段中
        medicalRecords.value = response.data.list;
        total.value = response.data.total || response.data.list.length;
        console.log('使用list数据，长度:', medicalRecords.value.length);
      } else if (response.data && Array.isArray(response.data.records)) {
        // 如果数据在records字段中
        medicalRecords.value = response.data.records;
        total.value = response.data.total || response.data.records.length;
        console.log('使用records数据，长度:', medicalRecords.value.length);
      } else {
        // 如果data为null或空，清空数据
        medicalRecords.value = [];
        total.value = 0;
        console.log('API返回空数据，清空medicalRecords');
      }
      
      console.log('最终medicalRecords数据:', medicalRecords.value);
    } else {
      console.error('API响应错误:', response);
      ElMessage.error(`获取就诊记录失败: ${response.message || '未知错误'}`);
      medicalRecords.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取就诊记录失败:', error);
    ElMessage.error(`获取就诊记录失败: ${error.message}`);
    medicalRecords.value = [];
    total.value = 0;
  } finally {
    loading.value = false;
  }
}

function refreshMedicalRecords() {
  currentPage.value = 1; // 重置到第一页
  fetchMedicalRecords();
  ElMessage.success('就诊记录已刷新');
}

function handleFilterChange() {
  currentPage.value = 1; // 重置到第一页
  fetchMedicalRecords();
}

function handleDateChange() {
  currentPage.value = 1; // 重置到第一页
  fetchMedicalRecords();
}

function handleSearch() {
  currentPage.value = 1; // 重置到第一页
  fetchMedicalRecords();
}

// 分页处理函数
function handleSizeChange(newSize) {
  pageSize.value = newSize;
  currentPage.value = 1; // 重置到第一页
  fetchMedicalRecords();
}

function handleCurrentChange(newPage) {
  currentPage.value = newPage;
  fetchMedicalRecords();
}

function handleRowClick(row) {
  viewMedicalRecordDetail(row);
}

function viewMedicalRecordDetail(medicalRecord) {
  selectedMedicalRecord.value = { ...medicalRecord };
  medicalRecordDrawerVisible.value = true;
}

async function viewPrescriptionFromRecord() {
  if (selectedMedicalRecord.value) {
    try {
      const response = await getPrescriptionInfoByMedicalRecordId(selectedMedicalRecord.value.medicalRecordId);
      
      if (response.code === 200 && response.data) {
        // 合并原有的处方信息和后端获取的药品数据
        selectedPrescription.value = {
          ...selectedMedicalRecord.value.prescription, // 保留原有的处方基本信息
          id: selectedMedicalRecord.value.medicalRecordId, // 设置处方编号
          drugs: response.data, // 使用后端获取的药品数据
          medicineCount: response.data.length,
          total_amount: calculateTotalPrice(response.data)
        };
        prescriptionDrawerVisible.value = true;
      } else {
        ElMessage.error(`获取处方信息失败: ${response.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('获取处方信息失败:', error);
      ElMessage.error(`获取处方信息失败: ${error.message}`);
    }
  }
}

async function viewPrescriptionDetail(medicalRecord) {
  try {
    const response = await getPrescriptionInfoByMedicalRecordId(medicalRecord.medicalRecordId);
    
    if (response.code === 200 && response.data) {
      // 合并原有的处方信息和后端获取的药品数据
      selectedPrescription.value = {
        ...medicalRecord.prescription, // 保留原有的处方基本信息
        id: medicalRecord.medicalRecordId, // 设置处方编号
        drugs: response.data, // 使用后端获取的药品数据
        medicineCount: response.data.length,
        total_amount: calculateTotalPrice(response.data)
      };
      prescriptionDrawerVisible.value = true;
    } else {
      ElMessage.error(`获取处方信息失败: ${response.message || '未知错误'}`);
    }
  } catch (error) {
    console.error('获取处方信息失败:', error);
    ElMessage.error(`获取处方信息失败: ${error.message}`);
  }
}

function printPrescription() {
  ElMessage.success('处方打印功能已触发');
  // 实际项目中可以调用打印API或使用浏览器打印功能
  // window.print();
}

// 辅助函数：计算总金额
function calculateTotalPrice(drugs) {
  if (!drugs || drugs.length === 0) {
    return 0;
  }
  return drugs.reduce((sum, drug) => sum + (drug.price || 0) * (drug.drugQuantity || 0), 0);
}

// 生命周期钩子
onMounted(() => {
  fetchMedicalRecords();
});
</script>

<style scoped>
.medical-records-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--app-text);
  letter-spacing: -0.2px;
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.medical-record-card {
  margin-bottom: 20px;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-actions {
  display: flex;
  gap: 8px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 就诊记录详情样式 */
.medical-record-detail {
  padding: 12px 20px 20px;
}

.prescription-detail {
  padding: 12px 20px 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--app-border);
}

.record-id, .prescription-id {
  font-size: 16px;
  font-weight: 600;
  color: var(--app-text);
}

.patient-section {
  display: flex;
  align-items: center;
  margin: 20px 0;
}

.patient-info-detail {
  margin-left: 16px;
}

.patient-info-detail h3 {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 500;
}

.patient-info-detail p {
  margin: 0 0 4px 0;
  color: var(--app-text-muted);
  font-size: 14px;
}

.detail-content {
  padding: 16px 0;
}

.detail-item {
  margin-bottom: 12px;
  display: flex;
}

.item-label {
  width: 100px;
  color: var(--app-text-muted);
  font-size: 14px;
}

.item-value {
  flex: 1;
  font-size: 14px;
  color: var(--app-text);
}

.price-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding: 16px;
  background: rgba(15, 23, 42, 0.03);
  border: 1px solid var(--app-border);
  border-radius: 12px;
}

.price-item {
  margin-left: 24px;
  font-size: 14px;
}

.total-price {
  font-size: 18px;
  font-weight: 500;
  color: var(--el-color-danger);
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .header-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .price-summary {
    flex-direction: column;
    gap: 8px;
  }
  
  .price-item {
    margin-left: 0;
  }
}

/* 动画效果 */
.el-table :deep(tbody tr) {
  transition: all 0.3s;
  cursor: pointer;
}

.el-table :deep(tbody tr:hover) {
  transform: translateY(-1px);
  box-shadow: var(--app-shadow-sm);
  z-index: 1;
  position: relative;
}

:deep(.el-drawer__body) {
  padding: 0;
}

.detail-header, .patient-section, .detail-content, .detail-actions {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 表格样式增强 */
.el-table :deep(th) {
  background-color: rgba(15, 23, 42, 0.04) !important;
}

.el-table :deep(.el-table__row:nth-child(even)) {
  background-color: rgba(15, 23, 42, 0.015);
}
</style>
