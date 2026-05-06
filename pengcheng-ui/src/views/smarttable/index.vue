<template>
  <div class="smart-table-page">
    <!-- 左侧表格列表 -->
    <div class="table-sidebar">
      <div class="sidebar-header">
        <h3>智能表格</h3>
        <n-dropdown :options="createOptions" @select="handleCreate">
          <n-button type="primary" size="small">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            新建
          </n-button>
        </n-dropdown>
      </div>
      <div class="table-list">
        <div
          v-for="table in tables"
          :key="table.id"
          class="table-item"
          :class="{ active: currentTable?.id === table.id }"
          @click="selectTable(table)"
        >
          <span class="table-icon">{{ table.icon || '📊' }}</span>
          <div class="table-info">
            <div class="table-name">{{ table.name }}</div>
            <div class="table-meta">{{ table.recordCount || 0 }}条记录</div>
          </div>
          <n-dropdown trigger="click" :options="tableActions" @select="(key: string) => handleTableAction(key, table)">
            <n-button text size="small" class="table-more" @click.stop>
              <n-icon><EllipsisHorizontalOutline /></n-icon>
            </n-button>
          </n-dropdown>
        </div>
        <n-empty v-if="tables.length === 0" description="暂无表格，点击新建" size="small" />
      </div>
    </div>

    <!-- 右侧表格内容 -->
    <div class="table-content" v-if="currentTable">
      <!-- 工具栏 -->
      <div class="content-toolbar">
        <div class="toolbar-left">
          <h3>{{ currentTable.name }}</h3>
          <n-tag size="small" type="info">{{ currentTable.visibility === 'all' ? '公开' : currentTable.visibility === 'dept' ? '部门' : '私有' }}</n-tag>
        </div>
        <div class="toolbar-right">
          <!-- 视图切换 -->
          <n-radio-group v-model:value="currentViewType" size="small">
            <n-radio-button value="grid">表格</n-radio-button>
            <n-radio-button value="kanban">看板</n-radio-button>
            <n-radio-button value="gantt">甘特图</n-radio-button>
            <n-radio-button value="calendar">日历</n-radio-button>
          </n-radio-group>
          <n-button size="small" @click="showFieldManager = true">
            <template #icon><n-icon><SettingsOutline /></n-icon></template>
            字段
          </n-button>
          <n-button size="small" type="primary" @click="addRecord">
            <template #icon><n-icon><AddOutline /></n-icon></template>
            添加记录
          </n-button>
        </div>
      </div>

      <!-- 筛选排序栏 -->
      <div class="filter-sort-bar" v-if="currentViewType === 'grid'">
        <n-select
          v-model:value="sortField"
          placeholder="排序字段"
          :options="sortFieldOptions"
          clearable
          size="small"
          style="width: 140px"
          @update:value="applySort"
        />
        <n-button-group size="small" v-if="sortField">
          <n-button :type="sortOrder === 'asc' ? 'primary' : 'default'" @click="sortOrder = 'asc'; applySort()">升序</n-button>
          <n-button :type="sortOrder === 'desc' ? 'primary' : 'default'" @click="sortOrder = 'desc'; applySort()">降序</n-button>
        </n-button-group>
        <n-divider vertical />
        <n-select
          v-model:value="filterField"
          placeholder="筛选字段"
          :options="sortFieldOptions"
          clearable
          size="small"
          style="width: 140px"
        />
        <n-input
          v-if="filterField"
          v-model:value="filterValue"
          placeholder="筛选值"
          clearable
          size="small"
          style="width: 150px"
          @keyup.enter="applyFilter"
        />
        <n-button v-if="filterField" size="small" type="primary" @click="applyFilter">筛选</n-button>
        <n-button v-if="filterField || sortField" size="small" @click="clearFilterSort">清除</n-button>
      </div>

      <!-- 表格视图 -->
      <div class="grid-view" v-if="currentViewType === 'grid'">
        <n-data-table
          :columns="tableColumns"
          :data="displayRecords"
          :loading="recordLoading"
          :row-key="(row: any) => row.id"
          :max-height="600"
          :scroll-x="scrollX"
          striped
          @update:checked-row-keys="handleCheck"
        />
        <div class="table-footer">
          <n-pagination
            v-model:page="pagination.page"
            :page-size="pagination.pageSize"
            :item-count="pagination.total"
            show-quick-jumper
            @update:page="loadRecords"
          />
        </div>
      </div>

      <!-- 看板视图 -->
      <div class="kanban-view" v-else-if="currentViewType === 'kanban'">
        <div v-if="!kanbanGroupField" class="kanban-empty">
          <n-empty description="请选择分组字段">
            <template #extra>
              <n-select
                v-model:value="kanbanGroupFieldKey"
                placeholder="选择用于分组的字段"
                :options="kanbanFieldOptions"
                style="width: 200px"
              />
            </template>
          </n-empty>
        </div>
        <div v-else class="kanban-board">
          <div
            v-for="col in kanbanColumns"
            :key="col.value"
            class="kanban-column"
            @dragover.prevent
            @drop="onKanbanDrop($event, col.value)"
          >
            <div class="kanban-col-header">
              <span class="kanban-col-title">{{ col.label }}</span>
              <n-tag size="tiny" round>{{ col.cards.length }}</n-tag>
            </div>
            <div class="kanban-cards">
              <div
                v-for="card in col.cards"
                :key="card.id"
                class="kanban-card"
                draggable="true"
                @dragstart="onKanbanDragStart($event, card)"
              >
                <div class="card-title">{{ getCardTitle(card) }}</div>
                <div class="card-meta">
                  <span v-for="f in kanbanDisplayFields" :key="f.fieldKey" class="card-field">
                    <span class="card-field-label">{{ f.name }}:</span>
                    {{ card.data?.[f.fieldKey] || '-' }}
                  </span>
                </div>
              </div>
              <div
                v-if="col.cards.length === 0"
                class="kanban-placeholder"
              >
                拖拽卡片到此处
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 甘特图视图 -->
      <div class="gantt-view" v-else-if="currentViewType === 'gantt'">
        <div class="gantt-config" v-if="!ganttStartField || !ganttEndField">
          <n-empty description="请选择开始和结束日期字段">
            <template #extra>
              <n-space>
                <n-select v-model:value="ganttStartField" :options="dateFieldOptions" placeholder="开始日期" size="small" style="width: 160px" />
                <n-select v-model:value="ganttEndField" :options="dateFieldOptions" placeholder="结束日期" size="small" style="width: 160px" />
              </n-space>
            </template>
          </n-empty>
        </div>
        <div v-else ref="ganttChartRef" class="gantt-chart-box"></div>
      </div>

      <!-- 日历视图 -->
      <div class="calendar-grid-view" v-else-if="currentViewType === 'calendar'">
        <div class="cal-config" v-if="!calendarDateField">
          <n-empty description="请选择日期字段">
            <template #extra>
              <n-select v-model:value="calendarDateField" :options="dateFieldOptions" placeholder="日期字段" size="small" style="width: 180px" />
            </template>
          </n-empty>
        </div>
        <div v-else class="cal-board">
          <div class="cal-header">
            <n-button size="small" quaternary @click="calMonth--">&lt;</n-button>
            <span class="cal-title">{{ calYear }}年{{ calMonth + 1 }}月</span>
            <n-button size="small" quaternary @click="calMonth++">></n-button>
          </div>
          <div class="cal-weekdays">
            <div v-for="d in ['一','二','三','四','五','六','日']" :key="d" class="cal-wk">{{ d }}</div>
          </div>
          <div class="cal-days">
            <div v-for="(day, i) in calendarDays" :key="i" :class="['cal-day', { 'other-month': !day.currentMonth, today: day.isToday }]">
              <div class="day-num">{{ day.day }}</div>
              <div v-for="evt in day.events" :key="evt.id" class="cal-event" :title="getCardTitle(evt)">
                {{ getCardTitle(evt) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="table-content" v-else>
      <n-empty description="选择一个表格或新建表格" />
    </div>

    <!-- 字段管理抽屉 -->
    <n-drawer v-model:show="showFieldManager" :width="400" placement="right">
      <n-drawer-content title="字段管理" :native-scrollbar="false">
        <div class="field-list">
          <div v-for="field in fields" :key="field.id" class="field-item">
            <div class="field-info">
              <span class="field-type-badge">{{ fieldTypeLabel(field.fieldType) }}</span>
              <span class="field-name">{{ field.name }}</span>
              <n-tag v-if="field.required" size="tiny" type="error">必填</n-tag>
            </div>
            <div class="field-actions">
              <n-button text size="small" @click="editField(field)">编辑</n-button>
              <n-button text size="small" type="error" @click="deleteFieldConfirm(field)">删除</n-button>
            </div>
          </div>
        </div>
        <n-button block dashed @click="showAddField = true" style="margin-top: 16px">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          添加字段
        </n-button>
      </n-drawer-content>
    </n-drawer>

    <!-- 创建表格弹窗 -->
    <n-modal v-model:show="showCreateModal" preset="card" title="新建表格" style="width: 500px">
      <n-form :model="createForm" label-placement="left" label-width="80">
        <n-form-item label="名称" required>
          <n-input v-model:value="createForm.name" placeholder="请输入表格名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="createForm.description" type="textarea" :rows="2" placeholder="可选描述" />
        </n-form-item>
        <n-form-item label="可见范围">
          <n-radio-group v-model:value="createForm.visibility">
            <n-radio value="private">仅自己</n-radio>
            <n-radio value="dept">本部门</n-radio>
            <n-radio value="all">全部</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateModal = false">取消</n-button>
          <n-button type="primary" @click="submitCreate" :disabled="!createForm.name?.trim()">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 模板选择弹窗 -->
    <n-modal v-model:show="showTemplateModal" preset="card" title="从模板创建" style="width: 700px">
      <div class="template-grid">
        <div
          v-for="tpl in templates"
          :key="tpl.id"
          class="template-card"
          @click="selectTemplate(tpl)"
        >
          <div class="template-icon">{{ tpl.icon || '📋' }}</div>
          <div class="template-name">{{ tpl.name }}</div>
          <div class="template-desc">{{ tpl.description }}</div>
          <div class="template-meta">{{ tpl.usageCount || 0 }}次使用</div>
        </div>
      </div>
    </n-modal>

    <!-- 添加字段弹窗 -->
    <n-modal v-model:show="showAddField" preset="card" :title="editingField ? '编辑字段' : '添加字段'" style="width: 450px">
      <n-form :model="fieldForm" label-placement="left" label-width="80">
        <n-form-item label="字段名" required>
          <n-input v-model:value="fieldForm.name" placeholder="如：客户姓名" />
        </n-form-item>
        <n-form-item label="字段标识" required>
          <n-input v-model:value="fieldForm.fieldKey" placeholder="如：customer_name" />
        </n-form-item>
        <n-form-item label="字段类型">
          <n-select v-model:value="fieldForm.fieldType" :options="fieldTypeOptions" />
        </n-form-item>
        <n-form-item label="必填">
          <n-switch v-model:value="fieldForm.required" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showAddField = false">取消</n-button>
          <n-button type="primary" @click="submitField">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, h, nextTick } from 'vue'
import {
  NButton, NButtonGroup, NIcon, NDropdown, NEmpty, NDataTable, NPagination,
  NModal, NForm, NFormItem, NInput, NInputNumber, NRadioGroup, NRadio, NRadioButton,
  NTag, NSpace, NDrawer, NDrawerContent, NSelect, NSwitch, NCheckbox,
  NDatePicker, NRate, NProgress, NDivider, useMessage, useDialog
} from 'naive-ui'
import { AddOutline, EllipsisHorizontalOutline, SettingsOutline } from '@vicons/ionicons5'
import { smartTableApi } from '@/api/smartTable'

const message = useMessage()
const dialog = useDialog()

const tables = ref<any[]>([])
const currentTable = ref<any>(null)
const fields = ref<any[]>([])
const records = ref<any[]>([])
const templates = ref<any[]>([])
const recordLoading = ref(false)
const currentViewType = ref('grid')
const checkedRowKeys = ref<number[]>([])

const showCreateModal = ref(false)
const showTemplateModal = ref(false)
const showFieldManager = ref(false)
const showAddField = ref(false)
const editingField = ref<any>(null)

const createForm = reactive({ name: '', description: '', visibility: 'private' })
const fieldForm = reactive({ name: '', fieldKey: '', fieldType: 'text', required: false })
const pagination = reactive({ page: 1, pageSize: 50, total: 0 })

const sortField = ref<string | null>(null)
const sortOrder = ref<'asc' | 'desc'>('asc')
const filterField = ref<string | null>(null)
const filterValue = ref('')
const editingCell = ref<{ recordId: number; fieldKey: string } | null>(null)
const editingCellValue = ref<any>(null)

const fieldTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '单选', value: 'select' },
  { label: '多选', value: 'multi_select' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '复选框', value: 'checkbox' },
  { label: '链接', value: 'url' },
  { label: '邮箱', value: 'email' },
  { label: '电话', value: 'phone' },
  { label: '评分', value: 'rating' },
  { label: '进度', value: 'progress' },
  { label: '成员', value: 'member' },
  { label: '附件', value: 'attachment' }
]

