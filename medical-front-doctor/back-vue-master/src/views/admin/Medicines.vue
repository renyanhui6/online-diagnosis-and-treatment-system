<template>
  <div class="medicines-container">
    <div class="page-header">
      <h2>药品管理</h2>
      <div class="header-actions">
        <el-select v-model="filterCategory" placeholder="药品分类" clearable @change="handleFilterChange">
          <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-select v-model="filterManufacturer" placeholder="生产厂商" clearable @change="handleFilterChange">
          <el-option v-for="item in manufacturerOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-select v-model="filterStatus" placeholder="状态" clearable @change="handleFilterChange">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        
        <el-input
          v-model="searchQuery"
          placeholder="搜索药品名称/编码"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        
        <el-button type="primary" @click="refreshMedicines">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        
        <el-button type="success" @click="addMedicine">
          <el-icon><Plus /></el-icon>
          添加药品
        </el-button>
        
        <el-button type="warning" @click="importMedicines">
          <el-icon><Upload /></el-icon>
          批量导入
        </el-button>
        
        <el-button type="info" @click="exportMedicines">
          <el-icon><Download /></el-icon>
          导出数据
        </el-button>
      </div>
    </div>
    
    <el-card shadow="hover" class="medicines-card">
      <el-table
        v-loading="loading"
        :data="paginatedMedicines"
        style="width: 100%"
        @row-click="handleRowClick"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="medicine-expand">
              <div class="expand-section">
                <h4>药品详情</h4>
                <div class="expand-content">
                  <div class="expand-item">
                    <span class="expand-label">批准文号：</span>
                    <span>{{ props.row.approvalNumber }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">生产日期：</span>
                    <span>{{ props.row.productionDate }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">有效期至：</span>
                    <span>{{ props.row.expirationDate }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">生产厂商：</span>
                    <span>{{ props.row.manufacturer }}</span>
                  </div>
                  <div class="expand-item">
                    <span class="expand-label">存储条件：</span>
                    <span>{{ props.row.storageCondition }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">适应症：</span>
                    <span>{{ props.row.indications }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">用法用量：</span>
                    <span>{{ props.row.dosage }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">不良反应：</span>
                    <span>{{ props.row.sideEffects }}</span>
                  </div>
                  <div class="expand-item full-width">
                    <span class="expand-label">禁忌：</span>
                    <span>{{ props.row.contraindications }}</span>
                  </div>
                </div>
              </div>
              
              <div class="expand-section">
                <h4>库存记录</h4>
                <el-table :data="props.row.stockRecords" style="width: 100%">
                  <el-table-column prop="date" label="日期" width="120" />
                  <el-table-column prop="type" label="类型" width="100">
                    <template #default="scope">
                      <el-tag :type="scope.row.type === '入库' ? 'success' : 'danger'">
                        {{ scope.row.type }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="quantity" label="数量" width="100" />
                  <el-table-column prop="operator" label="操作人" width="120" />
                  <el-table-column prop="remark" label="备注" min-width="200" />
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="code" label="药品编码" width="120" />
        <el-table-column label="药品信息" min-width="250">
          <template #default="{ row }">
            <div class="medicine-info">
              <div class="medicine-image">
                <el-image 
                  :src="row.image" 
                  fit="cover"
                  :preview-src-list="[row.image]"
                  preview-teleported
                >
                  <template #error>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="medicine-detail">
                <div class="medicine-name">{{ row.name }}</div>
                <div class="medicine-spec">规格：{{ row.specification }}</div>
                <div class="medicine-category">
                  <el-tag size="small" effect="plain">{{ row.category }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="单价(元)" width="100" sortable />
        <el-table-column prop="stock" label="库存" width="100" sortable>
          <template #default="{ row }">
            <span :class="{ 'stock-warning': row.stock < row.stockThreshold }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" />
        <el-table-column prop="manufacturer" label="生产厂商" width="180" show-overflow-tooltip />
        <el-table-column prop="expirationDate" label="有效期至" width="120" sortable />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button 
                type="primary" 
                size="small"
                @click.stop="editMedicine(row)"
              >
                编辑
              </el-button>
              <el-button 
                type="success" 
                size="small"
                @click.stop="stockOperation(row)"
              >
                入库
              </el-button>
              <el-dropdown 
                trigger="click" 
                @command="(command) => handleCommand(command, row)"
                @click.stop
              >
                <el-button size="small">
                  更多<el-icon class="el-icon--right"><arrow-down /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="view">查看详情</el-dropdown-item>
                    <el-dropdown-item command="stock">库存记录</el-dropdown-item>
                    <el-dropdown-item 
                      command="status" 
                      :disabled="row.status === '已停用'"
                    >
                      {{ row.status === '正常' ? '停用' : '启用' }}
                    </el-dropdown-item>
                    <el-dropdown-item 
                      command="delete" 
                      divided 
                      style="color: #F56C6C;"
                    >
                      删除
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
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
          :total="filteredMedicines.length"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
    
    <!-- 添加/编辑药品对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑药品信息' : '添加新药品'"
      width="60%"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="medicineFormRef"
        :model="medicineForm"
        :rules="medicineRules"
        label-width="100px"
        class="medicine-form"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="药品名称" prop="name">
                  <el-input v-model="medicineForm.name" placeholder="请输入药品名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="药品编码" prop="code">
                  <el-input 
                    v-model="medicineForm.code" 
                    placeholder="请输入药品编码" 
                    :disabled="isEdit"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="药品分类" prop="category">
                  <el-select v-model="medicineForm.category" placeholder="请选择药品分类" style="width: 100%">
                    <el-option
                      v-for="item in categoryOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="规格" prop="specification">
                  <el-input v-model="medicineForm.specification" placeholder="请输入规格" />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="单价" prop="price">
                  <el-input-number 
                    v-model="medicineForm.price" 
                    :precision="2" 
                    :step="0.1" 
                    :min="0"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="单位" prop="unit">
                  <el-input v-model="medicineForm.unit" placeholder="请输入单位" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="库存阈值" prop="stockThreshold">
                  <el-input-number 
                    v-model="medicineForm.stockThreshold" 
                    :min="0"
                    :step="1"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="药品图片">
              <el-upload
                class="medicine-uploader"
                action="#"
                :auto-upload="false"
                :show-file-list="false"
                :on-change="handleImageChange"
              >
                <img v-if="medicineForm.image" :src="medicineForm.image" class="medicine-image-preview" />
                <el-icon v-else class="medicine-uploader-icon"><Plus /></el-icon>
              </el-upload>
              <div class="upload-tip">建议上传药品清晰图片，大小不超过2MB</div>
            </el-form-item>
          </el-tab-pane>
          
          <el-tab-pane label="详细信息" name="detail">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="批准文号" prop="approvalNumber">
                  <el-input v-model="medicineForm.approvalNumber" placeholder="请输入批准文号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="生产厂商" prop="manufacturer">
                  <el-select 
                    v-model="medicineForm.manufacturer" 
                    placeholder="请选择生产厂商"
                    filterable
                    allow-create
                    default-first-option
                    style="width: 100%"
                  >
                    <el-option
                      v-for="item in manufacturerOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="生产日期" prop="productionDate">
                  <el-date-picker
                    v-model="medicineForm.productionDate"
                    type="date"
                    placeholder="选择生产日期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="有效期至" prop="expirationDate">
                  <el-date-picker
                    v-model="medicineForm.expirationDate"
                    type="date"
                    placeholder="选择有效期"
                    format="YYYY-MM-DD"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            
            <el-form-item label="存储条件" prop="storageCondition">
              <el-input v-model="medicineForm.storageCondition" placeholder="请输入存储条件" />
            </el-form-item>
            
            <el-form-item label="适应症" prop="indications">
              <el-input
                v-model="medicineForm.indications"
                type="textarea"
                :rows="3"
                placeholder="请输入适应症"
              />
            </el-form-item>
            
            <el-form-item label="用法用量" prop="dosage">
              <el-input
                v-model="medicineForm.dosage"
                type="textarea"
                :rows="3"
                placeholder="请输入用法用量"
              />
            </el-form-item>
            
            <el-form-item label="不良反应" prop="sideEffects">
              <el-input
                v-model="medicineForm.sideEffects"
                type="textarea"
                :rows="3"
                placeholder="请输入不良反应"
              />
            </el-form-item>
            
            <el-form-item label="禁忌" prop="contraindications">
              <el-input
                v-model="medicineForm.contraindications"
                type="textarea"
                :rows="3"
                placeholder="请输入禁忌"
              />
            </el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确认</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 入库/出库操作对话框 -->
    <el-dialog
      v-model="stockDialogVisible"
      :title="stockForm.type === '入库' ? '药品入库' : '药品出库'"
      width="40%"
    >
      <el-form
        ref="stockFormRef"
        :model="stockForm"
        :rules="stockRules"
        label-width="100px"
      >
        <el-form-item label="药品名称">
          <div>{{ stockForm.medicineName }}</div>
        </el-form-item>
        
        <el-form-item label="当前库存">
          <div>{{ stockForm.currentStock }} {{ stockForm.unit }}</div>
        </el-form-item>
        
        <el-form-item label="操作类型" prop="type">
          <el-radio-group v-model="stockForm.type">
            <el-radio label="入库">入库</el-radio>
            <el-radio label="出库">出库</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <el-form-item label="数量" prop="quantity">
          <el-input-number 
            v-model="stockForm.quantity" 
            :min="1" 
            :max="stockForm.type === '出库' ? stockForm.currentStock : 9999"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="操作日期" prop="date">
          <el-date-picker
            v-model="stockForm.date"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        
        <el-form-item label="操作人" prop="operator">
          <el-input v-model="stockForm.operator" placeholder="请输入操作人" />
        </el-form-item>
        
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="stockForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="stockDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStockForm">确认</el-button>
        </span>
      </template>
    </el-dialog>
    
    <!-- 批量导入对话框 -->
    <el-dialog
      v-model="importDialogVisible"
      title="批量导入药品"
      width="40%"
    >
      <div class="import-container">
        <el-upload
          class="import-uploader"
          action="#"
          :auto-upload="false"
          :on-change="handleImportFileChange"
          :limit="1"
          accept=".xlsx,.xls,.csv"
        >
          <el-button type="primary">选择文件</el-button>
          <template #tip>
            <div class="el-upload__tip">
              请上传Excel或CSV格式文件，大小不超过10MB
            </div>
          </template>
        </el-upload>
        
        <div class="import-steps">
          <h4>导入步骤：</h4>
          <ol>
            <li>下载<el-link type="primary" @click="downloadTemplate">药品导入模板</el-link></li>
            <li>按照模板格式填写药品信息</li>
            <li>上传填写好的文件</li>
            <li>点击"开始导入"按钮</li>
          </ol>
        </div>
        
        <div class="import-notes">
          <h4>注意事项：</h4>
          <ul>
            <li>药品编码必须唯一，已存在的编码将更新对应药品信息</li>
            <li>必填字段：药品名称、药品编码、药品分类、规格、单价、单位</li>
            <li>日期格式：YYYY-MM-DD</li>
            <li>导入过程中请勿刷新或关闭页面</li>
          </ul>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitImport">开始导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { Search, Refresh, Plus, Picture, Upload, Download, ArrowDown } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

// 状态和数据
const loading = ref(false);
const medicines = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);
const filterCategory = ref('');
const filterManufacturer = ref('');
const filterStatus = ref('');
const searchQuery = ref('');

// 对话框
const dialogVisible = ref(false);
const stockDialogVisible = ref(false);
const importDialogVisible = ref(false);
const isEdit = ref(false);
const activeTab = ref('basic');
const medicineFormRef = ref(null);
const stockFormRef = ref(null);

// 表单数据
const medicineForm = reactive({
  id: '',
  name: '',
  code: '',
  category: '',
  specification: '',
  price: 0,
  unit: '',
  stock: 0,
  stockThreshold: 10,
  image: '',
  approvalNumber: '',
  manufacturer: '',
  productionDate: '',
  expirationDate: '',
  storageCondition: '',
  indications: '',
  dosage: '',
  sideEffects: '',
  contraindications: '',
  status: '正常'
});

// 库存操作表单
const stockForm = reactive({
  medicineId: '',
  medicineName: '',
  currentStock: 0,
  unit: '',
  type: '入库',
  quantity: 1,
  date: new Date().toISOString().split('T')[0],
  operator: '',
  remark: ''
});

// 表单验证规则
const medicineRules = {
  name: [
    { required: true, message: '请输入药品名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入药品编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9-]+$/, message: '药品编码只能包含字母、数字和连字符', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择药品分类', trigger: 'change' }
  ],
  specification: [
    { required: true, message: '请输入规格', trigger: 'blur' }
  ],
  price: [
    { required: true, message: '请输入单价', trigger: 'blur' },
    { type: 'number', min: 0, message: '单价必须大于等于0', trigger: 'blur' }
  ],
  unit: [
    { required: true, message: '请输入单位', trigger: 'blur' }
  ],
  stockThreshold: [
    { required: true, message: '请输入库存阈值', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存阈值必须大于等于0', trigger: 'blur' }
  ],
  approvalNumber: [
    { required: true, message: '请输入批准文号', trigger: 'blur' }
  ],
  manufacturer: [
    { required: true, message: '请选择生产厂商', trigger: 'blur' }
  ],
  productionDate: [
    { required: true, message: '请选择生产日期', trigger: 'change' }
  ],
  expirationDate: [
    { required: true, message: '请选择有效期', trigger: 'change' }
  ],
  storageCondition: [
    { required: true, message: '请输入存储条件', trigger: 'blur' }
  ],
  indications: [
    { required: true, message: '请输入适应症', trigger: 'blur' }
  ],
  dosage: [
    { required: true, message: '请输入用法用量', trigger: 'blur' }
  ]
};

// 库存操作表单验证规则
const stockRules = {
  type: [
    { required: true, message: '请选择操作类型', trigger: 'change' }
  ],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '数量必须大于0', trigger: 'blur' }
  ],
  date: [
    { required: true, message: '请选择操作日期', trigger: 'change' }
  ],
  operator: [
    { required: true, message: '请输入操作人', trigger: 'blur' }
  ]
};

// 选项数据
const categoryOptions = [
  { value: '处方药', label: '处方药' },
  { value: '非处方药', label: '非处方药' },
  { value: '中成药', label: '中成药' },
  { value: '中药饮片', label: '中药饮片' },
  { value: '生物制品', label: '生物制品' },
  { value: '化学药品', label: '化学药品' },
  { value: '抗生素', label: '抗生素' },
  { value: '营养保健', label: '营养保健' }
];

const manufacturerOptions = [
  { value: '北京同仁堂', label: '北京同仁堂' },
  { value: '云南白药集团', label: '云南白药集团' },
  { value: '哈药集团', label: '哈药集团' },
  { value: '修正药业', label: '修正药业' },
  { value: '华润三九', label: '华润三九' },
  { value: '以岭药业', label: '以岭药业' },
  { value: '天士力', label: '天士力' },
  { value: '东阿阿胶', label: '东阿阿胶' },
  { value: '白云山', label: '白云山' },
  { value: '江中药业', label: '江中药业' },
  { value: '步长制药', label: '步长制药' },
  { value: '仁和药业', label: '仁和药业' }
];

const statusOptions = [
  { value: '正常', label: '正常' },
  { value: '缺货', label: '缺货' },
  { value: '已过期', label: '已过期' },
  { value: '已停用', label: '已停用' }
];

// 计算属性
const filteredMedicines = computed(() => {
  let result = [...medicines.value];
  
  // 分类筛选
  if (filterCategory.value) {
    result = result.filter(item => item.category === filterCategory.value);
  }
  
  // 厂商筛选
  if (filterManufacturer.value) {
    result = result.filter(item => item.manufacturer === filterManufacturer.value);
  }
  
  // 状态筛选
  if (filterStatus.value) {
    result = result.filter(item => item.status === filterStatus.value);
  }
  
  // 搜索筛选
  if (searchQuery.value) {
    const query = searchQuery.value.toLowerCase();
    result = result.filter(item => 
      item.name.toLowerCase().includes(query) || 
      item.code.toLowerCase().includes(query)
    );
  }
  
  return result;
});

const paginatedMedicines = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  const end = start + pageSize.value;
  return filteredMedicines.value.slice(start, end);
});

// 方法
function getStatusType(status) {
  switch (status) {
    case '正常': return 'success';
    case '缺货': return 'warning';
    case '已过期': return 'danger';
    case '已停用': return 'info';
    default: return '';
  }
}

async function fetchMedicines() {
  loading.value = true;
  try {
    // 模拟API调用
    // const response = await getMedicines();
    // medicines.value = response.data;
    
    // 模拟数据
    setTimeout(() => {
      const mockMedicines = [];
      const categories = categoryOptions.map(item => item.value);
      const manufacturers = manufacturerOptions.map(item => item.value);
      const statuses = statusOptions.map(item => item.value);
      
      for (let i = 1; i <= 50; i++) {
        const categoryIndex = Math.floor(Math.random() * categories.length);
        const manufacturerIndex = Math.floor(Math.random() * manufacturers.length);
        const statusIndex = Math.floor(Math.random() * statuses.length);
        
        // 生成随机生产日期（近1年内）
        const today = new Date();
        const productionDate = new Date(today);
        productionDate.setDate(today.getDate() - Math.floor(Math.random() * 365));
        const prodDateStr = productionDate.toISOString().split('T')[0];
        
        // 生成随机有效期（生产日期后1-3年）
        const expirationDate = new Date(productionDate);
        expirationDate.setFullYear(productionDate.getFullYear() + Math.floor(Math.random() * 2) + 1);
        const expDateStr = expirationDate.toISOString().split('T')[0];
        
        // 生成随机库存记录
        const stockRecords = [];
        const recordCount = Math.floor(Math.random() * 5) + 1;
        for (let j = 0; j < recordCount; j++) {
          const recordDate = new Date(today);
          recordDate.setDate(today.getDate() - Math.floor(Math.random() * 90));
          const recordDateStr = recordDate.toISOString().split('T')[0];
          
          const isInbound = Math.random() > 0.3; // 70%概率是入库
          
          stockRecords.push({
            date: recordDateStr,
            type: isInbound ? '入库' : '出库',
            quantity: isInbound ? Math.floor(Math.random() * 50) + 10 : Math.floor(Math.random() * 10) + 1,
            operator: isInbound ? '库管员' + Math.floor(Math.random() * 5 + 1) : '医生' + Math.floor(Math.random() * 10 + 1),
            remark: isInbound ? '常规采购入库' : '日常消耗出库'
          });
        }
        
        // 计算当前库存
        const stock = stockRecords.reduce((total, record) => {
          return record.type === '入库' ? total + record.quantity : total - record.quantity;
        }, Math.floor(Math.random() * 50) + 20);
        
        // 根据库存自动调整状态
        let status = statuses[statusIndex];
        const stockThreshold = Math.floor(Math.random() * 15) + 5;
        if (stock <= 0) {
          status = '缺货';
        } else if (stock < stockThreshold) {
          status = Math.random() > 0.5 ? '正常' : '缺货';
        }
        
        // 如果过期了，状态改为已过期
        if (new Date(expDateStr) < today) {
          status = '已过期';
        }
        
        mockMedicines.push({
          id: i,
          name: `${['感冒灵', '阿莫西林', '布洛芬', '维生素C', '板蓝根', '双黄连', '银翘解毒片', '复方丹参片'][Math.floor(Math.random() * 8)]}${i}号`,
          code: `M${String(i).padStart(5, '0')}`,
          category: categories[categoryIndex],
          specification: `${Math.floor(Math.random() * 5) * 100 + 100}${['mg', 'ml', 'g', '片', '粒', '丸'][Math.floor(Math.random() * 6)]}*${Math.floor(Math.random() * 3) * 10 + 10}${['片', '粒', '丸', '袋', '瓶'][Math.floor(Math.random() * 5)]}`,
          price: parseFloat((Math.random() * 100 + 5).toFixed(2)),
          unit: ['盒', '瓶', '袋', '支'][Math.floor(Math.random() * 4)],
          stock: stock,
          stockThreshold: stockThreshold,
          image: `https://picsum.photos/id/${Math.floor(Math.random() * 100)}/200/200`,
          approvalNumber: `国药准字H${Math.floor(Math.random() * 90000) + 10000}`,
          manufacturer: manufacturers[manufacturerIndex],
          productionDate: prodDateStr,
          expirationDate: expDateStr,
          storageCondition: ['常温保存', '阴凉干燥处保存', '冷藏2-8℃保存', '避光保存'][Math.floor(Math.random() * 4)],
          indications: `用于${['感冒发热', '细菌感染', '消炎止痛', '补充维生素', '清热解毒', '心脑血管疾病', '消化不良', '高血压'][Math.floor(Math.random() * 8)]}等症状。`,
          dosage: `口服，一次${Math.floor(Math.random() * 3) + 1}${['片', '粒', '袋', '勺', '毫升'][Math.floor(Math.random() * 5)]}，一日${Math.floor(Math.random() * 3) + 1}次。`,
          sideEffects: `可能出现${['恶心呕吐', '头晕', '皮疹', '腹泻', '嗜睡', '过敏反应', '肝功能异常', '无明显不良反应'][Math.floor(Math.random() * 8)]}等不良反应。`,
          contraindications: `${['孕妇', '儿童', '肝肾功能不全患者', '过敏体质者', '高血压患者', '糖尿病患者', '无明显禁忌'][Math.floor(Math.random() * 7)]}禁用。`,
          status: status,
          stockRecords: stockRecords
        });
      }
      
      medicines.value = mockMedicines;
      loading.value = false;
    }, 800);
  } catch (error) {
    console.error('获取药品列表失败:', error);
    ElMessage.error('获取药品列表失败');
    loading.value = false;
  }
}

function refreshMedicines() {
  fetchMedicines();
  ElMessage.success('药品列表已刷新');
}

function handleFilterChange() {
  currentPage.value = 1;
}

function handleSearch() {
  currentPage.value = 1;
}

function handleSizeChange(val) {
  pageSize.value = val;
  currentPage.value = 1;
}

function handleCurrentChange(val) {
  currentPage.value = val;
}

function handleRowClick(row) {
  // 行点击事件，可以展开行或者其他操作
}

function handleCommand(command, row) {
  switch (command) {
    case 'view':
      // 查看详情，可以实现为打开抽屉或对话框
      ElMessage({ type: 'info', message: `查看药品详情: ${row.name}` });
      break;
    case 'stock':
      // 查看库存记录
      ElMessage({ type: 'info', message: `查看库存记录: ${row.name}` });
      break;
    case 'status':
      // 更改状态
      changeStatus(row);
      break;
    case 'delete':
      // 删除药品
      deleteMedicine(row);
      break;
  }
}

function addMedicine() {
  resetForm();
  isEdit.value = false;
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function editMedicine(medicine) {
  resetForm();
  isEdit.value = true;
  
  // 复制药品信息到表单
  Object.keys(medicineForm).forEach(key => {
    if (key in medicine) {
      medicineForm[key] = medicine[key];
    }
  });
  
  dialogVisible.value = true;
  activeTab.value = 'basic';
}

function stockOperation(medicine) {
  // 设置库存操作表单
  stockForm.medicineId = medicine.id;
  stockForm.medicineName = medicine.name;
  stockForm.currentStock = medicine.stock;
  stockForm.unit = medicine.unit;
  stockForm.type = '入库';
  stockForm.quantity = 1;
  stockForm.date = new Date().toISOString().split('T')[0];
  stockForm.operator = '';
  stockForm.remark = '';
  
  stockDialogVisible.value = true;
}

function changeStatus(medicine) {
  const newStatus = medicine.status === '正常' ? '已停用' : '正常';
  const statusText = newStatus === '正常' ? '启用' : '停用';
  
  ElMessageBox.confirm(
    `确认${statusText}药品 ${medicine.name} 吗？`,
    `${statusText}确认`,
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: newStatus === '已停用' ? 'warning' : 'info'
    }
  ).then(() => {
    // 模拟API调用
    // await updateMedicineStatus(medicine.id, newStatus);
    
    // 模拟成功响应
    setTimeout(() => {
      // 更新本地数据
      const index = medicines.value.findIndex(item => item.id === medicine.id);
      if (index !== -1) {
        medicines.value[index].status = newStatus;
      }
      
      ElMessage.success(`已${statusText}药品 ${medicine.name}`);
    }, 500);
  }).catch(() => {});
}

function deleteMedicine(medicine) {
  ElMessageBox.confirm(
    `确认删除药品 ${medicine.name} 吗？此操作不可逆。`,
    '删除确认',
    {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 模拟API调用
    // await deleteMedicine(medicine.id);
    
    // 模拟成功响应
    setTimeout(() => {
      // 从列表中移除
      medicines.value = medicines.value.filter(item => item.id !== medicine.id);
      
      ElMessage.success('药品删除成功');
    }, 500);
  }).catch(() => {});
}

function handleImageChange(file) {
  // 实际项目中应该上传到服务器
  // 这里模拟上传成功
  const reader = new FileReader();
  reader.readAsDataURL(file.raw);
  reader.onload = () => {
    medicineForm.image = reader.result;
  };
}

function resetForm() {
  // 重置表单数据
  Object.assign(medicineForm, {
    id: '',
    name: '',
    code: '',
    category: '',
    specification: '',
    price: 0,
    unit: '',
    stock: 0,
    stockThreshold: 10,
    image: '',
    approvalNumber: '',
    manufacturer: '',
    productionDate: '',
    expirationDate: '',
    storageCondition: '',
    indications: '',
    dosage: '',
    sideEffects: '',
    contraindications: '',
    status: '正常'
  });
  
  // 如果表单引用存在，重置验证
  if (medicineFormRef.value) {
    medicineFormRef.value.resetFields();
  }
}

function handleDialogClose(done) {
  ElMessageBox.confirm(
    '确认关闭？未保存的数据将会丢失',
    '提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    resetForm();
    done();
  }).catch(() => {});
}

function submitForm() {
  medicineFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // 如果是编辑模式，调用更新API
        if (isEdit.value) {
          // await updateMedicine(medicineForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 更新本地数据
            const index = medicines.value.findIndex(item => item.id === medicineForm.id);
            if (index !== -1) {
              // 复制表单数据到药品对象
              const updatedMedicine = { ...medicines.value[index] };
              Object.keys(medicineForm).forEach(key => {
                updatedMedicine[key] = medicineForm[key];
              });
              
              medicines.value[index] = updatedMedicine;
            }
            
            ElMessage.success('药品信息更新成功');
            dialogVisible.value = false;
            loading.value = false;
          }, 1000);
        } else {
          // 如果是添加模式，调用创建API
          // await createMedicine(medicineForm);
          
          // 模拟成功响应
          setTimeout(() => {
            // 创建新药品对象并添加到列表
            const newId = Math.max(...medicines.value.map(item => item.id)) + 1;
            const newMedicine = {
              ...medicineForm,
              id: newId,
              stock: 0,
              stockRecords: [],
              status: '正常'
            };
            
            medicines.value.unshift(newMedicine);
            
            ElMessage.success('药品添加成功');
            dialogVisible.value = false;
            loading.value = false;
          }, 1000);
        }
      } catch (error) {
        console.error('提交表单失败:', error);
        ElMessage.error('操作失败，请重试');
        loading.value = false;
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
      return false;
    }
  });
}

