<template>
  <div class="automation-page">
    <div class="page-header">
      <div>
        <h2>自动化规则</h2>
        <p class="desc">配置自动化流程，让系统代替手工操作</p>
      </div>
      <n-button type="primary" @click="showCreateRule = true">
        <template #icon><n-icon><AddOutline /></n-icon></template>
        新建规则
      </n-button>
    </div>

    <n-empty v-if="rules.length === 0" description="暂无自动化规则，点击右上角新建" style="margin: 40px 0" />
    <div v-else class="rule-list">
      <div v-for="rule in rules" :key="rule.id" class="rule-card">
        <div class="rule-header">
          <div class="rule-title">
            <span class="rule-icon">{{ triggerIcon(rule.triggerType) }}</span>
            <span>{{ rule.name }}</span>
            <n-tag :type="rule.enabled ? 'success' : 'default'" size="small">
              {{ rule.enabled ? '启用' : '禁用' }}
            </n-tag>
          </div>
          <n-switch v-model:value="rule.enabled" @update:value="toggleRule(rule)" />
        </div>
        <div class="rule-desc">{{ rule.description }}</div>
        <div class="rule-meta">
          <div class="meta-item">
            <span class="meta-label">触发类型</span>
            <n-tag size="tiny">{{ triggerLabel(rule.triggerType) }}</n-tag>
          </div>
          <div class="meta-item">
            <span class="meta-label">动作</span>
            <n-tag size="tiny" type="info">{{ actionLabel(rule.actionType) }}</n-tag>
          </div>
          <div class="meta-item">
            <span class="meta-label">累计触发</span>
            <span>{{ rule.triggerCount || 0 }}次</span>
          </div>
          <div v-if="rule.lastTriggeredAt" class="meta-item">
            <span class="meta-label">上次触发</span>
            <span>{{ rule.lastTriggeredAt }}</span>
          </div>
        </div>
        <div class="rule-actions">
          <n-button size="small" @click="viewLogs(rule)">执行日志</n-button>
          <n-button size="small" type="error" @click="deleteRule(rule)">删除</n-button>
        </div>
      </div>
    </div>

    <!-- 创建规则弹窗 -->
    <n-modal v-model:show="showCreateRule" preset="card" title="新建规则" style="width: 600px">
      <n-form :model="ruleForm" label-placement="left" label-width="100">
        <n-form-item label="规则名称" required>
          <n-input v-model:value="ruleForm.name" placeholder="如：超期未跟进提醒" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="ruleForm.description" type="textarea" :rows="2" />
        </n-form-item>
        <n-form-item label="触发类型">
          <n-select v-model:value="ruleForm.triggerType" :options="triggerTypeOptions" />
        </n-form-item>
        <n-form-item label="动作类型">
          <n-select v-model:value="ruleForm.actionType" :options="actionTypeOptions" />
        </n-form-item>
        <n-form-item v-if="ruleForm.triggerType === 'time_based'" label="检查间隔(天)">
          <n-input-number v-model:value="ruleForm.intervalDays" :min="1" />
        </n-form-item>
        <n-form-item v-if="ruleForm.actionType === 'notify'" label="通知模板">
          <n-input v-model:value="ruleForm.notifyTemplate" type="textarea" :rows="2" placeholder="支持 {customer_name} 等变量" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreateRule = false">取消</n-button>
          <n-button type="primary" @click="submitRule" :disabled="!ruleForm.name">创建</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 执行日志弹窗 -->
    <n-modal v-model:show="showLogs" preset="card" title="执行日志" style="width: 700px">
      <n-data-table :columns="logColumns" :data="logs" :loading="logLoading" size="small" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NIcon, NTag, NSwitch, NModal, NForm, NFormItem, NInput, NInputNumber, NSelect, NSpace, NDataTable, useMessage, useDialog } from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

