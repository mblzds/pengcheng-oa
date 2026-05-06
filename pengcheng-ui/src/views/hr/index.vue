<template>
  <div class="hr-container">
    <n-card>
      <n-tabs type="line" animated>
        <n-tab-pane name="profile" tab="人事档案">
          <n-space vertical>
            <n-form inline>
              <n-form-item label="选择用户">
                <n-select v-model:value="profileUserId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" />
              </n-form-item>
              <n-button type="primary" @click="loadProfile">查询档案</n-button>
              <n-button v-if="profile" type="default" @click="openProfileEdit">编辑</n-button>
            </n-form>
            <n-descriptions v-if="profile" :column="2" bordered label-placement="left">
              <n-descriptions-item label="工号">{{ profile.employeeNo || '-' }}</n-descriptions-item>
              <n-descriptions-item label="入职日期">{{ profile.joinDate || '-' }}</n-descriptions-item>
              <n-descriptions-item label="转正日期">{{ profile.formalDate || '-' }}</n-descriptions-item>
              <n-descriptions-item label="合同期限">{{ profile.contractStart }} ~ {{ profile.contractEnd }}</n-descriptions-item>
              <n-descriptions-item label="职级">{{ profile.jobLevel || '-' }}</n-descriptions-item>
              <n-descriptions-item label="工作地点">{{ profile.workLocation || '-' }}</n-descriptions-item>
              <n-descriptions-item label="紧急联系人" :span="2">{{ profile.emergencyContact }} {{ profile.emergencyPhone }}</n-descriptions-item>
            </n-descriptions>
            <n-empty v-else-if="profileQueried" description="暂无档案或请先选择用户查询" />
          </n-space>
        </n-tab-pane>

        <n-tab-pane name="changes" tab="人事异动">
          <n-space vertical>
            <n-button type="primary" size="small" @click="openChangeForm">发起异动</n-button>
            <n-data-table :columns="changeColumns" :data="changes" :loading="changesLoading" :pagination="changePagination" />
          </n-space>
        </n-tab-pane>

        <n-tab-pane name="periods" tab="考核周期">
          <n-data-table :columns="periodColumns" :data="periods" :loading="periodsLoading" />
          <n-button type="primary" size="small" style="margin-top: 8px" @click="loadPeriods">刷新</n-button>
        </n-tab-pane>

        <n-tab-pane name="templates" tab="KPI 指标">
          <n-data-table :columns="templateColumns" :data="templates" :loading="templatesLoading" />
        </n-tab-pane>

        <n-tab-pane name="scores" tab="绩效考核">
          <n-form inline>
            <n-form-item label="考核周期">
              <n-select v-model:value="scorePeriodId" :options="periodOptions" label-field="name" value-field="id" style="width: 200px" @update:value="loadScores" />
            </n-form-item>
            <n-form-item label="选择用户">
              <n-select v-model:value="scoreUserId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" clearable style="width: 200px" @update:value="loadScores" />
            </n-form-item>
            <n-button type="primary" quaternary @click="loadScores">查询</n-button>
            <n-button type="primary" size="small" @click="openBatchScoreForm">批量填写</n-button>
          </n-form>
          <n-data-table :columns="scoreColumns" :data="scoreTableData" :loading="scoresLoading" />
        </n-tab-pane>
      </n-tabs>
    </n-card>

    <!-- 编辑档案 -->
    <n-modal v-model:show="showProfileEdit" title="编辑档案" preset="card" style="width: 520px" @after-leave="profileForm.userId = null">
      <n-form :model="profileForm" label-placement="left" label-width="100">
        <n-form-item label="选择用户" required>
          <n-select v-model:value="profileForm.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" style="width: 100%" :disabled="!!profile?.id" />
        </n-form-item>
        <n-form-item label="工号"><n-input v-model:value="profileForm.employeeNo" placeholder="工号" /></n-form-item>
        <n-form-item label="入职日期"><n-input v-model:value="profileForm.joinDate" placeholder="YYYY-MM-DD" /></n-form-item>
        <n-form-item label="转正日期"><n-input v-model:value="profileForm.formalDate" placeholder="YYYY-MM-DD" /></n-form-item>
        <n-form-item label="合同开始"><n-input v-model:value="profileForm.contractStart" placeholder="YYYY-MM-DD" /></n-form-item>
        <n-form-item label="合同结束"><n-input v-model:value="profileForm.contractEnd" placeholder="YYYY-MM-DD" /></n-form-item>
        <n-form-item label="职级"><n-input v-model:value="profileForm.jobLevel" placeholder="职级" /></n-form-item>
        <n-form-item label="工作地点"><n-input v-model:value="profileForm.workLocation" placeholder="工作地点" /></n-form-item>
        <n-form-item label="紧急联系人"><n-input v-model:value="profileForm.emergencyContact" placeholder="紧急联系人" /></n-form-item>
        <n-form-item label="紧急联系电话"><n-input v-model:value="profileForm.emergencyPhone" placeholder="电话" /></n-form-item>
        <n-form-item label="备注"><n-input v-model:value="profileForm.remark" type="textarea" placeholder="备注" /></n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showProfileEdit = false">取消</n-button>
          <n-button type="primary" :loading="profileSaveLoading" @click="submitProfile">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 批量填写考核 -->
    <n-modal v-model:show="showBatchScoreForm" title="批量填写考核" preset="card" style="width: 680px" @after-leave="resetBatchScoreForm">
      <n-form inline style="margin-bottom: 12px">
        <n-form-item label="考核周期" required>
          <n-select v-model:value="batchScorePeriodId" :options="periodOptions" label-field="name" value-field="id" style="width: 200px" placeholder="选择周期" />
        </n-form-item>
        <n-form-item label="选择用户" required>
          <n-select v-model:value="batchScoreUserId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" style="width: 200px" />
        </n-form-item>
        <n-button type="default" size="small" @click="fillBatchScoreRows">生成表格</n-button>
      </n-form>
      <n-data-table :columns="batchScoreColumns" :data="batchScoreRows" :max-height="320" />
      <template #footer>
        <n-space justify="end">
          <n-button @click="showBatchScoreForm = false">取消</n-button>
          <n-button type="primary" :loading="batchScoreSubmitLoading" @click="submitBatchScore">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 发起异动 -->
    <n-modal v-model:show="showChangeForm" title="发起异动" preset="card" style="width: 480px" @after-leave="resetChangeForm">
      <n-form :model="changeForm" label-placement="left" label-width="90">
        <n-form-item label="选择用户" required>
          <n-select v-model:value="changeForm.userId" :options="userOptions" label-field="nickname" value-field="id" filterable placeholder="选择用户" style="width: 100%" />
        </n-form-item>
        <n-form-item label="异动类型" required>
          <n-select v-model:value="changeForm.changeType" :options="changeTypeOptions" label-field="label" value-field="value" placeholder="选择类型" />
        </n-form-item>
        <n-form-item label="异动日期" required>
          <n-input v-model:value="changeForm.changeDate" placeholder="YYYY-MM-DD" />
        </n-form-item>
        <n-form-item label="原因">
          <n-input v-model:value="changeForm.reason" type="textarea" placeholder="异动原因" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showChangeForm = false">取消</n-button>
          <n-button type="primary" :loading="changeSubmitLoading" @click="submitChange">提交</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted, nextTick } from 'vue'
