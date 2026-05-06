<template>
  <div class="automation-page">
    <n-grid :cols="24" :x-gap="16">
      <!-- 左侧：规则列表 -->
      <n-gi :span="16">
        <n-card title="自动化规则">
          <template #header-extra>
            <n-button type="primary" size="small" @click="openRuleForm">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              新建规则
            </n-button>
          </template>

          <n-form inline :model="ruleFilter" class="rule-filter">
            <n-form-item label="模块">
              <n-select v-model:value="ruleFilter.module" :options="moduleOptions" style="width: 150px" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="ruleFilter.enabled" :options="enabledOptions" style="width: 120px" clearable />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadRules">查询</n-button>
              <n-button @click="resetFilter">重置</n-button>
            </n-form-item>
          </n-form>

          <n-data-table
            :columns="ruleColumns"
            :data="rules"
            :loading="rulesLoading"
            :pagination="rulePagination"
          />
        </n-card>

        <n-card title="执行日志" style="margin-top: 16px">
          <n-form inline :model="logFilter">
            <n-form-item label="规则">
              <n-select v-model:value="logFilter.ruleId" :options="ruleSelectOptions" style="width: 200px" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="logFilter.result" :options="resultOptions" style="width: 120px" clearable />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadLogs">查询</n-button>
            </n-form-item>
          </n-form>

          <n-data-table
            :columns="logColumns"
            :data="logs"
            :loading="logsLoading"
            :pagination="logPagination"
          />
        </n-card>
      </n-gi>

      <!-- 右侧：规则详情与模板 -->
      <n-gi :span="8">
        <n-card title="规则模板">
          <n-list>
            <n-list-item v-for="template in ruleTemplates" :key="template.id" @click="useTemplate(template)" style="cursor: pointer">
              <n-thing :title="template.name" :description="template.description">
                <template #header-extra>
                  <n-tag size="small" :type="template.moduleType">{{ template.module }}</n-tag>
                </template>
              </n-thing>
            </n-list-item>
          </n-list>
        </n-card>

        <n-card title="触发事件类型" style="margin-top: 16px">
          <n-collapse>
            <n-collapse-item title="客户管理" name="customer">
              <n-tag v-for="e in customerEvents" :key="e.value" :value="e.value" style="margin: 4px">{{ e.label }}</n-tag>
            </n-collapse-item>
            <n-collapse-item title="会议日程" name="meeting">
              <n-tag v-for="e in meetingEvents" :key="e.value" :value="e.value" style="margin: 4px">{{ e.label }}</n-tag>
            </n-collapse-item>
            <n-collapse-item title="人事管理" name="hr">
              <n-tag v-for="e in hrEvents" :key="e.value" :value="e.value" style="margin: 4px">{{ e.label }}</n-tag>
            </n-collapse-item>
          </n-collapse>
        </n-card>

        <n-card title="可用动作" style="margin-top: 16px">
          <n-list>
            <n-list-item v-for="action in availableActions" :key="action.type">
              <n-thing :title="action.name" :description="action.description" />
            </n-list-item>
          </n-list>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 新建/编辑规则弹窗 -->
    <n-modal v-model:show="showRuleForm" preset="card" :title="formTitle" style="width: 800px">
      <n-form :model="ruleForm" label-placement="left" label-width="100">
        <n-grid :cols="2">
          <n-gi :span="2">
            <n-form-item label="规则名称" required>
              <n-input v-model:value="ruleForm.name" placeholder="请输入规则名称" />
            </n-form-item>
          </n-gi>
          <n-gi :span="2">
            <n-form-item label="规则描述">
              <n-input v-model:value="ruleForm.description" type="textarea" placeholder="规则描述" :rows="2" />
            </n-form-item>
          </n-gi>
          <n-gi :span="1">
            <n-form-item label="所属模块" required>
              <n-select v-model:value="ruleForm.module" :options="moduleOptions" placeholder="选择模块" />
            </n-form-item>
          </n-gi>
          <n-gi :span="1">
            <n-form-item label="触发事件" required>
              <n-select v-model:value="ruleForm.eventType" :options="getEventOptions(ruleForm.module)" placeholder="选择事件" />
            </n-form-item>
          </n-gi>
          <n-gi :span="1">
            <n-form-item label="条件类型">
              <n-select v-model:value="ruleForm.conditionType" :options="conditionTypeOptions" />
            </n-form-item>
          </n-gi>
          <n-gi :span="1">
            <n-form-item label="优先级">
              <n-input-number v-model:value="ruleForm.priority" :min="1" :max="1000" />
            </n-form-item>
          </n-gi>
          <n-gi :span="2">
            <n-form-item label="启用状态">
              <n-switch v-model:value="ruleForm.enabled" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>条件配置</n-divider>
        <n-space vertical>
          <div v-for="(cond, index) in ruleForm.conditions" :key="index" class="condition-row">
            <n-space>
              <n-select v-model:value="cond.field" :options="getFieldOptions(ruleForm.module)" style="width: 150px" placeholder="字段" />
              <n-select v-model:value="cond.operator" :options="operatorOptions" style="width: 120px" placeholder="操作符" />
              <n-input v-model:value="cond.value" placeholder="值" style="width: 200px" />
              <n-button @click="removeCondition(index)">删除</n-button>
            </n-space>
          </div>
          <n-button size="small" @click="addCondition">+ 添加条件</n-button>
        </n-space>

        <n-divider>动作配置</n-divider>
        <n-space vertical>
          <div v-for="(action, index) in ruleForm.actions" :key="index" class="action-row">
            <n-space>
              <n-select v-model:value="action.type" :options="actionTypeOptions" style="width: 180px" placeholder="动作类型" />
              <n-dynamic-input v-model:value="action.params" :min="1" placeholder="参数配置 (JSON)" />
              <n-button @click="removeAction(index)">删除</n-button>
            </n-space>
          </div>
          <n-button size="small" @click="addAction">+ 添加动作</n-button>
        </n-space>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showRuleForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitRule">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, NIcon, useMessage, type DataTableColumns } from 'naive-ui'
