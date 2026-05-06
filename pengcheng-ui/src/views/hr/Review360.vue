<template>
  <div class="review-360-page">
    <n-grid :cols="24" :x-gap="16" :y-gap="16">
      <n-gi :span="16">
        <n-card title="我的评估任务">
          <template #header-extra>
            <n-space>
              <n-tag type="info">待办 {{ taskPagination.itemCount }}</n-tag>
              <n-button size="small" @click="loadPendingReviews">刷新</n-button>
            </n-space>
          </template>

          <n-data-table
            :columns="taskColumns"
            :data="pendingReviews"
            :loading="taskLoading"
            :pagination="taskPagination"
            :row-key="row => `${row.periodId}-${row.userId}-${row.reviewType}`"
            @update:page="handleTaskPageChange"
            @update:page-size="handleTaskPageSizeChange"
          />
        </n-card>

        <n-card title="评估结果汇总" style="margin-top: 16px">
          <n-form inline>
            <n-form-item label="考核周期">
              <n-select
                v-model:value="resultFilter.periodId"
                :options="periodOptions"
                style="width: 220px"
                clearable
              />
            </n-form-item>
            <n-form-item label="被评估人">
              <n-select
                v-model:value="resultFilter.userId"
                :options="userOptions"
                style="width: 220px"
                clearable
                filterable
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="loadReviewResult">查询</n-button>
            </n-form-item>
          </n-form>

          <n-empty v-if="!reviewResult" description="选择周期和被评估人后查看 360 评估结果" />

          <template v-else>
            <div class="result-header">
              <div>
                <h3>{{ reviewResult.userName }} 的 360 评估结果</h3>
                <p>共收到 {{ reviewResult.stats.totalCount }} 份评价</p>
              </div>
              <n-tag :type="gradeType(reviewResult.grade)" size="large">{{ reviewResult.grade }} 级</n-tag>
            </div>

            <n-grid :cols="4" :x-gap="12" :y-gap="12" class="score-grid">
              <n-gi>
                <n-statistic label="自评" :value="reviewResult.selfScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="上级评价" :value="reviewResult.managerScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="同事评价" :value="reviewResult.peerScore" />
              </n-gi>
              <n-gi>
                <n-statistic label="下级评价" :value="reviewResult.subordinateScore" />
              </n-gi>
            </n-grid>

            <n-divider>综合得分</n-divider>
            <div class="final-score">
              <n-progress
                type="dashboard"
                :percentage="Number(reviewResult.finalScore || 0)"
                :status="Number(reviewResult.finalScore || 0) >= 80 ? 'success' : Number(reviewResult.finalScore || 0) >= 70 ? 'warning' : 'error'"
              />
            </div>

            <n-divider>评价详情</n-divider>
            <n-timeline>
              <n-timeline-item
                v-for="(review, index) in reviewResult.reviews"
                :key="`${review.reviewType}-${index}`"
                :type="reviewTypeColor(review.reviewType)"
                :title="`${review.reviewTypeName} · ${review.reviewerName}`"
                :content="`${review.score} 分`"
              >
                <template #footer>
                  <div class="review-detail">
                    <p v-if="review.comment"><strong>评价：</strong>{{ review.comment }}</p>
                    <p v-if="review.strengths"><strong>优点：</strong>{{ review.strengths }}</p>
                    <p v-if="review.improvements"><strong>改进建议：</strong>{{ review.improvements }}</p>
                  </div>
                </template>
              </n-timeline-item>
            </n-timeline>
          </template>
        </n-card>
      </n-gi>

      <n-gi :span="8">
        <n-card title="任务分发">
          <n-form label-placement="left" label-width="90">
            <n-form-item label="考核周期">
              <n-select
                v-model:value="dispatchForm.periodId"
                :options="periodOptions"
                placeholder="选择一个周期"
              />
            </n-form-item>
            <n-form-item label="参与人员">
              <n-select
                v-model:value="dispatchForm.userIds"
                multiple
                filterable
                clearable
                :options="userOptions"
                placeholder="选择需要生成任务的员工"
              />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="dispatchLoading" @click="createReviewTasks">批量生成任务</n-button>
            </n-form-item>
          </n-form>
          <n-alert type="info" :show-icon="false">
            已支持四向任务生成：自评、上级、同事、下级。重复生成会自动去重。
          </n-alert>
        </n-card>

        <n-card title="权重配置" style="margin-top: 16px">
          <n-form label-placement="left" label-width="90">
            <n-form-item label="自评权重">
              <n-input-number v-model:value="weightConfig.selfWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ percent(weightConfig.selfWeight) }}</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="上级权重">
              <n-input-number v-model:value="weightConfig.managerWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ percent(weightConfig.managerWeight) }}</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="同事权重">
              <n-input-number v-model:value="weightConfig.peerWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ percent(weightConfig.peerWeight) }}</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="下级权重">
              <n-input-number v-model:value="weightConfig.subordinateWeight" :min="0" :max="1" :step="0.05" style="width: 100%">
                <template #suffix>{{ percent(weightConfig.subordinateWeight) }}</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="最少人数">
              <n-input-number v-model:value="weightConfig.minReviewers" :min="1" :max="20" style="width: 100%" />
            </n-form-item>
            <n-form-item label="匿名评估">
              <n-switch v-model:value="weightConfig.anonymous" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="weightSaving" @click="saveWeightConfig">保存配置</n-button>
            </n-form-item>
          </n-form>
        </n-card>

        <n-card title="统计速览" style="margin-top: 16px">
          <n-space vertical>
            <n-statistic label="当前待办任务" :value="taskPagination.itemCount" />
            <n-statistic label="本次结果评价数" :value="reviewResult?.stats.totalCount || 0" />
            <n-statistic label="匿名评估" :value="weightConfig.anonymous ? 1 : 0">
              <template #suffix>{{ weightConfig.anonymous ? '开启' : '关闭' }}</template>
            </n-statistic>
          </n-space>
        </n-card>
      </n-gi>
    </n-grid>

    <n-modal v-model:show="showReviewForm" preset="card" :title="reviewFormTitle" style="width: 600px">
      <n-form label-placement="left" label-width="100">
        <n-form-item label="被评估人">
          <span>{{ currentRevieweeName }}</span>
        </n-form-item>
        <n-form-item label="评估类型">
          <span>{{ currentReviewTypeName }}</span>
        </n-form-item>
        <n-form-item label="总分" required>
          <n-input-number
            v-model:value="reviewForm.totalScore"
            :min="0"
            :max="100"
            style="width: 100%"
            placeholder="请输入 0 - 100 分"
          />
        </n-form-item>
        <n-form-item label="评价意见">
          <n-input v-model:value="reviewForm.comment" type="textarea" :rows="3" placeholder="请写下您的整体评价" />
        </n-form-item>
        <n-form-item label="优点">
          <n-input v-model:value="reviewForm.strengths" type="textarea" :rows="2" placeholder="记录值得肯定的表现" />
        </n-form-item>
        <n-form-item label="改进建议">
          <n-input v-model:value="reviewForm.improvements" type="textarea" :rows="2" placeholder="记录后续改进方向" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showReviewForm = false">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="submitReview">提交评估</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NButton, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { hrKpiPeriodApi } from '@/api/hr'
