<template>
  <div class="page-container">
    <div class="meeting-layout">
      <!-- 左侧日历 -->
      <div class="calendar-card">
        <n-spin :show="loading">
          <n-calendar
            v-model:value="value"
            #="{ year, month, date }"
            @update:value="handleDateChange"
          >
            <div v-for="meeting in getMeetingsByDate(year, month, date)" :key="meeting.id" class="calendar-event">
              {{ meeting.title }}
            </div>
          </n-calendar>
        </n-spin>
      </div>

      <!-- 右侧会议列表 -->
      <div class="meeting-list-card">
        <div class="list-header">
          <div class="header-title">
            <span class="date">{{ formatDate(value) }}</span>
            <span class="count">{{ currentMeetings.length }} 个会议</span>
          </div>
          <div class="header-actions">
            <n-button type="primary" :loading="loading" @click="showCreateModal = true">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              预约会议
            </n-button>
          </div>
        </div>

        <div class="meeting-list">
          <div v-if="currentMeetings.length > 0" class="meeting-items">
            <div v-for="meeting in currentMeetings" :key="meeting.id" class="meeting-item">
              <div class="meeting-time">
                <span class="start-time">{{ formatTime(meeting.startTime) }}</span>
                <span class="end-time">{{ formatTime(meeting.endTime) }}</span>
              </div>
              <div class="meeting-info">
                <div class="meeting-title">{{ meeting.title }}</div>
                <div class="meeting-location">
                  <n-icon><LocationOutline /></n-icon>
                  {{ meeting.location }}
                </div>
                <div class="meeting-participants">
                  <n-avatar-group :options="meeting.participants" :size="24" :max="3" />
                </div>
              </div>
              <div class="meeting-status">
                <n-tag :type="getStatusType(meeting.status)" size="small">
                  {{ getStatusText(meeting.status) }}
                </n-tag>
              </div>
            </div>
          </div>
          <n-empty v-else description="当日无会议" class="empty-state">
            <template #extra>
              <n-button size="small" @click="showCreateModal = true">预约一个？</n-button>
            </template>
          </n-empty>
        </div>
      </div>
    </div>

    <!-- 预约会议弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="预约会议" style="width: 600px" @after-leave="resetForm">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="会议主题" required>
          <n-input v-model:value="newMeeting.title" placeholder="请输入会议主题" />
        </n-form-item>
        <n-form-item label="会议时间" required>
          <n-date-picker
            v-model:value="newMeeting.timeRange"
            type="datetimerange"
            clearable
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="会议地点">
          <n-input v-model:value="newMeeting.location" placeholder="请输入会议地点或链接" />
        </n-form-item>
        <n-form-item label="参会人员">
          <n-select multiple placeholder="请选择参会人员（可选）" :options="userOptions" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="newMeeting.remark" type="textarea" placeholder="请输入备注" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleCreateMeeting">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { AddOutline, LocationOutline } from '@vicons/ionicons5'
import { useMessage } from 'naive-ui'
import { calendarApi } from '@/api/calendar'

const message = useMessage()
const value = ref(Date.now())
const showCreateModal = ref(false)
const loading = ref(false)
const submitting = ref(false)

/** 后端日历事件（仅 eventType=meeting）映射为会议展示项 */
interface MeetingItem {
  id: number
  title: string
  startTime: number
  endTime: number
  location: string
  status: number // 0-未开始 1-进行中 2-已结束
  participants: Array<{ name?: string; src?: string }>
}

const meetings = ref<MeetingItem[]>([])
const lastLoadedKey = ref<string>('')

const newMeeting = ref({
  title: '',
  timeRange: null as [number, number] | null,
  location: '',
  participants: [] as number[],
  remark: ''
})

const userOptions = [
  { label: '张三', value: 1 },
  { label: '李四', value: 2 },
  { label: '王五', value: 3 }
]

/** 根据开始/结束时间计算进行状态 */
function computeStatus(startTime: number, endTime: number): number {
  const now = Date.now()
  if (now < startTime) return 0
  if (endTime && now > endTime) return 2
  return 1
}

/** 将后端事件转为会议展示项 */
function toMeetingItem(e: any): MeetingItem {
  const start = e.startTime ? new Date(e.startTime).getTime() : 0
  const end = e.endTime ? new Date(e.endTime).getTime() : start
  return {
    id: e.id,
    title: e.title || '未命名会议',
    startTime: start,
    endTime: end,
    location: e.location || '线上会议',
    status: computeStatus(start, end),
    participants: []
  }
}

