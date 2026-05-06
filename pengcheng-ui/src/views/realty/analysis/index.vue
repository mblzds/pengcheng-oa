<template>
  <div class="analysis-container">
    <!-- 顶部数据卡片 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16">
      <n-gi v-for="card in summaryCards" :key="card.label">
        <n-card size="small">
          <div class="summary-card">
            <div class="card-value" :style="{ color: card.color }">{{ card.value }}</div>
            <div class="card-label">{{ card.label }}</div>
            <div class="card-trend" :class="card.trendUp ? 'up' : 'down'">
              {{ card.trendUp ? '↑' : '↓' }} {{ card.trend }}
            </div>
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 图表区域 -->
    <n-grid :cols="2" :x-gap="16" :y-gap="16" style="margin-top: 16px">
      <n-gi>
        <n-card title="销售额趋势">
          <div class="chart-toolbar">
            <n-radio-group v-model:value="salesDimension" size="small">
              <n-radio-button value="day">日</n-radio-button>
              <n-radio-button value="week">周</n-radio-button>
              <n-radio-button value="month">月</n-radio-button>
            </n-radio-group>
          </div>
          <div ref="salesChartRef" class="chart-box"></div>
        </n-card>
      </n-gi>

      <n-gi>
        <n-card title="项目去化率分析">
          <div ref="dehuaChartRef" class="chart-box"></div>
        </n-card>
      </n-gi>

      <n-gi>
        <n-card title="回款进度跟踪">
          <div ref="paymentChartRef" class="chart-box"></div>
        </n-card>
      </n-gi>

      <n-gi>
        <n-card title="佣金统计分析">
          <div ref="commissionChartRef" class="chart-box"></div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- AI 洞察 -->
    <n-card title="AI 经营洞察" style="margin-top: 16px">
      <template #header-extra>
        <n-button size="small" type="primary" :loading="insightLoading" @click="generateInsight">
          AI 分析
        </n-button>
      </template>
      <div v-if="aiInsight" class="ai-insight">
        <div v-for="(line, i) in aiInsight.split('\n')" :key="i" class="insight-line">{{ line }}</div>
      </div>
      <n-empty v-else description="点击 AI 分析 生成经营洞察建议" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import {
  NCard, NGrid, NGi, NRadioGroup, NRadioButton, NButton, NEmpty, useMessage
} from 'naive-ui'
import { request } from '@/utils/request'
import { realtyApi } from '@/api/realty'
import * as echarts from 'echarts'

const message = useMessage()

const salesDimension = ref('month')
const insightLoading = ref(false)
const aiInsight = ref('')

const salesChartRef = ref<HTMLDivElement>()
const dehuaChartRef = ref<HTMLDivElement>()
const paymentChartRef = ref<HTMLDivElement>()
const commissionChartRef = ref<HTMLDivElement>()

let salesChart: echarts.ECharts | null = null
let dehuaChart: echarts.ECharts | null = null
let paymentChart: echarts.ECharts | null = null
let commissionChart: echarts.ECharts | null = null

const summaryCards = ref([
  { label: '本月签约额', value: '¥0', color: '#18a058', trend: '0%', trendUp: true },
  { label: '累计回款', value: '¥0', color: '#2080f0', trend: '0%', trendUp: true },
  { label: '待结佣金', value: '¥0', color: '#f0a020', trend: '0%', trendUp: false },
  { label: '整体去化率', value: '0%', color: '#d03050', trend: '0%', trendUp: true }
])

const overviewData = ref<any>(null)
const funnelData = ref<any>(null)
const rankingData = ref<any>(null)

onMounted(async () => {
  await loadAllData()
  await nextTick()
  initCharts()
})

watch(salesDimension, () => {
  updateSalesChart()
})

async function loadAllData() {
  await Promise.all([loadDashboardData(), loadFunnelData(), loadRankingData()])
}

async function loadDashboardData() {
  try {
    const d = await realtyApi.dashboardOverview({})
    overviewData.value = d
    if (d) {
      const amount = (v: number | undefined) => (v ?? 0).toLocaleString()
      summaryCards.value[0].value = '¥' + amount(Number(d.dealAmount ?? 0))
      summaryCards.value[0].trend = ((d as any).dealAmountTrend ?? 0) + '%'
      summaryCards.value[0].trendUp = ((d as any).dealAmountTrend ?? 0) >= 0
      summaryCards.value[1].value = '¥' + amount(Number(d.settledCommission ?? 0))
      summaryCards.value[1].trend = '0%'
      summaryCards.value[2].value = '¥' + amount(Number(d.receivableCommission ?? 0))
      summaryCards.value[2].trend = '0%'
      const visitRate = d.dealCount && d.visitCount
        ? ((Number(d.dealCount) / Math.max(1, Number(d.visitCount))) * 100).toFixed(1) + '%'
        : '0%'
      summaryCards.value[3].value = visitRate
      summaryCards.value[3].trend = ((d as any).dealCountTrend ?? 0) + '%'
      summaryCards.value[3].trendUp = ((d as any).dealCountTrend ?? 0) >= 0
    }
  } catch {
    summaryCards.value[0].value = '¥0'
    summaryCards.value[1].value = '¥0'
    summaryCards.value[2].value = '¥0'
    summaryCards.value[3].value = '0%'
  }
}

