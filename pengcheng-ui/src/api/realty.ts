import { request } from '@/utils/request'

export interface PageResult<T> {
  list: T[]
  total: number
  page: number
  pageSize: number
}

export interface ProjectOption {
  id: number
  projectName: string
}

export interface AllianceOption {
  id: number
  companyName: string
  status?: number
}

export interface CustomerRecord {
  id: number
  reportNo?: string
  customerName?: string
  phoneMasked?: string
  visitCount?: number
  visitTime?: string
  allianceId?: number
  allianceName?: string
  agentName?: string
  agentPhoneMasked?: string
  status?: number
  poolType?: number
  protectionExpireTime?: string
  lastFollowTime?: string
  dealProbability?: number
  createTime?: string
}

export interface CustomerQueryParams {
  page: number
  pageSize: number
  customerName?: string
  projectId?: number
  allianceId?: number
  status?: number
  startTime?: string
  endTime?: string
}

export interface CustomerCreateParams {
  projectIds: number[]
  customerName: string
  phone: string
  visitCount: number
  visitTime: string
  allianceId: number
  agentName: string
  agentPhone: string
}

export interface CustomerCreateResult {
  customerId: number
  reportNo: string
  hasDuplicate?: boolean
  analysisMessage?: string
}

export interface CustomerVisitRecord {
  id: number
  customerId: number
  actualVisitTime: string
  actualVisitCount: number
  receptionist: string
  remark?: string
  createTime?: string
}

export interface CustomerDealRecord {
  id: number
  customerId: number
  roomNo: string
  dealAmount: number
  dealTime: string
  signStatus: number
  subscribeType: number
  onlineSignStatus: number
  filingStatus: number
  loanStatus: number
  paymentStatus: number
  createTime?: string
}

export interface AllianceRecord {
  id: number
  companyName: string
  officeAddress?: string
  contactName?: string
  contactPhone?: string
  staffSize?: number
  level?: number
  status?: number
  userId?: number
  channelUserId?: number
  createTime?: string
  updateTime?: string
}

export interface AllianceQueryParams {
  page: number
  pageSize: number
  companyName?: string
  status?: number
  level?: number
}

export interface AllianceCreateParams {
  id?: number
  companyName: string
  officeAddress: string
  contactName: string
  contactPhone: string
  staffSize: number
  level: number
  channelUserId?: number
}

export interface AllianceStatsRecord {
  allianceId: number
  companyName: string
  customerCount?: number
  dealCount?: number
  dealAmount?: number
  settledCommission?: number
  pendingCommission?: number
  transportExpense?: number
  promotionExpense?: number
  entertainmentExpense?: number
  channelVisitCount?: number
}

export interface CommissionDetailRecord {
  baseCommission?: number
  jumpPointCommission?: number
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
}

export interface CommissionRecord {
  id: number
  dealId: number
  projectId: number
  allianceId: number
  receivableAmount: number
  payableAmount: number
  platformFee: number
  auditStatus: number
  auditRemark?: string
  auditorId?: number
  auditTime?: string
  createTime?: string
  detail?: CommissionDetailRecord
}

export interface CommissionQueryParams {
  page: number
  pageSize: number
  projectId?: number
  allianceId?: number
  auditStatus?: number
}

export interface CommissionCreateParams {
  dealId: number
  projectId: number
  allianceId: number
  receivableAmount: number
  payableAmount: number
  platformFee: number
  detail?: CommissionDetailRecord
}

export interface CommissionAuditParams {
  commissionId: number
  approved: boolean
  remark?: string
  auditorId?: number
}

export interface CommissionChangeLogRecord {
  id: number
  commissionId: number
  fieldName: string
  oldValue?: string
  newValue?: string
  operatorId?: number
  changeTime?: string
}

export interface AttendanceRecordItem {
  id: number
  userId: number
  attendanceDate: string
  clockInTime?: string
  clockInLocation?: string
  clockInStatus?: number
  clockOutTime?: string
  clockOutLocation?: string
  clockOutStatus?: number
}

export interface LeaveRequestItem {
  id: number
  userId: number
  leaveType: number
  startTime: string
  endTime: string
  reason?: string
  status: number
  createTime?: string
}

export interface CompensateRequestItem {
  id: number
  userId: number
  compensateDate: string
  reason?: string
  status: number
  createTime?: string
}

