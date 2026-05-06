<template>
  <div class="doc-container">
    <!-- 左侧空间与目录树 -->
    <div class="doc-sidebar">
      <div class="sidebar-header">
        <h3>文档空间</h3>
        <n-button size="small" type="primary" @click="showCreateSpace = true">
          <template #icon><n-icon :component="AddOutline" /></template>
        </n-button>
      </div>

      <div class="space-list">
        <div v-for="space in spaces" :key="space.id"
             :class="['space-item', { active: currentSpaceId === space.id }]"
             @click="selectSpace(space)">
          <n-icon :component="FolderOpenOutline" />
          <span>{{ space.name }}</span>
          <n-tag v-if="space.visibility === 'all'" size="tiny" type="info">公开</n-tag>
        </div>
        <n-empty v-if="spaces.length === 0" description="暂无文档空间" size="small" style="margin-top: 20px" />
      </div>

      <n-divider />

      <div v-if="currentSpaceId" class="doc-tree-header">
        <span>文档列表</span>
        <n-button size="tiny" quaternary @click="createFolder">
          <template #icon><n-icon :component="FolderOutline" /></template>
        </n-button>
        <n-button size="tiny" quaternary @click="createNewDoc">
          <template #icon><n-icon :component="AddOutline" /></template>
        </n-button>
      </div>

      <div v-if="currentSpaceId" class="doc-tree">
        <div v-for="doc in docTree" :key="doc.id"
             :class="['doc-item', { active: currentDocId === doc.id, folder: doc.docType === 'folder' }]"
             :style="{ paddingLeft: (doc.parentId > 0 ? 32 : 12) + 'px' }"
             @click="selectDoc(doc)">
          <n-icon :component="doc.docType === 'folder' ? FolderOutline : DocumentTextOutline" />
          <span class="doc-title">{{ doc.title }}</span>
          <n-button size="tiny" quaternary class="doc-action" @click.stop="deleteDoc(doc.id)">
            <template #icon><n-icon :component="TrashOutline" size="12" /></template>
          </n-button>
        </div>
      </div>

      <div v-if="currentSpaceId" class="search-box">
        <n-input v-model:value="searchKeyword" placeholder="搜索文档..." size="small" clearable
                 @keydown.enter="searchDocs">
          <template #prefix><n-icon :component="SearchOutline" /></template>
        </n-input>
      </div>
    </div>

    <!-- 右侧文档编辑区 -->
    <div class="doc-main">
      <template v-if="currentDoc">
        <div class="doc-toolbar">
          <n-input v-model:value="currentDoc.title" placeholder="文档标题" class="title-input" />
          <div class="toolbar-actions">
            <n-button size="small" @click="exportPdf">
              导出 PDF
            </n-button>
            <n-button size="small" @click="openVersionHistory">
              <template #icon><n-icon :component="TimeOutline" /></template>
              版本历史
            </n-button>
            <n-button size="small" type="primary" @click="saveDoc">
              <template #icon><n-icon :component="SaveOutline" /></template>
              保存
            </n-button>
          </div>
        </div>

        <div class="doc-meta">
          <span>字数：{{ currentDoc.wordCount || 0 }}</span>
          <span>版本：v{{ currentDoc.version || 1 }}</span>
          <span v-if="currentDoc.updatedAt">更新：{{ formatTime(currentDoc.updatedAt) }}</span>
          <span v-if="onlineUsers.length > 0" class="online-users">
            <n-tag size="tiny" type="success" round>{{ onlineUsers.length }} 人在线</n-tag>
            <n-avatar-group :options="onlineUsers.map(u => ({ name: u.userName || '用户' + u.userId }))" :size="20" :max="5" />
          </span>
        </div>

        <div class="doc-editor">
          <n-input v-model:value="currentDoc.content" type="textarea" placeholder="开始编写 Markdown 内容..."
                   :autosize="{ minRows: 20 }" class="editor-area" />
        </div>

        <div class="doc-preview">
          <div class="preview-label">预览</div>
          <div class="preview-content" v-html="renderedContent"></div>
        </div>
      </template>

      <div v-else class="doc-empty">
        <n-empty description="选择一个文档开始编辑，或创建新文档">
          <template #extra>
            <n-button v-if="currentSpaceId" type="primary" @click="createNewDoc">新建文档</n-button>
          </template>
        </n-empty>
      </div>
    </div>

    <!-- 创建空间弹窗 -->
    <n-modal v-model:show="showCreateSpace" preset="dialog" title="创建文档空间">
      <n-form>
        <n-form-item label="空间名称">
          <n-input v-model:value="newSpace.name" placeholder="输入空间名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="newSpace.description" type="textarea" placeholder="空间描述（可选）" />
        </n-form-item>
        <n-form-item label="可见性">
          <n-radio-group v-model:value="newSpace.visibility">
            <n-radio value="private">仅自己</n-radio>
            <n-radio value="dept">本部门</n-radio>
            <n-radio value="all">所有人</n-radio>
          </n-radio-group>
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showCreateSpace = false">取消</n-button>
        <n-button type="primary" @click="handleCreateSpace">创建</n-button>
      </template>
    </n-modal>

    <!-- 版本历史弹窗 -->
    <n-modal v-model:show="showVersions" preset="card" title="版本历史" style="width: 600px">
      <n-data-table :columns="versionColumns" :data="versions" :max-height="400" />
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, h } from 'vue'
import {
  NButton, NIcon, NInput, NTag, NDivider, NEmpty, NModal,
  NForm, NFormItem, NRadioGroup, NRadio, NDataTable, useMessage
} from 'naive-ui'
import {
  AddOutline, FolderOpenOutline, FolderOutline, DocumentTextOutline,
  TrashOutline, SearchOutline, TimeOutline, SaveOutline
} from '@vicons/ionicons5'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const message = useMessage()
const userStore = useUserStore()

