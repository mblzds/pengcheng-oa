<template>
  <div class="approval-pending-page">
    <n-card>
      <template #header>
        <n-space justify="space-between" align="center">
          <span>我的待审批</span>
          <n-text depth="3" style="font-size: 12px">
            列出当前账号作为审批人 / 候选审批人 的待办（请假、调休、付款、佣金）。
          </n-text>
        </n-space>
      </template>

      <n-space vertical :size="12">
        <n-space>
          <n-select
            v-model:value="typeFilter"
            :options="typeOptions"
            clearable
            placeholder="按类型筛选"
            style="width: 200px"
          />
          <n-button type="primary" @click="loadList" :loading="loading">刷新</n-button>
          <n-tag v-if="rows.length" type="info" :bordered="false">
            共 {{ filteredRows.length }} 条待办
          </n-tag>
        </n-space>

        <n-data-table
          :columns="columns"
          :data="filteredRows"
          :loading="loading"
          :pagination="{ pageSize: 10 }"
          :row-key="(row: ApprovalItem) => `${row.type}-${row.id}`"
        />
      </n-space>
    </n-card>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="detailVisible" :width="560" placement="right">
      <n-drawer-content :title="`${typeLabel(detail?.type)}详情`" closable>
        <n-spin :show="detailLoading">
          <n-descriptions
            v-if="detail"
            :column="1"
            label-placement="left"
            label-style="width:96px;"
            bordered
            size="small"
          >
            <n-descriptions-item label="申请人">{{ detail.applicantName }}</n-descriptions-item>
            <n-descriptions-item label="摘要">{{ detail.summary }}</n-descriptions-item>
            <n-descriptions-item v-if="detail.amount != null" label="金额">
              ¥ {{ detail.amount }}
            </n-descriptions-item>
            <n-descriptions-item label="申请时间">
              {{ formatTime(detail.applyTime) }}
            </n-descriptions-item>
            <n-descriptions-item label="状态">
              <n-tag :type="statusTagType(detail.status)" size="small">
                {{ statusText(detail.status) }}
              </n-tag>
            </n-descriptions-item>
          </n-descriptions>

          <n-divider title-placement="left">流转记录</n-divider>

          <n-timeline v-if="detail?.histories?.length">
            <n-timeline-item
              v-for="(h, idx) in detail.histories"
              :key="idx"
              :type="historyType(h.result)"
              :title="h.nodeName || h.approverName"
              :time="h.approvalTime ? formatTime(h.approvalTime) : '待审批'"
              :content="historyContent(h)"
            />
          </n-timeline>
          <n-empty v-else description="暂无流转记录" />
        </n-spin>
      </n-drawer-content>
    </n-drawer>

    <!-- 审批操作弹窗 -->
    <n-modal
      v-model:show="actionVisible"
      preset="dialog"
      :title="actionTitle"
      :positive-text="actionApproved ? '通过' : '驳回'"
      negative-text="取消"
      :positive-button-props="{ type: actionApproved ? 'primary' : 'error', loading: submitting }"
      @positive-click="confirmAction"
      @negative-click="actionVisible = false"
    >
      <n-form :model="actionForm" label-placement="top">
        <n-form-item v-if="actionTarget" label="申请摘要">
          <n-text>{{ actionTarget.summary }}</n-text>
        </n-form-item>
        <n-form-item :label="actionApproved ? '审批意见（可选）' : '驳回原因（必填）'">
          <n-input
            v-model:value="actionForm.reason"
            type="textarea"
            :placeholder="actionApproved ? '可填写审批备注' : '请说明驳回原因'"
            :autosize="{ minRows: 3, maxRows: 6 }"
          />
        </n-form-item>
      </n-form>
    </n-modal>
  </div>
</template>

<script setup lang="tsx">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { approvalApi, type ApprovalDetail, type ApprovalItem, type ApprovalType } from '@/api/approval'

const message = useMessage()
const loading = ref(false)
const rows = ref<ApprovalItem[]>([])
const typeFilter = ref<ApprovalType | null>(null)

const typeOptions = [
  { label: '请假', value: 'leave' },
  { label: '调休', value: 'compensate' },
  { label: '费用报销', value: 'expense' },
  { label: '垫佣申请', value: 'advance' },
  { label: '预付佣申请', value: 'prepay' },
  { label: '佣金审核', value: 'commission' }
]

const filteredRows = computed(() =>
  typeFilter.value ? rows.value.filter(r => r.type === typeFilter.value) : rows.value
)

