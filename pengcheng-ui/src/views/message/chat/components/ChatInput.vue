<template>
  <div class="chat-input-container">
    <!-- 引用预览 -->
    <div v-if="replyMessage" class="reply-bar">
      <div class="reply-info">
        <span class="reply-label">回复</span>
        <span class="reply-name">{{ replyMessage.senderName }}</span>
        <span class="reply-text">{{ truncate(replyMessage.content, 40) }}</span>
      </div>
      <n-button text size="tiny" @click="$emit('cancelReply')">
        <n-icon><CloseOutline /></n-icon>
      </n-button>
    </div>

    <!-- @提及弹窗（群聊） -->
    <div v-if="showMentionList" class="mention-popup">
      <div class="mention-header">选择要 @ 的成员</div>
      <div
        v-for="member in filteredMembers"
        :key="member.id"
        class="mention-item"
        :class="{ active: mentionIndex === filteredMembers.indexOf(member) }"
        @click="selectMention(member)"
      >
        <n-avatar round :size="24" :src="member.avatar || undefined">
          {{ member.nickname?.charAt(0) || 'U' }}
        </n-avatar>
        <span>{{ member.nickname }}</span>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="input-toolbar">
      <n-button text size="small" @click="$emit('selectEmoji')">
        <n-icon size="20"><HappyOutline /></n-icon>
      </n-button>
      <n-button text size="small" @click="triggerFileUpload">
        <n-icon size="20"><AttachOutline /></n-icon>
      </n-button>
      <n-button text size="small" @click="triggerImageUpload">
        <n-icon size="20"><ImageOutline /></n-icon>
      </n-button>
    </div>

    <!-- 输入框 -->
    <div
      ref="inputRef"
      class="input-area"
      contenteditable="true"
      :placeholder="placeholder"
      @input="handleInput"
      @keydown="handleKeydown"
      @paste="handlePaste"
      @drop.prevent="handleDrop"
      @dragover.prevent
    ></div>

    <!-- 发送按钮 -->
    <div class="input-footer">
      <span class="input-tip">Enter 发送 / Shift+Enter 换行</span>
      <n-button type="primary" size="small" :disabled="!canSend" @click="send">
        发送
      </n-button>
    </div>

    <!-- 隐藏的文件输入 -->
    <input ref="fileInputRef" type="file" style="display: none" @change="handleFileSelect" />
    <input ref="imageInputRef" type="file" accept="image/*" style="display: none" @change="handleImageSelect" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import { NButton, NIcon, NAvatar } from 'naive-ui'
import { HappyOutline, AttachOutline, ImageOutline, CloseOutline } from '@vicons/ionicons5'

interface Member {
  id: number
  nickname: string
  avatar?: string
}

interface ChatMessage {
  id: number
  senderId: number
  senderName: string
  content: string
  msgType: number
}

const props = defineProps<{
  replyMessage?: ChatMessage | null
  groupMembers?: Member[]
  isGroup?: boolean
  disabled?: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  send: [content: string, msgType: number, replyToId?: number, atUserIds?: string]
  sendFile: [file: File]
  sendImage: [file: File]
  cancelReply: []
  selectEmoji: []
}>()

const inputRef = ref<HTMLDivElement>()
const fileInputRef = ref<HTMLInputElement>()
const imageInputRef = ref<HTMLInputElement>()

const inputText = ref('')
const showMentionList = ref(false)
const mentionSearch = ref('')
const mentionIndex = ref(0)
const mentionedUsers = ref<number[]>([])

const canSend = computed(() => inputText.value.trim().length > 0 && !props.disabled)

const filteredMembers = computed(() => {
  if (!props.groupMembers) return []
  if (!mentionSearch.value) return props.groupMembers
  return props.groupMembers.filter(m =>
    m.nickname.toLowerCase().includes(mentionSearch.value.toLowerCase())
  )
})

