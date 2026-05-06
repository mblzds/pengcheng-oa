<template>
  <n-space vertical :size="16">
    <n-card size="small">
      <n-form inline>
        <n-form-item label="成交记录ID">
          <n-input-number v-model:value="filters.dealId" :min="1" clearable style="width: 180px" />
        </n-form-item>
        <n-form-item label="分期状态">
          <n-select v-model:value="filters.status" :options="statusOptions" clearable style="width: 180px" />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" @click="emitSearch">搜索</n-button>
            <n-button @click="emitReset">重置</n-button>
            <n-button tertiary @click="$emit('refresh')">刷新</n-button>
            <n-button type="success" @click="$emit('open-create')">新建回款计划</n-button>
          </n-space>
        </n-form-item>
      </n-form>
    </n-card>

    <n-data-table
      :columns="columns"
      :data="plans"
      :loading="loading"
      :pagination="pagination"
      :row-key="rowKey"
      @update:page="handlePageChange"
      @update:page-size="handlePageSizeChange"
    />
  </n-space>
</template>

<script setup lang="ts">
import { h, reactive } from 'vue'
import { NButton, NSpace, NTag, type DataTableColumns } from 'naive-ui'
import type { ReceivablePlanRecord } from '@/api/receivable'

type SearchFilters = {
  dealId?: number
  status?: number
}

const props = defineProps<{
  plans: ReceivablePlanRecord[]
  loading: boolean
  pagination: {
    page: number
    pageSize: number
    itemCount: number
    showSizePicker?: boolean
    pageSizes?: number[]
  }
}>()

const emit = defineEmits<{
  (e: 'search', filters: SearchFilters): void
  (e: 'reset'): void
  (e: 'refresh'): void
  (e: 'open-create'): void
  (e: 'view-records', plan: ReceivablePlanRecord): void
  (e: 'page-change', page: number, filters: SearchFilters): void
  (e: 'page-size-change', pageSize: number, filters: SearchFilters): void
}>()

const filters = reactive({
  dealId: null as number | null,
  status: null as number | null
})

const statusOptions = [
  { label: '未到期', value: 0 },
  { label: '待回款', value: 1 },
  { label: '部分回款', value: 2 },
  { label: '已回款', value: 3 },
  { label: '已逾期', value: 4 }
]

const statusTypeMap: Record<number, 'default' | 'warning' | 'info' | 'success' | 'error'> = {
  0: 'default',
  1: 'warning',
  2: 'info',
  3: 'success',
  4: 'error'
}

const statusTextMap: Record<number, string> = {
  0: '未到期',
  1: '待回款',
  2: '部分回款',
  3: '已回款',
  4: '已逾期'
}

const columns: DataTableColumns<ReceivablePlanRecord> = [
  { title: '分期ID', key: 'id', width: 90 },
  { title: '成交ID', key: 'dealId', width: 90 },
  { title: '期号', key: 'periodNo', width: 80 },
  {
    title: '分期名称',
    key: 'periodName',
    width: 120,
    render: row => row.periodName || `第 ${row.periodNo} 期`
  },
  { title: '应回日期', key: 'dueDate', width: 120 },
  {
    title: '应回金额',
    key: 'dueAmount',
    width: 130,
    render: row => currency(row.dueAmount)
  },
  {
    title: '已回金额',
    key: 'paidAmount',
    width: 130,
    render: row => currency(row.paidAmount)
  },
  {
    title: '未回金额',
    key: 'unpaidAmount',
    width: 130,
    render: row => currency(Math.max(Number(row.dueAmount ?? 0) - Number(row.paidAmount ?? 0), 0))
  },
  {
    title: '状态',
    key: 'status',
    width: 110,
    render: row =>
      h(NTag, { type: statusTypeMap[row.status] || 'default', size: 'small' }, {
        default: () => statusTextMap[row.status] || '未知'
      })
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              type: 'primary',
              tertiary: true,
              onClick: () => emit('view-records', row)
            },
            { default: () => '登记/查看' }
          )
        ]
      })
  }
]

function normalizeFilters(): SearchFilters {
  return {
    dealId: filters.dealId ?? undefined,
    status: filters.status ?? undefined
  }
}

function emitSearch() {
  emit('search', normalizeFilters())
}

function emitReset() {
  filters.dealId = null
  filters.status = null
  emit('reset')
}

function handlePageChange(page: number) {
  emit('page-change', page, normalizeFilters())
}

function handlePageSizeChange(pageSize: number) {
  emit('page-size-change', pageSize, normalizeFilters())
}

function rowKey(row: ReceivablePlanRecord) {
  return row.id
}

function currency(value?: number) {
  return `¥${Number(value ?? 0).toLocaleString()}`
}
</script>
