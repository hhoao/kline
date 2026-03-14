/**
 * Checkpoints Service API
 * 检查点相关服务
 */
import { CheckpointRestoreRequest } from '@/shared/proto/index.cline'
import { defHttp } from '@/utils/http/axios'

export const checkpointsService = {
  async checkpointDiff(request: number): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/checkpoints/diff',
      method: 'POST',
      data: { value: request }
    })
  },

  async checkpointRestore(request: CheckpointRestoreRequest): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/checkpoints/restore',
      method: 'POST',
      data: request
    })
  }
}

