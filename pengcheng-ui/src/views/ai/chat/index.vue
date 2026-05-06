<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <n-button block type="primary" @click="createNewChat">
          <template #icon><n-icon><AddOutline /></n-icon></template>
          新对话
        </n-button>
      </div>
      <div class="history-list">
        <div v-for="item in history" :key="item.id" class="history-item" :class="{ active: currentChatId === item.id }" @click="switchChat(item.id)">
          <n-icon><ChatbubbleOutline /></n-icon>
          <span class="title">{{ item.title }}</span>
          <n-button size="tiny" quaternary circle class="delete-btn">
            <template #icon><n-icon><TrashOutline /></n-icon></template>
          </n-button>
        </div>
      </div>
    </div>
    
    <div class="chat-main">
      <div class="chat-header">
        <div class="model-select">
          <n-select v-model:value="currentModel" :options="modelOptions" size="small" style="width: 200px" />
        </div>
        <div class="agent-select">
          <n-select v-model:value="currentAgent" :options="agentOptions" size="small" style="width: 150px" placeholder="选择智能体" />
        </div>
      </div>
      
      <div class="message-list" ref="scrollRef">
        <div v-if="messages.length === 0 && !loading" class="empty-chat">
          <div class="empty-icon">🤖</div>
          <div class="empty-title">智能销售助手</div>
          <div class="empty-desc">我可以帮你分析客户、生成话术、查询房源。输入问题开始对话吧！</div>
        </div>
        <div v-for="msg in messages" :key="msg.id" class="message-item" :class="msg.role">
          <n-avatar round size="small" :src="msg.role === 'user' ? userAvatar : aiAvatar">
            {{ msg.role === 'user' ? 'U' : 'AI' }}
          </n-avatar>
          <div class="message-content">
            <div class="bubble">
              <div v-if="msg.thinking" class="thinking-process">
                <n-collapse arrow-placement="right">
                  <n-collapse-item title="思考过程" name="1">
                    {{ msg.thinking }}
                  </n-collapse-item>
                </n-collapse>
              </div>
              <div class="text" v-html="renderMarkdown(msg.content)"></div>
            </div>
            <div class="meta" v-if="msg.tools && msg.tools.length > 0">
              <n-tag v-for="tool in msg.tools" :key="tool" size="tiny" type="info">调用: {{ tool }}</n-tag>
            </div>
          </div>
        </div>
        <div v-if="loading" class="message-item ai">
          <n-avatar round size="small">AI</n-avatar>
          <div class="message-content">
            <div class="bubble loading">
              <n-spin size="small" />
              <span>正在思考...</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="input-area">
        <div class="toolbar">
          <n-upload action="#" :show-file-list="false">
            <n-button quaternary circle size="small">
              <template #icon><n-icon><AttachOutline /></n-icon></template>
            </n-button>
          </n-upload>
          <n-button quaternary circle size="small">
            <template #icon><n-icon><MicOutline /></n-icon></template>
          </n-button>
        </div>
        <n-input
          v-model:value="input"
          type="textarea"
          placeholder="输入您的问题，例如：分析一下张三这个客户的购房意向..."
          :autosize="{ minRows: 2, maxRows: 6 }"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <div class="send-btn">
          <n-button type="primary" size="small" :disabled="!input.trim() || loading" @click="sendMessage">
            <template #icon><n-icon><SendOutline /></n-icon></template>
            发送
          </n-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useMessage } from 'naive-ui'
import { AddOutline, ChatbubbleOutline, TrashOutline, AttachOutline, MicOutline, SendOutline } from '@vicons/ionicons5'
import { marked } from 'marked'
import { realtyApi } from '@/api/realty'

const message = useMessage()

const currentChatId = ref(1)
const currentModel = ref('qwen-max')
const currentAgent = ref('sales_assistant')
const input = ref('')
const loading = ref(false)
const scrollRef = ref<HTMLElement>()

