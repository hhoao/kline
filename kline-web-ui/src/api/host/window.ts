/**
 * Host Window Service API
 * 窗口相关服务（Host）
 */
import { defHttp } from '../../utils/http/axios'

export const hostWindowService = {
  async showTextDocument(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/window/show-text-document',
      method: 'POST',
      data: request
    })
  },

  async showMessage(request: { requestId: string; response: any }): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/host/window/show-message',
      method: 'POST',
      data: request
    })
  }
}
