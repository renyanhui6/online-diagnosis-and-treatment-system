<template>
  <div class="chat-container">
    <!-- 聊天头部 -->
    <div class="chat-header">
      <div class="patient-info">
        <el-avatar :size="40" :src="consultation.patientAvatar" />
        <div class="patient-detail">
          <h3>{{ consultation.patientName }}</h3>
          <p>{{ consultation.patientGender }} · {{ consultation.patientAge }}岁</p>
          <el-tag :type="getTypeTag(consultation.type)" size="small" effect="plain">
            {{ consultation.type }}
          </el-tag>
        </div>
      </div>
      <div class="chat-actions">
        <el-button type="primary" @click="showRecordDialog">填写就诊记录</el-button>
        <el-button type="warning" @click="showPrescriptionDialog">开处方</el-button>
        <el-button type="success" @click="finishConsultation">结束问诊</el-button>
        <el-button type="info" @click="goBack" v-if="roomStatus === 3 || roomStatus === 4 || roomStatus === 5">返回</el-button>
        <el-button type="warning" @click="handleTimeoutRoom" v-if="roomStatus === 4">处理超时房间</el-button>
      </div>
    </div>

    <!-- 聊天内容区域 -->
    <div class="chat-content" ref="chatContentRef">
      <div class="message-list">
        <!-- 等待连接提示 -->
        <div v-if="!isConnected" class="connection-status">
          <el-alert
            title="正在连接聊天室..."
            type="info"
            :closable="false"
            show-icon
          />
        </div>

                 <!-- 系统消息 -->
         <div v-if="systemMessage" class="system-message">
           <p>{{ systemMessage }}</p>
         </div>

                   <!-- 房间状态显示 -->
          <div class="room-status-display">
            <el-tag
              :type="getRoomStatusType(roomStatus)"
              size="small"
              effect="dark"
            >
              {{ getRoomStatusText(roomStatus) }}
            </el-tag>
            <div class="status-description">
              <small>{{ getStatusDescription(roomStatus) }}</small>
            </div>
          </div>

        <div
          v-for="message in messages" 
          :key="message.id"
          :class="['message-item', message.sender === 'doctor' ? 'message-right' : 'message-left']"
        >
          <div class="message-avatar">
            <el-avatar :size="32" :src="message.avatar" />
          </div>
          <div class="message-content">
            <div class="message-info">
              <span class="sender-name">{{ message.senderName }}</span>
              <span class="message-time">{{ message.time }}</span>
            </div>
            <div class="message-bubble">
              <div v-if="message.type === 'text'" class="message-text">
                {{ message.content }}
              </div>
              <div v-else-if="message.type === 'image'" class="message-image">
                <el-image 
                  :src="message.content" 
                  fit="cover" 
                  style="max-width: 200px; max-height: 200px;"
                  :preview-src-list="[message.content]"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

         <!-- 输入区域 -->
     <div class="chat-input" v-if="isConnected && (roomStatus === 2 || roomStatus === 1)">
      <div class="input-toolbar">
        <el-button type="text" @click="selectImage">
          <el-icon><Picture /></el-icon>
          图片
        </el-button>
        <el-button type="text" @click="showQuickReply = !showQuickReply">
          <el-icon><ChatDotRound /></el-icon>
          快捷回复
        </el-button>
      </div>
      
      <!-- 快捷回复 -->
      <div v-if="showQuickReply" class="quick-reply">
        <el-tag 
          v-for="reply in quickReplies" 
          :key="reply"
          @click="insertQuickReply(reply)"
          style="margin: 2px; cursor: pointer;"
        >
          {{ reply }}
        </el-tag>
      </div>

      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入消息..."
          @keydown.ctrl.enter="sendMessage"
          resize="none"
        />
        <el-button 
          type="primary" 
          @click="sendMessage"
          :disabled="!inputMessage.trim()"
          class="send-button"
        >
          发送
        </el-button>
      </div>
    </div>

    <!-- 隐藏的文件输入 -->
    <input 
      ref="imageInputRef" 
      type="file" 
      accept="image/*" 
      style="display: none" 
      @change="handleImageSelect"
    />

    <!-- 就诊记录对话框 -->
    <el-dialog
      v-model="recordDialogVisible"
      title="填写就诊记录"
      width="50%"
    >
      <el-form :model="recordForm" label-width="100px" :rules="recordRules" ref="recordFormRef">
        <el-form-item label="就诊描述" prop="description">
          <el-input 
            v-model="recordForm.description" 
            type="textarea" 
            :rows="6" 
            placeholder="请详细描述患者的症状、检查结果、诊断过程等"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="recordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitRecord">保存就诊记录</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 处方对话框 -->
    <el-dialog
      v-model="prescriptionDialogVisible"
      title="开处方"
      width="60%"
    >
      <el-form :model="prescriptionForm" label-width="100px" :rules="prescriptionRules" ref="prescriptionFormRef">
        <el-form-item label="药品列表" prop="medicines">
          <div class="medicine-list">
            <div 
              v-for="(medicine, index) in prescriptionForm.medicines" 
              :key="index"
              class="medicine-item"
            >
              <el-row :gutter="12">
                <el-col :span="8">
                  <el-select 
                    v-model="medicine.drugId" 
                    placeholder="选择药品"
                    filterable
                    @change="onMedicineChange(index)"
                  >
                    <el-option
                      v-for="drug in availableDrugs"
                      :key="drug.id"
                      :label="drug.name"
                      :value="drug.id"
                    />
                  </el-select>
                </el-col>
                <el-col :span="4">
                  <el-input-number 
                    v-model="medicine.quantity" 
                    :min="1" 
                    :max="100"
                    placeholder="数量"
                  />
                </el-col>
                <el-col :span="8">
                  <el-button 
                    type="danger" 
                    size="small" 
                    @click="removeMedicine(index)"
                    :disabled="prescriptionForm.medicines.length === 1"
                  >
                    删除
                  </el-button>
                </el-col>
              </el-row>
            </div>
            <el-button type="primary" size="small" @click="addMedicine" style="margin-top: 10px;">
              添加药品
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="prescriptionDialogVisible = false">取消</el-button>
          <el-button type="warning" @click="submitPrescription">保存处方</el-button>
        </span>
      </template>
    </el-dialog>

         <!-- 结束问诊对话框 -->
     <el-dialog
       v-model="finishDialogVisible"
       title="结束问诊"
       width="40%"
     >
       <p>确认结束本次问诊吗？结束后将无法继续聊天。</p>
       <template #footer>
         <span class="dialog-footer">
           <el-button @click="finishDialogVisible = false">取消</el-button>
           <el-button type="success" @click="confirmFinishConsultation">确认结束</el-button>
         </span>
       </template>
       </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Picture, Document, ChatDotRound } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '../../stores/user';
import WebSocketService from '../../utils/websocket';
import {
  getChatRoom,
  getChatRoomById,
  getChatMessages,
  sendChatMessage,
  uploadChatImage,
  updateRoomStatus,
  endConsultation
} from '../../api/chat';
import { addMedicalRecord, getAllDrugs, addPrescription, getAllRegistrationInfoList, getRegistrationList, getRegistrationById, changeStatusToSuspended, changeStatusToInProgress, changeStatusToCompleted } from '../../api/doctor';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore(); // 在setup顶层初始化userStore

// 状态数据
const consultation = ref({});
const messages = ref([]);
const inputMessage = ref('');
const showQuickReply = ref(false);
const finishDialogVisible = ref(false);
const recordDialogVisible = ref(false);
const prescriptionDialogVisible = ref(false);
const isConnected = ref(false);
const roomStatus = ref(1); // 1-等待中, 2-进行中, 3-已结束, 4-患者超时未响应, 5-患者拒绝
const systemMessage = ref('');
const roomId = ref('');

