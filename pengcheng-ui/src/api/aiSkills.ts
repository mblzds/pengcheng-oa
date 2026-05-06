import { request } from '@/utils/request'

export type AiSkillIntent = 'REPORT' | 'KNOWLEDGE' | 'COPYWRITING' | 'APPROVAL' | 'CUSTOMER' | 'GENERAL'

export interface AiSkillRecord {
  name: string
  intent: AiSkillIntent
  className: string
  enabled: boolean
}

export interface AiSkillStats {
  totalTools: number
  enabledCount: number
  disabledCount: number
  intentDistribution: Partial<Record<AiSkillIntent, number>> & Record<string, number>
}

function skillToggleUrl(name: string, enabled: boolean): string {
  const action = enabled ? 'enable' : 'disable'
  return `/ai/skills/${action}/${encodeURIComponent(name)}`
}

export const aiSkillsApi = {
  list(): Promise<AiSkillRecord[]> {
    return request<AiSkillRecord[]>({
      url: '/ai/skills/list',
      method: 'get'
    })
  },

  enable(name: string): Promise<void> {
    return request<void>({
      url: skillToggleUrl(name, true),
      method: 'post'
    })
  },

  disable(name: string): Promise<void> {
    return request<void>({
      url: skillToggleUrl(name, false),
      method: 'post'
    })
  },

  toggle(name: string, enabled: boolean): Promise<void> {
    return request<void>({
      url: skillToggleUrl(name, enabled),
      method: 'post'
    })
  },

  disabledList(): Promise<string[]> {
    return request<string[]>({
      url: '/ai/skills/disabled',
      method: 'get'
    })
  },

  stats(): Promise<AiSkillStats> {
    return request<AiSkillStats>({
      url: '/ai/skills/stats',
      method: 'get'
    })
  }
}
