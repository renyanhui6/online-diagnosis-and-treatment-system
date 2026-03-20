<template>
  <div class="schedule-templates-container">
    <div class="page-header">
      <h2>排班模板管理</h2>
      <div class="header-actions">
        <el-select
          v-model="filterDoctorId"
          placeholder="选择医生"
          clearable
          filterable
          @change="handleFilterChange"
        >
          <el-option
            v-for="doctor in doctorOptions"
            :key="doctor.id"
            :label="doctor.name"
            :value="doctor.id"
          />
        </el-select>

        <el-button type="warning" @click="handleGenerate('fill_missing')">
          <el-icon><Refresh /></el-icon>
          仅补缺失
        </el-button>
        <el-button type="danger" plain @click="handleGenerate('fill_and_clean_invalid')">
          <el-icon><Refresh /></el-icon>
          补缺失并清理
        </el-button>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon><Plus /></el-icon>
          新增模板
        </el-button>
      </div>
    </div>

    <el-card shadow="hover" class="templates-card">
      <el-table v-loading="loading" :data="templateList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="医生" min-width="160">
          <template #default="{ row }">
            {{ getDoctorName(row.doctorId) }} (ID: {{ row.doctorId }})
          </template>
        </el-table-column>
        <el-table-column prop="weekDay" label="星期" width="120">
          <template #default="{ row }">
            {{ weekDayText(row.weekDay) }}
          </template>
        </el-table-column>
        <el-table-column prop="morningLimit" label="上午号源" width="120" />
        <el-table-column prop="afternoonLimit" label="下午号源" width="120" />
        <el-table-column label="启用" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.isActive"
              :active-value="1"
              :inactive-value="0"
              @change="() => handleToggleActive(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeTemplate(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="form.doctorId" placeholder="选择医生" filterable>
            <el-option
              v-for="doctor in doctorOptions"
              :key="doctor.id"
              :label="doctor.name"
              :value="doctor.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="星期" prop="weekDay">
          <el-select v-model="form.weekDay" placeholder="选择星期">
            <el-option v-for="item in weekDayOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="上午号源" prop="morningLimit">
          <el-input-number v-model="form.morningLimit" :min="0" :max="200" />
        </el-form-item>
        <el-form-item label="下午号源" prop="afternoonLimit">
          <el-input-number v-model="form.afternoonLimit" :min="0" :max="200" />
        </el-form-item>
        <el-form-item label="启用" prop="isActive">
          <el-switch v-model="form.isActive" :active-value="1" :inactive-value="0" />
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
import { Plus, Refresh } from '@element-plus/icons-vue';
import {
  getScheduleTemplates,
  addScheduleTemplate,
  updateScheduleTemplate,
  removeScheduleTemplate,
  getDoctorDetailList,
  generateSchedules
} from '@/api/admin';

const loading = ref(false);
const templateList = ref([]);
const doctorOptions = ref([]);
const filterDoctorId = ref(null);

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
});

const dialogVisible = ref(false);
const dialogTitle = ref('新增模板');
const formRef = ref(null);
const form = reactive({
  id: null,
  doctorId: null,
  weekDay: null,
  morningLimit: 0,
  afternoonLimit: 0,
  isActive: 1
});

const weekDayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' }
];

const rules = {
  doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
  weekDay: [{ required: true, message: '请选择星期', trigger: 'change' }]
};

const weekDayText = (value) => {
  const match = weekDayOptions.find(item => item.value === value);
  return match ? match.label : '-';
};

const getDoctorName = (doctorId) => {
  const doctor = doctorOptions.value.find(item => item.id === doctorId);
  return doctor ? doctor.name : '未知';
};

const fetchDoctors = async () => {
  const res = await getDoctorDetailList();
  if (res.code === 200 && Array.isArray(res.data)) {
    doctorOptions.value = res.data.map(item => ({
      id: item.id,
      name: item.realName || `医生${item.id}`
    }));
  } else {
    doctorOptions.value = [];
  }
};

