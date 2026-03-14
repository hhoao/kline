/**
 * Host Workspace Service API
 * 工作区相关服务（Host）
 */
import { defHttp } from '@/utils/http/axios'

export const hostWorkspaceService = {
  async getWorkspaceProblems(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/workspace/workspace-problems',
      method: 'POST',
      data: request
    })
  },

  async openProblemsPanel(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/workspace/open-problems-panel',
      method: 'POST',
      data: request
    })
  },

  async openInFileExplorerPanel(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/workspace/open-in-file-explorer-panel',
      method: 'POST',
      data: request
    })
  },

  async openTerminalPanel(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/workspace/open-terminal-panel',
      method: 'POST',
      data: request
    })
  }
}
