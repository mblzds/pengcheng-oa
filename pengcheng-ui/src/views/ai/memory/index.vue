<template>
  <div class="memory-management">
    <div class="page-header">
      <h2>AI 记忆管理</h2>
      <p class="desc">查看、搜索、管理 AI 系统的记忆数据</p>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-bar">
      <n-input v-model:value="searchKeyword" placeholder="搜索记忆内容..." clearable style="width: 300px" @keyup.enter="loadMemories">
        <template #prefix><n-icon><SearchOutline /></n-icon></template>
      </n-input>
      <n-select v-model:value="filterType" placeholder="记忆类型" :options="typeOptions" clearable style="width: 150px" />
      <n-select v-model:value="filterLevel" placeholder="记忆层级" :options="levelOptions" clearable style="width: 120px" />
      <n-button type="primary" @click="loadMemories">搜索</n-button>
      <n-button @click="resetFilter">重置</n-button>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-num">{{ stats.total }}</div>
        <div class="stat-label">总记忆数</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.l2Count }}</div>
        <div class="stat-label">长期记忆</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.l1Count }}</div>
        <div class="stat-label">短期记忆</div>
      </div>
      <div class="stat-item">
        <div class="stat-num">{{ stats.profileCount }}</div>
        <div class="stat-label">客户画像</div>
      </div>
    </div>

    <!-- 记忆列表 -->
    <n-data-table
      :columns="columns"
      :data="memories"
      :loading="loading"
      :row-key="(row: any) => row.id"
      :pagination="pagination"
      striped
      @update:page="handlePageChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { NButton, NTag, NIcon, NSpace, NPopconfirm, useMessage } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

const message = useMessage()
const searchKeyword = ref('')
const filterType = ref(null)
const filterLevel = ref(null)
const loading = ref(false)
const memories = ref<any[]>([])
const stats = reactive({ total: 0, l2Count: 0, l1Count: 0, profileCount: 0 })

const typeOptions = [
  { label: '事实', value: 'fact' },
  { label: '偏好', value: 'preference' },
  { label: '决策', value: 'decision' },
  { label: '事件', value: 'event' },
  { label: '画像', value: 'profile' }
]

const levelOptions = [
  { label: 'L1 短期', value: 'L1' },
  { label: 'L2 长期', value: 'L2' }
]

const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50]
})

const columns = [
  { title: 'ID', key: 'id', width: 70 },
  {
    title: '类型',
    key: 'memoryType',
    width: 80,
    render(row: any) {
      const typeMap: Record<string, string> = { fact: '事实', preference: '偏好', decision: '决策', event: '事件', profile: '画像' }
      const colorMap: Record<string, string> = { fact: 'info', preference: 'success', decision: 'warning', event: 'default', profile: 'error' }
      return h(NTag, { size: 'small', type: colorMap[row.memoryType] || 'default' as any }, { default: () => typeMap[row.memoryType] || row.memoryType })
    }
  },
  {
    title: '层级',
    key: 'memoryLevel',
    width: 80,
    render(row: any) {
      return h(NTag, { size: 'small', type: row.memoryLevel === 'L2' ? 'success' : 'info' as any }, { default: () => row.memoryLevel })
    }
  },
  {
    title: '内容',
    key: 'content',
    ellipsis: { tooltip: true },
    minWidth: 300
  },
  {
    title: '重要度',
    key: 'importance',
    width: 90,
    render(row: any) {
      const val = Number(row.importance || 0)
      const color = val >= 0.7 ? '#d03050' : val >= 0.5 ? '#f0a020' : '#999'
      return h('span', { style: { color, fontWeight: 'bold' } }, val.toFixed(2))
    }
  },
  { title: '访问', key: 'accessCount', width: 60 },
  { title: '来源', key: 'source', width: 80 },
  { title: '标签', key: 'tags', width: 120, ellipsis: { tooltip: true } },
  { title: '创建时间', key: 'createdAt', width: 160 },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    fixed: 'right' as const,
    render(row: any) {
      return h(NSpace, { size: 'small' }, {
        default: () => [
          row.memoryLevel === 'L1'
            ? h(NButton, { size: 'small', type: 'info', onClick: () => promoteMemory(row.id) }, { default: () => '升级L2' })
            : null,
          h(NPopconfirm, { onPositiveClick: () => deleteMemory(row.id) }, {
            trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
            default: () => '确定删除此记忆？'
          })
        ]
      })
    }
  }
]

async function loadMemories() {
  loading.value = true
  try {
    const params: any = { page: pagination.page, pageSize: pagination.pageSize }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filterType.value) params.type = filterType.value
    if (filterLevel.value) params.level = filterLevel.value

    const res: any = await request({ url: '/ai/memory/list', method: 'get', params })
    if (res) {
      memories.value = res.list || (Array.isArray(res) ? res : [])
      pagination.itemCount = res.total || memories.value.length
    }
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res: any = await request({ url: '/ai/memory/stats', method: 'get' })
    if (res && typeof res === 'object') Object.assign(stats, res)
  } catch { /* ignore */ }
}

async function promoteMemory(id: number) {
  try {
    await request({ url: `/ai/memory/${id}/promote`, method: 'post' })
    message.success('已升级为长期记忆')
    loadMemories()
    loadStats()
  } catch { message.error('操作失败') }
}

async function deleteMemory(id: number) {
  try {
    await request({ url: `/ai/memory/${id}`, method: 'delete' })
    message.success('已删除')
    loadMemories()
    loadStats()
  } catch { message.error('删除失败') }
}

function handlePageChange(page: number) {
  pagination.page = page
  loadMemories()
}

function resetFilter() {
  searchKeyword.value = ''
  filterType.value = null
  filterLevel.value = null
  pagination.page = 1
  loadMemories()
}

onMounted(() => {
  loadMemories()
  loadStats()
})
</script>

<style scoped>
.memory-management {
  padding: 20px;
}
.page-header h2 {
  margin: 0 0 4px;
}
.page-header .desc {
  color: #999;
  font-size: 13px;
  margin: 0 0 20px;
}
.filter-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}
.stat-item {
  flex: 1;
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
  text-align: center;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}
.stat-label {
  font-size: 13px;
  color: #999;
  margin-top: 4px;
}
</style>
