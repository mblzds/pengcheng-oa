<template>
  <div class="performance-container">
    <n-card title="绩效考核管理">
      <n-tabs type="line" animated>
        <!-- 考核周期管理 -->
        <n-tab-pane name="periods" tab="考核周期">
          <n-space vertical>
            <div class="toolbar">
              <n-button type="primary" @click="openPeriodForm">新建周期</n-button>
              <n-button @click="loadPeriods">刷新</n-button>
            </div>
            <n-data-table
              :columns="periodColumns"
              :data="periods"
              :loading="periodsLoading"
              :pagination="periodPagination"
            />
          </n-space>
        </n-tab-pane>

        <!-- KPI 指标管理 -->
        <n-tab-pane name="kpi" tab="KPI 指标库">
          <n-space vertical>
            <div class="toolbar">
              <n-button type="primary" @click="openTemplateForm">新建指标</n-button>
              <n-button @click="loadTemplates">刷新</n-button>
            </div>
            <n-data-table
              :columns="templateColumns"
              :data="templates"
              :loading="templatesLoading"
              :pagination="templatePagination"
            />
          </n-space>
        </n-tab-pane>

        <!-- 考核评分 -->
        <n-tab-pane name="scores" tab="考核评分">
          <n-form inline class="score-filter">
            <n-form-item label="考核周期">
              <n-select
                v-model:value="scorePeriodId"
                :options="periodOptions"
                label-field="name"
                value-field="id"
                style="width: 200px"
              />
            </n-form-item>
            <n-form-item label="部门">
              <n-select
                v-model:value="scoreDeptId"
                :options="deptOptions"
                label-field="name"
                value-field="id"
                style="width: 200px"
                clearable
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadScores">查询</n-button>
              <n-button type="primary" @click="openBatchScore">批量评分</n-button>
            </n-form-item>
          </n-form>
          <n-data-table
            :columns="scoreColumns"
            :data="scores"
            :loading="scoresLoading"
            :pagination="scorePagination"
          />
        </n-tab-pane>

        <!-- 考核结果 -->
        <n-tab-pane name="results" tab="考核结果">
          <n-form inline class="result-filter">
            <n-form-item label="考核周期">
              <n-select
                v-model:value="resultPeriodId"
                :options="periodOptions"
                label-field="name"
                value-field="id"
                style="width: 200px"
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadResults">查询</n-button>
              <n-button @click="exportResults">导出结果</n-button>
            </n-form-item>
          </n-form>
          <n-data-table
            :columns="resultColumns"
            :data="results"
            :loading="resultsLoading"
            :pagination="resultPagination"
          />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- 新建考核周期 -->
    <n-modal v-model:show="showPeriodForm" title="新建考核周期" preset="card" style="width: 500px">
      <n-form :model="periodForm" label-placement="left" label-width="100">
        <n-form-item label="周期名称" required>
          <n-input v-model:value="periodForm.name" placeholder="如：2024 年 Q1 季度考核" />
        </n-form-item>
        <n-form-item label="周期类型" required>
          <n-select
            v-model:value="periodForm.periodType"
            :options="periodTypeOptions"
            label-field="label"
            value-field="value"
          />
        </n-form-item>
        <n-form-item label="考核年份" required>
          <n-input-number v-model:value="periodForm.year" :min="2020" :max="2030" style="width: 100%" />
        </n-form-item>
        <n-form-item label="开始日期" required>
          <n-date-picker v-model:formatted-value="periodForm.startDate" value-format="yyyy-MM-dd" style="width: 100%" />
        </n-form-item>
        <n-form-item label="结束日期" required>
          <n-date-picker v-model:formatted-value="periodForm.endDate" value-format="yyyy-MM-dd" style="width: 100%" />
        </n-form-item>
        <n-form-item label="目标分数" required>
          <n-input-number v-model:value="periodForm.targetScore" :min="0" :max="100" style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showPeriodForm = false">取消</n-button>
          <n-button type="primary" :loading="periodSubmitLoading" @click="submitPeriod">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 新建 KPI 指标 -->
    <n-modal v-model:show="showTemplateForm" title="新建 KPI 指标" preset="card" style="width: 520px">
      <n-form :model="templateForm" label-placement="left" label-width="100">
        <n-form-item label="指标名称" required>
          <n-input v-model:value="templateForm.name" placeholder="如：销售额达成率" />
        </n-form-item>
        <n-form-item label="指标编码" required>
          <n-input v-model:value="templateForm.code" placeholder="如：SALES_TARGET" />
        </n-form-item>
        <n-form-item label="指标分类" required>
          <n-select
            v-model:value="templateForm.category"
            :options="categoryOptions"
            label-field="label"
            value-field="value"
          />
        </n-form-item>
        <n-form-item label="权重 (%)" required>
          <n-input-number v-model:value="templateForm.weight" :min="1" :max="100" style="width: 100%" />
        </n-form-item>
        <n-form-item label="数据来源">
          <n-input v-model:value="templateForm.dataSource" placeholder="如：CRM 系统/手动填写" />
        </n-form-item>
        <n-form-item label="计算公式">
          <n-input v-model:value="templateForm.formula" type="textarea" placeholder="如：实际销售额/目标销售额*100" />
        </n-form-item>
        <n-form-item label="启用状态">
          <n-switch v-model:value="templateForm.enabled" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTemplateForm = false">取消</n-button>
          <n-button type="primary" :loading="templateSubmitLoading" @click="submitTemplate">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 批量评分 -->
    <n-modal v-model:show="showBatchScore" title="批量评分" preset="card" style="width: 800px">
      <n-form inline style="margin-bottom: 12px">
        <n-form-item label="考核周期" required>
          <n-select
            v-model:value="batchPeriodId"
            :options="periodOptions"
            label-field="name"
            value-field="id"
            style="width: 200px"
          />
        </n-form-item>
        <n-form-item label="部门" required>
          <n-select
            v-model:value="batchDeptId"
            :options="deptOptions"
            label-field="name"
            value-field="id"
            style="width: 200px"
          />
        </n-form-item>
        <n-form-item>
          <n-button type="primary" @click="loadBatchScoreUsers">加载人员</n-button>
        </n-form-item>
      </n-form>
      <n-data-table
        :columns="batchScoreColumns"
        :data="batchScoreUsers"
        :max-height="400"
        :pagination="batchScorePagination"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showBatchScore = false">取消</n-button>
          <n-button type="primary" :loading="batchScoreSubmitLoading" @click="submitBatchScore">提交评分</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NInput, NInputNumber, NSelect, NDatePicker, NSwitch, useMessage, type DataTableColumns } from 'naive-ui'