const createOptions = [
  { label: '空白表格', key: 'blank' },
  { label: '从模板创建', key: 'template' }
]

const tableActions = [
  { label: '重命名', key: 'rename' },
  { label: '删除', key: 'delete' }
]

function fieldTypeLabel(type: string) {
  return fieldTypeOptions.find(o => o.value === type)?.label || type
}

const scrollX = computed(() => {
  return Math.max(fields.value.length * 150 + 100, 800)
})

const sortFieldOptions = computed(() =>
  fields.value.filter(f => !f.hidden).map(f => ({ label: f.name, value: f.fieldKey }))
)

const displayRecords = computed(() => {
  let result = [...records.value]

  if (filterField.value && filterValue.value) {
    const fk = filterField.value
    const fv = filterValue.value.toLowerCase()
    result = result.filter(r => {
      const v = r.data?.[fk]
      if (v === null || v === undefined) return false
      return String(v).toLowerCase().includes(fv)
    })
  }

  if (sortField.value) {
    const sk = sortField.value
    const dir = sortOrder.value === 'asc' ? 1 : -1
    result.sort((a, b) => {
      const va = a.data?.[sk] ?? ''
      const vb = b.data?.[sk] ?? ''
      if (typeof va === 'number' && typeof vb === 'number') return (va - vb) * dir
      return String(va).localeCompare(String(vb)) * dir
    })
  }

  return result
})