// 表单引用
// 表单引用
const chatContentRef = ref(null);
const imageInputRef = ref(null);
const recordFormRef = ref(null);
const prescriptionFormRef = ref(null);
const fileInputRef = ref(null);

// 就诊记录表单
const recordForm = ref({
  patientId: '',
  doctorId: '',
  description: ''
});

const recordRules = {
  description: [{ required: true, message: '请输入就诊描述', trigger: 'blur' }]
};

// 处方表单
const prescriptionForm = ref({
  patientId: '',
  doctorId: '',
  medicines: [
    {
      drugId: '',
      quantity: 1
    }
  ]
});

const prescriptionRules = {
  medicines: [{ required: true, message: '请至少添加一种药品', trigger: 'change' }]
};

// 可用药品列表
const availableDrugs = ref([]);

// 获取药品列表
async function fetchDrugs() {
  try {
    console.log('🔍 开始获取药品列表...');
    const response = await getAllDrugs();
    console.log('🔍 药品列表API响应:', response);
    
    if (response.code === 200 && response.data) {
      console.log('🔍 原始药品数据:', response.data);
      availableDrugs.value = response.data.map(drug => ({
        id: drug.id,
        name: drug.genericName,
        price: 0 // 后端没有返回价格，设置为0
      }));
      console.log('✅ 获取药品列表成功:', availableDrugs.value);
    } else {
      console.error('❌ 获取药品列表失败:', response);
      console.error('❌ 响应码:', response.code);
      console.error('❌ 响应数据:', response.data);
    }
  } catch (error) {
    console.error('❌ 获取药品列表失败:', error);
    console.error('❌ 错误详情:', error.message);
  }
}

// 快捷回复
const quickReplies = [
  '您好，我是您的主治医生',
  '请详细描述您的症状',
  '建议您多休息，多喝水',
  '请按时服药',
  '如有不适请及时联系',
  '祝您早日康复'
];

// 获取问诊信息
async function fetchConsultationInfo() {
  const roomIdParam = route.params.id;

  console.log('=== 获取问诊信息调试 ===');
  console.log('房间ID:', roomIdParam);

  try {
    // 通过roomId查询房间信息
    const roomResponse = await getChatRoomById(roomIdParam);
    if (roomResponse.code === 200 && roomResponse.data) {
      const roomInfo = roomResponse.data;

      // 设置房间ID
      roomId.value = roomInfo.id;
      console.log('✅ 设置房间ID:', roomId.value);
      console.log('✅ 完整的roomInfo数据:', roomInfo);
      console.log('✅ roomInfo.registrationId:', roomInfo.registrationId);
      console.log('✅ roomInfo.registration_id:', roomInfo.registration_id);
      console.log('✅ roomInfo.roomStatus:', roomInfo.roomStatus);
      console.log('✅ roomInfo.room_status:', roomInfo.room_status);

      // 获取患者ID - 从问诊记录中获取
      let patientId = null;
      let patientName = '张三';
      
      // 使用挂号ID直接查询单条挂号信息，确保拿到 RegistrationInfo.patientId
      if (roomInfo.registrationId || roomInfo.registration_id) {
        try {
          const regId = roomInfo.registrationId || roomInfo.registration_id;
          const regResp = await getRegistrationById(regId);
          if (regResp.code === 200 && regResp.data) {
            patientId = regResp.data.patientId;
            patientName = regResp.data.patientName || '张三';
            console.log('✅ 通过getRegistrationById获取到患者信息:', { patientId, patientName });
          } else {
            // 回退：用全部/待处理列表匹配，然后只取 RegistrationInfo.patientId
            const allConsultationsResponse = await getAllRegistrationInfoList();
            if (allConsultationsResponse.code === 200 && allConsultationsResponse.data) {
              const rec = allConsultationsResponse.data.find(item => item.id === regId);
              if (rec) {
                patientId = rec.patientId;
                patientName = rec.patientName || '张三';
                console.log('✅ 回退-从全部问诊记录获取patientId:', { patientId, patientName });
              }
            }
            if (!patientId) {
              const pendingResp = await getRegistrationList();
              if (pendingResp.code === 200 && pendingResp.data) {
                const rec = pendingResp.data.find(item => item.id === regId);
                if (rec) {
                  patientId = rec.patientId;
                  patientName = rec.patientName || '张三';
                  console.log('✅ 回退-从待处理问诊记录获取patientId:', { patientId, patientName });
                }
              }
            }
          }
        } catch (error) {
          console.error('通过挂号ID获取患者信息失败:', error);
        }
      }

      // 设置问诊信息
      consultation.value = {
        id: roomInfo.registrationId || roomInfo.registration_id, // 预约ID - 兼容两种字段名
        roomId: roomInfo.id, // 房间ID
        doctorId: roomInfo.doctorId || roomInfo.doctor_id || 131,
        doctorName: roomInfo.doctorName || roomInfo.doctor_name || '李医生', // 使用真实医生姓名
        // 仅使用 registrationInfo.patientId（patient_attendant.id），不要回退到 roomInfo 中的系统用户ID
        patientId: patientId || null,
        patientName: patientName || roomInfo.patientName || roomInfo.patient_name || '张三',
        patientAvatar: 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAiIGhlaWdodD0iNDAiIHZpZXdCb3g9IjAgMCA0MCA0MCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMjAiIGN5PSIyMCIgcj0iMjAiIGZpbGw9IiNGNUY1RjUiLz4KPHN2ZyB3aWR0aD0iMjQiIGhlaWdodD0iMjQiIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTEyIDEyQzE0LjIwOTEgMTIgMTYgMTAuMjA5MSAxNiA4QzE2IDUuMzAwODYgMTQuMjA5MSA0IDEyIDRDOS43OTA4NiA0IDggNS43OTA4NiA4IDhDOCAxMC4yMDkxIDkuNzkwODYgMTIgMTJaIiBmaWxsPSIjOTk5OTk5Ii8+CjxwYXRoIGQ9Ik0xMiAxNEM5LjMzIDEzLjk5IDcuMDEgMTUuNjIgNiAxOEMxMC4wMSAyMCAxMy45OSAyMCAxOCAxOEMxNi45OSAxNS42MiAxNC42NyAxMy45OSAxMiAxNFoiIGZpbGw9IiM5OTk5OTkiLz4KPC9zdmc+Cjwvc3ZnPgo8L3N2Zz4K',
        patientGender: '男',
        patientAge: 35,
        type: '图文问诊'
      };

      console.log('✅ 设置问诊信息:', consultation.value);
      console.log('✅ 最终患者ID:', consultation.value.patientId);
      console.log('✅ 最终患者姓名:', consultation.value.patientName);

      // 设置房间状态
      roomStatus.value = roomInfo.roomStatus || roomInfo.room_status;
      await updateSystemMessage(roomInfo.roomStatus || roomInfo.room_status);

      // 获取聊天记录（使用registration_id）
      await loadChatHistory();

      // 初始化WebSocket
      initWebSocket();

    } else {
      throw new Error('获取房间信息失败');
    }

  } catch (error) {
    console.error('获取问诊信息失败:', error);
    ElMessage.error('获取问诊信息失败');
  }
}

// 更新系统消息
async function updateSystemMessage(status) {
  console.log('🔌 updateSystemMessage函数被调用，状态:', status);
  if (status === 1) {
    systemMessage.value = '等待患者同意开始问诊，医生可以先发送消息';
  } else if (status === 2) {
    systemMessage.value = '问诊进行中';
  } else if (status === 3) {
    systemMessage.value = '问诊已结束';
  } else if (status === 4) {
    console.log('🔌 检测到患者超时未响应，调用handlePatientTimeoutOrReject');
    systemMessage.value = '患者超时未响应，问诊已取消';
    // 调用后端接口，将状态改为挂起
    await handlePatientTimeoutOrReject();
  } else if (status === 5) {
    console.log('🔌 检测到患者拒绝问诊，调用handlePatientTimeoutOrReject');
    systemMessage.value = '患者拒绝问诊';
    // 调用后端接口，将状态改为挂起
    await handlePatientTimeoutOrReject();
  }
}