export interface AttendanceMonthlyRecord {
  userId: number
  year: number
  month: number
  attendanceDays: number
  lateTimes: number
  earlyLeaveTimes: number
  leaveDays: number
  overtimeHours: number
}

export interface PaymentRecord {
  id: number
  applicantId: number
  requestType: number
  expenseType?: number
  amount: number
  description?: string
  relatedDealId?: number
  relatedAllianceId?: number
  attachments?: string
  status: number
  createTime?: string
  approvals?: PaymentApprovalRecord[]
}

export interface PaymentApprovalRecord {
  id: number
  requestId: number
  approverId: number
  result: number
  remark?: string
  approvalOrder?: number
  approvalTime?: string
  createTime?: string
  updateTime?: string
}

export interface PaymentQueryParams {
  page: number
  pageSize: number
  requestType?: number
  status?: number
  applicantId?: number
}

export interface PaymentCreateParams {
  applicantId: number
  requestType: number
  expenseType?: number
  amount: number
  description?: string
  relatedDealId?: number
  relatedAllianceId?: number
  attachments?: string
  occurTime?: string
}

export interface PaymentApproveParams {
  requestId: number
  approverId: number
  approved: boolean
  remark?: string
}

export interface ProjectRecord {
  id: number
  projectName: string
  developerName?: string
  address?: string
  projectType?: number
  status?: number
  district?: string
  agencyStartDate?: string
  agencyEndDate?: string
  contactPerson?: string
  contactPhone?: string
  description?: string
  createTime?: string
  updateTime?: string
  commissionRule?: ProjectCommissionRuleRecord
}

export interface ProjectQueryParams {
  page: number
  pageSize: number
  projectName?: string
  district?: string
  projectType?: number
  status?: number
}

export interface ProjectCreateParams {
  id?: number
  projectName: string
  developerName?: string
  address?: string
  projectType?: number
  status?: number
  district?: string
  agencyStartDate?: string
  agencyEndDate?: string
  contactPerson?: string
  contactPhone?: string
  description?: string
}

export interface ProjectCommissionRuleCreateParams {
  projectId: number
  baseRate?: number
  jumpPointRules?: string
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
}

export interface ProjectCommissionRuleRecord {
  id: number
  projectId: number
  baseRate?: number
  jumpPointRules?: string
  cashReward?: number
  firstDealReward?: number
  platformReward?: number
  version?: number
  status?: number
  createTime?: string
}

export interface DashboardOverviewRecord {
  reportCount: number
  visitCount: number
  dealCount: number
  dealAmount: number
  receivableCommission: number
  settledCommission: number
}

export interface DashboardFunnelRecord {
  reportCount: number
  visitCount: number
  dealCount: number
  reportToVisitRate: number
  visitToDealRate: number
  reportToDealRate: number
}

export interface DashboardRankingRecord {
  projectRanking: Array<{
    projectId: number
    projectName: string
    dealCount: number
    dealAmount: number
  }>
  allianceRanking: Array<{
    allianceId: number
    companyName: string
    customerCount: number
    dealCount: number
  }>
}

export interface AiChatRecord {
  content: string
  displayType: 'table' | 'chart' | 'text'
  conversationId?: string
  routedAgent?: string
  structuredData?: Record<string, unknown>
}

export interface AiKnowledgeDocRecord {
  id: number
  fileName?: string
  projectId?: number
  status?: string
  uploadTime?: string
}

