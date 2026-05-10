<template>
  <div class="holiday-page">
    <n-card>
      <template #header>
        <n-space align="center">
          <span>节假日 / 调休补班日历</span>
          <n-text depth="3" style="font-size: 13px">
            维护「应当出勤工作日」的依据，影响考勤月报缺勤计算
          </n-text>
        </n-space>
      </template>

      <n-space style="margin-bottom: 12px">
        <n-input-number v-model:value="year" :min="2020" :max="2100" :show-button="false" style="width: 100px" />
        <n-button @click="loadList" :loading="loading">刷新</n-button>
        <n-button type="primary" @click="openCreate">+ 新增</n-button>
      </n-space>

      <n-data-table
        :columns="columns"
        :data="rows"
        :loading="loading"
        :row-key="(r: HolidayCalendarItem) => r.id!"
        size="small"
        :bordered="false"
      />
    </n-card>

    <n-modal
      v-model:show="formVisible"
      preset="card"
      :title="form.id ? '编辑节假日' : '新增节假日'"
      style="width: 460px"
      :mask-closable="false"
    >
      <n-form label-placement="left" :label-width="100">
        <n-form-item label="日期" required>
          <n-date-picker
            v-model:value="form.dateTs"
            type="date"
            format="yyyy-MM-dd"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item label="类型" required>
          <n-radio-group v-model:value="form.type">
            <n-radio :value="1">法定节假日（休）</n-radio>
            <n-radio :value="2">调休补班（上）</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="显示名" required>
          <n-input v-model:value="form.label" placeholder="如 元旦 / 国庆调休" :input-props="{ maxlength: 64 }" />
        </n-form-item>
        <n-form-item label="备注">
          <n-input v-model:value="form.note" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" placeholder="可选" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="onSave">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="tsx">
import { onMounted, reactive, ref, h } from 'vue'
import { NButton, NSpace, NTag, useDialog, useMessage, type DataTableColumns } from 'naive-ui'
import { holidayApi, type HolidayCalendarItem } from '@/api/holiday'

const message = useMessage()
const dialog = useDialog()

const year = ref<number>(new Date().getFullYear())
const rows = ref<HolidayCalendarItem[]>([])
const loading = ref(false)

const formVisible = ref(false)
const saving = ref(false)
const form = reactive<{ id?: number; dateTs: number | null; type: number; label: string; note?: string | null }>({
  id: undefined,
  dateTs: Date.now(),
  type: 1,
  label: '',
  note: ''
})

const columns: DataTableColumns<HolidayCalendarItem> = [
  { title: '日期', key: 'holidayDate', width: 120 },
  {
    title: '星期',
    key: '_weekday',
    width: 80,
    render: row => weekdayOf(row.holidayDate)
  },
  {
    title: '类型',
    key: 'type',
    width: 140,
    render: row => h(
      NTag,
      { type: row.type === 1 ? 'error' : 'success', size: 'small', bordered: false },
      { default: () => row.type === 1 ? '法定节假日（休）' : '调休补班（上）' }
    )
  },
  { title: '名称', key: 'label' },
  { title: '备注', key: 'note', render: row => row.note || '-' },
  {
    title: '操作',
    key: '_actions',
    width: 160,
    render: row => h(NSpace, { size: 8 }, {
      default: () => [
        h(NButton, { size: 'small', type: 'primary', text: true, onClick: () => openEdit(row) }, { default: () => '编辑' }),
        h(NButton, { size: 'small', type: 'error', text: true, onClick: () => confirmDelete(row) }, { default: () => '删除' })
      ]
    })
  }
]

function weekdayOf(date: string): string {
  if (!date) return ''
  const d = new Date(date + 'T00:00:00')
  const map = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return map[d.getDay()]
}

async function loadList() {
  loading.value = true
  try {
    rows.value = await holidayApi.list(year.value)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, { id: undefined, dateTs: Date.now(), type: 1, label: '', note: '' })
  formVisible.value = true
}

function openEdit(row: HolidayCalendarItem) {
  Object.assign(form, {
    id: row.id,
    dateTs: new Date(row.holidayDate + 'T00:00:00').getTime(),
    type: row.type,
    label: row.label,
    note: row.note || ''
  })
  formVisible.value = true
}

async function onSave() {
  if (!form.dateTs) return message.error('请选择日期')
  if (!form.label.trim()) return message.error('显示名不能为空')
  const date = new Date(form.dateTs)
  const isoDate = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  saving.value = true
  try {
    const payload: HolidayCalendarItem = {
      id: form.id,
      holidayDate: isoDate,
      type: form.type,
      label: form.label.trim(),
      note: form.note?.trim() || null
    }
    if (form.id) {
      await holidayApi.update(form.id, payload)
    } else {
      await holidayApi.save(payload)
    }
    message.success('已保存')
    formVisible.value = false
    await loadList()
  } catch (err: any) {
    message.error(err?.message || err?.msg || '保存失败')
  } finally {
    saving.value = false
  }
}

function confirmDelete(row: HolidayCalendarItem) {
  dialog.warning({
    title: '删除节假日',
    content: `确定删除「${row.label}（${row.holidayDate}）」？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await holidayApi.remove(row.id!)
        message.success('已删除')
        await loadList()
      } catch (err: any) {
        message.error(err?.message || err?.msg || '删除失败')
      }
    }
  })
}

onMounted(loadList)
</script>

<style scoped>
.holiday-page {
  padding: 16px;
}
</style>
