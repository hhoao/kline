/**
 * Account Service API
 * 账户相关服务
 */
import { defHttp } from '@/utils/http/axios'

export const accountService = {
  async accountLoginClicked(): Promise<string> {
    return await defHttp.post<string>({
      url: '/api/cline/account/login-clicked',
      method: 'POST'
    })
  },

  async getRedirectUrl(): Promise<string> {
    return await defHttp.post<string>({
      url: '/api/cline/account/redirect-url',
      method: 'POST'
    })
  }
}

