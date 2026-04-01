<template>
  <div class="medical-records-container">
    <section class="records-hero">
      <div class="hero-copy">
        <span class="hero-badge">病历工作台</span>
        <h2>就诊记录管理</h2>
        <p>
          集中查看当前医生名下的病历记录、处方状态与患者信息，支持多维筛选和详情追溯。
        </p>
      </div>

      <div class="hero-side">
        <div class="hero-kpi">
          <span>当前记录总数</span>
          <strong>{{ medicalRecords.length }}</strong>
          <small>已同步到当前页面的数据量</small>
        </div>
        <div class="hero-kpi muted">
          <span>筛选结果</span>
          <strong>{{ filteredMedicalRecords.length }}</strong>
          <small>当前条件下可查看的病历数量</small>
        </div>
      </div>
    </section>

    <section class="stats-grid">
      <article class="stat-card">
        <span class="stat-label">全部记录</span>
        <strong>{{ recordStats.total }}</strong>
        <small>当前医生的全部病历数量</small>
      </article>
      <article class="stat-card accent-blue">
        <span class="stat-label">处方未使用</span>
        <strong>{{ recordStats.unused }}</strong>
        <small>已开处方但尚未使用的记录</small>
      </article>
      <article class="stat-card accent-green">
        <span class="stat-label">处方已使用</span>
        <strong>{{ recordStats.used }}</strong>
        <small>已完成用药流转的病历记录</small>
      </article>
      <article class="stat-card accent-amber">
        <span class="stat-label">尚未开方</span>
        <strong>{{ recordStats.notIssued }}</strong>
        <small>仅有病历记录，尚未开具处方</small>
      </article>
    </section>

    <el-card shadow="hover" class="filter-card">
      <div class="filter-header">
        <div>
          <h3>筛选条件</h3>
          <p>按处方状态、就诊日期和患者关键字快速定位目标病历。</p>
        </div>
        <el-button type="primary" @click="refreshMedicalRecords">
          <el-icon><Refresh /></el-icon>
          刷新数据
        </el-button>
      </div>

      <div class="filter-grid">
        <el-select v-model="filterStatus" placeholder="就诊状态" clearable @change="handleFilterChange">
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
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
      </div>
    </el-card>

    <el-card shadow="hover" class="medical-record-card">
      <template #header>
        <div class="table-header">
          <div>
            <h3>病历列表</h3>
            <p>当前展示 {{ filteredMedicalRecords.length }} 条记录，支持点击整行查看详情。</p>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="paginatedMedicalRecords"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column prop="medicalRecordId" label="就诊记录ID" width="220" />
        <el-table-column prop="patientName" label="患者姓名" min-width="220">
          <template #default="{ row }">
            <div class="patient-info">
              <el-avatar :size="28" :src="row.patientAvatar" />
              <div class="patient-copy">
                <span>{{ row.patientName }}</span>
                <small>{{ row.patientPhone || '未留电话' }}</small>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="doctorDescription" label="医生描述" min-width="260" show-overflow-tooltip />
        <el-table-column prop="isPurchasable" label="处方状态" width="140">
          <template #default="{ row }">
            <el-tag :type="getPrescriptionStatusType(row.isPurchasable)">
              {{ getPrescriptionStatusText(row.isPurchasable) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="就诊时间" min-width="180" sortable />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click.stop="viewMedicalRecordDetail(row)"
            >
              查看详情
            </el-button>
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
          <el-button type="info" @click="medicalRecordDrawerVisible = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>

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
          <el-button type="info" @click="prescriptionDrawerVisible = false">关闭</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { Search, Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getMedicalRecordList, getPrescriptionInfoByMedicalRecordId } from '@/api/doctor';

const loading = ref(false);
const medicalRecords = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterStatus = ref('');
const selectedDate = ref('');
const searchQuery = ref('');
const total = ref(0);

const medicalRecordDrawerVisible = ref(false);
const prescriptionDrawerVisible = ref(false);
const selectedMedicalRecord = ref(null);
const selectedPrescription = ref(null);

const statusOptions = [
  { value: 0, label: '未使用' },
  { value: 1, label: '已使用' },
  { value: 2, label: '未开具' }
];

const filteredMedicalRecords = computed(() => {
  const records = Array.isArray(medicalRecords.value) ? medicalRecords.value : [];
  let result = [...records];

  if (filterStatus.value !== '' && filterStatus.value !== null && filterStatus.value !== undefined) {
    result = result.filter(item => item.isPurchasable === filterStatus.value);
  }

  if (selectedDate.value) {
    const targetDate = new Date(selectedDate.value);
    const targetYear = targetDate.getFullYear();
    const targetMonth = targetDate.getMonth();
    const targetDay = targetDate.getDate();

    result = result.filter(item => {
      if (!item.createTime) return false;

      let processedTime = item.createTime;
      if (processedTime.includes('-')) {
        const parts = processedTime.split(' ');
        if (parts.length === 2) {
          processedTime = `${parts[0]} ${parts[1].replace(/-/g, ':')}`;
        }
      }

      const itemDate = new Date(processedTime);
      return (
        itemDate.getFullYear() === targetYear &&
        itemDate.getMonth() === targetMonth &&
        itemDate.getDate() === targetDay
      );
    });
  }

  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item =>
      (item.patientName && item.patientName.toLowerCase().includes(query)) ||
      (item.medicalRecordId && item.medicalRecordId.toString().includes(query)) ||
      (item.doctorDescription && item.doctorDescription.toLowerCase().includes(query))
    );
  }

  return result;
});

