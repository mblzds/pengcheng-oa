<template>
  <div class="project-detail">
    <n-page-header @back="router.back()" :title="project?.name">
      <template #extra>
        <n-space>
          <n-tag v-if="project">{{ statusMap[project.status] }}</n-tag>
          <n-button size="small" @click="loadProject">刷新</n-button>
        </n-space>
      </template>
    </n-page-header>
    <n-card v-if="project">
      <n-tabs type="line" animated>
        <n-tab-pane name="tasks" tab="任务列表">
          <n-space>
            <n-button type="primary" size="small" @click="showTaskForm = true">新建任务</n-button>
          </n-space>
          <n-data-table :columns="taskColumns" :data="tasks" :loading="taskLoading" style="margin-top: 12px" />
        </n-tab-pane>
        <n-tab-pane name="board" tab="看板">
          <div class="board-columns">
            <div
              v-for="col in boardColumnList"
              :key="col.statusValue"
              class="board-col"
              :class="{ 'board-col-drag-over': dragOverCol === col.statusValue }"
              @dragover.prevent="dragOverCol = col.statusValue"
              @dragleave="dragOverCol = dragOverCol === col.statusValue ? null : dragOverCol"
              @drop.prevent="onBoardDrop(col.statusValue)"
            >
              <h4>{{ col.name }}</h4>
              <div
                v-for="t in (boardData[col.statusValue] || [])"
                :key="t.id"
                class="board-card"
                draggable="true"
                @dragstart="onBoardDragStart($event, t)"
                @dragend="dragOverCol = null; draggedTask = null"
              >
                {{ t.title }}
              </div>
            </div>
          </div>
        </n-tab-pane>
        <n-tab-pane name="gantt" tab="甘特图">
          <div ref="ganttChartRef" style="width: 100%; height: 400px"></div>
        </n-tab-pane>
        <n-tab-pane name="calendar" tab="日历">
          <n-space style="margin-bottom: 8px" align="center">
            <n-button size="small" @click="calendarMonth = prevMonth(calendarMonth)">上月</n-button>
            <span style="font-weight: 500">{{ calendarMonth }}</span>
            <n-button size="small" @click="calendarMonth = nextMonth(calendarMonth)">下月</n-button>
          </n-space>
          <div class="cal-grid">
            <div v-for="d in ['一','二','三','四','五','六','日']" :key="d" class="cal-header">{{ d }}</div>
            <div v-for="cell in calendarCells" :key="cell.key" class="cal-cell" :class="{ today: cell.isToday, other: !cell.inMonth }">
              <div class="cal-day">{{ cell.day }}</div>
              <div v-for="ev in cell.events" :key="ev.id" class="cal-event" :title="ev.title">{{ ev.title }}</div>
            </div>
          </div>
        </n-tab-pane>
        <n-tab-pane name="milestones" tab="里程碑">
          <n-button size="small" @click="showMilestoneForm = true">新建里程碑</n-button>
          <n-data-table :columns="milestoneColumns" :data="milestones" :loading="milestoneLoading" style="margin-top: 12px" />
        </n-tab-pane>
        <n-tab-pane name="settings" tab="设置">
          <n-form :model="settingsForm" label-placement="left" label-width="90" style="max-width: 480px; margin-bottom: 24px">
            <n-form-item label="项目名称">
              <n-input v-model:value="settingsForm.name" placeholder="项目名称" />
            </n-form-item>
            <n-form-item label="描述">
              <n-input v-model:value="settingsForm.description" type="textarea" placeholder="项目描述" />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="settingsForm.status" :options="statusOptions" />
            </n-form-item>
            <n-form-item label="计划开始">
              <n-date-picker v-model:value="settingsForm.startDateTs" type="date" value-format="yyyy-MM-dd" clearable style="width: 100%" />
            </n-form-item>
            <n-form-item label="计划结束">
              <n-date-picker v-model:value="settingsForm.endDateTs" type="date" value-format="yyyy-MM-dd" clearable style="width: 100%" />
            </n-form-item>
            <n-form-item label="可见性">
              <n-select v-model:value="settingsForm.visibility" :options="visibilityOptions" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="saveProjectSettings">保存</n-button>
            </n-form-item>
          </n-form>
          <n-divider>看板列配置（V24）</n-divider>
          <p style="color: var(--n-text-color-3); font-size: 13px; margin-bottom: 8px">自定义看板列后，看板将按此顺序显示；未配置时使用默认：待办、进行中、已完成。</p>
          <n-space style="margin-bottom: 8px">
            <n-button size="small" @click="showStatusColumnForm(true)">新增列</n-button>
            <n-button size="small" secondary @click="applyDefaultBoardColumns">使用默认列</n-button>
          </n-space>
          <n-data-table :columns="statusColumnTableColumns" :data="boardColumns" style="margin-bottom: 16px" />
          <n-divider>成员管理</n-divider>
          <n-space style="margin-bottom: 8px">
            <n-button size="small" @click="loadMembers(); showAddMember = true">添加成员</n-button>
          </n-space>
          <n-data-table :columns="memberColumns" :data="members" :loading="memberLoading" />
        </n-tab-pane>
      </n-tabs>
    </n-card>
    <n-spin v-else :show="loading" />

    <n-modal v-model:show="showTaskForm" preset="card" title="新建任务" style="width: 420px" @ok="submitTask">
      <n-form :model="taskForm" label-width="80">
        <n-form-item label="标题" required>
          <n-input v-model:value="taskForm.title" placeholder="任务标题" />
        </n-form-item>
        <n-form-item label="状态">
          <n-select v-model:value="taskForm.status" :options="taskStatusOptions" />
        </n-form-item>
        <n-form-item label="进度">
          <n-input-number v-model:value="taskForm.progress" :min="0" :max="100" />
        </n-form-item>
      </n-form>
    </n-modal>
    <n-modal v-model:show="showMilestoneForm" preset="card" title="新建里程碑" style="width: 420px" @ok="submitMilestone">
      <n-form :model="milestoneForm" label-width="80">
        <n-form-item label="名称" required>
          <n-input v-model:value="milestoneForm.name" placeholder="里程碑名称" />
        </n-form-item>
        <n-form-item label="目标日期">
          <n-date-picker v-model:value="milestoneForm.dueDateTs" type="date" value-format="yyyy-MM-dd" />
        </n-form-item>
      </n-form>
    </n-modal>
    <n-modal v-model:show="showAddMember" preset="card" title="添加成员" style="width: 400px" @ok="submitAddMember">
      <n-form :model="addMemberForm" label-width="80">
        <n-form-item label="用户" required>
          <n-select v-model:value="addMemberForm.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" />
        </n-form-item>
        <n-form-item label="角色">
          <n-select v-model:value="addMemberForm.role" :options="roleOptions" />
        </n-form-item>
      </n-form>
    </n-modal>
    <n-modal v-model:show="showStatusColumnModal" preset="card" :title="editingStatusColumnId ? '编辑看板列' : '新增看板列'" style="width: 400px" @ok="submitStatusColumn">
      <n-form :model="statusColumnForm" label-width="90">
        <n-form-item label="列名" required>
          <n-input v-model:value="statusColumnForm.name" placeholder="如：需求评审" />
        </n-form-item>
        <n-form-item label="状态值" required>
          <n-input v-model:value="statusColumnForm.statusValue" placeholder="任务 status 取值，如：需求评审" />
        </n-form-item>
        <n-form-item label="视为已完成">
          <n-switch v-model:value="statusColumnForm.isDone" :checked-value="1" :unchecked-value="0" />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { projectApi, projectTaskApi, projectMilestoneApi } from '@/api/project'