import { NButton, NInput, NInputNumber, NTag, useMessage } from 'naive-ui'
import { userApi } from '@/api/system'
import { hrProfileApi, hrChangeApi, hrKpiPeriodApi, hrKpiTemplateApi, hrKpiScoreApi } from '@/api/hr'

const message = useMessage()
const userOptions = ref<any[]>([])
const profileUserId = ref<number | null>(null)
const profile = ref<any>(null)
const profileQueried = ref(false)
const changes = ref<any[]>([])
const changesLoading = ref(false)
const showChangeForm = ref(false)
const changeForm = ref<{ userId?: number; changeType?: number; changeDate?: string; reason?: string }>({})
const changeTypeOptions = [{ label: '入职', value: 1 }, { label: '离职', value: 2 }, { label: '调岗', value: 3 }, { label: '调薪', value: 4 }, { label: '其他', value: 5 }]
const changeSubmitLoading = ref(false)

const showProfileEdit = ref(false)
const profileForm = ref<any>({ userId: null, employeeNo: '', joinDate: '', formalDate: '', contractStart: '', contractEnd: '', jobLevel: '', workLocation: '', emergencyContact: '', emergencyPhone: '', remark: '' })
const profileSaveLoading = ref(false)
const periods = ref<any[]>([])
const periodsLoading = ref(false)
const periodOptions = ref<any[]>([])
const templates = ref<any[]>([])
const templatesLoading = ref(false)
const scorePeriodId = ref<number | null>(null)
const scoreUserId = ref<number | null>(null)
const scoreTableData = ref<any[]>([])
const scoresLoading = ref(false)