import { PlayOutline, StopOutline, CopyOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'
import { automationApi, type AutomationRuleDTO } from '@/api/automation'

/** 后端 DTO → 前端 Rule 视图模型适配 */
function adaptRule(dto: AutomationRuleDTO): Rule {
  const module = (dto.triggerConfig?.target_table as string) || 'custom'
  return {
    id: dto.id ?? 0,
    name: dto.name,
    description: dto.description ?? '',
    module,
    eventType: dto.triggerType,
    enabled: dto.enabled ?? true,
    priority: dto.priority ?? 100,
    executeCount: dto.triggerCount ?? 0,
    lastExecuteTime: dto.lastTriggeredAt ?? '',
    _raw: dto
  } as any
}

interface Rule {
  id: number
  name: string
  description: string
  module: string
  eventType: string
  enabled: boolean
  priority: number
  executeCount: number
  lastExecuteTime: string
}

interface Log {
  id: number
  ruleId: number
  ruleName: string
  executeResult: number
  errorMessage: string
  executeTime: string
  executeDuration: number
}

const message = useMessage()

const rules = ref<Rule[]>([])
const rulesLoading = ref(false)
const rulePagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })

const logs = ref<Log[]>([])
const logsLoading = ref(false)
const logPagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })

const ruleFilter = reactive({ module: '', enabled: null as boolean | null })
const logFilter = reactive({ ruleId: null as number | null, result: null as number | null })

const showRuleForm = ref(false)
const formTitle = ref('新建规则')
const submitting = ref(false)

const ruleForm = reactive({
  id: null as number | null,
  name: '',
  description: '',
  module: '',
  eventType: '',
  conditionType: 'all',
  conditions: [] as Array<{field: string; operator: string; value: any}>,
  actions: [] as Array<{type: string; params: any}>,
  enabled: true,
  priority: 100
})

const moduleOptions = [
  { label: '客户管理', value: 'customer' },
  { label: '会议日程', value: 'meeting' },
  { label: '人事管理', value: 'hr' },
  { label: '财务管理', value: 'finance' }
]

const enabledOptions = [
  { label: '启用', value: true },
  { label: '禁用', value: false }
]

const conditionTypeOptions = [
  { label: '满足所有条件', value: 'all' },
  { label: '满足任一条件', value: 'any' }
]

const operatorOptions = [
  { label: '等于', value: 'equals' },
  { label: '不等于', value: 'not_equals' },
  { label: '包含', value: 'contains' },
  { label: '大于', value: 'greater_than' },
  { label: '小于', value: 'less_than' },
  { label: '存在', value: 'exists' },
  { label: '不存在', value: 'not_exists' }
]

const actionTypeOptions = [
  { label: '分配用户', value: 'assign_user' },
  { label: '发送通知', value: 'send_notification' },
  { label: '创建任务', value: 'create_task' },
  { label: '更新字段', value: 'update_field' },
  { label: '调用 API', value: 'call_api' },
  { label: '发送邮件', value: 'send_email' },
  { label: '发送短信', value: 'send_sms' }
]

const resultOptions = [
  { label: '成功', value: 1 },
  { label: '失败', value: 0 }
]

type TagType = 'default' | 'primary' | 'info' | 'success' | 'warning' | 'error'

