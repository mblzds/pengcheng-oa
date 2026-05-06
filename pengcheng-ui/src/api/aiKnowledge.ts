import { request } from '@/utils/request'

export type AiKnowledgeDocStatus = 'PROCESSING' | 'DONE' | 'FAILED'

export interface AiKnowledgeDocRecord {
  id: number
  fileName: string
  projectId?: number
  status: AiKnowledgeDocStatus
  uploadTime?: string
}

export interface AiKnowledgeQueryParams {
  question: string
  projectId?: number
}

export const aiKnowledgeApi = {
  uploadDocument(file: File, projectId?: number): Promise<number> {
    const formData = new FormData()
    formData.append('file', file)
    if (projectId != null) {
      formData.append('projectId', String(projectId))
    }
    return request<number>({
      url: '/admin/ai/knowledge/upload',
      method: 'post',
      data: formData
    })
  },

  listDocs(): Promise<AiKnowledgeDocRecord[]> {
    return request<AiKnowledgeDocRecord[]>({
      url: '/admin/ai/knowledge/docs',
      method: 'get'
    })
  },

  deleteDoc(id: number): Promise<void> {
    return request<void>({
      url: `/admin/ai/knowledge/docs/${id}`,
      method: 'delete'
    })
  },

  query(params: AiKnowledgeQueryParams): Promise<string> {
    return request<string>({
      url: '/admin/ai/knowledge/query',
      method: 'post',
      data: params
    })
  }
}