// 处理患者超时或拒绝
async function handlePatientTimeoutOrReject() {
  console.log('🔌 handlePatientTimeoutOrReject函数被调用');
  console.log('🔌 当前时间:', new Date().toLocaleString());
  console.log('🔌 当前路由参数:', route.params);
  console.log('🔌 当前路由查询:', route.query);
  
  try {
    // 优先使用consultation.value.id，如果为空则尝试从roomId获取房间信息
    let finalRegistrationId = consultation.value.id;
    
    console.log('🔌 consultation.value:', consultation.value);
    console.log('🔌 consultation.value.id:', consultation.value.id);
    console.log('🔌 roomId.value:', roomId.value);
    
    if (!finalRegistrationId) {
      console.log('🔌 consultation.value.id为空，尝试从roomId获取房间信息');
      if (roomId.value) {
        try {
          const roomResponse = await getChatRoomById(roomId.value);
          if (roomResponse.code === 200 && roomResponse.data) {
            const roomInfo = roomResponse.data;
            finalRegistrationId = roomInfo.registrationId || roomInfo.registration_id;
            console.log('🔌 从房间信息获取到registrationId:', finalRegistrationId);
          }
        } catch (error) {
          console.error('🔌 获取房间信息失败:', error);
        }
      }
    }
    
    // 如果还是为空，尝试从路由参数中获取
    if (!finalRegistrationId) {
      console.log('🔌 roomId获取失败，尝试从路由参数获取');
      finalRegistrationId = route.params.id;
    }
    
    if (!finalRegistrationId) {
      console.error('🔌 无法获取registrationId');
      console.error('🔌 consultation.value:', consultation.value);
      console.error('🔌 roomId.value:', roomId.value);
      console.error('🔌 route.params:', route.params);
      ElMessage.error('无法获取问诊ID，请刷新页面重试');
      return;
    }
    
    console.log('🔌 最终使用的registrationId:', finalRegistrationId);
    console.log('🔌 调用changeStatusToSuspended接口，registrationId:', finalRegistrationId);
    console.log('🔌 调用changeStatusToSuspended接口，URL:', '/front/doctor/registration/changeStatusToSuspended');
    console.log('🔌 调用changeStatusToSuspended接口，参数:', { registrationId: finalRegistrationId });
    
    const response = await changeStatusToSuspended(finalRegistrationId);
    console.log('🔌 changeStatusToSuspended接口响应:', response);
    
    if (response.code === 200) {
      console.log('✅ 患者超时或拒绝，已将挂号状态改为挂起');
      ElMessage.success('患者超时或拒绝，已将挂号状态改为挂起');
      
      // 通知Consultations.vue更新状态
      const statusUpdate = {
        type: 'registration_status_update',
        registrationId: finalRegistrationId,
        newStatus: 5, // 5表示挂起状态
        timestamp: Date.now()
      };
      localStorage.setItem('registrationStatusUpdate', JSON.stringify(statusUpdate));
      
      // 触发自定义事件，通知其他组件更新
      window.dispatchEvent(new CustomEvent('registrationStatusUpdated', {
        detail: statusUpdate
      }));
      
    } else {
      console.error('❌ 处理患者超时或拒绝失败:', response);
      ElMessage.error('处理患者超时或拒绝失败');
    }
  } catch (error) {
    console.error('❌ 处理患者超时或拒绝失败:', error);
    console.error('❌ 错误详情:', error.message);
    console.error('❌ 错误堆栈:', error.stack);
    ElMessage.error('处理患者超时或拒绝失败');
  }
}

// 获取聊天室状态
async function getRoomStatus() {
  try {
    if (!roomId.value) return;

    const response = await getChatRoom(roomId.value);

    if (response.code === 200 && response.data) {
      roomStatus.value = response.data.roomStatus || response.data.room_status;
      await updateSystemMessage(response.data.roomStatus || response.data.room_status);
    }
  } catch (error) {
    console.error('获取聊天室状态失败:', error);
  }
}



// 获取聊天历史记录
const loadChatHistory = async () => {
  try {
    console.log('=== 医生端开始获取聊天记录 ===');
    console.log('roomId:', roomId.value);
    console.log('consultation.value.id:', consultation.value.id);

    const chatRoomId = roomId.value || consultation.value.id;
    if (!chatRoomId) {
      console.warn('房间ID为空，无法获取聊天记录');
      return;
    }

    const response = await getChatMessages(chatRoomId);
    console.log('医生端聊天记录API响应:', response);

    if (response.code === 200 && response.data && Array.isArray(response.data)) {
      // 映射消息数据
      messages.value = response.data.map(msg => ({
        id: msg.id,
        sender: msg.sender_type === 2 ? 'doctor' : 'patient',
        senderName: msg.sender_type === 2 ? (consultation.value.doctorName || '李医生') : consultation.value.patientName,
        avatar: msg.sender_type === 2 ? 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iMTYiIGZpbGw9IiM0MDlFRkYiLz4KPHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4PSI2IiB5PSI2Ij4KPHBhdGggZD0iTTEwIDEwQzExLjY1NjkgMTAgMTMgOC42NTY4NSAxMyA3QzEzIDUuMzQzMTUgMTEuNjU2OSA0IDEwIDRDOC4zNDMxNSA0IDcgNS4zNDMxNSA3IDdDNyA4LjY1boro5IDguMzQzMTUgMTAgMTBaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTAgMTJDNy43NzUgMTEuOTkgNS44NDI1IDEzLjAxNSA1IDE1QzguMzQgMTYuNSAxMS42NiAxNi41IDE1IDE1QzE0LjE1NzUgMTMuMDE1IDEyLjIyNSAxMS45OSAxMCAxMloiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo8L3N2Zz4K' : consultation.value.patientAvatar,
        type: msg.message_type === 1 ? 'text' : 'image',
        content: msg.content,
        time: new Date(msg.create_time).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        timestamp: new Date(msg.create_time)
      }));

      console.log('✅ 医生端成功加载聊天记录，消息数量:', messages.value.length);
      console.log('历史消息详情:', messages.value);

      // 确保DOM更新后滚动到底部
      await nextTick();
      setTimeout(() => {
        scrollToBottom();
      }, 100);
    } else {
      console.warn('医生端获取聊天记录失败或无数据:', response);
    }
  } catch (error) {
    console.error('医生端获取聊天记录失败:', error)
    ElMessage.error('获取聊天记录失败，请刷新页面重试')
  }
}

