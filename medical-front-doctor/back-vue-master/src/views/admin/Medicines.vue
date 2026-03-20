<template>
  <div class="medicines-container">
    <div class="page-header">
      <h2>药品管理</h2>
      <div class="header-actions">
        <el-input
          v-model="searchQuery"
          placeholder="搜索药品名称"
          clearable
          @input="handleFilterChange"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          添加药品
        </el-button>
      </div>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="medicineList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="genericName" label="药品名称" min-width="200" />
        <el-table-column prop="specification" label="规格" min-width="160" />
        <el-table-column prop="minimumSalesUnit" label="单位" width="100" />
        <el-table-column prop="drugPrice" label="单价(元)" width="120">
          <template #default="{ row }">
            {{ formatPrice(row.drugPrice) }}
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="库存" width="100" />
        <el-table-column prop="isPrescription" label="处方药" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isPrescription === 1 ? 'danger' : 'success'">
              {{ row.isPrescription === 1 ? '处方药' : '非处方药' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.currentPage"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="pagination.total"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑药品' : '新增药品'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="药品名称" prop="genericName">
          <el-input v-model="form.genericName" />
        </el-form-item>
        <el-form-item label="规格" prop="specification">
          <el-input v-model="form.specification" />
        </el-form-item>
        <el-form-item label="单位" prop="minimumSalesUnit">
          <el-input v-model="form.minimumSalesUnit" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="drugPrice">
          <el-input-number v-model="form.drugPrice" :min="0" :precision="2" :step="0.1" />
        </el-form-item>
        <el-form-item label="库存" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="处方药" prop="isPrescription">
          <el-radio-group v-model="form.isPrescription">
            <el-radio :label="1">处方药</el-radio>
            <el-radio :label="0">非处方药</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import {
  getDrugList,
  searchDrugList,
  addDrug,
  updateDrug,
  deleteDrug
} from '@/api/admin';

const loading = ref(false);
const medicineList = ref([]);
const searchQuery = ref('');

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
});

const dialogVisible = ref(false);
const isEdit = ref(false);
const formRef = ref(null);

const form = reactive({
  id: null,
  genericName: '',
  specification: '',
  minimumSalesUnit: '',
  drugPrice: 0,
  quantity: 1,
  isPrescription: 0
});

const rules = {
  genericName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  specification: [{ required: true, message: '请输入规格', trigger: 'blur' }],
  minimumSalesUnit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  drugPrice: [{ required: true, message: '请输入单价', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入库存', trigger: 'change' }]
};

const formatPrice = (value) => {
  if (value === null || value === undefined) return '0.00';
  const numberValue = Number(value);
  return Number.isNaN(numberValue) ? '0.00' : numberValue.toFixed(2);
};

const fetchMedicines = async () => {
  loading.value = true;
  try {
    const params = {
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize
    };
    const res = searchQuery.value
      ? await searchDrugList(params, searchQuery.value)
      : await getDrugList(params);
    if (res.code === 200 && res.data) {
      medicineList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      medicineList.value = [];
      pagination.total = 0;
    }
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pagination.currentPage = 1;
  fetchMedicines();
};

const handleSizeChange = (size) => {
  pagination.pageSize = size;
  pagination.currentPage = 1;
  fetchMedicines();
};

const handleCurrentChange = (page) => {
  pagination.currentPage = page;
  fetchMedicines();
};

const refreshList = () => {
  fetchMedicines();
};

const openCreateDialog = () => {
  isEdit.value = false;
  Object.assign(form, {
    id: null,
    genericName: '',
    specification: '',
    minimumSalesUnit: '',
    drugPrice: 0,
    quantity: 1,
    isPrescription: 0
  });
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEdit.value = true;
  Object.assign(form, {
    id: row.id,
    genericName: row.genericName,
    specification: row.specification,
    minimumSalesUnit: row.minimumSalesUnit,
    drugPrice: row.drugPrice,
    quantity: row.quantity,
    isPrescription: row.isPrescription ?? 0
  });
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    const payload = { ...form };
    const res = isEdit.value ? await updateDrug(payload) : await addDrug(payload);
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '新增成功');
      dialogVisible.value = false;
      fetchMedicines();
    } else {
      ElMessage.error(res.message || '操作失败');
    }
  });
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该药品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await deleteDrug(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchMedicines();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

onMounted(() => {
  fetchMedicines();
});
</script>

<style scoped>
.medicines-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
