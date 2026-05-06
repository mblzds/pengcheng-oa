import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface ReceivablePlanRecord {
  id: number
  dealId: number
  periodNo: number
  periodName?: string
  dueDate: string
  dueAmount: number
  paidAmount: number
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ReceivableRecordRecord {
  id: number
  planId: number
  amount: number
  paidDate: string
  payWay: number
  payer?: string
  voucherNo?: string
  attachmentUrl?: string
  remark?: string
  createTime?: string
}

export interface ReceivableAlertRecord {
  id: number
  planId: number
  alertType: number
  alertTime?: string
  lastNotified?: string
  notifyCount?: number
  handled: number
  handledBy?: number
  handledAt?: string
  handledRemark?: string
  createTime?: string
}

export interface ReceivableStatsRecord {
  totalDue: number
  totalPaid: number
  totalUnpaid: number
  totalOverdue: number
  overdueCount: number
  totalCount: number
}

export interface ReceivablePlanCreateItem {
  periodNo: number | null
  periodName: string
  dueDate: string | null
  dueAmount: number | null
  remark?: string
}

export interface ReceivablePlanCreateParams {
  dealId: number | null
  items: ReceivablePlanCreateItem[]
}

export interface ReceivablePlanQueryParams {
  dealId?: number
  status?: number
  page?: number
  pageSize?: number
}

export interface ReceivableRecordCreateParams {
  planId: number
  amount: number
  paidDate: string | null
  payWay?: number
  payer?: string
  voucherNo?: string
  attachmentUrl?: string
  remark?: string
}

export const receivableApi = {
  createPlan(data: ReceivablePlanCreateParams) {
    return request<number[]>({
      url: '/admin/receivable/plan',
      method: 'post',
      data
    })
  },

  pagePlans(data: ReceivablePlanQueryParams) {
    return request<PageResult<ReceivablePlanRecord>>({
      url: '/admin/receivable/plan/page',
      method: 'post',
      data
    })
  },

  createRecord(data: ReceivableRecordCreateParams) {
    return request<number>({
      url: '/admin/receivable/record',
      method: 'post',
      data
    })
  },

  listRecords(planId: number) {
    return request<ReceivableRecordRecord[]>({
      url: `/admin/receivable/record/list/${planId}`,
      method: 'get'
    })
  },

  openAlerts() {
    return request<ReceivableAlertRecord[]>({
      url: '/admin/receivable/alert/open',
      method: 'get'
    })
  },

  stats() {
    return request<ReceivableStatsRecord>({
      url: '/admin/receivable/stats',
      method: 'get'
    })
  },

  runOverdueCheck() {
    return request<[number, number]>({
      url: '/admin/receivable/check/overdue',
      method: 'post'
    })
  }
}