import { hrApi, type HrPeriod, type HrKpiTemplate, type HrKpiScore } from '@/api/hr'
import { deptApi } from '@/api/org'
import { userApi } from '@/api/system'

const message = useMessage()

// 考核周期
const periods = ref<HrPeriod[]>([])
const periodsLoading = ref(false)
const showPeriodForm = ref(false)
const periodSubmitLoading = ref(false)
const periodForm = ref<Partial<HrPeriod>>({
  name: '',
  periodType: 1,
  year: new Date().getFullYear(),
  startDate: '',
  endDate: '',
  targetScore: 80
})

const periodTypeOptions = [
  { label: '月度', value: 1 },
  { label: '季度', value: 2 },
  { label: '年度', value: 3 },
  { label: '半年度', value: 4 }
]

const periodColumns: DataTableColumns<HrPeriod> = [
  { title: '周期名称', key: 'name', width: 200 },
  {
    title: '类型', key: 'periodType', width: 80,
    render: (row) => {
      const map: Record<number, string> = { 1: '月度', 2: '季度', 3: '年度', 4: '半年度' }
      return h(NTag, { type: 'info', size: 'small' }, { default: () => map[row.periodType] || '-' })
    }
  },
  { title: '年份', key: 'year', width: 80 },
  { title: '开始日期', key: 'startDate', width: 120 },
  { title: '结束日期', key: 'endDate', width: 120 },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => {
      const map: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已结束' }
      const type: Record<number, 'default' | 'warning' | 'success'> = { 1: 'default', 2: 'warning', 3: 'success' }
      return h(NTag, { type: type[row.status] || 'default', size: 'small' }, { default: () => map[row.status] || '-' })
    }
  },
  {
    title: '操作', key: 'action', width: 150,
    render: (row) => h('div', { style: 'display: flex; gap: 8px' }, [
      h(NButton, { size: 'small', onClick: () => editPeriod(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: 'error', onClick: () => deletePeriod(row.id) }, { default: () => '删除' })
    ])
  }
]