function handleInput() {
  if (inputRef.value) {
    inputText.value = inputRef.value.innerText
  }

  if (props.isGroup && inputRef.value) {
    const text = inputRef.value.innerText
    const lastAtIndex = text.lastIndexOf('@')
    if (lastAtIndex >= 0) {
      const afterAt = text.substring(lastAtIndex + 1)
      if (!afterAt.includes(' ') && afterAt.length < 20) {
        mentionSearch.value = afterAt
        showMentionList.value = true
        return
      }
    }
  }
  showMentionList.value = false
}

function handleKeydown(e: KeyboardEvent) {
  if (showMentionList.value) {
    if (e.key === 'ArrowDown') {
      e.preventDefault()
      mentionIndex.value = Math.min(mentionIndex.value + 1, filteredMembers.value.length - 1)
      return
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault()
      mentionIndex.value = Math.max(mentionIndex.value - 1, 0)
      return
    }
    if (e.key === 'Enter') {
      e.preventDefault()
      if (filteredMembers.value[mentionIndex.value]) {
        selectMention(filteredMembers.value[mentionIndex.value])
      }
      return
    }
    if (e.key === 'Escape') {
      showMentionList.value = false
      return
    }
  }

  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

function handlePaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      e.preventDefault()
      const file = item.getAsFile()
      if (file) {
        emit('sendImage', file)
      }
      return
    }
  }
}

function handleDrop(e: DragEvent) {
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return

  for (const file of files) {
    if (file.type.startsWith('image/')) {
      emit('sendImage', file)
    } else {
      emit('sendFile', file)
    }
  }
}

function selectMention(member: Member) {
  if (!inputRef.value) return
  const text = inputRef.value.innerText
  const lastAtIndex = text.lastIndexOf('@')
  if (lastAtIndex >= 0) {
    inputRef.value.innerText = text.substring(0, lastAtIndex) + `@${member.nickname} `
    mentionedUsers.value.push(member.id)
  }
  showMentionList.value = false
  inputText.value = inputRef.value.innerText
  focusInput()
}

function send() {
  if (!canSend.value) return
  const content = inputText.value.trim()
  const replyToId = props.replyMessage?.id
  const atUserIds = mentionedUsers.value.length > 0 ? mentionedUsers.value.join(',') : undefined

  emit('send', content, 1, replyToId, atUserIds)

  if (inputRef.value) {
    inputRef.value.innerText = ''
  }
  inputText.value = ''
  mentionedUsers.value = []
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

function triggerImageUpload() {
  imageInputRef.value?.click()
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    emit('sendFile', input.files[0])
    input.value = ''
  }
}

function handleImageSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    emit('sendImage', input.files[0])
    input.value = ''
  }
}

function focusInput() {
  nextTick(() => {
    inputRef.value?.focus()
  })
}

function truncate(text: string, maxLen: number): string {
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

defineExpose({ focusInput })
</script>

<style scoped>
.chat-input-container {
  border-top: 1px solid #e8e8e8;
  background: #fff;
}
.reply-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #f7f7f7;
  border-bottom: 1px solid #eee;
}
.reply-info {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
.reply-label {
  color: #18a058;
  margin-right: 4px;
}
.reply-name {
  font-weight: 500;
  margin-right: 4px;
}
.input-toolbar {
  display: flex;
  gap: 8px;
  padding: 6px 12px 0;
}
.input-area {
  min-height: 60px;
  max-height: 120px;
  padding: 8px 12px;
  overflow-y: auto;
  outline: none;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
}
.input-area:empty::before {
  content: attr(placeholder);
  color: #ccc;
}
.input-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 12px 8px;
}
.input-tip {
  font-size: 12px;
  color: #999;
}
.mention-popup {
  position: absolute;
  bottom: 100%;
  left: 12px;
  width: 200px;
  max-height: 200px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 100;
}
.mention-header {
  padding: 8px 12px;
  font-size: 12px;
  color: #999;
  border-bottom: 1px solid #f0f0f0;
}
.mention-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  cursor: pointer;
  font-size: 13px;
}
.mention-item:hover, .mention-item.active {
  background: #f5f5f5;
}
</style>