interface BoardColumn { id?: number; name: string; statusValue: string; sortOrder?: number; isDone?: number }
import { userApi } from '@/api/system'
import type { DataTableColumns } from 'naive-ui'
import { NButton, NSpace, useMessage } from 'naive-ui'
import { h } from 'vue'
import * as echarts from 'echarts'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const projectId = computed(() => Number(route.params.id))

const statusMap: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已暂停', 4: '已完成', 5: '已归档' }
const project = ref<any>(null)
const loading = ref(true)
const tasks = ref<any[]>([])
const taskLoading = ref(false)
const boardData = ref<Record<string, any[]>>({})
const boardColumns = ref<BoardColumn[]>([])
const dragOverCol = ref<string | null>(null)
const DEFAULT_BOARD_COLUMNS: BoardColumn[] = [
  { name: '待办', statusValue: '待办' },
  { name: '进行中', statusValue: '进行中' },
  { name: '已完成', statusValue: '已完成' },
]
const boardColumnList = computed(() =>
  boardColumns.value?.length ? boardColumns.value : DEFAULT_BOARD_COLUMNS
)
const draggedTask = ref<{ id: number; status: string } | null>(null)
const milestones = ref<any[]>([])
const milestoneLoading = ref(false)
const showTaskForm = ref(false)
const showMilestoneForm = ref(false)
const showAddMember = ref(false)
const members = ref<any[]>([])
const memberLoading = ref(false)
const userOptions = ref<any[]>([])
const statusOptions = [
  { label: '未开始', value: 1 },
  { label: '进行中', value: 2 },
  { label: '已暂停', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已归档', value: 5 },
]
const visibilityOptions = [
  { label: '仅成员', value: 'private' },
  { label: '本部门', value: 'dept' },
  { label: '全公司', value: 'all' },
]
const roleOptions = [
  { label: '成员', value: 'member' },
  { label: '管理员', value: 'admin' },
  { label: '负责人', value: 'owner' },
]
const settingsForm = reactive({
  name: '',
  description: '',
  status: 1,
  startDateTs: null as number | null,
  endDateTs: null as number | null,
  visibility: 'private',
})
const addMemberForm = reactive({ userId: null as number | null, role: 'member' })
const showStatusColumnModal = ref(false)
const editingStatusColumnId = ref<number | null>(null)
const statusColumnForm = reactive({ name: '', statusValue: '', isDone: 0 })
const statusColumnTableColumns: DataTableColumns<BoardColumn> = [
  { title: '列名', key: 'name', width: 120 },
  { title: '状态值', key: 'statusValue', width: 100 },
  { title: '视为已完成', key: 'isDone', width: 90, render: (r) => (r.isDone ? '是' : '否') },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (r) => r.id != null ? h(NSpace, null, { default: () => [
      h(NButton, { size: 'small', onClick: () => showStatusColumnForm(false, r) }, () => '编辑'),
      h(NButton, { size: 'small', type: 'error', tertiary: true, onClick: () => removeStatusColumn(r.id!) }, () => '删除'),
    ] }) : null,
  },
]
const ganttChartRef = ref<HTMLElement | null>(null)
let ganttChart: echarts.ECharts | null = null
const ganttData = ref<any[]>([])
const calendarMonth = ref(new Date().toISOString().slice(0, 7))
const calendarEvents = ref<any[]>([])

