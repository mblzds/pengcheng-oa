<template>
  <div class="template-container">
    <n-card title="销售场景模板库">
      <template #header-extra>
        <n-radio-group v-model:value="filterCategory" size="small">
          <n-radio-button value="">全部</n-radio-button>
          <n-radio-button value="visit_memo">推介纪要</n-radio-button>
          <n-radio-button value="demand_analysis">需求分析</n-radio-button>
          <n-radio-button value="competitor">竞品对比</n-radio-button>
          <n-radio-button value="survey">踩盘报告</n-radio-button>
        </n-radio-group>
      </template>

      <n-empty v-if="filteredTemplates.length === 0" description="暂无模板数据" style="margin: 40px 0" />
      <n-grid v-else :cols="4" :x-gap="16" :y-gap="16">
        <n-gi v-for="tpl in filteredTemplates" :key="tpl.id">
          <n-card size="small" hoverable class="template-card" @click="openTemplate(tpl)">
            <div class="tpl-icon">{{ tpl.icon || '📋' }}</div>
            <div class="tpl-name">{{ tpl.name }}</div>
            <div class="tpl-desc">{{ tpl.description }}</div>
            <div class="tpl-meta">
              <n-tag size="tiny" :type="categoryType(tpl.category)">{{ categoryLabel(tpl.category) }}</n-tag>
              <span class="usage-count">使用 {{ tpl.usageCount || 0 }} 次</span>
            </div>
          </n-card>
        </n-gi>
      </n-grid>
    </n-card>

    <!-- 使用模板弹窗 -->
    <n-modal v-model:show="showFill" preset="card" :title="currentTemplate?.name || '填充模板'" style="width: 700px; max-height: 85vh">
      <n-scrollbar style="max-height: 65vh">
        <n-form v-if="currentTemplate" label-placement="top">
          <n-form-item v-for="field in currentTemplate.fields" :key="field.name" :label="field.label">
            <n-date-picker v-if="field.type === 'date'" v-model:value="fillData[field.name]" type="date" style="width: 100%" />
            <n-input v-else-if="field.type === 'textarea'" v-model:value="fillData[field.name]"
                     type="textarea" :placeholder="'输入' + field.label" :rows="3" />
            <n-select v-else-if="field.type === 'select'" v-model:value="fillData[field.name]"
                      :options="getSelectOptions(field.name)" clearable />
            <n-input v-else v-model:value="fillData[field.name]" :placeholder="'输入' + field.label" />
          </n-form-item>
        </n-form>
      </n-scrollbar>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showFill = false">取消</n-button>
          <n-button type="primary" :loading="filling" @click="fillTemplate">生成文档</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 生成结果弹窗 -->
    <n-modal v-model:show="showResult" preset="card" title="生成结果" style="width: 700px; max-height: 85vh">
      <n-scrollbar style="max-height: 65vh">
        <div class="result-content" v-html="renderedResult"></div>
      </n-scrollbar>
      <template #footer>
        <n-space justify="end">
          <n-button @click="copyResult">复制内容</n-button>
          <n-button type="primary" @click="showResult = false">关闭</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  NCard, NGrid, NGi, NTag, NButton, NSpace, NModal, NScrollbar,
  NForm, NFormItem, NInput, NSelect, NDatePicker, NRadioGroup, NRadioButton, useMessage
} from 'naive-ui'
import request from '@/utils/request'

const message = useMessage()

const templates = ref<any[]>([])
const filterCategory = ref('')
const showFill = ref(false)
const showResult = ref(false)
const currentTemplate = ref<any>(null)
const fillData = ref<Record<string, any>>({})
const filling = ref(false)
const resultContent = ref('')

const filteredTemplates = computed(() => {
  if (!filterCategory.value) return templates.value
  return templates.value.filter(t => t.category === filterCategory.value)
})

const categoryLabel = (cat: string) => {
  const map: Record<string, string> = { visit_memo: '推介纪要', demand_analysis: '需求分析', competitor: '竞品对比', survey: '踩盘报告' }
  return map[cat] || cat
}

const categoryType = (cat: string): any => {
  const map: Record<string, string> = { visit_memo: 'success', demand_analysis: 'info', competitor: 'warning', survey: 'default' }
  return map[cat] || 'default'
}

const renderedResult = computed(() => {
  return resultContent.value
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\|(.+)\|/g, (match) => '<div class="table-row">' + match + '</div>')
    .replace(/\n/g, '<br>')
})

const getSelectOptions = (name: string) => {
  const options: Record<string, any[]> = {
    visitType: [{ label: '自然到访', value: '自然到访' }, { label: '中介带看', value: '中介带看' }, { label: '老带新', value: '老带新' }, { label: '活动到访', value: '活动到访' }],
    intentLevel: [{ label: 'A-高意向', value: 'A-高意向' }, { label: 'B-较意向', value: 'B-较意向' }, { label: 'C-一般', value: 'C-一般' }, { label: 'D-低意向', value: 'D-低意向' }],
    purpose: [{ label: '自住刚需', value: '自住刚需' }, { label: '改善换房', value: '改善换房' }, { label: '投资', value: '投资' }, { label: '养老', value: '养老' }]
  }
  return options[name] || []
}

onMounted(() => loadTemplates())

async function loadTemplates() {
  try {
    const res = await request.get('/template/list')
    templates.value = Array.isArray(res) ? res : []
  } catch { templates.value = [] }
}

function openTemplate(tpl: any) {
  currentTemplate.value = tpl
  fillData.value = {}
  showFill.value = true
}

async function fillTemplate() {
  if (!currentTemplate.value) return
  filling.value = true
  const data: Record<string, string> = {}
  for (const [key, val] of Object.entries(fillData.value)) {
    if (val instanceof Date || typeof val === 'number') {
      data[key] = new Date(val as number).toLocaleDateString('zh-CN')
    } else {
      data[key] = String(val || '')
    }
  }
  try {
    const res: any = await request.post('/template/fill', { templateId: currentTemplate.value.id, data })
    resultContent.value = res?.content || (typeof res === 'string' ? res : '')
    showFill.value = false
    showResult.value = true
    message.success('文档已生成')
    loadTemplates()
  } catch {
    message.error('生成失败')
  } finally {
    filling.value = false
  }
}

function copyResult() {
  navigator.clipboard.writeText(resultContent.value).then(() => {
    message.success('已复制到剪贴板')
  }).catch(() => {
    message.error('复制失败')
  })
}
</script>

<style scoped>
.template-container { padding: 0; }
.template-card { cursor: pointer; text-align: center; transition: transform 0.2s, box-shadow 0.2s; }
.template-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.tpl-icon { font-size: 40px; margin-bottom: 8px; }
.tpl-name { font-size: 15px; font-weight: 600; margin-bottom: 4px; }
.tpl-desc { font-size: 12px; color: #999; margin-bottom: 8px; min-height: 32px; }
.tpl-meta { display: flex; align-items: center; justify-content: space-between; }
.usage-count { font-size: 11px; color: #bbb; }
.result-content { font-size: 14px; line-height: 1.8; }
.result-content :deep(h1) { font-size: 22px; margin: 16px 0 8px; }
.result-content :deep(h2) { font-size: 18px; margin: 12px 0 6px; }
.result-content :deep(h3) { font-size: 15px; margin: 8px 0 4px; }
.result-content :deep(li) { margin-left: 20px; }
.result-content :deep(strong) { color: #333; }
</style>
