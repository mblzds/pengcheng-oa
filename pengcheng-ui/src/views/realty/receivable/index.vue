<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-grid :cols="24" :x-gap="16" :y-gap="16">
        <n-gi :span="6">
          <n-card size="small">
            <n-statistic label="应收总额" :value="stats?.totalDue || 0" prefix="¥" />
          </n-card>
        </n-gi>
        <n-gi :span="6">
          <n-card size="small">
            <n-statistic label="已回总额" :value="stats?.totalPaid || 0" prefix="¥" />
          </n-card>
        </n-gi>
        <n-gi :span="6">
          <n-card size="small">
            <n-statistic label="未回总额" :value="stats?.totalUnpaid || 0" prefix="¥" />
          </n-card>
        </n-gi>
        <n-gi :span="6">
          <n-card size="small">
            <n-statistic label="逾期期数" :value="stats?.overdueCount || 0" />
          </n-card>
        </n-gi>
      </n-grid>

      <n-card>
        <n-tabs v-model:value="activeTab" type="line" animated>
          <n-tab-pane name="plans" tab="计划列表">
            <PlanList
              :plans="plans"
              :loading="planLoading"
              :pagination="pagination"
              @search="handleSearch"
              @reset="handleReset"
              @refresh="loadPlans"
              @open-create="showCreatePlan = true"
              @view-records="handleViewRecords"
              @page-change="handlePageChange"
              @page-size-change="handlePageSizeChange"
            />
          </n-tab-pane>
          <n-tab-pane name="records" tab="回款登记">
            <RecordRegister
              :selected-plan="selectedPlan"
              :records="records"
              :loading="recordLoading"
              :submitting="recordSubmitting"
              @refresh="refreshRecords"
              @submit="handleSubmitRecord"
            />
          </n-tab-pane>
          <n-tab-pane name="alerts" tab="逾期看板">
            <OverdueBoard
              :stats="stats"
              :alerts="alerts"
              :loading="alertLoading"
              :checking="manualChecking"
              @refresh="loadAlerts"
              @run-check="runManualCheck"
            />
          </n-tab-pane>
        </n-tabs>
      </n-card>
    </n-space>

    <n-modal v-model:show="showCreatePlan" preset="card" title="新建回款计划" style="width: 820px">
      <n-form label-placement="left" label-width="90">
        <n-form-item label="成交记录ID">
          <n-input-number v-model:value="createPlanForm.dealId" :min="1" style="width: 240px" />
        </n-form-item>

        <n-space vertical :size="12" style="width: 100%">
          <n-card v-for="(item, index) in createPlanForm.items" :key="index" size="small" :title="`分期 ${index + 1}`">
            <template #header-extra>
              <n-button text type="error" :disabled="createPlanForm.items.length === 1" @click="removePlanItem(index)">
                删除
              </n-button>
            </template>
            <n-grid :cols="24" :x-gap="12">
              <n-gi :span="6">
                <n-form-item label="期号">
                  <n-input-number v-model:value="item.periodNo" :min="1" style="width: 100%" />
                </n-form-item>
              </n-gi>
              <n-gi :span="8">
                <n-form-item label="分期名称">
                  <n-input v-model:value="item.periodName" placeholder="如：首付/尾款" />
                </n-form-item>
              </n-gi>
              <n-gi :span="10">
                <n-form-item label="应回日期">
                  <n-date-picker
                    v-model:value="item.dueDate"
                    type="date"
                    value-format="yyyy-MM-dd"
                    clearable
                    style="width: 100%"
                  />
                </n-form-item>
              </n-gi>
              <n-gi :span="12">
                <n-form-item label="应回金额">
                  <n-input-number v-model:value="item.dueAmount" :min="0" style="width: 100%" />
                </n-form-item>
              </n-gi>
              <n-gi :span="12">
                <n-form-item label="备注">
                  <n-input v-model:value="item.remark" placeholder="可选说明" />
                </n-form-item>
              </n-gi>
            </n-grid>
          </n-card>
        </n-space>

        <n-button dashed block style="margin-top: 12px" @click="addPlanItem">新增一期</n-button>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreatePlan = false">取消</n-button>
          <n-button type="primary" :loading="createSubmitting" @click="handleCreatePlan">保存计划</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import type {
  ReceivablePlanCreateItem,
  ReceivablePlanRecord,
  ReceivableRecordCreateParams,
  ReceivableRecordRecord,
  ReceivableStatsRecord,
  ReceivableAlertRecord
} from '@/api/receivable'
import { receivableApi } from '@/api/receivable'
import OverdueBoard from './OverdueBoard.vue'
import PlanList from './PlanList.vue'
import RecordRegister from './RecordRegister.vue'

const message = useMessage()

const activeTab = ref('plans')
const plans = ref<ReceivablePlanRecord[]>([])
const records = ref<ReceivableRecordRecord[]>([])
const alerts = ref<ReceivableAlertRecord[]>([])
const stats = ref<ReceivableStatsRecord | null>(null)
const selectedPlan = ref<ReceivablePlanRecord | null>(null)