const changeTypeMap: Record<number, string> = { 1: '入职', 2: '离职', 3: '调岗', 4: '调薪', 5: '其他' }
const changeColumns = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '用户ID', key: 'userId', width: 80 },
  { title: '异动类型', key: 'changeType', width: 90, render: (row: any) => changeTypeMap[row.changeType] ?? row.changeType },
  { title: '异动日期', key: 'changeDate', width: 110 },
  { title: '原因', key: 'reason', ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 80, render: (row: any) => row.status === 2 ? h(NTag, { type: 'success', size: 'small' }, { default: () => '已生效' }) : h(NTag, { size: 'small' }, { default: () => '草稿' }) },
  { title: '操作', key: 'action', width: 90, render: (row: any) => row.status === 1 ? h(NButton, { size: 'small', onClick: () => setChangeEffective(row.id) }, { default: () => '生效' }) : null },
]
const changePagination = { pageSize: 10 }

const periodTypeMap: Record<number, string> = { 1: '月度', 2: '季度', 3: '年度' }
const periodStatusMap: Record<number, string> = { 1: '未开始', 2: '考核中', 3: '已结束' }
const periodColumns = [
  { title: '名称', key: 'name', width: 180 },
  { title: '类型', key: 'periodType', width: 80, render: (row: any) => periodTypeMap[row.periodType] ?? '' },
  { title: '年', key: 'year', width: 70 },
  { title: '开始', key: 'startDate', width: 110 },
  { title: '结束', key: 'endDate', width: 110 },
  { title: '状态', key: 'status', width: 80, render: (row: any) => periodStatusMap[row.status] ?? '' },
]

const categoryMap: Record<number, string> = { 1: '销售业绩', 2: '考勤', 3: '过程质量', 4: '综合' }
const templateColumns = [
  { title: '名称', key: 'name', width: 140 },
  { title: '编码', key: 'code', width: 120 },
  { title: '分类', key: 'category', width: 90, render: (row: any) => categoryMap[row.category] ?? '' },
  { title: '权重', key: 'weight', width: 80 },
  { title: '数据来源', key: 'dataSource', width: 120 },
]

/** 考核记录列：按周期+用户查出的 KpiScore 列表，展示指标名/实际值/得分/加权分 */
function getScoreColumns() {
  return [
    { title: '指标', key: 'templateId', width: 120, render: (row: any) => templateNameMap.value[row.templateId] ?? row.templateId },
    { title: '目标值', key: 'targetValue', width: 90 },
    { title: '实际值', key: 'actualValue', width: 90 },
    { title: '得分', key: 'score', width: 80 },
    { title: '加权得分', key: 'weightedScore', width: 90 },
    { title: '备注', key: 'remark', ellipsis: { tooltip: true } },
  ]
}
const scoreColumns = getScoreColumns()
const templateNameMap = ref<Record<number, string>>({})

// 批量填写考核
const showBatchScoreForm = ref(false)
const batchScorePeriodId = ref<number | null>(null)
const batchScoreUserId = ref<number | null>(null)
const batchScoreRows = ref<any[]>([])
const batchScoreSubmitLoading = ref(false)
const batchScoreColumns = [
  { title: '指标', key: 'templateId', width: 120, render: (row: any) => templateNameMap.value[row.templateId] ?? row.templateId },
  { title: '目标值', key: 'targetValue', width: 100, render: (row: any) => h(NInputNumber, { value: row.targetValue, onUpdateValue: (v: number) => { row.targetValue = v }, size: 'small', placeholder: '目标', style: { width: '90px' } }) },
  { title: '实际值', key: 'actualValue', width: 100, render: (row: any) => h(NInputNumber, { value: row.actualValue, onUpdateValue: (v: number) => { row.actualValue = v }, size: 'small', placeholder: '实际', style: { width: '90px' } }) },
  { title: '得分', key: 'score', width: 90, render: (row: any) => h(NInputNumber, { value: row.score, onUpdateValue: (v: number) => { row.score = v }, size: 'small', placeholder: '得分', min: 0, max: 100, style: { width: '80px' } }) },
  { title: '备注', key: 'remark', width: 140, render: (row: any) => h(NInput, { value: row.remark, onUpdateValue: (v: string) => { row.remark = v }, size: 'small', placeholder: '备注', style: { width: '130px' } }) },
]

