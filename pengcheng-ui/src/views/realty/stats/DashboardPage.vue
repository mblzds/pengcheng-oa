<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card>
        <n-form inline :model="filterForm">
          <n-form-item label="时间范围">
            <n-date-picker v-model:value="filterForm.dateRange" type="daterange" clearable style="width: 280px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="loadAllData">刷新统计</n-button>
          </n-form-item>
        </n-form>
      </n-card>

      <n-grid :cols="6" :x-gap="12">
        <n-gi>
          <n-card>
            <n-statistic label="报备数" :value="overview.reportCount || 0" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="到访数" :value="overview.visitCount || 0" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="成交数" :value="overview.dealCount || 0" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="成交金额" :value="overview.dealAmount || 0" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="应收佣金" :value="overview.receivableCommission || 0" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card>
            <n-statistic label="已结佣金" :value="overview.settledCommission || 0" />
          </n-card>
        </n-gi>
      </n-grid>

      <n-card title="报备-到访-成交转化漏斗图">
        <div ref="funnelChartRef" class="chart-box"></div>
      </n-card>

      <n-grid :cols="2" :x-gap="12">
        <n-gi>
          <n-card title="项目业绩排行榜">
            <n-data-table :columns="projectRankColumns" :data="ranking.projectRanking" :pagination="false" size="small" />
          </n-card>
        </n-gi>
        <n-gi>
          <n-card title="联盟商业绩排行榜">
            <n-data-table :columns="allianceRankColumns" :data="ranking.allianceRanking" :pagination="false" size="small" />
          </n-card>
        </n-gi>
      </n-grid>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue'
import { type DataTableColumns } from 'naive-ui'
import { realtyApi, type DashboardFunnelRecord, type DashboardOverviewRecord, type DashboardRankingRecord } from '@/api/realty'

const filterForm = reactive<{
  dateRange: [number, number] | null
}>({
  dateRange: null
})

const overview = reactive<DashboardOverviewRecord>({
  reportCount: 0,
  visitCount: 0,
  dealCount: 0,
  dealAmount: 0,
  receivableCommission: 0,
  settledCommission: 0
})

const funnel = reactive<DashboardFunnelRecord>({
  reportCount: 0,
  visitCount: 0,
  dealCount: 0,
  reportToVisitRate: 0,
  visitToDealRate: 0,
  reportToDealRate: 0
})

const ranking = reactive<DashboardRankingRecord>({
  projectRanking: [],
  allianceRanking: []
})

const funnelChartRef = ref<HTMLDivElement | null>(null)
let funnelChart: any = null

const projectRankColumns: DataTableColumns<DashboardRankingRecord['projectRanking'][number]> = [
  { title: '项目', key: 'projectName' },
  { title: '成交数量', key: 'dealCount', width: 100 },
  { title: '成交金额', key: 'dealAmount', width: 120 }
]

const allianceRankColumns: DataTableColumns<DashboardRankingRecord['allianceRanking'][number]> = [
  { title: '联盟商', key: 'companyName' },
  { title: '上客数量', key: 'customerCount', width: 100 },
  { title: '成交数量', key: 'dealCount', width: 100 }
]

function toDateString(ms?: number) {
  if (!ms) return undefined
  const date = new Date(ms)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildDateParams() {
  const [startMs, endMs] = filterForm.dateRange || []
  return {
    startDate: toDateString(startMs),
    endDate: toDateString(endMs)
  }
}

async function loadAllData() {
  const dateParams = buildDateParams()

  const [overviewRes, funnelRes, rankingRes] = await Promise.all([
    realtyApi.dashboardOverview(dateParams),
    realtyApi.dashboardFunnel(dateParams),
    realtyApi.dashboardRanking(dateParams)
  ])

  Object.assign(overview, overviewRes)
  Object.assign(funnel, funnelRes)
  ranking.projectRanking = rankingRes.projectRanking || []
  ranking.allianceRanking = rankingRes.allianceRanking || []

  await nextTick()
  renderFunnelChart()
}

function renderFunnelChart() {
  if (!funnelChartRef.value) return

  import('echarts').then(echarts => {
    if (!funnelChart) {
      funnelChart = echarts.init(funnelChartRef.value)
    }

    funnelChart.setOption({
      tooltip: {
        trigger: 'item'
      },
      series: [
        {
          name: '转化漏斗',
          type: 'funnel',
          left: '10%',
          top: 20,
          bottom: 20,
          width: '80%',
          min: 0,
          max: Math.max(funnel.reportCount || 0, 1),
          sort: 'descending',
          gap: 4,
          label: {
            show: true,
            position: 'inside'
          },
          data: [
            { value: funnel.reportCount, name: '报备' },
            { value: funnel.visitCount, name: '到访' },
            { value: funnel.dealCount, name: '成交' }
          ]
        }
      ]
    })
  }).catch(() => {
    // ignore
  })
}

onMounted(() => {
  loadAllData()
})

onUnmounted(() => {
  if (funnelChart) {
    funnelChart.dispose()
  }
})
</script>

<style scoped>
.chart-box {
  width: 100%;
  height: 360px;
}
</style>
