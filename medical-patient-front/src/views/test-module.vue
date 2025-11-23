<template>
  <div class="test-module">
    <h1>模块导出测试</h1>
    <el-button @click="testImports">测试导入</el-button>
    <div v-if="testResult">
      <h3>测试结果：</h3>
      <pre>{{ testResult }}</pre>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const testResult = ref('')

const testImports = async () => {
  try {
    // 测试导入
    const { getDepartmentList, getSubDepartmentList, getScheduleList, createAppointment } = await import('../../api/appointment')
    const { getCaseList } = await import('../../api/user')
    const { getFutureDates, formatDate } = await import('../../utils')
    
    testResult.value = JSON.stringify({
      getDepartmentList: typeof getDepartmentList,
      getSubDepartmentList: typeof getSubDepartmentList,
      getScheduleList: typeof getScheduleList,
      createAppointment: typeof createAppointment,
      getCaseList: typeof getCaseList,
      getFutureDates: typeof getFutureDates,
      formatDate: typeof formatDate
    }, null, 2)
    
    ElMessage.success('模块导入测试成功！')
  } catch (error) {
    testResult.value = `错误: ${error.message}`
    ElMessage.error('模块导入测试失败！')
    console.error('模块导入错误:', error)
  }
}
</script>

<style scoped>
.test-module {
  padding: 20px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}
</style> 