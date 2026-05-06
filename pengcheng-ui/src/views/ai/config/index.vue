<template>
  <div class="ai-config-page">
    <div class="page-header">
      <h2>AI 模型配置</h2>
      <p class="desc">管理 AI 模型参数、API 密钥和服务配置</p>
    </div>

    <n-tabs type="line" animated>
      <!-- 模型配置 -->
      <n-tab-pane name="models" tab="模型管理">
        <div class="config-section">
          <n-empty v-if="models.length === 0" description="暂无模型配置" style="margin: 40px 0" />
          <n-card v-for="model in models" :key="model.id" class="model-card" :bordered="true" hoverable>
            <div class="model-header">
              <div class="model-name">
                <n-icon size="20"><SparklesOutline /></n-icon>
                <span>{{ model.name }}</span>
                <n-tag :type="model.enabled ? 'success' : 'default'" size="small">
                  {{ model.enabled ? '启用' : '禁用' }}
                </n-tag>
              </div>
              <n-switch v-model:value="model.enabled" @update:value="toggleModel(model)" />
            </div>
            <div class="model-info">
              <div class="info-row">
                <span class="label">提供商</span>
                <span>{{ model.provider }}</span>
              </div>
              <div class="info-row">
                <span class="label">模型ID</span>
                <span class="mono">{{ model.modelId }}</span>
              </div>
              <div class="info-row">
                <span class="label">用途</span>
                <span>{{ model.usage }}</span>
              </div>
              <div class="info-row">
                <span class="label">温度</span>
                <n-slider v-model:value="model.temperature" :min="0" :max="1" :step="0.1" style="width: 200px" />
                <span class="value">{{ model.temperature }}</span>
              </div>
              <div class="info-row">
                <span class="label">最大Token</span>
                <n-input-number v-model:value="model.maxTokens" :min="100" :max="32000" :step="100" size="small" style="width: 150px" />
              </div>
            </div>
            <div class="model-footer">
              <n-button size="small" type="primary" @click="saveModel(model)">保存</n-button>
              <n-button size="small" @click="testModel(model)">测试连通</n-button>
            </div>
          </n-card>
        </div>
      </n-tab-pane>

      <!-- API 密钥 -->
      <n-tab-pane name="keys" tab="API 密钥">
        <n-alert type="warning" style="margin-bottom: 16px">
          API 密钥仅在设置时显示，保存后将加密存储。建议通过环境变量配置。
        </n-alert>
        <n-form label-placement="left" label-width="140">
          <n-form-item label="DashScope API Key">
            <n-input v-model:value="apiKeys.dashscope" type="password" show-password-on="click" placeholder="sk-xxx" style="width: 400px" />
          </n-form-item>
          <n-form-item label="智谱 API Key">
            <n-input v-model:value="apiKeys.zhipu" type="password" show-password-on="click" placeholder="xxx.xxx" style="width: 400px" />
          </n-form-item>
          <n-form-item>
            <n-button type="primary" @click="saveApiKeys">保存密钥</n-button>
          </n-form-item>
        </n-form>
      </n-tab-pane>

      <!-- 功能开关 -->
      <n-tab-pane name="features" tab="功能开关">
        <n-empty v-if="features.length === 0" description="暂无功能开关配置" style="margin: 40px 0" />
        <div v-else class="feature-list">
          <div v-for="feature in features" :key="feature.key" class="feature-item">
            <div class="feature-info">
              <div class="feature-name">{{ feature.name }}</div>
              <div class="feature-desc">{{ feature.description }}</div>
            </div>
            <n-switch v-model:value="feature.enabled" @update:value="toggleFeature(feature)" />
          </div>
        </div>
      </n-tab-pane>

      <!-- 使用统计 -->
      <n-tab-pane name="usage" tab="使用统计">
        <div class="usage-stats">
          <div class="usage-card">
            <div class="usage-title">本月 API 调用</div>
            <div class="usage-value">{{ usageStats.monthCalls.toLocaleString() }}</div>
            <div class="usage-sub">Token: {{ usageStats.monthTokens.toLocaleString() }}</div>
          </div>
          <div class="usage-card">
            <div class="usage-title">今日 API 调用</div>
            <div class="usage-value">{{ usageStats.todayCalls.toLocaleString() }}</div>
            <div class="usage-sub">Token: {{ usageStats.todayTokens.toLocaleString() }}</div>
          </div>
          <div class="usage-card">
            <div class="usage-title">向量库文档</div>
            <div class="usage-value">{{ usageStats.vectorDocs }}</div>
            <div class="usage-sub">向量条数: {{ usageStats.vectorChunks }}</div>
          </div>
          <div class="usage-card">
            <div class="usage-title">记忆总数</div>
            <div class="usage-value">{{ usageStats.memoryCount }}</div>
            <div class="usage-sub">其中 L2: {{ usageStats.memoryL2Count }}</div>
          </div>
        </div>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { NTabs, NTabPane, NCard, NSwitch, NSlider, NInputNumber, NButton, NInput, NForm, NFormItem, NTag, NIcon, NAlert, useMessage } from 'naive-ui'