const spaces = ref<any[]>([])
const currentSpaceId = ref<number | null>(null)
const docTree = ref<any[]>([])
const currentDocId = ref<number | null>(null)
const currentDoc = ref<any>(null)
const searchKeyword = ref('')
const showCreateSpace = ref(false)
const showVersions = ref(false)
const versions = ref<any[]>([])
const onlineUsers = ref<any[]>([])
let docWs: WebSocket | null = null

const newSpace = ref({ name: '', description: '', visibility: 'private' })

const renderedContent = computed(() => {
  if (!currentDoc.value?.content) return ''
  return currentDoc.value.content
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`(.+?)`/g, '<code>$1</code>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br>')
})

const formatTime = (t: string) => t ? new Date(t).toLocaleString('zh-CN') : ''

const versionColumns = [
  { title: '版本', key: 'version', width: 80 },
  { title: '标题', key: 'title' },
  { title: '时间', key: 'createdAt', render: (row: any) => h('span', formatTime(row.createdAt)) },
  {
    title: '操作', key: 'actions', width: 100,
    render: (row: any) => h(NButton, { size: 'tiny', onClick: () => restoreVersion(row.version) }, () => '恢复')
  }
]

onMounted(() => loadSpaces())
onUnmounted(() => disconnectDocWs())

watch(currentDocId, (newId, oldId) => {
  if (oldId) disconnectDocWs()
  if (newId) connectDocWs(newId)
})

