<template>
  <div class="page-container">
    <n-card>
      <div class="search-form">
        <n-form inline :model="searchForm" label-placement="left">
          <n-form-item label="联盟商名称">
            <n-input v-model:value="searchForm.companyName" placeholder="请输入联盟商名称" clearable />
          </n-form-item>
          <n-form-item label="状态">
            <n-select v-model:value="searchForm.status" :options="statusOptions" clearable style="width: 140px" />
          </n-form-item>
          <n-form-item label="等级">
            <n-select v-model:value="searchForm.level" :options="levelOptions" clearable style="width: 140px" />
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
        <n-button type="primary" @click="openCreateModal">新增联盟商</n-button>
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
      <n-drawer-content title="联盟商详情" closable>
        <n-space vertical :size="16">
          <n-card size="small" title="基础信息">
            <n-descriptions :column="2" bordered>
              <n-descriptions-item label="公司名称">{{ detailData?.companyName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="状态">{{ detailData?.status === 1 ? '启用' : '停用' }}</n-descriptions-item>
              <n-descriptions-item label="负责人">{{ detailData?.contactName || '-' }}</n-descriptions-item>
              <n-descriptions-item label="联系方式">{{ detailData?.contactPhone || '-' }}</n-descriptions-item>
              <n-descriptions-item label="办公地址">{{ detailData?.officeAddress || '-' }}</n-descriptions-item>
              <n-descriptions-item label="人员规模">{{ detailData?.staffSize || '-' }}</n-descriptions-item>
              <n-descriptions-item label="联盟等级">{{ levelText(detailData?.level) }}</n-descriptions-item>
              <n-descriptions-item label="创建时间">{{ formatDateTime(detailData?.createTime) }}</n-descriptions-item>
            </n-descriptions>
          </n-card>

          <n-card size="small" title="运营数据统计">
            <div ref="statsChartRef" class="stats-chart"></div>
          </n-card>
        </n-space>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" style="width: 640px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="100">
        <n-form-item label="公司名称" path="companyName">
          <n-input v-model:value="formData.companyName" placeholder="请输入公司名称" />
        </n-form-item>
        <n-form-item label="办公地址" path="officeAddress">
          <n-input v-model:value="formData.officeAddress" placeholder="请输入办公地址" />
        </n-form-item>
        <n-form-item label="负责人" path="contactName">
          <n-input v-model:value="formData.contactName" placeholder="请输入负责人" />
        </n-form-item>
        <n-form-item label="联系方式" path="contactPhone">
          <n-input v-model:value="formData.contactPhone" placeholder="请输入联系方式" />
        </n-form-item>
        <n-form-item label="人员规模" path="staffSize">
          <n-input-number v-model:value="formData.staffSize" :min="1" style="width: 100%" />
        </n-form-item>
        <n-form-item label="联盟等级" path="level">
          <n-select v-model:value="formData.level" :options="levelOptions" placeholder="请选择联盟等级" />
        </n-form-item>
        <n-form-item label="渠道人员ID" path="channelUserId">
          <n-input-number v-model:value="formData.channelUserId" :min="1" clearable style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref, h } from 'vue'
import {
  NButton,
  NIcon,
  NSpace,
  NSwitch,
  NTag,
  useMessage,
  type DataTableColumns,
  type FormInst,
  type FormRules
} from 'naive-ui'
import { CreateOutline, EyeOutline } from '@vicons/ionicons5'
import {
  realtyApi,
  type AllianceCreateParams,
  type AllianceRecord,
  type AllianceStatsRecord
} from '@/api/realty'

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<AllianceRecord[]>([])

const searchForm = reactive<{
  companyName: string
  status: number | null
  level: number | null
}>({
  companyName: '',
  status: null,
  level: null
})

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
]

const levelOptions = [
  { label: '普通', value: 1 },
  { label: '银牌', value: 2 },
  { label: '金牌', value: 3 },
  { label: '钻石', value: 4 }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const detailVisible = ref(false)
const detailData = ref<AllianceRecord | null>(null)
const statsData = ref<AllianceStatsRecord | null>(null)
const statsChartRef = ref<HTMLDivElement | null>(null)
let statsChart: any = null

const modalVisible = ref(false)
const modalTitle = ref('新增联盟商')
const formRef = ref<FormInst | null>(null)
const formData = reactive<{
  id: number | null
  companyName: string
  officeAddress: string
  contactName: string
  contactPhone: string
  staffSize: number | null
  level: number | null
  channelUserId: number | null
}>({
  id: null,
  companyName: '',
  officeAddress: '',
  contactName: '',
  contactPhone: '',
  staffSize: 1,
  level: 1,
  channelUserId: null
})

const formRules: FormRules = {
  companyName: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  officeAddress: [{ required: true, message: '请输入办公地址', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入负责人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  staffSize: [{ required: true, type: 'number', message: '请输入人员规模', trigger: 'change' }],
  level: [{ required: true, type: 'number', message: '请选择联盟等级', trigger: 'change' }]
}

const columns: DataTableColumns<AllianceRecord> = [
  { title: '公司名称', key: 'companyName', width: 180 },
  { title: '负责人', key: 'contactName', width: 120 },
  { title: '联系方式', key: 'contactPhone', width: 140 },
  {
    title: '联盟等级',
    key: 'level',
    width: 100,
    render: row => levelText(row.level)
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: row => h(NTag, { type: row.status === 1 ? 'success' : 'error', size: 'small' }, { default: () => (row.status === 1 ? '启用' : '停用') })
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
    width: 240,
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
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              onClick: () => openEditModal(row)
            },
            {
              icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
              default: () => '编辑'
            }
          ),
          h(NSwitch, {
            value: row.status === 1,
            onUpdateValue: (val: boolean) => handleStatusChange(row, val)
          })
        ]
      })
  }
]