interface CalCell { key: string; day: number; inMonth: boolean; isToday: boolean; events: any[] }
const calendarCells = computed<CalCell[]>(() => {
  const [y, m] = calendarMonth.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const last = new Date(y, m, 0)
  const startDay = (first.getDay() + 6) % 7
  const todayStr = new Date().toISOString().slice(0, 10)
  const cells: CalCell[] = []
  for (let i = -startDay; i < 42 - startDay && cells.length < 42; i++) {
    const d = new Date(y, m - 1, 1 + i)
    const ds = d.toISOString().slice(0, 10)
    cells.push({
      key: ds,
      day: d.getDate(),
      inMonth: d.getMonth() === m - 1,
      isToday: ds === todayStr,
      events: calendarEvents.value.filter((e: any) => (e.dueDate || '').slice(0, 10) === ds),
    })
  }
  return cells
})
function prevMonth(m: string) { const d = new Date(m + '-01'); d.setMonth(d.getMonth() - 1); return d.toISOString().slice(0, 7) }
function nextMonth(m: string) { const d = new Date(m + '-01'); d.setMonth(d.getMonth() + 1); return d.toISOString().slice(0, 7) }
const memberColumns: DataTableColumns<any> = [
  { title: '用户ID', key: 'userId', width: 90 },
  { title: '角色', key: 'role', width: 90 },
  { title: '加入时间', key: 'joinTime', width: 170 },
  {
    title: '操作',
    key: 'action',
    width: 140,
    render: (row) => h(NButton, {
      size: 'tiny',
      type: 'error',
      quaternary: true,
      onClick: () => { if (window.confirm('确定移出项目？')) removeMember(row.userId) },
    }, { default: () => '移除' }),
  },
]