const fetchTemplates = async () => {
  loading.value = true;
  try {
    const res = await getScheduleTemplates({
      pageNum: pagination.currentPage,
      pageSize: pagination.pageSize,
      doctorId: filterDoctorId.value || undefined
    });
    if (res.code === 200 && res.data) {
      templateList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      templateList.value = [];
      pagination.total = 0;
    }
  } catch (e) {
    templateList.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pagination.currentPage = 1;
  fetchTemplates();
};

const handleSizeChange = (size) => {
  pagination.pageSize = size;
  pagination.currentPage = 1;
  fetchTemplates();
};

const handleCurrentChange = (page) => {
  pagination.currentPage = page;
  fetchTemplates();
};

const resetForm = () => {
  form.id = null;
  form.doctorId = null;
  form.weekDay = null;
  form.morningLimit = 0;
  form.afternoonLimit = 0;
  form.isActive = 1;
};

const openCreateDialog = () => {
  dialogTitle.value = '新增模板';
  resetForm();
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  dialogTitle.value = '编辑模板';
  form.id = row.id;
  form.doctorId = row.doctorId;
  form.weekDay = row.weekDay;
  form.morningLimit = row.morningLimit ?? 0;
  form.afternoonLimit = row.afternoonLimit ?? 0;
  form.isActive = row.isActive ?? 1;
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;
    if ((form.morningLimit || 0) <= 0 && (form.afternoonLimit || 0) <= 0) {
      ElMessage.warning('上午或下午号源至少填写一个');
      return;
    }
    const payload = {
      id: form.id,
      doctorId: form.doctorId,
      weekDay: form.weekDay,
      morningLimit: form.morningLimit,
      afternoonLimit: form.afternoonLimit,
      isActive: form.isActive
    };
    const res = form.id ? await updateScheduleTemplate(payload) : await addScheduleTemplate(payload);
    if (res.code === 200) {
      ElMessage.success('保存成功');
      dialogVisible.value = false;
      fetchTemplates();
    } else {
      ElMessage.error(res.message || '保存失败');
    }
  });
};

const handleToggleActive = async (row) => {
  const payload = {
    id: row.id,
    doctorId: row.doctorId,
    weekDay: row.weekDay,
    morningLimit: row.morningLimit,
    afternoonLimit: row.afternoonLimit,
    isActive: row.isActive
  };
  const res = await updateScheduleTemplate(payload);
  if (res.code !== 200) {
    ElMessage.error(res.message || '更新失败');
    row.isActive = row.isActive === 1 ? 0 : 1;
  }
};

const removeTemplate = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该模板吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await removeScheduleTemplate(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchTemplates();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

const generateModeText = (mode) => (
  mode === 'fill_and_clean_invalid' ? '补缺失并清理未来无效排班' : '仅补缺失排班'
);

const handleGenerate = async (mode) => {
  try {
    await ElMessageBox.confirm(`确认立即执行“${generateModeText(mode)}”吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await generateSchedules(undefined, mode);
    if (res.code === 200) {
      const createdCount = res.data?.createdCount ?? 0;
      const cleanedCount = res.data?.cleanedCount ?? 0;
      const fromDate = res.data?.fromDate || '-';
      const toDate = res.data?.toDate || '-';
      const actionText = mode === 'fill_and_clean_invalid'
        ? `新增排班 ${createdCount} 条，清理无效排班 ${cleanedCount} 条`
        : `新增排班 ${createdCount} 条`;
      ElMessage.success(`补偿完成，${actionText}，范围 ${fromDate} 至 ${toDate}`);
      fetchTemplates();
    } else {
      ElMessage.error(res.message || '补偿生成失败');
    }
  } catch (e) {
    // ignore
  }
};

onMounted(async () => {
  await fetchDoctors();
  await fetchTemplates();
});
</script>

<style scoped>
.schedule-templates-container {
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

.templates-card {
  border-radius: 12px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
