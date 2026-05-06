<template>
  <div class="page-container">
    <div class="file-layout">
      <!-- 左侧分组卡片 -->
      <n-card class="group-card" size="small">
        <template #header>
          <div class="card-header">文件分组</div>
        </template>

        <!-- 文件类型标签 -->
        <div class="type-tabs">
          <div
              v-for="tab in typeTabs"
              :key="tab.value"
              :class="['type-tab', { active: activeType === tab.value }]"
              @click="activeType = tab.value; loadFiles()"
          >
            {{ tab.label }}
          </div>
        </div>

        <div class="group-list-wrapper">
          <div class="group-list">
            <div
                :class="['group-item', { active: activeGroupId === -1 }]"
                @click="selectGroup(-1)"
            >
              <n-icon><FolderOutline/></n-icon>
              <span class="group-name">全部</span>
            </div>
            <div
                :class="['group-item', { active: activeGroupId === null }]"
                @click="selectGroup(null)"
            >
              <n-icon><FolderOutline/></n-icon>
              <span class="group-name">未分组</span>
              <span v-if="ungroupedCount > 0" class="group-count">{{ ungroupedCount }}</span>
            </div>
            <div
                v-for="group in groups"
                :key="group.id"
                :class="['group-item', { active: activeGroupId === group.id }]"
                @click="selectGroup(group.id!)"
                @contextmenu.prevent="showGroupMenu($event, group)"
            >
              <n-icon><FolderOutline/></n-icon>
              <span class="group-name">{{ group.name }}</span>
              <span v-if="group.fileCount && group.fileCount > 0" class="group-count">{{ group.fileCount }}</span>
              <n-dropdown
                  trigger="click"
                  :options="groupMenuOptions"
                  @select="(key: string) => handleGroupAction(key, group)"
              >
                <n-icon class="group-more" @click.stop><EllipsisHorizontalOutline/></n-icon>
              </n-dropdown>
            </div>
          </div>
        </div>

        <template #footer>
          <n-button block dashed size="small" @click="showGroupModal = true">
            <template #icon><n-icon><AddOutline/></n-icon></template>
            新增分组
          </n-button>
        </template>
      </n-card>

      <!-- 右侧主内容卡片 -->
      <n-card class="file-list-card" size="small">
        <template #header>
          <div class="toolbar">
            <div class="toolbar-left">
              <n-upload
                  v-if="hasPermission('sys:file:upload')"
                  :custom-request="handleUpload"
                  :show-file-list="false"
                  :multiple="true"
              >
                <n-button type="primary">
                  <template #icon><n-icon><CloudUploadOutline/></n-icon></template>
                  上传
                </n-button>
              </n-upload>
              <n-button :disabled="selectedIds.length === 0" @click="handleBatchDelete">
                删除
              </n-button>
              <n-button :disabled="selectedIds.length === 0" @click="showMoveModal = true">
                移动
              </n-button>
            </div>
            <div class="toolbar-right">
              <n-input
                  v-model:value="searchName"
                  placeholder="请输入文件名称"
                  clearable
                  style="width: 200px"
                  @keyup.enter="loadFiles"
              >
                <template #suffix>
                  <n-icon style="cursor: pointer" @click="loadFiles"><SearchOutline/></n-icon>
                </template>
              </n-input>
              <n-button-group>
                <n-button :type="viewMode === 'list' ? 'primary' : 'default'" @click="viewMode = 'list'">
                  <template #icon><n-icon><ListOutline/></n-icon></template>
                </n-button>
                <n-button :type="viewMode === 'grid' ? 'primary' : 'default'" @click="viewMode = 'grid'">
                  <template #icon><n-icon><GridOutline/></n-icon></template>
                </n-button>
              </n-button-group>
            </div>
          </div>
        </template>

        <div class="file-manager-body" @dragover.prevent="handleDragOver" @dragleave.prevent="handleDragLeave" @drop.prevent="handleDrop">
          <!-- 拖拽上传遮罩 -->
          <Transition name="fade">
            <div v-if="isDragging" class="drag-overlay">
              <div class="drag-content">
                <n-icon size="64" color="#fff"><CloudUploadOutline/></n-icon>
                <h3>松开鼠标上传文件</h3>
                <p>支持多文件同时上传</p>
              </div>
            </div>
          </Transition>

          <!-- 全选栏 -->
          <div class="select-all-bar">
            <n-checkbox
                :checked="isAllSelected"
                :indeterminate="isIndeterminate"
                @update:checked="handleSelectAll"
            >
              全选
            </n-checkbox>
          </div>

          <!-- 文件列表区 -->
          <div class="file-content-wrapper">
            <n-spin :show="loading" class="file-spin">
              <div v-if="files.length === 0" class="empty-state">
                <n-empty description="暂无数据"/>
              </div>

              <!-- 平铺视图 -->
              <div v-else-if="viewMode === 'grid'" class="file-grid">
                <div
                    v-for="file in files"
                    :key="file.id"
                    :class="['file-card', { selected: selectedIds.includes(file.id!) }]"
                    @click="toggleSelect(file)"
                >
                  <div class="file-checkbox" @click.stop>
                    <n-checkbox :checked="selectedIds.includes(file.id!)" @update:checked="toggleSelect(file)"/>
                  </div>
                  <div class="file-preview" @click.stop="handlePreview(file)">
                    <img v-if="isImage(file)" :src="file.url" alt=""/>
                    <video v-else-if="isVideo(file)" :src="file.url"/>
                    <div v-else class="file-icon">
                      <n-icon size="48" :color="getFileIconColor(file)">
                        <component :is="getFileIcon(file)"/>
                      </n-icon>
                    </div>
                  </div>
                  <div class="file-name" :title="file.originalName">{{ file.originalName }}</div>
                  <div class="file-actions">
                    <a @click.stop="handleRename(file)">重命名</a>
                    <span>|</span>
                    <a @click.stop="handleDownload(file)">下载</a>
                    <span v-if="isPreviewable(file)">|</span>
                    <a v-if="isPreviewable(file)" @click.stop="handlePreview(file)">查看</a>
                  </div>
                </div>
              </div>

              <!-- 列表视图 -->
              <div v-else class="file-list">
                <div
                    v-for="file in files"
                    :key="file.id"
                    :class="['file-row', { selected: selectedIds.includes(file.id!) }]"
                    @click="toggleSelect(file)"
                >
                  <div class="file-checkbox" @click.stop>
                    <n-checkbox :checked="selectedIds.includes(file.id!)" @update:checked="toggleSelect(file)"/>
                  </div>
                  <div class="file-preview-small" @click.stop="handlePreview(file)">
                    <img v-if="isImage(file)" :src="file.url" alt=""/>
                    <n-icon v-else size="32" :color="getFileIconColor(file)">
                      <component :is="getFileIcon(file)"/>
                    </n-icon>
                  </div>
                  <div class="file-info">
                    <div class="file-name">{{ file.originalName }}</div>
                    <div class="file-meta">
                      <span>{{ formatFileSize(file.fileSize) }}</span>
                      <span>{{ file.createTime }}</span>
                    </div>
                  </div>
                  <div class="file-actions" @click.stop>
                    <n-button size="small" quaternary @click="handlePreview(file)">预览</n-button>
                    <n-button size="small" quaternary @click="handleDownload(file)">下载</n-button>
                    <n-button size="small" quaternary @click="handleRename(file)">重命名</n-button>
                    <n-button size="small" quaternary type="error" @click="handleDelete(file)">删除</n-button>
                  </div>
                </div>
              </div>
            </n-spin>
          </div>
        </div>

        <template #footer>
          <div class="pagination" style="display: flex; justify-content: flex-end; margin-top: 12px; align-items: center; gap: 12px;">
            <n-pagination
                v-model:page="pagination.page"
                v-model:page-size="pagination.pageSize"
                :item-count="pagination.itemCount"
                :page-sizes="[10, 20, 50, 100]"
                show-size-picker
                show-quick-jumper
                @update:page="loadFiles"
                @update:page-size="handlePageSizeChange"
            >
              <template #prefix>
                共 {{ pagination.itemCount }} 条
              </template>
            </n-pagination>
          </div>
        </template>
      </n-card>
    </div>

    <!-- 各种弹窗 -->
    <!-- 新增/编辑分组弹窗 -->
    <n-modal v-model:show="showGroupModal" preset="dialog" :title="editingGroup ? '编辑分组' : '新增分组'">
      <n-form :model="groupForm">
        <n-form-item label="分组名称" required>
          <n-input v-model:value="groupForm.name" placeholder="请输入分组名称"/>
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="showGroupModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveGroup">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 移动到分组弹窗 -->
    <n-modal v-model:show="showMoveModal" preset="dialog" title="移动到分组">
      <n-form>
        <n-form-item label="目标分组">
          <n-select
              v-model:value="moveTargetGroupId"
              :options="moveGroupOptions"
              placeholder="请选择分组"
          />
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="showMoveModal = false">取消</n-button>
          <n-button type="primary" @click="handleMoveFiles">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 重命名弹窗 -->
    <n-modal v-model:show="showRenameModal" preset="dialog" title="重命名">
      <n-form>
        <n-form-item label="文件名">
          <n-input v-model:value="renameValue" placeholder="请输入新文件名"/>
        </n-form-item>
      </n-form>
      <template #action>
        <n-space>
          <n-button @click="showRenameModal = false">取消</n-button>
          <n-button type="primary" @click="handleSaveRename">确定</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 预览弹窗 -->
    <n-modal v-model:show="previewVisible" preset="card" title="文件预览" :style="previewModalStyle">
      <div class="preview-container">
        <!-- 图片预览 -->
        <img v-if="isImage(previewFile)" :src="previewUrl" alt="预览" class="preview-image"/>
        <!-- 视频预览 -->
        <video v-else-if="isVideo(previewFile)" :src="previewUrl" controls class="preview-video"/>
        <!-- 音频预览 -->
        <audio v-else-if="isAudio(previewFile)" :src="previewUrl" controls/>
        <!-- PDF预览 -->
        <iframe v-else-if="isPdf(previewFile)" :src="previewUrl" class="preview-pdf"/>
        <!-- 文本/代码预览 -->
        <div v-else-if="isText(previewFile)" class="preview-text-wrapper">
          <div class="preview-toolbar" v-if="!textLoading">
            <n-button v-if="!isEditing" size="small" type="primary" @click="startEdit">编辑</n-button>
            <template v-else>
              <n-button size="small" @click="cancelEdit">取消</n-button>
              <n-button size="small" type="primary" @click="handleSaveText">保存</n-button>
            </template>
          </div>
          <div class="preview-text" v-if="!isEditing">
            <n-spin :show="textLoading">
              <n-code :code="previewText" :language="getCodeLanguage(previewFile)" show-line-numbers/>
            </n-spin>
          </div>
          <div class="edit-text" v-else>
            <n-input
              v-model:value="editText"
              type="textarea"
              :autosize="{ minRows: 20, maxRows: 30 }"
              placeholder="请输入内容"
            />
          </div>
        </div>
        <!-- Office文档预览 / 在线编辑 -->
        <div v-else-if="isOffice(previewFile)" class="preview-office">
          <div class="preview-toolbar">
            <n-space>
              <n-button size="small" :type="officeMode === 'preview' ? 'primary' : 'default'" @click="officeMode = 'preview'">预览</n-button>
              <n-button size="small" :type="officeMode === 'edit' ? 'primary' : 'default'" @click="officeMode = 'edit'">在线编辑</n-button>
            </n-space>
          </div>
          <n-spin :show="officePreviewLoading && officeMode === 'preview'">
            <iframe v-if="officeMode === 'preview' && previewUrl" :src="previewUrl" class="preview-office-frame"/>
            <div v-else-if="officeMode === 'preview'" class="preview-other">
              <n-icon size="64"><DocumentOutline/></n-icon>
              <p>{{ previewFile?.originalName }}</p>
              <p class="preview-tip">{{ officePreviewError || '在线预览服务未启动，请先启动 kkFileView。' }}</p>
              <n-button type="primary" @click="handleDownload(previewFile!)">下载文件</n-button>
            </div>
          </n-spin>
          <div v-if="officeMode === 'edit'" class="office-editor-wrapper">
            <OnlyOfficeEditor
              :file-id="previewFile!.id!"
              @saved="handleOfficeSaved"
              @error="handleOfficeError"
            />
            <p v-if="officeEditorError" class="preview-tip">{{ officeEditorError }}</p>
          </div>
        </div>
        <!-- 其他文件 -->
        <div v-else class="preview-other">
          <n-icon size="64"><DocumentOutline/></n-icon>
          <p>{{ previewFile?.originalName }}</p>
          <p class="preview-tip">该文件类型暂不支持预览</p>
          <n-button type="primary" @click="handleDownload(previewFile!)">下载文件</n-button>
        </div>
      </div>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, computed, onMounted, watch, h} from 'vue'
