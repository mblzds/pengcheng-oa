<template>
  <div class="template-mgmt-container">
    <n-card title="智能表格模板管理">
      <template #header-extra>
        <n-space>
          <n-radio-group v-model:value="filterCategory" size="small">
            <n-radio-button value="">全部</n-radio-button>
            <n-radio-button value="realty">房产</n-radio-button>
            <n-radio-button value="sales">销售</n-radio-button>
            <n-radio-button value="finance">财务</n-radio-button>
            <n-radio-button value="general">通用</n-radio-button>
          </n-radio-group>
          <n-button type="primary" @click="openCreate">+ 创建模板</n-button>
          <n-upload :show-file-list="false" accept=".json" :custom-request="handleImport">
            <n-button>导入模板</n-button>
          </n-upload>
        </n-space>
      </template>

      <n-grid :cols="4" :x-gap="16" :y-gap="16" responsive="screen" :collapsed-rows="100">
        <n-gi v-for="tpl in filteredTemplates" :key="tpl.id">
          <n-card size="small" hoverable class="tpl-card">
            <div class="tpl-header">
              <span class="tpl-icon">{{ tpl.icon || '📊' }}</span>
              <n-tag v-if="tpl.builtIn" size="tiny" type="info">内置</n-tag>
            </div>
            <div class="tpl-name">{{ tpl.name }}</div>
            <div class="tpl-desc">{{ tpl.description }}</div>
            <div class="tpl-fields">
              <n-tag v-for="f in (tpl.fieldsConfig || []).slice(0, 4)" :key="f.field_key" size="tiny" round>
                {{ f.name }}
              </n-tag>
              <n-tag v-if="(tpl.fieldsConfig || []).length > 4" size="tiny" round type="default">
                +{{ (tpl.fieldsConfig || []).length - 4 }}
              </n-tag>
            </div>
            <div class="tpl-footer">
              <span class="usage">使用 {{ tpl.usageCount || 0 }} 次</span>
              <n-space :size="4">
                <n-button text size="tiny" type="primary" @click="useTemplate(tpl)">使用</n-button>
                <n-button text size="tiny" @click="exportTemplate(tpl)">导出</n-button>
                <n-button text size="tiny" @click="openEdit(tpl)" v-if="!tpl.builtIn">编辑</n-button>
                <n-button text size="tiny" type="error" @click="handleDelete(tpl)" v-if="!tpl.builtIn">删除</n-button>
              </n-space>
            </div>
          </n-card>
        </n-gi>
      </n-grid>

      <n-empty v-if="filteredTemplates.length === 0" description="暂无模板" />
    </n-card>

    <!-- 创建/编辑模板弹窗 -->
    <n-modal v-model:show="showEditor" preset="card" :title="editMode === 'create' ? '创建模板' : '编辑模板'" style="width: 800px; max-height: 85vh">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="80">
        <n-form-item label="模板名称" path="name">
          <n-input v-model:value="formData.name" placeholder="输入模板名称" />
        </n-form-item>
        <n-form-item label="描述">
          <n-input v-model:value="formData.description" type="textarea" placeholder="模板用途说明" :rows="2" />
        </n-form-item>
        <n-grid :cols="2" :x-gap="16">
          <n-gi>
            <n-form-item label="分类">
              <n-select v-model:value="formData.category" :options="categoryOptions" />
            </n-form-item>
          </n-gi>
          <n-gi>
            <n-form-item label="图标">
              <n-input v-model:value="formData.icon" placeholder="Emoji 图标" style="width: 80px" />
            </n-form-item>
          </n-gi>
        </n-grid>

        <n-divider>字段配置</n-divider>

        <div class="field-list">
          <div v-for="(field, idx) in formData.fieldsConfig" :key="idx" class="field-row">
            <n-input v-model:value="field.name" placeholder="字段名" style="width: 120px" />
            <n-input v-model:value="field.field_key" placeholder="字段标识" style="width: 120px" />
            <n-select v-model:value="field.field_type" :options="fieldTypeOptions" style="width: 110px" />
            <n-checkbox v-model:checked="field.required">必填</n-checkbox>
            <n-button text type="error" @click="formData.fieldsConfig.splice(idx, 1)">删除</n-button>
          </div>
          <n-button dashed block @click="addField">+ 添加字段</n-button>
        </div>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="showEditor = false">取消</n-button>
          <n-button type="primary" @click="handleSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 从模板创建表格弹窗 -->
    <n-modal v-model:show="showUse" preset="card" title="从模板创建表格" style="width: 500px">
      <n-form label-placement="left" label-width="80">
        <n-form-item label="模板">
          <n-tag>{{ useTarget?.icon }} {{ useTarget?.name }}</n-tag>
        </n-form-item>
        <n-form-item label="表格名称">
          <n-input v-model:value="newTableName" placeholder="输入新表格名称" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="showUse = false">取消</n-button>
          <n-button type="primary" :loading="creating" @click="handleCreateFromTemplate">创建</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessage, useDialog } from 'naive-ui'
import { smartTableApi } from '@/api/smartTable'

const message = useMessage()
const dialog = useDialog()

