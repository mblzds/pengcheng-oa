<template>
  <div class="page-container">
    <n-card title="签到记录">
      <!-- 筛选区：用户 + 日期范围 -->
      <n-form inline :model="filter" class="filter">
        <n-form-item v-if="!isEmployeeOnly" label="选择用户">
          <n-select
            v-model:value="filter.userId"
            :options="userOptions"
            label-field="nickname"
            value-field="id"
            filterable
            placeholder="不选则显示可见范围内的全员"
            clearable
            style="width: 220px"
          />
        </n-form-item>
        <n-form-item label="日期范围">
          <n-date-picker
            v-model:value="filter.dateRange"
            type="daterange"
            clearable
            :shortcuts="dateShortcuts"
            :update-value-on-close="true"
            format="yyyy-MM-dd"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 320px"
          />
        </n-form-item>
        <n-form-item>
          <n-space>
            <n-button type="primary" @click="onSearch">查询</n-button>
            <n-button @click="onReset">重置</n-button>
          </n-space>
        </n-form-item>
      </n-form>

      <n-data-table
        remote
        :columns="columns"
        :data="rows"
        :loading="loading"
        :pagination="pagination"
        :row-key="(row: SignInRecordItem) => row.id!"
        @update:page="onPageChange"
        @update:page-size="onPageSizeChange"
        style="margin-top: 12px"
      />
    </n-card>

    <!-- 详情抽屉：放大看照片 + 完整地址 + 备注 -->
    <n-drawer v-model:show="detailVisible" :width="520" placement="right">
      <n-drawer-content title="签到详情" closable>
        <template v-if="detailRow">
          <n-descriptions label-placement="left" :column="1" bordered size="small">
            <n-descriptions-item label="签到人">
              {{ detailRow.userName || '-' }}
              <span v-if="detailRow.employeeNo" style="color:#999;margin-left:6px">{{ detailRow.employeeNo }}</span>
              <span v-if="detailRow.deptName" style="color:#999;margin-left:6px">· {{ detailRow.deptName }}</span>
            </n-descriptions-item>
            <n-descriptions-item label="签到时间">{{ formatDateTime(detailRow.signInTime) }}</n-descriptions-item>
            <n-descriptions-item label="地点">
              <span v-if="detailRow.address">{{ detailRow.address }}</span>
              <span v-else-if="detailRow.location" style="color:#999">{{ detailRow.location }}（未解析）</span>
              <span v-else>-</span>
            </n-descriptions-item>
            <n-descriptions-item v-if="detailRow.latitude != null && detailRow.longitude != null" label="经纬度">
              {{ detailRow.latitude }}, {{ detailRow.longitude }}
            </n-descriptions-item>
            <n-descriptions-item label="备注">{{ detailRow.remark || '-' }}</n-descriptions-item>
            <n-descriptions-item label="照片">
              <n-image v-if="detailRow.photoUrl" :src="detailRow.photoUrl" width="320" />
              <span v-else>-</span>
            </n-descriptions-item>
          </n-descriptions>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, reactive, ref } from 'vue'
import { NImage, NButton, NTag } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { request } from '@/utils/request'
import { attendanceApi, type SignInRecordItem } from '@/api/attendance'

const loading = ref(false)
const userOptions = ref<any[]>([])
const rows = ref<SignInRecordItem[]>([])

// 默认查最近 7 天，避免一上来就触发跨年大查询
function defaultRange(): [number, number] {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime()
  const sevenAgo = today - 6 * 86400000
  return [sevenAgo, today + 86400000 - 1]
}

const filter = reactive<{
  userId: number | null
  dateRange: [number, number] | null
}>({
  userId: null,
  dateRange: defaultRange()
})

const dateShortcuts = {
  '今天': (): [number, number] => {
    const d = new Date()
    const start = new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
    return [start, start + 86400000 - 1]
  },
  '最近 7 天': (): [number, number] => defaultRange(),
  '本月': (): [number, number] => {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth(), 1).getTime()
    return [first, defaultRange()[1]]
  },
  '上月': (): [number, number] => {
    const d = new Date()
    const first = new Date(d.getFullYear(), d.getMonth() - 1, 1).getTime()
    const last = new Date(d.getFullYear(), d.getMonth(), 0, 23, 59, 59, 999).getTime()
    return [first, last]
  }
}

