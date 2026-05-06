<template>
  <n-drawer v-model:show="visible" :width="420" placement="right">
    <n-drawer-content :title="group?.name || '群聊设置'" :native-scrollbar="false">
      <n-tabs type="line" animated>
        <!-- 群信息 -->
        <n-tab-pane name="info" tab="群信息">
          <n-form :model="editForm" label-placement="left" label-width="80">
            <n-form-item label="群名称">
              <n-input v-model:value="editForm.name" placeholder="群名称" :disabled="!canEdit" />
            </n-form-item>
            <n-form-item label="群公告">
              <n-input v-model:value="editForm.announcement" type="textarea" :rows="3" placeholder="群公告" :disabled="!canEdit" />
            </n-form-item>
            <n-form-item label="免打扰">
              <n-switch v-model:value="editForm.muted" />
            </n-form-item>
            <n-form-item label="置顶聊天">
              <n-switch v-model:value="editForm.pinned" />
            </n-form-item>
            <n-form-item v-if="canEdit">
              <n-button type="primary" @click="handleSave">保存修改</n-button>
            </n-form-item>
          </n-form>
        </n-tab-pane>

        <!-- 群成员 -->
        <n-tab-pane name="members" :tab="`成员(${members.length})`">
          <div class="member-header">
            <n-input v-model:value="memberSearch" placeholder="搜索成员" clearable size="small" />
            <n-button v-if="canEdit" size="small" type="primary" @click="showAddMember = true">
              <template #icon><n-icon><AddOutline /></n-icon></template>
              添加
            </n-button>
          </div>

          <div class="member-list">
            <div v-for="member in filteredMembers" :key="member.id" class="member-item">
              <n-avatar round size="small" :src="member.avatar || undefined">
                {{ member.userNickname?.charAt(0) || 'U' }}
              </n-avatar>
              <div class="member-info">
                <div class="member-name">
                  {{ member.nickname || member.userNickname }}
                  <n-tag v-if="member.role === 2" size="tiny" type="warning">群主</n-tag>
                  <n-tag v-else-if="member.role === 1" size="tiny" type="info">管理员</n-tag>
                </div>
                <div class="member-status">
                  <span v-if="onlineStatus[member.userId]" class="status-online">在线</span>
                  <span v-else class="status-offline">离线</span>
                </div>
              </div>
              <n-dropdown v-if="canManage(member)" :options="getMemberActions(member)" @select="(key: string) => handleMemberAction(key, member)">
                <n-button text size="small"><n-icon><EllipsisVerticalOutline /></n-icon></n-button>
              </n-dropdown>
            </div>
          </div>
        </n-tab-pane>

        <!-- 群文件 -->
        <n-tab-pane name="files" tab="群文件">
          <n-empty v-if="!groupFiles.length" description="暂无群文件" size="small" />
          <div v-else class="file-list">
            <div v-for="file in groupFiles" :key="file.id" class="file-item" @click="$emit('previewFile', file.url)">
              <n-icon size="20" color="#18a058"><DocumentTextOutline /></n-icon>
              <div class="file-info">
                <div class="file-name">{{ file.name }}</div>
                <div class="file-meta">{{ file.sender }} · {{ file.time }}</div>
              </div>
            </div>
          </div>
        </n-tab-pane>
      </n-tabs>

      <!-- 底部操作 -->
      <template #footer>
        <n-space>
          <n-button v-if="isOwner" type="error" @click="handleDissolve">解散群聊</n-button>
          <n-button v-else type="warning" @click="handleQuit">退出群聊</n-button>
          <n-button @click="handleClearHistory">清空聊天记录</n-button>
        </n-space>
      </template>
    </n-drawer-content>
  </n-drawer>

  <!-- 添加成员弹窗 -->
  <n-modal v-model:show="showAddMember" preset="card" title="添加成员" style="width: 400px">
    <n-transfer
      v-model:value="selectedNewMembers"
      :options="availableUsers"
      source-filterable
      style="height: 300px"
    />
    <template #footer>
      <n-space justify="end">
        <n-button @click="showAddMember = false">取消</n-button>
        <n-button type="primary" @click="handleAddMembers" :disabled="selectedNewMembers.length === 0">确定</n-button>
      </n-space>
    </template>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import {
  NDrawer, NDrawerContent, NTabs, NTabPane, NForm, NFormItem, NInput, NSwitch,
  NButton, NIcon, NAvatar, NTag, NDropdown, NSpace, NEmpty, NModal, NTransfer,
  useDialog, useMessage
} from 'naive-ui'
import { AddOutline, EllipsisVerticalOutline, DocumentTextOutline } from '@vicons/ionicons5'

