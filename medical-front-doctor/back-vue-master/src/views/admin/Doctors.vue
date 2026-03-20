<template>
  <div class="doctors-container">
    <div class="page-header">
      <h2>医生管理</h2>
      <div class="header-actions">
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleFilterChange">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-input
          v-model="searchQuery"
          placeholder="搜索用户名"
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
          添加医生
        </el-button>
      </div>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="doctorList" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="140" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openDetailDialog(row)">详情</el-button>
            <el-button
              size="small"
              type="warning"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <el-dialog v-model="createDialogVisible" title="新增医生" width="640px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-divider content-position="left">账号信息</el-divider>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="createForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="createForm.email" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="createForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>

        <el-divider content-position="left">医生信息</el-divider>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="身份证" prop="idCard">
          <el-input v-model="createForm.idCard" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="createForm.title" />
        </el-form-item>
        <el-form-item label="所属子科室" prop="subDepartmentId">
          <el-select v-model="createForm.subDepartmentId" placeholder="选择子科室" filterable>
            <el-option
              v-for="item in subDepartmentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挂号费" prop="price">
          <el-input-number v-model="createForm.price" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="执业证号" prop="professionalLicenseNumber">
          <el-input v-model="createForm.professionalLicenseNumber" />
        </el-form-item>
        <el-form-item label="简介" prop="introduction">
          <el-input v-model="createForm.introduction" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="医生详情" width="560px">
      <el-form ref="detailFormRef" :model="detailForm" :rules="detailRules" label-width="110px">
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="detailForm.realName" />
        </el-form-item>
        <el-form-item label="身份证" prop="idCard">
          <el-input v-model="detailForm.idCard" />
        </el-form-item>
        <el-form-item label="职称" prop="title">
          <el-input v-model="detailForm.title" />
        </el-form-item>
        <el-form-item label="所属子科室" prop="subDepartmentId">
          <el-select v-model="detailForm.subDepartmentId" placeholder="选择子科室" filterable>
            <el-option
              v-for="item in subDepartmentOptions"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="挂号费" prop="price">
          <el-input-number v-model="detailForm.price" :min="0" :max="1000" />
        </el-form-item>
        <el-form-item label="执业证号" prop="professionalLicenseNumber">
          <el-input v-model="detailForm.professionalLicenseNumber" />
        </el-form-item>
        <el-form-item label="简介" prop="introduction">
          <el-input v-model="detailForm.introduction" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="submitDetailUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import {
  getDoctorList,
  getDoctorDetailByUserId,
  createDoctor,
  updateDoctorDetail,
  removeDoctor,
  updateDoctorStatus,
  getDepartments,
  getSubDepartments
} from '@/api/admin';

const loading = ref(false);
const doctorList = ref([]);
const filterStatus = ref(null);
const searchQuery = ref('');

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
});

const subDepartmentOptions = ref([]);

const createDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const createFormRef = ref(null);
const detailFormRef = ref(null);

const createForm = reactive({
  username: '',
  password: '',
  email: '',
  status: 1,
  realName: '',
  idCard: '',
  title: '',
  subDepartmentId: null,
  price: 0,
  professionalLicenseNumber: '',
  introduction: ''
});

const detailForm = reactive({
  id: null,
  realName: '',
  idCard: '',
  title: '',
  subDepartmentId: null,
  price: 0,
  professionalLicenseNumber: '',
  introduction: ''
});

const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCard: [{ required: true, message: '请输入身份证', trigger: 'blur' }],
  title: [{ required: true, message: '请输入职称', trigger: 'blur' }],
  subDepartmentId: [{ required: true, message: '请选择子科室', trigger: 'change' }]
};

const detailRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  title: [{ required: true, message: '请输入职称', trigger: 'blur' }],
  subDepartmentId: [{ required: true, message: '请选择子科室', trigger: 'change' }]
};

const fetchSubDepartments = async () => {
  const depRes = await getDepartments();
  if (depRes.code !== 200 || !Array.isArray(depRes.data)) {
    subDepartmentOptions.value = [];
    return;
  }
  const allOptions = [];
  for (const dep of depRes.data) {
    const subRes = await getSubDepartments(dep.id);
    if (subRes.code === 200 && Array.isArray(subRes.data)) {
      subRes.data.forEach(sub => {
        allOptions.push({
          id: sub.id,
          label: `${dep.departmentName} - ${sub.departmentName}`
        });
      });
    }
  }
  subDepartmentOptions.value = allOptions;
};

const fetchDoctors = async () => {
  loading.value = true;
  try {
    const res = await getDoctorList(
      {
        pageNum: pagination.currentPage,
        pageSize: pagination.pageSize
      },
      {
        username: searchQuery.value || undefined,
        status: filterStatus.value
      }
    );
    if (res.code === 200 && res.data) {
      doctorList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      doctorList.value = [];
      pagination.total = 0;
    }
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pagination.currentPage = 1;
  fetchDoctors();
};

const handleSizeChange = (size) => {
  pagination.pageSize = size;
  pagination.currentPage = 1;
  fetchDoctors();
};

const handleCurrentChange = (page) => {
  pagination.currentPage = page;
  fetchDoctors();
};

const refreshList = () => {
  fetchDoctors();
};

const openCreateDialog = () => {
  Object.assign(createForm, {
    username: '',
    password: '',
    email: '',
    status: 1,
    realName: '',
    idCard: '',
    title: '',
    subDepartmentId: null,
    price: 0,
    professionalLicenseNumber: '',
    introduction: ''
  });
  createDialogVisible.value = true;
};

const submitCreate = () => {
  createFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const res = await createDoctor({ ...createForm });
    if (res.code === 200) {
      ElMessage.success('新增医生成功');
      createDialogVisible.value = false;
      fetchDoctors();
    } else {
      ElMessage.error(res.message || '新增失败');
    }
  });
};

const openDetailDialog = async (row) => {
  const res = await getDoctorDetailByUserId(row.id);
  if (res.code === 200 && res.data) {
    Object.assign(detailForm, {
      id: res.data.id,
      realName: res.data.realName,
      idCard: res.data.idCard,
      title: res.data.title,
      subDepartmentId: res.data.subDepartmentId,
      price: res.data.price,
      professionalLicenseNumber: res.data.professionalLicenseNumber,
      introduction: res.data.introduction
    });
    detailDialogVisible.value = true;
  } else {
    ElMessage.error(res.message || '获取详情失败');
  }
};

const submitDetailUpdate = () => {
  detailFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const res = await updateDoctorDetail({ ...detailForm });
    if (res.code === 200) {
      ElMessage.success('更新成功');
      detailDialogVisible.value = false;
      fetchDoctors();
    } else {
      ElMessage.error(res.message || '更新失败');
    }
  });
};

const toggleStatus = async (row) => {
  const targetStatus = row.status === 1 ? 0 : 1;
  const res = await updateDoctorStatus(row.id, targetStatus);
  if (res.code === 200) {
    ElMessage.success('状态已更新');
    fetchDoctors();
  } else {
    ElMessage.error(res.message || '更新失败');
  }
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该医生账号吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await removeDoctor(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchDoctors();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

onMounted(async () => {
  await fetchSubDepartments();
  await fetchDoctors();
});
</script>

<style scoped>
.doctors-container {
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
