<template>
  <div class="page-container">
    <n-card>
      <div class="search-form">
        <n-form inline :model="searchForm">
          <n-form-item label="项目">
            <n-select
              v-model:value="searchForm.projectId"
              :options="projectOptions"
              label-field="projectName"
              value-field="id"
              clearable
              filterable
              remote
              @search="handleProjectSearch"
              style="width: 220px"
            />
          </n-form-item>
          <n-form-item label="联盟商">
            <n-select
              v-model:value="searchForm.allianceId"
              :options="allianceOptions"
              label-field="companyName"
              value-field="id"
              clearable
              filterable
              remote
              @search="handleAllianceSearch"
              style="width: 220px"
            />
          </n-form-item>
          <n-form-item label="审核状态">
            <n-select v-model:value="searchForm.auditStatus" :options="auditStatusOptions" clearable style="width: 180px" />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">搜索</n-button>
              <n-button @click="handleReset">重置</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <div class="table-toolbar">
        <n-button type="primary" @click="openCreateModal">录入佣金</n-button>
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

    <n-modal v-model:show="createVisible" preset="card" title="佣金录入" style="width: 700px">
      <n-form ref="createFormRef" :model="createForm" :rules="createRules" label-placement="left" label-width="120">
        <n-form-item label="成交记录ID" path="dealId">
          <n-input-number v-model:value="createForm.dealId" :min="1" style="width: 100%" />
        </n-form-item>
        <n-form-item label="项目" path="projectId">
          <n-select
            v-model:value="createForm.projectId"
            :options="projectOptions"
            label-field="projectName"
            value-field="id"
            filterable
            remote
            @search="handleProjectSearch"
          />
        </n-form-item>
        <n-form-item label="联盟商" path="allianceId">
          <n-select
            v-model:value="createForm.allianceId"
            :options="allianceOptions"
            label-field="companyName"
            value-field="id"
            filterable
            remote
            @search="handleAllianceSearch"
          />
        </n-form-item>
        <n-form-item label="应收佣金" path="receivableAmount">
          <n-input-number v-model:value="createForm.receivableAmount" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="应结佣金" path="payableAmount">
          <n-input-number v-model:value="createForm.payableAmount" :min="0" style="width: 100%" />
        </n-form-item>
        <n-form-item label="平台费" path="platformFee">
          <n-input-number v-model:value="createForm.platformFee" :min="0" style="width: 100%" />
        </n-form-item>
        <n-divider title-placement="left">佣金明细（可选）</n-divider>
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="基础佣金">
            <n-input-number v-model:value="createForm.detail.baseCommission" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="跳点佣金">
            <n-input-number v-model:value="createForm.detail.jumpPointCommission" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="现金奖">
            <n-input-number v-model:value="createForm.detail.cashReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="开单奖">
            <n-input-number v-model:value="createForm.detail.firstDealReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="平台奖励">
            <n-input-number v-model:value="createForm.detail.platformReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
        </n-grid>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitCreate">提交</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-modal v-model:show="rejectVisible" preset="card" title="驳回佣金" style="width: 520px">
      <n-form>
        <n-form-item label="驳回原因" label-placement="left" label-width="90">
          <n-input v-model:value="rejectRemark" type="textarea" placeholder="请输入驳回原因" :rows="4" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="rejectVisible = false">取消</n-button>
          <n-button type="error" @click="confirmReject">确认驳回</n-button>
        </n-space>
      </template>
    </n-modal>

    <n-drawer v-model:show="logVisible" :width="680" placement="right">
      <n-drawer-content title="佣金变更历史" closable>
        <n-data-table :columns="logColumns" :data="logData" :pagination="false" size="small" />
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NIcon,
  NSpace,
  NTag,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { CheckmarkCircleOutline, CloseCircleOutline, ListOutline } from '@vicons/ionicons5'
import { useUserStore } from '@/stores/user'
import {
  realtyApi,
  type AllianceOption,
  type CommissionChangeLogRecord,
  type CommissionCreateParams,
  type CommissionRecord,
  type ProjectOption
} from '@/api/realty'

