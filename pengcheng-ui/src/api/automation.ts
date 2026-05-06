import { request } from '@/utils/request'

/** 自动化规则引擎 API — 对接 /automation/** */
export interface AutomationRuleDTO {
  id?: number
  name: string
  description?: string
  /** time_based / event_based / condition_based */
  triggerType: string
  triggerConfig?: Record<string, any>
  /** notify / assign / update_status / create_task */
  actionType: string
  actionConfig?: Record<string, any>
  enabled?: boolean
  priority?: number
  triggerCount?: number
  lastTriggeredAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface AutomationLogDTO {
  id: number
  ruleId: number
  triggerData?: Record<string, any>
  actionResult?: string
  status: number
  executedAt: string
}

export const automationApi = {
  listRules() {
    return request<AutomationRuleDTO[]>({ url: '/automation/rules', method: 'get' })
  },
  createRule(rule: AutomationRuleDTO) {
    return request<AutomationRuleDTO>({ url: '/automation/rule', method: 'post', data: rule })
  },
  updateRule(rule: AutomationRuleDTO) {
    return request<void>({ url: '/automation/rule', method: 'put', data: rule })
  },
  toggleRule(id: number, enabled: boolean) {
    return request<void>({
      url: `/automation/rule/${id}/toggle`,
      method: 'post',
      params: { enabled }
    })
  },
  deleteRule(id: number) {
    return request<void>({ url: `/automation/rule/${id}`, method: 'delete' })
  },
  getRuleLogs(ruleId: number, limit = 20) {
    return request<AutomationLogDTO[]>({
      url: `/automation/rule/${ruleId}/logs`,
      method: 'get',
      params: { limit }
    })
  },
  /** 手动触发 time_based 规则扫描（运维/调试用） */
  executeNow() {
    return request<void>({ url: '/automation/execute', method: 'post' })
  }
}