function submitStockForm() {
  stockFormRef.value.validate(async (valid) => {
    if (valid) {
      // 模拟API调用
      try {
        loading.value = true;
        
        // await updateMedicineStock(stockForm);
        
        // 模拟成功响应
        setTimeout(() => {
          // 更新本地数据
          const index = medicines.value.findIndex(item => item.id === stockForm.medicineId);
          if (index !== -1) {
            // 更新库存
            if (stockForm.type === '入库') {
              medicines.value[index].stock += stockForm.quantity;
            } else {
              medicines.value[index].stock -= stockForm.quantity;
            }
            
            // 添加库存记录
            medicines.value[index].stockRecords.unshift({
              date: stockForm.date,
              type: stockForm.type,
              quantity: stockForm.quantity,
              operator: stockForm.operator,
              remark: stockForm.remark
            });
            
            // 更新状态
            if (medicines.value[index].stock <= 0) {
              medicines.value[index].status = '缺货';
            } else if (medicines.value[index].stock < medicines.value[index].stockThreshold) {
              // 低于阈值但还有库存，50%概率显示为缺货
              medicines.value[index].status = Math.random() > 0.5 ? '正常' : '缺货';
            } else {
              medicines.value[index].status = '正常';
            }
          }
          
          ElMessage.success(`药品${stockForm.type}操作成功`);
          stockDialogVisible.value = false;
          loading.value = false;
        }, 1000);
      } catch (error) {
        console.error('提交库存操作失败:', error);
        ElMessage.error('操作失败，请重试');
        loading.value = false;
      }
    } else {
      ElMessage.warning('请正确填写表单信息');
      return false;
    }
  });
}

