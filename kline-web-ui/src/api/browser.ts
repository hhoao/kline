/**
 * Browser Service API
 * 浏览器相关服务
 */
import { BrowserConnectionInfo } from '@/shared/proto/cline/browser'
import { defHttp } from '@/utils/http/axios'

export const browserService = {
  async getBrowserConnectionInfo(): Promise<BrowserConnectionInfo> {
    return await defHttp.get<BrowserConnectionInfo>({
      url: '/api/cline/browser/connection-info',
      method: 'GET'
    })
  }
}