function applySort() { /* computed 会自动更新 */ }

function applyFilter() { /* computed 会自动更新 */ }

function clearFilterSort() {
  sortField.value = null
  sortOrder.value = 'asc'
  filterField.value = null
  filterValue.value = ''
}

// ==================== 看板视图 ====================

const kanbanGroupFieldKey = ref<string | null>(null)
let draggedCard: any = null

// 甘特图
const ganttStartField = ref<string | null>(null)
const ganttEndField = ref<string | null>(null)
const ganttChartRef = ref<HTMLDivElement>()
let ganttChart: any = null

const dateFieldOptions = computed(() =>
  fields.value
    .filter(f => f.fieldType === 'date' || f.fieldType === 'datetime')
    .map(f => ({ label: f.name, value: f.fieldKey }))
)

// 日历视图
const calendarDateField = ref<string | null>(null)
const calYear = ref(new Date().getFullYear())
const calMonth = ref(new Date().getMonth())

const calendarDays = computed(() => {
  if (!calendarDateField.value) return []
  const first = new Date(calYear.value, calMonth.value, 1)
  const last = new Date(calYear.value, calMonth.value + 1, 0)
  const startDay = (first.getDay() + 6) % 7
  const days: any[] = []
  const today = new Date().toDateString()

  for (let i = startDay - 1; i >= 0; i--) {
    const d = new Date(calYear.value, calMonth.value, -i)
    days.push({ day: d.getDate(), date: d, currentMonth: false, isToday: false, events: [] })
  }
  for (let i = 1; i <= last.getDate(); i++) {
    const d = new Date(calYear.value, calMonth.value, i)
    const dateStr = d.toISOString().split('T')[0]
    const events = records.value.filter(r => {
      const val = r.data?.[calendarDateField.value!]
      return val && String(val).startsWith(dateStr)
    })
    days.push({ day: i, date: d, currentMonth: true, isToday: d.toDateString() === today, events })
  }
  const remaining = 42 - days.length
  for (let i = 1; i <= remaining; i++) {
    const d = new Date(calYear.value, calMonth.value + 1, i)
    days.push({ day: d.getDate(), date: d, currentMonth: false, isToday: false, events: [] })
  }
  return days
})

