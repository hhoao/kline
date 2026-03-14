import type { McpMarketplaceCatalog, McpServers } from '@/shared/proto/cline/mcp'
import { McpServerStatus } from '@/shared/proto/cline/mcp'
import { defHttp } from '@/utils/http/axios'

export interface ToggleMcpServerRequest {
  serverName: string
  disabled: boolean
}

export interface UpdateMcpTimeoutRequest {
  serverName: string
  timeout: number
}

export interface AddRemoteMcpServerRequest {
  serverName: string
  serverUrl: string
}

export interface ToggleToolAutoApproveRequest {
  serverName: string
  toolNames: string[]
  autoApprove: boolean
}

const BASE = '/api/cline/mcp'

function parseJsonData<T>(data: unknown): T {
  if (typeof data === 'string') {
    return JSON.parse(data) as T
  }
  return data as T
}

function normalizeServersResponse(raw: unknown): McpServers {
  const parsed = typeof raw === 'string' ? parseJsonData<unknown>(raw) : raw
  if (parsed && typeof parsed === 'object' && 'mcpServers' in parsed && Array.isArray((parsed as McpServers).mcpServers)) {
    return parsed as McpServers
  }
  const list = Array.isArray(parsed) ? parsed : (parsed && typeof parsed === 'object' && 'mcpServers' in parsed) ? (parsed as McpServers).mcpServers : []
  const mcpServers = (list as Record<string, unknown>[]).map((item) => ({
    name: String(item.name ?? ''),
    config: typeof item.config === 'string' ? item.config : JSON.stringify({ timeout: item.timeout ?? 60 }),
    status: typeof item.status === 'number' ? item.status : McpServerStatus.MCP_SERVER_STATUS_DISCONNECTED,
    error: item.error as string | undefined,
    tools: Array.isArray(item.tools) ? item.tools : [],
    resources: Array.isArray(item.resources) ? item.resources : [],
    resourceTemplates: Array.isArray(item.resourceTemplates) ? item.resourceTemplates : [],
    disabled: Boolean(item.disabled),
    timeout: typeof item.timeout === 'number' ? item.timeout : undefined,
  }))
  return { mcpServers } as McpServers
}

export const mcpService = {
  async toggleMcpServer(request: ToggleMcpServerRequest): Promise<McpServers> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/toggle-server`, method: 'POST', data: request })
    return normalizeServersResponse(res)
  },

  async updateMcpTimeout(request: UpdateMcpTimeoutRequest): Promise<McpServers> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/update-timeout`, method: 'POST', data: request })
    return normalizeServersResponse(res)
  },

  async addRemoteMcpServer(request: AddRemoteMcpServerRequest): Promise<McpServers> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/add-remote-server`, method: 'POST', data: request })
    return normalizeServersResponse(res)
  },

  async downloadMcp(mcpId: string): Promise<{ error?: string }> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/download`, method: 'POST', data: mcpId })
    const raw = parseJsonData<{ error?: string } | string>(res)
    return typeof raw === 'string' ? {} : raw
  },

  async restartMcpServer(serverName: string): Promise<McpServers> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/restart-server`, method: 'POST', data: serverName })
    return normalizeServersResponse(res)
  },

  async deleteMcpServer(serverName: string): Promise<McpServers> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/delete-server`, method: 'POST', data: serverName })
    return normalizeServersResponse(res)
  },

  async toggleToolAutoApprove(request: ToggleToolAutoApproveRequest): Promise<McpServers> {
    const res = await defHttp.post<unknown>({
      url: `${BASE}/toggle-tool-auto-approve`,
      method: 'POST',
      data: request,
    })
    return normalizeServersResponse(res)
  },

  async refreshMcpMarketplace(): Promise<McpMarketplaceCatalog> {
    const res = await defHttp.post<unknown>({ url: `${BASE}/refresh-marketplace`, method: 'POST' })
    return parseJsonData<McpMarketplaceCatalog>(res)
  },

  async openMcpSettings(): Promise<void> {
    await defHttp.post({ url: `${BASE}/open-settings`, method: 'POST' })
  },

  async getLatestMcpServers(): Promise<McpServers> {
    const res = await defHttp.get<unknown>({ url: `${BASE}/latest-servers`, method: 'GET' })
    return normalizeServersResponse(res)
  },
}
