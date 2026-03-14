/**
 * Slash Service API
 * 斜杠命令相关服务
 */
import { defHttp } from '@/utils/http/axios'

export const slashService = {
  /**
   * 报告 Bug
   */
  async reportBug(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/slash/report-bug',
      method: 'POST',
      data: { value: request }
    })
  },

  /**
   * 压缩对话
   */
  async condense(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/slash/condense',
      method: 'POST',
      data: { value: request }
    })
  }
}
