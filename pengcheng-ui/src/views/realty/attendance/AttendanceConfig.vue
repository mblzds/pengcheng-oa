<template>
  <div class="attendance-config-page">
    <n-card title="考勤设置" :bordered="false">
      <n-spin :show="loading">
        <n-form :model="form" label-placement="left" label-width="140" require-mark-placement="right-hanging">
          <n-divider title-placement="left">上下班时间</n-divider>

          <n-form-item label="启用时间校验">
            <n-switch v-model:value="form.enforceTime" />
            <span class="hint">关闭后不再判定迟到/早退</span>
          </n-form-item>

          <n-form-item label="上班时间">
            <n-time-picker
              v-model:formatted-value="form.workStartTime"
              format="HH:mm"
              value-format="HH:mm"
              :disabled="!form.enforceTime"
              style="width: 200px"
            />
          </n-form-item>

          <n-form-item label="下班时间">
            <n-time-picker
              v-model:formatted-value="form.workEndTime"
              format="HH:mm"
              value-format="HH:mm"
              :disabled="!form.enforceTime"
              style="width: 200px"
            />
          </n-form-item>

          <n-divider title-placement="left">考勤起算</n-divider>

          <n-form-item label="考勤启用日期">
            <n-date-picker
              v-model:formatted-value="form.startDate"
              value-format="yyyy-MM-dd"
              type="date"
              clearable
              style="width: 200px"
            />
            <span class="hint">系统级兜底；员工 joinDate 优先。早于该日期的工作日不算缺勤，留空表示无截止线</span>
          </n-form-item>

          <n-divider title-placement="left">合规打卡位置</n-divider>

          <n-alert type="info" :bordered="false" style="margin-bottom: 16px">
            <template #header>
              <n-space align="center" justify="space-between" style="width: 100%">
                <span>合规位置 / 百度地图 AK 已迁至「系统配置 → 考勤设置」统一管理</span>
                <n-button size="small" type="primary" @click="goSystemConfig">前往设置</n-button>
              </n-space>
            </template>
            当前位置校验状态：
            <n-tag :type="form.enforceLocation ? 'success' : 'default'" size="small">
              {{ form.enforceLocation ? '已启用' : '未启用' }}
            </n-tag>
            ，已配置 {{ form.allowedLocations.length }} 个合规位置。
          </n-alert>

          <n-form-item label="">
            <n-space>
              <n-button type="primary" :loading="saving" @click="handleSave">保存设置</n-button>
              <n-button @click="loadConfig">重置</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-spin>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { configGroupApi } from '@/api/org'

interface AllowedLocation {
  name: string
  address?: string
  lat: number | null
  lng: number | null
  radius: number
}

interface AttendanceConfig {
  workStartTime: string
  workEndTime: string
  enforceTime: boolean
  enforceLocation: boolean
  allowedLocations: AllowedLocation[]
  baiduMapAk: string
  startDate: string | null
}

const message = useMessage()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)

function goSystemConfig() {
  router.push({ path: '/system/config', query: { tab: 'attendance' } })
}

const form = reactive<AttendanceConfig>({
  workStartTime: '09:00',
  workEndTime: '18:00',
  enforceTime: true,
  enforceLocation: false,
  allowedLocations: [],
  baiduMapAk: '',
  startDate: null
})

async function loadConfig() {
  loading.value = true
  try {
    const group = await configGroupApi.getByCode('attendance')
    if (group?.configValue) {
      const parsed = JSON.parse(group.configValue)
      form.workStartTime = parsed.workStartTime || '09:00'
      form.workEndTime = parsed.workEndTime || '18:00'
      form.enforceTime = parsed.enforceTime !== false
      form.enforceLocation = !!parsed.enforceLocation
      form.allowedLocations = Array.isArray(parsed.allowedLocations)
        ? parsed.allowedLocations.map((l: any) => ({
            name: l.name || '',
            address: l.address || '',
            lat: typeof l.lat === 'number' ? l.lat : null,
            lng: typeof l.lng === 'number' ? l.lng : null,
            radius: typeof l.radius === 'number' ? l.radius : 500
          }))
        : []
      form.baiduMapAk = parsed.baiduMapAk || ''
      form.startDate = typeof parsed.startDate === 'string' && parsed.startDate ? parsed.startDate : null
    }
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    // 全量保存（包含位置字段），防止覆盖「系统配置 → 考勤设置」tab 写入的位置数据
    await configGroupApi.save('attendance', { ...form })
    message.success('保存成功')
  } finally {
    saving.value = false
  }
}

onMounted(loadConfig)
</script>

<style scoped>
.attendance-config-page {
  padding: 16px;
}
.hint {
  margin-left: 12px;
  color: #888;
  font-size: 12px;
}
</style>