const kanbanFieldOptions = computed(() =>
  fields.value
    .filter(f => ['select', 'multi_select', 'text'].includes(f.fieldType))
    .map(f => ({ label: f.name, value: f.fieldKey }))
)

const kanbanGroupField = computed(() => {
  if (!kanbanGroupFieldKey.value) return null
  return fields.value.find(f => f.fieldKey === kanbanGroupFieldKey.value) || null
})

const kanbanDisplayFields = computed(() =>
  fields.value.filter(f => !f.hidden && f.fieldKey !== kanbanGroupFieldKey.value).slice(0, 3)
)

const kanbanColumns = computed(() => {
  const gf = kanbanGroupField.value
  if (!gf) return []

  const groupValues: string[] = Array.isArray(gf.options) && gf.options.length > 0
    ? gf.options
    : [...new Set(records.value.map(r => r.data?.[gf.fieldKey]).filter(Boolean))]

  if (groupValues.length === 0) groupValues.push('未分组')

  return groupValues.map(val => ({
    label: String(val),
    value: String(val),
    cards: records.value.filter(r => {
      const rv = r.data?.[gf.fieldKey]
      return String(rv || '未分组') === String(val)
    })
  }))
})

function getCardTitle(card: any) {
  const firstText = fields.value.find(f => f.fieldType === 'text' && !f.hidden)
  if (firstText) return card.data?.[firstText.fieldKey] || `#${card.id}`
  return `#${card.id}`
}

