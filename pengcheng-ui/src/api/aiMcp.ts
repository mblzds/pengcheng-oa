import { request } from '@/utils/request'

export interface AiMcpSchemaNode {
  type?: string
  description?: string
  properties?: Record<string, AiMcpSchemaNode>
  required?: string[]
  items?: AiMcpSchemaNode | AiMcpSchemaNode[]
  enum?: Array<string | number | boolean | null>
  default?: unknown
  additionalProperties?: boolean | AiMcpSchemaNode
  [key: string]: unknown
}

export interface AiMcpToolRecord {
  name: string
  description: string
  inputSchema: AiMcpSchemaNode
  enabled: boolean
}

export interface AiMcpToolExecuteParams {
  name: string
  arguments?: Record<string, unknown>
}

export interface AiMcpToolResultContent {
  type: string
  text?: string
  [key: string]: unknown
}

export interface AiMcpToolResult {
  content: AiMcpToolResultContent[]
  isError: boolean
}

function mcpToolUrl(name: string, action: 'enable' | 'disable' | 'execute'): string {
  return `/mcp/tools/${encodeURIComponent(name)}/${action}`
}

export const aiMcpApi = {
  listTools(): Promise<AiMcpToolRecord[]> {
    return request<AiMcpToolRecord[]>({
      url: '/mcp/tools',
      method: 'get'
    })
  },

  executeTool(params: AiMcpToolExecuteParams): Promise<AiMcpToolResult> {
    return request<AiMcpToolResult>({
      url: mcpToolUrl(params.name, 'execute'),
      method: 'post',
      data: params.arguments ?? {}
    })
  },

  enableTool(name: string): Promise<void> {
    return request<void>({
      url: mcpToolUrl(name, 'enable'),
      method: 'post'
    })
  },

  disableTool(name: string): Promise<void> {
    return request<void>({
      url: mcpToolUrl(name, 'disable'),
      method: 'post'
    })
  },

  toggleTool(name: string, enabled: boolean): Promise<void> {
    return request<void>({
      url: mcpToolUrl(name, enabled ? 'enable' : 'disable'),
      method: 'post'
    })
  }
}