const message = useMessage()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<CommissionRecord[]>([])
const projectOptions = ref<ProjectOption[]>([])
const allianceOptions = ref<AllianceOption[]>([])

const searchForm = reactive<{
  projectId: number | null
  allianceId: number | null
  auditStatus: number | null
}>({
  projectId: null,
  allianceId: null,
  auditStatus: null
})

const auditStatusOptions = [
  { label: '待审核', value: 1 },
  { label: '审核通过', value: 2 },
  { label: '审核驳回', value: 3 }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const createVisible = ref(false)
const createFormRef = ref<FormInst | null>(null)
const createForm = reactive<{
  dealId: number | null
  projectId: number | null
  allianceId: number | null
  receivableAmount: number | null
  payableAmount: number | null
  platformFee: number | null
  detail: {
    baseCommission: number | null
    jumpPointCommission: number | null
    cashReward: number | null
    firstDealReward: number | null
    platformReward: number | null
  }
}>({
  dealId: null,
  projectId: null,
  allianceId: null,
  receivableAmount: 0,
  payableAmount: 0,
  platformFee: 0,
  detail: {
    baseCommission: null,
    jumpPointCommission: null,
    cashReward: null,
    firstDealReward: null,
    platformReward: null
  }
})

const createRules: FormRules = {
  dealId: [{ required: true, type: 'number', message: '请输入成交记录ID', trigger: 'change' }],
  projectId: [{ required: true, type: 'number', message: '请选择项目', trigger: 'change' }],
  allianceId: [{ required: true, type: 'number', message: '请选择联盟商', trigger: 'change' }],
  receivableAmount: [{ required: true, type: 'number', message: '请输入应收佣金', trigger: 'change' }],
  payableAmount: [{ required: true, type: 'number', message: '请输入应结佣金', trigger: 'change' }],
  platformFee: [{ required: true, type: 'number', message: '请输入平台费', trigger: 'change' }]
}

const rejectVisible = ref(false)
const rejectRemark = ref('')
const currentRejectId = ref<number | null>(null)

const logVisible = ref(false)
const logData = ref<CommissionChangeLogRecord[]>([])

const columns: DataTableColumns<CommissionRecord> = [
  { title: '佣金ID', key: 'id', width: 90 },
  { title: '成交ID', key: 'dealId', width: 90 },
  { title: '项目ID', key: 'projectId', width: 90 },
  { title: '联盟商ID', key: 'allianceId', width: 100 },
  { title: '应收佣金', key: 'receivableAmount', width: 120 },
  { title: '应结佣金', key: 'payableAmount', width: 120 },
  { title: '平台费', key: 'platformFee', width: 110 },
  {
    title: '审核状态',
    key: 'auditStatus',
    width: 120,
    render: row => renderStatus(row.auditStatus)
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
    width: 260,
    render: row =>
      h(NSpace, null, {
        default: () => [
          ...(row.auditStatus === 1
            ? [
                h(
                  NButton,
                  {
                    size: 'small',
                    type: 'success',
                    tertiary: true,
                    onClick: () => approveCommission(row)
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
                    onClick: () => openRejectModal(row)
                  },
                  {
                    icon: () => h(NIcon, null, { default: () => h(CloseCircleOutline) }),
                    default: () => '驳回'
                  }
                )
              ]
            : []),
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => openLogDrawer(row)
            },
            {
              icon: () => h(NIcon, null, { default: () => h(ListOutline) }),
              default: () => '变更历史'
            }
          )
        ]
      })
  }
]

const logColumns: DataTableColumns<CommissionChangeLogRecord> = [
  { title: '字段', key: 'fieldName', width: 140 },
  { title: '旧值', key: 'oldValue' },
  { title: '新值', key: 'newValue' },
  { title: '操作人ID', key: 'operatorId', width: 110 },
  {
    title: '变更时间',
    key: 'changeTime',
    width: 170,
    render: row => formatDateTime(row.changeTime)
  }
]

