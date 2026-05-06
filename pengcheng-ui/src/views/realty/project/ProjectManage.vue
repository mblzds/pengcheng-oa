<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card>
        <div class="search-form">
          <n-form inline :model="searchForm">
            <n-form-item label="项目名称">
              <n-input v-model:value="searchForm.projectName" clearable placeholder="请输入项目名称" />
            </n-form-item>
            <n-form-item label="片区">
              <n-input v-model:value="searchForm.district" clearable placeholder="请输入片区" />
            </n-form-item>
            <n-form-item label="类型">
              <n-select v-model:value="searchForm.projectType" :options="projectTypeOptions" clearable style="width: 160px" />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="searchForm.status" :options="statusOptions" clearable style="width: 160px" />
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
          <n-button type="primary" @click="openCreateModal">新建项目</n-button>
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

      <n-card title="项目到期提醒列表">
        <n-data-table :columns="remindColumns" :data="expiringProjects" :pagination="false" size="small" />
      </n-card>
    </n-space>

    <n-modal v-model:show="modalVisible" preset="card" :title="modalTitle" style="width: 760px">
      <n-form ref="formRef" :model="formData" :rules="formRules" label-placement="left" label-width="110">
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="项目名称" path="projectName">
            <n-input v-model:value="formData.projectName" />
          </n-form-item-gi>
          <n-form-item-gi label="开发商" path="developerName">
            <n-input v-model:value="formData.developerName" />
          </n-form-item-gi>
          <n-form-item-gi label="项目地址" path="address" :span="2">
            <n-input v-model:value="formData.address" />
          </n-form-item-gi>
          <n-form-item-gi label="项目类型" path="projectType">
            <n-select v-model:value="formData.projectType" :options="projectTypeOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="项目状态" path="status">
            <n-select v-model:value="formData.status" :options="statusOptions" />
          </n-form-item-gi>
          <n-form-item-gi label="所属片区" path="district">
            <n-input v-model:value="formData.district" />
          </n-form-item-gi>
          <n-form-item-gi label="联系驻场" path="contactPerson">
            <n-input v-model:value="formData.contactPerson" />
          </n-form-item-gi>
          <n-form-item-gi label="联系电话" path="contactPhone">
            <n-input v-model:value="formData.contactPhone" />
          </n-form-item-gi>
          <n-form-item-gi label="代理开始" path="agencyStartDate">
            <n-date-picker v-model:value="formData.agencyStartDate" type="date" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="代理结束" path="agencyEndDate">
            <n-date-picker v-model:value="formData.agencyEndDate" type="date" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="项目介绍" path="description" :span="2">
            <n-input v-model:value="formData.description" type="textarea" :rows="3" />
          </n-form-item-gi>
        </n-grid>

        <n-divider title-placement="left">佣金规则配置</n-divider>
        <n-grid :cols="2" :x-gap="12">
          <n-form-item-gi label="基础佣金比例">
            <n-input-number v-model:value="ruleForm.baseRate" :min="0" :step="0.001" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="跳点规则(JSON)">
            <n-input v-model:value="ruleForm.jumpPointRules" placeholder='例如: [{"min":0,"max":1000000,"rate":0.02}]' />
          </n-form-item-gi>
          <n-form-item-gi label="现金奖">
            <n-input-number v-model:value="ruleForm.cashReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="开单奖">
            <n-input-number v-model:value="ruleForm.firstDealReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="平台奖励">
            <n-input-number v-model:value="ruleForm.platformReward" :min="0" style="width: 100%" />
          </n-form-item-gi>
        </n-grid>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="modalVisible = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitProject">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NIcon, NSpace, NTag, useMessage, type DataTableColumns, type FormInst, type FormRules } from 'naive-ui'
import { CreateOutline } from '@vicons/ionicons5'
import { realtyApi, type ProjectCreateParams, type ProjectRecord } from '@/api/realty'