import { userApi } from '@/api/system'
import { request } from '@/utils/request'

interface PendingReview {
  periodId: number
  userId: number
  reviewType: number
}

interface PeriodOption {
  label: string
  value: number
}

interface UserOption {
  label: string
  value: number
}

interface ReviewResult {
  userId: number
  userName: string
  selfScore: number
  managerScore: number
  peerScore: number
  subordinateScore: number
  finalScore: number
  grade: string
  stats: {
    selfCount: number
    managerCount: number
    peerCount: number
    subordinateCount: number
    totalCount: number
  }
  reviews: Array<{
    reviewType: number
    reviewTypeName: string
    reviewerName: string
    score: number
    comment?: string
    strengths?: string
    improvements?: string
  }>
}

interface WeightConfig {
  selfWeight: number
  managerWeight: number
  peerWeight: number
  subordinateWeight: number
  minReviewers: number
  anonymous: boolean
}

interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

const message = useMessage()

const pendingReviews = ref<PendingReview[]>([])
const reviewResult = ref<ReviewResult | null>(null)
const periodOptions = ref<PeriodOption[]>([])
const userOptions = ref<UserOption[]>([])

const taskLoading = ref(false)
const dispatchLoading = ref(false)
const weightSaving = ref(false)
const submitting = ref(false)
const showReviewForm = ref(false)

const taskPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const resultFilter = reactive({
  periodId: null as number | null,
  userId: null as number | null
})

const dispatchForm = reactive({
  periodId: null as number | null,
  userIds: [] as number[]
})

const weightConfig = reactive<WeightConfig>({
  selfWeight: 0.1,
  managerWeight: 0.4,
  peerWeight: 0.3,
  subordinateWeight: 0.2,
  minReviewers: 3,
  anonymous: true
})

const reviewForm = reactive({
  periodId: null as number | null,
  userId: null as number | null,
  reviewType: 1,
  totalScore: null as number | null,
  comment: '',
  strengths: '',
  improvements: ''
})

const currentRevieweeName = ref('')

const periodNameMap = computed(() => {
  const map = new Map<number, string>()
  periodOptions.value.forEach(item => map.set(item.value, item.label))
  return map
})

const userNameMap = computed(() => {
  const map = new Map<number, string>()
  userOptions.value.forEach(item => map.set(item.value, item.label))
  return map
})

const currentReviewTypeName = computed(() => getReviewTypeName(reviewForm.reviewType))
const reviewFormTitle = computed(() => `提交${currentReviewTypeName.value}`)

const taskColumns: DataTableColumns<PendingReview> = [
  {
    title: '考核周期',
    key: 'periodId',
    width: 180,
    render: row => periodNameMap.value.get(row.periodId) || `周期 ${row.periodId}`
  },
  {
    title: '被评估人',
    key: 'userId',
    width: 140,
    render: row => userNameMap.value.get(row.userId) || `用户 ${row.userId}`
  },
  {
    title: '评估类型',
    key: 'reviewType',
    width: 120,
    render: row =>
      h(NTag, { size: 'small', type: reviewTypeTag(row.reviewType) }, {
        default: () => getReviewTypeName(row.reviewType)
      })
  },
  {
    title: '操作',
    key: 'action',
    width: 120,
    render: row =>
      h(
        NButton,
        {
          size: 'small',
          type: 'primary',
          onClick: () => openReviewForm(row)
        },
        { default: () => '去评估' }
      )
  }
]

onMounted(async () => {
  await Promise.all([loadPeriods(), loadUserOptions(), loadWeightConfig()])
  await loadPendingReviews()
})

async function loadPeriods() {
  const res: any = await hrKpiPeriodApi.page({ pageNum: 1, pageSize: 100 })
  const list = res?.list || res?.records || []
  periodOptions.value = list.map((item: any) => ({
    label: item.name || `周期 ${item.id}`,
    value: item.id
  }))
  if (!resultFilter.periodId && periodOptions.value.length > 0) {
    resultFilter.periodId = periodOptions.value[0].value
  }
  if (!dispatchForm.periodId && periodOptions.value.length > 0) {
    dispatchForm.periodId = periodOptions.value[0].value
  }
}

async function loadUserOptions() {
  const res: any = await userApi.page({ page: 1, pageSize: 200, status: 1 })
  const list = res?.list || []
  userOptions.value = list.map((item: any) => ({
    label: item.nickname || item.username || `用户 ${item.id}`,
    value: item.id
  }))
}

async function loadPendingReviews() {
  taskLoading.value = true
  try {
    const res = await request<PageResult<PendingReview>>({
      url: '/admin/hr/kpi/review/360/pending',
      method: 'get',
      params: {
        page: taskPagination.page,
        pageSize: taskPagination.pageSize
      }
    })
    pendingReviews.value = res.list || []
    taskPagination.itemCount = res.total || 0
  } finally {
    taskLoading.value = false
  }
}

function handleTaskPageChange(page: number) {
  taskPagination.page = page
  loadPendingReviews()
}

function handleTaskPageSizeChange(pageSize: number) {
  taskPagination.page = 1
  taskPagination.pageSize = pageSize
  loadPendingReviews()
}

async function loadReviewResult() {
  if (!resultFilter.periodId || !resultFilter.userId) {
    message.warning('请选择考核周期和被评估人')
    return
  }
  try {
    reviewResult.value = await request<ReviewResult>({
      url: '/admin/hr/kpi/review/360/result',
      method: 'get',
      params: {
        periodId: resultFilter.periodId,
        userId: resultFilter.userId
      }
    })
  } catch {
    reviewResult.value = null
  }
}