async function loadFunnelData() {
  try {
    funnelData.value = await realtyApi.dashboardFunnel({})
  } catch { funnelData.value = null }
}

async function loadRankingData() {
  try {
    rankingData.value = await realtyApi.dashboardRanking({})
  } catch { rankingData.value = null }
}

function initCharts() {
  if (salesChartRef.value) {
    salesChart = echarts.init(salesChartRef.value)
    updateSalesChart()
  }
  if (dehuaChartRef.value) {
    dehuaChart = echarts.init(dehuaChartRef.value)
    updateDehuaChart()
  }
  if (paymentChartRef.value) {
    paymentChart = echarts.init(paymentChartRef.value)
    updatePaymentChart()
  }
  if (commissionChartRef.value) {
    commissionChart = echarts.init(commissionChartRef.value)
    updateCommissionChart()
  }

  window.addEventListener('resize', () => {
    salesChart?.resize()
    dehuaChart?.resize()
    paymentChart?.resize()
    commissionChart?.resize()
  })
}

function updateDehuaChart() {
  if (!dehuaChart) return
  const ranking = rankingData.value
  const projectNames = ranking?.projectRanking?.map((p: any) => p.projectName) || []
  const dealCounts = ranking?.projectRanking?.map((p: any) => p.dealCount || 0) || []
  if (projectNames.length === 0) {
    dehuaChart.setOption({
      title: { text: '暂无项目数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
      xAxis: { show: false }, yAxis: { show: false }, series: []
    })
    return
  }
  dehuaChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: projectNames },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: dealCounts,
      itemStyle: {
        color: (params: any) => {
          const colors = ['#18a058', '#2080f0', '#f0a020', '#d03050', '#722ed1']
          return colors[params.dataIndex % colors.length]
        }
      },
      label: { show: true, position: 'top' }
    }]
  })
}

function updatePaymentChart() {
  if (!paymentChart) return
  const d = overviewData.value
  const settled = Number(d?.settledCommission ?? 0)
  const receivable = Number(d?.receivableCommission ?? 0)
  if (settled === 0 && receivable === 0) {
    paymentChart.setOption({
      title: { text: '暂无回款数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
      series: []
    })
    return
  }
  paymentChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: settled, name: '已结佣金', itemStyle: { color: '#18a058' } },
        { value: receivable, name: '应收佣金', itemStyle: { color: '#f0a020' } }
      ],
      label: { formatter: '{b}: ¥{c}' }
    }]
  })
}

function updateCommissionChart() {
  if (!commissionChart) return
  const funnel = funnelData.value
  if (!funnel) {
    commissionChart.setOption({
      title: { text: '暂无转化数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
      xAxis: { show: false }, yAxis: { show: false }, series: []
    })
    return
  }
  commissionChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['报备', '到访', '成交'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [
        { value: funnel.reportCount || 0, itemStyle: { color: '#2080f0' } },
        { value: funnel.visitCount || 0, itemStyle: { color: '#f0a020' } },
        { value: funnel.dealCount || 0, itemStyle: { color: '#18a058' } }
      ],
      label: { show: true, position: 'top' }
    }]
  })
}

function updateSalesChart() {
  if (!salesChart) return
  const ranking = rankingData.value
  const projects = ranking?.projectRanking || []
  if (projects.length === 0) {
    salesChart.setOption({
      title: { text: '暂无销售数据', left: 'center', top: 'center', textStyle: { color: '#999', fontSize: 14 } },
      xAxis: { show: false }, yAxis: { show: false }, series: []
    })
    return
  }
  const names = projects.map((p: any) => p.projectName)
  const amounts = projects.map((p: any) => Number(p.dealAmount || 0))
  salesChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: ¥{c}' },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: amounts,
      itemStyle: { color: '#18a058' },
      label: { show: true, position: 'top', formatter: (p: any) => '¥' + (p.value || 0).toLocaleString() }
    }]
  })
}

async function generateInsight() {
  insightLoading.value = true
  aiInsight.value = ''
  try {
    const res: any = await request({ url: '/ai/analysis/insight', method: 'post', data: { type: 'business_overview' } })
    aiInsight.value = (typeof res === 'string' ? res : res?.content || res?.message) || '暂无 AI 洞察数据'
  } catch {
    aiInsight.value = '暂时无法生成 AI 洞察，请稍后重试。'
  } finally {
    insightLoading.value = false
  }
}
</script>

<style scoped>
.analysis-container { padding: 0; }
.summary-card { text-align: center; padding: 8px 0; }
.card-value { font-size: 24px; font-weight: 700; }
.card-label { font-size: 13px; color: #666; margin: 4px 0; }
.card-trend { font-size: 12px; }
.card-trend.up { color: #18a058; }
.card-trend.down { color: #d03050; }
.chart-toolbar { display: flex; justify-content: flex-end; margin-bottom: 8px; }
.chart-box { width: 100%; height: 320px; }
.ai-insight { padding: 12px; background: linear-gradient(135deg, #f0fdf4, #ecfdf5); border-radius: 8px; }
.insight-line { padding: 4px 0; font-size: 14px; line-height: 1.8; }
</style>
