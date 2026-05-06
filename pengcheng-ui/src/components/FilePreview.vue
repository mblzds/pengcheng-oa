<template>
  <n-modal v-model:show="visible" preset="card" :title="title" style="width: 90vw; height: 90vh;">
    <div class="file-preview-container">
      <!-- 图片预览 -->
      <template v-if="isImage">
        <img :src="fileUrl" :alt="fileName" class="preview-image" />
      </template>

      <!-- kkFileView 预览（Office/PDF） -->
      <template v-else-if="previewUrl">
        <iframe :src="previewUrl" class="preview-iframe" />
      </template>

      <!-- 不支持的文件类型 -->
      <template v-else>
        <n-empty description="暂不支持该文件类型的预览">
          <template #extra>
            <n-button type="primary" @click="downloadFile">下载文件</n-button>
          </template>
        </n-empty>
      </template>
    </div>

    <template #header-extra>
      <n-space>
        <n-button size="small" @click="downloadFile">下载</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { NModal, NButton, NSpace, NEmpty } from 'naive-ui'
import { request } from '@/utils/request'

const props = withDefaults(defineProps<{
  filePath?: string
  fileName?: string
}>(), {
  filePath: '',
  fileName: ''
})

const visible = defineModel<boolean>('show', { default: false })

const previewUrl = ref('')
const fileUrl = ref('')

const title = computed(() => props.fileName || '文件预览')

const imageExts = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'bmp']
const previewableExts = ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'pdf', 'txt', 'csv']

const ext = computed(() => {
  const name = props.filePath || props.fileName || ''
  const parts = name.split('.')
  return parts.length > 1 ? parts.pop()!.toLowerCase() : ''
})

const isImage = computed(() => imageExts.includes(ext.value))
const isPreviewable = computed(() => previewableExts.includes(ext.value))

watch([() => props.filePath, visible], async ([path, show]) => {
  if (!show || !path) {
    previewUrl.value = ''
    return
  }

  fileUrl.value = path.startsWith('http') ? path : '/' + path

  if (isImage.value) return

  if (isPreviewable.value) {
    try {
      const res = await request({
        url: '/sys/file/preview-url',
        method: 'get',
        params: { filePath: path }
      })
      previewUrl.value = res?.data?.previewUrl || ''
    } catch {
      previewUrl.value = ''
    }
  }
}, { immediate: true })

function downloadFile() {
  const url = fileUrl.value || props.filePath
  if (url) {
    window.open(url, '_blank')
  }
}
</script>

<style scoped>
.file-preview-container {
  height: calc(90vh - 120px);
  display: flex;
  align-items: center;
  justify-content: center;
}
.preview-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
}
.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  border-radius: 4px;
}
</style>