// 初始化WebSocket
function initWebSocket() {
  if (!roomId.value) {
    console.error('房间ID为空，无法建立WebSocket连接');
    return;
  }

  console.log('开始建立WebSocket连接，房间ID:', roomId.value);

  // 连接WebSocket
  WebSocketService.connect(roomId.value);

  // 监听患者问诊响应（通过医生端长连接）
  WebSocketService.onConsultationResponse(async (data) => {
    console.log('📨 收到患者问诊响应:', data);
    if (data.response === 'accept') {
      roomStatus.value = 2; // 设置为问诊中
      systemMessage.value = '患者已同意开始问诊，可以开始聊天了';
      ElMessage.success('患者已同意开始问诊');
      
      // 调用后端接口，将状态设置为问诊中
      try {
        const registrationId = consultation.value.id || route.params.id;
        if (registrationId) {
          console.log('🔌 调用changeStatusToInProgress，registrationId:', registrationId);
          await changeStatusToInProgress(registrationId);
          console.log('✅ 状态已更新为问诊中');
        } else {
          console.error('❌ 无法获取registrationId');
        }
      } catch (error) {
        console.error('❌ 更新状态为问诊中失败:', error);
        ElMessage.error('更新状态失败');
      }
    } else if (data.response === 'reject') {
      roomStatus.value = 5; // 设置为患者拒绝
      systemMessage.value = '患者拒绝问诊';
      ElMessage.warning('患者拒绝问诊');
      
      // 断开医生端连接
      if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
        console.log('🔌 患者拒绝问诊，断开医生端连接');
        WebSocketService.ws.close(1000, 'Patient rejected consultation');
        WebSocketService.ws = null;
      }
      
      // 调用后端接口，将状态改为挂起
      await handlePatientTimeoutOrReject();
    }
  });

  // 监听消息
  WebSocketService.onMessage(async (data) => {
    console.log('📨 医生端收到WebSocket消息:', data);
    console.log('📨 消息详细信息:', JSON.stringify(data, null, 2));

    if (data.type === 'connection') {
      // 连接成功消息
      console.log('✅ WebSocket连接已建立，房间ID:', data.roomId);
      systemMessage.value = '聊天连接已建立';
    } else if (data.type === 'ready') {
      // 患者准备就绪消息
      console.log('✅ 患者已准备就绪');
      systemMessage.value = '患者已准备就绪，可以开始问诊';
    } else if (data.type === 'message' || data.type === 'chat') {
      console.log('📨 处理聊天消息:', data);

      // 处理聊天消息 - 支持多种数据格式
      let messageData = data.data || data;
      let senderId = messageData.senderId || messageData.sender_id || data.senderId;
      let senderType = messageData.senderType || messageData.sender_type || data.senderType;
      let messageType = messageData.messageType || messageData.message_type || data.messageType || 1;
      let content = messageData.content || data.content;

      console.log('📨 解析消息数据:', {
        senderId,
        senderType,
        messageType,
        content,
        originalData: data
      });

      // 检查是否是当前医生发送的消息，避免重复添加
      const currentDoctorId = userStore.userInfo?.userId || consultation.value.doctorId;
      if (senderType === 2 && senderId === currentDoctorId) {
        console.log('📨 检测到医生自己发送的消息，检查是否已存在');

        // 检查消息是否已经存在（基于内容和时间戳）
        const existingMessage = messages.value.find(msg =>
          msg.sender === 'doctor' &&
          msg.content === content &&
          Math.abs(msg.timestamp - new Date(messageData.timestamp || Date.now())) < 1000 // 1秒内的消息认为是同一消息
        );

        if (existingMessage) {
          console.log('📨 消息已存在，跳过添加');
          return;
        }
      }

      const message = {
        id: messageData.id || Date.now(),
        sender: senderType === 2 ? 'doctor' : 'patient',
        senderName: senderType === 2 ? (consultation.value.doctorName || '李医生') : consultation.value.patientName,
        avatar: senderType === 2 ? 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzIiIGhlaWdodD0iMzIiIHZpZXdCb3g9IjAgMCAzMiAzMiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPGNpcmNsZSBjeD0iMTYiIGN5PSIxNiIgcj0iMTYiIGZpbGw9IiM0MDlFRkYiLz4KPHN2ZyB3aWR0aD0iMjAiIGhlaWdodD0iMjAiIHZpZXdCb3g9IjAgMCAyMCAyMCIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIiB4PSI2IiB5PSI2Ij4KPHBhdGggZD0iTTEwIDEwQzExLjY1NjkgMTAgMTMgOC42NTY4NSAxMyA3QzEzIDUuMzQzMTUgMTEuNjU2OSA0IDEwIDRDOC4zNDMxNSA0IDcgNS4zNDMxNSA3IDdDNyA4LjY1boro5IDguMzQzMTUgMTAgMTBaIiBmaWxsPSJ3aGl0ZSIvPgo8cGF0aCBkPSJNMTAgMTJDNy43NzUgMTEuOTkgNS44NDI1IDEzLjAxNSA1IDE1QzguMzQgMTYuNSAxMS42NiAxNi41IDE1IDE1QzE0LjE1NzUgMTMuMDE1IDEyLjIyNSAxMS45OSAxMCAxMloiIGZpbGw9IndoaXRlIi8+Cjwvc3ZnPgo8L3N2Zz4K' : consultation.value.patientAvatar,
        type: messageType === 1 ? 'text' : 'image',
        content: content,
        time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }),
        timestamp: new Date(messageData.timestamp || Date.now())
      };

      console.log('📨 添加消息到聊天记录:', message);

      messages.value.push(message);

      // 检查是否是患者同意问诊的消息
      if (message.content === '患者已同意开始问诊，可以开始聊天了。') {
        console.log('✅ 患者已同意开始问诊');
        roomStatus.value = 2; // 设置为问诊中
        systemMessage.value = '问诊进行中';
        ElMessage.success('患者已同意开始问诊，可以开始聊天了');
        
        // 调用后端接口，将状态设置为问诊中
        try {
          const registrationId = consultation.value.id || route.params.id;
          if (registrationId) {
            console.log('🔌 调用changeStatusToInProgress，registrationId:', registrationId);
            await changeStatusToInProgress(registrationId);
            console.log('✅ 状态已更新为问诊中');
          } else {
            console.error('❌ 无法获取registrationId');
          }
        } catch (error) {
          console.error('❌ 更新状态为问诊中失败:', error);
          ElMessage.error('更新状态失败');
        }
      }

      nextTick(() => {
        scrollToBottom();
      });
    } else if (data.type === 'status') {
      console.log('📨 处理状态更新消息:', data);
      // 更新聊天室状态
      roomStatus.value = data.data?.room_status || data.room_status || data.roomStatus;
      if (roomStatus.value === 2) {
        systemMessage.value = '问诊进行中';
        ElMessage.success('问诊已开始，可以开始聊天了');
        
        // 调用后端接口，将状态设置为问诊中
        try {
          const registrationId = consultation.value.id || route.params.id;
          if (registrationId) {
            console.log('🔌 调用changeStatusToInProgress，registrationId:', registrationId);
            await changeStatusToInProgress(registrationId);
            console.log('✅ 状态已更新为问诊中');
          } else {
            console.error('❌ 无法获取registrationId');
          }
        } catch (error) {
          console.error('❌ 更新状态为问诊中失败:', error);
          ElMessage.error('更新状态失败');
        }
      } else if (roomStatus.value === 3) {
        systemMessage.value = '问诊已结束';
      } else if (roomStatus.value === 4) {
        roomStatus.value = 4; // 超时未响应
        systemMessage.value = '患者超时未响应，问诊已取消';
        
        // 断开医生端连接
        if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
          console.log('🔌 患者超时未响应，断开医生端连接');
          WebSocketService.ws.close(1000, 'Patient timeout');
          WebSocketService.ws = null;
        }
        
        // 直接调用后端接口
        console.log('🔌 直接调用handlePatientTimeoutOrReject (超时)');
        if (!consultation.value.id) {
          consultation.value.id = route.params.id;
          console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
        }
        await handlePatientTimeoutOrReject();
      } else if (status === 5) {
        roomStatus.value = 5; // 患者拒绝
        systemMessage.value = '患者拒绝问诊';
        
        // 断开医生端连接
        if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
          console.log('🔌 患者拒绝问诊，断开医生端连接');
          WebSocketService.ws.close(1000, 'Patient rejected consultation');
          WebSocketService.ws = null;
        }
        
        // 直接调用后端接口
        console.log('🔌 直接调用handlePatientTimeoutOrReject (拒绝)');
        if (!consultation.value.id) {
          consultation.value.id = route.params.id;
          console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
        }
        await handlePatientTimeoutOrReject();
      }
    } else if (data.type === 'consultation_response') {
      // 患者响应问诊请求
      console.log('📨 收到患者问诊响应:', data);
      if (data.response === 'accept') {
        roomStatus.value = 2; // 设置为问诊中
        systemMessage.value = '患者已同意开始问诊，可以开始聊天了';
        ElMessage.success('患者已同意开始问诊');
        
        // 调用后端接口，将状态设置为问诊中
        try {
          const registrationId = consultation.value.id || route.params.id;
          if (registrationId) {
            console.log('🔌 调用changeStatusToInProgress，registrationId:', registrationId);
            await changeStatusToInProgress(registrationId);
            console.log('✅ 状态已更新为问诊中');
          } else {
            console.error('❌ 无法获取registrationId');
          }
        } catch (error) {
          console.error('❌ 更新状态为问诊中失败:', error);
          ElMessage.error('更新状态失败');
        }
      } else if (data.response === 'reject') {
        console.log('🔌 检测到患者拒绝问诊，设置roomStatus为5');
        roomStatus.value = 5; // 设置为患者拒绝
        systemMessage.value = '患者拒绝问诊';
        
        // 断开医生端连接
        if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
          console.log('🔌 患者拒绝问诊，断开医生端连接');
          WebSocketService.ws.close(1000, 'Patient rejected consultation');
          WebSocketService.ws = null;
        }
        
        // 直接调用后端接口，使用路由参数中的id
        console.log('🔌 直接调用handlePatientTimeoutOrReject');
        console.log('🔌 路由参数:', route.params);
        console.log('🔌 路由参数id:', route.params.id);
        
        // 确保consultation.value.id已经设置
        if (!consultation.value.id) {
          consultation.value.id = route.params.id;
          console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
        }
        
        await handlePatientTimeoutOrReject();
      }
    } else if (data.type === 'room_status_update') {
      // 房间状态更新消息
      console.log('📨 收到房间状态更新:', data);
      const status = data.room_status || data.roomStatus;
      if (status === 2) {
        roomStatus.value = 2; // 问诊中
        systemMessage.value = '问诊进行中';
        ElMessage.success('问诊已开始，可以开始聊天了');
        
        // 调用后端接口，将状态设置为问诊中
        try {
          const registrationId = consultation.value.id || route.params.id;
          if (registrationId) {
            console.log('🔌 调用changeStatusToInProgress，registrationId:', registrationId);
            await changeStatusToInProgress(registrationId);
            console.log('✅ 状态已更新为问诊中');
          } else {
            console.error('❌ 无法获取registrationId');
          }
        } catch (error) {
          console.error('❌ 更新状态为问诊中失败:', error);
          ElMessage.error('更新状态失败');
        }
      } else if (status === 3) {
        roomStatus.value = 3; // 已结束
        systemMessage.value = '问诊已结束';
      } else if (status === 4) {
        roomStatus.value = 4; // 超时未响应
        systemMessage.value = '患者超时未响应，问诊已取消';
        
        // 断开医生端连接
        if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
          console.log('🔌 患者超时未响应，断开医生端连接');
          WebSocketService.ws.close(1000, 'Patient timeout');
          WebSocketService.ws = null;
        }
        
        // 直接调用后端接口
        console.log('🔌 直接调用handlePatientTimeoutOrReject (超时)');
        if (!consultation.value.id) {
          consultation.value.id = route.params.id;
          console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
        }
        await handlePatientTimeoutOrReject();
      } else if (status === 5) {
        roomStatus.value = 5; // 患者拒绝
        systemMessage.value = '患者拒绝问诊';
        
        // 断开医生端连接
        if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
          console.log('🔌 患者拒绝问诊，断开医生端连接');
          WebSocketService.ws.close(1000, 'Patient rejected consultation');
          WebSocketService.ws = null;
        }
        
        // 直接调用后端接口
        console.log('🔌 直接调用handlePatientTimeoutOrReject (拒绝)');
        if (!consultation.value.id) {
          consultation.value.id = route.params.id;
          console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
        }
        await handlePatientTimeoutOrReject();
      }
    } else {
      console.log('📨 其他类型消息:', data);
    }
  });

  // 监听连接状态
  WebSocketService.onConnection(() => {
    console.log('WebSocket连接成功，房间ID:', roomId.value);
    isConnected.value = true;
    ElMessage.success('聊天连接已建立');
  });

  WebSocketService.onError(async (error) => {
    console.error('WebSocket连接错误:', error);
    ElMessage.error('连接异常，请刷新页面重试');
    isConnected.value = false;
    
    // 检查房间状态，如果是患者拒绝或超时状态，调用后端接口
    console.log('🔌 WebSocket连接错误，检查房间状态:', roomStatus.value);
    if (roomStatus.value === 4 || roomStatus.value === 5) {
      console.log('🔌 检测到患者超时或拒绝状态，连接错误时调用handlePatientTimeoutOrReject');
      
      // 确保consultation.value.id已经设置
      if (!consultation.value.id) {
        consultation.value.id = route.params.id;
        console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
      }
      
      // 断开医生端连接
      if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
        console.log('🔌 患者超时或拒绝，断开医生端连接');
        WebSocketService.ws.close(1000, 'Patient timeout or rejected');
        WebSocketService.ws = null;
      }
      
      await handlePatientTimeoutOrReject();
    }
  });

  WebSocketService.onClose(async (event) => {
    console.log('WebSocket连接关闭:', event);
    isConnected.value = false;
    
    // 检查房间状态，如果是患者拒绝或超时状态，调用后端接口
    console.log('🔌 WebSocket连接断开，检查房间状态:', roomStatus.value);
    if (roomStatus.value === 4 || roomStatus.value === 5) {
      console.log('🔌 检测到患者超时或拒绝状态，连接断开时调用handlePatientTimeoutOrReject');
      
      // 确保consultation.value.id已经设置
      if (!consultation.value.id) {
        consultation.value.id = route.params.id;
        console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
      }
      
      // 断开医生端连接
      if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
        console.log('🔌 患者超时或拒绝，断开医生端连接');
        WebSocketService.ws.close(1000, 'Patient timeout or rejected');
        WebSocketService.ws = null;
      }
      
      await handlePatientTimeoutOrReject();
    }
  });
}


