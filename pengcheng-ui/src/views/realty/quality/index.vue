<template>
  <div class="quality-container">
    <n-grid :cols="2" :x-gap="16" :y-gap="16">
      <!-- 左侧：能力雷达图 -->
      <n-gi>
        <n-card title="销售能力画像">
          <template #header-extra>
            <n-button size="small" type="primary" :loading="evaluating" @click="evaluateSelf">
              立即评估
            </n-button>
          </template>
          <div ref="radarChartRef" class="chart-box"></div>
          <div v-if="latestScore" class="score-info">
            <div class="overall-score">
              <span class="score-num" :style="{ color: scoreColor(latestScore.overallScore) }">{{ latestScore.overallScore }}</span>
              <span class="score-label">综合评分</span>
            </div>
            <div class="ai-comment">{{ latestScore.aiComment }}</div>
            <div class="ai-suggestion">💡 {{ latestScore.aiSuggestion }}</div>
          </div>
        </n-card>
      </n-gi>

      <!-- 右侧：排行榜 -->
      <n-gi>
        <n-card title="销售排行榜">
          <template #header-extra>
            <n-date-picker v-model:value="rankDate" type="date" size="small" />
          </template>
          <div class="ranking-list">
            <div v-for="(item, index) in ranking" :key="item.id" class="ranking-item">
              <div :class="['rank-badge', 'rank-' + (index + 1)]">{{ index + 1 }}</div>
              <div class="rank-info">
                <div class="rank-name">{{ item.userName || '用户' + item.userId }}</div>
                <n-progress :percentage="item.overallScore" :color="scoreColor(item.overallScore)" :height="8" :show-indicator="false" />
              </div>
              <div class="rank-score" :style="{ color: scoreColor(item.overallScore) }">{{ item.overallScore }}</div>
            </div>
            <n-empty v-if="ranking.length === 0" description="暂无评分数据，请先执行评估" />
          </div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 评分趋势 -->
    <n-card title="评分趋势" style="margin-top: 16px">
      <div ref="trendChartRef" class="chart-box"></div>
    </n-card>

    <!-- 维度分数表 -->
    <n-card title="各维度评分明细" style="margin-top: 16px">
      <n-data-table :columns="detailColumns" :data="scoreHistory" :max-height="300" />
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, h } from 'vue'
import { NCard, NGrid, NGi, NButton, NDatePicker, NProgress, NDataTable, NEmpty, useMessage } from 'naive-ui'
import request from '@/utils/request'
import * as echarts from 'echarts'

const message = useMessage()

const latestScore = ref<any>(null)
const ranking = ref<any[]>([])
const scoreHistory = ref<any[]>([])
const evaluating = ref(false)
const rankDate = ref<number>(Date.now())

const radarChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()
let radarChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const scoreColor = (score: number) => score >= 80 ? '#18a058' : score >= 60 ? '#f0a020' : '#d03050'

const detailColumns = [
  { title: '评分日期', key: 'scoreDate', width: 120 },
  { title: '沟通', key: 'communicationScore', width: 70 },
  { title: '需求挖掘', key: 'demandMiningScore', width: 90 },
  { title: '异议处理', key: 'objectionHandlingScore', width: 90 },
  { title: '闭合能力', key: 'closingAbilityScore', width: 90 },
  { title: '跟进频率', key: 'followUpFrequencyScore', width: 90 },
  { title: '响应时效', key: 'responseTimeScore', width: 90 },
  { title: '综合', key: 'overallScore', width: 70,
    render: (row: any) => h('span', { style: { color: scoreColor(row.overallScore), fontWeight: '700' } }, row.overallScore)
  }
]

onMounted(async () => {
  await loadData()
  await nextTick()
  initCharts()
})

watch(rankDate, () => loadRanking())

async function loadData() {
  await Promise.all([loadLatest(), loadHistory(), loadRanking()])
}

async function loadLatest() {
  try {
    const res = await request.get('/quality/latest')
    latestScore.value = res
  } catch { latestScore.value = null }
}

