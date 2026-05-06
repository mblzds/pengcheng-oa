<template>
  <n-space vertical :size="16">
    <n-grid :cols="24" :x-gap="16" :y-gap="16">
      <n-gi :span="8">
        <n-card size="small">
          <n-statistic label="应收总额" :value="stats?.totalDue || 0" prefix="¥" />
        </n-card>
      </n-gi>
      <n-gi :span="8">
        <n-card size="small">
          <n-statistic label="已回总额" :value="stats?.totalPaid || 0" prefix="¥" />
        </n-card>
      </n-gi>
      <n-gi :span="8">
        <n-card size="small">
          <n-statistic label="逾期金额" :value="stats?.totalOverdue || 0" prefix="¥" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="逾期/临期告警">
      <template #header-extra>
        <n-space>
          <n-button tertiary @click="$emit('refresh')">刷新</n-button>
          <n-button type="primary" :loading="checking" @click="$emit('run-check')">手动巡检</n-button>
        </n-space>
      </template>

      <n-data-table :columns="columns" :data="alerts" :loading="loading" :pagination="false" />
    </n-card>
  </n-space>
</template>

<script setup lang="ts">
import { h } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import type { ReceivableAlertRecord, ReceivableStatsRecord } from '@/api/receivable'

defineProps<{
  stats: ReceivableStatsRecord | null
  alerts: ReceivableAlertRecord[]
  loading: boolean
  checking: boolean
}>()

defineEmits<{
  (e: 'refresh'): void
  (e: 'run-check'): void
}>()

const alertTypeMap: Record<number, { text: string; type: 'warning' | 'error' }> = {
  1: { text: '逾期未回款', type: 'error' },
  2: { text: '即将到期', type: 'warning' }
}

const columns: DataTableColumns<ReceivableAlertRecord> = [
  { title: '告警ID', key: 'id', width: 90 },
  { title: '分期ID', key: 'planId', width: 90 },
  {
    title: '告警类型',
    key: 'alertType',
    width: 120,
    render: row =>
      h(NTag, { type: alertTypeMap[row.alertType]?.type || 'warning', size: 'small' }, {
        default: () => alertTypeMap[row.alertType]?.text || '未知'
      })
  },
  { title: '首次告警', key: 'alertTime', width: 180 },
  { title: '最后通知', key: 'lastNotified', width: 180 },
  { title: '通知次数', key: 'notifyCount', width: 100 },
  {
    title: '处理状态',
    key: 'handled',
    width: 100,
    render: row =>
      h(NTag, { type: row.handled === 1 ? 'success' : 'warning', size: 'small' }, {
        default: () => (row.handled === 1 ? '已处理' : '未处理')
      })
  },
  { title: '处理备注', key: 'handledRemark', minWidth: 180 }
]
</script>