import {useMessage, useDialog, type UploadCustomRequestOptions} from 'naive-ui'
import {
  CloudUploadOutline, SearchOutline, ListOutline, GridOutline, FolderOutline,
  AddOutline, EllipsisHorizontalOutline, DocumentOutline, DocumentTextOutline,
  ImageOutline, VideocamOutline, MusicalNotesOutline, CodeSlashOutline
} from '@vicons/ionicons5'
import {fileApi, fileGroupApi, type SysFile, type SysFileGroup} from '@/api/system'
import {useUserStore} from '@/stores/user'
import {fetchBlobUrl, downloadBlob} from '@/utils/request'
import OnlyOfficeEditor from '@/components/OnlyOfficeEditor.vue'

const message = useMessage()
const dialog = useDialog()
const userStore = useUserStore()
const hasPermission = (permission: string) => userStore.hasPermission(permission)

// 文件类型标签
const typeTabs = [
  {label: '图片', value: 'image'},
  {label: '视频', value: 'video'},
  {label: '文件', value: 'other'}
]
const activeType = ref('image')

// 分组相关
const groups = ref<SysFileGroup[]>([])
const ungroupedCount = ref(0)
const activeGroupId = ref<number | null>(-1) // -1 表示全部

// 视图模式
const viewMode = ref<'list' | 'grid'>('grid')