function onKanbanDragStart(event: DragEvent, card: any) {
  draggedCard = card
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(card.id))
  }
}

async function onKanbanDrop(_event: DragEvent, targetValue: string) {
  if (!draggedCard || !kanbanGroupFieldKey.value) return
  const fk = kanbanGroupFieldKey.value
  const oldVal = draggedCard.data?.[fk]
  if (String(oldVal) === targetValue) { draggedCard = null; return }

  const newData = { ...draggedCard.data, [fk]: targetValue }
  try {
    await smartTableApi.updateRecord(draggedCard.id, newData)
    draggedCard.data = newData
    message.success('已移动')
  } catch {
    message.error('移动失败')
  }
  draggedCard = null
}

// ========== 甘特图渲染 ==========
watch([ganttStartField, ganttEndField, () => records.value], () => {
  if (currentViewType.value === 'gantt' && ganttStartField.value && ganttEndField.value) {
    nextTick(() => renderGantt())
  }
})
watch(currentViewType, (val) => {
  if (val === 'gantt' && ganttStartField.value && ganttEndField.value) {
    nextTick(() => renderGantt())
  }
})

function renderGantt() {
  if (!ganttChartRef.value) return
  const echarts = (window as any).echarts
  if (!echarts) return

  if (!ganttChart) {
    ganttChart = echarts.init(ganttChartRef.value)
  }

  const titleField = fields.value.find(f => f.fieldType === 'text')
  const items = records.value
    .filter(r => r.data?.[ganttStartField.value!] && r.data?.[ganttEndField.value!])
    .map(r => ({
      name: titleField ? (r.data?.[titleField.fieldKey] || 'Record ' + r.id) : 'Record ' + r.id,
      start: new Date(r.data[ganttStartField.value!]).getTime(),
      end: new Date(r.data[ganttEndField.value!]).getTime()
    }))
    .filter(item => !isNaN(item.start) && !isNaN(item.end))

  if (items.length === 0) {
    ganttChart.setOption({ title: { text: '暂无数据（需要记录包含有效的开始和结束日期）', left: 'center', top: 'center' }, series: [] })
    return
  }

  const categories = items.map(i => i.name)

  ganttChart.setOption({
    tooltip: { formatter: (p: any) => p.name + '<br>' + new Date(p.value[1]).toLocaleDateString() + ' ~ ' + new Date(p.value[2]).toLocaleDateString() },
    grid: { left: 180, right: 40, top: 30, bottom: 30 },
    xAxis: { type: 'time', position: 'top' },
    yAxis: { type: 'category', data: categories, inverse: true, axisLabel: { width: 160, overflow: 'truncate' } },
    series: [{
      type: 'custom',
      renderItem: (_params: any, api: any) => {
        const catIdx = api.value(0)
        const start = api.coord([api.value(1), catIdx])
        const end = api.coord([api.value(2), catIdx])
        const height = api.size([0, 1])[1] * 0.6
        return { type: 'rect', shape: { x: start[0], y: start[1] - height / 2, width: end[0] - start[0], height }, style: api.style() }
      },
      encode: { x: [1, 2], y: 0 },
      data: items.map((item, i) => ({
        value: [i, item.start, item.end],
        name: item.name,
        itemStyle: { color: ['#2080f0', '#18a058', '#f0a020', '#d03050', '#722ed1', '#13c2c2'][i % 6] }
      }))
    }]
  })

  ganttChart.resize()
  window.addEventListener('resize', () => ganttChart?.resize())
}

function isEditing(recordId: number, fieldKey: string) {
  return editingCell.value?.recordId === recordId && editingCell.value?.fieldKey === fieldKey
}

function startEdit(recordId: number, fieldKey: string, currentValue: any) {
  editingCell.value = { recordId, fieldKey }
  editingCellValue.value = currentValue ?? ''
}