// 普通员工 = 后端 visible-users 只回自己
const isEmployeeOnly = computed(() => userOptions.value.length === 1)

// 服务端分页：pagination 受控
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0,
  showSizePicker: true,
  pageSizes: [10, 20, 50, 100],
  prefix: ({ itemCount }: { itemCount: number }) => `共 ${itemCount} 条`
})

const detailVisible = ref(false)
const detailRow = ref<SignInRecordItem | null>(null)
function openDetail(row: SignInRecordItem) {
  detailRow.value = row
  detailVisible.value = true
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  // 后端 LocalDateTime 序列化通常带 T；统一替换为空格便于展示
  return String(value).replace('T', ' ').slice(0, 19)
}

const columns: DataTableColumns<SignInRecordItem> = [
  { title: '签到人', key: 'userName', width: 110, render: (row) => row.userName || '-' },
  { title: '工号', key: 'employeeNo', width: 100, render: (row) => row.employeeNo || '-' },
  { title: '部门', key: 'deptName', width: 130, render: (row) => row.deptName || '-' },
  { title: '签到时间', key: 'signInTime', width: 170, render: (row) => formatDateTime(row.signInTime) },
  {
    title: '地点',
    key: 'address',
    ellipsis: { tooltip: true },
    render: (row) => {
      if (row.address) return row.address
      if (row.location) return h('span', { style: 'color:#999' }, [row.location, ' （未解析）'])
      return '-'
    }
  },
  {
    title: '照片',
    key: 'photoUrl',
    width: 90,
    render: (row) =>
      row.photoUrl
        ? h(NImage, {
            src: row.photoUrl,
            width: 60,
            height: 60,
            objectFit: 'cover',
            previewDisabled: false
          })
        : '-'
  },
  { title: '备注', key: 'remark', ellipsis: { tooltip: true }, render: (row) => row.remark || '-' },
  {
    title: '操作',
    key: 'actions',
    width: 90,
    fixed: 'right',
    render: (row) =>
      h(NButton, { text: true, type: 'primary', onClick: () => openDetail(row) }, () => '详情')
  }
]

function rangeToParams() {
  if (!filter.dateRange) return { startDate: undefined, endDate: undefined }
  const [s, e] = filter.dateRange
  const fmt = (ts: number) => {
    const d = new Date(ts)
    const pad = (v: number) => String(v).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  }
  return { startDate: fmt(s), endDate: fmt(e) }
}

async function loadData() {
  loading.value = true
  try {
    const { startDate, endDate } = rangeToParams()
    const res: any = await attendanceApi.signInList({
      userId: filter.userId ?? undefined,
      startDate,
      endDate,
      page: pagination.page,
      size: pagination.pageSize
    })
    // 后端返回 IPage，可能直接是 {records,total,...}，也可能被外层 Result 解包后是这个对象
    const records = res?.records ?? []
    const total = res?.total ?? 0
    rows.value = records
    pagination.itemCount = total
  } catch (err: any) {
    rows.value = []
    pagination.itemCount = 0
    console.error('加载签到记录失败', err)
  } finally {
    loading.value = false
  }
}

function onSearch() {
  pagination.page = 1
  loadData()
}

function onReset() {
  filter.userId = null
  filter.dateRange = defaultRange()
  pagination.page = 1
  loadData()
}

function onPageChange(page: number) {
  pagination.page = page
  loadData()
}

function onPageSizeChange(size: number) {
  pagination.pageSize = size
  pagination.page = 1
  loadData()
}

async function loadUsers() {
  try {
    const list: any = await request({ url: '/admin/attendance/visible-users', method: 'get' })
    userOptions.value = Array.isArray(list) ? list : []
    // 普通员工：自动锁定为自己
    if (userOptions.value.length === 1) {
      filter.userId = userOptions.value[0].id
    }
  } catch (err) {
    userOptions.value = []
  }
}

onMounted(async () => {
  await loadUsers()
  await loadData()
})
</script>

<style scoped>
.page-container {
  padding: 16px;
}
.filter :deep(.n-form-item) {
  margin-bottom: 0;
}
</style>