// 发送消息
let isSending = false; // 防止重复发送

async function sendMessage() {
  if (!inputMessage.value.trim() || !roomId.value || isSending) return;

  isSending = true; // 设置发送状态

  try {
    // 获取当前登录医生的ID
    let doctorId = userStore.userInfo?.userId;

    // 如果userStore中没有，尝试从localStorage获取
    if (!doctorId) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      doctorId = userInfo.userId;
    }

    // 如果还是没有，使用consultation中的doctorId
    if (!doctorId) {
      doctorId = consultation.value.doctorId;
    }

    console.log('📤 医生发送消息，使用的医生ID:', doctorId);

    if (!doctorId) {
      ElMessage.error('无法获取医生ID，请重新登录');
      return;
    }

    const messageData = {
      type: 'chat', // 添加消息类型，确保后端能正确路由
      roomId: roomId.value,
      senderId: doctorId, // 使用正确的医生ID
      senderType: 2, // 2表示医生
      senderName: consultation.value.doctorName || userStore.userInfo?.username || '医生',
      messageType: 1, // 1表示文本消息
      content: inputMessage.value.trim()
    };

    console.log('📤 发送消息数据:', messageData);

    // 通过WebSocket发送消息
    try {
      const success = WebSocketService.send(messageData);

      if (success) {
        console.log('✅ 消息发送成功');

        // 清空输入框
        inputMessage.value = '';

        ElMessage.success('消息发送成功');
      } else {
        console.error('❌ 消息发送失败');
        ElMessage.error('消息发送失败，请检查网络连接');
      }
    } catch (error) {
      console.error('发送消息异常:', error);
      ElMessage.error('发送消息失败');
    }

  } catch (error) {
    console.error('发送消息失败:', error);
    ElMessage.error('发送消息失败');
  } finally {
    isSending = false; // 重置发送状态
  }
}



