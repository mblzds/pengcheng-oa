<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card title="考勤记录">
        <div class="search-form">
          <n-form inline :model="recordFilter">
            <n-form-item v-if="!isEmployeeOnly" label="选择用户">
              <n-select v-model:value="recordFilter.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" />
            </n-form-item>
            <n-form-item label="日期范围">
              <n-date-picker
                v-model:value="recordFilter.dateRange"
                type="daterange"
                clearable
                :shortcuts="dateShortcuts"
                :update-value-on-close="false"
                format="yyyy-MM-dd"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 320px"
              />
            </n-form-item>
            <n-form-item label="状态">
              <n-select
                v-model:value="recordFilter.statusFilter"
                :options="statusFilterOptions"
                style="width: 140px"
              />
            </n-form-item>
            <n-form-item>
              <n-space>
                <n-button type="primary" @click="loadAttendanceRecords">查询</n-button>
                <n-button @click="resetRecordFilter">重置</n-button>
              </n-space>
            </n-form-item>
          </n-form>
        </div>

        <n-data-table
          :columns="recordColumns"
          :data="filteredRecordData"
          :loading="recordLoading"
          :pagination="recordPagination"
          :row-props="recordRowProps"
        />
        <div v-if="filteredRecordData.length > 200" class="table-tip">
          数据较多，建议缩小日期范围或加状态筛选
        </div>
      </n-card>

      <n-card title="月度考勤汇总报表">
        <n-form inline :model="summaryFilter">
          <n-form-item label="选择用户">
            <n-select v-model:value="summaryFilter.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="不选则显示可见范围内的全员" clearable style="width: 260px" :disabled="isEmployeeOnly" />
          </n-form-item>
          <n-form-item label="月份">
            <n-date-picker v-model:value="summaryFilter.month" type="month" style="width: 180px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="loadMonthlySummary">查询</n-button>
          </n-form-item>
        </n-form>

        <!-- 单人详情视图：选了具体用户时 -->
        <n-grid v-if="monthlySummary && summaryFilter.userId != null" :cols="4" :x-gap="12" :y-gap="12" style="margin-top: 12px">
          <n-gi><n-statistic label="出勤天数" :value="monthlySummary.attendanceDays || 0" /></n-gi>
          <n-gi><n-statistic label="迟到次数" :value="monthlySummary.lateTimes || 0" /></n-gi>
          <n-gi><n-statistic label="早退次数" :value="monthlySummary.earlyLeaveTimes || 0" /></n-gi>
          <n-gi><n-statistic label="请假天数" :value="monthlySummary.leaveDays || 0" /></n-gi>
        </n-grid>

        <!-- 批量列表视图：HR / 部门主管 默认看到全员 -->
        <n-data-table
          v-if="summaryFilter.userId == null"
          :columns="monthlyBatchColumns"
          :data="monthlyBatchData"
          :pagination="{ pageSize: 20 }"
          :bordered="false"
          size="small"
          style="margin-top: 12px"
        />
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

    <!-- 考勤详情抽屉：把主表删掉的字段（上班/下班 状态 / 位置 / 照片 + 豁免原因）集中到这里 -->
    <n-drawer v-model:show="detailVisible" :width="520" placement="right">
      <n-drawer-content title="考勤详情" closable>
        <template v-if="detailRow">
          <n-descriptions label-placement="left" :column="1" bordered size="small">
            <n-descriptions-item label="员工">
              {{ detailRow.userName || '-' }}
              <span v-if="detailRow.employeeNo" style="color:#999;margin-left:6px">{{ detailRow.employeeNo }}</span>
              <span v-if="detailRow.deptName" style="color:#999;margin-left:6px">· {{ detailRow.deptName }}</span>
            </n-descriptions-item>
            <n-descriptions-item label="日期">{{ detailRow.attendanceDate || '-' }}</n-descriptions-item>
            <n-descriptions-item label="日考勤状态">
              <n-tag size="small" :type="deriveDayStatus(detailRow).type">{{ deriveDayStatus(detailRow).label }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item v-if="detailRow.exemptReason" label="豁免原因">
              {{ detailRow.exemptReason }}
            </n-descriptions-item>
          </n-descriptions>

          <div class="detail-section">
            <div class="detail-section-title">上班打卡</div>
            <n-descriptions label-placement="left" :column="1" bordered size="small">
              <n-descriptions-item label="时间">{{ formatDateTime(detailRow.clockInTime) }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <component :is="renderClockStatus(detailRow.clockInStatus, true)" />
              </n-descriptions-item>
              <n-descriptions-item label="位置">{{ detailRow.clockInLocation || '-' }}</n-descriptions-item>
              <n-descriptions-item label="照片">
                <n-image v-if="detailRow.clockInPhoto" :src="detailRow.clockInPhoto" width="180" />
                <span v-else>-</span>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">下班打卡</div>
            <n-descriptions label-placement="left" :column="1" bordered size="small">
              <n-descriptions-item label="时间">{{ formatDateTime(detailRow.clockOutTime) }}</n-descriptions-item>
              <n-descriptions-item label="状态">
                <component :is="renderClockStatus(detailRow.clockOutStatus, false)" />
              </n-descriptions-item>
              <n-descriptions-item label="位置">{{ detailRow.clockOutLocation || '-' }}</n-descriptions-item>
              <n-descriptions-item label="照片">
                <n-image v-if="detailRow.clockOutPhoto" :src="detailRow.clockOutPhoto" width="180" />
                <span v-else>-</span>
              </n-descriptions-item>
            </n-descriptions>
          </div>

          <div class="detail-section">
            <div class="detail-section-title">工时</div>
            <div>{{ calcWorkHours(detailRow.clockInTime, detailRow.clockOutTime) }}</div>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NTag, NImage, NButton, type DataTableColumns } from 'naive-ui'
import { attendanceApi, type AttendanceMonthlyVO, type AttendanceRecordItem, type CompensateRequestItem, type LeaveRequestItem } from '@/api/attendance'

const userOptions = ref<any[]>([])
const recordLoading = ref(false)
const approvalLoading = ref(false)

const recordData = ref<AttendanceRecordItem[]>([])

// 详情抽屉
const detailVisible = ref(false)
const detailRow = ref<AttendanceRecordItem | null>(null)
function openDetail(row: AttendanceRecordItem) {
  detailRow.value = row
  detailVisible.value = true
}

// 整行可点击打开详情；按钮 / 链接等内部交互元素拦截掉，不触发行级 onClick
function recordRowProps(row: AttendanceRecordItem) {
  return {
    style: 'cursor: pointer;',
    onClick: (e: MouseEvent) => {
      const target = e.target as HTMLElement
      if (target && target.closest('button, a, .n-button, input, .n-tag')) return
      openDetail(row)
    }
  }
}
const leaveData = ref<LeaveRequestItem[]>([])
const compensateData = ref<CompensateRequestItem[]>([])
const monthlySummary = ref<AttendanceMonthlyVO | null>(null)
const monthlyBatchData = ref<AttendanceMonthlyVO[]>([])

const monthlyBatchColumns = [
  { title: '姓名', key: 'nickname', width: 120 },
  { title: '部门', key: 'deptName', width: 140 },
  { title: '应出勤', key: 'expectedWorkdays', width: 90, align: 'right' },
  { title: '出勤天数', key: 'attendanceDays', width: 90, align: 'right' },
  { title: '迟到', key: 'lateTimes', width: 70, align: 'right' },
  { title: '早退', key: 'earlyLeaveTimes', width: 70, align: 'right' },
  { title: '请假', key: 'leaveDays', width: 70, align: 'right' },
  { title: '调休', key: 'compensateDays', width: 70, align: 'right' },
  {
    title: '缺勤',
    key: 'absentDays',
    width: 80,
    align: 'right',
    render: (row: AttendanceRecordItem & { absentDays?: number }) => {
      const v = (row as any).absentDays ?? 0
      return v > 0 ? h('span', { style: 'color:#d03050;font-weight:600' }, v) : v
    }
  }
] as any[]

// 默认日期 = 今天（避免打开就拉全月/全历史）
function todayRange(): [number, number] {
  const d = new Date()
  const start = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  return [start, start + 86400000 - 1]
}

const recordFilter = reactive<{
  userId: number | null
  dateRange: [number, number] | null
  statusFilter: string  // 'all' | 'abnormal' | 'normal' | 'leave' | 'compensate'
}>({
  userId: null,
  dateRange: todayRange(),
  statusFilter: 'all'
})

const statusFilterOptions = [
  { label: '全部', value: 'all' },
  { label: '仅异常', value: 'abnormal' },
  { label: '正常', value: 'normal' },
  { label: '请假', value: 'leave' },
  { label: '调休', value: 'compensate' }
]

// 日期范围快捷预设
const dateShortcuts = {
  '今天': () => todayRange(),
  '本周': (): [number, number] => {
    const d = new Date()
    const day = d.getDay() || 7  // 周一=1, 周日转 7
    const monday = new Date(d.getFullYear(), d.getMonth(), d.getDate() - day + 1).getTime()
    return [monday, todayRange()[1]]
  },
  '本月': (): [number, number] => {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth(), 1).getTime()
    return [first, todayRange()[1]]
  },
  '上月': (): [number, number] => {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth() - 1, 1).getTime()
    const last = new Date(d.getFullYear(), d.getMonth(), 0, 23, 59, 59, 999).getTime()
    return [first, last]
  }
}