const paginatedMedicalRecords = computed(() => filteredMedicalRecords.value);

const recordStats = computed(() => {
  const records = Array.isArray(medicalRecords.value) ? medicalRecords.value : [];
  return {
    total: records.length,
    unused: records.filter(item => item.isPurchasable === 0).length,
    used: records.filter(item => item.isPurchasable === 1).length,
    notIssued: records.filter(item => item.isPurchasable === 2).length
  };
});

function getPrescriptionStatusType(status) {
  switch (status) {
    case 0:
      return 'info';
    case 1:
      return 'success';
    case 2:
      return 'warning';
    default:
      return 'info';
  }
}

function getPrescriptionStatusText(status) {
  switch (status) {
    case 0:
      return '未使用';
    case 1:
      return '已使用';
    case 2:
      return '未开具';
    default:
      return '未知';
  }
}

async function fetchMedicalRecords() {
  loading.value = true;
  medicalRecords.value = [];

  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };

    const response = await getMedicalRecordList(params);

    if (response.code === 200) {
      if (response.data && Array.isArray(response.data)) {
        medicalRecords.value = response.data;
        total.value = response.data.length;
      } else if (response.data && Array.isArray(response.data.list)) {
        medicalRecords.value = response.data.list;
        total.value = response.data.total || response.data.list.length;
      } else if (response.data && Array.isArray(response.data.records)) {
        medicalRecords.value = response.data.records;
        total.value = response.data.total || response.data.records.length;
      } else {
        medicalRecords.value = [];
        total.value = 0;
      }
      return;
    }

    ElMessage.error(`获取就诊记录失败: ${response.message || '未知错误'}`);
    medicalRecords.value = [];
    total.value = 0;
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
  currentPage.value = 1;
  fetchMedicalRecords();
  ElMessage.success('就诊记录已刷新');
}

function handleFilterChange() {
  currentPage.value = 1;
  fetchMedicalRecords();
}

function handleDateChange() {
  currentPage.value = 1;
  fetchMedicalRecords();
}

function handleSearch() {
  currentPage.value = 1;
  fetchMedicalRecords();
}

