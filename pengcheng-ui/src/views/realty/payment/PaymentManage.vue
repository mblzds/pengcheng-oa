<template>
  <div class="page-container">
    <n-card>
      <div class="search-form">
        <n-form inline :model="searchForm">
          <n-form-item label="申请类型">
            <n-select v-model:value="searchForm.requestType" :options="requestTypeOptions" clearable style="width: 180px" />
          </n-form-item>
          <n-form-item label="审批状态">
            <n-select v-model:value="searchForm.status" :options="statusOptions" clearable style="width: 180px" />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">搜索</n-button>
              <n-button @click="handleReset">重置</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <n-data-table
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="rowKey"
        @update:page="handlePageChange"
        @update:page-size="handlePageSizeChange"
      />
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="760" placement="right">
      <n-drawer-content title="付款申请详情" closable>
        <n-space vertical :size="16">
          <n-card size="small" title="申请信息">
            <n-descriptions :column="2" bordered>
              <n-descriptions-item label="申请ID">{{ detailData?.id || '-' }}</n-descriptions-item>
              <n-descriptions-item label="申请人ID">{{ detailData?.applicantId || '-' }}</n-descriptions-item>
              <n-descriptions-item label="申请类型">{{ requestTypeText(detailData?.requestType) }}</n-descriptions-item>
              <n-descriptions-item label="状态">{{ statusText(detailData?.status) }}</n-descriptions-item>
              <n-descriptions-item label="金额">{{ detailData?.amount || '-' }}</n-descriptions-item>
              <n-descriptions-item label="报销类型">{{ expenseTypeText(detailData?.expenseType) }}</n-descriptions-item>
              <n-descriptions-item label="关联成交ID">{{ detailData?.relatedDealId || '-' }}</n-descriptions-item>
              <n-descriptions-item label="关联联盟商ID">{{ detailData?.relatedAllianceId || '-' }}</n-descriptions-item>
              <n-descriptions-item label="申请说明" :span="2">{{ detailData?.description || '-' }}</n-descriptions-item>
              <n-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detailData?.createTime) }}</n-descriptions-item>
            </n-descriptions>
          </n-card>

          <n-card size="small" title="审批流转历史时间线">
            <n-timeline>
              <n-timeline-item
                v-for="item in approvalTimeline"
                :key="item.id"
                :title="item.title"
                :time="item.time"
                :content="item.content"
              />
            </n-timeline>
          </n-card>

          <n-space justify="end" v-if="canApprove(detailData?.status)">
            <n-button type="success" @click="approveRequest">通过</n-button>
            <n-button type="error" @click="openRejectModal">驳回</n-button>
          </n-space>
        </n-space>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="rejectVisible" preset="card" title="驳回申请" style="width: 520px">
      <n-form>
        <n-form-item label="驳回原因" label-placement="left" label-width="90">
          <n-input v-model:value="rejectRemark" type="textarea" :rows="4" placeholder="请输入驳回原因" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="rejectVisible = false">取消</n-button>
          <n-button type="error" @click="rejectRequest">确认驳回</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, reactive, ref, onMounted } from 'vue'
