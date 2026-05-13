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
        <n-space :wrap="false" style="flex-wrap: wrap">
          <n-input
            v-model:value="keyword"
            placeholder="搜申请人 / 部门 / 摘要"
            clearable
            style="width: 260px"
          />
          <n-select
            v-model:value="typeFilter"
            :options="typeOptions"
            clearable
            placeholder="按类型筛选"
            style="width: 180px"
          />
          <n-button type="primary" @click="loadList" :loading="loading">刷新</n-button>
          <n-tag v-if="rows.length" type="info" :bordered="false">
            共 {{ filteredRows.length }} 条待办
          </n-tag>
        </n-space>

        <n-data-table
          :columns="unifiedColumns"
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
            <!-- 假期专属字段 -->
            <n-descriptions-item v-if="detail.leaveTypeLabel" label="假期类型">
              {{ detail.leaveTypeLabel }}
            </n-descriptions-item>
            <n-descriptions-item v-if="detail.startTime" label="开始时间">
              {{ formatTime(detail.startTime) }}
            </n-descriptions-item>
            <n-descriptions-item v-if="detail.endTime" label="结束时间">
              {{ formatTime(detail.endTime) }}
            </n-descriptions-item>
            <n-descriptions-item v-if="detail.days != null" label="天数">
              {{ detail.days }} 天
            </n-descriptions-item>
            <n-descriptions-item v-if="detail.reason" label="原因">
              {{ detail.reason }}
            </n-descriptions-item>
            <!-- 付款类才显示金额；假期类隐藏 -->
            <n-descriptions-item v-if="detail.amount != null" label="金额">
              ¥ {{ detail.amount }}
            </n-descriptions-item>
            <!-- 假期类已经把信息拆开展示，摘要冗余可隐；非假期保留 -->
            <n-descriptions-item v-if="!detail.leaveTypeLabel" label="摘要">
              {{ detail.summary }}
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

          <template v-if="detail?.attachments?.length">
            <n-divider title-placement="left">票据附件</n-divider>
            <n-image-group>
              <n-space>
                <n-image
                  v-for="(p, i) in detail.attachments"
                  :key="i"
                  :src="resolveImg(p)"
                  width="120"
                  height="120"
                  object-fit="cover"
                  style="border-radius: 6px"
                />
              </n-space>
            </n-image-group>
          </template>

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
import { NButton, NImage, NImageGroup, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { approvalApi, approvalFlowApi, type ApprovalDetail, type ApprovalItem, type ApprovalType, type BusinessTypeOption } from '@/api/approval'
import { formatDateTime } from '@/utils/datetime'

const message = useMessage()
const loading = ref(false)
const rows = ref<ApprovalItem[]>([])
const typeFilter = ref<ApprovalType | null>(null)
const keyword = ref('')

// 类型筛选/展示从 approval_business_type 表实时拉，支持运营新建的自定义类型
// commission（佣金审核）有独立的审计入口，不放在此筛选里——表格里如果出现佣金行用户也能直接看到
const typeOptions = ref<{ label: string, value: string }[]>([])
const businessTypes = ref<BusinessTypeOption[]>([])

async function loadBusinessTypes() {
  try {
    businessTypes.value = await approvalFlowApi.businessTypes()
    typeOptions.value = businessTypes.value.map(t => ({
      label: t.builtin === 1 ? builtinFullLabel(t.businessType, t.label) : t.label,
      value: t.businessType
    }))
  } catch (e) {
    // 拉不到也不阻塞页面
  }
}

// 内置类型沿用之前的"全名"（如「费用报销」「垫佣申请」），自定义类型直接用 label
function builtinFullLabel(key: string, label: string): string {
  const fullMap: Record<string, string> = {
    leave: '请假',
    compensate: '调休',
    expense: '费用报销',
    advance: '垫佣申请',
    prepay: '预付佣申请'
  }
  return fullMap[key] || label
}

const filteredRows = computed(() => {
  let result = rows.value
  if (typeFilter.value) {
    result = result.filter(r => r.type === typeFilter.value)
  }
  const kw = keyword.value.trim().toLowerCase()
  if (kw) {
    result = result.filter(r =>
      (r.applicantName || '').toLowerCase().includes(kw) ||
      (r.applicantDept || '').toLowerCase().includes(kw) ||
      (r.summary || '').toLowerCase().includes(kw) ||
      (r.reason || '').toLowerCase().includes(kw)
    )
  }
  return result
})

/** 数字千分位 + 2 位小数 */
function formatAmount(v?: number | null): string {
  if (v == null) return '—'
  return '¥ ' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** "已等待"格式化：< 1h 显示分钟；< 1d 显示小时；其它显示天 */
function formatWaiting(applyTime?: string | null): { text: string; level: 'normal' | 'warn' | 'urgent' } {
  if (!applyTime) return { text: '—', level: 'normal' }
  const ms = Date.now() - new Date(applyTime).getTime()
  if (ms < 0) return { text: '刚刚', level: 'normal' }
  const minutes = Math.floor(ms / 60000)
  const hours = Math.floor(ms / 3600000)
  const days = Math.floor(ms / 86400000)
  let text: string
  if (minutes < 60) text = `${minutes} 分钟`
  else if (hours < 24) text = `${hours} 小时`
  else text = `${days} 天`
  const level: 'normal' | 'warn' | 'urgent' = days >= 3 ? 'urgent' : hours >= 24 ? 'warn' : 'normal'
  return { text, level }
}

/** 列表"类型"列展示：拼接基础类型 + 子类型，例如 "请假·事假" / "报销·餐饮" */
function buildTypeDisplay(row: ApprovalItem): string {
  const base = typeLabel(row.type)
  if ((row.type === 'leave') && row.leaveTypeLabel) {
    return `${base}·${row.leaveTypeLabel}`
  }
  if (row.type === 'expense' && row.expenseTypeLabel) {
    return `${base}·${row.expenseTypeLabel}`
  }
  return base
}

/** 按类型拼装"关键内容"列文案 */
function buildKeyContent(row: ApprovalItem): string {
  if (row.type === 'leave' || row.type === 'compensate') {
    const parts: string[] = []
    if (row.days != null) parts.push(`${row.days} 天`)
    if (row.dateRange) parts.push(row.dateRange)
    if (row.leaveTypeLabel) parts.push(row.leaveTypeLabel)
    if (row.reason) parts.push(row.reason)
    return parts.join(' · ') || row.summary || '—'
  }
  return row.summary || '—'
}

function typeLabel(t?: string | null): string {
  return typeOptions.value.find(o => o.value === t)?.label || (t || '审批')
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

// 走统一工具 formatDateTime（utils/datetime.ts）；保留 formatTime 函数名兼容现有 8 处调用
function formatTime(t?: string | null): string {
  return formatDateTime(t, '')
}

/** 后端本地存储返回 /api/files/...，浏览器在 admin 域上能否解析取决于反代；
 *  这里走 axios baseURL 同源逻辑：相对 /api/* 直接给浏览器（同域），其他原样透传 */
function resolveImg(p?: string | null): string {
  if (!p) return ''
  if (/^(blob|https?):/i.test(p)) return p
  return p // /api/files/... 同源由 nginx/spring 处理
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

// 统一列定义：不再按 typeFilter 切换列集
// 设计参照 doc/PERMISSION-AND-ROLES.md 配套的方案 C：审批人多半要快速扫读 + 跨类决策，统一表格更高效
const unifiedColumns = computed<DataTableColumns<ApprovalItem>>(() => [
  {
    title: '申请人',
    key: 'applicantName',
    width: 130,
    render: row => h('div', { style: 'line-height:1.4' }, [
      h('div', { style: 'font-weight:500' }, row.applicantName || '—'),
      row.applicantDept
        ? h('div', { style: 'color:#999;font-size:12px;margin-top:2px' }, row.applicantDept)
        : null
    ])
  },
  {
    title: '类型',
    key: 'type',
    width: 110,
    render: row => h(NTag, { type: typeTagType(row.type), size: 'small', bordered: false }, { default: () => buildTypeDisplay(row) })
  },
  {
    title: '关键内容',
    key: 'keyContent',
    minWidth: 240,
    ellipsis: { tooltip: true },
    render: row => buildKeyContent(row)
  },
  {
    title: '金额',
    key: 'amount',
    width: 130,
    align: 'right' as const,
    sorter: (a: ApprovalItem, b: ApprovalItem) => (a.amount || 0) - (b.amount || 0),
    render: row => row.amount != null ? formatAmount(row.amount) : h('span', { style: 'color:#bbb' }, '—')
  },
  {
    title: '已等',
    key: 'waiting',
    width: 100,
    sorter: (a: ApprovalItem, b: ApprovalItem) => {
      const ta = a.applyTime ? new Date(a.applyTime).getTime() : 0
      const tb = b.applyTime ? new Date(b.applyTime).getTime() : 0
      return ta - tb  // 申请时间早的 → 已等时间长 → 排前
    },
    defaultSortOrder: 'ascend' as const,
    render: row => {
      const w = formatWaiting(row.applyTime)
      const color = w.level === 'urgent' ? '#d03050' : w.level === 'warn' ? '#f0a020' : '#666'
      const weight = w.level === 'urgent' ? '600' : '400'
      return h('span', { style: `color:${color};font-weight:${weight}` }, w.text)
    }
  },
  {
    title: '当前节点',
    key: 'currentNodeName',
    width: 110,
    render: row => row.currentNodeName || '—'
  },
  {
    title: '操作',
    key: 'actions',
    width: 220,
    fixed: 'right' as const,
    render: row =>
      h(NSpace, { size: 'small' }, {
        default: () => [
          h(NButton, { size: 'small', type: 'primary', secondary: true, onClick: () => openDetail(row) }, { default: () => '详情' }),
          h(NButton, { size: 'small', type: 'primary', onClick: () => openAction(row, true) }, { default: () => '通过' }),
          h(NButton, { size: 'small', type: 'error', ghost: true, onClick: () => openAction(row, false) }, { default: () => '驳回' })
        ]
      })
  }
])

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

onMounted(async () => {
  await loadBusinessTypes()
  await loadList()
})
</script>

<style scoped>
.approval-pending-page {
  padding: 16px;
}
</style>
