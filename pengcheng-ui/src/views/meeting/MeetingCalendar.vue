<template>
  <div class="meeting-page">
    <n-grid :cols="24" :x-gap="16" :y-gap="16">
      <n-gi :span="16">
        <n-card title="会议日历">
          <n-spin :show="loadingMonth">
            <n-calendar v-model:value="selectedDate" @update:value="handleDateChange">
              <template #header>
                <n-space>
                  <n-button size="small" @click="jumpToday">今天</n-button>
                  <n-button size="small" @click="jumpPrevMonth">上月</n-button>
                  <n-button size="small" @click="jumpNextMonth">下月</n-button>
                </n-space>
              </template>
              <template #default="{ year, month, date }">
                <div class="calendar-cell">
                  <div
                    v-for="event in getDayEvents(year, month, date)"
                    :key="event.id"
                    class="calendar-event"
                    :class="getTypeClass(event.type)"
                  >
                    {{ event.title }}
                  </div>
                </div>
              </template>
            </n-calendar>
          </n-spin>
        </n-card>

        <n-card title="当日会议" style="margin-top: 16px">
          <template #header-extra>
            <n-button type="primary" size="small" @click="openMeetingForm()">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              预约会议
            </n-button>
          </template>

          <n-spin :show="loadingDay">
            <n-timeline v-if="dayMeetings.length > 0">
              <n-timeline-item
                v-for="meeting in dayMeetings"
                :key="meeting.id"
                :type="meeting.status === 4 ? 'success' : meeting.status === 3 ? 'error' : 'default'"
                :title="meeting.title"
                :content="formatMeetingTime(meeting)"
              >
                <template #footer>
                  <n-space>
                    <n-tag size="small" :type="getTypeTag(meeting.type)">{{ getTypeText(meeting.type) }}</n-tag>
                    <span><n-icon><LocationOutline /></n-icon> {{ meeting.location || '待定' }}</span>
                    <n-button size="tiny" @click="openMeetingDetail(meeting)">详情</n-button>
                    <n-button v-if="canEdit(meeting)" size="tiny" @click="openMeetingForm(meeting)">编辑</n-button>
                    <n-button v-if="canEdit(meeting)" size="tiny" type="error" @click="cancelMeeting(meeting)">取消</n-button>
                  </n-space>
                </template>
              </n-timeline-item>
            </n-timeline>
            <n-empty v-else description="当日无会议" />
          </n-spin>
        </n-card>
      </n-gi>

      <n-gi :span="8">
        <n-card title="会议统计">
          <n-space vertical>
            <n-statistic label="本周会议" :value="weekStats.total" />
            <n-statistic label="待参加" :value="weekStats.pending" />
            <n-statistic label="已参加" :value="weekStats.attended" />
            <n-statistic label="已取消" :value="weekStats.cancelled" />
          </n-space>
        </n-card>

        <n-card title="我的待办" style="margin-top: 16px">
          <n-list v-if="meetingTasks.length > 0">
            <n-list-item v-for="task in meetingTasks" :key="task.id">
              <n-thing :title="task.title" :description="task.meetingTitle">
                <template #suffix>
                  <n-tag size="small" :type="task.tagType">{{ task.tagText }}</n-tag>
                </template>
              </n-thing>
            </n-list-item>
          </n-list>
          <n-empty v-else description="暂无待办" />
        </n-card>

        <n-card title="会议提醒设置" style="margin-top: 16px">
          <n-form label-placement="left" label-width="100">
            <n-form-item label="默认提醒">
              <n-select
                v-model:value="reminderConfig.defaultReminder"
                :options="reminderOptions"
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item label="站内信提醒">
              <n-switch v-model:value="reminderConfig.internalNotification" />
            </n-form-item>
            <n-form-item label="邮件提醒">
              <n-switch v-model:value="reminderConfig.email" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="savingReminder" @click="saveReminderConfig">保存设置</n-button>
            </n-form-item>
          </n-form>
        </n-card>
      </n-gi>
    </n-grid>

    <n-modal v-model:show="showMeetingDetail" preset="card" title="会议详情" style="width: 600px">
      <n-spin :show="loadingDetail">
        <template v-if="currentMeeting">
          <n-descriptions :column="2" bordered>
            <n-descriptions-item label="会议主题">{{ currentMeeting.title }}</n-descriptions-item>
            <n-descriptions-item label="会议类型">{{ getTypeText(currentMeeting.type) }}</n-descriptions-item>
            <n-descriptions-item label="会议时间" :span="2">{{ formatMeetingTime(currentMeeting) }}</n-descriptions-item>
            <n-descriptions-item label="会议地点" :span="2">
              <n-icon><LocationOutline /></n-icon> {{ currentMeeting.location || '待定' }}
            </n-descriptions-item>
            <n-descriptions-item v-if="currentMeeting.meetingUrl" label="会议链接" :span="2">
              <n-a :href="currentMeeting.meetingUrl" target="_blank">{{ currentMeeting.meetingUrl }}</n-a>
            </n-descriptions-item>
            <n-descriptions-item label="组织者">{{ currentMeeting.organizerName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="状态">
              <n-tag :type="getStatusType(currentMeeting.status)">{{ getStatusText(currentMeeting.status) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="参会人员" :span="2">
              <n-space v-if="currentMeeting.participants.length > 0">
                <n-avatar
                  v-for="participant in currentMeeting.participants"
                  :key="participant.id"
                  :src="participant.avatar"
                  :size="32"
                >
                  {{ participant.name }}
                </n-avatar>
              </n-space>
              <span v-else>-</span>
            </n-descriptions-item>
          </n-descriptions>

          <n-tabs type="line" style="margin-top: 16px">
            <n-tab-pane name="minutes" tab="会议纪要">
              <n-input
                v-model:value="meetingMinutes.content"
                type="textarea"
                placeholder="请输入会议纪要..."
                :rows="6"
                :disabled="!canEditMinutes"
              />
              <n-space style="margin-top: 8px" v-if="canEditMinutes">
                <n-button size="small" @click="saveMinutes">保存纪要</n-button>
              </n-space>
            </n-tab-pane>
            <n-tab-pane name="files" tab="会议文件">
              <n-upload :action="uploadUrl" :headers="uploadHeaders" multiple @finish="handleFileUpload">
                <n-button size="small">上传文件</n-button>
              </n-upload>
              <n-list style="margin-top: 12px">
                <n-list-item v-for="file in meetingFiles" :key="file.id">
                  <n-space justify="space-between" style="width: 100%">
                    <n-space>
                      <n-icon :component="getFileIcon(file.type)" />
                      <span>{{ file.name }}</span>
                    </n-space>
                    <n-button size="tiny" @click="downloadFile(file)">下载</n-button>
                  </n-space>
                </n-list-item>
              </n-list>
              <n-empty v-if="meetingFiles.length === 0" description="暂无会议文件" style="margin-top: 12px" />
            </n-tab-pane>
          </n-tabs>
        </template>
      </n-spin>
    </n-modal>

    <n-modal v-model:show="showMeetingForm" preset="card" :title="formTitle" style="width: 600px">
      <n-form :model="meetingForm" label-placement="left" label-width="100">
        <n-form-item label="会议主题" required>
          <n-input v-model:value="meetingForm.title" placeholder="请输入会议主题" />
        </n-form-item>
        <n-form-item label="会议类型">
          <n-select v-model:value="meetingForm.type" :options="typeOptions" />
        </n-form-item>
        <n-form-item label="会议时间" required>
          <n-date-picker v-model:value="meetingForm.timeRange" type="datetimerange" style="width: 100%" />
        </n-form-item>
        <n-form-item label="会议地点">
          <n-input v-model:value="meetingForm.location" placeholder="会议室名称或地址" />
        </n-form-item>
        <n-form-item label="视频链接">
          <n-input v-model:value="meetingForm.meetingUrl" placeholder="视频会议链接" />
        </n-form-item>
        <n-form-item label="提前提醒">
          <n-select v-model:value="meetingForm.reminderMinutes" :options="reminderOptions" />
        </n-form-item>
        <n-form-item label="参会人员">
          <n-select
            v-model:value="meetingForm.participantIds"
            :options="userOptions"
            label-field="nickname"
            value-field="id"
            multiple
            filterable
            style="width: 100%"
          />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showMeetingForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitMeeting">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import type { UploadFileInfo } from 'naive-ui'
import { useMessage } from 'naive-ui'
import { AddOutline, DocumentOutline, ImageOutline, LocationOutline, VideocamOutline } from '@vicons/ionicons5'
import { userApi, type SysUser } from '@/api/system'
import {
  meetingApi,
  type MeetingCalendarItem,
  type MeetingFile,
  type MeetingMinutes,
  type MeetingReminderConfig,
  type MeetingSavePayload
} from '@/api/meeting'
import { useUserStore } from '@/stores/user'

type TagType = 'default' | 'info' | 'success' | 'warning' | 'error'

interface MeetingTask {
  id: string
  title: string
  meetingTitle: string
  tagType: TagType
  tagText: string
}

const message = useMessage()
const userStore = useUserStore()

const selectedDate = ref(Date.now())
const monthMeetings = ref<MeetingCalendarItem[]>([])
const dayMeetings = ref<MeetingCalendarItem[]>([])

const reminderConfig = reactive<MeetingReminderConfig>({
  defaultReminder: 15,
  internalNotification: true,
  email: false
})

const loadingMonth = ref(false)
const loadingDay = ref(false)
const loadingDetail = ref(false)
const savingReminder = ref(false)
const submitting = ref(false)

const showMeetingDetail = ref(false)
const showMeetingForm = ref(false)
const currentMeeting = ref<MeetingCalendarItem | null>(null)

const meetingForm = reactive({
  id: null as number | null,
  title: '',
  type: 1,
  timeRange: null as [number, number] | null,
  location: '',
  meetingUrl: '',
  reminderMinutes: 15,
  participantIds: [] as number[]
})

const meetingMinutes = reactive<MeetingMinutes>({
  content: '',
  conclusions: '',
  actionItems: '',
  status: 2
})
const meetingFiles = ref<MeetingFile[]>([])
const userOptions = ref<SysUser[]>([])

const typeOptions = [
  { label: '普通会议', value: 1 },
  { label: '视频会议', value: 2 },
  { label: '电话会议', value: 3 }
]

const reminderOptions = [
  { label: '不提醒', value: 0 },
  { label: '5 分钟前', value: 5 },
  { label: '10 分钟前', value: 10 },
  { label: '15 分钟前', value: 15 },
  { label: '30 分钟前', value: 30 },
  { label: '1 小时前', value: 60 },
  { label: '1 天前', value: 1440 }
]

const formTitle = computed(() => meetingForm.id ? '编辑会议' : '预约会议')
const canEditMinutes = computed(() => currentMeeting.value ? canEdit(currentMeeting.value) : false)
const uploadUrl = '/api/sys/file/upload'
const uploadHeaders = computed(() => userStore.token ? { Authorization: userStore.token } : {})

const weekStats = computed(() => {
  const base = new Date(selectedDate.value)
  const start = startOfWeek(base)
  const end = new Date(start)
  end.setDate(end.getDate() + 7)
  const weeklyMeetings = monthMeetings.value.filter((meeting) => {
    const timestamp = new Date(meeting.startTime).getTime()
    return timestamp >= start.getTime() && timestamp < end.getTime()
  })

  return {
    total: weeklyMeetings.length,
    pending: weeklyMeetings.filter(item => item.status === 1 || item.status === 2).length,
    attended: weeklyMeetings.filter(item => item.status === 4).length,
    cancelled: weeklyMeetings.filter(item => item.status === 3).length
  }
})

const meetingTasks = computed<MeetingTask[]>(() => {
  const tasks: MeetingTask[] = []
  dayMeetings.value.forEach((meeting) => {
    if (meeting.status === 3) {
      return
    }
    if (canEdit(meeting) && meeting.status === 1) {
      tasks.push({
        id: `prepare-${meeting.id}`,
        title: '确认会议安排',
        meetingTitle: meeting.title,
        tagType: 'info',
        tagText: '待准备'
      })
      return
    }
    if (canEdit(meeting) && meeting.status === 4) {
      tasks.push({
        id: `minutes-${meeting.id}`,
        title: '补充会议纪要',
        meetingTitle: meeting.title,
        tagType: 'warning',
        tagText: '待纪要'
      })
      return
    }
    tasks.push({
      id: `join-${meeting.id}`,
      title: '按时参会',
      meetingTitle: meeting.title,
      tagType: 'success',
      tagText: '待参加'
    })
  })
  return tasks
})

function toDateString(value: number) {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toDateTimeString(value: number) {
  const date = new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

function startOfWeek(source: Date) {
  const date = new Date(source)
  date.setHours(0, 0, 0, 0)
  const day = date.getDay() || 7
  date.setDate(date.getDate() - day + 1)
  return date
}

function sameDay(dateString: string, year: number, month: number, day: number) {
  const current = new Date(dateString)
  return current.getFullYear() === year && current.getMonth() + 1 === month && current.getDate() === day
}

function getDayEvents(year: number, month: number, date: number) {
  return monthMeetings.value.filter(meeting => sameDay(meeting.startTime, year, month, date))
}

async function loadMonthMeetings() {
  const current = new Date(selectedDate.value)
  loadingMonth.value = true
  try {
    monthMeetings.value = await meetingApi.getMonthMeetings(current.getFullYear(), current.getMonth() + 1)
  } finally {
    loadingMonth.value = false
  }
}

async function loadDayMeetings() {
  loadingDay.value = true
  try {
    dayMeetings.value = await meetingApi.getDayMeetings(toDateString(selectedDate.value))
  } finally {
    loadingDay.value = false
  }
}

async function reloadMeetings() {
  await Promise.all([loadMonthMeetings(), loadDayMeetings()])
}

function handleDateChange(value: number) {
  selectedDate.value = value
  void reloadMeetings()
}

function jumpToday() {
  selectedDate.value = Date.now()
  void reloadMeetings()
}

function jumpPrevMonth() {
  const current = new Date(selectedDate.value)
  current.setMonth(current.getMonth() - 1)
  selectedDate.value = current.getTime()
  void reloadMeetings()
}

function jumpNextMonth() {
  const current = new Date(selectedDate.value)
  current.setMonth(current.getMonth() + 1)
  selectedDate.value = current.getTime()
  void reloadMeetings()
}

function formatMeetingTime(meeting: Pick<MeetingCalendarItem, 'startTime' | 'endTime'>) {
  const start = new Date(meeting.startTime)
  const end = new Date(meeting.endTime)
  return `${start.toLocaleDateString()} ${start.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${end.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
}

function getTypeText(type: number) {
  const map: Record<number, string> = { 1: '普通会议', 2: '视频会议', 3: '电话会议' }
  return map[type] || '未知'
}

function getTypeTag(type: number): TagType {
  const map: Record<number, TagType> = { 1: 'default', 2: 'info', 3: 'warning' }
  return map[type] || 'default'
}

function getTypeClass(type: number) {
  const map: Record<number, string> = { 1: 'normal', 2: 'video', 3: 'phone' }
  return map[type] || 'normal'
}

function getStatusText(status: number) {
  const map: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已取消', 4: '已结束' }
  return map[status] || '未知'
}

function getStatusType(status: number): TagType {
  const map: Record<number, TagType> = { 1: 'default', 2: 'info', 3: 'error', 4: 'success' }
  return map[status] || 'default'
}

function canEdit(meeting: Pick<MeetingCalendarItem, 'organizerId'>) {
  return !!userStore.user?.id && meeting.organizerId === userStore.user.id
}

function syncMeetingDetail(detail: MeetingCalendarItem) {
  currentMeeting.value = detail
  meetingMinutes.content = detail.minutes?.content || ''
  meetingMinutes.conclusions = detail.minutes?.conclusions || ''
  meetingMinutes.actionItems = detail.minutes?.actionItems || ''
  meetingMinutes.status = detail.minutes?.status || 2
  meetingFiles.value = detail.files || []
}

async function openMeetingDetail(meeting: MeetingCalendarItem) {
  showMeetingDetail.value = true
  loadingDetail.value = true
  try {
    const detail = await meetingApi.getMeetingDetail(meeting.id)
    syncMeetingDetail(detail)
  } finally {
    loadingDetail.value = false
  }
}

function resetMeetingForm() {
  meetingForm.id = null
  meetingForm.title = ''
  meetingForm.type = 1
  meetingForm.timeRange = null
  meetingForm.location = ''
  meetingForm.meetingUrl = ''
  meetingForm.reminderMinutes = reminderConfig.defaultReminder
  meetingForm.participantIds = []
}

function openMeetingForm(meeting?: MeetingCalendarItem) {
  if (!meeting) {
    resetMeetingForm()
    showMeetingForm.value = true
    return
  }

  meetingForm.id = meeting.id
  meetingForm.title = meeting.title
  meetingForm.type = meeting.type
  meetingForm.timeRange = [new Date(meeting.startTime).getTime(), new Date(meeting.endTime).getTime()]
  meetingForm.location = meeting.location || ''
  meetingForm.meetingUrl = meeting.meetingUrl || ''
  meetingForm.reminderMinutes = meeting.reminderMinutes ?? reminderConfig.defaultReminder
  meetingForm.participantIds = [...(meeting.participantIds || [])]
  showMeetingForm.value = true
}

function buildMeetingPayload(): MeetingSavePayload {
  if (!meetingForm.timeRange) {
    throw new Error('会议时间不能为空')
  }

  return {
    title: meetingForm.title.trim(),
    type: meetingForm.type,
    startTime: toDateTimeString(meetingForm.timeRange[0]),
    endTime: toDateTimeString(meetingForm.timeRange[1]),
    location: meetingForm.location.trim() || undefined,
    meetingUrl: meetingForm.meetingUrl.trim() || undefined,
    reminderMinutes: meetingForm.reminderMinutes,
    participantIds: meetingForm.participantIds
  }
}

async function submitMeeting() {
  if (!meetingForm.title.trim() || !meetingForm.timeRange) {
    message.warning('请填写必填项')
    return
  }

  submitting.value = true
  try {
    const payload = buildMeetingPayload()
    if (meetingForm.id) {
      await meetingApi.updateMeeting(meetingForm.id, payload)
      message.success('会议已更新')
    } else {
      await meetingApi.createMeeting(payload)
      message.success('会议已创建')
    }
    showMeetingForm.value = false
    await reloadMeetings()
    if (currentMeeting.value?.id === meetingForm.id && meetingForm.id) {
      const detail = await meetingApi.getMeetingDetail(meetingForm.id)
      syncMeetingDetail(detail)
    }
  } finally {
    submitting.value = false
  }
}

async function cancelMeeting(meeting: MeetingCalendarItem) {
  if (!window.confirm(`确定取消会议“${meeting.title}”吗？`)) {
    return
  }
  await meetingApi.cancelMeeting(meeting.id)
  message.success('会议已取消')
  await reloadMeetings()
  if (currentMeeting.value?.id === meeting.id) {
    const detail = await meetingApi.getMeetingDetail(meeting.id)
    syncMeetingDetail(detail)
  }
}

async function saveMinutes() {
  if (!currentMeeting.value) {
    return
  }
  const saved = await meetingApi.saveMinutes(currentMeeting.value.id, {
    content: meetingMinutes.content,
    conclusions: meetingMinutes.conclusions,
    actionItems: meetingMinutes.actionItems,
    status: 2
  })
  currentMeeting.value.minutes = saved
  message.success('纪要已保存')
}

function handleFileUpload(options: { file: UploadFileInfo }) {
  const fileName = options.file?.name || '文件'
  message.info(`${fileName} 上传完成，会议文件关联待接入文件服务`)
}

function downloadFile(file: MeetingFile) {
  if (!file.url) {
    message.warning('当前文件暂无可用下载地址')
    return
  }
  window.open(file.url, '_blank')
}

function getFileIcon(type?: string) {
  if (type?.startsWith('image/')) return ImageOutline
  if (type?.startsWith('video/')) return VideocamOutline
  return DocumentOutline
}

async function loadUserOptions() {
  const result = await userApi.page({ page: 1, pageSize: 100, status: 1 })
  userOptions.value = result?.list || []
}

async function loadReminderConfig() {
  const config = await meetingApi.getReminderConfig()
  reminderConfig.defaultReminder = config.defaultReminder ?? 15
  reminderConfig.internalNotification = config.internalNotification ?? true
  reminderConfig.email = config.email ?? false
}

async function saveReminderConfig() {
  savingReminder.value = true
  try {
    await meetingApi.saveReminderConfig({
      defaultReminder: reminderConfig.defaultReminder,
      internalNotification: reminderConfig.internalNotification,
      email: reminderConfig.email
    })
    message.success('设置已保存')
    if (!meetingForm.id) {
      meetingForm.reminderMinutes = reminderConfig.defaultReminder
    }
  } finally {
    savingReminder.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadReminderConfig(), loadUserOptions()])
  resetMeetingForm()
  await reloadMeetings()
})
</script>

<style scoped>
.meeting-page { padding: 20px; }
.calendar-cell { min-height: 60px; }
.calendar-event {
  font-size: 12px;
  padding: 2px 6px;
  margin: 2px 0;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.calendar-event.normal {
  background: rgba(24, 160, 88, 0.2);
  color: #18a058;
}
.calendar-event.video {
  background: rgba(32, 128, 240, 0.18);
  color: #2080f0;
}
.calendar-event.phone {
  background: rgba(240, 160, 32, 0.18);
  color: #f0a020;
}
</style>