const message = useMessage()
const dialog = useDialog()
const rules = ref<any[]>([])
const logs = ref<any[]>([])
const logLoading = ref(false)
const showCreateRule = ref(false)
const showLogs = ref(false)

const ruleForm = reactive({
  name: '',
  description: '',
  triggerType: 'time_based',
  actionType: 'notify',
  intervalDays: 7,
  notifyTemplate: ''
})

const triggerTypeOptions = [
  { label: '定时触发', value: 'time_based' },
  { label: '事件触发', value: 'event_based' },
  { label: '条件触发', value: 'condition_based' }
]

const actionTypeOptions = [
  { label: '发送通知', value: 'notify' },
  { label: '自动分配', value: 'assign' },
  { label: '更新状态', value: 'update_status' },
  { label: '创建任务', value: 'create_task' }
]

function triggerIcon(type: string) {
  return { time_based: '⏰', event_based: '⚡', condition_based: '🔍' }[type] || '📋'
}
function triggerLabel(type: string) {
  return { time_based: '定时', event_based: '事件', condition_based: '条件' }[type] || type
}
function actionLabel(type: string) {
  return { notify: '通知', assign: '分配', update_status: '状态', create_task: '任务' }[type] || type
}

const logColumns = [
  { title: '时间', key: 'executedAt', width: 160 },
  { title: '结果', key: 'actionResult' },
  {
    title: '状态',
    key: 'status',
    width: 80,
    render(row: any) {
      return h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' },
        { default: () => row.status === 1 ? '成功' : '失败' })
    }
  }
]

async function loadRules() {
  try {
    const res = await request({ url: '/automation/rules', method: 'get' })
    rules.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ }
}

async function toggleRule(rule: any) {
  try {
    await request({ url: `/automation/rule/${rule.id}/toggle`, method: 'post', params: { enabled: rule.enabled } })
    message.success(`已${rule.enabled ? '启用' : '禁用'}`)
  } catch { message.error('操作失败') }
}

async function submitRule() {
  const data: any = {
    name: ruleForm.name,
    description: ruleForm.description,
    triggerType: ruleForm.triggerType,
    triggerConfig: ruleForm.triggerType === 'time_based'
      ? { interval_days: ruleForm.intervalDays, check_field: 'last_follow_time', target_table: 'customer' }
      : {},
    actionType: ruleForm.actionType,
    actionConfig: ruleForm.actionType === 'notify'
      ? { template: ruleForm.notifyTemplate || '自动化通知', channel: 'system' }
      : {}
  }
  try {
    await request({ url: '/automation/rule', method: 'post', data })
    message.success('创建成功')
    showCreateRule.value = false
    loadRules()
  } catch { message.error('创建失败') }
}

function deleteRule(rule: any) {
  dialog.warning({
    title: '删除规则',
    content: `确定删除规则「${rule.name}」？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await request({ url: `/automation/rule/${rule.id}`, method: 'delete' })
        message.success('已删除')
        loadRules()
      } catch { message.error('删除失败') }
    }
  })
}

async function viewLogs(rule: any) {
  showLogs.value = true
  logLoading.value = true
  try {
    const res = await request({ url: `/automation/rule/${rule.id}/logs`, method: 'get', params: { limit: 20 } })
    logs.value = Array.isArray(res) ? res : []
  } catch { /* ignore */ } finally {
    logLoading.value = false
  }
}

onMounted(loadRules)
</script>

<style scoped>
.automation-page { padding: 20px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0 0 4px; }
.desc { color: #999; font-size: 13px; margin: 0; }
.rule-list { display: flex; flex-direction: column; gap: 12px; }
.rule-card {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  padding: 16px;
}
.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.rule-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
}
.rule-icon { font-size: 20px; }
.rule-desc { color: #666; font-size: 13px; margin-bottom: 12px; }
.rule-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.meta-item { display: flex; align-items: center; gap: 4px; font-size: 13px; }
.meta-label { color: #999; }
.rule-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
</style>
