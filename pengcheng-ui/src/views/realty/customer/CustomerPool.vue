<template>
  <div class="customer-pool-page">
    <n-grid :cols="24" :x-gap="16">
      <!-- 左侧：公海池客户列表 -->
      <n-gi :span="16">
        <n-card title="公海池客户">
          <template #header-extra>
            <n-space>
              <n-button size="small" @click="loadPublicPool">刷新</n-button>
              <n-button type="primary" size="small" @click="claimSelected" :disabled="selectedRowKeys.length === 0">
                领取选中
              </n-button>
            </n-space>
          </template>

          <n-form inline :model="poolFilter" class="pool-filter">
            <n-form-item label="客户姓氏">
              <n-input v-model:value="poolFilter.customerName" placeholder="请输入" style="width: 150px" clearable />
            </n-form-item>
            <n-form-item label="状态">
              <n-select v-model:value="poolFilter.status" :options="statusOptions" style="width: 120px" clearable />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" @click="handlePoolSearch">查询</n-button>
              <n-button @click="resetPoolFilter">重置</n-button>
            </n-form-item>
          </n-form>

          <n-data-table
            :columns="poolColumns"
            :data="poolData"
            :loading="poolLoading"
            :pagination="poolPagination"
            :row-key="rowKey"
            :checked-row-keys="selectedRowKeys"
            @update:checked-row-keys="selectedRowKeys = $event"
          />
        </n-card>
      </n-gi>

      <!-- 右侧：统计与配置 -->
      <n-gi :span="8">
        <n-card title="公海池统计" style="margin-bottom: 16px">
          <n-space vertical>
            <n-statistic label="公海池客户总数" :value="poolStats.total" />
            <n-statistic label="今日新增" :value="poolStats.todayNew" />
            <n-statistic label="今日领取" :value="poolStats.todayClaimed" />
            <n-statistic label="今日回收" :value="poolStats.todayRecycled" />
          </n-space>
        </n-card>

        <n-card title="回收规则配置">
          <n-form label-placement="left" label-width="100">
            <n-form-item label="无跟进回收">
              <n-input-number v-model:value="poolConfig.noFollowDays" :min="1" :max="90" style="width: 100%">
                <template #suffix>天</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="未到访回收">
              <n-input-number v-model:value="poolConfig.noVisitDays" :min="1" :max="180" style="width: 100%">
                <template #suffix>天</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="保护期">
              <n-input-number v-model:value="poolConfig.protectionDays" :min="1" :max="30" style="width: 100%">
                <template #suffix>天</template>
              </n-input-number>
            </n-form-item>
            <n-form-item label="自动回收">
              <n-switch v-model:value="poolConfig.autoRecycleEnabled" />
            </n-form-item>
            <n-form-item>
              <n-button type="primary" :loading="configSaving" @click="savePoolConfig">保存配置</n-button>
            </n-form-item>
          </n-form>
        </n-card>

        <n-card title="回收日志" style="margin-top: 16px">
          <n-timeline>
            <n-timeline-item
              v-for="log in recycleLogs"
              :key="log.id"
              :type="log.type"
              :title="log.title"
              :content="log.content"
              :time="log.time"
            />
          </n-timeline>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 领取确认弹窗 -->
    <n-modal v-model:show="showClaimConfirm" preset="card" title="确认领取" style="width: 400px">
      <p>确定要领取选中的 {{ selectedRowKeys.length }} 个客户吗？</p>
      <p style="color: var(--n-text-color-3); font-size: 13px; margin-top: 8px">
        领取后客户将转入您的私海池，保护期为 {{ poolConfig.protectionDays }} 天。
      </p>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showClaimConfirm = false">取消</n-button>
          <n-button type="primary" :loading="claiming" @click="confirmClaim">确认</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, h, onMounted } from 'vue'
import { NButton, NTag, NStatistic, NTimeline, NTimelineItem, useMessage, type DataTableColumns } from 'naive-ui'
import { realtyApi } from '@/api/realty'
import { request } from '@/utils/request'

interface PoolCustomer {
  id: number
  customerName: string
  phoneMasked: string
  status: number
  poolType: number
  creatorId: number
  createTime: string
  lastFollowTime: string
  allianceName?: string
  projectName?: string
}

interface PoolStats {
  total: number
  todayNew: number
  todayClaimed: number
  todayRecycled: number
}

interface RecycleLog {
  id: number
  type: 'default' | 'info' | 'success' | 'warning' | 'error'
  title: string
  content: string
  time: string
}

const message = useMessage()

const poolFilter = reactive({
  customerName: '',
  status: null as number | null
})

const poolData = ref<PoolCustomer[]>([])
const poolLoading = ref(false)
const poolPagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0
})

const selectedRowKeys = ref<number[]>([])
const poolStats = ref<PoolStats>({ total: 0, todayNew: 0, todayClaimed: 0, todayRecycled: 0 })