function importMedicines() {
  importDialogVisible.value = true;
}

function handleImportFileChange(file) {
  // 实际项目中应该解析文件内容
  console.log('Selected file:', file);
}

function submitImport() {
  // 模拟导入过程
  loading.value = true;
  setTimeout(() => {
    ElMessage.success('成功导入15条药品数据');
    importDialogVisible.value = false;
    loading.value = false;
    fetchMedicines(); // 刷新列表
  }, 1500);
}

function downloadTemplate() {
  // 实际项目中应该提供模板下载
  ElMessage.success('模板下载成功');
}

function exportMedicines() {
  // 模拟导出过程
  loading.value = true;
  setTimeout(() => {
    ElMessage.success('药品数据导出成功');
    loading.value = false;
  }, 1000);
}

// 生命周期钩子
onMounted(() => {
  fetchMedicines();
});
</script>

<style scoped>
.medicines-container {
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
  font-weight: 500;
}

.header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.medicines-card {
  margin-bottom: 20px;
}

.medicine-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.medicine-image {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  color: #909399;
}

.medicine-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.medicine-name {
  font-weight: 500;
}

.medicine-spec {
  font-size: 12px;
  color: #606266;
}

.medicine-category {
  margin-top: 2px;
}

.stock-warning {
  color: #E6A23C;
  font-weight: bold;
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

/* 展开行样式 */
.medicine-expand {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.expand-section {
  width: 100%;
}

.expand-section h4 {
  margin-top: 0;
  margin-bottom: 15px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;
}

.expand-content {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.expand-item {
  width: calc(25% - 8px);
  display: flex;
  margin-bottom: 10px;
}

.expand-item.full-width {
  width: 100%;
}

.expand-label {
  width: 100px;
  color: #909399;
  font-size: 14px;
}

/* 对话框样式 */
.medicine-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
}

.medicine-uploader {
  width: 150px;
  height: 150px;
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.3s;
}

.medicine-uploader:hover {
  border-color: #409EFF;
}

.medicine-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 150px;
  height: 150px;
  line-height: 150px;
  text-align: center;
}

.medicine-image-preview {
  width: 150px;
  height: 150px;
  display: block;
  object-fit: cover;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* 导入对话框样式 */
.import-container {
  padding: 20px;
}

.import-uploader {
  margin-bottom: 20px;
}

.import-steps {
  margin-bottom: 20px;
}

.import-steps h4,
.import-notes h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
}

.import-steps ol,
.import-notes ul {
  margin: 0;
  padding-left: 20px;
}

.import-steps li,
.import-notes li {
  margin-bottom: 8px;
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
  
  .expand-item {
    width: 100%;
  }
}

/* 动画效果 */
.el-table :deep(tbody tr) {
  transition: all 0.3s;
}

.el-table :deep(tbody tr:hover) {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1;
  position: relative;
}

/* 表格样式增强 */
.el-table :deep(th) {
  background-color: #f5f7fa !important;
}

.el-table :deep(.el-table__row:nth-child(even)) {
  background-color: #fafafa;
}
</style>