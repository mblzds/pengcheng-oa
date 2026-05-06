<template>
  <n-space vertical :size="16">
    <n-card size="small" title="当前分期">
      <n-empty v-if="!selectedPlan" description="请先在“计划列表”中选择一条回款分期" />
      <n-descriptions v-else :column="2" bordered>
        <n-descriptions-item label="分期ID">{{ selectedPlan.id }}</n-descriptions-item>
        <n-descriptions-item label="成交ID">{{ selectedPlan.dealId }}</n-descriptions-item>
        <n-descriptions-item label="分期">{{ selectedPlan.periodName || `第 ${selectedPlan.periodNo} 期` }}</n-descriptions-item>
        <n-descriptions-item label="应回日期">{{ selectedPlan.dueDate }}</n-descriptions-item>
        <n-descriptions-item label="应回金额">{{ currency(selectedPlan.dueAmount) }}</n-descriptions-item>
        <n-descriptions-item label="已回金额">{{ currency(selectedPlan.paidAmount) }}</n-descriptions-item>
        <n-descriptions-item label="备注" :span="2">{{ selectedPlan.remark || '-' }}</n-descriptions-item>
      </n-descriptions>
    </n-card>

    <n-card size="small" title="登记到账流水">
      <n-empty v-if="!selectedPlan" description="选择分期后即可登记到账" />
      <n-form v-else label-placement="left" label-width="90">
        <n-grid :cols="24" :x-gap="16">
          <n-gi :span="8">
            <n-form-item label="到账金额">
              <n-input-number v-model:value="form.amount" :min="0" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi :span="8">
            <n-form-item label="到账日期">
              <n-date-picker
                v-model:value="form.paidDate"
                type="date"
                value-format="yyyy-MM-dd"
                clearable
                style="width: 100%"
              />
            </n-form-item>
          </n-gi>
          <n-gi :span="8">
            <n-form-item label="回款方式">
              <n-select v-model:value="form.payWay" :options="payWayOptions" style="width: 100%" />
            </n-form-item>
          </n-gi>
          <n-gi :span="8">
            <n-form-item label="付款方">
              <n-input v-model:value="form.payer" placeholder="付款方名称" />
            </n-form-item>
          </n-gi>
          <n-gi :span="8">
            <n-form-item label="凭证号">
              <n-input v-model:value="form.voucherNo" placeholder="流水号/凭证号" />
            </n-form-item>
          </n-gi>
          <n-gi :span="8">
            <n-form-item label="附件地址">
              <n-input v-model:value="form.attachmentUrl" placeholder="附件 URL" />
            </n-form-item>
          </n-gi>
          <n-gi :span="24">
            <n-form-item label="备注">
              <n-input v-model:value="form.remark" type="textarea" :rows="2" placeholder="可填写到账说明" />
            </n-form-item>
          </n-gi>
          <n-gi :span="24">
            <n-space justify="end">
              <n-button tertiary @click="$emit('refresh')">刷新流水</n-button>
              <n-button type="primary" :loading="submitting" @click="handleSubmit">登记到账</n-button>
            </n-space>
          </n-gi>
        </n-grid>
      </n-form>
    </n-card>

    <n-card size="small" title="到账流水记录">
      <n-data-table :columns="columns" :data="records" :loading="loading" :pagination="false" />
    </n-card>
  </n-space>
</template>

<script setup lang="ts">
import { h, reactive, watch } from 'vue'
import { NTag, type DataTableColumns } from 'naive-ui'
import type { ReceivablePlanRecord, ReceivableRecordCreateParams, ReceivableRecordRecord } from '@/api/receivable'

const props = defineProps<{
  selectedPlan: ReceivablePlanRecord | null
  records: ReceivableRecordRecord[]
  loading: boolean
  submitting: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: ReceivableRecordCreateParams): void
  (e: 'refresh'): void
}>()

const form = reactive({
  amount: null as number | null,
  paidDate: null as string | null,
  payWay: 1,
  payer: '',
  voucherNo: '',
  attachmentUrl: '',
  remark: ''
})

const payWayOptions = [
  { label: '银行转账', value: 1 },
  { label: '支票', value: 2 },
  { label: '现金', value: 3 },
  { label: '承兑', value: 4 },
  { label: '其他', value: 5 }
]

const payWayMap: Record<number, string> = {
  1: '银行转账',
  2: '支票',
  3: '现金',
  4: '承兑',
  5: '其他'
}

const columns: DataTableColumns<ReceivableRecordRecord> = [
  { title: '流水ID', key: 'id', width: 90 },
  { title: '到账日期', key: 'paidDate', width: 120 },
  {
    title: '到账金额',
    key: 'amount',
    width: 140,
    render: row => currency(row.amount)
  },
  {
    title: '回款方式',
    key: 'payWay',
    width: 120,
    render: row =>
      h(NTag, { size: 'small', type: 'info' }, {
        default: () => payWayMap[row.payWay] || '未知'
      })
  },
  { title: '付款方', key: 'payer', width: 160 },
  { title: '凭证号', key: 'voucherNo', width: 160 },
  { title: '备注', key: 'remark', minWidth: 180 }
]

watch(
  () => props.selectedPlan?.id,
  () => {
    form.amount = null
    form.paidDate = null
    form.payWay = 1
    form.payer = ''
    form.voucherNo = ''
    form.attachmentUrl = ''
    form.remark = ''
  }
)

function handleSubmit() {
  if (!props.selectedPlan || !form.amount) {
    return
  }
  emit('submit', {
    planId: props.selectedPlan.id,
    amount: form.amount,
    paidDate: form.paidDate,
    payWay: form.payWay,
    payer: form.payer || undefined,
    voucherNo: form.voucherNo || undefined,
    attachmentUrl: form.attachmentUrl || undefined,
    remark: form.remark || undefined
  })
}

function currency(value?: number) {
  return `¥${Number(value ?? 0).toLocaleString()}`
}
</script>
