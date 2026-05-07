import request from '@/utils/request'

export interface ApprovalFlowNodeVO {
  id?: number
  businessType: string
  seq?: number
  nodeName: string
  /** direct_supervisor / role / user */
  approverType: string
  /** role/user 时为逗号分隔的 ID 列表；direct_supervisor 时为空 */
  approverValue?: string | null
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
