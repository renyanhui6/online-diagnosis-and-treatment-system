<template>
  <div class="triage-module">
    <div class="triage-hero">
      <div class="triage-hero-content">
        <div class="triage-badge">AI 智能分诊</div>
        <h2>先描述症状，再决定挂号方向</h2>
        <p>
          不确定该挂哪个科室时，先和 AI 助手对话。系统会优先结合院内真实科室、子科室和排班信息给出建议。
        </p>
      </div>
      <div class="triage-hero-side">
        <el-alert
          title="分诊结果仅供挂号参考，出现胸痛、呼吸困难、意识障碍等急症时请直接线下急诊。"
          type="warning"
          :closable="false"
          show-icon
        />
      </div>
    </div>

    <div class="triage-workspace">
      <div class="triage-chat-panel">
        <div class="triage-panel-header">
          <div>
            <div class="triage-section-title">对话区</div>
            <div class="triage-session-meta">
              {{ currentSessionStatus === 'CLOSED' ? '历史会话' : '当前会话' }}
              <span v-if="triageSessionId">#{{ triageSessionId.slice(-8) }}</span>
            </div>
          </div>
          <el-button plain @click="openTriageHistory">历史分诊</el-button>
        </div>
        <div class="triage-chat-messages">
          <div
            v-for="(message, index) in triageMessages"
            :key="`${message.role}-${index}`"
            class="triage-message"
            :class="message.role === 'assistant' ? 'assistant' : 'user'"
          >
            <div class="triage-message-role">{{ message.role === 'assistant' ? 'AI 助手' : '我' }}</div>
            <div class="triage-message-content">{{ message.content }}</div>
          </div>
        </div>

        <el-input
          v-model="triageInput"
          type="textarea"
          :rows="4"
          resize="none"
          :disabled="currentSessionStatus === 'CLOSED'"
          placeholder="请描述你的主要不适，例如：咳嗽三天伴低热、胸闷，夜间加重。"
          @keyup.ctrl.enter="sendTriageMessage"
        />

        <div class="triage-chat-actions">
          <el-button @click="resetTriageDialog">重新开始</el-button>
          <el-button
            type="primary"
            :loading="triageLoading"
            :disabled="currentSessionStatus === 'CLOSED'"
            @click="sendTriageMessage"
          >
            发送症状
          </el-button>
        </div>
      </div>

      <div class="triage-result-panel">
        <div class="triage-section-title">推荐结果</div>
        <el-alert
          v-if="showAiFallbackNotice"
          class="ai-status-alert"
          :title="aiFallbackTitle"
          :description="aiFallbackDescription"
          type="warning"
          show-icon
          :closable="false"
        />

        <div class="triage-result-card">
          <template v-if="triageResult.recommendedSubDepartments.length || triageResult.recommendedDepartments.length">
            <div class="triage-result-header">
              <span class="triage-result-label">推荐方向</span>
              <span class="triage-result-source">{{ triageResult.sourceLabel }}</span>
            </div>
            <div class="triage-result-names">
              {{
                (triageResult.recommendedSubDepartments.length
                  ? triageResult.recommendedSubDepartments
                  : triageResult.recommendedDepartments).join('，')
              }}
            </div>
            <p class="triage-result-reason">{{ triageResult.rationale || '系统会根据对话信息继续收敛推荐范围。' }}</p>
            <div class="triage-actions">
              <el-button
                v-for="name in (triageResult.recommendedSubDepartments.length
                  ? triageResult.recommendedSubDepartments
                  : triageResult.recommendedDepartments)"
                :key="name"
                type="primary"
                plain
                @click="applyTriageRecommendation(name)"
              >
                去挂“{{ name }}”
              </el-button>
            </div>
          </template>
          <template v-else>
            <el-empty description="还没有形成推荐，请先描述症状或补充 AI 追问信息。" />
          </template>
        </div>

        <div class="triage-result-card follow-up-card">
          <div class="triage-result-header">
            <span class="triage-result-label">当前建议</span>
          </div>
          <ul class="triage-guide-list">
            <li>症状越具体，推荐越稳定。</li>
            <li>优先描述部位、时长、伴随症状和严重程度。</li>
            <li>拿到推荐后会自动跳转到预约挂号页并带入科室。</li>
          </ul>
        </div>
      </div>
    </div>

    <el-dialog
      v-model="historyDialogVisible"
      title="历史分诊"
      width="860px"
    >
      <el-table :data="triageSessionList" border empty-text="暂无历史分诊会话">
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">{{ row.status === 'ACTIVE' ? '进行中' : '已关闭' }}</template>
        </el-table-column>
        <el-table-column label="来源" width="150">
          <template #default="{ row }">{{ sourceLabel(row.source) }}</template>
        </el-table-column>
        <el-table-column label="推荐方向" min-width="220">
          <template #default="{ row }">
            {{ displayRecommendations(row) || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="loadTriageHistory(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <el-dialog
      v-model="aiUnavailableDialogVisible"
      title="AI 服务不可用"
      width="720px"
    >
      <pre class="ai-unavailable-pre">{{ aiUnavailableText }}</pre>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="aiUnavailableDialogVisible = false">我知道了</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  chatTriageAgent,
  closeTriageSession,
  getAiStatus,
  getTriageSessionList,
  getTriageSessionMessages,
  startTriageSession
} from '../api/ai'
import { getDepartmentList, getSubDepartmentList } from '../api/appointment'

const router = useRouter()

const triageLoading = ref(false)
const historyDialogVisible = ref(false)
const aiUnavailableDialogVisible = ref(false)
const aiUnavailableText = ref('')
const triageSessionId = ref('')
const currentSessionStatus = ref('ACTIVE')
const triageInput = ref('')
const triageMessages = ref([])
const triageSessionList = ref([])
const aiStatus = ref(null)
const departments = ref([])
const subDepartmentsCache = ref({})

const AI_UNAVAILABLE_FALLBACK = `AI 分诊暂时不可用。

可能原因：
- 后端未配置在线模型 Key
- AI 服务连接失败
- 当前请求触发了限流

你仍然可以返回预约挂号页手动选择科室。`

const triageResult = reactive({
  recommendedDepartments: [],
  recommendedSubDepartments: [],
  rationale: '',
  sourceLabel: '院内知识优先'
})

const showAiFallbackNotice = computed(() => aiStatus.value?.effectiveMode === 'LOCAL_FALLBACK')

const aiFallbackTitle = computed(() => {
  if (aiStatus.value?.apiKeyPresent === false) {
    return '当前使用本地分诊兜底：后端没有读取到 DeepSeek Key'
  }
  return '当前使用本地分诊兜底'
})

const aiFallbackDescription = computed(() => {
  if (aiStatus.value?.apiKeyPresent === false) {
    return '请检查 medical-back/src/main/resources/application-local.secret.yml 或 DEEPSEEK_API_KEY 环境变量，配置后需要重启后端。'
  }
  return '后端已启用兜底策略，在线模型调用失败时会返回院内规则推荐。可打开 /treat/ai/status 查看详细状态。'
})

const normalizeName = (value) => (value || '').replace(/\s+/g, '').trim()

const sourceLabel = (source) => {
  if (source === 'langchain-agent' || source === 'online-agent') return 'DeepSeek 在线 Agent'
  if (source === 'local-fallback') return '本地分诊兜底'
  return '院内知识优先'
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

const displayRecommendations = (session) => {
  const names = session?.recommendedSubDepartments?.length
    ? session.recommendedSubDepartments
    : session?.recommendedDepartments || []
  return names.join('，')
}

const setIntroMessage = (content) => {
  triageMessages.value = [{
    role: 'assistant',
    content
  }]
}

const updateSourceLabel = (source) => {
  if (source === 'langchain-agent' || source === 'online-agent') {
    triageResult.sourceLabel = 'DeepSeek 在线 Agent'
    return
  }
  if (source === 'local-fallback') {
    triageResult.sourceLabel = '本地分诊兜底'
    return
  }
  triageResult.sourceLabel = '院内知识优先'
}

const resetTriageDialogState = () => {
  triageSessionId.value = ''
  currentSessionStatus.value = 'ACTIVE'
  triageInput.value = ''
  triageResult.recommendedDepartments = []
  triageResult.recommendedSubDepartments = []
  triageResult.rationale = ''
  triageResult.sourceLabel = '院内知识优先'
  setIntroMessage('请先告诉我你的主要不适部位、持续时间和伴随症状。我会结合院内真实科室信息，帮你判断更适合挂哪个科室。')
}

const createTriageSession = async () => {
  const res = await startTriageSession()
  if (res.code === 200 && res.data?.sessionId) {
    triageSessionId.value = res.data.sessionId
    currentSessionStatus.value = res.data.status || 'ACTIVE'
    return true
  }
  ElMessage.warning(res.message || '创建 AI 分诊会话失败')
  return false
}

const resetTriageDialog = async () => {
  const oldSessionId = triageSessionId.value
  if (oldSessionId && currentSessionStatus.value === 'ACTIVE') {
    try {
      await closeTriageSession(oldSessionId)
    } catch (error) {
      console.warn('关闭 AI 分诊会话失败', error)
    }
  }
  resetTriageDialogState()
  await loadTriageSessions()
}

const loadTriageSessions = async () => {
  try {
    const res = await getTriageSessionList()
    if (res.code === 200) {
      triageSessionList.value = res.data || []
    }
  } catch (error) {
    console.warn('加载 AI 分诊历史失败', error)
  }
}

const loadAiStatus = async () => {
  try {
    const res = await getAiStatus()
    if (res.code === 200) {
      aiStatus.value = res.data || null
    }
  } catch (error) {
    console.warn('加载 AI 运行状态失败', error)
  }
}

const openTriageHistory = async () => {
  await loadTriageSessions()
  historyDialogVisible.value = true
}

const loadTriageHistory = async (session) => {
  if (!session?.sessionId) return
  try {
    const res = await getTriageSessionMessages(session.sessionId)
    if (res.code !== 200) {
      ElMessage.warning(res.message || '加载历史分诊消息失败')
      return
    }
    triageSessionId.value = session.sessionId
    currentSessionStatus.value = session.status || 'CLOSED'
    triageResult.recommendedDepartments = session.recommendedDepartments || []
    triageResult.recommendedSubDepartments = session.recommendedSubDepartments || []
    triageResult.rationale = session.summary || ''
    updateSourceLabel(session.source)
    triageMessages.value = (res.data || []).map((item) => ({
      role: item.role === 'assistant' ? 'assistant' : 'user',
      content: item.content
    }))
    if (!triageMessages.value.length) {
      setIntroMessage('这个历史会话暂无消息。')
    }
    historyDialogVisible.value = false
  } catch (error) {
    console.error('加载历史分诊消息失败', error)
    ElMessage.error('加载历史分诊消息失败')
  }
}

const loadDepartments = async () => {
  const deptRes = await getDepartmentList()
  if (deptRes.code === 200) {
    departments.value = deptRes.data || []
  }
}

const getCachedSubDepartments = async (departmentId) => {
  if (subDepartmentsCache.value[departmentId]) {
    return subDepartmentsCache.value[departmentId]
  }

  const res = await getSubDepartmentList(departmentId)
  const subDepartments = res.code === 200 ? (res.data || []) : []
  subDepartmentsCache.value = {
    ...subDepartmentsCache.value,
    [departmentId]: subDepartments
  }
  return subDepartments
}

const sendTriageMessage = async () => {
  if (currentSessionStatus.value === 'CLOSED') {
    ElMessage.warning('历史会话已关闭，请点击“重新开始”后继续咨询')
    return
  }

  const message = triageInput.value?.trim()
  if (!message) {
    ElMessage.warning('请先输入症状描述')
    return
  }

  if (!triageSessionId.value) {
    const created = await createTriageSession()
    if (!created) return
  }

  triageMessages.value.push({ role: 'user', content: message })
  triageInput.value = ''
  triageLoading.value = true

  try {
    const resp = await chatTriageAgent({
      sessionId: triageSessionId.value,
      message
    })

    if (resp.code === 200 && resp.data) {
      triageSessionId.value = resp.data.sessionId || triageSessionId.value
      triageResult.recommendedDepartments = resp.data.recommendedDepartments || []
      triageResult.recommendedSubDepartments = resp.data.recommendedSubDepartments || []
      triageResult.rationale = resp.data.rationale || ''
      updateSourceLabel(resp.data.source)
      triageMessages.value.push({
        role: 'assistant',
        content: resp.data.assistantMessage || '我已经记录了你的情况，请继续补充。'
      })
      currentSessionStatus.value = 'ACTIVE'
      await loadTriageSessions()
      return
    }

    ElMessage.warning(resp.message || '获取推荐失败')
  } catch (error) {
    console.error('AI 对话失败', error)
    if (error && error.code === 9001) {
      aiUnavailableText.value = error.message || AI_UNAVAILABLE_FALLBACK
      aiUnavailableDialogVisible.value = true
      return
    }
    ElMessage.error(error.message || 'AI 推荐失败')
  } finally {
    triageLoading.value = false
  }
}

const applyTriageRecommendation = async (targetName) => {
  const normalizedTarget = normalizeName(targetName)
  if (!normalizedTarget) {
    return
  }

  for (const department of departments.value) {
    const candidates = await getCachedSubDepartments(department.id)
    const matchedSub = candidates.find((item) => {
      const name = normalizeName(item.departmentName)
      return name === normalizedTarget || name.includes(normalizedTarget) || normalizedTarget.includes(name)
    })

    if (matchedSub) {
      router.push({
        path: '/appointment',
        query: {
          deptId: department.id,
          subDeptId: matchedSub.id
        }
      })
      ElMessage.success(`已带入推荐科室：${department.departmentName} - ${matchedSub.departmentName}`)
      return
    }
  }

  const matchedDepartment = departments.value.find((item) => {
    const name = normalizeName(item.departmentName)
    return name === normalizedTarget || name.includes(normalizedTarget) || normalizedTarget.includes(name)
  })

  if (matchedDepartment) {
    router.push({
      path: '/appointment',
      query: {
        deptId: matchedDepartment.id
      }
    })
    ElMessage.success(`已带入推荐主科室：${matchedDepartment.departmentName}`)
    return
  }

  ElMessage.warning('暂时没有在系统科室中匹配到该推荐，请手动选择')
}

onMounted(async () => {
  resetTriageDialogState()
  try {
    await loadAiStatus()
    await loadDepartments()
    await loadTriageSessions()
  } catch (error) {
    console.error('加载 AI 分诊科室数据失败', error)
  }
})
</script>

<style scoped>
.triage-module {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.triage-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.8fr);
  gap: 16px;
  padding: 24px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgb(var(--primary-100-rgb) / 0.75) 100%);
  border: 1px solid rgb(var(--primary-300-rgb) / 0.35);
  box-shadow: var(--shadow-lg);
}

