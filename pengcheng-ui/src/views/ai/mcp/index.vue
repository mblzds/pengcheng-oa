<template>
  <div class="mcp-container">
    <!-- 统计卡片 -->
    <n-grid :cols="3" :x-gap="16" style="margin-bottom: 16px">
      <n-gi>
        <n-card size="small" class="stat-card">
          <div class="stat-value">{{ tools.length }}</div>
          <div class="stat-label">MCP Tool 总数</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card enabled">
          <div class="stat-value">{{ enabledCount }}</div>
          <div class="stat-label">已启用</div>
        </n-card>
      </n-gi>
      <n-gi>
        <n-card size="small" class="stat-card disabled">
          <div class="stat-value">{{ tools.length - enabledCount }}</div>
          <div class="stat-label">已禁用</div>
        </n-card>
      </n-gi>
    </n-grid>

    <!-- 工具列表 -->
    <n-card title="MCP Tool 注册表">
      <template #header-extra>
        <n-tag size="small" type="info">遵循 Model Context Protocol 规范</n-tag>
      </template>

      <n-empty v-if="tools.length === 0" description="暂无 MCP 工具，请在后端注册 Tool" style="margin: 40px 0" />
      <div v-else class="tool-grid">
        <n-card v-for="tool in tools" :key="tool.name" size="small" class="tool-card" :class="{ disabled: !tool.enabled }">
          <div class="tool-header">
            <span class="tool-name">{{ tool.name }}</span>
            <n-switch :value="tool.enabled" size="small" @update:value="(v: boolean) => toggleTool(tool.name, v)" />
          </div>
          <div class="tool-desc">{{ tool.description }}</div>

          <n-collapse :default-expanded-names="[]" style="margin-top: 8px">
            <n-collapse-item title="输入参数 Schema" name="schema">
              <n-code :code="formatSchema(tool.inputSchema)" language="json" />
            </n-collapse-item>
          </n-collapse>

          <div class="tool-actions" style="margin-top: 8px">
            <n-button size="tiny" @click="openTest(tool)">测试执行</n-button>
          </div>
        </n-card>
      </div>
    </n-card>

    <!-- 测试执行弹窗 -->
    <n-modal v-model:show="showTest" preset="card" :title="'测试: ' + testTool?.name" style="width: 600px">
      <n-form label-placement="left" label-width="100px">
        <n-form-item v-for="(prop, key) in testProps" :key="key" :label="String(key)">
          <n-input v-model:value="testArgs[key as string]" :placeholder="prop.description || ''" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showTest = false">关闭</n-button>
          <n-button type="primary" :loading="testing" @click="executeTest">执行</n-button>
        </n-space>
      </template>
      <template v-if="testResult">
        <n-divider />
        <n-alert :type="testResult.isError ? 'error' : 'success'" :title="testResult.isError ? '执行失败' : '执行成功'">
          <pre class="test-output">{{ testResult.content?.map((c: any) => c.text).join('\n') }}</pre>
        </n-alert>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessage } from 'naive-ui'
import { request } from '@/utils/request'

const message = useMessage()
const tools = ref<any[]>([])
const showTest = ref(false)
const testTool = ref<any>(null)
const testArgs = ref<any>({})
const testProps = ref<any>({})
const testResult = ref<any>(null)
const testing = ref(false)

const enabledCount = computed(() => tools.value.filter(t => t.enabled).length)

async function loadTools() {
  try {
    const res: any = await request({ url: '/mcp/tools', method: 'get' })
    tools.value = Array.isArray(res) ? res : []
  } catch { tools.value = [] }
}

async function toggleTool(name: string, enabled: boolean) {
  const url = enabled ? `/mcp/tools/${name}/enable` : `/mcp/tools/${name}/disable`
  await request({ url, method: 'post' })
  message.success(enabled ? '已启用' : '已禁用')
  loadTools()
}

function formatSchema(schema: any) {
  try {
    return JSON.stringify(schema, null, 2)
  } catch {
    return '{}'
  }
}

function openTest(tool: any) {
  testTool.value = tool
  testArgs.value = {}
  testResult.value = null
  testProps.value = tool.inputSchema?.properties || {}
  for (const key of Object.keys(testProps.value)) {
    testArgs.value[key] = ''
  }
  showTest.value = true
}

async function executeTest() {
  if (!testTool.value) return
  testing.value = true
  testResult.value = null
  try {
    const args: any = {}
    for (const [k, v] of Object.entries(testArgs.value)) {
      if (v === '') continue
      const prop = testProps.value[k]
      if (prop?.type === 'integer') {
        args[k] = parseInt(v as string)
      } else {
        args[k] = v
      }
    }
    const res: any = await request({
      url: `/mcp/tools/${testTool.value.name}/execute`,
      method: 'post',
      data: args
    })
    testResult.value = res
  } catch (e: any) {
    testResult.value = { isError: true, content: [{ text: e.message || '执行失败' }] }
  } finally {
    testing.value = false
  }
}

onMounted(() => loadTools())
</script>

<style scoped>
.mcp-container { padding: 4px; }
.stat-card { text-align: center; }
.stat-card.enabled { border-left: 4px solid #18a058; }
.stat-card.disabled { border-left: 4px solid #d03050; }
.stat-value { font-size: 28px; font-weight: 700; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
.tool-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 12px; }
.tool-card { transition: opacity 0.2s; }
.tool-card.disabled { opacity: 0.5; }
.tool-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.tool-name { font-weight: 600; font-size: 15px; font-family: monospace; }
.tool-desc { font-size: 13px; color: #666; line-height: 1.5; }
.test-output { margin: 0; white-space: pre-wrap; font-size: 13px; max-height: 300px; overflow-y: auto; }
</style>
