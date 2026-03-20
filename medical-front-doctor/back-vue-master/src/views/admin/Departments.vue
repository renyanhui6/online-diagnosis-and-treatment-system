<template>
  <div class="departments-container">
    <div class="page-header">
      <h2>科室管理</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshDepartments">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="openDepartmentDialog">
          <el-icon><Plus /></el-icon>
          添加科室
        </el-button>
        <el-button type="warning" @click="openSubDialog" :disabled="departments.length === 0">
          <el-icon><Plus /></el-icon>
          添加子科室
        </el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>科室列表</span>
            </div>
          </template>
          <el-table
            v-loading="loading"
            :data="departments"
            highlight-current-row
            @current-change="handleDepartmentSelect"
          >
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="departmentName" label="科室名称" />
            <el-table-column prop="description" label="简介" show-overflow-tooltip />
            <el-table-column prop="createTime" label="创建时间" width="160" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="handleRemoveDepartment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header">
              <span>子科室列表</span>
              <span class="sub-title">{{ currentDepartmentName }}</span>
            </div>
          </template>
          <el-table v-loading="subLoading" :data="subDepartments">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="departmentName" label="子科室名称" />
            <el-table-column prop="description" label="简介" show-overflow-tooltip />
            <el-table-column prop="treatmentScope" label="诊疗范围" show-overflow-tooltip />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button size="small" type="danger" @click="handleRemoveSubDepartment(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="departmentDialogVisible" title="添加科室" width="480px">
      <el-form :model="departmentForm" :rules="departmentRules" ref="departmentFormRef" label-width="100px">
        <el-form-item label="科室名称" prop="departmentName">
          <el-input v-model="departmentForm.departmentName" />
        </el-form-item>
        <el-form-item label="科室简介" prop="description">
          <el-input v-model="departmentForm.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="departmentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitDepartment">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="subDialogVisible" title="添加子科室" width="560px">
      <el-form :model="subForm" :rules="subRules" ref="subFormRef" label-width="110px">
        <el-form-item label="所属科室" prop="parentDepartmentId">
          <el-select v-model="subForm.parentDepartmentId" placeholder="选择科室" filterable>
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.departmentName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="子科室名称" prop="departmentName">
          <el-input v-model="subForm.departmentName" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="subForm.description" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="诊疗范围" prop="treatmentScope">
          <el-input v-model="subForm.treatmentScope" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="科室特色" prop="departmentFeatures">
          <el-input v-model="subForm.departmentFeatures" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="科室图片" prop="imageFile">
          <el-upload
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary">选择图片</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSubDepartment">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import {
  getDepartments,
  getSubDepartments,
  addDepartment,
  addSubDepartment,
  removeDepartment,
  removeSubDepartment
} from '@/api/admin';

const loading = ref(false);
const subLoading = ref(false);
const departments = ref([]);
const subDepartments = ref([]);
const currentDepartmentId = ref(null);

const departmentDialogVisible = ref(false);
const subDialogVisible = ref(false);
const departmentFormRef = ref(null);
const subFormRef = ref(null);

const departmentForm = reactive({
  departmentName: '',
  description: ''
});

const subForm = reactive({
  parentDepartmentId: null,
  departmentName: '',
  description: '',
  treatmentScope: '',
  departmentFeatures: '',
  imageFile: null
});

const departmentRules = {
  departmentName: [{ required: true, message: '请输入科室名称', trigger: 'blur' }]
};

const subRules = {
  parentDepartmentId: [{ required: true, message: '请选择所属科室', trigger: 'change' }],
  departmentName: [{ required: true, message: '请输入子科室名称', trigger: 'blur' }],
  imageFile: [{ required: true, message: '请上传科室图片', trigger: 'change' }]
};

const currentDepartmentName = computed(() => {
  const match = departments.value.find(item => item.id === currentDepartmentId.value);
  return match ? `（${match.departmentName}）` : '';
});

const refreshDepartments = async () => {
  loading.value = true;
  try {
    const res = await getDepartments();
    if (res.code === 200 && Array.isArray(res.data)) {
      departments.value = res.data;
    } else {
      departments.value = [];
    }
  } finally {
    loading.value = false;
  }
};

const loadSubDepartments = async (departmentId) => {
  if (!departmentId) {
    subDepartments.value = [];
    return;
  }
  subLoading.value = true;
  try {
    const res = await getSubDepartments(departmentId);
    if (res.code === 200 && Array.isArray(res.data)) {
      subDepartments.value = res.data;
    } else {
      subDepartments.value = [];
    }
  } finally {
    subLoading.value = false;
  }
};

const handleDepartmentSelect = (row) => {
  if (!row) return;
  currentDepartmentId.value = row.id;
  loadSubDepartments(row.id);
};

const openDepartmentDialog = () => {
  departmentForm.departmentName = '';
  departmentForm.description = '';
  departmentDialogVisible.value = true;
};

const openSubDialog = () => {
  subForm.parentDepartmentId = currentDepartmentId.value || null;
  subForm.departmentName = '';
  subForm.description = '';
  subForm.treatmentScope = '';
  subForm.departmentFeatures = '';
  subForm.imageFile = null;
  subDialogVisible.value = true;
};

const handleFileChange = (file) => {
  subForm.imageFile = file.raw;
  if (subFormRef.value) {
    subFormRef.value.validateField('imageFile');
  }
};

const handleFileRemove = () => {
  subForm.imageFile = null;
  if (subFormRef.value) {
    subFormRef.value.validateField('imageFile');
  }
};

const submitDepartment = () => {
  departmentFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const res = await addDepartment({
      departmentName: departmentForm.departmentName,
      description: departmentForm.description
    });
    if (res.code === 200) {
      ElMessage.success('新增科室成功');
      departmentDialogVisible.value = false;
      refreshDepartments();
    } else {
      ElMessage.error(res.message || '新增失败');
    }
  });
};

const submitSubDepartment = () => {
  subFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const formData = new FormData();
    formData.append('parentDepartmentId', subForm.parentDepartmentId);
    formData.append('departmentName', subForm.departmentName);
    formData.append('description', subForm.description || '');
    formData.append('treatmentScope', subForm.treatmentScope || '');
    formData.append('departmentFeatures', subForm.departmentFeatures || '');
    formData.append('imageFile', subForm.imageFile);
    const res = await addSubDepartment(formData);
    if (res.code === 200) {
      ElMessage.success('新增子科室成功');
      subDialogVisible.value = false;
      loadSubDepartments(subForm.parentDepartmentId);
    } else {
      ElMessage.error(res.message || '新增失败');
    }
  });
};

const handleRemoveDepartment = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该科室吗？若存在子科室将无法删除。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await removeDepartment(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      if (currentDepartmentId.value === row.id) {
        currentDepartmentId.value = null;
        subDepartments.value = [];
      }
      refreshDepartments();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

const handleRemoveSubDepartment = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该子科室吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await removeSubDepartment(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      loadSubDepartments(currentDepartmentId.value);
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

onMounted(async () => {
  await refreshDepartments();
});
</script>

<style scoped>
.departments-container {
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

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.sub-title {
  color: rgba(15, 23, 42, 0.6);
  font-size: 12px;
}
</style>
