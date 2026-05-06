<template>
  <div class="visit-container">
    <!-- 统计卡片 -->
    <n-grid :cols="4" :x-gap="16" style="margin-bottom: 16px">
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ stats.total || 0 }}</div>
          <div class="stat-label">近30天拜访</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ stats.todayCount || 0 }}</div>
          <div class="stat-label">今日拜访</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ fieldCount }}</div>
          <div class="stat-label">实地拜访</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ phoneCount }}</div>
          <div class="stat-label">电话拜访</div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 工具栏 -->
    <n-card>
      <div class="toolbar">
        <n-space>
          <n-select v-model:value="filter.visitType" placeholder="拜访类型" clearable style="width: 120px" size="small"
            :options="[{ label: '实地', value: 'field' }, { label: '电话', value: 'phone' }, { label: '线上', value: 'online' }]" />
          <n-date-picker v-model:value="filter.dateRange" type="daterange" size="small" clearable />
          <n-button size="small" @click="loadVisits">查询</n-button>
        </n-space>
        <n-button type="primary" size="small" @click="openCreate">新建拜访记录</n-button>
      </div>

      <!-- 拜访列表 -->
      <n-data-table :columns="columns" :data="visits" :loading="loading" size="small"
        :pagination="{ page: currentPage, pageSize: 20, itemCount: total, onChange: handlePageChange }" />
    </n-card>

    <!-- 新建/编辑弹窗 -->
    <n-modal v-model:show="showForm" preset="card" :title="formTitle" style="width: 650px">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="100px">
        <n-grid :cols="2" :x-gap="12">
          <n-gi>
            <n-form-item label="客户姓名" path="customerName">
              <n-input v-model:value="form.customerName" placeholder="请输入客户姓名" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="项目名称" path="projectName">
              <n-input v-model:value="form.projectName" placeholder="请输入项目名称" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="拜访类型" path="visitType">
              <n-select v-model:value="form.visitType" :options="[
                { label: '实地拜访', value: 'field' },
                { label: '电话沟通', value: 'phone' },
                { label: '线上会议', value: 'online' }
              ]" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="拜访时间" path="visitTime">
              <n-date-picker v-model:value="form.visitTime" type="datetime" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="时长(分钟)" path="duration">
              <n-input-number v-model:value="form.duration" :min="1" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="拜访地点" path="location">
              <n-input v-model:value="form.location" placeholder="拜访地点" />
            </n-form-item>
          </n-gi>
        </n-grid>
        <n-form-item label="拜访目的" path="purpose">
          <n-input v-model:value="form.purpose" type="textarea" :rows="2" placeholder="简述拜访目的" />
        </n-form-item>
        <n-form-item label="拜访总结" path="summary">
          <n-input v-model:value="form.summary" type="textarea" :rows="4" placeholder="记录拜访要点、客户反馈、关键信息" />
        </n-form-item>
        <n-form-item label="跟进事项" path="followUp">
          <n-input v-model:value="form.followUp" type="textarea" :rows="2" placeholder="需要后续跟进的事项" />
        </n-form-item>
        <n-form-item label="下次计划" path="nextPlan">
          <n-input v-model:value="form.nextPlan" placeholder="下次拜访计划" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showForm = false">取消</n-button>
          <n-button type="primary" @click="handleSubmit">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 详情抽屉 -->
    <n-drawer v-model:show="showDetail" :width="520">
      <n-drawer-content :title="'拜访详情 #' + (detailData?.visit?.id || '')">
        <template v-if="detailData?.visit">
          <n-descriptions :column="2" label-placement="left" bordered size="small">
            <n-descriptions-item label="客户">{{ detailData.visit.customerName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="项目">{{ detailData.visit.projectName || '-' }}</n-descriptions-item>
            <n-descriptions-item label="类型">
              <n-tag :type="visitTypeColor(detailData.visit.visitType)" size="small">{{ visitTypeLabel(detailData.visit.visitType) }}</n-tag>
            </n-descriptions-item>
            <n-descriptions-item label="时间">{{ formatTime(detailData.visit.visitTime) }}</n-descriptions-item>
            <n-descriptions-item label="时长">{{ detailData.visit.duration || '-' }} 分钟</n-descriptions-item>
            <n-descriptions-item label="地点">{{ detailData.visit.location || '-' }}</n-descriptions-item>
          </n-descriptions>

          <n-divider>拜访总结</n-divider>
          <div class="detail-text">{{ detailData.visit.summary || '暂无总结' }}</div>

          <n-divider>跟进事项</n-divider>
          <div class="detail-text">{{ detailData.visit.followUp || '无' }}</div>

          <template v-if="detailData.visit.transcript">
            <n-divider>ASR 转写</n-divider>
            <div class="detail-text transcript">{{ detailData.visit.transcript }}</div>
          </template>

          <template v-if="detailData.visit.aiScore != null">
            <n-divider>AI 评分</n-divider>
            <n-progress type="circle" :percentage="detailData.visit.aiScore" :color="scoreColor(detailData.visit.aiScore)" />
          </template>

          <template v-if="detailData.tags?.length">
            <n-divider>分析标签</n-divider>
            <n-space>
              <n-tag v-for="tag in detailData.tags" :key="tag.id" :type="tagColor(tag.tagType)" size="small">
                {{ tagLabel(tag.tagType) }}: {{ tag.tagContent }}
              </n-tag>
            </n-space>
          </template>

          <n-divider />
          <n-space>
            <n-button size="small" @click="triggerAnalyze" :disabled="!detailData.visit.summary">
              AI 分析
            </n-button>
          </n-space>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, h, onMounted } from 'vue'
import { NButton, NTag, NSpace, useMessage } from 'naive-ui'
import { salesVisitApi } from '@/api/salesVisit'

const message = useMessage()
const visits = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const stats = ref<any>({})
const filter = ref<any>({ visitType: null, dateRange: null })
const showForm = ref(false)
const showDetail = ref(false)
const detailData = ref<any>(null)
const formTitle = ref('新建拜访记录')
const form = ref<any>({})
const formRef = ref()

const fieldCount = computed(() => {
  const byType = stats.value.byType || []
  return byType.find((t: any) => t.visit_type === 'field')?.cnt || 0
})
const phoneCount = computed(() => {
  const byType = stats.value.byType || []
  return byType.find((t: any) => t.visit_type === 'phone')?.cnt || 0
})

function visitTypeLabel(type: string) {
  return { field: '实地拜访', phone: '电话沟通', online: '线上会议' }[type] || type
}
function visitTypeColor(type: string) {
  return ({ field: 'success', phone: 'info', online: 'warning' } as any)[type] || 'default'
}
function tagLabel(type: string) {
  return { need: '需求', objection: '异议', commitment: '承诺', competitor: '竞品', risk: '风险' }[type] || type
}
function tagColor(type: string) {
  return ({ need: 'info', objection: 'warning', commitment: 'success', competitor: 'error', risk: 'error' } as any)[type] || 'default'
}
function scoreColor(score: number) {
  if (score >= 80) return '#18a058'
  if (score >= 60) return '#f0a020'
  return '#d03050'
}
function formatTime(t: string) {
  return t ? t.replace('T', ' ').substring(0, 16) : '-'
}

const columns = [
  { title: '客户', key: 'customerName', width: 100, ellipsis: { tooltip: true } },
  { title: '项目', key: 'projectName', width: 120, ellipsis: { tooltip: true } },
  {
    title: '类型', key: 'visitType', width: 90,
    render: (row: any) => h(NTag, { type: visitTypeColor(row.visitType), size: 'small' }, { default: () => visitTypeLabel(row.visitType) })
  },
  { title: '时间', key: 'visitTime', width: 140, render: (row: any) => formatTime(row.visitTime) },
  { title: '时长', key: 'duration', width: 70, render: (row: any) => (row.duration || '-') + ' 分' },
  { title: '总结', key: 'summary', ellipsis: { tooltip: true } },
  {
    title: 'AI评分', key: 'aiScore', width: 80,
    render: (row: any) => row.aiScore != null ? h(NTag, { type: row.aiScore >= 80 ? 'success' : row.aiScore >= 60 ? 'warning' : 'error', size: 'small' }, { default: () => row.aiScore }) : '-'
  },
  {
    title: '操作', key: 'actions', width: 150,
    render: (row: any) => h(NSpace, { size: 'small' }, {
      default: () => [
        h(NButton, { size: 'tiny', onClick: () => openDetail(row.id) }, { default: () => '详情' }),
        h(NButton, { size: 'tiny', onClick: () => openEdit(row) }, { default: () => '编辑' }),
        h(NButton, { size: 'tiny', type: 'error', onClick: () => handleDelete(row.id) }, { default: () => '删除' }),
      ]
    })
  }
]

async function loadVisits() {
  loading.value = true
  try {
    const params: any = { page: currentPage.value, size: 20 }
    if (filter.value.visitType) params.visitType = filter.value.visitType
    if (filter.value.dateRange) {
      params.startDate = new Date(filter.value.dateRange[0]).toISOString().split('T')[0]
      params.endDate = new Date(filter.value.dateRange[1]).toISOString().split('T')[0]
    }
    const res: any = await salesVisitApi.list(params)
    visits.value = res?.records || (Array.isArray(res) ? res : [])
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res: any = await salesVisitApi.stats()
    stats.value = (res && typeof res === 'object') ? res : {}
  } catch { stats.value = {} }
}

function handlePageChange(page: number) {
  currentPage.value = page
  loadVisits()
}

function openCreate() {
  formTitle.value = '新建拜访记录'
  form.value = { visitType: 'field', duration: 30 }
  showForm.value = true
}

function openEdit(row: any) {
  formTitle.value = '编辑拜访记录'
  form.value = { ...row, visitTime: row.visitTime ? new Date(row.visitTime).getTime() : null }
  showForm.value = true
}

async function handleSubmit() {
  const data = {
    ...form.value,
    visitTime: form.value.visitTime ? new Date(form.value.visitTime).toISOString() : null
  }
  try {
    if (data.id) {
      await salesVisitApi.update(data)
      message.success('更新成功')
    } else {
      await salesVisitApi.create(data)
      message.success('创建成功')
    }
    showForm.value = false
    loadVisits()
    loadStats()
  } catch (e: any) {
    message.error(e.message || '操作失败')
  }
}

async function handleDelete(id: number) {
  await salesVisitApi.remove(id)
  message.success('已删除')
  loadVisits()
  loadStats()
}

async function openDetail(id: number) {
  try {
    const res: any = await salesVisitApi.detail(id)
    detailData.value = res
    showDetail.value = true
  } catch (e: any) {
    message.error('加载失败')
  }
}

async function triggerAnalyze() {
  if (!detailData.value?.visit?.id) return
  const res: any = await salesVisitApi.analyze(detailData.value.visit.id)
  message.info(res?.msg || (typeof res === 'string' ? res : 'AI 分析已触发'))
}

onMounted(() => {
  loadVisits()
  loadStats()
})
</script>

<style scoped>
.visit-container { padding: 4px; }
.stat-card { text-align: center; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.detail-text { padding: 8px 12px; background: #f9f9f9; border-radius: 6px; line-height: 1.6; white-space: pre-wrap; }
.transcript { max-height: 200px; overflow-y: auto; font-size: 13px; color: #666; }
</style>
