/**
 * Task Service API
 * 任务相关服务
 */
import { NewTaskRequest, GetTaskHistoryRequest, TaskHistoryArray, AskResponseRequest, ExecuteQuickWinRequest, TaskFavoriteRequest } from '@/shared/proto/index.cline'
import { defHttp } from '@/utils/http/axios'

export const taskService = {
  /**
   * 取消当前运行的任务
   */
  async cancelTask(taskId: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/cancel',
      method: 'POST',
      data: { taskId }
    })
  },

  /**
   * 取消后台命令
   */
  async cancelBackgroundCommand(taskId: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/cancel-background-command',
      method: 'POST',
      data: { taskId }
    })
  },

  /**
   * 清除当前任务
   */
  async clearTask(taskId: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/clear',
      method: 'POST',
      data: { taskId }
    })
  },

  /**
   * 获取所有任务的总大小
   */
  async getTotalTasksSize(): Promise<number> {
    return await defHttp.get<number>({
      url: '/api/cline/task/total-size',
      method: 'GET'
    })
  },

  /**
   * 根据 ID 删除任务
   */
  async deleteTasksWithIds(request: string[]): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/delete',
      method: 'POST',
      data: { value: request }
    })
  },

  /**
   * 创建新任务
   */
  async newTask(request: NewTaskRequest): Promise<string> {
    return await defHttp.post<string>({
      url: '/api/cline/task/new',
      method: 'POST',
      data: request
    })
  },

  /**
   * 根据 ID 显示任务
   */
  async showTaskWithId(taskId: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/show',
      method: 'POST',
      data: { taskId }
    })
  },

  /**
   * 根据 ID 导出任务
   */
  async exportTaskWithId(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/export',
      method: 'POST',
      data: { value: request }
    })
  },

  /**
   * 切换任务收藏
   */
  async toggleTaskFavorite(request: TaskFavoriteRequest): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/toggle-favorite',
      method: 'POST',
      data: request
    })
  },

  /**
   * 获取任务历史
   */
  async getTaskHistory(request: GetTaskHistoryRequest): Promise<TaskHistoryArray> {
    return await defHttp.post<TaskHistoryArray>({
      url: '/api/cline/task/history',
      method: 'POST',
      data: request
    })
  },

  /**
   * 发送响应（askResponse）
   */
  async askResponse(request: AskResponseRequest): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/ask-response',
      method: 'POST',
      data: request
    })
  },

  /**
   * 任务反馈
   */
  async taskFeedback(request: string): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/feedback',
      method: 'POST',
      data: { value: request }
    })
  },

  async taskCompletionViewChanges(request: number): Promise<void> {
    return await defHttp.post<void>({
      url: '/api/cline/task/completion-view-changes',
      method: 'POST',
      data: { value: request }
    })
  },

  async deleteAllTaskHistory(): Promise<number> {
    return await defHttp.post<number>({
      url: '/api/cline/task/delete-all-history',
      method: 'POST'
    })
  }
}