/** 加载当前选中日期所在月份的会议（仅 meeting 类型） */
async function loadMeetingsForCurrentMonth() {
  const d = new Date(value.value)
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  const key = `${year}-${month}`
  if (lastLoadedKey.value === key) return
  loading.value = true
  try {
    const list = await calendarApi.getMonthEvents(year, month)
    const raw = Array.isArray(list) ? list : []
    meetings.value = raw
      .filter((e: any) => e.eventType === 'meeting' && e.status !== 0)
      .map(toMeetingItem)
    lastLoadedKey.value = key
  } catch {
    message.error('加载会议列表失败')
    meetings.value = []
  } finally {
    loading.value = false
  }
}

/** 日历选中日期变化：若月份变化则重新拉取 */
function handleDateChange(ts: number) {
  value.value = ts
  const d = new Date(ts)
  const key = `${d.getFullYear()}-${d.getMonth() + 1}`
  if (lastLoadedKey.value !== key) loadMeetingsForCurrentMonth()
}

// 当日会议（右侧列表）
const currentMeetings = computed(() => {
  const d = new Date(value.value)
  return meetings.value.filter(m => {
    const start = new Date(m.startTime)
    return start.getFullYear() === d.getFullYear() &&
      start.getMonth() === d.getMonth() &&
      start.getDate() === d.getDate()
  })
})

function getMeetingsByDate(year: number, month: number, date: number) {
  return meetings.value.filter(m => {
    const start = new Date(m.startTime)
    return start.getFullYear() === year &&
      start.getMonth() + 1 === month &&
      start.getDate() === date
  })
}

function formatDate(ts: number) {
  const d = new Date(ts)
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatTime(ts: number) {
  const d = new Date(ts)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function getStatusType(status: number) {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'default'
    default: return 'default'
  }
}

function getStatusText(status: number) {
  switch (status) {
    case 0: return '未开始'
    case 1: return '进行中'
    case 2: return '已结束'
    default: return '未知'
  }
}

function resetForm() {
  newMeeting.value = {
    title: '',
    timeRange: null,
    location: '',
    participants: [],
    remark: ''
  }
}

async function handleCreateMeeting() {
  if (!newMeeting.value.title || !newMeeting.value.timeRange) {
    message.warning('请填写会议主题和会议时间')
    return
  }
  const [start, end] = newMeeting.value.timeRange
  submitting.value = true
  try {
    await calendarApi.createEvent({
      title: newMeeting.value.title,
      eventType: 'meeting',
      startTime: new Date(start).toISOString(),
      endTime: new Date(end).toISOString(),
      location: newMeeting.value.location || '线上会议'
    })
    message.success('预约成功')
    showCreateModal.value = false
    lastLoadedKey.value = ''
    await loadMeetingsForCurrentMonth()
  } catch {
    message.error('预约失败，请重试')
  } finally {
    submitting.value = false
  }
}

onMounted(loadMeetingsForCurrentMonth)
</script>

<style scoped>
.page-container {
  height: calc(100vh - 92px);
  background: #f5f7fa;
  padding: 16px;
}

.meeting-layout {
  display: flex;
  height: 100%;
  gap: 16px;
}

.calendar-card {
  flex: 2;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.calendar-event {
  font-size: 12px;
  color: #18a058;
  background: #e8f5e9;
  border-radius: 2px;
  padding: 1px 4px;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meeting-list-card {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  min-width: 350px;
}

.list-header {
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  display: flex;
  flex-direction: column;
}

.date {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.count {
  font-size: 12px;
  color: #999;
}

.meeting-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.meeting-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.meeting-item {
  display: flex;
  align-items: flex-start;
  padding: 12px;
  border-radius: 8px;
  background: #f9f9f9;
  gap: 12px;
  transition: all 0.2s;
}

.meeting-item:hover {
  background: #f0f0f0;
}

.meeting-time {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 48px;
  font-weight: 500;
  color: #333;
}

.end-time {
  font-size: 12px;
  color: #999;
}

.meeting-info {
  flex: 1;
}

.meeting-title {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}

.meeting-location {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.meeting-participants {
  display: flex;
}

.meeting-status {
  flex-shrink: 0;
}

.empty-state {
  margin-top: 60px;
}
</style>