const taskForm = reactive({ title: '', status: '待办', progress: 0 })
const taskStatusOptions = computed(() =>
  boardColumnList.value.map(c => ({ label: c.name, value: c.statusValue }))
)
const milestoneForm = reactive({ name: '', dueDateTs: null as number | null })

const taskColumns: DataTableColumns<any> = [
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 90 },
  { title: '进度', key: 'progress', width: 80, render: (row) => `${row.progress ?? 0}%` },
  { title: '截止', key: 'dueDate', width: 110 },
]
const milestoneColumns: DataTableColumns<any> = [
  { title: '名称', key: 'name' },
  { title: '目标日期', key: 'dueDate', width: 120 },
  { title: '状态', key: 'status', width: 80, render: (row) => row.status === 1 ? '已完成' : '未完成' },
]

function loadProject() {
  if (!projectId.value) return
  loading.value = true
  projectApi.get(projectId.value).then((res: any) => {
    project.value = res?.data ?? res
    if (project.value) {
      settingsForm.name = project.value.name ?? ''
      settingsForm.description = project.value.description ?? ''
      settingsForm.status = project.value.status ?? 1
      settingsForm.visibility = project.value.visibility ?? 'private'
      settingsForm.startDateTs = project.value.startDate ? new Date(project.value.startDate).getTime() : null
      settingsForm.endDateTs = project.value.endDate ? new Date(project.value.endDate).getTime() : null
    }
  }).finally(() => { loading.value = false })
}

function loadMembers() {
  if (!projectId.value) return
  memberLoading.value = true
  projectApi.members(projectId.value).then((res: any) => {
    members.value = res?.data ?? res ?? []
  }).finally(() => { memberLoading.value = false })
}

function saveProjectSettings() {
  if (!projectId.value || !project.value) return
  const startDate = settingsForm.startDateTs ? new Date(settingsForm.startDateTs).toISOString().slice(0, 10) : null
  const endDate = settingsForm.endDateTs ? new Date(settingsForm.endDateTs).toISOString().slice(0, 10) : null
  projectApi.update(projectId.value, {
    name: settingsForm.name,
    description: settingsForm.description,
    status: settingsForm.status,
    visibility: settingsForm.visibility,
    startDate,
    endDate,
  }).then(() => { message.success('已保存'); loadProject() })
}

function submitAddMember() {
  if (!projectId.value || addMemberForm.userId == null) { message.warning('请选择用户'); return }
  projectApi.addMember(projectId.value, addMemberForm.userId, addMemberForm.role).then(() => {
    showAddMember.value = false
    addMemberForm.userId = null
    addMemberForm.role = 'member'
    loadMembers()
    message.success('已添加')
  })
}

function removeMember(userId: number) {
  if (!projectId.value) return
  projectApi.removeMember(projectId.value, userId).then(() => { loadMembers(); message.success('已移除') })
}

function loadUserOptions() {
  userApi.page({ page: 1, pageSize: 200 }).then((res: any) => {
    const list = res?.list ?? res?.data?.records ?? []
    userOptions.value = Array.isArray(list) ? list : []
  })
}

function loadGantt() {
  if (!projectId.value) return
  projectTaskApi.gantt(projectId.value).then((res: any) => {
    ganttData.value = res?.data ?? res ?? []
    nextTick(renderGantt)
  })
}

