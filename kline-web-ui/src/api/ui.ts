/**
 * Ui Service API
 * UI 相关服务
 */
import { defHttp } from '@/utils/http/axios'

export const uiService = {
  async scrollToSettings(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/ui/scroll-to-settings',
      method: 'POST',
      data: { value: request }
    })
  },

  async setTerminalExecutionMode(request: boolean): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/ui/set-terminal-execution-mode',
      method: 'POST',
      data: { value: request }
    })
  },

  async initializeWebview(): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/ui/initialize-webview',
      method: 'POST'
    })
  }
}
