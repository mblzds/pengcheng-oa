<template>
  <div class="calendar-page">
    <div class="page-header">
      <h2>销售日历</h2>
      <div class="header-actions">
        <n-button-group size="small">
          <n-button :type="viewMode === 'month' ? 'primary' : 'default'" @click="viewMode = 'month'">月</n-button>
          <n-button :type="viewMode === 'week' ? 'primary' : 'default'" @click="viewMode = 'week'">周</n-button>
          <n-button :type="viewMode === 'day' ? 'primary' : 'default'" @click="viewMode = 'day'">日</n-button>
        </n-button-group>
        <n-button type="primary" @click="showCreateEvent = true">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新建日程
        </n-button>
      </div>
    </div>

    <!-- 导航栏 -->
    <div class="calendar-nav">
      <n-button text @click="navigatePrev">
        <n-icon size="20"><ChevronBackOutline /></n-icon>
      </n-button>
      <h3 class="nav-title">{{ navTitle }}</h3>
      <n-button text @click="navigateNext">
        <n-icon size="20"><ChevronForwardOutline /></n-icon>
      </n-button>
      <n-button size="small" @click="goToday" style="margin-left: 12px">今天</n-button>
    </div>

    <!-- 月视图 -->
    <div v-if="viewMode === 'month'" class="month-view">
      <div class="weekday-header">
        <div v-for="day in weekDays" :key="day" class="weekday-cell">{{ day }}</div>
      </div>
      <div class="month-grid">
        <div
          v-for="(cell, idx) in monthCells"
          :key="idx"
          class="day-cell"
          :class="{ 'other-month': !cell.currentMonth, today: cell.isToday, selected: cell.isSelected }"
          @click="selectDate(cell.date)"
        >
          <div class="day-number">{{ cell.date.getDate() }}</div>
          <div class="day-events">
            <div
              v-for="event in cell.events.slice(0, 3)"
              :key="event.id"
              class="event-dot"
              :style="{ background: event.color || '#18a058' }"
              :title="event.title"
              @click.stop="editEvent(event)"
            >
              {{ event.title }}
            </div>
            <div v-if="cell.events.length > 3" class="more-events">
              +{{ cell.events.length - 3 }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 周视图 -->
    <div v-else-if="viewMode === 'week'" class="week-view">
      <div class="time-grid">
        <div class="time-column">
          <div v-for="h in hours" :key="h" class="time-label">{{ h }}:00</div>
        </div>
        <div v-for="(day, idx) in weekDates" :key="idx" class="day-column">
          <div class="day-col-header" :class="{ today: isToday(day) }">
            <span class="day-name">{{ weekDays[idx] }}</span>
            <span class="day-num">{{ day.getDate() }}</span>
          </div>
          <div class="day-slots">
            <div v-for="h in hours" :key="h" class="time-slot" @click="quickCreate(day, h)"></div>
            <div
              v-for="event in getEventsForDate(day)"
              :key="event.id"
              class="week-event"
              :style="getEventStyle(event)"
              @click="editEvent(event)"
            >
              <span class="week-event-title">{{ event.title }}</span>
              <span class="week-event-time">{{ formatEventTime(event) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 日视图 -->
    <div v-else class="day-view">
      <div class="day-timeline">
        <div v-for="h in hours" :key="h" class="timeline-row">
          <div class="timeline-time">{{ h }}:00</div>
          <div class="timeline-content" @click="quickCreate(selectedDate, h)">
            <div
              v-for="event in getEventsForHour(h)"
              :key="event.id"
              class="timeline-event"
              :style="{ borderLeft: '3px solid ' + (event.color || '#18a058') }"
              @click.stop="editEvent(event)"
            >
              <div class="tl-event-title">{{ event.title }}</div>
              <div class="tl-event-meta">
                {{ formatEventTime(event) }}
                <span v-if="event.location"> · {{ event.location }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑事件弹窗 -->
    <n-modal v-model:show="showCreateEvent" preset="card" :title="editingEvent ? '编辑日程' : '新建日程'" style="width: 500px">
      <n-form :model="eventForm" label-placement="left" label-width="80">
        <n-form-item label="标题" required>
          <n-input v-model:value="eventForm.title" placeholder="日程标题" />
        </n-form-item>
        <n-form-item label="类型">
          <n-select v-model:value="eventForm.eventType" :options="eventTypeOptions" />
        </n-form-item>
        <n-form-item label="开始时间">
          <n-date-picker v-model:value="eventForm.startTime" type="datetime" style="width: 100%" />
        </n-form-item>
        <n-form-item label="结束时间">
          <n-date-picker v-model:value="eventForm.endTime" type="datetime" style="width: 100%" />
        </n-form-item>
        <n-form-item label="地点">
          <n-input v-model:value="eventForm.location" placeholder="可选" />
        </n-form-item>
        <n-form-item label="颜色">
          <div class="color-picker">
            <div
              v-for="c in colorOptions"
              :key="c"
              class="color-dot"
              :style="{ background: c }"
              :class="{ selected: eventForm.color === c }"
              @click="eventForm.color = c"
            />
          </div>
        </n-form-item>
        <n-form-item label="提醒">
          <n-select v-model:value="eventForm.reminderMinutes" :options="reminderOptions" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button v-if="editingEvent" type="error" @click="deleteEvent">删除</n-button>
          <n-button @click="showCreateEvent = false">取消</n-button>
          <n-button type="primary" @click="submitEvent" :disabled="!eventForm.title">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { NButton, NButtonGroup, NIcon, NModal, NForm, NFormItem, NInput, NSelect, NDatePicker, NSpace, useMessage } from 'naive-ui'
import { AddOutline, ChevronBackOutline, ChevronForwardOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

const message = useMessage()
const viewMode = ref<'month' | 'week' | 'day'>('month')
const currentDate = ref(new Date())
const selectedDate = ref(new Date())
const events = ref<any[]>([])
const showCreateEvent = ref(false)
const editingEvent = ref<any>(null)

const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const hours = Array.from({ length: 14 }, (_, i) => i + 7)
const colorOptions = ['#18a058', '#2196f3', '#ff9800', '#d03050', '#9c27b0', '#607d8b', '#795548']

const eventTypeOptions = [
  { label: '客户拜访', value: 'visit' },
  { label: '签约', value: 'sign' },
  { label: '付款', value: 'payment' },
  { label: '会议', value: 'meeting' },
  { label: '提醒', value: 'reminder' },
  { label: '其他', value: 'custom' }
]

const reminderOptions = [
  { label: '不提醒', value: 0 },
  { label: '15分钟前', value: 15 },
  { label: '30分钟前', value: 30 },
  { label: '1小时前', value: 60 },
  { label: '1天前', value: 1440 }
]

const eventForm = reactive({
  title: '',
  eventType: 'custom',
  startTime: null as number | null,
  endTime: null as number | null,
  location: '',
  color: '#18a058',
  reminderMinutes: 30
})

const navTitle = computed(() => {
  const d = currentDate.value
  if (viewMode.value === 'month') return `${d.getFullYear()}年${d.getMonth() + 1}月`
  if (viewMode.value === 'week') {
    const start = getWeekStart(d)
    const end = new Date(start)
    end.setDate(end.getDate() + 6)
    return `${start.getMonth() + 1}月${start.getDate()}日 - ${end.getMonth() + 1}月${end.getDate()}日`
  }
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
})

const monthCells = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()
  const firstDay = new Date(year, month, 1)
  const startDay = (firstDay.getDay() + 6) % 7
  const cells = []
  const today = new Date()

  for (let i = -startDay; i < 42 - startDay; i++) {
    const d = new Date(year, month, 1 + i)
    const isCurrentMonth = d.getMonth() === month
    const dayEvents = getEventsForDate(d)
    cells.push({
      date: d,
      currentMonth: isCurrentMonth,
      isToday: d.toDateString() === today.toDateString(),
      isSelected: d.toDateString() === selectedDate.value.toDateString(),
      events: dayEvents
    })
  }
  return cells
})

const weekDates = computed(() => {
  const start = getWeekStart(currentDate.value)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    return d
  })
})

function getWeekStart(d: Date) {
  const day = (d.getDay() + 6) % 7
  const start = new Date(d)
  start.setDate(start.getDate() - day)
  return start
}

function isToday(d: Date) { return d.toDateString() === new Date().toDateString() }

function getEventsForDate(date: Date) {
  const ds = date.toDateString()
  return events.value.filter(e => new Date(e.startTime).toDateString() === ds)
}

function getEventsForHour(hour: number) {
  const ds = selectedDate.value.toDateString()
  return events.value.filter(e => {
    const d = new Date(e.startTime)
    return d.toDateString() === ds && d.getHours() === hour
  })
}

function getEventStyle(event: any) {
  const start = new Date(event.startTime)
  const end = event.endTime ? new Date(event.endTime) : new Date(start.getTime() + 3600000)
  const top = (start.getHours() - 7) * 50 + (start.getMinutes() / 60) * 50
  const height = Math.max(((end.getTime() - start.getTime()) / 3600000) * 50, 24)
  return { top: top + 'px', height: height + 'px', background: event.color || '#18a058' }
}

function formatEventTime(event: any) {
  const s = new Date(event.startTime)
  const e = event.endTime ? new Date(event.endTime) : null
  const st = `${String(s.getHours()).padStart(2, '0')}:${String(s.getMinutes()).padStart(2, '0')}`
  if (!e) return st
  return `${st}-${String(e.getHours()).padStart(2, '0')}:${String(e.getMinutes()).padStart(2, '0')}`
}

function navigatePrev() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'month') d.setMonth(d.getMonth() - 1)
  else if (viewMode.value === 'week') d.setDate(d.getDate() - 7)
  else d.setDate(d.getDate() - 1)
  currentDate.value = d
  loadEvents()
}

