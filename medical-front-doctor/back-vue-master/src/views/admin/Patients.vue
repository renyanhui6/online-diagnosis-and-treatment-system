<template>
  <div class="patients-container">
    <div class="page-header">
      <h2>患者管理</h2>
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
          添加患者
        </el-button>
      </div>
    </div>

    <el-card shadow="hover">
      <el-table v-loading="loading" :data="patientList" style="width: 100%">
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

    <el-dialog v-model="createDialogVisible" title="新增患者" width="640px">
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

        <el-divider content-position="left">患者信息</el-divider>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="createForm.nickname" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="createForm.realName" />
        </el-form-item>
        <el-form-item label="身份证" prop="idCard">
          <el-input v-model="createForm.idCard" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="createForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="createForm.phone" />
        </el-form-item>
        <el-form-item label="地址" prop="homeAddress">
          <el-input v-model="createForm.homeAddress" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialogVisible" title="患者详情" width="560px">
      <el-form ref="detailFormRef" :model="detailForm" :rules="detailRules" label-width="110px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="detailForm.nickname" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="detailForm.realName" />
        </el-form-item>
        <el-form-item label="身份证" prop="idCard">
          <el-input v-model="detailForm.idCard" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="detailForm.gender">
            <el-radio :label="1">男</el-radio>
            <el-radio :label="2">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="detailForm.phone" />
        </el-form-item>
        <el-form-item label="地址" prop="homeAddress">
          <el-input v-model="detailForm.homeAddress" />
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
  getPatientList,
  getPatientDetailByUserId,
  createPatient,
  updatePatientDetail,
  removePatient,
  updatePatientStatus
} from '@/api/admin';

const loading = ref(false);
const patientList = ref([]);
const filterStatus = ref(null);
const searchQuery = ref('');

const pagination = reactive({
  currentPage: 1,
  pageSize: 10,
  total: 0
});

const createDialogVisible = ref(false);
const detailDialogVisible = ref(false);
const createFormRef = ref(null);
const detailFormRef = ref(null);

const createForm = reactive({
  username: '',
  password: '',
  email: '',
  status: 1,
  nickname: '',
  realName: '',
  idCard: '',
  gender: 1,
  phone: '',
  homeAddress: ''
});

const detailForm = reactive({
  id: null,
  systemUserId: null,
  nickname: '',
  realName: '',
  idCard: '',
  gender: 1,
  phone: '',
  homeAddress: ''
});

const createRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /.+@.+\..+/, message: '请输入正确的邮箱', trigger: 'blur' }
  ]
};

const detailRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
};

const fetchPatients = async () => {
  loading.value = true;
  try {
    const res = await getPatientList(
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
      patientList.value = res.data.records || [];
      pagination.total = res.data.total || 0;
    } else {
      patientList.value = [];
      pagination.total = 0;
    }
  } finally {
    loading.value = false;
  }
};

const handleFilterChange = () => {
  pagination.currentPage = 1;
  fetchPatients();
};

const handleSizeChange = (size) => {
  pagination.pageSize = size;
  pagination.currentPage = 1;
  fetchPatients();
};

const handleCurrentChange = (page) => {
  pagination.currentPage = page;
  fetchPatients();
};

const refreshList = () => {
  fetchPatients();
};

const openCreateDialog = () => {
  Object.assign(createForm, {
    username: '',
    password: '',
    email: '',
    status: 1,
    nickname: '',
    realName: '',
    idCard: '',
    gender: 1,
    phone: '',
    homeAddress: ''
  });
  createDialogVisible.value = true;
};

const submitCreate = () => {
  createFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const res = await createPatient({ ...createForm });
    if (res.code === 200) {
      ElMessage.success('新增患者成功');
      createDialogVisible.value = false;
      fetchPatients();
    } else {
      ElMessage.error(res.message || '新增失败');
    }
  });
};

const openDetailDialog = async (row) => {
  const res = await getPatientDetailByUserId(row.id);
  if (res.code === 200 && res.data) {
    Object.assign(detailForm, {
      id: res.data.id,
      systemUserId: res.data.systemUserId,
      nickname: res.data.nickname,
      realName: res.data.realName,
      idCard: res.data.idCard,
      gender: res.data.gender ?? 1,
      phone: res.data.phone,
      homeAddress: res.data.homeAddress
    });
    detailDialogVisible.value = true;
  } else {
    ElMessage.error(res.message || '获取详情失败');
  }
};

const submitDetailUpdate = () => {
  detailFormRef.value.validate(async (valid) => {
    if (!valid) return;
    const res = await updatePatientDetail({ ...detailForm });
    if (res.code === 200) {
      ElMessage.success('更新成功');
      detailDialogVisible.value = false;
      fetchPatients();
    } else {
      ElMessage.error(res.message || '更新失败');
    }
  });
};

const toggleStatus = async (row) => {
  const targetStatus = row.status === 1 ? 0 : 1;
  const res = await updatePatientStatus(row.id, targetStatus);
  if (res.code === 200) {
    ElMessage.success('状态已更新');
    fetchPatients();
  } else {
    ElMessage.error(res.message || '更新失败');
  }
};

const removeRow = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该患者账号吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    const res = await removePatient(row.id);
    if (res.code === 200) {
      ElMessage.success('删除成功');
      fetchPatients();
    } else {
      ElMessage.error(res.message || '删除失败');
    }
  } catch (e) {
    // ignore
  }
};

onMounted(() => {
  fetchPatients();
});
</script>

<style scoped>
.patients-container {
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
