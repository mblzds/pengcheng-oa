import request from '@/utils/request'

export interface ApprovalFlowNodeVO {
  id?: number
  businessType: string
  seq?: number
  nodeName: string
  /** direct_supervisor / applicant_dept_manager / role / user */
  approverType: string
  /** role/user 时为逗号分隔的 ID 列表；direct_supervisor / applicant_dept_manager 时为空 */
  approverValue?: string | null
  /** 适用申请人角色 ID 列表（逗号分隔）；空 = 全员适用 */
  appliesToRoleIds?: string | null
  enabled?: number
}

export const approvalFlowApi = {
  list(businessType: string): Promise<ApprovalFlowNodeVO[]> {
    return request({ url: `/admin/approval-flow/${businessType}`, method: 'get' })
  },

  save(businessType: string, nodes: ApprovalFlowNodeVO[]): Promise<void> {
    return request({ url: `/admin/approval-flow/${businessType}`, method: 'put', data: nodes })
  }
}

/** 审批类型 — 与后端 controller switch 分支保持一致 */
export type ApprovalType = 'leave' | 'compensate' | 'expense' | 'advance' | 'prepay' | 'commission'

export interface ApprovalItem {
  id: number
  type: ApprovalType
  applicantName: string
  summary: string
  amount?: number | null
  applyTime: string
  currentNodeName?: string | null
}

export interface ApprovalHistory {
  nodeName?: string | null
  approverName: string
  /** 1 通过 / 2 驳回 / NULL 待审批 */
  result?: number | null
  remark?: string | null
  approvalTime?: string | null
}

export interface ApprovalDetail {
  id: number
  type: ApprovalType
  applicantName: string
  summary: string
  amount?: number | null
  status?: number | null
  applyTime: string
  histories: ApprovalHistory[]
}

export interface ApproveDTO {
  type: ApprovalType
  approved: boolean
  reason?: string
}

export const approvalApi = {
  pending(): Promise<ApprovalItem[]> {
    return request({ url: '/admin/approval/pending', method: 'get' })
  },

  detail(id: number, type: ApprovalType): Promise<ApprovalDetail> {
    return request({ url: `/admin/approval/${id}`, method: 'get', params: { type } })
  },

  approve(id: number, dto: ApproveDTO): Promise<void> {
    return request({ url: `/admin/approval/${id}/approve`, method: 'post', data: dto })
  }
}
