<template>
  <div class="channel-container">
    <n-card title="多渠道推送管理">
      <template #header-extra>
        <n-button type="primary" @click="showAddChannel = true">
          <template #icon><n-icon :component="AddOutline" /></template>
          添加渠道
        </n-button>
      </template>

      <n-empty v-if="channels.length === 0" description="暂无推送渠道配置" style="margin: 40px 0" />
      <n-grid v-else :cols="3" :x-gap="16" :y-gap="16">
        <n-gi v-for="channel in channels" :key="channel.id">
          <n-card size="small" hoverable>
            <div class="channel-card">
              <div class="channel-header">
                <div class="channel-icon" :style="{ background: channelColor(channel.channelType) }">
                  {{ channelLabel(channel.channelType).charAt(0) }}
                </div>
                <div class="channel-info">
                  <div class="channel-name">{{ channel.channelName }}</div>
                  <n-tag :type="channel.enabled ? 'success' : 'default'" size="small">
                    {{ channel.enabled ? '已启用' : '未启用' }}
                  </n-tag>
                </div>
              </div>

              <div class="channel-type">{{ channelLabel(channel.channelType) }}</div>

              <div class="channel-webhook">
                <span class="label">Webhook:</span>
                <span class="value">{{ channel.webhookUrl ? maskUrl(channel.webhookUrl) : '未配置' }}</span>
              </div>

              <div class="channel-actions">
                <n-button size="small" :type="channel.enabled ? 'warning' : 'success'"
                          @click="toggleChannel(channel)">
                  {{ channel.enabled ? '禁用' : '启用' }}
                </n-button>
                <n-button size="small" @click="editChannel(channel)">编辑</n-button>
                <n-button size="small" @click="testChannel(channel.id)">测试</n-button>
                <n-button size="small" type="error" @click="deleteChannel(channel.id)">删除</n-button>
              </div>
            </div>
          </n-card>
        </n-gi>
      </n-grid>
    </n-card>

    <!-- 推送日志 -->
    <n-card title="推送日志" style="margin-top: 16px">
      <n-data-table :columns="logColumns" :data="pushLogs" :max-height="400" />
    </n-card>

    <!-- 编辑弹窗 -->
    <n-modal v-model:show="showAddChannel" preset="dialog" :title="editingChannel.id ? '编辑渠道' : '添加渠道'" style="width: 520px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="渠道类型">
          <n-select v-model:value="editingChannel.channelType" :options="channelTypeOptions" />
        </n-form-item>
        <n-form-item label="渠道名称">
          <n-input v-model:value="editingChannel.channelName" placeholder="输入渠道名称" />
        </n-form-item>
        <n-form-item label="Webhook">
          <n-input v-model:value="editingChannel.webhookUrl" placeholder="输入 Webhook URL" />
        </n-form-item>
        <n-form-item label="App Key">
          <n-input v-model:value="editingChannel.appKey" placeholder="可选" />
        </n-form-item>
        <n-form-item label="App Secret">
          <n-input v-model:value="editingChannel.appSecret" type="password" placeholder="可选" show-password-on="click" />
        </n-form-item>
      </n-form>
      <template #action>
        <n-button @click="showAddChannel = false">取消</n-button>
        <n-button type="primary" @click="saveChannel">保存</n-button>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, h } from 'vue'
import {
  NCard, NButton, NIcon, NTag, NGrid, NGi, NModal,
  NForm, NFormItem, NInput, NSelect, NDataTable, useMessage
} from 'naive-ui'
import { AddOutline } from '@vicons/ionicons5'
import request from '@/utils/request'

const message = useMessage()

const channels = ref<any[]>([])
const pushLogs = ref<any[]>([])
const showAddChannel = ref(false)
const editingChannel = ref<any>({ channelType: 'dingtalk', channelName: '', webhookUrl: '' })

