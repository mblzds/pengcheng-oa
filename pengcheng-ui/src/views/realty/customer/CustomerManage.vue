<template>
  <div class="page-container">
    <n-card>
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="客户姓氏">
            <n-input v-model:value="searchForm.customerName" placeholder="请输入客户姓氏" clearable />
          </n-form-item>
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
              placeholder="请选择项目"
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
              placeholder="请选择联盟商"
              style="width: 220px"
            />
          </n-form-item>
          <n-form-item label="状态">
            <n-select
              v-model:value="searchForm.status"
              :options="statusOptions"
              clearable
              placeholder="请选择状态"
              style="width: 160px"
            />
          </n-form-item>
          <n-form-item label="报备时间">
            <n-date-picker
              v-model:value="searchForm.timeRange"
              type="datetimerange"
              clearable
              style="width: 320px"
            />
          </n-form-item>
          <n-form-item>
            <n-space>
              <n-button type="primary" @click="handleSearch">
                <template #icon><n-icon><SearchOutline /></n-icon></template>
                搜索
              </n-button>
              <n-button @click="handleReset">
                <template #icon><n-icon><RefreshOutline /></n-icon></template>
                重置
              </n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </div>

      <div class="table-toolbar">
        <n-button type="primary" @click="openCreateModal">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          报备录入
        </n-button>
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
      <n-drawer-content title="客户详情" closable>
        <n-space vertical :size="16">
          <n-card size="small" title="报备信息">
            <n-descriptions :column="2" label-placement="left" bordered>
              <n-descriptions-item label="报备编号">{{ detailRow?.reportNo || '-' }}</n-descriptions-item>
              <n-descriptions-item label="客户姓氏">{{ detailRow?.customerName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="联系方式">{{ detailRow?.phoneMasked || '-' }}</n-descriptions-item>
              <n-descriptions-item label="带看人数">{{ detailRow?.visitCount || '-' }}</n-descriptions-item>
              <n-descriptions-item label="带看时间">{{ formatDateTime(detailRow?.visitTime) }}</n-descriptions-item>
              <n-descriptions-item label="联盟商">{{ detailRow?.allianceName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="经纪人">{{ detailRow?.agentName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="状态">{{ statusText(detailRow?.status) }}</n-descriptions-item>
              <n-descriptions-item label="AI成交概率">{{ aiDealProbability }}</n-descriptions-item>
              <n-descriptions-item label="评分更新时间">{{ formatDateTime(aiScoreUpdateTime) }}</n-descriptions-item>
            </n-descriptions>
          </n-card>

          <n-card size="small" title="到访记录">
            <n-data-table
              :columns="visitColumns"
              :data="visitRecords"
              size="small"
              :pagination="false"
            />
          </n-card>

          <n-card size="small" title="成交信息">
            <n-data-table
              :columns="dealColumns"
              :data="dealRecords"
              size="small"
              :pagination="false"
            />
          </n-card>

          <n-card size="small" title="跟进历史">
            <n-timeline>
              <n-timeline-item
                v-for="item in followTimeline"
                :key="item.title + item.time"
                :title="item.title"
                :time="item.time"
                :content="item.content"
              />
            </n-timeline>
          </n-card>
        </n-space>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="createVisible" preset="card" title="客户报备录入" style="width: 680px">
      <n-form ref="createFormRef" :model="createForm" :rules="createRules" label-placement="left" label-width="110">
        <n-form-item label="带看项目" path="projectIds">
          <n-select
            v-model:value="createForm.projectIds"
            :options="projectOptions"
            label-field="projectName"
            value-field="id"
            multiple
            filterable
            remote
            @search="handleProjectSearch"
            placeholder="请选择带看项目"
          />
        </n-form-item>
        <n-form-item label="客户姓氏" path="customerName">
          <n-input v-model:value="createForm.customerName" placeholder="请输入客户姓氏" />
        </n-form-item>
        <n-form-item label="联系方式" path="phone">
          <n-input v-model:value="createForm.phone" placeholder="请输入手机号" />
        </n-form-item>
        <n-form-item label="带看人数" path="visitCount">
          <n-input-number v-model:value="createForm.visitCount" :min="1" style="width: 100%" />
        </n-form-item>
        <n-form-item label="带看时间" path="visitTime">
          <n-date-picker v-model:value="createForm.visitTime" type="datetime" clearable style="width: 100%" />
        </n-form-item>
        <n-form-item label="带看公司" path="allianceId">
          <n-select
            v-model:value="createForm.allianceId"
            :options="allianceOptions"
            label-field="companyName"
            value-field="id"
            filterable
            remote
            @search="handleAllianceSearch"
            placeholder="请选择带看公司"
          />
        </n-form-item>
        <n-form-item label="经纪人姓名" path="agentName">
          <n-input v-model:value="createForm.agentName" placeholder="请输入经纪人姓名" />
        </n-form-item>
        <n-form-item label="经纪人联系方式" path="agentPhone">
          <n-input v-model:value="createForm.agentPhone" placeholder="请输入经纪人手机号" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="createVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleCreateSubmit">提交</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
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
import { AddOutline, EyeOutline, RefreshOutline, SearchOutline } from '@vicons/ionicons5'
import {
  realtyApi,
  type AllianceOption,
  type CustomerCreateParams,
  type CustomerDealRecord,
  type CustomerRecord,
  type CustomerVisitRecord,
  type ProjectOption
} from '@/api/realty'

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)

const tableData = ref<CustomerRecord[]>([])
const projectOptions = ref<ProjectOption[]>([])
const allianceOptions = ref<AllianceOption[]>([])
const visitRecords = ref<CustomerVisitRecord[]>([])
const dealRecords = ref<CustomerDealRecord[]>([])

const searchForm = reactive<{
  customerName: string
  projectId: number | null
  allianceId: number | null
  status: number | null
  timeRange: [number, number] | null
}>({
  customerName: '',
  projectId: null,
  allianceId: null,
  status: null,
  timeRange: null
})

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const statusOptions = [
  { label: '已报备', value: 1 },
  { label: '已到访', value: 2 },
  { label: '已成交', value: 3 }
]

const detailVisible = ref(false)
const detailRow = ref<CustomerRecord | null>(null)
const aiScore = ref<number | null>(null)
const aiScoreUpdateTime = ref<string>('')

const createVisible = ref(false)
const createFormRef = ref<FormInst | null>(null)

const createForm = reactive<{
  projectIds: number[]
  customerName: string
  phone: string
  visitCount: number | null
  visitTime: number | null
  allianceId: number | null
  agentName: string
  agentPhone: string
}>({
  projectIds: [],
  customerName: '',
  phone: '',
  visitCount: 1,
  visitTime: Date.now(),
  allianceId: null,
  agentName: '',
  agentPhone: ''
})

const createRules: FormRules = {
  projectIds: [{ required: true, type: 'array', min: 1, message: '请选择带看项目', trigger: 'change' }],
  customerName: [{ required: true, message: '请输入客户姓氏', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  visitCount: [{ required: true, type: 'number', message: '请输入带看人数', trigger: 'change' }],
  visitTime: [{ required: true, type: 'number', message: '请选择带看时间', trigger: 'change' }],
  allianceId: [{ required: true, type: 'number', message: '请选择带看公司', trigger: 'change' }],
  agentName: [{ required: true, message: '请输入经纪人姓名', trigger: 'blur' }],
  agentPhone: [{ required: true, message: '请输入经纪人联系方式', trigger: 'blur' }]
}

const columns: DataTableColumns<CustomerRecord> = [
  { title: '报备编号', key: 'reportNo', width: 160 },
  { title: '客户姓氏', key: 'customerName', width: 120 },
  { title: '联系方式', key: 'phoneMasked', width: 120 },
  { title: '带看人数', key: 'visitCount', width: 90 },
  {
    title: '带看时间',
    key: 'visitTime',
    width: 170,
    render: row => formatDateTime(row.visitTime)
  },
  { title: '联盟商', key: 'allianceName', width: 160 },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => renderStatusTag(row.status)
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
    width: 120,
    render: row =>
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
      )
  }
]

const visitColumns: DataTableColumns<CustomerVisitRecord> = [
  {
    title: '到访时间',
    key: 'actualVisitTime',
    render: row => formatDateTime(row.actualVisitTime)
  },
  { title: '到访人数', key: 'actualVisitCount', width: 90 },
  { title: '接待人员', key: 'receptionist', width: 120 },
  { title: '备注', key: 'remark' }
]

const dealColumns: DataTableColumns<CustomerDealRecord> = [
  { title: '房号', key: 'roomNo', width: 110 },
  { title: '成交金额', key: 'dealAmount', width: 120 },
  {
    title: '成交时间',
    key: 'dealTime',
    width: 170,
    render: row => formatDateTime(row.dealTime)
  },
  {
    title: '签约状态',
    key: 'signStatus',
    width: 100,
    render: row => (row.signStatus === 1 ? '已签约' : '未签约')
  },
  {
    title: '认购类型',
    key: 'subscribeType',
    width: 100,
    render: row => (row.subscribeType === 1 ? '小订' : '大定')
  }
]

const followTimeline = computed(() => {
  const row = detailRow.value
  if (!row) {
    return []
  }

  const timeline: Array<{ title: string; time: string; content: string }> = []
  if (row.createTime) {
    timeline.push({
      title: '报备创建',
      time: formatDateTime(row.createTime),
      content: `生成报备编号 ${row.reportNo || '-'}，客户状态 ${statusText(1)}`
    })
  }
  if (row.visitTime) {
    timeline.push({
      title: '带看计划',
      time: formatDateTime(row.visitTime),
      content: `计划带看人数 ${row.visitCount || 0} 人`
    })
  }
  if (row.lastFollowTime) {
    timeline.push({
      title: '最近跟进',
      time: formatDateTime(row.lastFollowTime),
      content: '客户跟进时间更新'
    })
  }
  if (row.protectionExpireTime) {
    timeline.push({
      title: '保护期截止',
      time: formatDateTime(row.protectionExpireTime),
      content: `当前池类型：${row.poolType === 1 ? '公海' : '私海'}`
    })
  }

  return timeline
})

function rowKey(row: CustomerRecord) {
  return row.id
}

function renderStatusTag(status?: number) {
  const tagType: 'default' | 'info' | 'success' = status === 1 ? 'info' : status === 2 ? 'default' : 'success'
  return h(
    NTag,
    {
      size: 'small',
      type: tagType
    },
    { default: () => statusText(status) }
  )
}

function statusText(status?: number) {
  if (status === 1) return '已报备'
  if (status === 2) return '已到访'
  if (status === 3) return '已成交'
  return '-'
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

function formatForApi(ms: number) {
  const date = new Date(ms)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hour}:${minute}:${second}`
}

async function loadOptions() {
  const [projects, alliances] = await Promise.all([
    realtyApi.searchProjects(''),
    realtyApi.searchAlliances('')
  ])
  projectOptions.value = projects
  allianceOptions.value = alliances
}

async function loadTableData() {
  loading.value = true
  try {
    const [startTime, endTime] = searchForm.timeRange || []
    const res = await realtyApi.customerPage({
      page: pagination.page,
      pageSize: pagination.pageSize,
      customerName: searchForm.customerName || undefined,
      projectId: searchForm.projectId || undefined,
      allianceId: searchForm.allianceId || undefined,
      status: searchForm.status || undefined,
      startTime: startTime ? formatForApi(startTime) : undefined,
      endTime: endTime ? formatForApi(endTime) : undefined
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
  searchForm.customerName = ''
  searchForm.projectId = null
  searchForm.allianceId = null
  searchForm.status = null
  searchForm.timeRange = null
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

async function handleProjectSearch(keyword: string) {
  projectOptions.value = await realtyApi.searchProjects(keyword)
}

async function handleAllianceSearch(keyword: string) {
  allianceOptions.value = await realtyApi.searchAlliances(keyword)
}

async function openDetail(row: CustomerRecord) {
  detailRow.value = row
  detailVisible.value = true
  aiScore.value = null
  aiScoreUpdateTime.value = ''
  const [visits, deals, probability] = await Promise.all([
    realtyApi.customerVisits(row.id),
    realtyApi.customerDeals(row.id),
    realtyApi.customerDealProbability(row.id).catch(() => null)
  ])
  visitRecords.value = visits
  dealRecords.value = deals
  if (probability !== null && probability !== undefined) {
    aiScore.value = Number(probability)
    aiScoreUpdateTime.value = new Date().toISOString()
  }
}

const aiDealProbability = computed(() => {
  if (aiScore.value === null || Number.isNaN(aiScore.value)) {
    return '-'
  }
  return `${(aiScore.value * 100).toFixed(1)}%`
})

function resetCreateForm() {
  createForm.projectIds = []
  createForm.customerName = ''
  createForm.phone = ''
  createForm.visitCount = 1
  createForm.visitTime = Date.now()
  createForm.allianceId = null
  createForm.agentName = ''
  createForm.agentPhone = ''
}

function openCreateModal() {
  resetCreateForm()
  createVisible.value = true
}

async function handleCreateSubmit() {
  await createFormRef.value?.validate()

  if (!createForm.visitTime || !createForm.visitCount || !createForm.allianceId) {
    return
  }

  const payload: CustomerCreateParams = {
    projectIds: createForm.projectIds,
    customerName: createForm.customerName,
    phone: createForm.phone,
    visitCount: createForm.visitCount,
    visitTime: formatForApi(createForm.visitTime),
    allianceId: createForm.allianceId,
    agentName: createForm.agentName,
    agentPhone: createForm.agentPhone
  }

  submitting.value = true
  try {
    const res = await realtyApi.createCustomer(payload)
    message.success(`报备成功，编号：${res.reportNo}`)
    createVisible.value = false
    handleSearch()
  } finally {
    submitting.value = false
  }
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
