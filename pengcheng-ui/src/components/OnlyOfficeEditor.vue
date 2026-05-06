<template>
  <div class="onlyoffice-editor">
    <div v-if="loading" class="editor-loading">
      <n-spin size="large" />
      <p>正在加载文档编辑器...</p>
    </div>
    <div v-else-if="error" class="editor-error">
      <n-result status="error" :title="error" description="OnlyOffice Document Server 连接失败">
        <template #footer>
          <n-button @click="initEditor">重试</n-button>
        </template>
      </n-result>
    </div>
    <div :id="editorId" ref="editorRef" class="editor-container" />
  </div>
</template>

<script setup lang="ts">
/**
 * OnlyOffice 文档编辑器组件
 * 支持 docx/xlsx/pptx 在线编辑
 */
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { request } from '@/utils/request'

const props = defineProps<{
  fileId: number
  serverUrl?: string
}>()

const emit = defineEmits<{
  (e: 'saved'): void
  (e: 'error', msg: string): void
}>()

const editorId = `onlyoffice-editor-${Date.now()}`
const editorRef = ref<HTMLElement>()
const loading = ref(true)
const error = ref('')
let docEditor: any = null

async function initEditor() {
  loading.value = true
  error.value = ''

  try {
    const res: any = await request({
      url: `/wopi/files/editor-url/${props.fileId}`,
      method: 'get'
    })
    const config = res?.data ?? res
    const serverUrl = config?.documentServerUrl || props.serverUrl || ''
    if (!serverUrl) {
      error.value = '未配置 OnlyOffice 文档服务。请在「系统配置」或配置文件中设置 onlyoffice.server-url 后使用在线编辑。'
      return
    }

    await loadOnlyOfficeScript(serverUrl)

    const editorConfig = {
      document: config.document,
      documentType: config.documentType || 'word',
      editorConfig: {
        ...config.editorConfig,
        customization: {
          autosave: true,
          chat: false,
          comments: true,
          compactHeader: true,
          compactToolbar: false,
          forcesave: true,
          hideRightMenu: false,
          logo: { visible: false },
        },
      },
      events: {
        onDocumentStateChange: (event: any) => {
          if (event.data) {
            // 文档有未保存的更改
          }
        },
        onSave: () => {
          emit('saved')
        },
        onError: (event: any) => {
          error.value = event.data?.message || '编辑器错误'
          emit('error', error.value)
        },
      },
      height: '100%',
      width: '100%',
    }

    if ((window as any).DocsAPI) {
      docEditor = new (window as any).DocsAPI.DocEditor(editorId, editorConfig)
    } else {
      error.value = 'OnlyOffice SDK 加载失败'
    }
  } catch (e: any) {
    error.value = e.message || '加载编辑器配置失败'
  } finally {
    loading.value = false
  }
}

function loadOnlyOfficeScript(serverUrl: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if ((window as any).DocsAPI) {
      resolve()
      return
    }
    const script = document.createElement('script')
    script.src = `${serverUrl}/web-apps/apps/api/documents/api.js`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('无法加载 OnlyOffice SDK，请检查 Document Server 是否运行'))
    document.head.appendChild(script)
  })
}

watch(() => props.fileId, () => {
  if (docEditor) {
    docEditor.destroyEditor()
    docEditor = null
  }
  initEditor()
})

onMounted(() => initEditor())

onUnmounted(() => {
  if (docEditor) {
    docEditor.destroyEditor()
    docEditor = null
  }
})
</script>

<style scoped>
.onlyoffice-editor {
  width: 100%;
  height: 100%;
  min-height: 600px;
}
.editor-container {
  width: 100%;
  height: 100%;
  min-height: 600px;
}
.editor-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
  gap: 16px;
  color: #999;
}
.editor-error {
  padding: 40px;
}
</style>