async function loadProfile() {
  if (!profileUserId.value) { message.warning('请输入用户ID'); return }
  try {
    const res: any = await hrProfileApi.get(profileUserId.value)
    profile.value = res.data ?? res
    profileQueried.value = true
  } catch {
    profile.value = null
    profileQueried.value = true
  }
}

async function loadChanges() {
  changesLoading.value = true
  try {
    const res: any = await hrChangeApi.page({ pageNum: 1, pageSize: 20 })
    const data = res.data ?? res
    changes.value = data.records ?? data ?? []
  } finally {
    changesLoading.value = false
  }
}

async function setChangeEffective(id: number) {
  try {
    await hrChangeApi.setEffective(id)
    message.success('已生效')
    loadChanges()
  } catch (e: any) {
    message.error(e?.message ?? '操作失败')
  }
}

function openChangeForm() {
  changeForm.value = { changeType: 1, changeDate: new Date().toISOString().slice(0, 10) }
  showChangeForm.value = true
}
function resetChangeForm() {
  changeForm.value = {}
}
async function submitChange() {
  if (changeForm.value.userId == null || changeForm.value.changeType == null || !changeForm.value.changeDate) {
    message.warning('请填写用户ID、异动类型和异动日期')
    return
  }
  changeSubmitLoading.value = true
  try {
    await hrChangeApi.create({
      userId: changeForm.value.userId,
      changeType: changeForm.value.changeType,
      changeDate: changeForm.value.changeDate,
      reason: changeForm.value.reason ?? '',
      status: 1,
    })
    message.success('已提交')
    showChangeForm.value = false
    loadChanges()
  } catch (e: any) {
    message.error(e?.message ?? '提交失败')
  } finally {
    changeSubmitLoading.value = false
  }
}

function openProfileEdit() {
  if (!profile.value) return
  profileForm.value = {
    id: profile.value.id,
    userId: profile.value.userId,
    employeeNo: profile.value.employeeNo ?? '',
    joinDate: profile.value.joinDate ?? '',
    formalDate: profile.value.formalDate ?? '',
    contractStart: profile.value.contractStart ?? '',
    contractEnd: profile.value.contractEnd ?? '',
    jobLevel: profile.value.jobLevel ?? '',
    workLocation: profile.value.workLocation ?? '',
    emergencyContact: profile.value.emergencyContact ?? '',
    emergencyPhone: profile.value.emergencyPhone ?? '',
    remark: profile.value.remark ?? '',
  }
  showProfileEdit.value = true
}
async function submitProfile() {
  if (profileForm.value.userId == null) {
    message.warning('请填写用户ID')
    return
  }
  profileSaveLoading.value = true
  try {
    await hrProfileApi.save(profileForm.value)
    message.success('已保存')
    showProfileEdit.value = false
    if (profileUserId.value === profileForm.value.userId) loadProfile()
  } catch (e: any) {
    message.error(e?.message ?? '保存失败')
  } finally {
    profileSaveLoading.value = false
  }
}

async function loadPeriods() {
  periodsLoading.value = true
  try {
    const res: any = await hrKpiPeriodApi.page({ pageNum: 1, pageSize: 50 })
    const data = res.data ?? res
    periods.value = data.records ?? data ?? []
    periodOptions.value = periods.value.map((p: any) => ({ id: p.id, name: p.name }))
    if (periods.value.length && !scorePeriodId.value) scorePeriodId.value = periods.value[0].id
  } finally {
    periodsLoading.value = false
  }
}