const message = useMessage()

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<ProjectRecord[]>([])

const searchForm = reactive<{
  projectName: string
  district: string
  projectType: number | null
  status: number | null
}>({
  projectName: '',
  district: '',
  projectType: null,
  status: null
})

const projectTypeOptions = [
  { label: '住宅', value: 1 },
  { label: '商业', value: 2 },
  { label: '办公', value: 3 },
  { label: '综合体', value: 4 }
]

const statusOptions = [
  { label: '在售', value: 1 },
  { label: '待售', value: 2 },
  { label: '售罄', value: 3 },
  { label: '已到期', value: 4 }
]

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const modalVisible = ref(false)
const modalTitle = ref('新建项目')
const formRef = ref<FormInst | null>(null)

const formData = reactive<{
  id: number | null
  projectName: string
  developerName: string
  address: string
  projectType: number
  status: number
  district: string
  agencyStartDate: number | null
  agencyEndDate: number | null
  contactPerson: string
  contactPhone: string
  description: string
}>({
  id: null,
  projectName: '',
  developerName: '',
  address: '',
  projectType: 1,
  status: 1,
  district: '',
  agencyStartDate: null,
  agencyEndDate: null,
  contactPerson: '',
  contactPhone: '',
  description: ''
})

const ruleForm = reactive({
  baseRate: null as number | null,
  jumpPointRules: '',
  cashReward: null as number | null,
  firstDealReward: null as number | null,
  platformReward: null as number | null
})

const formRules: FormRules = {
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  projectType: [{ required: true, type: 'number', message: '请选择项目类型', trigger: 'change' }],
  status: [{ required: true, type: 'number', message: '请选择项目状态', trigger: 'change' }]
}

const columns: DataTableColumns<ProjectRecord> = [
  { title: '项目名称', key: 'projectName', width: 160 },
  { title: '片区', key: 'district', width: 100 },
  {
    title: '类型',
    key: 'projectType',
    width: 100,
    render: row => projectTypeText(row.projectType)
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    render: row => renderStatus(row.status)
  },
  { title: '代理结束', key: 'agencyEndDate', width: 120 },
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
          onClick: () => openEditModal(row)
        },
        {
          icon: () => h(NIcon, null, { default: () => h(CreateOutline) }),
          default: () => '编辑'
        }
      )
  }
]

const remindColumns: DataTableColumns<ProjectRecord> = [
  { title: '项目名称', key: 'projectName' },
  { title: '片区', key: 'district', width: 120 },
  { title: '代理结束', key: 'agencyEndDate', width: 140 },
  {
    title: '剩余天数',
    key: 'leftDays',
    width: 100,
    render: row => daysLeft(row.agencyEndDate)
  }
]

const expiringProjects = computed(() => {
  const today = new Date()
  const limit = new Date()
  limit.setDate(limit.getDate() + 30)

  return tableData.value.filter(item => {
    if (!item.agencyEndDate) return false
    const end = new Date(item.agencyEndDate)
    if (Number.isNaN(end.getTime())) return false
    return end >= today && end <= limit
  })
})

function rowKey(row: ProjectRecord) {
  return row.id
}

function projectTypeText(type?: number) {
  return projectTypeOptions.find(i => i.value === type)?.label || '-'
}

function statusText(status?: number) {
  return statusOptions.find(i => i.value === status)?.label || '-'
}

function renderStatus(status?: number) {
  const type: 'success' | 'warning' | 'error' = status === 1 ? 'success' : status === 4 ? 'error' : 'warning'
  return h(NTag, { size: 'small', type }, { default: () => statusText(status) })
}