const planLoading = ref(false)
const recordLoading = ref(false)
const alertLoading = ref(false)
const recordSubmitting = ref(false)
const createSubmitting = ref(false)
const manualChecking = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const queryState = reactive({
  dealId: undefined as number | undefined,
  status: undefined as number | undefined
})

const showCreatePlan = ref(false)
const createPlanForm = reactive({
  dealId: null as number | null,
  items: [buildPlanItem(1)] as ReceivablePlanCreateItem[]
})

onMounted(async () => {
  await Promise.all([loadPlans(), loadAlerts(), loadStats()])
})

async function loadPlans() {
  planLoading.value = true
  try {
    const res = await receivableApi.pagePlans({
      page: pagination.page,
      pageSize: pagination.pageSize,
      dealId: queryState.dealId,
      status: queryState.status
    })
    plans.value = res.list || []
    pagination.itemCount = res.total || 0
    if (selectedPlan.value) {
      const latest = plans.value.find(item => item.id === selectedPlan.value?.id)
      if (latest) {
        selectedPlan.value = latest
      }
    }
  } finally {
    planLoading.value = false
  }
}

async function loadStats() {
  stats.value = await receivableApi.stats()
}

async function loadAlerts() {
  alertLoading.value = true
  try {
    alerts.value = await receivableApi.openAlerts()
  } finally {
    alertLoading.value = false
  }
}

async function loadRecords(planId: number) {
  recordLoading.value = true
  try {
    records.value = await receivableApi.listRecords(planId)
  } finally {
    recordLoading.value = false
  }
}

function handleSearch(filters: { dealId?: number; status?: number }) {
  pagination.page = 1
  queryState.dealId = filters.dealId
  queryState.status = filters.status
  loadPlans()
}

function handleReset() {
  pagination.page = 1
  queryState.dealId = undefined
  queryState.status = undefined
  loadPlans()
}

function handlePageChange(page: number, filters: { dealId?: number; status?: number }) {
  pagination.page = page
  queryState.dealId = filters.dealId
  queryState.status = filters.status
  loadPlans()
}

function handlePageSizeChange(pageSize: number, filters: { dealId?: number; status?: number }) {
  pagination.page = 1
  pagination.pageSize = pageSize
  queryState.dealId = filters.dealId
  queryState.status = filters.status
  loadPlans()
}

async function handleViewRecords(plan: ReceivablePlanRecord) {
  selectedPlan.value = plan
  activeTab.value = 'records'
  await loadRecords(plan.id)
}

async function refreshRecords() {
  if (!selectedPlan.value) {
    message.warning('请先在计划列表中选择一条分期')
    return
  }
  await loadRecords(selectedPlan.value.id)
}

async function handleSubmitRecord(payload: ReceivableRecordCreateParams) {
  if (!payload.amount) {
    message.warning('请输入到账金额')
    return
  }
  recordSubmitting.value = true
  try {
    await receivableApi.createRecord(payload)
    message.success('到账登记成功')
    await Promise.all([loadPlans(), loadStats(), loadAlerts(), loadRecords(payload.planId)])
  } finally {
    recordSubmitting.value = false
  }
}

function addPlanItem() {
  createPlanForm.items.push(buildPlanItem(createPlanForm.items.length + 1))
}

function removePlanItem(index: number) {
  if (createPlanForm.items.length === 1) {
    return
  }
  createPlanForm.items.splice(index, 1)
}

async function handleCreatePlan() {
  if (!createPlanForm.dealId) {
    message.warning('请输入成交记录ID')
    return
  }
  const invalid = createPlanForm.items.find(item => !item.periodNo || !item.dueDate || !item.dueAmount)
  if (invalid) {
    message.warning('请完整填写每一期的期号、日期和金额')
    return
  }
  createSubmitting.value = true
  try {
    await receivableApi.createPlan({
      dealId: createPlanForm.dealId,
      items: createPlanForm.items
    })
    message.success('回款计划创建成功')
    showCreatePlan.value = false
    resetCreatePlanForm()
    await Promise.all([loadPlans(), loadStats(), loadAlerts()])
  } finally {
    createSubmitting.value = false
  }
}

async function runManualCheck() {
  manualChecking.value = true
  try {
    const [overdueNew, upcomingNew] = await receivableApi.runOverdueCheck()
    message.success(`巡检完成：新增逾期 ${overdueNew} 条，新增临期 ${upcomingNew} 条`)
    await Promise.all([loadPlans(), loadStats(), loadAlerts()])
  } finally {
    manualChecking.value = false
  }
}

function resetCreatePlanForm() {
  createPlanForm.dealId = null
  createPlanForm.items = [buildPlanItem(1)]
}

function buildPlanItem(periodNo: number): ReceivablePlanCreateItem {
  return {
    periodNo,
    periodName: '',
    dueDate: null,
    dueAmount: null,
    remark: ''
  }
}
</script>
