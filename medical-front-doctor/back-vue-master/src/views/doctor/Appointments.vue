<template>
  <div class="appointments-container">
    <div class="page-header">
      <h2>我的排班</h2>
      <div class="header-actions">
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          @change="handleDateChange"
          style="width: 200px"
        />
        <el-button type="primary" @click="refreshSchedule">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>
    
    <el-card shadow="hover" class="appointment-card">
      <div class="schedule-container">
        <div class="schedule-header">
          <h3>医生排班表</h3>
          <el-button type="primary" @click="refreshSchedule">
            <el-icon><Refresh /></el-icon>
            刷新排班
          </el-button>
        </div>
        <div v-loading="scheduleLoading" class="schedule-content">
          <div v-if="filteredSchedule.length === 0" class="empty-state">
            <el-empty description="暂无排班信息" />
          </div>
          <div v-else class="schedule-grid">
            <div 
              v-for="schedule in filteredSchedule" 
              :key="schedule.id"
              class="schedule-card"
            >
              <div class="schedule-date">
                <h4>{{ schedule.date }}</h4>
                <p>{{ schedule.dayOfWeek }}</p>
              </div>
              <div class="schedule-time">
                <div v-if="schedule.morningTime" class="time-slot">
                  <span class="label">上午：</span>
                  <span class="time">{{ schedule.morningTime }}</span>
                </div>
                <div v-if="schedule.afternoonTime" class="time-slot">
                  <span class="label">下午：</span>
                  <span class="time">{{ schedule.afternoonTime }}</span>
                </div>
              </div>
              <div class="schedule-info">
                <p><strong>科室：</strong>{{ schedule.department }}</p>
                <p><strong>已预约人数：</strong>{{ schedule.currentAppointmentCount }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import { getScheduleList } from '@/api/doctor';

const router = useRouter();

// 状态和数据
const scheduleLoading = ref(false);
const doctorSchedule = ref([]);
const selectedDate = ref('');

// 计算属性 - 根据日期过滤排班并按时间排序
const filteredSchedule = computed(() => {
  let result = doctorSchedule.value;
  
  // 日期过滤
  if (selectedDate.value) {
    const targetDate = new Date(selectedDate.value);
    const targetYear = targetDate.getFullYear();
    const targetMonth = targetDate.getMonth();
    const targetDay = targetDate.getDate();
    
    result = result.filter(schedule => {
      try {
        // 解析排班日期
        const scheduleDate = new Date(schedule.scheduleDate);
        const scheduleYear = scheduleDate.getFullYear();
        const scheduleMonth = scheduleDate.getMonth();
        const scheduleDay = scheduleDate.getDate();
        
        return scheduleYear === targetYear && scheduleMonth === targetMonth && scheduleDay === targetDay;
      } catch (error) {
        console.error('日期解析错误:', error);
        return false;
      }
    });
  }
  
  // 按时间排序：同样的时间，上午先，下午后
  result.sort((a, b) => {
    try {
      // 解析日期
      const dateA = new Date(a.scheduleDate);
      const dateB = new Date(b.scheduleDate);
      
      // 首先按日期排序
      if (dateA.getTime() !== dateB.getTime()) {
        return dateA.getTime() - dateB.getTime();
      }
      
      // 如果日期相同，按时间段排序：上午先，下午后
      // 上午时间段优先级为1，下午时间段优先级为2
      const getTimeSlotPriority = (schedule) => {
        // 如果只有上午，优先级为1
        if (schedule.morningTime && !schedule.afternoonTime) {
          return 1;
        }
        // 如果只有下午，优先级为2
        else if (!schedule.morningTime && schedule.afternoonTime) {
          return 2;
        }
        // 如果同时有上午和下午，优先级为1（上午优先）
        else if (schedule.morningTime && schedule.afternoonTime) {
          return 1;
        }
        // 如果都没有时间段，优先级为3
        else {
          return 3;
        }
      };
      
      const priorityA = getTimeSlotPriority(a);
      const priorityB = getTimeSlotPriority(b);
      
      // 如果优先级相同，按ID排序保持稳定
      if (priorityA === priorityB) {
        return (a.id || 0) - (b.id || 0);
      }
      
      return priorityA - priorityB;
    } catch (error) {
      console.error('排序错误:', error);
      return 0;
    }
  });
  
  return result;
});

// 方法
async function fetchDoctorSchedule() {
  scheduleLoading.value = true;
  try {
    console.log('开始获取排班信息...');
    const response = await getScheduleList();
    console.log('排班API响应:', response);
    
    if (response.code === 200 && response.data) {
      console.log('排班原始数据:', response.data);
      // 处理排班数据
      doctorSchedule.value = response.data.map(schedule => {
        console.log('处理排班记录:', schedule);
        // 处理日期格式
        let scheduleDate;
        try {
          scheduleDate = new Date(schedule.scheduleDate);
          if (isNaN(scheduleDate.getTime())) {
            console.error('无效的日期格式:', schedule.scheduleDate);
            scheduleDate = new Date();
          }
        } catch (error) {
          console.error('日期解析错误:', error);
          scheduleDate = new Date();
        }
        
        const dateStr = scheduleDate.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
        const dayOfWeek = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][scheduleDate.getDay()];
        
        const processedSchedule = {
          id: schedule.id || Math.random(),
          date: dateStr,
          dayOfWeek: dayOfWeek,
          scheduleDate: schedule.scheduleDate, // 保存原始日期用于过滤
          morningTime: schedule.isMorning === 1 ? '08:00-12:00' : null,
          afternoonTime: schedule.isAfternoon === 1 ? '14:00-17:30' : null,
          department: schedule.departmentName || '未知科室',
          currentAppointmentCount: schedule.currentAppointmentCount || 0
        };
        
        console.log('处理后的排班记录:', processedSchedule);
        return processedSchedule;
      });
      console.log('最终排班数据:', doctorSchedule.value);
    } else {
      console.error('获取排班信息失败:', response);
      ElMessage.error(`获取排班信息失败: ${response.message || '未知错误'}`);
      doctorSchedule.value = [];
    }
  } catch (error) {
    console.error('获取排班信息失败:', error);
    ElMessage.error('获取排班信息失败');
    doctorSchedule.value = [];
  } finally {
    scheduleLoading.value = false;
  }
}

function refreshSchedule() {
  fetchDoctorSchedule();
  ElMessage.success('排班信息已刷新');
}

function handleDateChange(date) {
  selectedDate.value = date;
}

// 生命周期钩子
onMounted(() => {
  fetchDoctorSchedule();
});
</script>

<style scoped>
.appointments-container {
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
  gap: 12px;
}

.appointment-card {
  margin-bottom: 20px;
}

/* 排班表样式 */
.schedule-container {
  padding: 20px 0;
}

.schedule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.schedule-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.schedule-content {
  min-height: 200px;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.schedule-card {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  background: white;
}

.schedule-date {
  text-align: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.schedule-date h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 500;
}

.schedule-date p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.schedule-time {
  margin-bottom: 12px;
}

.time-slot {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.time-slot .label {
  font-weight: 500;
  color: #606266;
}

.time-slot .time {
  color: #409eff;
}

.schedule-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #606266;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .schedule-grid {
    grid-template-columns: 1fr;
  }
}
</style>