function navigateNext() {
  const d = new Date(currentDate.value)
  if (viewMode.value === 'month') d.setMonth(d.getMonth() + 1)
  else if (viewMode.value === 'week') d.setDate(d.getDate() + 7)
  else d.setDate(d.getDate() + 1)
  currentDate.value = d
  loadEvents()
}

function goToday() {
  currentDate.value = new Date()
  selectedDate.value = new Date()
  loadEvents()
}

function selectDate(date: Date) {
  selectedDate.value = date
}

function quickCreate(date: Date, hour: number) {
  editingEvent.value = null
  const start = new Date(date)
  start.setHours(hour, 0, 0, 0)
  const end = new Date(start)
  end.setHours(hour + 1)
  Object.assign(eventForm, {
    title: '',
    eventType: 'custom',
    startTime: start.getTime(),
    endTime: end.getTime(),
    location: '',
    color: '#18a058',
    reminderMinutes: 30
  })
  showCreateEvent.value = true
}

function editEvent(event: any) {
  editingEvent.value = event
  Object.assign(eventForm, {
    title: event.title,
    eventType: event.eventType,
    startTime: new Date(event.startTime).getTime(),
    endTime: event.endTime ? new Date(event.endTime).getTime() : null,
    location: event.location || '',
    color: event.color || '#18a058',
    reminderMinutes: event.reminderMinutes || 30
  })
  showCreateEvent.value = true
}