// 普通员工 = 后端 visible-users 只回自己一条
const isEmployeeOnly = computed(() => userOptions.value.length === 1)

// 前端按 statusFilter 过滤；状态由 deriveDayStatus 派生，不需要后端配合
const filteredRecordData = computed(() => {
  const f = recordFilter.statusFilter
  if (f === 'all') return recordData.value
  return recordData.value.filter(row => {
    const s = deriveDayStatus(row)
    if (f === 'abnormal') return s.type === 'error' || s.type === 'warning'
    if (f === 'normal') return s.type === 'success'
    if (f === 'leave') return s.label.startsWith('请假')
    if (f === 'compensate') return s.label === '调休'
    return true
  })
})

// 表格分页（前端分页）
const recordPagination = reactive({
  pageSize: 20,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100]
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

async function loadUserOptions() {
  try {
    const { request } = await import('@/utils/request')
    // 走考勤专用的可见用户接口（HR 全部 / 主管本部门+下级 / 员工仅自己），
    // 后端按当前登录角色过滤；员工只会拿到自己一条，下拉自然只能选自己。
    const list: any = await request({ url: '/admin/attendance/visible-users', method: 'get' })
    userOptions.value = Array.isArray(list) ? list : []
    // 员工只有自己 → 自动锁定到本人，省去手动选
    if (userOptions.value.length === 1) {
      const onlyId = userOptions.value[0].id
      recordFilter.userId = onlyId
      summaryFilter.userId = onlyId
      approvalFilter.userId = onlyId
    }
  } catch {
    userOptions.value = []
  }
}

const approvalStatusOptions = [
  { label: '待审批', value: 1 },
  { label: '已通过', value: 2 },
  { label: '已驳回', value: 3 }
]

const recordColumns: DataTableColumns<AttendanceRecordItem> = [
  { title: '员工', key: 'userName', width: 220, render: row => renderEmployee(row) },
  {
    title: '日考勤状态',
    key: 'dayStatus',
    width: 100,
    align: 'center',
    titleAlign: 'center',
    render: row => {
      const s = deriveDayStatus(row)
      return h(NTag, { size: 'small', type: s.type }, { default: () => s.label })
    }
  },
  { title: '日期', key: 'attendanceDate', width: 100, align: 'center', titleAlign: 'center' },
  {
    title: '上班打卡',
    key: 'clockInTime',
    width: 90,
    align: 'center',
    titleAlign: 'center',
    render: row => formatTimeOnly(row.clockInTime)
  },
  {
    title: '下班打卡',
    key: 'clockOutTime',
    width: 90,
    align: 'center',
    titleAlign: 'center',
    render: row => formatTimeOnly(row.clockOutTime)
  },
  {
    title: '工时',
    key: 'workHours',
    width: 80,
    align: 'center',
    titleAlign: 'center',
    render: row => calcWorkHours(row.clockInTime, row.clockOutTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 80,
    align: 'center',
    titleAlign: 'center',
    render: row => h(NButton, {
      size: 'small',
      quaternary: true,
      type: 'primary',
      onClick: () => openDetail(row)
    }, { default: () => '详情' })
  }
]

const leaveColumns: DataTableColumns<LeaveRequestItem> = [
  { title: '申请ID', key: 'id', width: 90 },
  { title: '员工', key: 'userName', width: 200, render: row => renderEmployee(row) },
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
  { title: '员工', key: 'userName', width: 200, render: row => renderEmployee(row) },
  { title: '调休日期', key: 'compensateDate', width: 120 },
  { title: '原因', key: 'reason' },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => renderApprovalStatus(row.status)
  }
]

function renderEmployee(row: { userId?: number; userName?: string; employeeNo?: string; deptName?: string }) {
  const name = row.userName || (row.userId != null ? `用户#${row.userId}` : '-')
  const meta = [row.employeeNo, row.deptName].filter(Boolean).join(' · ')
  return h('div', null, [
    h('div', { style: 'font-weight: 500' }, name),
    meta ? h('div', { style: 'color: #999; font-size: 12px; margin-top: 2px' }, meta) : null
  ])
}

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

/** 仅显示 HH:mm，考勤记录列表的"日期"列已经显示了日期，打卡时间无需重复 */
function formatTimeOnly(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

/** 计算工时（小时，保留 1 位小数）；任一打卡缺失返回 '-' */
function calcWorkHours(clockIn?: string, clockOut?: string) {
  if (!clockIn || !clockOut) return '-'
  const start = new Date(clockIn).getTime()
  const end = new Date(clockOut).getTime()
  if (Number.isNaN(start) || Number.isNaN(end) || end <= start) return '-'
  return ((end - start) / 3600000).toFixed(1) + 'h'
}

/**
 * 由打卡情况 + 豁免原因推导日考勤状态汇总
 * 豁免（请假 / 调休）优先级高于打卡判定：当天请假即使没打卡也不算缺卡
 */
function deriveDayStatus(row: AttendanceRecordItem): { label: string; type: 'success' | 'warning' | 'error' | 'info' } {
  // ① 优先看豁免原因：请假/调休审批通过会联动写入 exemptReason
  if (row.exemptReason) {
    if (row.exemptReason.startsWith('leave-')) {
      const sub = row.exemptReason.slice(6)
      return { label: sub ? '请假·' + sub : '请假', type: 'info' }
    }
    if (row.exemptReason.startsWith('leave')) {
      return { label: '请假', type: 'info' }
    }
    if (row.exemptReason === 'compensate') {
      return { label: '调休', type: 'info' }
    }
    // 兜底：未知豁免原因原样展示，方便后续扩展
    return { label: row.exemptReason, type: 'info' }
  }
  // ② 打卡判定
  if (!row.clockInTime && !row.clockOutTime) return { label: '缺勤', type: 'error' }
  if (!row.clockInTime || !row.clockOutTime) return { label: '缺卡', type: 'error' }
  const inLate = row.clockInStatus !== 1
  const outEarly = row.clockOutStatus !== 1
  if (inLate && outEarly) return { label: '迟到+早退', type: 'error' }
  if (inLate) return { label: '迟到', type: 'warning' }
  if (outEarly) return { label: '早退', type: 'warning' }
  return { label: '正常', type: 'success' }
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
  recordFilter.userId = isEmployeeOnly.value && userOptions.value.length === 1
    ? userOptions.value[0].id
    : null
  recordFilter.dateRange = todayRange()
  recordFilter.statusFilter = 'all'
  loadAttendanceRecords()
}

async function loadMonthlySummary() {
  const monthDate = new Date(summaryFilter.month)
  const year = monthDate.getFullYear()
  const month = monthDate.getMonth() + 1
  if (summaryFilter.userId != null) {
    // 选了具体用户 → 单人详情
    const res: any = await attendanceApi.monthly({
      userId: summaryFilter.userId,
      year,
      month
    })
    monthlySummary.value = res?.data ?? res ?? null
    monthlyBatchData.value = []
  } else {
    // 未选用户 → 批量列表（HR/主管视图）
    const res: any = await attendanceApi.monthlyBatch({ year, month })
    monthlyBatchData.value = (res?.data ?? res ?? []) as AttendanceMonthlyVO[]
    monthlySummary.value = null
  }
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

onMounted(async () => {
  // 先把 visible-users 拿到（员工被锁到自己；HR/主管 保持 null），
  // 月度汇总根据 userId 状态自动决定走单人 / 批量
  await loadUserOptions()
  loadAttendanceRecords()
  loadApprovalLists()
  loadMonthlySummary()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 12px;
}

.approval-filter {
  margin-bottom: 12px;
}

.detail-section {
  margin-top: 16px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 8px;
  border-left: 3px solid #18a058;
  padding-left: 8px;
}

.table-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: right;
}
</style>