.triage-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgb(var(--primary-600-rgb) / 0.12);
  color: var(--primary-700);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.triage-hero h2 {
  margin: 14px 0 10px;
  font-size: 30px;
  line-height: 1.25;
  color: var(--primary-800);
}

.triage-hero p {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: var(--neutral-700);
}

.triage-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(300px, 0.9fr);
  gap: 18px;
}

.triage-chat-panel,
.triage-result-panel {
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.96) 0%, var(--neutral-50) 100%);
  border: 1px solid rgb(var(--primary-200-rgb) / 0.35);
  box-shadow: var(--shadow-md);
}

.triage-section-title {
  margin-bottom: 14px;
  font-size: 18px;
  font-weight: 700;
  color: var(--primary-800);
}

.triage-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.triage-panel-header .triage-section-title {
  margin-bottom: 4px;
}

.triage-session-meta {
  font-size: 12px;
  color: var(--neutral-500);
}

.triage-session-meta span {
  margin-left: 6px;
  color: var(--neutral-600);
}

.triage-chat-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.triage-chat-messages {
  max-height: 420px;
  overflow-y: auto;
  padding: 12px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92) 0%, var(--neutral-50) 100%);
  border: 1px solid rgb(var(--primary-500-rgb) / 0.12);
}

.triage-message {
  margin-bottom: 10px;
  padding: 12px;
  border-radius: 12px;
}

