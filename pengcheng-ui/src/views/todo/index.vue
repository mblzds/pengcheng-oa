<template>
  <div class="todo-container">
    <!-- 顶部统计 -->
    <n-grid :cols="4" :x-gap="16" :y-gap="16" style="margin-bottom: 16px">
      <n-gi>
        <n-card size="small">
          <n-statistic label="待处理" :value="pendingCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="进行中" :value="inProgressCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="今日到期" :value="dueTodayCount" />
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small">
          <n-statistic label="已完成" :value="completedCount" />
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="待办事项">
      <template #header-extra>
        <n-space>
          <n-select v-model:value="filterStatus" :options="statusOptions" size="small" style="width: 120px" clearable placeholder="全部状态" />
          <n-button type="primary" size="small" @click="showCreate = true">
            <template #icon><n-icon :component="AddOutline" /></template>
            新建待办
          </n-button>
        </n-space>
      </template>

      <div class="todo-list">
        <div v-for="todo in filteredTodos" :key="todo.id" :class="['todo-item', 'priority-' + todo.priority, 'status-' + todo.status]">
          <div class="todo-check" @click="toggleComplete(todo)">
            <n-icon :component="todo.status === 2 ? CheckmarkCircle : EllipseOutline" :size="22"
                    :color="todo.status === 2 ? '#18a058' : '#ccc'" />
          </div>

          <div class="todo-content">
            <div class="todo-title" :class="{ completed: todo.status === 2 }">
              {{ todo.title }}
              <n-tag v-if="todo.priority === 2" size="tiny" type="error">紧急</n-tag>
              <n-tag v-else-if="todo.priority === 1" size="tiny" type="warning">重要</n-tag>
              <n-tag v-if="todo.sourceType === 'chat'" size="tiny" type="info">聊天提取</n-tag>
            </div>
            <div class="todo-meta">
              <span v-if="todo.dueDate" :class="{ overdue: isOverdue(todo) }">
                截止：{{ formatDate(todo.dueDate) }}
              </span>
              <span v-if="todo.description" class="todo-desc">{{ todo.description }}</span>
            </div>
          </div>

          <div class="todo-actions">
            <n-button v-if="todo.status === 0" size="tiny" @click="startTodo(todo)">开始</n-button>
            <n-button v-if="todo.status < 2" size="tiny" type="error" @click="cancelTodo(todo.id)">取消</n-button>
            <n-button size="tiny" quaternary @click="deleteTodo(todo.id)">
              <template #icon><n-icon :component="TrashOutline" size="14" /></template>
            </n-button>
          </div>
        </div>

        <n-empty v-if="filteredTodos.length === 0" description="暂无待办事项" />
      </div>
    </n-card>

    <!-- 新建待办弹窗 -->
    <n-modal v-model:show="showCreate" preset="dialog" title="新建待办" style="width: 500px">
      <n-form label-placement="left" label-width="70">
        <n-form-item label="标题">
          <n-input v-model:value="newTodo.title" placeholder="待办事项标题" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="newTodo.description" type="textarea" placeholder="详细描述（可选）" :rows="3" />
        </n-form-item>
        <n-form-item label="优先级">
          <n-radio-group v-model:value="newTodo.priority">
            <n-radio :value="0">普通</n-radio>
            <n-radio :value="1">重要</n-radio>
            <n-radio :value="2">紧急</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="截止日期">
          <n-date-picker v-model:value="newTodoDueTs" type="datetime" clearable style="width: 100%" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showCreate = false">取消</n-button>
        <n-button type="primary" @click="handleCreate">创建</n-button>
      </template>
    </n-modal>

    <!-- AI 提取待办 -->
    <n-card title="从消息提取待办" size="small" style="margin-top: 16px">
      <n-input-group>
        <n-input v-model:value="extractContent" placeholder="粘贴聊天消息内容，AI 自动识别待办..." type="textarea" :rows="2" />
        <n-button type="primary" style="height: auto" :loading="extracting" @click="extractTodos">提取待办</n-button>
      </n-input-group>
      <div v-if="extractedTodos.length > 0" class="extracted-list">
        <div v-for="(todo, i) in extractedTodos" :key="i" class="extracted-item">
          <n-icon :component="CheckmarkCircleOutline" color="#18a058" />
          <span>{{ todo.title }}</span>
          <n-tag size="tiny">已添加</n-tag>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  NCard, NGrid, NGi, NStatistic, NButton, NIcon, NTag, NSpace,
  NSelect, NModal, NForm, NFormItem, NInput, NInputGroup, NRadioGroup,
  NRadio, NDatePicker, NEmpty, useMessage
} from 'naive-ui'
import {
  AddOutline, TrashOutline, CheckmarkCircle, EllipseOutline, CheckmarkCircleOutline
} from '@vicons/ionicons5'
import { request } from '@/utils/request'

const message = useMessage()