function rowKey(row: AllianceRecord) {
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

function levelText(level?: number) {
  if (level === 1) return '普通'
  if (level === 2) return '银牌'
  if (level === 3) return '金牌'
  if (level === 4) return '钻石'
  return '-'
}

async function loadTableData() {
  loading.value = true
  try {
    const res = await realtyApi.alliancePage({
      page: pagination.page,
      pageSize: pagination.pageSize,
      companyName: searchForm.companyName || undefined,
      status: searchForm.status ?? undefined,
      level: searchForm.level ?? undefined
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
  searchForm.companyName = ''
  searchForm.status = null
  searchForm.level = null
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

function resetForm() {
  formData.id = null
  formData.companyName = ''
  formData.officeAddress = ''
  formData.contactName = ''
  formData.contactPhone = ''
  formData.staffSize = 1
  formData.level = 1
  formData.channelUserId = null
}

function openCreateModal() {
  modalTitle.value = '新增联盟商'
  resetForm()
  modalVisible.value = true
}

function openEditModal(row: AllianceRecord) {
  modalTitle.value = '编辑联盟商'
  formData.id = row.id
  formData.companyName = row.companyName || ''
  formData.officeAddress = row.officeAddress || ''
  formData.contactName = row.contactName || ''
  formData.contactPhone = row.contactPhone || ''
  formData.staffSize = row.staffSize || 1
  formData.level = row.level || 1
  formData.channelUserId = row.channelUserId || null
  modalVisible.value = true
}

async function handleSubmit() {
  await formRef.value?.validate()

  if (!formData.staffSize || !formData.level) {
    return
  }

  const payload: AllianceCreateParams = {
    id: formData.id || undefined,
    companyName: formData.companyName,
    officeAddress: formData.officeAddress,
    contactName: formData.contactName,
    contactPhone: formData.contactPhone,
    staffSize: formData.staffSize,
    level: formData.level,
    channelUserId: formData.channelUserId || undefined
  }

  submitting.value = true
  try {
    if (formData.id) {
      await realtyApi.allianceUpdate(payload)
      message.success('编辑成功')
    } else {
      await realtyApi.allianceCreate(payload)
      message.success('新增成功')
    }
    modalVisible.value = false
    loadTableData()
  } finally {
    submitting.value = false
  }
}

async function handleStatusChange(row: AllianceRecord, enabled: boolean) {
  try {
    if (enabled) {
      await realtyApi.allianceEnable(row.id)
      message.success('已启用')
    } else {
      await realtyApi.allianceDisable(row.id)
      message.success('已停用')
    }
    loadTableData()
  } catch {
    // 失败时回滚展示
    row.status = enabled ? 0 : 1
  }
}

async function openDetail(row: AllianceRecord) {
  detailVisible.value = true
  const [detail, stats] = await Promise.all([realtyApi.allianceDetail(row.id), realtyApi.allianceStats(row.id)])
  detailData.value = detail
  statsData.value = stats
  await nextTick()
  renderStatsChart()
}

function renderStatsChart() {
  if (!statsChartRef.value || !statsData.value) {
    return
  }

  const values = [
    Number(statsData.value.customerCount || 0),
    Number(statsData.value.dealCount || 0),
    Number(statsData.value.dealAmount || 0),
    Number(statsData.value.settledCommission || 0),
    Number(statsData.value.pendingCommission || 0)
  ]

  import('echarts').then(echarts => {
    if (!statsChart) {
      statsChart = echarts.init(statsChartRef.value)
    }
    statsChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['上客数', '成交数', '成交业绩', '已结佣', '待结佣']
      },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: values,
          itemStyle: {
            color: '#18a058'
          }
        }
      ]
    })
  }).catch(() => {
    // ignore
  })
}

onMounted(() => {
  loadTableData()
})

onUnmounted(() => {
  if (statsChart) {
    statsChart.dispose()
  }
})
</script>

<style scoped>
.search-form {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.stats-chart {
  width: 100%;
  height: 320px;
}
</style>