const userAvatar = 'https://07akioni.oss-cn-beijing.aliyuncs.com/07akioni.jpeg'
const aiAvatar = '' // Use default

const modelOptions = [
  { label: '通义千问 Max', value: 'qwen-max' },
  { label: '通义千问 Plus', value: 'qwen-plus' }
]

const agentOptions = [
  { label: '通用助手', value: 'general' },
  { label: '销售专家', value: 'sales_assistant' },
  { label: '数据分析师', value: 'data_analyst' }
]

const history = ref<{ id: number; title: string }[]>([])

const messages = ref<{ id: number; role: string; content: string; thinking: string; tools: string[] }[]>([])

function createNewChat() {
  chatMessagesCache.set(currentChatId.value, [...messages.value])
  const newId = Date.now()
  history.value.unshift({ id: newId, title: '新对话' })
  currentChatId.value = newId
  messages.value = []
}

function switchChat(id: number) {
  currentChatId.value = id
  // 会话历史为前端按会话 id 维护，切换时恢复该会话的消息（若已缓存）
  const cached = chatMessagesCache.get(id)
  messages.value = cached ? [...cached] : []
}

// 按会话 id 缓存消息，便于切换会话时展示
const chatMessagesCache = new Map<number, typeof messages.value>()

async function sendMessage() {
  if (!input.value.trim()) return

  const userMsg = input.value
  messages.value.push({ id: Date.now(), role: 'user', content: userMsg, thinking: '', tools: [] })
  input.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await realtyApi.aiChat(
      userMsg,
      String(currentChatId.value),
      undefined
    )
    loading.value = false
    const content = (res && (res as any).content) ? (res as any).content : '暂无回复，请稍后重试。'
    const thinking = (res && (res as any).routedAgent) ? `智能体: ${(res as any).routedAgent}` : ''
    const tools: string[] = (res && (res as any).structuredData) ? ['数据查询'] : []
    messages.value.push({
      id: Date.now() + 1,
      role: 'ai',
      content,
      thinking,
      tools
    })
    chatMessagesCache.set(currentChatId.value, [...messages.value])
    nextTick(() => scrollToBottom())
  } catch {
    loading.value = false
    message.error('发送失败，请检查网络或稍后重试')
    messages.value.push({
      id: Date.now() + 1,
      role: 'ai',
      content: '抱歉，请求失败。请检查网络或联系管理员。',
      thinking: '',
      tools: []
    })
    nextTick(() => scrollToBottom())
  }
}

function scrollToBottom() {
  if (scrollRef.value) {
    scrollRef.value.scrollTop = scrollRef.value.scrollHeight
  }
}

function renderMarkdown(text: string) {
  return marked(text)
}
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
}

.chat-sidebar {
  width: 260px;
  background: #f9f9f9;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px;
}

.history-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  gap: 8px;
  cursor: pointer;
  border-radius: 6px;
  margin-bottom: 4px;
  color: #333;
}

.history-item:hover {
  background: #eee;
}

.history-item.active {
  background: #e6f7ff;
  color: #1890ff;
}

.history-item .title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.delete-btn {
  opacity: 0;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  gap: 12px;
  align-items: center;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-item {
  display: flex;
  gap: 12px;
  max-width: 80%;
}

.message-item.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.bubble {
  padding: 12px 16px;
  border-radius: 8px;
  background: #f4f6f8;
  font-size: 14px;
  line-height: 1.6;
}

.message-item.user .bubble {
  background: #e6f7ff;
}

.thinking-process {
  margin-bottom: 8px;
  font-size: 12px;
  color: #666;
  border-left: 2px solid #ddd;
  padding-left: 8px;
}

.input-area {
  padding: 16px;
  border-top: 1px solid #eee;
  position: relative;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.send-btn {
  position: absolute;
  bottom: 24px;
  right: 24px;
}

.empty-chat {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}
.empty-icon { font-size: 64px; margin-bottom: 16px; }
.empty-title { font-size: 20px; font-weight: 600; color: #333; margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: #999; }

.meta {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}
</style>