const todos = ref<any[]>([])
const filterStatus = ref<number | null>(null)
const showCreate = ref(false)
const newTodo = ref({ title: '', description: '', priority: 0 })
const newTodoDueTs = ref<number | null>(null)
const extractContent = ref('')
const extracting = ref(false)
const extractedTodos = ref<any[]>([])

const statusOptions = [
  { label: '待办', value: 0 },
  { label: '进行中', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: 3 }
]

const filteredTodos = computed(() => {
  if (filterStatus.value !== null) {
    return todos.value.filter(t => t.status === filterStatus.value)
  }
  return todos.value
})

const pendingCount = computed(() => todos.value.filter(t => t.status === 0).length)
const inProgressCount = computed(() => todos.value.filter(t => t.status === 1).length)
const completedCount = computed(() => todos.value.filter(t => t.status === 2).length)
const dueTodayCount = computed(() => {
  const today = new Date().toDateString()
  return todos.value.filter(t => t.status < 2 && t.dueDate && new Date(t.dueDate).toDateString() === today).length
})

const formatDate = (d: string) => d ? new Date(d).toLocaleString('zh-CN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''
const isOverdue = (todo: any) => todo.status < 2 && todo.dueDate && new Date(todo.dueDate) < new Date()

onMounted(() => loadTodos())

async function loadTodos() {
  try {
    const list = await request<unknown[]>({
      url: '/todo/list',
      method: 'get',
      params: filterStatus.value != null ? { status: filterStatus.value } : undefined
    })
    todos.value = Array.isArray(list) ? list : []
  } catch {
    todos.value = []
  }
}

async function handleCreate() {
  if (!newTodo.value.title) {
    message.warning('请输入待办标题')
    return
  }
  const body: any = { ...newTodo.value }
  if (newTodoDueTs.value) {
    body.dueDate = new Date(newTodoDueTs.value).toISOString()
  }
  try {
    await request({ url: '/todo/create', method: 'post', data: body })
    message.success('已创建')
    showCreate.value = false
    newTodo.value = { title: '', description: '', priority: 0 }
    newTodoDueTs.value = null
    loadTodos()
  } catch {
    message.error('创建失败')
  }
}

async function toggleComplete(todo: any) {
  if (todo.status === 2) return
  try {
    await request({ url: `/todo/complete/${todo.id}`, method: 'post' })
    message.success('已完成')
    loadTodos()
  } catch {
    message.error('操作失败')
  }
}

async function startTodo(todo: any) {
  try {
    await request({ url: '/todo/update', method: 'put', data: { ...todo, status: 1 } })
    message.success('已开始')
    loadTodos()
  } catch {
    message.error('操作失败')
  }
}

async function cancelTodo(id: number) {
  try {
    await request({ url: `/todo/cancel/${id}`, method: 'post' })
    message.success('已取消')
    loadTodos()
  } catch {
    message.error('操作失败')
  }
}

async function deleteTodo(id: number) {
  try {
    await request({ url: `/todo/${id}`, method: 'delete' })
    loadTodos()
  } catch {
    message.error('删除失败')
  }
}

async function extractTodos() {
  if (!extractContent.value.trim()) {
    message.warning('请输入消息内容')
    return
  }
  extracting.value = true
  try {
    const list = await request<unknown[]>({
      url: '/todo/extract',
      method: 'post',
      data: { content: extractContent.value }
    })
    extractedTodos.value = Array.isArray(list) ? list : []
    if (extractedTodos.value.length > 0) {
      message.success(`已提取 ${extractedTodos.value.length} 条待办`)
      loadTodos()
    } else {
      message.info('未识别到待办事项')
    }
  } catch {
    message.error('提取失败')
  } finally {
    extracting.value = false
  }
}
</script>

<style scoped>
.todo-container { padding: 0; }
.todo-list { display: flex; flex-direction: column; gap: 4px; }
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 8px;
  transition: background 0.2s;
  border-left: 3px solid transparent;
}
.todo-item:hover { background: rgba(0,0,0,0.02); }
.todo-item.priority-2 { border-left-color: #d03050; }
.todo-item.priority-1 { border-left-color: #f0a020; }
.todo-item.status-2 { opacity: 0.6; }
.todo-check { cursor: pointer; flex-shrink: 0; }
.todo-content { flex: 1; min-width: 0; }
.todo-title { font-size: 14px; display: flex; align-items: center; gap: 6px; }
.todo-title.completed { text-decoration: line-through; color: #999; }
.todo-meta { font-size: 12px; color: #999; margin-top: 4px; display: flex; gap: 12px; }
.todo-meta .overdue { color: #d03050; }
.todo-desc { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 300px; }
.todo-actions { display: flex; gap: 4px; flex-shrink: 0; }
.extracted-list { margin-top: 12px; display: flex; flex-direction: column; gap: 6px; }
.extracted-item { display: flex; align-items: center; gap: 8px; font-size: 13px; padding: 4px 0; }
</style>