// 搜索
const searchName = ref('')

// 文件列表
const files = ref<SysFile[]>([])
const loading = ref(false)
const selectedIds = ref<number[]>([])
const pagination = reactive({
  page: 1,
  pageSize: 20,
  itemCount: 0
})
const gotoPage = ref<number | null>(1)

// 分组弹窗
const showGroupModal = ref(false)
const editingGroup = ref<SysFileGroup | null>(null)
const groupForm = reactive({name: ''})

// 移动弹窗
const showMoveModal = ref(false)
const moveTargetGroupId = ref<number | null>(null)

// 重命名弹窗
const showRenameModal = ref(false)
const renameValue = ref('')
const renamingFile = ref<SysFile | null>(null)

// 预览
const previewVisible = ref(false)
const previewFile = ref<SysFile | null>(null)
const previewUrl = ref('')
const previewText = ref('')
const textLoading = ref(false)
const isEditing = ref(false)
const editText = ref('')
const officeMode = ref<'preview' | 'edit'>('preview')
const officePreviewLoading = ref(false)
const officePreviewError = ref('')
const officeEditorError = ref('')
/** 预览用 Blob URL，关闭弹窗时需 revoke */
let previewBlobUrl = ''

// 预览弹窗样式（根据文件类型调整大小）
const previewModalStyle = computed(() => {
  if (!previewFile.value) return {width: '800px'}
  if (isPdf(previewFile.value) || isOffice(previewFile.value)) {
    return {width: '90vw', height: '90vh'}
  }
  if (isText(previewFile.value)) {
    return {width: '900px', maxHeight: '80vh'}
  }
  return {width: '800px'}
})