import { NButton, NIcon, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { CheckmarkCircleOutline, CloseCircleOutline, EyeOutline } from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import { realtyApi, type PaymentApprovalRecord, type PaymentRecord } from '@/api/realty'

const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const tableData = ref<PaymentRecord[]>([])

const searchForm = reactive<{
  requestType: number | null
  status: number | null
}>({
  requestType: null,
  status: null
})

const requestTypeOptions = [
  { label: '费用报销', value: 1 },
  { label: '垫佣', value: 2 },
  { label: '预付佣', value: 3 }
]

const statusOptions = [
  { label: '待审批', value: 1 },
  { label: '审批中', value: 2 },
  { label: '已通过', value: 3 },
  { label: '已驳回', value: 4 }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const detailVisible = ref(false)
const detailData = ref<PaymentRecord | null>(null)
const approvalHistory = ref<PaymentApprovalRecord[]>([])

const rejectVisible = ref(false)
const rejectRemark = ref('')

const columns: DataTableColumns<PaymentRecord> = [
  { title: '申请ID', key: 'id', width: 90 },
  { title: '申请人ID', key: 'applicantId', width: 100 },
  {
    title: '申请类型',
    key: 'requestType',
    width: 120,
    render: row => requestTypeText(row.requestType)
  },
  { title: '金额', key: 'amount', width: 120 },
  {
    title: '审批状态',
    key: 'status',
    width: 110,
    render: row => renderStatus(row.status)
  },
  {
    title: '创建时间',
    key: 'createTime',
    width: 170,
    render: row => formatDateTime(row.createTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => openDetail(row)
            },
            {
              icon: () => h(NIcon, null, { default: () => h(EyeOutline) }),
              default: () => '详情'
            }
          ),
          ...(canApprove(row.status)
            ? [
                h(
                  NButton,
                  {
                    size: 'small',
                    type: 'success',
                    tertiary: true,
                    onClick: () => quickApprove(row)
                  },
                  {
                    icon: () => h(NIcon, null, { default: () => h(CheckmarkCircleOutline) }),
                    default: () => '通过'
                  }
                ),
                h(
                  NButton,
                  {
                    size: 'small',
                    type: 'error',
                    tertiary: true,
                    onClick: () => quickReject(row)
                  },
                  {
                    icon: () => h(NIcon, null, { default: () => h(CloseCircleOutline) }),
                    default: () => '驳回'
                  }
                )
              ]
            : [])
        ]
      })
  }
]

const approvalTimeline = computed(() => {
  return approvalHistory.value.map(item => ({
    id: item.id,
    title: `审批人 #${item.approverId} - ${item.result === 1 ? '通过' : '驳回'}`,
    time: formatDateTime(item.approvalTime),
    content: item.remark || '无备注'
  }))
})

function rowKey(row: PaymentRecord) {
  return row.id
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function requestTypeText(type?: number) {
  if (type === 1) return '费用报销'
  if (type === 2) return '垫佣'
  if (type === 3) return '预付佣'
  return '-'
}

function expenseTypeText(type?: number) {
  if (type === 1) return '交通费'
  if (type === 2) return '餐饮费'
  if (type === 3) return '住宿费'
  if (type === 4) return '办公用品'
  if (type === 5) return '其他'
  return '-'
}

function statusText(status?: number) {
  if (status === 1) return '待审批'
  if (status === 2) return '审批中'
  if (status === 3) return '已通过'
  if (status === 4) return '已驳回'
  return '-'
}

function renderStatus(status: number) {
  const map: Record<number, { text: string; type: 'warning' | 'info' | 'success' | 'error' }> = {
    1: { text: '待审批', type: 'warning' },
    2: { text: '审批中', type: 'info' },
    3: { text: '已通过', type: 'success' },
    4: { text: '已驳回', type: 'error' }
  }
  const item = map[status] || { text: '-', type: 'warning' as const }
  return h(NTag, { size: 'small', type: item.type }, { default: () => item.text })
}

function canApprove(status?: number) {
  return status === 1 || status === 2
}

async function loadData() {
  loading.value = true
  try {
    const res = await realtyApi.paymentPage({
      page: pagination.page,
      pageSize: pagination.pageSize,
      requestType: searchForm.requestType || undefined,
      status: searchForm.status || undefined
    })
    tableData.value = res.list || []
    pagination.itemCount = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  searchForm.requestType = null
  searchForm.status = null
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadData()
}

async function openDetail(row: PaymentRecord) {
  detailData.value = row
  approvalHistory.value = await realtyApi.paymentApprovals(row.id)
  detailVisible.value = true
}

async function quickApprove(row: PaymentRecord) {
  await realtyApi.paymentApprove({
    requestId: row.id,
    approverId: userStore.user?.id || 0,
    approved: true
  })
  message.success('审批通过')
  loadData()
  if (detailData.value?.id === row.id) {
    openDetail(row)
  }
}

function quickReject(row: PaymentRecord) {
  detailData.value = row
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function approveRequest() {
  if (!detailData.value) return
  await quickApprove(detailData.value)
}

function openRejectModal() {
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function rejectRequest() {
  if (!detailData.value) return
  if (!rejectRemark.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }

  await realtyApi.paymentApprove({
    requestId: detailData.value.id,
    approverId: userStore.user?.id || 0,
    approved: false,
    remark: rejectRemark.value.trim()
  })
  rejectVisible.value = false
  message.success('已驳回')
  loadData()
  openDetail(detailData.value)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}
</style>