async function loadHistory() {
  try {
    const res = await request.get('/quality/history', { params: { limit: 12 } })
    scoreHistory.value = Array.isArray(res) ? res : []
  } catch { scoreHistory.value = [] }
}

async function loadRanking() {
  const date = new Date(rankDate.value).toISOString().split('T')[0]
  try {
    const res = await request.get('/quality/ranking', { params: { date } })
    ranking.value = Array.isArray(res) ? res : []
  } catch { ranking.value = [] }
}

async function evaluateSelf() {
  evaluating.value = true
  try {
    const res = await request.post('/quality/evaluate')
    latestScore.value = res
    message.success('评估完成')
    await loadData()
    updateCharts()
  } catch {
    message.error('评估失败')
  } finally {
    evaluating.value = false
  }
}

function initCharts() {
  if (radarChartRef.value) {
    radarChart = echarts.init(radarChartRef.value)
    updateRadar()
  }
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    updateTrend()
  }
  window.addEventListener('resize', () => {
    radarChart?.resize()
    trendChart?.resize()
  })
}

function updateCharts() {
  updateRadar()
  updateTrend()
}

function updateRadar() {
  if (!radarChart) return
  const s = latestScore.value || {}
  radarChart.setOption({
    radar: {
      indicator: [
        { name: '沟通', max: 100 },
        { name: '需求挖掘', max: 100 },
        { name: '异议处理', max: 100 },
        { name: '闭合能力', max: 100 },
        { name: '跟进频率', max: 100 },
        { name: '响应时效', max: 100 }
      ],
      shape: 'circle'
    },
    series: [{
      type: 'radar',
      data: [{
        value: [
          s.communicationScore || 0, s.demandMiningScore || 0,
          s.objectionHandlingScore || 0, s.closingAbilityScore || 0,
          s.followUpFrequencyScore || 0, s.responseTimeScore || 0
        ],
        areaStyle: { color: 'rgba(24,160,88,0.2)' },
        lineStyle: { color: '#18a058' },
        itemStyle: { color: '#18a058' }
      }]
    }]
  })
}

function updateTrend() {
  if (!trendChart || scoreHistory.value.length === 0) return
  const dates = scoreHistory.value.map(s => s.scoreDate).reverse()
  const scores = scoreHistory.value.map(s => s.overallScore).reverse()
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      type: 'line',
      data: scores,
      smooth: true,
      areaStyle: { color: 'rgba(32,128,240,0.1)' },
      lineStyle: { color: '#2080f0', width: 2 },
      itemStyle: { color: '#2080f0' },
      markLine: {
        data: [{ yAxis: 60, label: { formatter: '及格线' }, lineStyle: { color: '#f0a020', type: 'dashed' } }]
      }
    }]
  })
}
</script>

<style scoped>
.quality-container { padding: 0; }
.chart-box { width: 100%; height: 300px; }
.score-info { text-align: center; margin-top: 12px; }
.overall-score { margin-bottom: 8px; }
.score-num { font-size: 48px; font-weight: 700; }
.score-label { font-size: 14px; color: #999; margin-left: 8px; }
.ai-comment { font-size: 15px; font-weight: 600; margin: 8px 0; }
.ai-suggestion { font-size: 13px; color: #666; padding: 8px; background: #f9fafb; border-radius: 6px; }
.ranking-list { display: flex; flex-direction: column; gap: 12px; }
.ranking-item { display: flex; align-items: center; gap: 12px; }
.rank-badge {
  width: 28px; height: 28px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 13px; color: #fff; background: #ccc; flex-shrink: 0;
}
.rank-1 { background: linear-gradient(135deg, #ffd700, #ffb300); }
.rank-2 { background: linear-gradient(135deg, #c0c0c0, #9e9e9e); }
.rank-3 { background: linear-gradient(135deg, #cd7f32, #a0522d); }
.rank-info { flex: 1; }
.rank-name { font-size: 14px; font-weight: 500; margin-bottom: 4px; }
.rank-score { font-size: 20px; font-weight: 700; min-width: 40px; text-align: right; }
</style>