function renderGantt() {
  if (!ganttChartRef.value) return
  if (!ganttChart) ganttChart = echarts.init(ganttChartRef.value)
  const items = ganttData.value.filter((t: any) => t.startDate && t.dueDate)
  if (!items.length) { ganttChart.clear(); return }
  const cats = items.map((t: any) => t.title)
  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']
  ganttChart.setOption({
    tooltip: { formatter: (p: any) => `${p.name}<br/>进度: ${p.data?.progress ?? 0}%` },
    grid: { left: 160, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'time', axisLabel: { formatter: '{MM}-{dd}' } },
    yAxis: { type: 'category', data: cats, inverse: true },
    series: [{
      type: 'custom',
      renderItem: (_: any, api: any) => {
        const catIdx = api.value(0)
        const start = api.coord([api.value(1), catIdx])
        const end = api.coord([api.value(2), catIdx])
        const h = api.size([0, 1])[1] * 0.6
        return { type: 'rect', shape: { x: start[0], y: start[1] - h / 2, width: Math.max(end[0] - start[0], 4), height: h }, style: { fill: colors[catIdx % colors.length] } }
      },
      encode: { x: [1, 2], y: 0 },
      data: items.map((t: any, i: number) => ({
        value: [i, new Date(t.startDate).getTime(), new Date(t.dueDate).getTime()],
        name: t.title,
        progress: t.progress,
      })),
    }],
  }, true)
}

function loadCalendarData() {
  if (!projectId.value) return
  projectTaskApi.calendar(projectId.value).then((res: any) => {
    const taskEvts = res?.data ?? res ?? []
    projectMilestoneApi.list(projectId.value).then((mres: any) => {
      const ms = (mres?.data ?? mres ?? []).filter((m: any) => m.dueDate).map((m: any) => ({
        id: 'ms-' + m.id, title: '🏁 ' + m.name, dueDate: m.dueDate, type: 'milestone',
      }))
      calendarEvents.value = [...taskEvts, ...ms]
    })
  })
}

function loadTasks() {
  if (!projectId.value) return
  taskLoading.value = true
  projectTaskApi.page(projectId.value, { page: 1, size: 100 }).then((res: any) => {
    const d = res?.data ?? res
    tasks.value = d?.records ?? d ?? []
  }).finally(() => { taskLoading.value = false })
}

function loadBoard() {
  if (!projectId.value) return
  Promise.all([
    projectTaskApi.board(projectId.value),
    projectApi.statusColumns(projectId.value),
  ]).then(([boardRes, colsRes]: any[]) => {
    boardData.value = boardRes?.data ?? boardRes ?? {}
    const list = colsRes?.data ?? colsRes ?? []
    boardColumns.value = Array.isArray(list) ? list : []
  })
}

function loadStatusColumns() {
  if (!projectId.value) return
  projectApi.statusColumns(projectId.value).then((res: any) => {
    const list = res?.data ?? res ?? []
    boardColumns.value = Array.isArray(list) ? list : []
  })
}

function showStatusColumnForm(isNew: boolean, row?: BoardColumn) {
  editingStatusColumnId.value = isNew ? null : (row?.id ?? null)
  statusColumnForm.name = isNew ? '' : (row?.name ?? '')
  statusColumnForm.statusValue = isNew ? '' : (row?.statusValue ?? '')
  statusColumnForm.isDone = isNew ? 0 : (row?.isDone ?? 0)
  showStatusColumnModal.value = true
}

function submitStatusColumn() {
  if (!projectId.value || !statusColumnForm.name?.trim() || !statusColumnForm.statusValue?.trim()) return
  if (editingStatusColumnId.value != null) {
    projectApi.updateStatusColumn(editingStatusColumnId.value, {
      name: statusColumnForm.name,
      statusValue: statusColumnForm.statusValue,
      isDone: statusColumnForm.isDone,
    }).then(() => { showStatusColumnModal.value = false; loadStatusColumns(); loadBoard() })
  } else {
    projectApi.createStatusColumn(projectId.value, {
      name: statusColumnForm.name,
      statusValue: statusColumnForm.statusValue,
      isDone: statusColumnForm.isDone,
    }).then(() => { showStatusColumnModal.value = false; loadStatusColumns(); loadBoard() })
  }
}

