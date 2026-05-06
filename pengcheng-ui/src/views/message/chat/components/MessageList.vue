<template>
  <div ref="containerRef" class="message-list" @scroll="handleScroll">
    <!-- 加载历史消息 -->
    <div v-if="loadingHistory" class="loading-more">
      <n-spin size="small" />
      <span>加载中...</span>
    </div>
    <div v-else-if="noMoreHistory" class="no-more">
      <span>没有更多消息了</span>
    </div>

    <!-- 日期分隔线 -->
    <template v-for="(group, idx) in groupedMessages" :key="idx">
      <div class="date-divider">
        <span>{{ group.date }}</span>
      </div>
      <div
        v-for="msg in group.messages"
        :key="msg.id"
        :data-msg-id="msg.id"
        class="message-row"
        :class="{ 'message-self': msg.senderId === currentUserId, 'message-system': msg.msgType === 4 }"
      >
        <!-- 系统消息 -->
        <div v-if="msg.msgType === 4" class="system-message">
          {{ msg.content }}
        </div>
        <!-- 普通消息 -->
        <template v-else>
          <n-avatar
            v-if="msg.senderId !== currentUserId"
            round
            size="small"
            :src="msg.senderAvatar || undefined"
            class="msg-avatar"
          >
            {{ msg.senderName?.charAt(0) || 'U' }}
          </n-avatar>

          <div class="msg-body">
            <div v-if="showSenderName && msg.senderId !== currentUserId" class="msg-sender">
              {{ msg.senderName }}
            </div>
            <MessageBubble
              :message="msg"
              :current-user-id="currentUserId"
              @recall="handleRecall"
              @reply="handleReply"
              @copy="handleCopy"
              @scroll-to-message="scrollToMessage"
              @preview-image="handlePreviewImage"
              @download-file="handleDownloadFile"
            />
          </div>

          <n-avatar
            v-if="msg.senderId === currentUserId"
            round
            size="small"
            :src="currentUserAvatar || undefined"
            class="msg-avatar"
          >
            {{ currentUserName?.charAt(0) || 'U' }}
          </n-avatar>
        </template>
      </div>
    </template>

    <n-empty v-if="messages.length === 0 && !loadingHistory" description="暂无消息" size="small" class="empty-state" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { NAvatar, NSpin, NEmpty, useMessage } from 'naive-ui'
import MessageBubble from './MessageBubble.vue'

interface ChatMessage {
  id: number
  senderId: number
  senderName: string
  senderAvatar?: string
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

interface DateGroup {
  date: string
  messages: ChatMessage[]
}

const props = withDefaults(defineProps<{
  messages: ChatMessage[]
  currentUserId: number
  currentUserName?: string
  currentUserAvatar?: string
  showSenderName?: boolean
  loadingHistory?: boolean
  noMoreHistory?: boolean
}>(), {
  showSenderName: false,
  loadingHistory: false,
  noMoreHistory: false
})

const emit = defineEmits<{
  loadMore: []
  recall: [messageId: number]
  reply: [message: ChatMessage]
  previewImage: [url: string]
  downloadFile: [url: string]
}>()

const message = useMessage()
const containerRef = ref<HTMLDivElement>()
const isUserScrolledUp = ref(false)

const groupedMessages = computed<DateGroup[]>(() => {
  const groups: DateGroup[] = []
  let currentDate = ''
  for (const msg of props.messages) {
    const dateStr = formatDate(msg.sendTime)
    if (dateStr !== currentDate) {
      currentDate = dateStr
      groups.push({ date: dateStr, messages: [] })
    }
    groups[groups.length - 1].messages.push(msg)
  }
  return groups
})

function formatDate(time: string): string {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  if (isToday) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return '昨天'
  const year = d.getFullYear() === now.getFullYear() ? '' : d.getFullYear() + '年'
  return `${year}${d.getMonth() + 1}月${d.getDate()}日`
}

function handleScroll() {
  const el = containerRef.value
  if (!el) return

  const distanceFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  isUserScrolledUp.value = distanceFromBottom > 100

  if (el.scrollTop < 50 && !props.loadingHistory && !props.noMoreHistory) {
    emit('loadMore')
  }
}

function scrollToBottom(smooth = false) {
  nextTick(() => {
    const el = containerRef.value
    if (el) {
      el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
    }
  })
}

function scrollToMessage(msgId: number) {
  const el = containerRef.value?.querySelector(`[data-msg-id="${msgId}"]`) as HTMLElement
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    el.classList.add('highlight-flash')
    setTimeout(() => el.classList.remove('highlight-flash'), 2000)
  }
}

function handleRecall(msgId: number) {
  emit('recall', msgId)
}

function handleReply(msg: ChatMessage) {
  emit('reply', msg)
}

function handleCopy(content: string) {
  navigator.clipboard.writeText(content).then(() => {
    message.success('已复制')
  })
}

function handlePreviewImage(url: string) {
  emit('previewImage', url)
}

function handleDownloadFile(url: string) {
  emit('downloadFile', url)
}

watch(() => props.messages.length, (newLen, oldLen) => {
  if (newLen > oldLen && !isUserScrolledUp.value) {
    scrollToBottom(true)
  }
})

onMounted(() => {
  scrollToBottom()
})

defineExpose({ scrollToBottom, scrollToMessage })
</script>

<style scoped>
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.loading-more, .no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  color: #999;
  font-size: 12px;
}
.date-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 12px 0 8px;
}
.date-divider span {
  font-size: 12px;
  color: #999;
  background: #f0f0f0;
  padding: 2px 12px;
  border-radius: 10px;
}
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}
.message-row.message-self {
  flex-direction: row-reverse;
}
.message-row.message-system {
  justify-content: center;
}
.system-message {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 4px 12px;
  border-radius: 10px;
}
.msg-avatar {
  flex-shrink: 0;
  margin-top: 4px;
}
.msg-body {
  max-width: 60%;
  min-width: 80px;
}
.msg-sender {
  font-size: 12px;
  color: #999;
  margin-bottom: 2px;
  padding-left: 4px;
}
.empty-state {
  margin: auto;
}
.highlight-flash {
  animation: flashHighlight 2s ease-out;
}
@keyframes flashHighlight {
  0% { background: rgba(24, 160, 88, 0.15); }
  100% { background: transparent; }
}
</style>