async function submitEvent() {
  const data: any = {
    title: eventForm.title,
    eventType: eventForm.eventType,
    startTime: eventForm.startTime ? new Date(eventForm.startTime).toISOString() : null,
    endTime: eventForm.endTime ? new Date(eventForm.endTime).toISOString() : null,
    location: eventForm.location,
    color: eventForm.color,
    reminderMinutes: eventForm.reminderMinutes
  }

  try {
    if (editingEvent.value) {
      data.id = editingEvent.value.id
      await request({ url: '/calendar/event', method: 'put', data })
      message.success('已更新')
    } else {
      await request({ url: '/calendar/event', method: 'post', data })
      message.success('已创建')
    }
    showCreateEvent.value = false
    editingEvent.value = null
    loadEvents()
  } catch { message.error('操作失败') }
}

async function deleteEvent() {
  if (!editingEvent.value) return
  try {
    await request({ url: `/calendar/event/${editingEvent.value.id}`, method: 'delete' })
    message.success('已删除')
    showCreateEvent.value = false
    editingEvent.value = null
    loadEvents()
  } catch { message.error('删除失败') }
}

async function loadEvents() {
  const d = currentDate.value
  const year = d.getFullYear()
  const month = d.getMonth() + 1
  const start = `${year}-${String(month).padStart(2, '0')}-01`
  const lastDay = new Date(year, month, 0).getDate()
  const end = `${year}-${String(month).padStart(2, '0')}-${lastDay}`
  try {
    const res = await request({
      url: '/calendar/merged',
      method: 'get',
      params: { start, end }
    })
    events.value = res?.data || []
  } catch {
    try {
      const res = await request({
        url: '/calendar/month',
        method: 'get',
        params: { year, month }
      })
      events.value = res?.data || []
    } catch { /* ignore */ }
  }
}

