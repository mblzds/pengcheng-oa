<template>
  <div
    class="message-bubble"
    :class="{ 'is-self': isSelf, 'is-recalled': message.recalled }"
    @contextmenu.prevent="showContextMenu"
  >
    <!-- 引用的消息 -->
    <div v-if="message.replyMessage" class="reply-preview" @click="$emit('scrollToMessage', message.replyToId)">
      <div class="reply-name">{{ message.replyMessage.senderName }}</div>
      <div class="reply-content">{{ truncateText(message.replyMessage.content, 50) }}</div>
    </div>

    <!-- 消息内容 -->
    <div class="bubble-content">
      <template v-if="message.recalled">
        <span class="recalled-text">{{ isSelf ? '你撤回了一条消息' : `"${message.senderName}" 撤回了一条消息` }}</span>
      </template>
      <template v-else-if="message.msgType === 1">
        <span class="text-content" v-html="renderContent(message.content)"></span>
      </template>
      <template v-else-if="message.msgType === 2">
        <img :src="message.content" class="image-content" @click="$emit('previewImage', message.content)" />
      </template>
      <template v-else-if="message.msgType === 3">
        <div class="file-content" @click="$emit('downloadFile', message.content)">
          <n-icon size="24"><DocumentOutline /></n-icon>
          <span>{{ extractFileName(message.content) }}</span>
        </div>
      </template>
    </div>

    <!-- 时间和状态 -->
    <div class="bubble-footer">
      <span class="time">{{ formatTime(message.sendTime) }}</span>
      <span v-if="isSelf && !message.recalled" class="status">
        <n-icon v-if="message.delivered" size="12" color="#18a058"><CheckmarkDoneOutline /></n-icon>
        <n-icon v-else size="12" color="#999"><CheckmarkOutline /></n-icon>
      </span>
    </div>

    <!-- 右键菜单 -->
    <n-dropdown
      :show="contextMenuVisible"
      :x="contextMenuX"
      :y="contextMenuY"
      :options="contextMenuOptions"
      placement="bottom-start"
      @clickoutside="contextMenuVisible = false"
      @select="handleContextMenu"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { NIcon, NDropdown } from 'naive-ui'
import { DocumentOutline, CheckmarkOutline, CheckmarkDoneOutline } from '@vicons/ionicons5'

interface ChatMessage {
  id: number
  senderId: number
  senderName: string
  content: string
  msgType: number
  recalled: number
  replyToId?: number
  replyMessage?: ChatMessage
  seq: number
  delivered: number
  readFlag: number
  sendTime: string
}

const props = defineProps<{
  message: ChatMessage
  currentUserId: number
}>()

const emit = defineEmits<{
  recall: [messageId: number]
  reply: [message: ChatMessage]
  copy: [content: string]
  scrollToMessage: [messageId: number]
  previewImage: [url: string]
  downloadFile: [url: string]
}>()

const isSelf = computed(() => props.message.senderId === props.currentUserId)

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)

const RECALL_WINDOW = 2 * 60 * 1000

const canRecall = computed(() => {
  if (!isSelf.value || props.message.recalled) return false
  const sendTime = new Date(props.message.sendTime).getTime()
  return Date.now() - sendTime < RECALL_WINDOW
})

const contextMenuOptions = computed(() => {
  const options: Array<{ label: string; key: string }> = []
  if (!props.message.recalled) {
    options.push({ label: '回复', key: 'reply' })
    if (props.message.msgType === 1) {
      options.push({ label: '复制', key: 'copy' })
    }
    if (canRecall.value) {
      options.push({ label: '撤回', key: 'recall' })
    }
  }
  return options
})

function showContextMenu(e: MouseEvent) {
  if (props.message.recalled) return
  contextMenuX.value = e.clientX
  contextMenuY.value = e.clientY
  contextMenuVisible.value = true
}

function handleContextMenu(key: string) {
  contextMenuVisible.value = false
  switch (key) {
    case 'recall': emit('recall', props.message.id); break
    case 'reply': emit('reply', props.message); break
    case 'copy': emit('copy', props.message.content); break
  }
}

function formatTime(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function truncateText(text: string, maxLen: number): string {
  return text.length > maxLen ? text.substring(0, maxLen) + '...' : text
}

function extractFileName(url: string): string {
  return url.split('/').pop() || '文件'
}

function renderContent(content: string): string {
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}
</script>

<style scoped>
.message-bubble {
  max-width: 70%;
  margin-bottom: 12px;
  position: relative;
}
.message-bubble.is-self {
  margin-left: auto;
}
.bubble-content {
  padding: 8px 12px;
  border-radius: 12px;
  background: #f0f0f0;
  word-break: break-word;
  line-height: 1.5;
}
.is-self .bubble-content {
  background: #95ec69;
}
.is-recalled .bubble-content {
  background: transparent;
}
.recalled-text {
  color: #999;
  font-size: 13px;
  font-style: italic;
}
.reply-preview {
  padding: 4px 8px;
  margin-bottom: 4px;
  background: rgba(0, 0, 0, 0.04);
  border-left: 3px solid #18a058;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
}
.reply-name {
  color: #18a058;
  font-weight: 500;
}
.reply-content {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.image-content {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
}
.file-content {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #18a058;
}
.bubble-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 2px;
  justify-content: flex-end;
}
.time {
  font-size: 11px;
  color: #999;
}
.text-content {
  font-size: 14px;
}
</style>