function rowKey(row: CommissionRecord) {
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

function renderStatus(status: number) {
  const map: Record<number, { text: string; type: 'warning' | 'success' | 'error' }> = {
    1: { text: '待审核', type: 'warning' },
    2: { text: '审核通过', type: 'success' },
    3: { text: '审核驳回', type: 'error' }
  }
  const item = map[status] || { text: '-', type: 'warning' as const }
  return h(NTag, { type: item.type, size: 'small' }, { default: () => item.text })
}

async function loadOptions() {
  const [projects, alliances] = await Promise.all([
    realtyApi.searchProjects(''),
    realtyApi.alliancePage({ page: 1, pageSize: 200 })
  ])
  projectOptions.value = projects
  allianceOptions.value = alliances.list || []
}

async function handleProjectSearch(keyword: string) {
  projectOptions.value = await realtyApi.searchProjects(keyword)
}

async function handleAllianceSearch(keyword: string) {
  const res = await realtyApi.alliancePage({
    page: 1,
    pageSize: 200,
    companyName: keyword || undefined
  })
  allianceOptions.value = res.list || []
}

async function loadTableData() {
  loading.value = true
  try {
    const res = await realtyApi.commissionPage({
      page: pagination.page,
      pageSize: pagination.pageSize,
      projectId: searchForm.projectId || undefined,
      allianceId: searchForm.allianceId || undefined,
      auditStatus: searchForm.auditStatus || undefined
    })
    tableData.value = res.list || []
    pagination.itemCount = res.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadTableData()
}

function handleReset() {
  searchForm.projectId = null
  searchForm.allianceId = null
  searchForm.auditStatus = null
  handleSearch()
}

function handlePageChange(page: number) {
  pagination.page = page
  loadTableData()
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadTableData()
}

function resetCreateForm() {
  createForm.dealId = null
  createForm.projectId = null
  createForm.allianceId = null
  createForm.receivableAmount = 0
  createForm.payableAmount = 0
  createForm.platformFee = 0
  createForm.detail.baseCommission = null
  createForm.detail.jumpPointCommission = null
  createForm.detail.cashReward = null
  createForm.detail.firstDealReward = null
  createForm.detail.platformReward = null
}

function openCreateModal() {
  resetCreateForm()
  createVisible.value = true
}

async function submitCreate() {
  await createFormRef.value?.validate()

  if (
    !createForm.dealId ||
    !createForm.projectId ||
    !createForm.allianceId ||
    createForm.receivableAmount === null ||
    createForm.payableAmount === null ||
    createForm.platformFee === null
  ) {
    return
  }

  const payload: CommissionCreateParams = {
    dealId: createForm.dealId,
    projectId: createForm.projectId,
    allianceId: createForm.allianceId,
    receivableAmount: createForm.receivableAmount,
    payableAmount: createForm.payableAmount,
    platformFee: createForm.platformFee,
    detail: {
      baseCommission: createForm.detail.baseCommission ?? undefined,
      jumpPointCommission: createForm.detail.jumpPointCommission ?? undefined,
      cashReward: createForm.detail.cashReward ?? undefined,
      firstDealReward: createForm.detail.firstDealReward ?? undefined,
      platformReward: createForm.detail.platformReward ?? undefined
    }
  }

  submitting.value = true
  try {
    await realtyApi.commissionCreate(payload)
    message.success('佣金录入成功')
    createVisible.value = false
    loadTableData()
  } finally {
    submitting.value = false
  }
}

async function approveCommission(row: CommissionRecord) {
  await realtyApi.commissionAudit({
    commissionId: row.id,
    approved: true,
    auditorId: userStore.user?.id
  })
  message.success('审核通过')
  loadTableData()
}

function openRejectModal(row: CommissionRecord) {
  currentRejectId.value = row.id
  rejectRemark.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!currentRejectId.value) {
    return
  }
  if (!rejectRemark.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  await realtyApi.commissionAudit({
    commissionId: currentRejectId.value,
    approved: false,
    remark: rejectRemark.value.trim(),
    auditorId: userStore.user?.id
  })
  rejectVisible.value = false
  message.success('已驳回')
  loadTableData()
}

async function openLogDrawer(row: CommissionRecord) {
  logData.value = await realtyApi.commissionChangeLog(row.id)
  logVisible.value = true
}

onMounted(async () => {
  await loadOptions()
  await loadTableData()
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}
</style>
