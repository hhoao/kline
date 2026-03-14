/**
 * Models Service API
 * 模型相关服务
 */
import { defHttp } from '@/utils/http/axios'

export const modelsService = {
  async refreshOpenRouterModelsRpc(): Promise<any> {
    return await defHttp.post<any>({
      url: '/api/cline/models/refresh-openrouter',
      method: 'POST'
    })
  },

  async updateApiConfigurationProto(request: any): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/models/update-api-configuration',
      method: 'POST',
      data: request
    })
  }
}
