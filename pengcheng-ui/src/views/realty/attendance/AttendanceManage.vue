<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card title="考勤记录">
        <div class="search-form">
          <n-form inline :model="recordFilter">
            <n-form-item label="选择用户">
              <n-select v-model:value="recordFilter.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" />
            </n-form-item>
            <n-form-item label="日期范围">
              <n-date-picker v-model:value="recordFilter.dateRange" type="daterange" clearable style="width: 280px" />
            </n-form-item>
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="loadAttendanceRecords">查询</n-button>
                <n-button @click="resetRecordFilter">重置</n-button>
              </n-space>
            </n-form-item>
          </n-form>
        </div>

        <n-data-table :columns="recordColumns" :data="recordData" :loading="recordLoading" :pagination="false" />
      </n-card>

      <n-card title="月度考勤汇总报表">
        <n-form inline :model="summaryFilter">
          <n-form-item label="选择用户">
            <n-select v-model:value="summaryFilter.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" />
          </n-form-item>
          <n-form-item label="月份">
            <n-date-picker v-model:value="summaryFilter.month" type="month" style="width: 180px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="loadMonthlySummary">查询汇总</n-button>
          </n-form-item>
        </n-form>

        <n-grid v-if="monthlySummary" :cols="4" :x-gap="12" :y-gap="12" style="margin-top: 12px">
          <n-gi><n-statistic label="出勤天数" :value="monthlySummary.attendanceDays || 0" /></n-gi>
          <n-gi><n-statistic label="迟到次数" :value="monthlySummary.lateTimes || 0" /></n-gi>
          <n-gi><n-statistic label="早退次数" :value="monthlySummary.earlyLeaveTimes || 0" /></n-gi>
          <n-gi><n-statistic label="请假天数" :value="monthlySummary.leaveDays || 0" /></n-gi>
        </n-grid>
      </n-card>

      <n-card title="请假/调休审批列表">
        <n-form inline :model="approvalFilter" class="approval-filter">
          <n-form-item label="选择用户">
            <n-select v-model:value="approvalFilter.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" />
          </n-form-item>
          <n-form-item label="审批状态">
            <n-select v-model:value="approvalFilter.status" :options="approvalStatusOptions" clearable style="width: 160px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="loadApprovalLists">查询</n-button>
          </n-form-item>
        </n-form>

        <n-tabs type="line" animated>
          <n-tab-pane name="leave" tab="请假申请">
            <n-data-table :columns="leaveColumns" :data="leaveData" :loading="approvalLoading" :pagination="false" />
          </n-tab-pane>
          <n-tab-pane name="compensate" tab="调休申请">
            <n-data-table :columns="compensateColumns" :data="compensateData" :loading="approvalLoading" :pagination="false" />
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import { userApi } from '@/api/system'
import { attendanceApi, type AttendanceMonthlyVO, type AttendanceRecordItem, type CompensateRequestItem, type LeaveRequestItem } from '@/api/attendance'

const userOptions = ref<any[]>([])
const recordLoading = ref(false)
const approvalLoading = ref(false)

const recordData = ref<AttendanceRecordItem[]>([])
const leaveData = ref<LeaveRequestItem[]>([])
const compensateData = ref<CompensateRequestItem[]>([])
const monthlySummary = ref<AttendanceMonthlyVO | null>(null)

const recordFilter = reactive<{
  userId: number | null
  dateRange: [number, number] | null
}>({
  userId: null,
  dateRange: null
})

const summaryFilter = reactive<{
  userId: number | null
  month: number
}>({
  userId: null,
  month: Date.now()
})

const approvalFilter = reactive<{
  userId: number | null
  status: number | null
}>({
  userId: null,
  status: null
})

function loadUserOptions() {
  userApi.page({ page: 1, pageSize: 500 }).then((res: any) => {
    const list = res?.list ?? res?.data?.records ?? res?.records ?? []
    userOptions.value = Array.isArray(list) ? list : []
  }).catch(() => { userOptions.value = [] })
}

const approvalStatusOptions = [
  { label: '待审批', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 }
]