// 滚动到底部
function scrollToBottom() {
  if (chatContentRef.value) {
    chatContentRef.value.scrollTop = chatContentRef.value.scrollHeight;
  }
}

// 插入快捷回复
function insertQuickReply(reply) {
  inputMessage.value = reply;
  showQuickReply.value = false;
}

// 选择图片
function selectImage() {
  imageInputRef.value.click();
}

// 处理图片选择
async function handleImageSelect(event) {
  const file = event.target.files[0];
  if (file && roomId.value) {
    try {
      // 获取当前登录医生的ID
      let doctorId = userStore.userInfo?.userId;

      // 如果userStore中没有，尝试从localStorage获取
      if (!doctorId) {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
        doctorId = userInfo.userId;
      }

      // 如果还是没有，使用consultation中的doctorId
      if (!doctorId) {
        doctorId = consultation.value.doctorId;
      }

      console.log('📤 医生发送图片，使用的医生ID:', doctorId);

      if (!doctorId) {
        ElMessage.error('无法获取医生ID，请重新登录');
        return;
      }

      const formData = new FormData();
      formData.append('file', file); // 修改字段名为'file'以匹配后端期望
      formData.append('room_id', roomId.value);
      formData.append('sender_id', doctorId); // 使用正确的医生ID
      formData.append('sender_type', 2); // 添加发送者类型，2表示医生
      formData.append('sender_name', consultation.value.doctorName || userStore.userInfo?.username || '医生'); // 使用实际医生姓名

      console.log('📤 医生发送图片，参数:', {
        room_id: roomId.value,
        sender_id: doctorId,
        sender_type: 2,
        sender_name: consultation.value.doctorName || userStore.userInfo?.username || '医生'
      });

      // 上传图片到七牛云
      const response = await uploadChatImage(formData);

      if (response.code === 200) {
        // 图片已经通过后端上传并保存到数据库，直接显示
        ElMessage.success('图片发送成功');
      } else {
        ElMessage.error(response.message || '图片上传失败');
      }

    } catch (error) {
      console.error('图片上传失败:', error);
      ElMessage.error('图片上传失败');
    }
  }
  event.target.value = '';
}



// 获取类型标签样式
function getTypeTag(type) {
  const typeMap = {
    '图文问诊': 'primary',
    '视频问诊': 'success',
    '电话问诊': 'warning'
  };
  return typeMap[type] || 'info';
}

// 获取房间状态文本
function getRoomStatusText(status) {
  const statusMap = {
    1: '等待中',
    2: '进行中',
    3: '已结束',
    4: '超时未响应',
    5: '患者拒绝'
  };
  return statusMap[status] || '未知状态';
}

// 获取房间状态标签类型
function getRoomStatusType(status) {
  const typeMap = {
    1: 'warning',   // 等待中 - 黄色
    2: 'success',   // 进行中 - 绿色
    3: 'info',      // 已结束 - 蓝色
    4: 'danger',    // 超时未响应 - 红色
    5: 'danger'     // 患者拒绝 - 红色
  };
  return typeMap[status] || 'info';
}

// 获取状态描述
function getStatusDescription(status) {
  const descriptionMap = {
    1: '等待患者同意开始问诊，医生可以先发送消息与患者沟通',
    2: '问诊进行中，医生和患者可以正常聊天',
    3: '问诊已结束，无法继续聊天',
    4: '患者超时未响应，医生可以处理此房间',
    5: '患者拒绝问诊，问诊已结束'
  };
  return descriptionMap[status] || '未知状态';
}

// 显示就诊记录对话框
function showRecordDialog() {
  // 获取医生ID（系统用户ID，后端会转换）
  let doctorId = userStore.userInfo?.userId;
  if (!doctorId) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    doctorId = userInfo.userId;
  }
  if (!doctorId) {
    doctorId = consultation.value.doctorId;
  }

  recordForm.value.patientId = consultation.value.patientId;
  recordForm.value.doctorId = doctorId;
  recordDialogVisible.value = true;
}

// 显示处方对话框
function showPrescriptionDialog() {
  // 获取医生ID
  let doctorId = userStore.userInfo?.userId;
  if (!doctorId) {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    doctorId = userInfo.userId;
  }
  if (!doctorId) {
    doctorId = consultation.value.doctorId;
  }

  prescriptionForm.value.patientId = consultation.value.patientId; // 使用 RegistrationInfo.patientId
  prescriptionForm.value.doctorId = doctorId;
  prescriptionDialogVisible.value = true;
}

// 提交就诊记录
async function submitRecord() {
  if (!recordFormRef.value) return;
  
  await recordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 保存就诊记录到本地状态，等待结束问诊时一起提交
        console.log('就诊记录已保存到本地:', recordForm.value);
        ElMessage.success('就诊记录已保存');
        recordDialogVisible.value = false;
        
        // 不重置表单，保持数据用于结束问诊时提交
      } catch (error) {
        console.error('保存就诊记录失败:', error);
        ElMessage.error('保存就诊记录失败');
      }
    }
  });
}

// 提交处方
async function submitPrescription() {
  if (!prescriptionFormRef.value) return;
  
  await prescriptionFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 保存处方到本地状态，等待结束问诊时一起提交
        console.log('处方已保存到本地:', prescriptionForm.value);
        ElMessage.success('处方已保存');
        prescriptionDialogVisible.value = false;
        
        // 不重置表单，保持数据用于结束问诊时提交
      } catch (error) {
        console.error('保存处方失败:', error);
        ElMessage.error('保存处方失败');
      }
    }
  });
}

// 添加药品
function addMedicine() {
  prescriptionForm.value.medicines.push({
    drugId: '',
    quantity: 1
  });
}

// 删除药品
function removeMedicine(index) {
  if (prescriptionForm.value.medicines.length > 1) {
    prescriptionForm.value.medicines.splice(index, 1);
  }
}

// 药品选择变化
function onMedicineChange(index) {
  // 可以在这里处理药品选择变化的逻辑
  console.log('药品选择变化:', index, prescriptionForm.value.medicines[index]);
}

// 结束问诊
function finishConsultation() {
  finishDialogVisible.value = true;
}