function typeLabel(t?: string | null): string {
  return typeOptions.find(o => o.value === t)?.label || '审批'
}

function typeTagType(t: string): 'info' | 'success' | 'warning' | 'error' | 'default' {
  switch (t) {
    case 'leave':
    case 'compensate':
      return 'info'
    case 'expense':
    case 'advance':
    case 'prepay':
      return 'warning'
    case 'commission':
      return 'success'
    default:
      return 'default'
  }
}

function formatTime(t?: string | null): string {
  if (!t) return ''
  return t.replace('T', ' ').slice(0, 19)
}

function statusText(s?: number | null): string {
  switch (s) {
    case 1: return '待审批'
    case 2: return '审批中'
    case 3: return '已通过'
    case 4: return '已驳回'
    default: return '未知'
  }
}

function statusTagType(s?: number | null): 'info' | 'warning' | 'success' | 'error' | 'default' {
  switch (s) {
    case 1: return 'info'
    case 2: return 'warning'
    case 3: return 'success'
    case 4: return 'error'
    default: return 'default'
  }
}

function historyType(r?: number | null): 'info' | 'success' | 'error' | 'default' {
  if (r === 1) return 'success'
  if (r === 2) return 'error'
  return 'info'
}

function historyContent(h: { approverName: string; remark?: string | null; result?: number | null }): string {
  const label = h.result === 1 ? '通过' : h.result === 2 ? '驳回' : '待处理'
  const parts = [`${h.approverName}（${label}）`]
  if (h.remark) parts.push(`意见：${h.remark}`)
  return parts.join('\n')
}

const columns: DataTableColumns<ApprovalItem> = [
  {
    title: '申请人',
    key: 'applicantName',
    width: 110
  },
  {
    title: '类型',
    key: 'type',
    width: 110,
    render: row => h(NTag, { type: typeTagType(row.type), size: 'small', bordered: false }, { default: () => typeLabel(row.type) })
  },
  {
    title: '摘要',
    key: 'summary',
    minWidth: 200,
    ellipsis: { tooltip: true }
  },
  {
    title: '金额',
    key: 'amount',
    width: 120,
    render: row => row.amount != null ? `¥ ${row.amount}` : '—'
  },
  {
    title: '当前节点',
    key: 'currentNodeName',
    width: 130,
    render: row => row.currentNodeName || '—'
  },
  {
    title: '申请时间',
    key: 'applyTime',
    width: 170,
    render: row => formatTime(row.applyTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    fixed: 'right',
    render: row =>
      h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', secondary: true, onClick: () => openDetail(row) }, { default: () => '详情' }),
          h(NButton, { size: 'small', type: 'primary', onClick: () => openAction(row, true) }, { default: () => '通过' }),
          h(NButton, { size: 'small', type: 'error', ghost: true, onClick: () => openAction(row, false) }, { default: () => '驳回' })
        ]
      })
  }
]

async function loadList() {
  loading.value = true
  try {
    const data = await approvalApi.pending()
    rows.value = Array.isArray(data) ? data : []
  } finally {
    loading.value = false
  }
}

// ---- 详情 ----
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<ApprovalDetail | null>(null)

async function openDetail(row: ApprovalItem) {
  detail.value = null
  detailVisible.value = true
  detailLoading.value = true
  try {
    detail.value = await approvalApi.detail(row.id, row.type)
  } finally {
    detailLoading.value = false
  }
}

// ---- 审批操作 ----
const actionVisible = ref(false)
const actionApproved = ref(true)
const actionTarget = ref<ApprovalItem | null>(null)
const actionForm = reactive({ reason: '' })
const submitting = ref(false)

const actionTitle = computed(() =>
  `${actionApproved.value ? '通过' : '驳回'} - ${typeLabel(actionTarget.value?.type)}申请`
)

function openAction(row: ApprovalItem, approved: boolean) {
  actionTarget.value = row
  actionApproved.value = approved
  actionForm.reason = ''
  actionVisible.value = true
}

async function confirmAction() {
  if (!actionTarget.value) return false
  if (!actionApproved.value && !actionForm.reason.trim()) {
    message.error('驳回时必须填写驳回原因')
    return false
  }
  submitting.value = true
  try {
    await approvalApi.approve(actionTarget.value.id, {
      type: actionTarget.value.type,
      approved: actionApproved.value,
      reason: actionForm.reason.trim() || undefined
    })
    message.success(`${actionApproved.value ? '通过' : '驳回'}成功`)
    actionVisible.value = false
    await loadList()
    return true
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.approval-pending-page {
  padding: 16px;
}
</style>