async function commitEdit(row: any, fieldKey: string) {
  if (!editingCell.value) return
  const newData = { ...row.data, [fieldKey]: editingCellValue.value }
  editingCell.value = null
  try {
    await smartTableApi.updateRecord(row.id, newData)
    row.data = newData
  } catch {
    message.error('保存失败')
  }
}

function cancelEdit() {
  editingCell.value = null
}

function renderCellEditor(row: any, field: any) {
  const fieldKey = field.fieldKey
  const val = row.data?.[fieldKey]
  const rid = row.id
  const editing = isEditing(rid, fieldKey)

  if (editing) {
    return renderFieldEditor(row, field)
  }

  return h('div', {
    class: 'cell-display',
    onDblclick: () => startEdit(rid, fieldKey, val)
  }, [renderFieldDisplay(val, field)])
}

function renderFieldDisplay(val: any, field: any): any {
  if (val === undefined || val === null || val === '') return h('span', { class: 'cell-empty' }, '-')

  switch (field.fieldType) {
    case 'checkbox':
      return h(NCheckbox, { checked: !!val, disabled: true })
    case 'rating':
      return h(NRate, { value: Number(val), readonly: true, size: 'small' })
    case 'progress':
      return h(NProgress, { percentage: Number(val), showIndicator: true, style: 'width: 100px' })
    case 'url':
      return h('a', { href: val, target: '_blank', class: 'cell-link' }, val)
    case 'select':
      return h(NTag, { size: 'small', type: 'info' }, { default: () => val })
    case 'multi_select':
      if (Array.isArray(val)) {
        return h(NSpace, { size: 4 }, { default: () => val.map((v: string) => h(NTag, { size: 'tiny' }, { default: () => v })) })
      }
      return String(val)
    default:
      return String(val)
  }
}

function renderFieldEditor(row: any, field: any) {
  const onBlur = () => commitEdit(row, field.fieldKey)
  const onKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) commitEdit(row, field.fieldKey)
    if (e.key === 'Escape') cancelEdit()
  }
  const common = { size: 'small' as const, autofocus: true, onBlur, onKeydown }

  switch (field.fieldType) {
    case 'number':
      return h(NInputNumber, { ...common, value: editingCellValue.value, onUpdateValue: (v: any) => editingCellValue.value = v, style: 'width: 100%' })
    case 'checkbox':
      return h(NCheckbox, { checked: !!editingCellValue.value, onUpdateChecked: (v: boolean) => { editingCellValue.value = v; commitEdit(row, field.fieldKey) } })
    case 'date':
    case 'datetime':
      return h(NDatePicker, { ...common, type: field.fieldType === 'datetime' ? 'datetime' : 'date', value: editingCellValue.value ? new Date(editingCellValue.value).getTime() : null, onUpdateValue: (v: any) => { editingCellValue.value = v ? new Date(v).toISOString() : ''; commitEdit(row, field.fieldKey) } })
    case 'rating':
      return h(NRate, { value: Number(editingCellValue.value) || 0, onUpdateValue: (v: number) => { editingCellValue.value = v; commitEdit(row, field.fieldKey) }, size: 'small' })
    case 'select':
      return h(NSelect, {
        ...common,
        value: editingCellValue.value,
        options: (field.options || []).map((o: string) => ({ label: o, value: o })),
        onUpdateValue: (v: any) => { editingCellValue.value = v; commitEdit(row, field.fieldKey) }
      })
    case 'progress':
      return h(NInputNumber, { ...common, value: editingCellValue.value, min: 0, max: 100, onUpdateValue: (v: any) => editingCellValue.value = v, style: 'width: 100%' })
    default:
      return h(NInput, { ...common, value: editingCellValue.value, onUpdateValue: (v: string) => editingCellValue.value = v })
  }
}

const tableColumns = computed(() => {
  const cols: any[] = [
    { type: 'selection', width: 40 }
  ]
  for (const field of fields.value) {
    if (field.hidden) continue
    cols.push({
      title: field.name,
      key: `data.${field.fieldKey}`,
      width: field.width || 150,
      render(row: any) {
        return renderCellEditor(row, field)
      }
    })
  }
  cols.push({
    title: '操作',
    key: 'actions',
    width: 100,
    fixed: 'right' as const,
    render(row: any) {
      return h(NSpace, null, {
        default: () => [
          h(NButton, { text: true, size: 'small', type: 'error', onClick: () => deleteRecordConfirm(row) }, { default: () => '删除' })
        ]
      })
    }
  })
  return cols
})