.triage-message.assistant {
  background: rgb(var(--primary-500-rgb) / 0.08);
}

.triage-message.user {
  background: rgb(var(--primary-500-rgb) / 0.16);
}

.triage-message-role {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary-700);
  margin-bottom: 6px;
}

.triage-message-content {
  font-size: 14px;
  color: var(--neutral-700);
  line-height: 1.6;
  white-space: pre-wrap;
}

.triage-chat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.triage-result-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.triage-result-card {
  padding: 16px;
  border-radius: 14px;
  background: rgb(var(--success-500-rgb) / 0.06);
  border: 1px solid rgb(var(--success-500-rgb) / 0.14);
}

.follow-up-card {
  background: rgb(var(--primary-500-rgb) / 0.05);
  border-color: rgb(var(--primary-500-rgb) / 0.14);
}

.triage-result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.triage-result-label {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary-800);
}

.triage-result-source {
  font-size: 12px;
  color: var(--neutral-600);
}

.triage-result-names {
  font-size: 20px;
  font-weight: 700;
  color: var(--success-600);
  line-height: 1.5;
}

.triage-result-reason {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--neutral-700);
}

.triage-actions {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.triage-guide-list {
  margin: 0;
  padding-left: 18px;
  color: var(--neutral-700);
  line-height: 1.8;
}

.ai-unavailable-pre {
  padding: 14px;
  border-radius: 12px;
  background: var(--neutral-900);
  color: var(--neutral-50);
  white-space: pre-wrap;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .triage-hero,
  .triage-workspace {
    grid-template-columns: 1fr;
  }

  .triage-hero h2 {
    font-size: 24px;
  }
}
</style>