// 拖拽上传
const isDragging = ref(false)
let dragCounter = 0

// 分组菜单选项
const groupMenuOptions = [
  {label: '编辑', key: 'edit'},
  {label: '删除', key: 'delete'}
]

// 移动分组选项
const moveGroupOptions = computed(() => {
  return [
    {label: '未分组', value: null},
    ...groups.value.map(g => ({label: g.name, value: g.id}))
  ]
})

// 全选相关
const isAllSelected = computed(() => files.value.length > 0 && selectedIds.value.length === files.value.length)
const isIndeterminate = computed(() => selectedIds.value.length > 0 && selectedIds.value.length < files.value.length)

// 加载分组
async function loadGroups() {
  try {
    const res = await fileGroupApi.list()
    groups.value = res.groups
    ungroupedCount.value = res.ungroupedCount
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 加载文件
async function loadFiles() {
  loading.value = true
  selectedIds.value = []
  try {
    const res = await fileApi.pageByGroup({
      page: pagination.page,
      pageSize: pagination.pageSize,
      groupId: activeGroupId.value === -1 ? undefined : activeGroupId.value,
      fileCategory: activeType.value,
      originalName: searchName.value || undefined
    })
    files.value = res.list
    pagination.itemCount = Number(res.total)
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}

function handlePageSizeChange(pageSize: number) {
  pagination.pageSize = pageSize
  pagination.page = 1
  loadFiles()
}

// 选择分组
function selectGroup(groupId: number | null) {
  activeGroupId.value = groupId
  pagination.page = 1
  loadFiles()
}

// 选择/取消选择文件
function toggleSelect(file: SysFile) {
  const idx = selectedIds.value.indexOf(file.id!)
  if (idx === -1) {
    selectedIds.value.push(file.id!)
  } else {
    selectedIds.value.splice(idx, 1)
  }
}

// 全选/取消全选
function handleSelectAll(checked: boolean) {
  if (checked) {
    selectedIds.value = files.value.map(f => f.id!)
  } else {
    selectedIds.value = []
  }
}

// 分组操作
function handleGroupAction(key: string, group: SysFileGroup) {
  if (key === 'edit') {
    editingGroup.value = group
    groupForm.name = group.name
    showGroupModal.value = true
  } else if (key === 'delete') {
    dialog.warning({
      title: '提示',
      content: `确定要删除分组"${group.name}"吗？分组内的文件将移动到"未分组"。`,
      positiveText: '确定',
      negativeText: '取消',
      onPositiveClick: async () => {
        try {
          await fileGroupApi.delete(group.id!)
          message.success('删除成功')
          loadGroups()
          if (activeGroupId.value === group.id) {
            selectGroup(-1)
          }
        } catch (error) {
          // 错误已在拦截器处理
        }
      }
    })
  }
}

function showGroupMenu(e: MouseEvent, group: SysFileGroup) {
  // 右键菜单暂不实现，使用下拉菜单
}

// 保存分组
async function handleSaveGroup() {
  if (!groupForm.name.trim()) {
    message.warning('请输入分组名称')
    return
  }
  try {
    if (editingGroup.value) {
      await fileGroupApi.update({id: editingGroup.value.id, name: groupForm.name})
      message.success('更新成功')
    } else {
      await fileGroupApi.create({name: groupForm.name})
      message.success('创建成功')
    }
    showGroupModal.value = false
    editingGroup.value = null
    groupForm.name = ''
    loadGroups()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 获取当前上传目标分组ID
function getUploadGroupId(): number | null {
  if (activeGroupId.value === -1 || activeGroupId.value === null) {
    return null
  }
  return activeGroupId.value
}

// 上传
async function handleUpload(options: UploadCustomRequestOptions) {
  const {file, onFinish, onError} = options
  try {
    await fileApi.upload(file.file as File, undefined, getUploadGroupId())
    message.success('上传成功')
    onFinish()
    loadFiles()
    loadGroups()
  } catch (error) {
    onError()
  }
}

// 拖拽上传
function handleDragOver() {
  dragCounter++
  isDragging.value = true
}

function handleDragLeave() {
  dragCounter--
  if (dragCounter === 0) {
    isDragging.value = false
  }
}

async function handleDrop(e: DragEvent) {
  isDragging.value = false
  dragCounter = 0
  const droppedFiles = e.dataTransfer?.files
  if (!droppedFiles || droppedFiles.length === 0) return
  const uploadGroupId = getUploadGroupId()
  for (let i = 0; i < droppedFiles.length; i++) {
    try {
      await fileApi.upload(droppedFiles[i], undefined, uploadGroupId)
      message.success(`${droppedFiles[i].name} 上传成功`)
    } catch (error) {
      message.error(`${droppedFiles[i].name} 上传失败`)
    }
  }
  loadFiles()
  loadGroups()
}

// 预览
async function handlePreview(file: SysFile) {
  previewFile.value = file
  isEditing.value = false
  editText.value = ''
  officeMode.value = 'preview'
  officePreviewError.value = ''
  officeEditorError.value = ''
  // 优先使用存储 URL（/api/files/xxx 已放行匿名），否则用带认证的预览接口生成 Blob URL
  if (file.url && (file.url.startsWith('http') || file.url.startsWith('/'))) {
    previewUrl.value = file.url
  } else {
    try {
      previewBlobUrl = await fetchBlobUrl(`/sys/file/preview/${file.id}`)
      previewUrl.value = previewBlobUrl
    } catch {
      previewUrl.value = ''
      message.error('预览加载失败')
    }
  }
  previewText.value = ''

  if (isText(file)) {
    textLoading.value = true
    try {
      const text = await fileApi.getTextContent(file.id!)
      previewText.value = text
    } catch (error) {
      previewText.value = '无法加载文件内容'
    } finally {
      textLoading.value = false
    }
  }

  if (isOffice(file)) {
    officePreviewLoading.value = true
    try {
      const res = await fileApi.getOfficePreviewUrl(file.filePath)
      previewUrl.value = res.previewUrl
    } catch (error) {
      previewUrl.value = ''
      officePreviewError.value = '未检测到 kkFileView 预览服务，Office 文档可尝试使用在线编辑或下载后查看。'
    } finally {
      officePreviewLoading.value = false
    }
  }

  previewVisible.value = true
}

// 关闭预览时释放 Blob URL
function revokePreviewBlobUrl() {
  if (previewBlobUrl) {
    URL.revokeObjectURL(previewBlobUrl)
    previewBlobUrl = ''
  }
}

// 编辑文本
function startEdit() {
  editText.value = previewText.value
  isEditing.value = true
}

function cancelEdit() {
  isEditing.value = false
}

async function handleSaveText() {
  if (!previewFile.value) return
  try {
    textLoading.value = true
    await fileApi.updateTextContent(previewFile.value.id!, editText.value)
    message.success('保存成功')
    previewText.value = editText.value
    isEditing.value = false
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    textLoading.value = false
  }
}

function handleOfficeSaved() {
  officeEditorError.value = ''
  message.success('文档已保存')
  loadFiles()
}

function handleOfficeError(msg: string) {
  officeEditorError.value = msg
}

// 下载（带认证，避免直接跳转无 Token）
async function handleDownload(file: SysFile) {
  try {
    await downloadBlob(`/sys/file/download/${file.id}`, file.originalName || 'download')
    message.success('下载已开始')
  } catch {
    message.error('下载失败')
  }
}

// 重命名
function handleRename(file: SysFile) {
  renamingFile.value = file
  renameValue.value = file.originalName
  showRenameModal.value = true
}

async function handleSaveRename() {
  if (!renameValue.value.trim()) {
    message.warning('请输入文件名')
    return
  }
  try {
    await fileApi.rename(renamingFile.value!.id!, renameValue.value)
    message.success('重命名成功')
    showRenameModal.value = false
    loadFiles()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 删除
function handleDelete(file: SysFile) {
  dialog.warning({
    title: '提示',
    content: `确定要删除文件"${file.originalName}"吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fileApi.delete(file.id!)
        message.success('删除成功')
        loadFiles()
        loadGroups()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 批量删除
function handleBatchDelete() {
  dialog.warning({
    title: '提示',
    content: `确定要删除选中的 ${selectedIds.value.length} 个文件吗？`,
    positiveText: '确定',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await fileApi.deleteBatch(selectedIds.value)
        message.success('删除成功')
        selectedIds.value = []
        loadFiles()
        loadGroups()
      } catch (error) {
        // 错误已在拦截器处理
      }
    }
  })
}

// 移动文件
async function handleMoveFiles() {
  try {
    await fileApi.moveToGroup(selectedIds.value, moveTargetGroupId.value)
    message.success('移动成功')
    showMoveModal.value = false
    selectedIds.value = []
    loadFiles()
    loadGroups()
  } catch (error) {
    // 错误已在拦截器处理
  }
}

// 文件类型判断等工具函数
function isImage(file: SysFile | null): boolean { return file?.fileType?.startsWith('image/') || false }
function isVideo(file: SysFile | null): boolean { return file?.fileType?.startsWith('video/') || false }
function isAudio(file: SysFile | null): boolean { return file?.fileType?.startsWith('audio/') || false }
function isPdf(file: SysFile | null): boolean { return file?.fileType === 'application/pdf' || file?.fileSuffix?.toLowerCase() === '.pdf' }
function isOffice(file: SysFile | null): boolean { const s = file?.fileSuffix?.toLowerCase() || ''; return ['.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx'].includes(s) }
function isText(file: SysFile | null): boolean { if (!file) return false; const textTypes = ['text/', 'application/json', 'application/xml', 'application/javascript']; const s = file.fileSuffix?.toLowerCase() || ''; return textTypes.some(t => file.fileType?.startsWith(t)) || ['.txt', '.md', '.json', '.xml', '.yaml', '.yml', '.ini', '.conf', '.cfg', '.properties', '.js', '.ts', '.vue', '.jsx', '.tsx', '.css', '.scss', '.less', '.html', '.htm', '.java', '.py', '.go', '.rs', '.c', '.cpp', '.h', '.hpp', '.cs', '.php', '.rb', '.swift', '.kt', '.sql', '.sh', '.bat', '.ps1', '.log', '.csv'].includes(s) }
function isPreviewable(file: SysFile): boolean { return isImage(file) || isVideo(file) || isAudio(file) || isPdf(file) || isText(file) || isOffice(file) }
function getCodeLanguage(file: SysFile | null): string { const s = file?.fileSuffix?.toLowerCase() || ''; const m: any = {'.js': 'javascript', '.ts': 'typescript', '.vue': 'vue', '.json': 'json', '.java': 'java', '.py': 'python', '.md': 'markdown'}; return m[s] || 'text' }
function getFileIcon(file: SysFile) { const s = file.fileSuffix?.toLowerCase() || ''; if (['.doc', '.docx', '.xls', '.xlsx', '.pdf', '.txt', '.md'].includes(s)) return DocumentTextOutline; if (['.js', '.ts', '.vue'].includes(s)) return CodeSlashOutline; if (file.fileType?.startsWith('image/')) return ImageOutline; return DocumentOutline }
function getFileIconColor(file: SysFile) { const s = file.fileSuffix?.toLowerCase() || ''; if (['.doc', '.docx'].includes(s)) return '#2b579a'; if (['.xls', '.xlsx'].includes(s)) return '#217346'; if (['.pdf'].includes(s)) return '#f40f02'; return '#9ca3af' }
function formatFileSize(bytes: number): string { if (bytes === 0) return '0 B'; const k = 1024; const s = ['B', 'KB', 'MB', 'GB', 'TB']; const i = Math.floor(Math.log(bytes) / Math.log(k)); return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + s[i] }

onMounted(() => {
  loadGroups()
  loadFiles()
})

watch(previewVisible, (visible) => {
  if (!visible) revokePreviewBlobUrl()
})
</script>

<style scoped>
.file-layout {
  display: flex;
  gap: 12px;
  height: 100%;
}

.group-card {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 160px);
}

.group-card :deep(.n-card__content) {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.card-header {
  font-size: 14px;
  font-weight: 600;
}

.type-tabs {
  display: flex;
  padding: 8px 12px;
  gap: 8px;
  border-bottom: 1px solid var(--n-border-color);
}

.type-tab {
  padding: 4px 12px;
  cursor: pointer;
  font-size: 13px;
  color: var(--n-text-color-3);
  border-radius: 4px;
  transition: all 0.2s;
}

.type-tab:hover { color: #18a058; }
.type-tab.active {
  color: #fff;
  font-weight: 500;
  background: #18a058;
}

.group-list-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.group-item {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  cursor: pointer;
  gap: 8px;
  color: var(--n-text-color);
  transition: all 0.2s;
}

.group-item:hover { background: var(--n-hover-color); }
.group-item.active { background: var(--n-primary-color-hover); color: var(--n-primary-color); }

.group-name { flex: 1; font-size: 14px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-count { font-size: 12px; color: var(--n-text-color-3); }
.group-more { opacity: 0; transition: opacity 0.2s; }
.group-item:hover .group-more { opacity: 1; }

.file-list-card {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.file-list-card :deep(.n-card__content) {
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }

.file-manager-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
}

.select-all-bar {
  padding: 8px 16px;
  border-bottom: 1px solid var(--n-border-color);
}

.file-content-wrapper {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.file-spin { height: 100%; }

.empty-state { height: 100%; display: flex; align-items: center; justify-content: center; }

/* 平铺视图 */
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.file-card {
  border: 1px solid var(--n-border-color);
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  position: relative;
  transition: all 0.2s;
}

.file-card:hover { border-color: var(--n-primary-color); box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.file-card.selected { border-color: var(--n-primary-color); background: var(--n-primary-color-hover); }

.file-checkbox { position: absolute; top: 8px; left: 8px; z-index: 1; }
.file-preview { width: 100%; height: 100px; display: flex; align-items: center; justify-content: center; background: var(--n-hover-color); border-radius: 4px; overflow: hidden; margin-bottom: 8px; }
.file-preview img { max-width: 100%; max-height: 100%; object-fit: contain; }
.file-preview video { max-width: 100%; max-height: 100%; }

.file-name { font-size: 13px; margin-bottom: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-actions { display: flex; gap: 4px; font-size: 12px; }
.file-actions a { color: var(--n-primary-color); cursor: pointer; }
.file-actions span { color: var(--n-text-color-3); }

/* 列表视图 */
.file-list { display: flex; flex-direction: column; }
.file-row { display: flex; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--n-border-color); cursor: pointer; gap: 12px; }
.file-row:hover { background: var(--n-hover-color); }
.file-preview-small { width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; background: var(--n-hover-color); border-radius: 4px; overflow: hidden; }
.file-preview-small img { width: 100%; height: 100%; object-fit: cover; }
.file-info { flex: 1; min-width: 0; }
.file-meta { display: flex; gap: 12px; font-size: 12px; color: var(--n-text-color-3); margin-top: 2px; }

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
}
.goto { display: flex; align-items: center; gap: 4px; }

/* 预览相关样式 */
.preview-container { display: flex; justify-content: center; align-items: center; min-height: 300px; }
.preview-image, .preview-video { max-width: 100%; max-height: 70vh; }
.preview-pdf, .preview-office { width: 100%; height: 75vh; border: none; }
.preview-office-frame { width: 100%; height: 100%; border: none; }
.office-editor-wrapper { width: 100%; height: 75vh; }
.preview-text-wrapper { width: 100%; height: 100%; display: flex; flex-direction: column; gap: 12px; }
.preview-toolbar { display: flex; justify-content: flex-end; gap: 8px; }
.preview-text { width: 100%; max-height: 70vh; overflow: auto; background: #1e1e1e; border-radius: 4px; padding: 12px; }
.edit-text { width: 100%; height: 70vh; display: flex; }
.edit-text :deep(.n-input) { height: 100%; }

/* 拖拽上传遮罩 */
.drag-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.7); backdrop-filter: blur(4px); z-index: 100; display: flex; align-items: center; justify-content: center; }
.drag-content { text-align: center; color: #fff; padding: 40px; border: 2px dashed rgba(255,255,255,0.3); border-radius: 16px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
