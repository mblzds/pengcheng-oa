<template>
  <div class="project-container">
    <n-card title="项目管理">
      <n-space vertical>
        <n-space>
          <n-select v-model:value="scope" :options="scopeOptions" style="width: 140px" @update:value="loadList" />
          <n-select v-model:value="statusFilter" :options="statusOptions" clearable placeholder="状态" style="width: 120px" @update:value="loadList" />
          <n-button type="primary" @click="showCreate = true">新建项目</n-button>
        </n-space>
        <n-data-table :columns="columns" :data="list" :loading="loading" :pagination="pagination" />
      </n-space>
    </n-card>

    <n-drawer v-model:show="detailVisible" :width="480" placement="right">
      <n-drawer-content v-if="currentProject" :title="currentProject.name" closable>
        <n-descriptions :column="1" bordered size="small">
          <n-descriptions-item label="状态">{{ statusMap[currentProject.status] }}</n-descriptions-item>
          <n-descriptions-item label="计划">{{ currentProject.startDate }} ~ {{ currentProject.endDate }}</n-descriptions-item>
          <n-descriptions-item label="可见性">{{ currentProject.visibility }}</n-descriptions-item>
        </n-descriptions>
        <n-divider />
        <h4>统计</h4>
        <n-grid :cols="2" :x-gap="12">
          <n-gi><n-statistic label="任务总数" :value="stats.totalTasks" /></n-gi>
          <n-gi><n-statistic label="已完成" :value="stats.completedTasks" /></n-gi>
          <n-gi><n-statistic label="逾期" :value="stats.overdueTasks" /></n-gi>
          <n-gi><n-statistic label="完成率" :value="`${(stats.completionRate || 0).toFixed(1)}%`" /></n-gi>
        </n-grid>
        <n-divider />
        <h4>任务列表</h4>
        <n-data-table :columns="taskColumns" :data="taskList" :max-height="240" size="small" />
        <template #footer>
          <n-button type="primary" size="small" @click="goDetail">进入项目</n-button>
        </template>
      </n-drawer-content>
    </n-drawer>

    <n-modal v-model:show="showCreate" preset="card" title="新建项目" style="width: 480px">
      <n-form ref="formRef" :model="form" label-placement="left" label-width="80">
        <n-form-item label="名称" path="name" required>
          <n-input v-model:value="form.name" placeholder="项目名称" />
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="form.description" type="textarea" placeholder="项目描述" />
        </n-form-item>
        <n-form-item label="可见性" path="visibility">
          <n-select v-model:value="form.visibility" :options="visibilityOptions" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showCreate = false">取消</n-button>
          <n-button type="primary" :disabled="!form.name?.trim()" @click="handleCreate">创建</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { projectApi, projectTaskApi } from '@/api/project'
import type { DataTableColumns } from 'naive-ui'

const router = useRouter()
const scope = ref<string>('all')
const scopeOptions = [
  { label: '全部', value: 'all' },
  { label: '我创建的', value: 'my_created' },
  { label: '我参与的', value: 'my_joined' },
]
const statusFilter = ref<number | null>(null)
const statusMap: Record<number, string> = { 1: '未开始', 2: '进行中', 3: '已暂停', 4: '已完成', 5: '已归档' }
const statusOptions = Object.entries(statusMap).map(([k, v]) => ({ label: v, value: Number(k) }))
const visibilityOptions = [
  { label: '仅成员', value: 'private' },
  { label: '本部门', value: 'dept' },
  { label: '全公司', value: 'all' },
]

const list = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 20, itemCount: 0 })

const columns: DataTableColumns<any> = [
  { title: 'ID', key: 'id', width: 70 },
  { title: '名称', key: 'name', ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 90, render: (row) => statusMap[row.status] ?? row.status },
  { title: '开始', key: 'startDate', width: 110 },
  { title: '结束', key: 'endDate', width: 110 },
  { title: '操作', key: 'action', width: 100, render: (row) => h('span', { class: 'link', onClick: () => openDetail(row) }, '查看') },
]

const detailVisible = ref(false)
const currentProject = ref<any>(null)
const stats = ref<Record<string, any>>({})
const taskList = ref<any[]>([])
const taskColumns: DataTableColumns<any> = [
  { title: '标题', key: 'title', ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 80 },
  { title: '进度', key: 'progress', width: 70, render: (row) => `${row.progress ?? 0}%` },
]

const showCreate = ref(false)
const formRef = ref()
const form = reactive({ name: '', description: '', visibility: 'private' })

function loadList() {
  loading.value = true
  projectApi.list({ page: pagination.page, size: pagination.pageSize, scope: scope.value || undefined, status: statusFilter.value ?? undefined })
    .then((res: any) => {
      const data = res?.data ?? res
      list.value = data?.records ?? data ?? []
      pagination.itemCount = data?.total ?? list.value.length
    })
    .finally(() => { loading.value = false })
}

function openDetail(row: any) {
  currentProject.value = row
  detailVisible.value = true
  projectApi.stats(row.id).then((res: any) => { stats.value = res?.data ?? res ?? {} })
  projectTaskApi.page(row.id, { page: 1, size: 10 }).then((res: any) => {
    const d = res?.data ?? res
    taskList.value = d?.records ?? d ?? []
  })
}

function goDetail() {
  if (currentProject.value?.id) router.push({ name: 'ProjectDetail', params: { id: currentProject.value.id } })
}

function handleCreate() {
  if (!form.name?.trim()) return
  projectApi.create({ name: form.name, description: form.description, visibility: form.visibility })
    .then(() => { showCreate.value = false; form.name = ''; form.description = ''; loadList() })
}

onMounted(loadList)
</script>

<style scoped>
.project-container { padding: 16px; }
.link { color: var(--n-primary-color); cursor: pointer; }
</style>