function removeStatusColumn(columnId: number) {
  projectApi.removeStatusColumn(columnId).then(() => { loadStatusColumns(); loadBoard() })
}

function applyDefaultBoardColumns() {
  if (!projectId.value) return
  const defaults = [{ name: '待办', statusValue: '待办', isDone: 0 }, { name: '进行中', statusValue: '进行中', isDone: 0 }, { name: '已完成', statusValue: '已完成', isDone: 1 }]
  Promise.all(defaults.map(d => projectApi.createStatusColumn(projectId.value!, d))).then(() => {
    message.success('已应用默认列')
    loadStatusColumns()
    loadBoard()
  })
}

function onBoardDragStart(e: DragEvent, task: any) {
  if (!e.dataTransfer) return
  draggedTask.value = { id: task.id, status: task.status }
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(task.id))
}

function onBoardDrop(targetStatus: string) {
  const task = draggedTask.value
  dragOverCol.value = null
  draggedTask.value = null
  if (!task || task.status === targetStatus) return
  projectTaskApi.updateStatus(task.id, targetStatus).then(() => {
    message.success('已更新状态')
    loadBoard()
  }).catch(() => {
    message.error('更新状态失败')
  })
}

function loadMilestones() {
  if (!projectId.value) return
  milestoneLoading.value = true
  projectMilestoneApi.list(projectId.value).then((res: any) => {
    milestones.value = res?.data ?? res ?? []
  }).finally(() => { milestoneLoading.value = false })
}

function submitTask() {
  if (!taskForm.title?.trim() || !projectId.value) return
  projectTaskApi.create(projectId.value, { title: taskForm.title, status: taskForm.status, progress: taskForm.progress })
    .then(() => { showTaskForm.value = false; taskForm.title = ''; loadTasks(); loadBoard() })
}

function submitMilestone() {
  if (!milestoneForm.name?.trim() || !projectId.value) return
  const dueDate = milestoneForm.dueDateTs ? new Date(milestoneForm.dueDateTs).toISOString().slice(0, 10) : null
  projectMilestoneApi.create(projectId.value, { name: milestoneForm.name, dueDate })
    .then(() => { showMilestoneForm.value = false; milestoneForm.name = ''; milestoneForm.dueDateTs = null; loadMilestones() })
}

watch(projectId, () => { loadProject(); loadTasks(); loadBoard(); loadMilestones(); loadMembers(); loadGantt(); loadCalendarData() }, { immediate: true })
watch(showAddMember, (v) => { if (v) loadUserOptions() })
watch(calendarMonth, () => loadCalendarData())
onMounted(() => { loadProject(); loadTasks(); loadBoard(); loadMilestones(); loadMembers(); loadGantt(); loadCalendarData() })
</script>

<style scoped>
.project-detail { padding: 16px; }
.board-columns { display: flex; gap: 16px; overflow-x: auto; padding: 8px 0; }
.board-col { min-width: 240px; background: var(--n-color); border-radius: 8px; padding: 12px; transition: box-shadow 0.15s ease; }
.board-col.board-col-drag-over { box-shadow: inset 0 0 0 2px var(--n-primary-color); }
.board-col h4 { margin: 0 0 8px 0; font-size: 14px; }
.board-card { padding: 8px; background: var(--n-color-modal); border-radius: 6px; margin-bottom: 6px; font-size: 13px; cursor: grab; }
.board-card:active { cursor: grabbing; }
.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); gap: 1px; background: var(--n-border-color); border-radius: 6px; overflow: hidden; }
.cal-header { background: var(--n-color); padding: 6px; text-align: center; font-weight: 500; font-size: 12px; }
.cal-cell { background: var(--n-color-modal); min-height: 72px; padding: 4px 6px; }
.cal-cell.other { opacity: 0.4; }
.cal-cell.today .cal-day { color: var(--n-primary-color); font-weight: 700; }
.cal-day { font-size: 12px; margin-bottom: 2px; }
.cal-event { font-size: 11px; background: var(--n-primary-color); color: #fff; border-radius: 3px; padding: 1px 4px; margin-bottom: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>