const periodPagination = { pageSize: 10 }

// KPI 指标
const templates = ref<HrKpiTemplate[]>([])
const templatesLoading = ref(false)
const showTemplateForm = ref(false)
const templateSubmitLoading = ref(false)
const templateForm = ref<Partial<HrKpiTemplate>>({
  name: '',
  code: '',
  category: 1,
  weight: 10,
  dataSource: '',
  formula: '',
  enabled: true
})

const categoryOptions = [
  { label: '销售业绩', value: 1 },
  { label: '考勤', value: 2 },
  { label: '过程质量', value: 3 },
  { label: '综合能力', value: 4 },
  { label: '其他', value: 5 }
]

const templateColumns: DataTableColumns<HrKpiTemplate> = [
  { title: '指标名称', key: 'name', width: 180 },
  { title: '编码', key: 'code', width: 120 },
  {
    title: '分类', key: 'category', width: 100,
    render: (row) => {
      const map: Record<number, string> = { 1: '销售业绩', 2: '考勤', 3: '过程质量', 4: '综合能力', 5: '其他' }
      return h(NTag, { type: 'success', size: 'small' }, { default: () => map[row.category] || '-' })
    }
  },
  { title: '权重', key: 'weight', width: 80, render: (row) => `${row.weight}%` },
  { title: '数据来源', key: 'dataSource', width: 150 },
  {
    title: '启用', key: 'enabled', width: 80,
    render: (row) => h(NTag, { type: row.enabled ? 'success' : 'default', size: 'small' }, { default: () => row.enabled ? '是' : '否' })
  },
  {
    title: '操作', key: 'action', width: 150,
    render: (row) => h('div', { style: 'display: flex; gap: 8px' }, [
      h(NButton, { size: 'small', onClick: () => editTemplate(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'small', type: row.enabled ? 'warning' : 'primary', onClick: () => toggleTemplate(row) }, { default: () => row.enabled ? '禁用' : '启用' })
    ])
  }
]

const templatePagination = { pageSize: 10 }

// 考核评分
const scorePeriodId = ref<number | null>(null)
const scoreDeptId = ref<number | null>(null)
const scores = ref<any[]>([])
const scoresLoading = ref(false)
const scorePagination = { pageSize: 10 }

const scoreColumns: DataTableColumns<any> = [
  { title: '姓名', key: 'userName', width: 120 },
  { title: '部门', key: 'deptName', width: 120 },
  { title: '指标', key: 'kpiName', width: 150 },
  { title: '目标值', key: 'targetValue', width: 90 },
  { title: '实际值', key: 'actualValue', width: 90 },
  { title: '得分', key: 'score', width: 80 },
  { title: '加权得分', key: 'weightedScore', width: 90 },
  {
    title: '操作', key: 'action', width: 100,
    render: (row) => h(NButton, { size: 'small', onClick: () => editScore(row) }, { default: () => '修改' })
  }
]

// 考核结果
const resultPeriodId = ref<number | null>(null)
const results = ref<any[]>([])
const resultsLoading = ref(false)
const resultPagination = { pageSize: 10 }

const resultColumns: DataTableColumns<any> = [
  { title: '姓名', key: 'userName', width: 120 },
  { title: '部门', key: 'deptName', width: 120 },
  { title: '周期', key: 'periodName', width: 150 },
  { title: '总分', key: 'totalScore', width: 80 },
  { title: '等级', key: 'grade', width: 80, render: (row) => {
    const grade = row.grade || 'C'
    const type: Record<string, 'success' | 'warning' | 'error'> = { A: 'success', B: 'success', C: 'warning', D: 'error' }
    return h(NTag, { type: type[grade] || 'default', size: 'small' }, { default: () => grade })
  }},
  { title: '排名', key: 'rank', width: 80 },
  { title: '评语', key: 'comment', width: 200, ellipsis: { tooltip: true } }
]

// 批量评分
const showBatchScore = ref(false)
const batchPeriodId = ref<number | null>(null)
const batchDeptId = ref<number | null>(null)
const batchScoreUsers = ref<any[]>([])
const batchScoreSubmitLoading = ref(false)
const batchScorePagination = { pageSize: 10 }

const batchScoreColumns: DataTableColumns<any> = [
  { title: '姓名', key: 'userName', width: 120 },
  { title: '部门', key: 'deptName', width: 120 },
  { title: '总分', key: 'totalScore', width: 80, render: (row: any, index: number) => 
    h(NInputNumber, {
      value: row.totalScore,
      onUpdateValue: (v: number) => { batchScoreUsers.value[index].totalScore = v },
      min: 0, max: 100, style: { width: '80px' }
    })
  },
  { title: '等级', key: 'grade', width: 80, render: (row: any, index: number) => 
    h(NSelect, {
      value: row.grade,
      onUpdateValue: (v: string) => { batchScoreUsers.value[index].grade = v },
      options: [
        { label: 'A', value: 'A' },
        { label: 'B', value: 'B' },
        { label: 'C', value: 'C' },
        { label: 'D', value: 'D' }
      ],
      style: { width: '80px' }
    })
  },
  { title: '评语', key: 'comment', width: 200, render: (row: any, index: number) => 
    h(NInput, {
      value: row.comment,
      onUpdateValue: (v: string) => { batchScoreUsers.value[index].comment = v },
      placeholder: '请输入评语',
      style: { width: '180px' }
    })
  }
]

// 部门选项
const deptOptions = ref<any[]>([])
const periodOptions = ref<any[]>([])

// 加载数据
async function loadPeriods() {
  periodsLoading.value = true
  try {
    const res: any = await hrApi.periodPage({ pageNum: 1, pageSize: 50 })
    periods.value = res?.records || res?.data?.records || []
    periodOptions.value = periods.value.map((p: any) => ({ label: p.name, value: p.id }))
    scorePeriodId.value = periods.value[0]?.id || null
    resultPeriodId.value = periods.value[0]?.id || null
  } finally {
    periodsLoading.value = false
  }
}

async function loadTemplates() {
  templatesLoading.value = true
  try {
    const res: any = await hrApi.templateList()
    templates.value = res?.records || res?.data?.records || []
  } finally {
    templatesLoading.value = false
  }
}

async function loadScores() {
  if (!scorePeriodId.value) {
    message.warning('请选择考核周期')
    return
  }
  scoresLoading.value = true
  try {
    const res: any = await hrApi.scoreList({ periodId: scorePeriodId.value, deptId: scoreDeptId.value })
    scores.value = res?.records || res?.data?.records || []
  } finally {
    scoresLoading.value = false
  }
}

async function loadResults() {
  if (!resultPeriodId.value) {
    message.warning('请选择考核周期')
    return
  }
  resultsLoading.value = true
  try {
    const res: any = await hrApi.resultList({ periodId: resultPeriodId.value })
    results.value = res?.records || res?.data?.records || []
  } finally {
    resultsLoading.value = false
  }
}

async function loadDeptOptions() {
  try {
    const res: any = await deptApi.list()
    deptOptions.value = res?.list || res?.data?.list || []
  } catch {
    deptOptions.value = []
  }
}

// 表单操作
function openPeriodForm() {
  periodForm.value = {
    name: '',
    periodType: 1,
    year: new Date().getFullYear(),
    startDate: '',
    endDate: '',
    targetScore: 80
  }
  showPeriodForm.value = true
}

function editPeriod(row: HrPeriod) {
  periodForm.value = { ...row, id: row.id }
  showPeriodForm.value = true
}

async function submitPeriod() {
  if (!periodForm.value.name || !periodForm.value.periodType || !periodForm.value.year) {
    message.warning('请填写必填项')
    return
  }
  periodSubmitLoading.value = true
  try {
    if (periodForm.value.id) {
      await hrApi.updatePeriod(periodForm.value)
    } else {
      await hrApi.createPeriod(periodForm.value)
    }
    message.success('保存成功')
    showPeriodForm.value = false
    loadPeriods()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    periodSubmitLoading.value = false
  }
}

async function deletePeriod(id: number) {
  try {
    await hrApi.deletePeriod(id)
    message.success('删除成功')
    loadPeriods()
  } catch (e: any) {
    message.error(e?.message || '删除失败')
  }
}

function openTemplateForm() {
  templateForm.value = {
    name: '',
    code: '',
    category: 1,
    weight: 10,
    dataSource: '',
    formula: '',
    enabled: true
  }
  showTemplateForm.value = true
}

function editTemplate(row: HrKpiTemplate) {
  templateForm.value = { ...row, id: row.id }
  showTemplateForm.value = true
}

async function submitTemplate() {
  if (!templateForm.value.name || !templateForm.value.code) {
    message.warning('请填写必填项')
    return
  }
  templateSubmitLoading.value = true
  try {
    if (templateForm.value.id) {
      await hrApi.updateTemplate(templateForm.value)
    } else {
      await hrApi.createTemplate(templateForm.value)
    }
    message.success('保存成功')
    showTemplateForm.value = false
    loadTemplates()
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    templateSubmitLoading.value = false
  }
}

async function toggleTemplate(row: HrKpiTemplate) {
  try {
    await hrApi.updateTemplate({ ...row, enabled: !row.enabled })
    message.success('操作成功')
    loadTemplates()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  }
}

function editScore(row: any) {
  // 实现评分编辑逻辑
  message.info('编辑评分功能开发中')
}

async function loadBatchScoreUsers() {
  if (!batchPeriodId.value || !batchDeptId.value) {
    message.warning('请选择周期和部门')
    return
  }
  try {
    const res: any = await userApi.page({ page: 1, pageSize: 100, deptId: batchDeptId.value })
    batchScoreUsers.value = (res?.list || res?.data?.list || []).map((u: any) => ({
      userId: u.id,
      userName: u.nickname,
      deptName: u.deptName,
      totalScore: 80,
      grade: 'C',
      comment: ''
    }))
  } catch {
    batchScoreUsers.value = []
  }
}

async function submitBatchScore() {
  if (!batchPeriodId.value || batchScoreUsers.value.length === 0) {
    message.warning('没有可提交的数据')
    return
  }
  batchScoreSubmitLoading.value = true
  try {
    const scores = batchScoreUsers.value.map(u => ({
      userId: u.userId,
      periodId: batchPeriodId.value,
      totalScore: u.totalScore,
      grade: u.grade,
      comment: u.comment
    }))
    await hrApi.batchSaveResult(scores)
    message.success('提交成功')
    showBatchScore.value = false
    loadResults()
  } catch (e: any) {
    message.error(e?.message || '提交失败')
  } finally {
    batchScoreSubmitLoading.value = false
  }
}

function openBatchScore() {
  batchPeriodId.value = scorePeriodId.value
  batchDeptId.value = scoreDeptId.value
  batchScoreUsers.value = []
  showBatchScore.value = true
}

function exportResults() {
  message.info('导出功能开发中')
}

onMounted(() => {
  loadPeriods()
  loadTemplates()
  loadDeptOptions()
})
</script>

<style scoped>
.performance-container {
  padding: 20px;
}
.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.score-filter, .result-filter {
  margin-bottom: 12px;
}
</style>