const poolConfig = reactive({
  noFollowDays: 7,
  noVisitDays: 30,
  protectionDays: 3,
  autoRecycleEnabled: true
})

const configSaving = ref(false)
const claiming = ref(false)
const showClaimConfirm = ref(false)

const recycleLogs = ref<RecycleLog[]>([
  { id: 1, type: 'success', title: '自动回收执行', content: '回收 3 个客户至公海池', time: '2026-03-12 02:00:00' },
  { id: 2, type: 'info', title: '用户领取', content: '用户 1001 领取客户 2001', time: '2026-03-11 15:30:00' }
])

const statusOptions = [
  { label: '已报备', value: 1 },
  { label: '已到访', value: 2 },
  { label: '已成交', value: 3 }
]

const poolColumns: DataTableColumns<PoolCustomer> = [
  { type: 'selection', fixed: 'left' },
  { title: '客户姓氏', key: 'customerName', width: 100 },
  { title: '联系方式', key: 'phoneMasked', width: 120 },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => h(NTag, {
      size: 'small',
      type: row.status === 1 ? 'info' : row.status === 2 ? 'default' : 'success'
    }, { default: () => statusText(row.status) })
  },
  { title: '联盟商', key: 'allianceName', width: 150, ellipsis: { tooltip: true } },
  { title: '项目', key: 'projectName', width: 150, ellipsis: { tooltip: true } },
  { title: '最后跟进', key: 'lastFollowTime', width: 170 },
  { title: '创建时间', key: 'createTime', width: 170 },
  {
    title: '操作', key: 'action', width: 100, fixed: 'right',
    render: (row) => h(NButton, {
      size: 'small',
      type: 'primary',
      tertiary: true,
      onClick: () => claimSingle(row.id)
    }, { default: () => '领取' })
  }
]

function rowKey(row: PoolCustomer) {
  return row.id
}

function statusText(status?: number) {
  if (status === 1) return '已报备'
  if (status === 2) return '已到访'
  if (status === 3) return '已成交'
  return '-'
}

async function loadPublicPool() {
  poolLoading.value = true
  try {
    const res: any = await request({
      url: '/realty/customer/pool/public',
      method: 'get',
      params: {
        page: poolPagination.page,
        pageSize: poolPagination.pageSize,
        customerName: poolFilter.customerName || undefined,
        status: poolFilter.status || undefined
      }
    })
    poolData.value = res?.list || res?.data?.list || []
    poolPagination.itemCount = res?.total || res?.data?.total || 0
  } catch (err: any) {
    message.error(err?.message || '加载失败')
  } finally {
    poolLoading.value = false
  }
}

async function loadPoolStats() {
  try {
    const res: any = await request({ url: '/realty/customer/pool/stats', method: 'get' })
    poolStats.value = res?.data || res || { total: 0, todayNew: 0, todayClaimed: 0, todayRecycled: 0 }
  } catch {
    // 静默失败
  }
}

async function loadPoolConfig() {
  try {
    const res: any = await request({ url: '/realty/customer/pool/config', method: 'get' })
    const config = res?.data || res || {}
    poolConfig.noFollowDays = config.noFollowDays || 7
    poolConfig.noVisitDays = config.noVisitDays || 30
    poolConfig.protectionDays = config.protectionDays || 3
    poolConfig.autoRecycleEnabled = config.autoRecycleEnabled !== false
  } catch {
    // 使用默认值
  }
}

async function savePoolConfig() {
  configSaving.value = true
  try {
    await request({
      url: '/realty/customer/pool/config',
      method: 'put',
      data: poolConfig
    })
    message.success('配置已保存')
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  } finally {
    configSaving.value = false
  }
}

function handlePoolSearch() {
  poolPagination.page = 1
  loadPublicPool()
}

function resetPoolFilter() {
  poolFilter.customerName = ''
  poolFilter.status = null
  handlePoolSearch()
}

function handlePoolPageChange(page: number) {
  poolPagination.page = page
  loadPublicPool()
}

function claimSingle(id: number) {
  selectedRowKeys.value = [id]
  showClaimConfirm.value = true
}

function claimSelected() {
  showClaimConfirm.value = true
}

async function confirmClaim() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请选择要领取的客户')
    return
  }
  claiming.value = true
  try {
    await request({
      url: '/realty/customer/pool/claim',
      method: 'post',
      data: { customerIds: selectedRowKeys.value }
    })
    message.success('领取成功')
    selectedRowKeys.value = []
    showClaimConfirm.value = false
    loadPublicPool()
    loadPoolStats()
  } catch (err: any) {
    message.error(err?.message || '领取失败')
  } finally {
    claiming.value = false
  }
}

onMounted(() => {
  loadPublicPool()
  loadPoolStats()
  loadPoolConfig()
})
</script>

<style scoped>
.customer-pool-page {
  padding: 20px;
}
.pool-filter {
  margin-bottom: 16px;
}
</style>
