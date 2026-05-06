<template>
  <div class="heartbeat-container">
    <!-- 统计卡片 -->
    <n-grid :cols="3" :x-gap="16" :y-gap="16" style="margin-bottom: 16px">
      <n-gi>
        <n-card size="small" class="stat-card critical">
          <div class="stat-value">{{ stats.critical || 0 }}</div>
          <div class="stat-label">严重告警</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card warn">
          <div class="stat-value">{{ stats.warn || 0 }}</div>
          <div class="stat-label">警告</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card total">
          <div class="stat-value">{{ stats.unhandled || 0 }}</div>
          <div class="stat-label">待处理总数</div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="AI 巡检告警">
      <template #header-extra>
        <n-space>
          <n-radio-group v-model:value="filter.severity" size="small" @update:value="loadList">
            <n-radio-button value="">全部</n-radio-button>
            <n-radio-button value="critical">严重</n-radio-button>
            <n-radio-button value="warn">警告</n-radio-button>
            <n-radio-button value="info">提示</n-radio-button>
          </n-radio-group>
          <n-switch v-model:value="filter.showHandled" @update:value="loadList" size="small">
            <template #checked>显示已处理</template>
            <template #unchecked>仅未处理</template>
          </n-switch>
          <n-button type="primary" size="small" :loading="running" @click="handleRun">手动巡检</n-button>
          <n-button size="small" @click="handleBatchHandle" :disabled="selectedIds.length === 0">
            批量处理 ({{ selectedIds.length }})
          </n-button>
        </n-space>
      </template>

      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :row-key="(row: any) => row.id"
        @update:checked-row-keys="onCheck"
        size="small"
      />

      <n-space justify="end" style="margin-top: 12px">
        <n-pagination
          v-model:page="filter.page"
          :page-size="filter.pageSize"
          :item-count="total"
          show-size-picker
          :page-sizes="[20, 50, 100]"
          @update:page="loadList"
          @update:page-size="(s: number) => { filter.pageSize = s; loadList() }"
        />
      </n-space>
    </n-card>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="500" placement="right">
      <n-drawer-content :title="detailItem?.title || '告警详情'">
        <n-descriptions :column="1" label-placement="left" bordered>
          <n-descriptions-item label="类型">
            <n-tag :type="typeColor(detailItem?.checkType)">{{ typeLabel(detailItem?.checkType) }}</n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="严重程度">
            <n-tag :type="severityColor(detailItem?.severity)">{{ severityLabel(detailItem?.severity) }}</n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="状态">
            <n-tag :type="detailItem?.handled ? 'success' : 'warning'">
              {{ detailItem?.handled ? '已处理' : '待处理' }}
            </n-tag>
          </n-descriptions-item>
          <n-descriptions-item label="时间">{{ detailItem?.createdAt }}</n-descriptions-item>
        </n-descriptions>
        <n-divider />
        <h4>详情</h4>
        <p>{{ detailItem?.content }}</p>
        <n-divider />
        <h4>AI 建议</h4>
        <n-alert type="info">{{ detailItem?.suggestion }}</n-alert>
        <template #footer>
          <n-button v-if="!detailItem?.handled" type="primary" block @click="handleSingle(detailItem?.id)">
            标记为已处理
          </n-button>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, useMessage } from 'naive-ui'
import { heartbeatApi } from '@/api/heartbeat'

const message = useMessage()
const loading = ref(false)
const running = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const stats = ref<any>({})
const selectedIds = ref<number[]>([])
const showDetail = ref(false)
const detailItem = ref<any>(null)

const filter = reactive({
  page: 1,
  pageSize: 20,
  severity: '',
  showHandled: false
})

function severityColor(s: string) {
  return s === 'critical' ? 'error' : s === 'warn' ? 'warning' : 'info'
}
function severityLabel(s: string) {
  return s === 'critical' ? '严重' : s === 'warn' ? '警告' : '提示'
}
function typeColor(t: string) {
  const m: any = { customer_followup: 'warning', commission: 'info', contract: 'success', overdue: 'error' }
  return m[t] || 'default'
}
function typeLabel(t: string) {
  const m: any = { customer_followup: '客户跟进', commission: '佣金结算', contract: '合同到期', overdue: '回款逾期' }
  return m[t] || t
}

const columns = [
  { type: 'selection' as const, width: 40 },
  {
    title: '严重程度', key: 'severity', width: 80,
    render: (row: any) => h(NTag, { type: severityColor(row.severity), size: 'small' }, { default: () => severityLabel(row.severity) })
  },
  {
    title: '类型', key: 'checkType', width: 100,
    render: (row: any) => h(NTag, { type: typeColor(row.checkType), size: 'small' }, { default: () => typeLabel(row.checkType) })
  },
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  {
    title: '状态', key: 'handled', width: 80,
    render: (row: any) => h(NTag, { type: row.handled ? 'success' : 'warning', size: 'small' }, { default: () => row.handled ? '已处理' : '待处理' })
  },
  { title: '时间', key: 'createdAt', width: 160 },
  {
    title: '操作', key: 'actions', width: 120,
    render: (row: any) => h(NSpace, { size: 4 }, {
      default: () => [
        h(NButton, { text: true, size: 'tiny', type: 'primary', onClick: () => { detailItem.value = row; showDetail.value = true } }, { default: () => '详情' }),
        !row.handled ? h(NButton, { text: true, size: 'tiny', onClick: () => handleSingle(row.id) }, { default: () => '处理' }) : null
      ]
    })
  }
]

async function loadList() {
  loading.value = true
  try {
    const res: any = await heartbeatApi.list({
      page: filter.page,
      pageSize: filter.pageSize,
      severity: filter.severity || undefined,
      handled: filter.showHandled ? undefined : false
    })
    list.value = res?.records || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res: any = await heartbeatApi.stats()
  stats.value = (res && typeof res === 'object') ? res : {}
}

async function handleSingle(id: number) {
  await heartbeatApi.handle(id)
  message.success('已标记为已处理')
  showDetail.value = false
  loadList()
  loadStats()
}

async function handleBatchHandle() {
  await heartbeatApi.batchHandle(selectedIds.value)
  message.success(`已批量处理 ${selectedIds.value.length} 条告警`)
  selectedIds.value = []
  loadList()
  loadStats()
}

function onCheck(keys: number[]) {
  selectedIds.value = keys
}

async function handleRun() {
  running.value = true
  try {
    const res: any = await heartbeatApi.run()
    message.success(`巡检完成，新增 ${res || 0} 条告警`)
    loadList()
    loadStats()
  } finally {
    running.value = false
  }
}

onMounted(() => {
  loadList()
  loadStats()
})
</script>

<style scoped>
.heartbeat-container { padding: 4px; }
.stat-card { text-align: center; border-radius: 8px; }
.stat-card.critical { border-left: 4px solid #e53e3e; }
.stat-card.warn { border-left: 4px solid #ed8936; }
.stat-card.total { border-left: 4px solid #4299e1; }
.stat-value { font-size: 28px; font-weight: 700; color: #333; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
</style>