const ruleTemplates: Array<{
  id: number
  name: string
  description: string
  module: string
  moduleType: TagType
}> = [
  { id: 1, name: '新客户自动分配', description: '新客户报备后自动分配给销售负责人', module: '客户管理', moduleType: 'success' },
  { id: 2, name: '会议前提醒', description: '会议开始前发送提醒通知', module: '会议日程', moduleType: 'info' },
  { id: 3, name: '成交祝贺', description: '客户成交后发送祝贺通知', module: '客户管理', moduleType: 'success' },
  { id: 4, name: '合同到期提醒', description: '合同到期前 30 天提醒', module: '财务管理', moduleType: 'warning' }
]

const customerEvents = [
  { label: '客户创建', value: 'customer.created' },
  { label: '客户更新', value: 'customer.updated' },
  { label: '客户成交', value: 'customer.deal_closed' },
  { label: '客户流失', value: 'customer.lost' }
]

const meetingEvents = [
  { label: '会议创建', value: 'meeting.created' },
  { label: '会议更新', value: 'meeting.updated' },
  { label: '会议取消', value: 'meeting.cancelled' },
  { label: '会议提醒', value: 'meeting.reminder' }
]

const hrEvents = [
  { label: '员工入职', value: 'hr.employee_joined' },
  { label: '员工离职', value: 'hr.employee_left' },
  { label: '考核周期开始', value: 'hr.review_started' }
]

const availableActions = [
  { type: 'assign_user', name: '分配用户', description: '将记录分配给指定用户或角色' },
  { type: 'send_notification', name: '发送通知', description: '发送站内信、邮件或短信通知' },
  { type: 'create_task', name: '创建任务', description: '自动创建待办任务' },
  { type: 'update_field', name: '更新字段', description: '自动更新记录字段' },
  { type: 'call_api', name: '调用 API', description: '调用外部 API 接口' }
]

const ruleSelectOptions = ref<any[]>([])

const ruleColumns: DataTableColumns<Rule> = [
  { title: '规则名称', key: 'name', width: 200 },
  { title: '模块', key: 'module', width: 100, render: (row) => getModuleName(row.module) },
  { title: '触发事件', key: 'eventType', width: 150 },
  { title: '优先级', key: 'priority', width: 80 },
  { title: '执行次数', key: 'executeCount', width: 100 },
  {
    title: '状态', key: 'enabled', width: 80,
    render: (row) => h(NTag, { type: row.enabled ? 'success' : 'default' }, { default: () => row.enabled ? '启用' : '禁用' })
  },
  {
    title: '操作', key: 'action', width: 200,
    render: (row) => h(NSpace, null, { default: () => [
      h(NButton, { size: 'tiny', onClick: () => editRule(row) }, { default: () => '编辑' }),
      h(NButton, { size: 'tiny', onClick: () => toggleRule(row) }, { default: () => row.enabled ? '禁用' : '启用' }),
      h(NButton, { size: 'tiny', onClick: () => copyRule(row) }, { default: () => '复制' })
    ] })
  }
]

const logColumns: DataTableColumns<Log> = [
  { title: '规则', key: 'ruleName', width: 150 },
  { title: '执行时间', key: 'executeTime', width: 170 },
  { title: '耗时', key: 'executeDuration', width: 80, render: (row) => `${row.executeDuration}ms` },
  {
    title: '结果', key: 'executeResult', width: 80,
    render: (row) => h(NTag, { type: row.executeResult ? 'success' : 'error' }, { default: () => row.executeResult ? '成功' : '失败' })
  },
  { title: '错误信息', key: 'errorMessage', ellipsis: { tooltip: true } }
]

function getModuleName(module: string) {
  const map: Record<string, string> = { customer: '客户', meeting: '会议', hr: '人事', finance: '财务' }
  return map[module] || module
}

function getEventOptions(module: string) {
  const map: Record<string, any[]> = { customer: customerEvents, meeting: meetingEvents, hr: hrEvents }
  return map[module] || []
}

function getFieldOptions(module: string) {
  // 根据模块返回可用字段
  return [
    { label: '状态', value: 'status' },
    { label: '类型', value: 'type' },
    { label: '创建人', value: 'creator_id' },
    { label: '创建时间', value: 'create_time' }
  ]
}

function loadRules() {
  rulesLoading.value = true
  automationApi.listRules()
    .then((list) => {
      const arr = Array.isArray(list) ? list : []
      const mapped = arr.map(adaptRule)
      rules.value = ruleFilter.enabled === null
        ? mapped
        : mapped.filter((r) => r.enabled === ruleFilter.enabled)
      rulePagination.itemCount = rules.value.length
    })
    .catch(() => { rules.value = [] })
    .finally(() => { rulesLoading.value = false })
}