// 房产业务真实后端接口（交付标准：全部对接 /admin/customer、/admin/project、/admin/alliance、/admin/commission、/admin/payment、/admin/dashboard）
export const realtyApi = {
  // ---------- 客户 ----------
  customerPage(params: CustomerQueryParams) {
    return request<PageResult<CustomerRecord>>({
      url: '/admin/customer/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        customerName: params.customerName,
        projectId: params.projectId,
        allianceId: params.allianceId,
        status: params.status,
        startTime: params.startTime,
        endTime: params.endTime
      }
    })
  },

  createCustomer(data: CustomerCreateParams) {
    return request<CustomerCreateResult>({
      url: '/admin/customer/create',
      method: 'post',
      data
    })
  },

  searchProjects(keyword?: string) {
    return request<ProjectOption[]>({
      url: '/admin/customer/project/search',
      method: 'get',
      params: { keyword: keyword || '' }
    })
  },

  searchAlliances(keyword?: string) {
    return request<AllianceOption[]>({
      url: '/admin/customer/alliance/search',
      method: 'get',
      params: { keyword: keyword || '' }
    })
  },

  customerVisits(customerId: number) {
    return request<CustomerVisitRecord[]>({
      url: '/admin/customer/visit/list',
      method: 'get',
      params: { customerId }
    })
  },

  customerDeals(customerId: number) {
    return request<CustomerDealRecord[]>({
      url: '/admin/customer/deal/list',
      method: 'get',
      params: { customerId }
    })
  },

  customerDealProbability(customerId: number) {
    return request<number>({
      url: '/admin/ai/analysis/probability',
      method: 'get',
      params: { customerId }
    }).catch(() => 0)
  },

  // ---------- 联盟商 ----------
  alliancePage(params: AllianceQueryParams) {
    return request<PageResult<AllianceRecord>>({
      url: '/admin/alliance/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        companyName: params.companyName,
        status: params.status,
        level: params.level
      }
    })
  },

  allianceDetail(id: number) {
    return request<AllianceRecord>({
      url: `/admin/alliance/${id}`,
      method: 'get'
    })
  },

  allianceCreate(data: AllianceCreateParams) {
    return request<number>({
      url: '/admin/alliance/create',
      method: 'post',
      data: {
        companyName: data.companyName,
        officeAddress: data.officeAddress,
        contactName: data.contactName,
        contactPhone: data.contactPhone,
        staffSize: data.staffSize,
        level: data.level,
        channelUserId: data.channelUserId
      }
    })
  },

  allianceUpdate(data: AllianceCreateParams & { id?: number }) {
    return request<void>({
      url: '/admin/alliance/update',
      method: 'put',
      data
    })
  },

  allianceEnable(id: number) {
    return request<void>({
      url: `/admin/alliance/enable/${id}`,
      method: 'post'
    })
  },

  allianceDisable(id: number) {
    return request<void>({
      url: `/admin/alliance/disable/${id}`,
      method: 'post'
    })
  },

  allianceStats(id: number) {
    return request<AllianceStatsRecord>({
      url: `/admin/alliance/stats/${id}`,
      method: 'get'
    })
  },

  // ---------- 佣金 ----------
  commissionPage(params: CommissionQueryParams) {
    return request<PageResult<CommissionRecord>>({
      url: '/admin/commission/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        projectId: params.projectId,
        allianceId: params.allianceId,
        auditStatus: params.auditStatus
      }
    })
  },

  commissionCreate(data: CommissionCreateParams) {
    return request<number>({
      url: '/admin/commission/create',
      method: 'post',
      data
    })
  },

  commissionAudit(data: CommissionAuditParams) {
    return request<void>({
      url: '/admin/commission/audit',
      method: 'post',
      data
    })
  },

  commissionChangeLog(commissionId: number) {
    return request<CommissionChangeLogRecord[]>({
      url: '/admin/commission/changelog',
      method: 'get',
      params: { commissionId }
    })
  },

  // ---------- 考勤（对接 /admin/attendance） ----------
  attendanceRecords(params: { userId?: number; startDate?: string; endDate?: string }) {
    return request<AttendanceRecordItem[]>({
      url: '/admin/attendance/records',
      method: 'get',
      params: { userId: params.userId, startDate: params.startDate, endDate: params.endDate }
    }).catch(() => [])
  },

  attendanceMonthly(params: { userId: number; year: number; month: number }) {
    return request<AttendanceMonthlyRecord>({
      url: '/admin/attendance/monthly',
      method: 'get',
      params
    }).catch(() => ({
      userId: params.userId,
      year: params.year,
      month: params.month,
      attendanceDays: 0,
      lateTimes: 0,
      earlyLeaveTimes: 0,
      leaveDays: 0,
      overtimeHours: 0
    }))
  },

  leaveRequestList(params: { userId?: number; status?: number }) {
    return request<LeaveRequestItem[]>({
      url: '/admin/attendance/leave/list',
      method: 'get',
      params: { userId: params.userId, status: params.status }
    }).catch(() => [])
  },

  compensateRequestList(params: { userId?: number; status?: number }) {
    return request<CompensateRequestItem[]>({
      url: '/admin/attendance/compensate/list',
      method: 'get',
      params: { userId: params.userId, status: params.status }
    }).catch(() => [])
  },

  // ---------- 付款审批 ----------
  paymentPage(params: PaymentQueryParams) {
    return request<PageResult<PaymentRecord>>({
      url: '/admin/payment/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        requestType: params.requestType,
        status: params.status,
        applicantId: params.applicantId
      }
    })
  },

  paymentCreate(data: PaymentCreateParams) {
    return request<number>({
      url: '/admin/payment/create',
      method: 'post',
      data
    })
  },

  paymentApprove(data: PaymentApproveParams) {
    return request<void>({
      url: '/admin/payment/approve',
      method: 'post',
      data: {
        requestId: data.requestId,
        approverId: data.approverId,
        approved: data.approved,
        remark: data.remark
      }
    })
  },

  paymentApprovals(requestId: number) {
    return request<PaymentApprovalRecord[]>({
      url: '/admin/payment/approvals',
      method: 'get',
      params: { requestId }
    })
  },

  // ---------- 项目 ----------
  projectPage(params: ProjectQueryParams) {
    return request<PageResult<ProjectRecord>>({
      url: '/admin/project/page',
      method: 'get',
      params: {
        page: params.page,
        pageSize: params.pageSize,
        projectName: params.projectName,
        district: params.district,
        projectType: params.projectType,
        status: params.status
      }
    })
  },

  projectDetail(id: number) {
    return request<ProjectRecord>({
      url: `/admin/project/${id}`,
      method: 'get'
    })
  },

  projectCreate(data: ProjectCreateParams) {
    return request<number>({
      url: '/admin/project/create',
      method: 'post',
      data
    })
  },

  projectUpdate(data: ProjectCreateParams & { id?: number }) {
    return request<void>({
      url: '/admin/project/update',
      method: 'put',
      data
    })
  },

  saveProjectCommissionRule(data: ProjectCommissionRuleCreateParams) {
    return request<number>({
      url: '/admin/project/commission-rule',
      method: 'post',
      data
    })
  },

  activeProjectCommissionRule(projectId: number) {
    return request<ProjectCommissionRuleRecord>({
      url: `/admin/project/commission-rule/active/${projectId}`,
      method: 'get'
    })
  },

  projectCommissionRuleVersions(projectId: number) {
    return request<ProjectCommissionRuleRecord[]>({
      url: `/admin/project/commission-rule/versions/${projectId}`,
      method: 'get'
    })
  },

  // ---------- 数据统计仪表盘 ----------
  dashboardOverview(params: { startDate?: string; endDate?: string }) {
    return request<DashboardOverviewRecord>({
      url: '/admin/dashboard/overview',
      method: 'get',
      params: { startDate: params.startDate, endDate: params.endDate }
    })
  },

  dashboardFunnel(params: { projectId?: number; allianceId?: number; startDate?: string; endDate?: string }) {
    return request<DashboardFunnelRecord>({
      url: '/admin/dashboard/funnel',
      method: 'get',
      params: {
        projectId: params.projectId,
        allianceId: params.allianceId,
        startDate: params.startDate,
        endDate: params.endDate
      }
    })
  },

  dashboardRanking(params: { startDate?: string; endDate?: string }) {
    return request<DashboardRankingRecord>({
      url: '/admin/dashboard/ranking',
      method: 'get',
      params: { startDate: params.startDate, endDate: params.endDate }
    })
  },

  // ---------- AI 对话（对接 /admin/ai/chat） ----------
  aiChat(message: string, conversationId?: string, projectId?: number) {
    return request<AiChatRecord & { conversationId?: string; routedAgent?: string; structuredData?: Record<string, unknown> }>({
      url: '/admin/ai/chat',
      method: 'post',
      data: { message, conversationId: conversationId || null, projectId: projectId || null }
    })
  },

  aiKnowledgeUpload(file: File, projectId?: number) {
    const formData = new FormData()
    formData.append('file', file)
    if (projectId != null && projectId > 0) {
      formData.append('projectId', String(projectId))
    }
    return request<number>({
      url: '/admin/ai/knowledge/upload',
      method: 'post',
      data: formData
    })
  },

  aiKnowledgeDocs() {
    return request<AiKnowledgeDocRecord[]>({
      url: '/admin/ai/knowledge/docs',
      method: 'get'
    }).catch(() => [])
  },

  aiKnowledgeDeleteDoc(id: number) {
    return request<void>({
      url: `/admin/ai/knowledge/docs/${id}`,
      method: 'delete'
    })
  }
}