async function loadTemplates() {
  templatesLoading.value = true
  try {
    const res: any = await hrKpiTemplateApi.listEnabled()
    templates.value = res.data ?? res ?? []
    templateNameMap.value = Object.fromEntries((templates.value as any[]).map((t: any) => [t.id, t.name]))
  } finally {
    templatesLoading.value = false
  }
}

async function loadScores() {
  if (!scorePeriodId.value || !scoreUserId.value) return
  scoresLoading.value = true
  try {
    const res: any = await hrKpiScoreApi.list(scorePeriodId.value, scoreUserId.value)
    scoreTableData.value = res.data ?? res ?? []
  } finally {
    scoresLoading.value = false
  }
}

function openBatchScoreForm() {
  batchScorePeriodId.value = scorePeriodId.value
  batchScoreUserId.value = scoreUserId.value
  batchScoreRows.value = []
  showBatchScoreForm.value = true
  nextTick(() => {
    if (batchScorePeriodId.value && batchScoreUserId.value && (templates.value as any[])?.length) fillBatchScoreRows()
  })
}
async function fillBatchScoreRows() {
  if (!batchScorePeriodId.value || !batchScoreUserId.value) {
    message.warning('请选择考核周期并填写用户ID')
    return
  }
  const list = templates.value as any[]
  if (!list?.length) {
    message.warning('暂无启用的 KPI 指标，请先在 KPI 指标 Tab 确认有启用模板')
    return
  }
  const existing = (scorePeriodId.value === batchScorePeriodId.value && scoreUserId.value === batchScoreUserId.value)
    ? (scoreTableData.value as any[] || [])
    : []
  const byTemplate = Object.fromEntries((existing as any[]).map((s: any) => [s.templateId, s]))
  batchScoreRows.value = list.map((t: any) => {
    const prev = byTemplate[t.id]
    return {
      templateId: t.id,
      targetValue: prev?.targetValue ?? null,
      actualValue: prev?.actualValue ?? null,
      score: prev?.score ?? null,
      remark: prev?.remark ?? '',
    }
  })
  // 按 data_source 自动拉取建议值并合并（覆盖未填的 actualValue）
  try {
    const res: any = await hrKpiScoreApi.suggest(batchScorePeriodId.value, batchScoreUserId.value)
    const suggestMap = res?.data ?? res ?? {}
    batchScoreRows.value = batchScoreRows.value.map((r: any) => {
      const suggested = suggestMap[r.templateId] ?? suggestMap[String(r.templateId)]
      if (suggested != null && (r.actualValue == null || r.actualValue === '')) {
        return { ...r, actualValue: Number(suggested) }
      }
      return r
    })
  } catch (_) { /* 忽略建议接口失败 */ }
}
function resetBatchScoreForm() {
  batchScoreRows.value = []
  batchScorePeriodId.value = null
  batchScoreUserId.value = null
}
async function submitBatchScore() {
  if (!batchScorePeriodId.value || !batchScoreUserId.value) {
    message.warning('请选择考核周期并填写用户ID')
    return
  }
  if (!batchScoreRows.value.length) {
    message.warning('请先点击「生成表格」')
    return
  }
  const scores = batchScoreRows.value.map((r: any) => ({
    templateId: r.templateId,
    targetValue: r.targetValue ?? undefined,
    actualValue: r.actualValue ?? undefined,
    score: r.score ?? undefined,
    remark: r.remark ?? undefined,
  }))
  batchScoreSubmitLoading.value = true
  try {
    await hrKpiScoreApi.batchFill(batchScorePeriodId.value, batchScoreUserId.value, scores)
    message.success('已保存')
    showBatchScoreForm.value = false
    if (scorePeriodId.value === batchScorePeriodId.value && scoreUserId.value === batchScoreUserId.value) loadScores()
  } catch (e: any) {
    message.error(e?.message ?? '保存失败')
  } finally {
    batchScoreSubmitLoading.value = false
  }
}

function loadUserOptions() {
  userApi.page({ page: 1, pageSize: 500 }).then((res: any) => {
    const list = res?.list ?? res?.data?.records ?? res?.records ?? []
    userOptions.value = Array.isArray(list) ? list : []
  }).catch(() => { userOptions.value = [] })
}

onMounted(() => {
  loadUserOptions()
  loadChanges()
  loadPeriods()
  loadTemplates()
})
</script>

<style scoped>
.hr-container { padding: 16px; }
</style>