interface GroupInfo {
  id: number
  name: string
  announcement?: string
  ownerId: number
  memberCount: number
}

interface GroupMember {
  id: number
  userId: number
  userNickname: string
  nickname?: string
  avatar?: string
  role: number
}

interface GroupFile {
  id: number
  name: string
  url: string
  sender: string
  time: string
}

const props = defineProps<{
  group: GroupInfo | null
  members: GroupMember[]
  currentUserId: number
  onlineStatus: Record<number, boolean>
  availableUsers: Array<{ label: string; value: number }>
  groupFiles?: GroupFile[]
}>()

const emit = defineEmits<{
  save: [data: { name: string; announcement: string }]
  addMembers: [memberIds: number[]]
  removeMember: [memberId: number]
  setAdmin: [memberId: number]
  removeAdmin: [memberId: number]
  dissolve: []
  quit: []
  clearHistory: []
  previewFile: [url: string]
}>()

const visible = defineModel<boolean>('show', { default: false })

const dialog = useDialog()
const msg = useMessage()

const memberSearch = ref('')
const showAddMember = ref(false)
const selectedNewMembers = ref<number[]>([])

const editForm = reactive({
  name: '',
  announcement: '',
  muted: false,
  pinned: false
})

watch(() => props.group, (g) => {
  if (g) {
    editForm.name = g.name || ''
    editForm.announcement = g.announcement || ''
  }
}, { immediate: true })

const isOwner = computed(() => props.group?.ownerId === props.currentUserId)

const canEdit = computed(() => {
  if (isOwner.value) return true
  return props.members.some(m => m.userId === props.currentUserId && m.role >= 1)
})

const filteredMembers = computed(() => {
  if (!memberSearch.value) return props.members
  const kw = memberSearch.value.toLowerCase()
  return props.members.filter(m =>
    (m.nickname || m.userNickname)?.toLowerCase().includes(kw)
  )
})

const groupFiles = computed(() => props.groupFiles || [])

function canManage(member: GroupMember): boolean {
  if (member.userId === props.currentUserId) return false
  if (isOwner.value) return true
  const myRole = props.members.find(m => m.userId === props.currentUserId)?.role || 0
  return myRole > member.role
}

function getMemberActions(member: GroupMember) {
  const actions: Array<{ label: string; key: string }> = []
  if (isOwner.value) {
    if (member.role === 1) {
      actions.push({ label: '取消管理员', key: 'removeAdmin' })
    } else if (member.role === 0) {
      actions.push({ label: '设为管理员', key: 'setAdmin' })
    }
  }
  if (canManage(member)) {
    actions.push({ label: '移出群聊', key: 'remove' })
  }
  return actions
}

function handleMemberAction(key: string, member: GroupMember) {
  switch (key) {
    case 'remove':
      dialog.warning({
        title: '移出成员',
        content: `确定将 ${member.nickname || member.userNickname} 移出群聊？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: () => emit('removeMember', member.userId)
      })
      break
    case 'setAdmin':
      emit('setAdmin', member.userId)
      break
    case 'removeAdmin':
      emit('removeAdmin', member.userId)
      break
  }
}

function handleSave() {
  emit('save', { name: editForm.name, announcement: editForm.announcement })
  msg.success('已保存')
}

function handleAddMembers() {
  emit('addMembers', selectedNewMembers.value)
  showAddMember.value = false
  selectedNewMembers.value = []
}

function handleDissolve() {
  dialog.error({
    title: '解散群聊',
    content: '解散后所有群聊记录将被清除，此操作不可恢复！',
    positiveText: '解散',
    negativeText: '取消',
    onPositiveClick: () => emit('dissolve')
  })
}

function handleQuit() {
  dialog.warning({
    title: '退出群聊',
    content: '退出后将不再接收群消息，确定退出？',
    positiveText: '退出',
    negativeText: '取消',
    onPositiveClick: () => emit('quit')
  })
}

function handleClearHistory() {
  dialog.warning({
    title: '清空聊天记录',
    content: '仅清空本地聊天记录，其他成员不受影响',
    positiveText: '清空',
    negativeText: '取消',
    onPositiveClick: () => emit('clearHistory')
  })
}
</script>

<style scoped>
.member-header {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
}
.member-item:hover {
  background: #f5f5f5;
}
.member-info {
  flex: 1;
  min-width: 0;
}
.member-name {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
}
.member-status {
  font-size: 12px;
}
.status-online {
  color: #18a058;
}
.status-offline {
  color: #999;
}
.file-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
}
.file-item:hover {
  background: #f5f5f5;
}
.file-info {
  flex: 1;
  min-width: 0;
}
.file-name {
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.file-meta {
  font-size: 11px;
  color: #999;
}
</style>
