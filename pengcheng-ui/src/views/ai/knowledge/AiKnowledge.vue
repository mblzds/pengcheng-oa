<template>
  <div class="page-container">
    <n-space vertical :size="16">
      <n-card title="文档上传">
        <n-space>
          <input ref="fileInputRef" type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.md,.csv" @change="onFileChange" />
          <n-input-number v-model:value="projectId" :min="1" clearable placeholder="项目ID(可选)" style="width: 160px" />
          <n-button type="primary" :loading="uploading" @click="uploadFile">上传文档</n-button>
        </n-space>
      </n-card>

      <n-card title="已上传文档列表">
        <n-data-table :columns="columns" :data="docs" :loading="loading" :pagination="false" />
      </n-card>
    </n-space>
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { NButton, NSpace, NTag, useMessage, type DataTableColumns } from 'naive-ui'
import { realtyApi, type AiKnowledgeDocRecord } from '@/api/realty'

const message = useMessage()

const loading = ref(false)
const uploading = ref(false)
const docs = ref<AiKnowledgeDocRecord[]>([])
const selectedFile = ref<File | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const projectId = ref<number | null>(null)

const columns: DataTableColumns<AiKnowledgeDocRecord> = [
  { title: '文档ID', key: 'id', width: 100 },
  { title: '文件名', key: 'fileName' },
  { title: '项目ID', key: 'projectId', width: 100 },
  {
    title: '处理状态',
    key: 'status',
    width: 120,
    render: row => h(NTag, { type: row.status === 'DONE' ? 'success' : 'warning', size: 'small' }, { default: () => (row.status === 'DONE' ? '已处理' : '处理中') })
  },
  {
    title: '上传时间',
    key: 'uploadTime',
    width: 180,
    render: row => formatDateTime(row.uploadTime)
  },
  {
    title: '操作',
    key: 'actions',
    width: 110,
    render: row =>
      h(NSpace, null, {
        default: () => [
          h(
            NButton,
            {
              size: 'small',
              tertiary: true,
              type: 'error',
              onClick: () => deleteDoc(row)
            },
            { default: () => '删除' }
          )
        ]
      })
  }
]

function formatDateTime(value?: string) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  const second = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}:${second}`
}

function onFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const files = input.files
  selectedFile.value = files && files.length > 0 ? files[0] : null
}

async function loadDocs() {
  loading.value = true
  try {
    docs.value = await realtyApi.aiKnowledgeDocs()
  } finally {
    loading.value = false
  }
}

async function uploadFile() {
  if (!selectedFile.value) {
    message.warning('请先选择文件')
    return
  }

  uploading.value = true
  try {
    await realtyApi.aiKnowledgeUpload(selectedFile.value, projectId.value || undefined)
    message.success('上传成功')
    selectedFile.value = null
    if (fileInputRef.value) {
      fileInputRef.value.value = ''
    }
    await loadDocs()
  } finally {
    uploading.value = false
  }
}

async function deleteDoc(row: AiKnowledgeDocRecord) {
  if (!row.id) return
  await realtyApi.aiKnowledgeDeleteDoc(row.id)
  message.success('删除成功')
  await loadDocs()
}

onMounted(() => {
  loadDocs()
})
</script>