// 确认结束问诊
async function confirmFinishConsultation() {
  try {
    // 获取医生ID
    let doctorId = userStore.userInfo?.userId;
    console.log('🔍 初始获取到的医生ID (userStore):', doctorId);
    
    if (!doctorId) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
      doctorId = userInfo.userId;
      console.log('🔍 从localStorage获取到的医生ID:', doctorId);
    }
    
    if (!doctorId) {
      doctorId = consultation.value.doctorId;
      console.log('🔍 从consultation获取到的医生ID:', doctorId);
    }

    // 确保医生ID是数字类型
    if (doctorId) {
      // 如果doctorId是字符串，尝试转换为数字
      if (typeof doctorId === 'string') {
        doctorId = parseInt(doctorId);
      }
      console.log('🔍 转换后的医生ID (数字):', doctorId);
      console.log('🔍 医生ID类型:', typeof doctorId);
    }

    if (!doctorId || isNaN(doctorId)) {
      console.error('❌ 无法获取有效的医生ID');
      console.error('❌ doctorId值:', doctorId);
      console.error('❌ doctorId类型:', typeof doctorId);
      ElMessage.error('无法获取医生ID，请重新登录');
      return;
    }

    // 获取患者ID
    let patientId = consultation.value.patientId;
    
    // 如果consultation中没有patientId，尝试从其他地方获取
    if (!patientId) {
      console.warn('consultation.value.patientId为空，尝试从其他方式获取');
      // 尝试从问诊记录中获取患者ID
      if (consultation.value.id) {
        try {
          // 获取全部问诊记录
          const allConsultationsResponse = await getAllRegistrationInfoList();
          if (allConsultationsResponse.code === 200 && allConsultationsResponse.data) {
              const consultationRecord = allConsultationsResponse.data.find(item => item.id === consultation.value.id);
              if (consultationRecord) {
              // 使用registrationInfo中的patientId（patient_attendant表主键）
              patientId = consultationRecord.patientId;
              console.log('✅ 从全部问诊记录获取到患者ID:', patientId);
            }
          }
          
          // 如果全部问诊中没有找到，尝试从待处理问诊中获取
          if (!patientId) {
            const pendingConsultationsResponse = await getRegistrationList();
            if (pendingConsultationsResponse.code === 200 && pendingConsultationsResponse.data) {
              const consultationRecord = pendingConsultationsResponse.data.find(item => item.id === consultation.value.id);
              if (consultationRecord) {
                // 使用registrationInfo中的patientId（patient_attendant表主键）
                patientId = consultationRecord.patientId;
                console.log('✅ 从待处理问诊记录获取到患者ID:', patientId);
              }
            }
          }
        } catch (error) {
          console.error('获取问诊记录失败:', error);
        }
      }
      
      // 移除从roomInfo中获取patientId（该值为系统用户ID，非patient_attendant主键）
    }

    // 确保患者ID是数字类型（patient_attendant主键）
    if (patientId) {
      if (typeof patientId === 'string') {
        patientId = parseInt(patientId);
      }
      console.log('🔍 转换后的患者ID (patient_attendant.id):', patientId);
      console.log('🔍 患者ID类型:', typeof patientId);
    }

    console.log('最终使用的patientId:', patientId);
    console.log('最终使用的doctorId:', doctorId);
    console.log('consultation.value:', consultation.value);

    if (!patientId || isNaN(patientId)) {
      ElMessage.error('无法获取患者ID，请刷新页面重试');
      return;
    }

    // 检查是否有就诊记录描述
    const doctorDescription = recordForm.value.description || '';

    // 检查是否有处方内容
    const hasPrescription = prescriptionForm.value.medicines && 
                           prescriptionForm.value.medicines.length > 0 && 
                           prescriptionForm.value.medicines.some(medicine => medicine.drugId && medicine.drugId !== '');

    // 确定处方状态：如果处方内容为空，状态为2；处方内容不为空，状态为0
    const isPurchasable = hasPrescription ? 0 : 2;

    // 构建就诊记录数据
    const medicalRecordData = {
      doctorId: doctorId,
      patientId: patientId,
      doctorDescription: doctorDescription,
      isPurchasable: isPurchasable
    };

    console.log('准备提交就诊记录:', medicalRecordData);
    console.log('🔍 数据类型检查:');
    console.log('  - doctorId:', medicalRecordData.doctorId, '类型:', typeof medicalRecordData.doctorId);
    console.log('  - patientId:', medicalRecordData.patientId, '类型:', typeof medicalRecordData.patientId);
    console.log('  - doctorDescription:', medicalRecordData.doctorDescription, '类型:', typeof medicalRecordData.doctorDescription);
    console.log('  - isPurchasable:', medicalRecordData.isPurchasable, '类型:', typeof medicalRecordData.isPurchasable);

    // 调用就诊记录API
    const recordResponse = await addMedicalRecord(medicalRecordData);
    
    if (recordResponse.code === 200) {
      ElMessage.success('就诊记录已保存');
      
      // 如果有处方内容，调用处方API
      if (hasPrescription) {
        try {
          // 构建处方数据 - 确保格式正确
          let medicinesList = prescriptionForm.value.medicines
            .filter(medicine => medicine.drugId && medicine.drugId !== '')
            .map(medicine => {
              // 确保数据类型正确
              const drugId = parseInt(medicine.drugId);
              const drugQuantity = parseInt(medicine.quantity);
              
              // 验证数据有效性
              if (isNaN(drugId) || isNaN(drugQuantity)) {
                throw new Error(`药品数据无效: drugId=${medicine.drugId}, quantity=${medicine.quantity}`);
              }
              
              return {
                drugId: drugId,
                drugQuantity: drugQuantity
              };
            });

          // 验证是否有有效的药品数据
          if (medicinesList.length === 0) {
            console.warn('没有有效的药品数据，跳过处方提交');
            ElMessage.warning('处方数据无效，但就诊记录已保存');
          } else {
            console.log('准备提交处方:');
            console.log('🔍 处方数据格式检查:');
            console.log('  - medicalRecordId:', recordResponse.data, '类型:', typeof recordResponse.data);
            console.log('  - medicines:', medicinesList, '类型:', Array.isArray(medicinesList) ? 'Array' : typeof medicinesList);
            console.log('  - medicines长度:', medicinesList.length);
            console.log('  - medicines内容:', JSON.stringify(medicinesList));
            
            // 确保medicines是数组格式
            if (!Array.isArray(medicinesList)) {
              console.error('medicines不是数组格式，强制转换为数组');
              medicinesList = [medicinesList];
            }
            
            // 调用处方API - 修改为新的函数签名
            const prescriptionResponse = await addPrescription(medicinesList, recordResponse.data);
            
            if (prescriptionResponse.code === 200) {
              ElMessage.success('处方已保存');
            } else {
              console.error('保存处方失败:', prescriptionResponse);
              ElMessage.warning('处方保存失败，但就诊记录已保存');
            }
          }
        } catch (error) {
          console.error('保存处方失败:', error);
          ElMessage.warning('处方保存失败，但就诊记录已保存');
        }
      }
    } else {
      console.error('保存就诊记录失败:', recordResponse);
      ElMessage.warning('就诊记录保存失败，但问诊已结束');
    }

    // 结束问诊
    if (roomId.value) {
      await endConsultation(roomId.value);
      await updateRoomStatus(roomId.value, 3);
    }

    // 调用后端接口，将状态设置为已完成
    try {
      const registrationId = consultation.value.id || route.params.id;
      if (registrationId) {
        console.log('🔌 调用changeStatusToCompleted，registrationId:', registrationId);
        await changeStatusToCompleted(registrationId);
        console.log('✅ 状态已更新为已完成');
      } else {
        console.error('❌ 无法获取registrationId');
      }
    } catch (error) {
      console.error('❌ 更新状态为已完成失败:', error);
      ElMessage.error('更新状态失败');
    }

    ElMessage.success('问诊已结束');
    finishDialogVisible.value = false;
    
    // 清空表单数据
    recordForm.value.description = '';
    prescriptionForm.value.medicines = [
      {
        drugId: '',
        quantity: 1
      }
    ];
    
    // 返回问诊列表
    router.push('/doctor/consultations');
  } catch (error) {
    console.error('结束问诊失败:', error);
    ElMessage.error('结束问诊失败');
  }
}