function loadLogs() {
  logsLoading.value = true
  const ruleId = logFilter.ruleId
  if (!ruleId) {
    logs.value = []
    logsLoading.value = false
    return
  }
  automationApi.getRuleLogs(ruleId, 50)
    .then((list) => {
      const arr = Array.isArray(list) ? list : []
      logs.value = arr.map((l) => ({
        id: l.id,
        ruleId: l.ruleId,
        ruleName: `#${l.ruleId}`,
        executeResult: l.status,
        errorMessage: l.status === 0 ? (l.actionResult ?? '') : '',
        executeTime: l.executedAt,
        executeDuration: 0
      }))
      logPagination.itemCount = logs.value.length
    })
    .catch(() => { logs.value = [] })
    .finally(() => { logsLoading.value = false })
}

function resetFilter() {
  ruleFilter.module = ''
  ruleFilter.enabled = null
  loadRules()
}

function openRuleForm() {
  formTitle.value = '新建规则'
  ruleForm.id = null
  ruleForm.name = ''
  ruleForm.description = ''
  ruleForm.module = ''
  ruleForm.eventType = ''
  ruleForm.conditions = []
  ruleForm.actions = []
  ruleForm.enabled = true
  ruleForm.priority = 100
  showRuleForm.value = true
}

function editRule(row: Rule) {
  formTitle.value = '编辑规则'
  ruleForm.id = row.id
  ruleForm.name = row.name
  ruleForm.description = row.description
  ruleForm.module = row.module
  ruleForm.eventType = row.eventType
  ruleForm.enabled = row.enabled
  ruleForm.priority = row.priority
  // 从原始 DTO 中还原条件/动作（actionConfig/triggerConfig → 表单项）
  const raw = (row as any)._raw as AutomationRuleDTO | undefined
  ruleForm.conditions = []
  ruleForm.actions = raw?.actionType
    ? [{ type: raw.actionType, params: raw.actionConfig ?? {} }]
    : []
  showRuleForm.value = true
}

function toggleRule(row: Rule) {
  const next = !row.enabled
  automationApi.toggleRule(row.id, next)
    .then(() => {
      message.success(next ? '规则已启用' : '规则已禁用')
      loadRules()
    })
    .catch(() => { message.error('切换失败') })
}

function copyRule(row: Rule) {
  const raw = (row as any)._raw as AutomationRuleDTO | undefined
  if (!raw) { message.error('规则数据缺失，无法复制'); return }
  const clone: AutomationRuleDTO = {
    name: row.name + ' - 副本',
    description: row.description,
    triggerType: raw.triggerType,
    triggerConfig: raw.triggerConfig,
    actionType: raw.actionType,
    actionConfig: raw.actionConfig,
    enabled: false,
    priority: raw.priority
  }
  automationApi.createRule(clone)
    .then(() => { message.success('规则已复制（默认停用，请确认后启用）'); loadRules() })
    .catch(() => { message.error('复制失败') })
}

function addCondition() {
  ruleForm.conditions.push({ field: '', operator: '', value: '' })
}

function removeCondition(index: number) {
  ruleForm.conditions.splice(index, 1)
}

function addAction() {
  ruleForm.actions.push({ type: '', params: {} })
}

function removeAction(index: number) {
  ruleForm.actions.splice(index, 1)
}

function submitRule() {
  if (!ruleForm.name || !ruleForm.module || !ruleForm.eventType) {
    message.warning('请填写必填项')
    return
  }
  submitting.value = true
  const firstAction = ruleForm.actions[0] ?? { type: 'notify', params: {} }
  const dto: AutomationRuleDTO = {
    id: ruleForm.id ?? undefined,
    name: ruleForm.name,
    description: ruleForm.description,
    triggerType: ruleForm.eventType,
    triggerConfig: { target_table: ruleForm.module },
    actionType: firstAction.type,
    actionConfig: firstAction.params,
    enabled: ruleForm.enabled,
    priority: ruleForm.priority
  }
  const call = dto.id
    ? automationApi.updateRule(dto)
    : automationApi.createRule(dto).then(() => undefined)
  call
    .then(() => {
      message.success('规则已保存')
      showRuleForm.value = false
      loadRules()
    })
    .catch(() => { message.error('保存失败') })
    .finally(() => { submitting.value = false })
}

function useTemplate(template: any) {
  openRuleForm()
  ruleForm.name = template.name
  ruleForm.description = template.description
  // 模板分组映射到 module
  const moduleMap: Record<string, string> = {
    '客户管理': 'customer', '会议日程': 'meeting', '人事管理': 'hr', '财务管理': 'finance'
  }
  ruleForm.module = moduleMap[template.module] ?? ''
  ruleForm.eventType = 'time_based'
  message.success(`已套用模板：${template.name}，请调整后保存`)
}

onMounted(() => {
  loadRules()
  loadLogs()
  ruleSelectOptions.value = moduleOptions
})
</script>

<style scoped>
.automation-page { padding: 20px; }
.rule-filter { margin-bottom: 16px; }
.condition-row, .action-row { margin-bottom: 8px; padding: 8px; background: #f5f5f5; border-radius: 6px; }
</style>
