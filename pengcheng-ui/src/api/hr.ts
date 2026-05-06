import { request } from '@/utils/request'

export interface HrPeriod {
  id?: number
  name: string
  periodType: number
  year: number
  startDate?: string
  endDate?: string
  targetScore?: number
  status?: number
}

export interface HrKpiTemplate {
  id?: number
  name: string
  code: string
  category: number
  weight: number
  dataSource?: string
  formula?: string
  enabled: boolean
}

export interface HrKpiScore {
  id?: number
  periodId?: number
  userId?: number
  templateId?: number
  targetValue?: number
  actualValue?: number
  score?: number
  weightedScore?: number
  remark?: string
}

/** 员工档案 */
export const hrProfileApi = {
  get: (userId: number) => request({ url: `/admin/hr/employee/profile/${userId}`, method: 'get' }),
  save: (data: any) => request({ url: '/admin/hr/employee/profile', method: 'put', data }),
}

/** 人事异动 */
export const hrChangeApi = {
  page: (params: any) => request({ url: '/admin/hr/employee/changes', method: 'get', params }),
  get: (id: number) => request({ url: `/admin/hr/employee/changes/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/admin/hr/employee/changes', method: 'post', data }),
  setEffective: (id: number) => request({ url: `/admin/hr/employee/changes/${id}/effective`, method: 'post' }),
}

/** 考核周期 */
export const hrKpiPeriodApi = {
  page: (params: { pageNum?: number; pageSize?: number; periodType?: number; status?: number }) =>
    request({ url: '/admin/hr/kpi/periods', method: 'get', params: { pageNum: params.pageNum ?? 1, pageSize: params.pageSize ?? 10, ...params } }),
  get: (id: number) => request({ url: `/admin/hr/kpi/periods/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/admin/hr/kpi/periods', method: 'post', data }),
  update: (data: any) => request({ url: '/admin/hr/kpi/periods', method: 'put', data }),
  remove: (id: number) => request({ url: `/admin/hr/kpi/periods/${id}`, method: 'delete' }),
}

/** KPI 模板 */
export const hrKpiTemplateApi = {
  page: (params: any) => request({ url: '/admin/hr/kpi/templates', method: 'get', params }),
  /** 启用列表，对应后端 GET /templates/list?status=1 */
  listEnabled: () => request({ url: '/admin/hr/kpi/templates/list', method: 'get', params: { status: 1 } }),
  get: (id: number) => request({ url: `/admin/hr/kpi/templates/${id}`, method: 'get' }),
  create: (data: any) => request({ url: '/admin/hr/kpi/templates', method: 'post', data }),
  update: (data: any) => request({ url: '/admin/hr/kpi/templates', method: 'put', data }),
  remove: (id: number) => request({ url: `/admin/hr/kpi/templates/${id}`, method: 'delete' }),
}

/** 考核记录（按周期 + 用户查询） */
export const hrKpiScoreApi = {
  list: (periodId: number, userId: number) =>
    request({ url: '/admin/hr/kpi/scores', method: 'get', params: { periodId, userId } }),
  /** 按 data_source 自动拉取各业务建议实际值（templateId -> actualValue） */
  suggest: (periodId: number, userId: number) =>
    request<Record<string, number>>({ url: '/admin/hr/kpi/scores/suggest', method: 'get', params: { periodId, userId } }),
  save: (data: any) => request({ url: '/admin/hr/kpi/scores', method: 'post', data }),
  batchFill: (periodId: number, userId: number, scores: any[]) =>
    request({ url: '/admin/hr/kpi/scores/batch', method: 'post', params: { periodId, userId }, data: scores }),
}

/** 统一 HR API（供绩效考核页面使用） */
export const hrApi = {
  // 周期管理
  periodPage: (params: any) => hrKpiPeriodApi.page(params),
  createPeriod: (data: any) => hrKpiPeriodApi.create(data),
  updatePeriod: (data: any) => hrKpiPeriodApi.update(data),
  deletePeriod: (id: number) => hrKpiPeriodApi.remove(id),
  
  // 模板管理
  templateList: () => hrKpiTemplateApi.listEnabled(),
  createTemplate: (data: any) => hrKpiTemplateApi.create(data),
  updateTemplate: (data: any) => hrKpiTemplateApi.update(data),
  
  // 考核评分
  scoreList: (params: any) => request({ url: '/admin/hr/kpi/scores/list', method: 'get', params }),
  
  // 考核结果
  resultList: (params: any) => request({ url: '/admin/hr/kpi/results', method: 'get', params }),
  batchSaveResult: (data: any[]) => request({ url: '/admin/hr/kpi/results/batch', method: 'post', data }),
}