import { SparklesOutline } from '@vicons/ionicons5'
import { request } from '@/utils/request'

const message = useMessage()

/** 模型列表（从后端加载，默认空数组） */
const models = ref<any[]>([])
/** 功能开关（从后端加载） */
const features = ref<any[]>([])
const apiKeys = reactive({
  dashscope: '',
  zhipu: ''
})

const usageStats = reactive({
  monthCalls: 0,
  monthTokens: 0,
  todayCalls: 0,
  todayTokens: 0,
  vectorDocs: 0,
  vectorChunks: 0,
  memoryCount: 0,
  memoryL2Count: 0
})

async function loadModels() {
  try {
    const res: any = await request({ url: '/ai/config/models', method: 'get' })
    if (Array.isArray(res)) models.value = res
  } catch {
    models.value = []
  }
}

async function loadFeatures() {
  try {
    const res: any = await request({ url: '/ai/config/features', method: 'get' })
    if (Array.isArray(res)) features.value = res
  } catch {
    features.value = []
  }
}

async function loadUsageStats() {
  try {
    const res: any = await request({ url: '/ai/config/usage-stats', method: 'get' })
    if (res && typeof res === 'object') Object.assign(usageStats, res)
  } catch { /* 静默 */ }
}

async function toggleModel(model: any) {
  try {
    await request({
      url: '/ai/config/models',
      method: 'put',
      data: models.value.map((m: any) => ({
        modelId: m.modelId,
        enabled: m.enabled,
        temperature: m.temperature,
        maxTokens: m.maxTokens
      }))
    })
    message.success(`${model.name} 已${model.enabled ? '启用' : '禁用'}`)
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
}

async function saveModel(model: any) {
  try {
    await request({
      url: '/ai/config/models',
      method: 'put',
      data: models.value.map((m: any) => ({
        modelId: m.modelId,
        enabled: m.enabled,
        temperature: m.temperature,
        maxTokens: m.maxTokens
      }))
    })
    message.success(`${model.name} 配置已保存`)
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
}

async function testModel(model: any) {
  const loadingMessage = message.loading(`正在测试 ${model.name} 连通性...`, { duration: 0 })
  try {
    const res: any = await request({
      url: '/ai/config/test-connection',
      method: 'post',
      data: { modelName: model.name }
    })
    loadingMessage.destroy()
    message.success(res?.message || `${model.name} 连接正常`)
  } catch (e: any) {
    loadingMessage.destroy()
    message.error(e?.message || '测试失败')
  }
}

async function saveApiKeys() {
  try {
    await request({
      url: '/ai/config/api-keys',
      method: 'put',
      data: { dashscope: apiKeys.dashscope, zhipu: apiKeys.zhipu }
    })
    message.success('API 密钥已保存（仅保存至服务器配置，重启后请确认环境变量已设置）')
  } catch {
    message.warning('当前版本请通过服务器环境变量 DASHSCOPE_API_KEY / ZHIPU_API_KEY 配置密钥')
  }
}

async function toggleFeature(feature: any) {
  try {
    await request({
      url: '/ai/config/features',
      method: 'put',
      data: features.value.map((f: any) => ({ key: f.key, enabled: f.enabled }))
    })
    message.success(`${feature.name} 已${feature.enabled ? '启用' : '禁用'}`)
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  }
}

onMounted(() => {
  loadModels()
  loadFeatures()
  loadUsageStats()
})
</script>

<style scoped>
.ai-config-page {
  padding: 20px;
}
.page-header h2 {
  margin: 0 0 4px;
}
.page-header .desc {
  color: #999;
  font-size: 13px;
  margin: 0 0 20px;
}
.config-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.model-card {
  border-radius: 12px;
}
.model-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.model-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}
.model-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.info-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.info-row .label {
  width: 80px;
  color: #999;
  font-size: 13px;
}
.info-row .value {
  font-size: 13px;
  color: #666;
  margin-left: 8px;
}
.mono {
  font-family: monospace;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
}
.model-footer {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.feature-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.feature-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
}
.feature-name {
  font-size: 14px;
  font-weight: 500;
}
.feature-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.usage-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
.usage-card {
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
  text-align: center;
}
.usage-title {
  font-size: 13px;
  color: #999;
  margin-bottom: 8px;
}
.usage-value {
  font-size: 28px;
  font-weight: 700;
  color: #333;
}
.usage-sub {
  font-size: 12px;
  color: #bbb;
  margin-top: 4px;
}

@media (max-width: 768px) {
  .usage-stats {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