// ==================== 生命周期 ====================

onMounted(() => {
  loadTables()
})

// ==================== 数据加载 ====================

async function loadTables() {
  try {
    const res: any = await smartTableApi.listTables()
    tables.value = Array.isArray(res) ? res : []
    if (tables.value.length > 0 && !currentTable.value) {
      selectTable(tables.value[0])
    }
  } catch { /* ignore */ }
}

async function selectTable(table: any) {
  currentTable.value = table
  await Promise.all([loadFields(), loadRecords()])
}

async function loadFields() {
  if (!currentTable.value) return
  try {
    const res: any = await smartTableApi.listFields(currentTable.value.id)
    fields.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ }
}

async function loadRecords() {
  if (!currentTable.value) return
  recordLoading.value = true
  try {
    const res: any = await smartTableApi.listRecords(currentTable.value.id, pagination.page, pagination.pageSize)
    records.value = res?.list || (Array.isArray(res) ? res : [])
    pagination.total = res?.total || 0
  } finally {
    recordLoading.value = false
  }
}

// ==================== 表格操作 ====================

function handleCreate(key: string) {
  if (key === 'blank') {
    Object.assign(createForm, { name: '', description: '', visibility: 'private' })
    showCreateModal.value = true
  } else {
    loadTemplates()
    showTemplateModal.value = true
  }
}

async function submitCreate() {
  try {
    const res: any = await smartTableApi.createTable(createForm)
    message.success('创建成功')
    showCreateModal.value = false
    await loadTables()
    if (res) selectTable(res)
  } catch { message.error('创建失败') }
}

async function loadTemplates() {
  try {
    const res: any = await smartTableApi.listTemplates()
    templates.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ }
}

async function selectTemplate(tpl: any) {
  const name = prompt('请输入表格名称:', tpl.name + ' - 副本')
  if (!name) return
  try {
    const res: any = await smartTableApi.createFromTemplate(tpl.id, name)
    message.success('创建成功')
    showTemplateModal.value = false
    await loadTables()
    if (res) selectTable(res)
  } catch { message.error('创建失败') }
}

function handleTableAction(key: string, table: any) {
  if (key === 'rename') {
    const name = prompt('请输入新名称:', table.name)
    if (name && name !== table.name) {
      smartTableApi.updateTable({ id: table.id, name }).then(() => {
        message.success('修改成功')
        table.name = name
      })
    }
  } else if (key === 'delete') {
    dialog.warning({
      title: '删除表格',
      content: `确定删除「${table.name}」？删除后数据不可恢复。`,
      positiveText: '删除',
      negativeText: '取消',
      onPositiveClick: async () => {
        await smartTableApi.deleteTable(table.id)
        message.success('已删除')
        if (currentTable.value?.id === table.id) currentTable.value = null
        await loadTables()
      }
    })
  }
}

// ==================== 记录操作 ====================

async function addRecord() {
  const data: Record<string, any> = {}
  for (const f of fields.value) {
    data[f.fieldKey] = f.defaultValue || null
  }
  try {
    await smartTableApi.addRecord(currentTable.value.id, data)
    await loadRecords()
  } catch { message.error('添加失败') }
}

function handleCheck(keys: number[]) {
  checkedRowKeys.value = keys
}

function deleteRecordConfirm(row: any) {
  dialog.warning({
    title: '删除记录',
    content: '确定删除此记录？',
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await smartTableApi.deleteRecord(row.id)
      await loadRecords()
      message.success('已删除')
    }
  })
}

// ==================== 字段操作 ====================

function editField(field: any) {
  editingField.value = field
  Object.assign(fieldForm, {
    name: field.name,
    fieldKey: field.fieldKey,
    fieldType: field.fieldType,
    required: field.required
  })
  showAddField.value = true
}

async function submitField() {
  if (editingField.value) {
    try {
      await smartTableApi.updateField(currentTable.value.id, { ...editingField.value, ...fieldForm })
      message.success('修改成功')
      showAddField.value = false
      editingField.value = null
      await loadFields()
    } catch { message.error('修改失败') }
  } else {
    try {
      await smartTableApi.addField(currentTable.value.id, fieldForm)
      message.success('添加成功')
      showAddField.value = false
      await loadFields()
    } catch { message.error('添加失败') }
  }
}

