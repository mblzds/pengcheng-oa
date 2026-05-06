<template>
  <div class="skills-container">
    <!-- 统计卡片 -->
    <n-grid :cols="3" :x-gap="16" style="margin-bottom: 16px">
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ stats.totalTools || 0 }}</div>
          <div class="stat-label">注册工具总数</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card enabled">
          <div class="stat-value">{{ stats.enabledCount || 0 }}</div>
          <div class="stat-label">已启用</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card disabled">
          <div class="stat-value">{{ stats.disabledCount || 0 }}</div>
          <div class="stat-label">已禁用</div>
        </n-card>
      </n-gi>
    </n-grid>

    <n-card title="Agent Skill 管理">
      <template #header-extra>
        <n-tag size="small" type="info">工具根据意图自动路由</n-tag>
      </template>

      <n-data-table :columns="columns" :data="skills" :loading="loading" size="small" />
    </n-card>

    <!-- 意图分布 -->
    <n-card title="意图分布" size="small" style="margin-top: 16px">
      <n-space>
        <n-tag v-for="(count, intent) in (stats.intentDistribution || {})" :key="intent" :type="intentColor(intent as string)" round>
          {{ intent }}: {{ count }}
        </n-tag>
      </n-space>
    </n-card>
  </div>
</template>

<script setup lang="ts">
import { ref, h, onMounted } from 'vue'
import { NSwitch, NTag, useMessage } from 'naive-ui'
import { request } from '@/utils/request'

const message = useMessage()
const skills = ref<any[]>([])
const stats = ref<any>({})
const loading = ref(false)

function intentColor(intent: string) {
  const map: any = {
    CUSTOMER_QUERY: 'info', REPORT: 'warning', KNOWLEDGE: 'success',
    COPYWRITING: 'default', APPROVAL: 'error', GENERAL: 'info'
  }
  return map[intent] || 'default'
}

const columns = [
  { title: '工具名称', key: 'name', width: 180 },
  {
    title: '意图类型', key: 'intent', width: 160,
    render: (row: any) => h(NTag, { type: intentColor(row.intent), size: 'small' }, { default: () => row.intent })
  },
  { title: '实现类', key: 'className', width: 220 },
  {
    title: '状态', key: 'enabled', width: 100,
    render: (row: any) => h(NSwitch, {
      value: row.enabled,
      onUpdateValue: (v: boolean) => toggleSkill(row.name, v)
    })
  }
]

async function loadSkills() {
  loading.value = true
  try {
    const res: any = await request({ url: '/ai/skills/list', method: 'get' })
    skills.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res: any = await request({ url: '/ai/skills/stats', method: 'get' })
  stats.value = (res && typeof res === 'object') ? res : {}
}

async function toggleSkill(name: string, enabled: boolean) {
  const url = enabled ? `/ai/skills/enable/${name}` : `/ai/skills/disable/${name}`
  await request({ url, method: 'post' })
  message.success(enabled ? '已启用' : '已禁用')
  loadSkills()
  loadStats()
}

onMounted(() => {
  loadSkills()
  loadStats()
})
</script>

<style scoped>
.skills-container { padding: 4px; }
.stat-card { text-align: center; }
.stat-card.enabled { border-left: 4px solid #18a058; }
.stat-card.disabled { border-left: 4px solid #d03050; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
</style>