function handleSizeChange(newSize) {
  pageSize.value = newSize;
  currentPage.value = 1;
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
  if (!selectedMedicalRecord.value) return;

  try {
    const response = await getPrescriptionInfoByMedicalRecordId(selectedMedicalRecord.value.medicalRecordId);
    if (response.code === 200 && response.data) {
      selectedPrescription.value = {
        ...selectedMedicalRecord.value.prescription,
        id: selectedMedicalRecord.value.medicalRecordId,
        drugs: response.data,
        medicineCount: response.data.length,
        total_amount: calculateTotalPrice(response.data)
      };
      prescriptionDrawerVisible.value = true;
      return;
    }
    ElMessage.error(`获取处方信息失败: ${response.message || '未知错误'}`);
  } catch (error) {
    console.error('获取处方信息失败:', error);
    ElMessage.error(`获取处方信息失败: ${error.message}`);
  }
}

function calculateTotalPrice(drugs) {
  if (!drugs || drugs.length === 0) return 0;
  return drugs.reduce((sum, drug) => sum + (drug.price || 0) * (drug.drugQuantity || 0), 0);
}

onMounted(() => {
  fetchMedicalRecords();
});
</script>

<style scoped>
.medical-records-container {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.records-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(280px, 1fr);
  gap: 20px;
  padding: 28px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(59, 130, 246, 0.18), transparent 36%),
    radial-gradient(circle at top right, rgba(45, 212, 191, 0.12), transparent 30%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.98));
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: 0 28px 60px rgba(15, 23, 42, 0.1);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: #1d4ed8;
  background: rgba(219, 234, 254, 0.9);
}

.hero-copy h2 {
  margin: 14px 0 10px;
  font-size: 34px;
  line-height: 1.1;
  letter-spacing: -0.03em;
  color: #0f172a;
}

.hero-copy p {
  margin: 0;
  max-width: 700px;
  line-height: 1.8;
  color: #475569;
}

.hero-side {
  display: grid;
  gap: 14px;
}

.hero-kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.hero-kpi span,
.stat-label {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #64748b;
}

.hero-kpi strong {
  font-size: 30px;
  line-height: 1;
  color: #0f172a;
}

.hero-kpi small {
  color: #64748b;
  line-height: 1.6;
}

.hero-kpi.muted strong {
  color: #0f766e;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  padding: 20px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: #0f172a;
}

.stat-card small {
  display: block;
  margin-top: 8px;
  line-height: 1.65;
  color: #64748b;
}

.accent-blue strong {
  color: #1d4ed8;
}

.accent-green strong {
  color: #047857;
}

.accent-amber strong {
  color: #b45309;
}

.filter-card,
.medical-record-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 24px;
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
}

.filter-header,
.table-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.filter-header h3,
.table-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.filter-header p,
.table-header p {
  margin: 6px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.filter-grid {
  display: grid;
  grid-template-columns: 180px 220px minmax(220px, 1fr);
  gap: 14px;
  margin-top: 18px;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.patient-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.patient-copy span {
  font-weight: 600;
  color: #0f172a;
}

.patient-copy small {
  color: #64748b;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.medical-record-detail,
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

.record-id,
.prescription-id {
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
  font-weight: 600;
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
  font-weight: 600;
  color: var(--el-color-danger);
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.el-table :deep(tbody tr) {
  transition: all 0.25s;
  cursor: pointer;
}

.el-table :deep(tbody tr:hover) {
  transform: translateY(-1px);
  box-shadow: var(--app-shadow-sm);
  z-index: 1;
  position: relative;
}

.el-table :deep(th) {
  background-color: rgba(15, 23, 42, 0.04) !important;
}

.el-table :deep(.el-table__row:nth-child(even)) {
  background-color: rgba(15, 23, 42, 0.015);
}

:deep(.el-drawer__body) {
  padding: 0;
}

@media (max-width: 1200px) {
  .records-hero,
  .stats-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 900px) {
  .records-hero,
  .stats-grid,
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-header,
  .table-header {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 768px) {
  .records-hero {
    padding: 22px;
    border-radius: 24px;
  }

  .hero-copy h2 {
    font-size: 28px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .price-summary {
    flex-direction: column;
    gap: 8px;
  }

  .price-item {
    margin-left: 0;
  }
}
</style>