const channelTypeOptions = [
  { label: '钉钉机器人', value: 'dingtalk' },
  { label: '飞书机器人', value: 'feishu' },
  { label: '企业微信', value: 'wecom' },
  { label: '邮件', value: 'email' }
]

const channelLabel = (type: string) => {
  const map: Record<string, string> = { dingtalk: '钉钉机器人', feishu: '飞书机器人', wecom: '企业微信', email: '邮件通知' }
  return map[type] || type
}

const channelColor = (type: string) => {
  const map: Record<string, string> = { dingtalk: '#1890ff', feishu: '#00d6b9', wecom: '#07c160', email: '#fa8c16' }
  return map[type] || '#999'
}

const maskUrl = (url: string) => url.length > 40 ? url.substring(0, 25) + '...' + url.substring(url.length - 10) : url

const logColumns = [
  { title: 'ID', key: 'id', width: 60 },
  { title: '消息类型', key: 'messageType', width: 100 },
  { title: '目标', key: 'target', width: 120 },
  { title: '内容', key: 'content', ellipsis: true },
  {
    title: '状态', key: 'status', width: 80,
    render: (row: any) => h(NTag, { type: row.status === 1 ? 'success' : row.status === 2 ? 'error' : 'default', size: 'small' },
      () => row.status === 1 ? '成功' : row.status === 2 ? '失败' : '待发')
  },
  { title: '时间', key: 'sentAt', width: 170, render: (row: any) => h('span', row.sentAt ? new Date(row.sentAt).toLocaleString('zh-CN') : '') }
]

onMounted(async () => {
  await loadChannels()
  await loadLogs()
})

async function loadChannels() {
  try {
    const res = await request.get('/channel/list')
    channels.value = Array.isArray(res) ? res : []
  } catch { channels.value = [] }
}

async function loadLogs() {
  try {
    const res = await request.get('/channel/logs', { params: { limit: 50 } })
    pushLogs.value = Array.isArray(res) ? res : []
  } catch { pushLogs.value = [] }
}

function editChannel(ch: any) {
  editingChannel.value = { ...ch }
  showAddChannel.value = true
}

async function saveChannel() {
  try {
    await request.post('/channel/save', editingChannel.value)
    message.success('保存成功')
    showAddChannel.value = false
    editingChannel.value = { channelType: 'dingtalk', channelName: '', webhookUrl: '' }
    loadChannels()
  } catch {
    message.error('保存失败')
  }
}

async function toggleChannel(ch: any) {
  try {
    await request.post(`/channel/toggle/${ch.id}`, null, { params: { enabled: !ch.enabled } })
    message.success(ch.enabled ? '已禁用' : '已启用')
    loadChannels()
  } catch {
    message.error('操作失败')
  }
}

async function testChannel(id: number) {
  try {
    const res: any = await request.post(`/channel/test/${id}`)
    if (res?.success) {
      message.success('测试消息发送成功')
    } else {
      message.warning('测试失败，请检查 Webhook 配置')
    }
    loadLogs()
  } catch {
    message.error('测试请求失败')
  }
}

async function deleteChannel(id: number) {
  try {
    await request.delete(`/channel/${id}`)
    message.success('已删除')
    loadChannels()
  } catch {
    message.error('删除失败')
  }
}
</script>

<style scoped>
.channel-container { padding: 0; }
.channel-card { display: flex; flex-direction: column; gap: 12px; }
.channel-header { display: flex; align-items: center; gap: 12px; }
.channel-icon {
  width: 40px; height: 40px; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  color: #fff; font-weight: 700; font-size: 18px;
}
.channel-info { flex: 1; }
.channel-name { font-weight: 600; font-size: 15px; margin-bottom: 4px; }
.channel-type { color: #666; font-size: 13px; }
.channel-webhook { font-size: 12px; color: #999; }
.channel-webhook .label { margin-right: 4px; }
.channel-actions { display: flex; gap: 8px; flex-wrap: wrap; }
</style>