function deleteFieldConfirm(field: any) {
  dialog.warning({
    title: '删除字段',
    content: `确定删除字段「${field.name}」？所有记录中的该字段数据将丢失。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await smartTableApi.deleteField(currentTable.value.id, field.id)
      message.success('已删除')
      await loadFields()
    }
  })
}
</script>

<style scoped>
.smart-table-page {
  display: flex;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
.table-sidebar {
  width: 260px;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
}
.table-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.table-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  gap: 10px;
  margin-bottom: 4px;
}
.table-item:hover {
  background: #f5f5f5;
}
.table-item.active {
  background: rgba(24, 160, 88, 0.08);
}
.table-icon {
  font-size: 20px;
}
.table-info {
  flex: 1;
  min-width: 0;
}
.table-name {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.table-meta {
  font-size: 12px;
  color: #999;
}
.table-more {
  opacity: 0;
}
.table-item:hover .table-more {
  opacity: 1;
}
.table-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.content-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar-left h3 {
  margin: 0;
  font-size: 16px;
}
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.grid-view {
  flex: 1;
  padding: 16px 20px;
  overflow: auto;
}
.table-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
}
.kanban-view {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.field-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.field-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
}
.field-info {
  display: flex;
  align-items: center;
  gap: 8px;
}
.field-type-badge {
  font-size: 11px;
  padding: 2px 6px;
  background: #f0f0f0;
  border-radius: 3px;
  color: #666;
}
.field-actions {
  display: flex;
  gap: 4px;
}
.template-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.template-card {
  padding: 16px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  cursor: pointer;
  text-align: center;
  transition: all 0.2s;
}
.template-card:hover {
  border-color: #18a058;
  box-shadow: 0 2px 8px rgba(24, 160, 88, 0.15);
}
.template-icon {
  font-size: 32px;
  margin-bottom: 8px;
}
.template-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}
.template-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}
.template-meta {
  font-size: 11px;
  color: #999;
}
.filter-sort-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 20px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  flex-wrap: wrap;
}
.cell-display {
  cursor: text;
  min-height: 22px;
  padding: 2px 0;
}
.cell-display:hover {
  background: rgba(24, 160, 88, 0.05);
  border-radius: 3px;
}
.cell-empty {
  color: #ccc;
}
.cell-link {
  color: #18a058;
  text-decoration: none;
}
.cell-link:hover {
  text-decoration: underline;
}
.kanban-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.kanban-board {
  display: flex;
  gap: 16px;
  padding: 16px 20px;
  overflow-x: auto;
  height: 100%;
}
.kanban-column {
  min-width: 280px;
  max-width: 320px;
  flex-shrink: 0;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}
.kanban-col-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  font-weight: 600;
  font-size: 14px;
  border-bottom: 2px solid #e0e0e0;
}
.kanban-cards {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
  min-height: 100px;
}
.kanban-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  cursor: grab;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: box-shadow 0.2s;
}
.kanban-card:hover {
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.12);
}
.kanban-card:active {
  cursor: grabbing;
  opacity: 0.7;
}
.card-title {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.card-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.card-field {
  font-size: 12px;
  color: #666;
}
.card-field-label {
  color: #999;
}
.kanban-placeholder {
  text-align: center;
  color: #ccc;
  font-size: 13px;
  padding: 20px;
  border: 2px dashed #e8e8e8;
  border-radius: 6px;
}
/* 甘特图 */
.gantt-view { flex: 1; overflow: hidden; }
.gantt-config { display: flex; align-items: center; justify-content: center; height: 300px; }
.gantt-chart-box { width: 100%; height: calc(100vh - 260px); }
/* 日历视图 */
.calendar-grid-view { flex: 1; overflow-y: auto; }
.cal-config { display: flex; align-items: center; justify-content: center; height: 300px; }
.cal-board { padding: 0 16px; }
.cal-header { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 12px 0; }
.cal-title { font-size: 16px; font-weight: 600; }
.cal-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); text-align: center; font-size: 13px; color: #999; padding: 8px 0; border-bottom: 1px solid #e8e8e8; }
.cal-days { display: grid; grid-template-columns: repeat(7, 1fr); }
.cal-day { min-height: 90px; border: 1px solid #f0f0f0; padding: 4px 6px; font-size: 13px; }
.cal-day.other-month { background: #fafafa; color: #ccc; }
.cal-day.today { background: #f0fdf4; }
.day-num { font-weight: 500; margin-bottom: 4px; }
.cal-event { font-size: 11px; padding: 2px 4px; background: #e6f4ff; border-radius: 3px; margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; cursor: pointer; }
.cal-event:hover { background: #bae0ff; }
</style>
