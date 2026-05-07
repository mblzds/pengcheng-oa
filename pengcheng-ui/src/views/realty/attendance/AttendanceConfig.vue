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

          <n-divider title-placement="left">合规打卡位置</n-divider>

          <n-form-item label="启用位置校验">
            <n-switch v-model:value="form.enforceLocation" />
            <span class="hint">关闭后任何位置打卡均视为合规</span>
          </n-form-item>

          <n-form-item label="百度地图 AK">
            <n-input
              v-model:value="form.baiduMapAk"
              placeholder="https://lbsyun.baidu.com/ 申请 JS API AK"
              clearable
              style="max-width: 480px"
            />
          </n-form-item>

          <n-form-item label="允许位置列表">
            <div style="width: 100%">
              <n-button type="primary" size="small" @click="addLocation" :disabled="!form.enforceLocation">
                <template #icon>
                  <n-icon><AddOutline /></n-icon>
                </template>
                新增位置
              </n-button>

              <n-empty
                v-if="!form.allowedLocations.length"
                description="暂无配置，添加至少一个位置后位置校验才会真正生效"
                style="margin-top: 16px"
              />

              <n-list v-else bordered style="margin-top: 12px">
                <n-list-item v-for="(loc, idx) in form.allowedLocations" :key="idx">
                  <n-grid :cols="24" :x-gap="12" :y-gap="8">
                    <n-gi :span="5">
                      <n-input v-model:value="loc.name" placeholder="名称（如：总部）" size="small" />
                    </n-gi>
                    <n-gi :span="8">
                      <n-input v-model:value="loc.address" placeholder="地址" size="small" readonly />
                    </n-gi>
                    <n-gi :span="3">
                      <n-input-number v-model:value="loc.lat" placeholder="纬度" :precision="6" :show-button="false" size="small" readonly />
                    </n-gi>
                    <n-gi :span="3">
                      <n-input-number v-model:value="loc.lng" placeholder="经度" :precision="6" :show-button="false" size="small" readonly />
                    </n-gi>
                    <n-gi :span="2">
                      <n-input-number v-model:value="loc.radius" placeholder="半径(米)" :min="10" :max="10000" :show-button="false" size="small" />
                    </n-gi>
                    <n-gi :span="2">
                      <n-button size="small" type="primary" ghost @click="openMapPicker(idx)">选点</n-button>
                    </n-gi>
                    <n-gi :span="1">
                      <n-button quaternary type="error" size="small" @click="removeLocation(idx)">
                        <template #icon><n-icon><TrashOutline /></n-icon></template>
                      </n-button>
                    </n-gi>
                  </n-grid>
                </n-list-item>
              </n-list>

              <n-alert v-if="form.enforceLocation && !form.allowedLocations.length" type="warning" style="margin-top: 12px">
                已开启位置校验但未配置位置 — 当前所有打卡都会被标记为「位置异常」。
              </n-alert>
            </div>
          </n-form-item>

          <n-form-item label="">
            <n-space>
              <n-button type="primary" :loading="saving" @click="handleSave">保存设置</n-button>
              <n-button @click="loadConfig">重置</n-button>
            </n-space>
          </n-form-item>
        </n-form>
      </n-spin>
    </n-card>

    <n-modal
      v-model:show="mapPickerVisible"
      preset="card"
      :title="`地图选点 — ${editingLocation?.name || '未命名位置'}`"
      style="width: 760px; max-width: 90vw"
      :mask-closable="false"
    >
      <BaiduMapPicker
        v-if="mapPickerVisible"
        :ak="form.baiduMapAk"
        :model-value="pickedSnapshot"
        :radius="editingLocation?.radius"
        @update:model-value="onPicked"
      />
      <template #footer>
        <n-space justify="end">
          <n-button @click="mapPickerVisible = false">取消</n-button>
          <n-button type="primary" @click="confirmMapPick" :disabled="!pickedSnapshot">确定使用此位置</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useMessage } from 'naive-ui'
import { AddOutline, TrashOutline } from '@vicons/ionicons5'
import { configGroupApi } from '@/api/org'
import BaiduMapPicker from '@/components/BaiduMapPicker.vue'

interface AllowedLocation {
  name: string
  address?: string
  lat: number | null
  lng: number | null
  radius: number
}

interface PickedPoint {
  lng: number
  lat: number
  address?: string
}

interface AttendanceConfig {
  workStartTime: string
  workEndTime: string
  enforceTime: boolean
  enforceLocation: boolean
  allowedLocations: AllowedLocation[]
  baiduMapAk: string
}

const message = useMessage()
const loading = ref(false)
const saving = ref(false)

const form = reactive<AttendanceConfig>({
  workStartTime: '09:00',
  workEndTime: '18:00',
  enforceTime: true,
  enforceLocation: false,
  allowedLocations: [],
  baiduMapAk: ''
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
    }
  } finally {
    loading.value = false
  }
}

function addLocation() {
  form.allowedLocations.push({ name: '', address: '', lat: null, lng: null, radius: 500 })
}

function removeLocation(idx: number) {
  form.allowedLocations.splice(idx, 1)
}

const mapPickerVisible = ref(false)
const editingIdx = ref<number | null>(null)
const pickedSnapshot = ref<PickedPoint | null>(null)
const editingLocation = computed<AllowedLocation | null>(() =>
  editingIdx.value != null ? form.allowedLocations[editingIdx.value] : null
)

function openMapPicker(idx: number) {
  if (!form.baiduMapAk?.trim()) {
    message.warning('请先在上方填入百度地图 AK 后再选点')
    return
  }
  editingIdx.value = idx
  const loc = form.allowedLocations[idx]
  pickedSnapshot.value = (loc.lat != null && loc.lng != null)
    ? { lat: loc.lat, lng: loc.lng, address: loc.address }
    : null
  mapPickerVisible.value = true
}

function onPicked(point: PickedPoint) {
  pickedSnapshot.value = point
}

function confirmMapPick() {
  if (!pickedSnapshot.value || editingIdx.value == null) return
  const loc = form.allowedLocations[editingIdx.value]
  loc.lat = pickedSnapshot.value.lat
  loc.lng = pickedSnapshot.value.lng
  if (pickedSnapshot.value.address) loc.address = pickedSnapshot.value.address
  mapPickerVisible.value = false
}

async function handleSave() {
  if (form.enforceLocation) {
    for (const [i, loc] of form.allowedLocations.entries()) {
      if (!loc.name?.trim()) return message.error(`第 ${i + 1} 个位置的名称必填`)
      if (loc.lat == null || loc.lng == null) return message.error(`第 ${i + 1} 个位置的经纬度必填`)
      if (!loc.radius || loc.radius < 10) return message.error(`第 ${i + 1} 个位置的半径必须 ≥ 10 米`)
    }
  }
  saving.value = true
  try {
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
