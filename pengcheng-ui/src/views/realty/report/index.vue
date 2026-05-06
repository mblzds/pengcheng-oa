<template>
  <div class="report-container">
    <n-card title="AI 工作日报">
      <template #header-extra>
        <n-space>
          <n-date-picker v-model:value="selectedDate" type="date" size="small" />
          <n-button type="primary" size="small" :loading="generating" @click="generateReport">
            生成日报
          </n-button>
        </n-space>
      </template>

      <!-- 当前日报 -->
      <div v-if="currentReport" class="report-detail">
        <div class="report-date">{{ currentReport.reportDate }} 工作日报</div>

        <n-grid :cols="4" :x-gap="12" :y-gap="12" style="margin: 16px 0">
          <n-gi>
            <div class="stat-card blue">
              <div class="stat-value">{{ currentReport.customerFollowUp || 0 }}</div>
              <div class="stat-label">跟进客户</div>
            </div>
          </n-gi>
          <n-gi>
            <div class="stat-card green">
              <div class="stat-value">{{ currentReport.newCustomers || 0 }}</div>
              <div class="stat-label">新增客户</div>
            </div>
          </n-gi>
          <n-gi>
            <div class="stat-card orange">
              <div class="stat-value">{{ currentReport.dealCount || 0 }}</div>
              <div class="stat-label">签约单数</div>
            </div>
          </n-gi>
          <n-gi>
            <div class="stat-card purple">
              <div class="stat-value">{{ currentReport.todoCompleted || 0 }}/{{ (currentReport.todoCompleted || 0) + (currentReport.todoPending || 0) }}</div>
              <div class="stat-label">待办完成</div>
            </div>
          </n-gi>
        </n-grid>

        <n-card size="small" title="日报摘要" style="margin-bottom: 12px">
          <div class="report-summary">
            <div v-for="(line, i) in summaryLines" :key="i" class="summary-line">{{ line }}</div>
          </div>
        </n-card>

        <n-card v-if="currentReport.aiSuggestions" size="small" title="AI 建议">
          <div class="ai-suggestions">{{ currentReport.aiSuggestions }}</div>
        </n-card>
      </div>

      <n-empty v-else description="选择日期并点击生成日报" />
    </n-card>

    <!-- 历史日报列表 -->
    <n-card title="历史日报" style="margin-top: 16px">
      <n-data-table :columns="reportColumns" :data="reportHistory" :max-height="400" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import { NCard, NGrid, NGi, NButton, NSpace, NDatePicker, NDataTable, NEmpty, NTag, useMessage } from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()

const selectedDate = ref<number>(Date.now())
const currentReport = ref<any>(null)
const reportHistory = ref<any[]>([])
const generating = ref(false)

const summaryLines = computed(() => {
  if (!currentReport.value?.summary) return []
  return currentReport.value.summary.split('\n').filter((l: string) => l.trim())
})

const reportColumns = [
  { title: '日期', key: 'reportDate', width: 120 },
  { title: '跟进', key: 'customerFollowUp', width: 70 },
  { title: '新增', key: 'newCustomers', width: 70 },
  { title: '签约', key: 'dealCount', width: 70 },
  { title: '签约额', key: 'dealAmount', width: 120, render: (row: any) => h('span', '¥' + (row.dealAmount || 0).toLocaleString()) },
  { title: '待办完成', key: 'todoCompleted', width: 80 },
  {
    title: '操作', key: 'actions', width: 80,
    render: (row: any) => h(NButton, { size: 'tiny', onClick: () => viewReport(row) }, () => '查看')
  }
]

onMounted(() => loadHistory())

async function loadHistory() {
  try {
    const res = await request.get('/report/list')
    reportHistory.value = Array.isArray(res) ? res : []
  } catch { reportHistory.value = [] }
}

async function generateReport() {
  generating.value = true
  const date = new Date(selectedDate.value).toISOString().split('T')[0]
  try {
    const res = await request.post('/report/generate', null, { params: { date } })
    currentReport.value = res
    message.success('日报已生成')
    loadHistory()
  } catch {
    message.error('生成失败')
  } finally {
    generating.value = false
  }
}

function viewReport(report: any) {
  currentReport.value = report
}
</script>

<style scoped>
.report-container { padding: 0; }
.report-date { font-size: 18px; font-weight: 700; color: #333; }
.stat-card {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  color: #fff;
}
.stat-card.blue { background: linear-gradient(135deg, #2080f0, #409eff); }
.stat-card.green { background: linear-gradient(135deg, #18a058, #36d399); }
.stat-card.orange { background: linear-gradient(135deg, #f0a020, #fbbf24); }
.stat-card.purple { background: linear-gradient(135deg, #722ed1, #a78bfa); }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; margin-top: 4px; opacity: 0.9; }
.report-summary { line-height: 1.8; font-size: 14px; }
.summary-line { padding: 2px 0; }
.ai-suggestions { color: #18a058; line-height: 1.6; font-size: 14px; padding: 8px; background: #f0fdf4; border-radius: 6px; }
</style>
