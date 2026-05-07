<template>
  <div class="baidu-map-picker">
    <div class="search-row">
      <n-input
        v-model:value="searchKeyword"
        placeholder="搜索地点名称、地址（如：广州塔）"
        clearable
        @keyup.enter="handleSearch"
        :disabled="!ready"
      >
        <template #suffix>
          <n-button quaternary size="tiny" @click="handleSearch" :disabled="!ready">搜索</n-button>
        </template>
      </n-input>
    </div>

    <div ref="mapEl" class="map-area"></div>

    <div class="picked-info">
      <template v-if="picked">
        <n-tag type="success" size="small">已选</n-tag>
        <span class="address">{{ picked.address || '位置未识别' }}</span>
        <span class="coord">WGS-84: {{ picked.lng.toFixed(6) }}, {{ picked.lat.toFixed(6) }}</span>
      </template>
      <span v-else style="color: #999">在地图上点击或搜索后选择位置</span>
    </div>

    <n-alert v-if="loadError" type="error" :show-icon="false" style="margin-top: 8px">{{ loadError }}</n-alert>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { loadBaiduMap } from '@/utils/baiduMap'
import { bd09ToWgs84, wgs84ToBd09 } from '@/utils/coordTransform'

interface PickedPoint {
  lng: number
  lat: number
  address?: string
}

const props = defineProps<{
  ak: string
  modelValue?: PickedPoint | null
  /** 半径（米），用于在地图上画一个圆作为预览 */
  radius?: number | null
}>()

const emit = defineEmits<{
  'update:modelValue': [PickedPoint]
}>()

const mapEl = ref<HTMLDivElement>()
const searchKeyword = ref('')
const picked = ref<PickedPoint | null>(props.modelValue ?? null)
const loadError = ref('')
const ready = ref(false)

let BMap: any = null
let map: any = null
let marker: any = null
let circle: any = null
let geocoder: any = null
let localSearch: any = null

onMounted(async () => {
  if (!props.ak) {
    loadError.value = '尚未配置百度地图 AK，无法选点。请在配置页顶部填入后再试。'
    return
  }
  try {
    BMap = await loadBaiduMap(props.ak)
    initMap()
    ready.value = true
  } catch (e: any) {
    loadError.value = e?.message || '百度地图加载失败'
  }
})

function initMap() {
  if (!mapEl.value || !BMap) return
  map = new BMap.Map(mapEl.value)

  let bdLng = 113.265, bdLat = 23.13  // 广州默认中心
  if (props.modelValue?.lng && props.modelValue?.lat) {
    const [bLng, bLat] = wgs84ToBd09(props.modelValue.lng, props.modelValue.lat)
    bdLng = bLng
    bdLat = bLat
  }
  map.centerAndZoom(new BMap.Point(bdLng, bdLat), 16)
  map.enableScrollWheelZoom(true)
  map.addControl(new BMap.NavigationControl())

  geocoder = new BMap.Geocoder()
  if (props.modelValue) drawMarker(bdLng, bdLat)

  map.addEventListener('click', (e: any) => pick(e.point.lng, e.point.lat))
}

function drawMarker(bdLng: number, bdLat: number) {
  if (marker) map.removeOverlay(marker)
  if (circle) map.removeOverlay(circle)
  const point = new BMap.Point(bdLng, bdLat)
  marker = new BMap.Marker(point)
  map.addOverlay(marker)
  if (props.radius && props.radius > 0) {
    circle = new BMap.Circle(point, props.radius, {
      strokeColor: '#1890ff',
      strokeWeight: 2,
      strokeOpacity: 0.6,
      fillColor: '#1890ff',
      fillOpacity: 0.1
    })
    map.addOverlay(circle)
  }
}

function pick(bdLng: number, bdLat: number) {
  drawMarker(bdLng, bdLat)
  geocoder.getLocation(new BMap.Point(bdLng, bdLat), (result: any) => {
    const address = result?.address || ''
    const [wgsLng, wgsLat] = bd09ToWgs84(bdLng, bdLat)
    picked.value = { lng: wgsLng, lat: wgsLat, address }
    emit('update:modelValue', picked.value)
  })
}

function handleSearch() {
  if (!ready.value || !searchKeyword.value.trim()) return
  if (!localSearch) {
    localSearch = new BMap.LocalSearch(map, {
      onSearchComplete: (results: any) => {
        if (localSearch.getStatus() !== 0) return
        const first = results.getPoi(0)
        if (first) {
          map.centerAndZoom(first.point, 17)
          pick(first.point.lng, first.point.lat)
        }
      }
    })
  }
  localSearch.search(searchKeyword.value)
}

// 半径变化时重画圆圈
watch(() => props.radius, () => {
  if (picked.value) {
    const [bLng, bLat] = wgs84ToBd09(picked.value.lng, picked.value.lat)
    drawMarker(bLng, bLat)
  }
})
</script>

<style scoped>
.baidu-map-picker {
  width: 100%;
}
.search-row {
  margin-bottom: 8px;
}
.map-area {
  width: 100%;
  height: 420px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}
.picked-info {
  margin-top: 8px;
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  font-size: 13px;
}
.address {
  color: #333;
}
.coord {
  color: #888;
  font-size: 12px;
  font-family: monospace;
}
</style>