async function loadWeightConfig() {
  const config = await request<WeightConfig>({
    url: '/admin/hr/kpi/review/360/config',
    method: 'get'
  })
  weightConfig.selfWeight = config.selfWeight ?? 0.1
  weightConfig.managerWeight = config.managerWeight ?? 0.4
  weightConfig.peerWeight = config.peerWeight ?? 0.3
  weightConfig.subordinateWeight = config.subordinateWeight ?? 0.2
  weightConfig.minReviewers = config.minReviewers ?? 3
  weightConfig.anonymous = config.anonymous !== false
}

async function saveWeightConfig() {
  const totalWeight = weightConfig.selfWeight + weightConfig.managerWeight + weightConfig.peerWeight + weightConfig.subordinateWeight
  if (Math.abs(totalWeight - 1) > 0.001) {
    message.warning('四向权重合计必须等于 1')
    return
  }
  weightSaving.value = true
  try {
    await request({
      url: '/admin/hr/kpi/review/360/config',
      method: 'post',
      data: weightConfig
    })
    message.success('权重配置已保存')
  } finally {
    weightSaving.value = false
  }
}

async function createReviewTasks() {
  if (!dispatchForm.periodId || dispatchForm.userIds.length === 0) {
    message.warning('请选择考核周期和参与人员')
    return
  }
  dispatchLoading.value = true
  try {
    const count = await request<number>({
      url: '/admin/hr/kpi/review/360/tasks',
      method: 'post',
      params: { periodId: dispatchForm.periodId },
      data: dispatchForm.userIds
    })
    message.success(`任务生成完成，本次新增 ${count} 条`)
    await loadPendingReviews()
  } finally {
    dispatchLoading.value = false
  }
}

function openReviewForm(row: PendingReview) {
  reviewForm.periodId = row.periodId
  reviewForm.userId = row.userId
  reviewForm.reviewType = row.reviewType
  reviewForm.totalScore = null
  reviewForm.comment = ''
  reviewForm.strengths = ''
  reviewForm.improvements = ''
  currentRevieweeName.value = userNameMap.value.get(row.userId) || `用户 ${row.userId}`
  showReviewForm.value = true
}

async function submitReview() {
  if (reviewForm.totalScore === null) {
    message.warning('请输入评分')
    return
  }
  submitting.value = true
  try {
    await request({
      url: '/admin/hr/kpi/review/360',
      method: 'post',
      data: reviewForm
    })
    message.success('评估已提交')
    showReviewForm.value = false
    await loadPendingReviews()
    if (resultFilter.periodId === reviewForm.periodId && resultFilter.userId === reviewForm.userId) {
      await loadReviewResult()
    }
  } finally {
    submitting.value = false
  }
}

function getReviewTypeName(type: number) {
  const map: Record<number, string> = {
    1: '自评',
    2: '上级评价',
    3: '同事评价',
    4: '下级评价'
  }
  return map[type] || '未知类型'
}

function reviewTypeTag(type: number): 'info' | 'success' | 'warning' | 'error' {
  const map: Record<number, 'info' | 'success' | 'warning' | 'error'> = {
    1: 'info',
    2: 'success',
    3: 'warning',
    4: 'error'
  }
  return map[type] || 'info'
}

function reviewTypeColor(type: number): 'default' | 'info' | 'success' | 'warning' | 'error' {
  return reviewTypeTag(type)
}

function gradeType(grade: string): 'success' | 'warning' | 'error' | 'default' {
  if (grade === 'A' || grade === 'B') return 'success'
  if (grade === 'C') return 'warning'
  if (grade === 'D') return 'error'
  return 'default'
}

function percent(value: number) {
  return `${Math.round((value || 0) * 100)}%`
}
</script>

<style scoped>
.review-360-page {
  padding: 20px;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.result-header h3 {
  margin: 0 0 6px;
  font-size: 18px;
}

.result-header p {
  margin: 0;
  color: #666;
}

.score-grid {
  margin-bottom: 16px;
}

.final-score {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.review-detail {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 8px;
  margin-top: 8px;
}

.review-detail p {
  margin: 4px 0;
}
</style>