function connectDocWs(docId: number) {
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${location.host}/ws/doc`
  docWs = new WebSocket(wsUrl)
  docWs.onopen = () => {
    docWs?.send(JSON.stringify({ type: 'join', docId, userId: userStore.user?.id || 0, userName: userStore.nickname || '未知用户' }))
  }
  docWs.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      switch (data.type) {
        case 'joined':
          onlineUsers.value = data.onlineUsers || []
          break
        case 'user_joined':
        case 'user_left':
          onlineUsers.value = data.onlineUsers || []
          break
        case 'edit':
          if (currentDoc.value && data.content !== undefined) {
            currentDoc.value.content = data.content
          }
          break
        case 'saved':
          message.info('文档已被其他用户保存')
          break
      }
    } catch {}
  }
  docWs.onclose = () => { docWs = null }
}

function disconnectDocWs() {
  if (docWs && docWs.readyState === WebSocket.OPEN) {
    docWs.send(JSON.stringify({ type: 'leave' }))
    docWs.close()
  }
  docWs = null
  onlineUsers.value = []
}

async function loadSpaces() {
  try {
    const res = await request.get('/doc/spaces')
    spaces.value = Array.isArray(res) ? res : []
  } catch { spaces.value = [] }
}

async function selectSpace(space: any) {
  currentSpaceId.value = space.id
  currentDoc.value = null
  currentDocId.value = null
  try {
    const res = await request.get(`/doc/tree/${space.id}`)
    docTree.value = Array.isArray(res) ? res : []
  } catch { docTree.value = [] }
}

async function selectDoc(doc: any) {
  if (doc.docType === 'folder') return
  currentDocId.value = doc.id
  try {
    const res = await request.get(`/doc/${doc.id}`)
    currentDoc.value = res
  } catch {
    message.error('加载文档失败')
  }
}

async function handleCreateSpace() {
  try {
    await request.post('/doc/space', newSpace.value)
    message.success('创建成功')
    showCreateSpace.value = false
    newSpace.value = { name: '', description: '', visibility: 'private' }
    loadSpaces()
  } catch {
    message.error('创建失败')
  }
}

async function createNewDoc() {
  if (!currentSpaceId.value) return
  const doc = { spaceId: currentSpaceId.value, parentId: 0, title: '未命名文档', content: '', docType: 'markdown' }
  try {
    const res = await request.post('/doc/create', doc)
    message.success('已创建')
    await selectSpace({ id: currentSpaceId.value })
    if (res) selectDoc(res)
  } catch {
    message.error('创建失败')
  }
}

async function createFolder() {
  if (!currentSpaceId.value) return
  const doc = { spaceId: currentSpaceId.value, parentId: 0, title: '新建文件夹', docType: 'folder' }
  try {
    await request.post('/doc/create', doc)
    await selectSpace({ id: currentSpaceId.value })
  } catch {
    message.error('创建失败')
  }
}

function exportPdf() {
  if (!currentDoc.value?.id) {
    message.warning('请先选择文档')
    return
  }
  window.open(`/api/doc/export/${currentDoc.value.id}`, '_blank')
}

async function saveDoc() {
  if (!currentDoc.value) return
  try {
    await request.put('/doc/update', currentDoc.value)
    message.success('已保存')
    await selectSpace({ id: currentSpaceId.value })
  } catch {
    message.error('保存失败')
  }
}

async function deleteDoc(id: number) {
  try {
    await request.delete(`/doc/${id}`)
    message.success('已删除')
    if (currentDocId.value === id) {
      currentDoc.value = null
      currentDocId.value = null
    }
    await selectSpace({ id: currentSpaceId.value })
  } catch {
    message.error('删除失败')
  }
}

async function searchDocs() {
  if (!searchKeyword.value || !currentSpaceId.value) return
  try {
    const res = await request.get('/doc/search', { params: { spaceId: currentSpaceId.value, keyword: searchKeyword.value } })
    docTree.value = Array.isArray(res) ? res : []
  } catch {
    message.error('搜索失败')
  }
}

async function openVersionHistory() {
  if (!currentDocId.value) return
  showVersions.value = true
  try {
    const res = await request.get(`/doc/versions/${currentDocId.value}`)
    versions.value = Array.isArray(res) ? res : []
  } catch {
    versions.value = []
  }
}

async function restoreVersion(ver: number) {
  if (!currentDocId.value) return
  try {
    await request.post('/doc/versions/restore', null, { params: { docId: currentDocId.value, version: ver } })
    message.success('已恢复到 v' + ver)
    showVersions.value = false
    selectDoc({ id: currentDocId.value })
  } catch {
    message.error('恢复失败')
  }
}
</script>

<style scoped>
.doc-container {
  display: flex;
  height: calc(100vh - 100px);
  gap: 0;
}
.doc-sidebar {
  width: 280px;
  border-right: 1px solid var(--n-border-color, #e0e0e6);
  background: var(--n-card-color, #fff);
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}
.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid var(--n-border-color, #e0e0e6);
}
.sidebar-header h3 { margin: 0; font-size: 15px; }
.space-list { padding: 8px; }
.space-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}
.space-item:hover { background: rgba(24, 160, 88, 0.08); }
.space-item.active { background: rgba(24, 160, 88, 0.15); color: #18a058; }
.doc-tree-header {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  font-size: 13px;
  color: #999;
}
.doc-tree-header span { flex: 1; }
.doc-tree { flex: 1; overflow-y: auto; padding: 0 8px; }
.doc-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  position: relative;
}
.doc-item:hover { background: rgba(0,0,0,0.04); }
.doc-item.active { background: rgba(24, 160, 88, 0.12); }
.doc-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-action { opacity: 0; }
.doc-item:hover .doc-action { opacity: 1; }
.search-box { padding: 8px 12px; }
.doc-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--n-card-color, #fff);
}
.doc-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-bottom: 1px solid var(--n-border-color, #e0e0e6);
}
.title-input { flex: 1; }
.title-input :deep(input) { font-size: 18px; font-weight: 600; }
.toolbar-actions { display: flex; gap: 8px; }
.doc-meta {
  display: flex;
  gap: 16px;
  padding: 8px 20px;
  font-size: 12px;
  color: #999;
}
.doc-editor { flex: 1; padding: 0 20px; overflow-y: auto; }
.editor-area :deep(textarea) {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 14px;
  line-height: 1.6;
}
.doc-preview {
  max-height: 300px;
  overflow-y: auto;
  padding: 16px 20px;
  border-top: 1px solid var(--n-border-color, #e0e0e6);
}
.preview-label { font-size: 12px; color: #999; margin-bottom: 8px; }
.preview-content { font-size: 14px; line-height: 1.8; }
.preview-content :deep(h1) { font-size: 24px; margin: 12px 0; }
.preview-content :deep(h2) { font-size: 20px; margin: 10px 0; }
.preview-content :deep(h3) { font-size: 16px; margin: 8px 0; }
.preview-content :deep(code) { background: #f0f0f0; padding: 2px 6px; border-radius: 3px; font-size: 13px; }
.preview-content :deep(li) { margin-left: 20px; }
.doc-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