const templates = ref<any[]>([])
const filterCategory = ref('')
const showEditor = ref(false)
const editMode = ref<'create' | 'edit'>('create')
const showUse = ref(false)
const useTarget = ref<any>(null)
const newTableName = ref('')
const creating = ref(false)

const categoryOptions = [
  { label: '房产', value: 'realty' },
  { label: '销售', value: 'sales' },
  { label: '财务', value: 'finance' },
  { label: '通用', value: 'general' }
]

const fieldTypeOptions = [
  { label: '文本', value: 'text' },
  { label: '数字', value: 'number' },
  { label: '日期', value: 'date' },
  { label: '日期时间', value: 'datetime' },
  { label: '选择', value: 'select' },
  { label: '复选框', value: 'checkbox' },
  { label: '评分', value: 'rating' },
  { label: '进度', value: 'progress' },
  { label: '成员', value: 'member' },
  { label: '电话', value: 'phone' },
  { label: '邮箱', value: 'email' },
  { label: '链接', value: 'url' }
]

const formData = ref<any>({
  name: '', description: '', category: 'realty', icon: '📊',
  fieldsConfig: [{ name: '', field_key: '', field_type: 'text', required: false }]
})

const filteredTemplates = computed(() =>
  filterCategory.value ? templates.value.filter(t => t.category === filterCategory.value) : templates.value
)

async function loadTemplates() {
  const res = await smartTableApi.listTemplates()
  templates.value = (res as any).data || []
}

function openCreate() {
  editMode.value = 'create'
  formData.value = {
    name: '', description: '', category: 'realty', icon: '📊',
    fieldsConfig: [{ name: '', field_key: '', field_type: 'text', required: false }]
  }
  showEditor.value = true
}

function openEdit(tpl: any) {
  editMode.value = 'edit'
  formData.value = { ...tpl, fieldsConfig: [...(tpl.fieldsConfig || [])] }
  showEditor.value = true
}

function addField() {
  formData.value.fieldsConfig.push({ name: '', field_key: '', field_type: 'text', required: false })
}

async function handleSave() {
  if (!formData.value.name) {
    message.warning('请输入模板名称')
    return
  }
  const validFields = formData.value.fieldsConfig.filter((f: any) => f.name && f.field_key)
  if (validFields.length === 0) {
    message.warning('至少配置一个有效字段')
    return
  }
  formData.value.fieldsConfig = validFields

  if (editMode.value === 'create') {
    await smartTableApi.createTemplate(formData.value)
    message.success('模板创建成功')
  } else {
    await smartTableApi.updateTemplate(formData.value)
    message.success('模板更新成功')
  }
  showEditor.value = false
  loadTemplates()
}

function handleDelete(tpl: any) {
  dialog.warning({
    title: '确认删除',
    content: `确定删除模板「${tpl.name}」吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      await smartTableApi.deleteTemplate(tpl.id)
      message.success('已删除')
      loadTemplates()
    }
  })
}

function useTemplate(tpl: any) {
  useTarget.value = tpl
  newTableName.value = tpl.name
  showUse.value = true
}

async function handleCreateFromTemplate() {
  if (!newTableName.value) {
    message.warning('请输入表格名称')
    return
  }
  creating.value = true
  try {
    await smartTableApi.createFromTemplate(useTarget.value.id, newTableName.value)
    message.success('表格创建成功，可前往智能表格页面查看')
    showUse.value = false
  } finally {
    creating.value = false
  }
}

function exportTemplate(tpl: any) {
  const data = { name: tpl.name, description: tpl.description, category: tpl.category, icon: tpl.icon, fieldsConfig: tpl.fieldsConfig, sampleData: tpl.sampleData }
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `template-${tpl.name}.json`
  a.click()
  URL.revokeObjectURL(url)
  message.success('模板已导出')
}

function handleImport({ file }: any) {
  const reader = new FileReader()
  reader.onload = async (e) => {
    try {
      const data = JSON.parse(e.target?.result as string)
      if (!data.name || !data.fieldsConfig) {
        message.error('模板文件格式不正确')
        return
      }
      await smartTableApi.createTemplate(data)
      message.success('模板导入成功')
      loadTemplates()
    } catch {
      message.error('解析模板文件失败')
    }
  }
  reader.readAsText(file.file)
}

onMounted(loadTemplates)
</script>

<style scoped>
.template-mgmt-container { padding: 4px; }
.tpl-card { cursor: default; transition: box-shadow 0.2s; }
.tpl-card:hover { box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1); }
.tpl-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.tpl-icon { font-size: 28px; }
.tpl-name { font-weight: 600; font-size: 14px; margin-bottom: 4px; }
.tpl-desc { color: #999; font-size: 12px; margin-bottom: 8px; min-height: 32px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.tpl-fields { display: flex; flex-wrap: wrap; gap: 4px; margin-bottom: 8px; }
.tpl-footer { display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #f0f0f0; padding-top: 8px; }
.usage { font-size: 11px; color: #bbb; }
.field-list { display: flex; flex-direction: column; gap: 8px; }
.field-row { display: flex; align-items: center; gap: 8px; }
</style>