onMounted(loadEvents)
</script>

<style scoped>
.calendar-page {
  padding: 20px;
  background: #fff;
  border-radius: 8px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.page-header h2 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.calendar-nav {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.nav-title { margin: 0; font-size: 18px; }

/* 月视图 */
.weekday-header { display: grid; grid-template-columns: repeat(7, 1fr); }
.weekday-cell { text-align: center; font-size: 13px; color: #999; padding: 8px 0; }
.month-grid { display: grid; grid-template-columns: repeat(7, 1fr); border: 1px solid #e8e8e8; }
.day-cell {
  min-height: 100px;
  border-right: 1px solid #e8e8e8;
  border-bottom: 1px solid #e8e8e8;
  padding: 4px 6px;
  cursor: pointer;
}
.day-cell:hover { background: #fafafa; }
.day-cell.other-month { color: #ccc; background: #fafafa; }
.day-cell.today .day-number {
  background: #18a058;
  color: #fff;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  border-radius: 50%;
  display: inline-block;
}
.day-cell.selected { background: rgba(24, 160, 88, 0.05); }
.day-number { font-size: 13px; font-weight: 500; }
.event-dot {
  font-size: 11px;
  color: #fff;
  padding: 1px 4px;
  border-radius: 3px;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  cursor: pointer;
}
.more-events { font-size: 11px; color: #999; margin-top: 2px; }

/* 周视图 */
.time-grid { display: flex; overflow: hidden; }
.time-column { width: 60px; flex-shrink: 0; }
.time-label { height: 50px; font-size: 12px; color: #999; text-align: right; padding-right: 8px; line-height: 50px; }
.day-column { flex: 1; border-left: 1px solid #e8e8e8; position: relative; }
.day-col-header { text-align: center; padding: 8px 0; border-bottom: 1px solid #e8e8e8; }
.day-col-header.today { background: rgba(24, 160, 88, 0.08); }
.day-name { font-size: 12px; color: #999; }
.day-num { display: block; font-size: 16px; font-weight: 600; }
.day-slots { position: relative; }
.time-slot { height: 50px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.time-slot:hover { background: rgba(24, 160, 88, 0.04); }
.week-event {
  position: absolute;
  left: 2px;
  right: 2px;
  border-radius: 4px;
  color: #fff;
  padding: 2px 6px;
  font-size: 12px;
  cursor: pointer;
  overflow: hidden;
  z-index: 1;
}
.week-event-title { font-weight: 500; }
.week-event-time { opacity: 0.8; }

/* 日视图 */
.day-timeline { padding: 0 20px; }
.timeline-row { display: flex; min-height: 60px; border-bottom: 1px solid #f0f0f0; }
.timeline-time { width: 60px; font-size: 12px; color: #999; padding-top: 4px; }
.timeline-content { flex: 1; padding: 4px 0; cursor: pointer; }
.timeline-content:hover { background: rgba(24, 160, 88, 0.03); }
.timeline-event { padding: 6px 10px; margin-bottom: 4px; background: #f9f9f9; border-radius: 4px; cursor: pointer; }
.tl-event-title { font-size: 14px; font-weight: 500; }
.tl-event-meta { font-size: 12px; color: #999; margin-top: 2px; }

/* 颜色选择 */
.color-picker { display: flex; gap: 8px; }
.color-dot {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
}
.color-dot.selected { border-color: #333; }
</style>