function dateToYmd(ms?: number | null) {
  if (!ms) return undefined
  const date = new Date(ms)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function daysLeft(endDate?: string) {
  if (!endDate) return '-'
  const end = new Date(endDate)
  if (Number.isNaN(end.getTime())) return '-'
  const now = new Date()
  const diff = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  return diff < 0 ? '已过期' : `${diff} 天`
}

async function loadData() {
  loading.value = true
  try {
    const res = await realtyApi.projectPage({
      page: pagination.page,
      pageSize: pagination.pageSize,
      projectName: searchForm.projectName || undefined,
      district: searchForm.district || undefined,
      projectType: searchForm.projectType || undefined,
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
  searchForm.projectName = ''
  searchForm.district = ''
  searchForm.projectType = null
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

function resetForm() {
  formData.id = null
  formData.projectName = ''
  formData.developerName = ''
  formData.address = ''
  formData.projectType = 1
  formData.status = 1
  formData.district = ''
  formData.agencyStartDate = null
  formData.agencyEndDate = null
  formData.contactPerson = ''
  formData.contactPhone = ''
  formData.description = ''

  ruleForm.baseRate = null
  ruleForm.jumpPointRules = ''
  ruleForm.cashReward = null
  ruleForm.firstDealReward = null
  ruleForm.platformReward = null
}

function openCreateModal() {
  modalTitle.value = '新建项目'
  resetForm()
  modalVisible.value = true
}

async function openEditModal(row: ProjectRecord) {
  modalTitle.value = '编辑项目'
  resetForm()

  formData.id = row.id
  formData.projectName = row.projectName || ''
  formData.developerName = row.developerName || ''
  formData.address = row.address || ''
  formData.projectType = row.projectType || 1
  formData.status = row.status || 1
  formData.district = row.district || ''
  formData.agencyStartDate = row.agencyStartDate ? new Date(row.agencyStartDate).getTime() : null
  formData.agencyEndDate = row.agencyEndDate ? new Date(row.agencyEndDate).getTime() : null
  formData.contactPerson = row.contactPerson || ''
  formData.contactPhone = row.contactPhone || ''
  formData.description = row.description || ''

  try {
    const activeRule = await realtyApi.activeProjectCommissionRule(row.id)
    if (activeRule) {
      ruleForm.baseRate = activeRule.baseRate ?? null
      ruleForm.jumpPointRules = activeRule.jumpPointRules || ''
      ruleForm.cashReward = activeRule.cashReward ?? null
      ruleForm.firstDealReward = activeRule.firstDealReward ?? null
      ruleForm.platformReward = activeRule.platformReward ?? null
    }
  } catch {
    // ignore
  }

  modalVisible.value = true
}

async function submitProject() {
  await formRef.value?.validate()

  const payload: ProjectCreateParams = {
    id: formData.id || undefined,
    projectName: formData.projectName,
    developerName: formData.developerName || undefined,
    address: formData.address || undefined,
    projectType: formData.projectType,
    status: formData.status,
    district: formData.district || undefined,
    agencyStartDate: dateToYmd(formData.agencyStartDate),
    agencyEndDate: dateToYmd(formData.agencyEndDate),
    contactPerson: formData.contactPerson || undefined,
    contactPhone: formData.contactPhone || undefined,
    description: formData.description || undefined
  }

  submitting.value = true
  try {
    let projectId = formData.id || 0
    if (formData.id) {
      await realtyApi.projectUpdate(payload)
      message.success('项目更新成功')
    } else {
      projectId = await realtyApi.projectCreate(payload)
      message.success('项目创建成功')
    }

    if (projectId && (ruleForm.baseRate !== null || ruleForm.jumpPointRules || ruleForm.cashReward !== null || ruleForm.firstDealReward !== null || ruleForm.platformReward !== null)) {
      await realtyApi.saveProjectCommissionRule({
        projectId,
        baseRate: ruleForm.baseRate ?? undefined,
        jumpPointRules: ruleForm.jumpPointRules || undefined,
        cashReward: ruleForm.cashReward ?? undefined,
        firstDealReward: ruleForm.firstDealReward ?? undefined,
        platformReward: ruleForm.platformReward ?? undefined
      })
    }

    modalVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadData()
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