const recordColumns: DataTableColumns<AttendanceRecordItem> = [
  { title: '人员ID', key: 'userId', width: 100 },
  { title: '考勤日期', key: 'attendanceDate', width: 120 },
  {
    title: '上班打卡',
    key: 'clockInTime',
    width: 170,
    render: row => formatDateTime(row.clockInTime)
  },
  {
    title: '上班状态',
    key: 'clockInStatus',
    width: 100,
    render: row => renderClockStatus(row.clockInStatus, true)
  },
  {
    title: '下班打卡',
    key: 'clockOutTime',
    width: 170,
    render: row => formatDateTime(row.clockOutTime)
  },
  {
    title: '下班状态',
    key: 'clockOutStatus',
    width: 100,
    render: row => renderClockStatus(row.clockOutStatus, false)
  },
  { title: '上班位置', key: 'clockInLocation' },
  { title: '下班位置', key: 'clockOutLocation' }
]

const leaveColumns: DataTableColumns<LeaveRequestItem> = [
  { title: '申请ID', key: 'id', width: 90 },
  { title: '人员ID', key: 'userId', width: 90 },
  {
    title: '类型',
    key: 'leaveType',
    width: 100,
    render: row => leaveTypeText(row.leaveType)
  },
  {
    title: '开始时间',
    key: 'startTime',
    width: 170,
    render: row => formatDateTime(row.startTime)
  },
  {
    title: '结束时间',
    key: 'endTime',
    width: 170,
    render: row => formatDateTime(row.endTime)
  },
  { title: '原因', key: 'reason' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => renderApprovalStatus(row.status)
  }
]

const compensateColumns: DataTableColumns<CompensateRequestItem> = [
  { title: '申请ID', key: 'id', width: 90 },
  { title: '人员ID', key: 'userId', width: 90 },
  { title: '调休日期', key: 'compensateDate', width: 120 },
  { title: '原因', key: 'reason' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => renderApprovalStatus(row.status)
  }
]

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function toDateString(ms: number) {
  const date = new Date(ms)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function renderClockStatus(status?: number, isIn = true) {
  if (status === undefined || status === null) {
    return '-'
  }
  const text = isIn ? (status === 1 ? '正常' : '迟到') : status === 1 ? '正常' : '早退'
  const type: 'success' | 'warning' = status === 1 ? 'success' : 'warning'
  return h(NTag, { size: 'small', type }, { default: () => text })
}

function renderApprovalStatus(status: number) {
  const map: Record<number, { text: string; type: 'warning' | 'success' | 'error' }> = {
    1: { text: '待审批', type: 'warning' },
    2: { text: '已通过', type: 'success' },
    3: { text: '已驳回', type: 'error' }
  }
  const item = map[status] || { text: '-', type: 'warning' as const }
  return h(NTag, { size: 'small', type: item.type }, { default: () => item.text })
}

function leaveTypeText(type: number) {
  const map: Record<number, string> = {
    1: '事假',
    2: '病假',
    3: '年假',
    4: '婚假',
    5: '产假',
    6: '调休'
  }
  return map[type] || '-'
}

async function loadAttendanceRecords() {
  recordLoading.value = true
  try {
    const [startDate, endDate] = recordFilter.dateRange || []
    const res: any = await attendanceApi.records({
      userId: recordFilter.userId || undefined,
      startDate: startDate ? toDateString(startDate) : undefined,
      endDate: endDate ? toDateString(endDate) : undefined
    })
    recordData.value = res?.data ?? res ?? []
  } finally {
    recordLoading.value = false
  }
}

function resetRecordFilter() {
  recordFilter.userId = null
  recordFilter.dateRange = null
  loadAttendanceRecords()
}

async function loadMonthlySummary() {
  if (summaryFilter.userId == null) return
  const monthDate = new Date(summaryFilter.month)
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth() + 1
  const res: any = await attendanceApi.monthly({
    userId: summaryFilter.userId,
    year,
    month
  })
  monthlySummary.value = res?.data ?? res ?? null
}

async function loadApprovalLists() {
  approvalLoading.value = true
  try {
    const [leavesRes, compRes] = await Promise.all([
      attendanceApi.leaveList({
        userId: approvalFilter.userId || undefined,
        status: approvalFilter.status || undefined
      }),
      attendanceApi.compensateList({
        userId: approvalFilter.userId || undefined,
        status: approvalFilter.status || undefined
      })
    ])
    leaveData.value = (leavesRes as any)?.data ?? (leavesRes as any) ?? []
    compensateData.value = (compRes as any)?.data ?? (compRes as any) ?? []
  } finally {
    approvalLoading.value = false
  }
}

onMounted(() => {
  loadUserOptions()
  loadAttendanceRecords()
  loadApprovalLists()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 12px;
}

.approval-filter {
  margin-bottom: 12px;
}
</style>