// 处理超时房间
async function handleTimeoutRoom() {
  try {
    // 调用后端接口，将状态改为挂起
    const registrationId = consultation.value.id;
    if (registrationId) {
      console.log('🔌 处理超时房间，调用changeStatusToSuspended接口，registrationId:', registrationId);
      const suspendResponse = await changeStatusToSuspended(registrationId);
      if (suspendResponse.code === 200) {
        console.log('✅ 超时房间状态已改为挂起');
        ElMessage.success('超时房间状态已改为挂起');
      } else {
        console.error('❌ 修改超时房间状态失败:', suspendResponse);
        ElMessage.error('修改超时房间状态失败');
      }
    }
    
    // 直接将超时房间状态改为已结束
    const response = await updateRoomStatus(roomId.value, 3);
    if (response.code === 200) {
      ElMessage.success('超时房间已处理');
      roomStatus.value = 3;
      systemMessage.value = '问诊已结束';
    } else {
      ElMessage.error(response.message || '处理失败');
    }
  } catch (error) {
    console.error('处理超时房间失败:', error);
    ElMessage.error('处理超时房间失败');
  }
}



// 返回
function goBack() {
  router.go(-1);
}

// 生命周期
onMounted(() => {
  fetchConsultationInfo();
  fetchDrugs(); // 在组件挂载时获取药品列表
});

onBeforeUnmount(async () => {
  // 检查房间状态，如果是患者拒绝或超时状态，调用后端接口
  console.log('🔌 组件卸载，检查房间状态:', roomStatus.value);
  if (roomStatus.value === 4 || roomStatus.value === 5) {
    console.log('🔌 检测到患者超时或拒绝状态，组件卸载时调用handlePatientTimeoutOrReject');
    
    // 确保consultation.value.id已经设置
    if (!consultation.value.id) {
      consultation.value.id = route.params.id;
      console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
    }
    
    // 断开医生端连接
    if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
      console.log('🔌 患者超时或拒绝，断开医生端连接');
      WebSocketService.ws.close(1000, 'Patient timeout or rejected');
      WebSocketService.ws = null;
    }
    
    await handlePatientTimeoutOrReject();
  }
  
  // 只关闭聊天室连接，保持医生端长连接
  if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
    console.log('🔌 组件卸载，关闭聊天室连接');
    WebSocketService.ws.close(1000, 'Component unmounted');
    WebSocketService.ws = null;
  }
});

// 监听房间状态变化，当状态变为4或5时立即调用后端接口
watch(roomStatus, async (newStatus, oldStatus) => {
  console.log('🔌 房间状态变化:', oldStatus, '->', newStatus);
  if ((newStatus === 4 || newStatus === 5) && (oldStatus !== 4 && oldStatus !== 5)) {
    console.log('🔌 检测到房间状态变为患者超时或拒绝，立即调用handlePatientTimeoutOrReject');
    
    // 确保consultation.value.id已经设置
    if (!consultation.value.id) {
      consultation.value.id = route.params.id;
      console.log('🔌 从路由参数设置consultation.value.id:', consultation.value.id);
    }
    
    // 断开医生端连接
    if (WebSocketService.ws && WebSocketService.ws.readyState === WebSocket.OPEN) {
      console.log('🔌 患者超时或拒绝，断开医生端连接');
      WebSocketService.ws.close(1000, 'Patient timeout or rejected');
      WebSocketService.ws = null;
    }
    
    // 延迟一点时间调用，确保状态已经更新
    setTimeout(async () => {
      await handlePatientTimeoutOrReject();
    }, 100);
  }
});

// 监听消息变化，自动滚动到底部
watch(() => messages.value.length, () => {
  nextTick(() => {
    scrollToBottom();
  });
});
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.chat-header {
  background: white;
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.patient-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.patient-detail h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.patient-detail p {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 14px;
}

.chat-content {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-image: linear-gradient(rgba(240, 242, 245, 0.8), rgba(240, 242, 245, 0.8)), url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgdmlld0JveD0iMCAwIDIwMCAyMDAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGcgZmlsbD0ibm9uZSIgZmlsbC1ydWxlPSJldmVub2RkIj48cGF0aCBmaWxsPSIjZWVlIiBkPSJNMCAwaDIwMHYyMDBIMHoiLz48cGF0aCBkPSJNMTAgMTBoMTgwdjE4MEgxMHoiIGZpbGw9IiNmZmYiLz48L2c+PC9zdmc+');
  background-repeat: repeat;
  background-size: 200px;
}

.message-list {
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 20px;
}

.connection-status {
  margin-bottom: 20px;
}

.system-message {
  text-align: center;
  margin: 16px 0;
}

.system-message p {
  display: inline-block;
  padding: 6px 12px;
  background-color: rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  font-size: 13px;
  color: #606266;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
}

.room-status-display {
  text-align: center;
  margin: 16px 0;
  animation: fadeIn 0.5s ease-in-out;
}

.status-description {
  margin-top: 5px;
  color: #666;
  font-size: 12px;
}

.message-item {
  display: flex;
  margin-bottom: 24px; /* 增加消息间距 */
  align-items: flex-start;
  animation: slideIn 0.3s ease-out;
  padding: 0 20px; /* 增加左右内边距 */
}

.message-left {
  justify-content: flex-start;
  margin-right: 60px; /* 减少右侧空白区域，让患者消息左移 */
}

.message-right {
  justify-content: flex-end;
  flex-direction: row-reverse;
  margin-left: 100px; /* 增加左侧空白区域，让医生消息右移 */
}

.message-avatar {
  margin: 0 20px; /* 增加头像间距 */
}

.message-content {
  max-width: 65%; /* 稍微减小最大宽度，让布局更宽松 */
}

.message-right .message-content {
  text-align: right;
}

.message-info {
  margin-bottom: 4px;
  font-size: 12px;
  color: #999;
}

.message-right .message-info {
  text-align: right;
}

.sender-name {
  margin-right: 8px;
}

.message-bubble {
  background: white;
  padding: 16px 22px; /* 增加内边距 */
  border-radius: 24px 24px 24px 6px; /* 增大圆角 */
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1); /* 增强阴影 */
  display: inline-block;
  transition: all 0.2s ease;
  font-size: 16px; /* 增大字体 */
  min-width: 140px; /* 增加最小宽度 */
  line-height: 1.6; /* 增加行高 */
}

.message-bubble:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15); /* 增强悬停阴影 */
}

.message-right .message-bubble {
  background: linear-gradient(135deg, #409eff, #3a8ee6);
  color: white;
  border-radius: 24px 24px 6px 24px; /* 增大圆角 */
}

.message-text {
  line-height: 1.5;
  word-break: break-word;
}

.message-image {
  padding: 4px;
}

.message-file {
  display: flex;
  align-items: center;
  gap: 8px;
}

.chat-input {
  background: white;
  border-top: 1px solid #e4e7ed;
  padding: 16px 20px;
  box-shadow: 0 -2px 12px 0 rgba(0, 0, 0, 0.05);
  z-index: 10;
}

.input-toolbar {
  margin-bottom: 12px;
  display: flex;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.quick-reply {
  margin-bottom: 12px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 8px;
  box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.05);
}

.input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}

.input-area .el-textarea :deep(.el-textarea__inner) {
  border-radius: 8px;
  transition: all 0.3s ease;
  resize: none;
  box-shadow: 0 0 0 1px #dcdfe6;
}

.input-area .el-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.3);
}

.send-button {
  height: 40px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.medicine-list {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.medicine-item {
  margin-bottom: 12px;
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.medicine-item:last-child {
  margin-bottom: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
/* 添加动画效果 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

/* 添加发送按钮悬停效果 */
.send-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

/* 添加工具栏按钮悬停效果 */
.input-toolbar .el-button {
  transition: all 0.3s ease;
}

.input-toolbar .el-button:hover {
  color: #409eff;
  transform: translateY(-2px);
}

/* 添加头部按钮悬停效果 */
.chat-actions .el-button {
  transition: all 0.3s ease;
}

.chat-actions .el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 添加滚动条样式 */
.chat-content::-webkit-scrollbar {
  width: 6px;
}

.chat-content::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.03);
  border-radius: 3px;
}

.chat-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

.chat-content::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}
</style>