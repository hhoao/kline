/**
 * 工作区消息处理器
 */
import type {
  WorkspaceGetWorkspaceProblemData,
  WorkspaceOpenProblemsPanelData,
  WorkspaceOpenTerminalData,
  WorkspaceOpenInFileExplorerPanelData
} from '../../types/subscription'
import { validateRequestMessage } from '../../utils/messageUtils'
import { SubscriptionMessageType } from '../../constants/subscription'
import { hostWorkspaceService } from '@/api/host/workspace'
import type { SubscriptionHandler } from './handler'

/**
 * 发送工作区响应（公共方法）
 */
async function sendWorkspaceResponse(
  requestId: string,
  responseHandler: (requestId: string) => Promise<void>
): Promise<void> {
  try {
    await responseHandler(requestId)
  } catch (error) {
    console.error('[WorkspaceMessageHandler] 发送工作区响应失败:', error)
  }
}

/**
 * 工作区获取问题请求处理器类
 */
export class WorkspaceGetWorkspaceProblemHandler implements SubscriptionHandler<WorkspaceGetWorkspaceProblemData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WORKSPACE_GET_WORKSPACE_PROBLEM
  }

  /**
   * 处理工作区获取问题请求
   */
  async handle(message: WorkspaceGetWorkspaceProblemData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WORKSPACE_GET_WORKSPACE_PROBLEM)) {
      return
    }
    
    await sendWorkspaceResponse(message.requestId!, async (requestId) => {
      // 获取工作区问题（这里需要根据实际实现）
      // 暂时发送空响应，实际应该获取问题列表
      await hostWorkspaceService.getWorkspaceProblems({
        requestId,
        response: null // 实际应该获取问题列表
      })
    })
  }
}

/**
 * 工作区打开问题面板请求处理器类
 */
export class WorkspaceOpenProblemsPanelHandler implements SubscriptionHandler<WorkspaceOpenProblemsPanelData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WORKSPACE_OPEN_PROBLEMS_PANEL
  }

  /**
   * 处理工作区打开问题面板请求
   */
  async handle(message: WorkspaceOpenProblemsPanelData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WORKSPACE_OPEN_PROBLEMS_PANEL)) {
      return
    }
    
    await sendWorkspaceResponse(message.requestId!, async (requestId) => {
      // 打开问题面板（这里需要根据实际UI实现）
      await hostWorkspaceService.openProblemsPanel({
        requestId,
        response: null // 实际应该返回打开结果
      })
    })
  }
}

/**
 * 工作区打开终端请求处理器类
 */
export class WorkspaceOpenTerminalHandler implements SubscriptionHandler<WorkspaceOpenTerminalData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WORKSPACE_OPEN_TERMINAL
  }

  /**
   * 处理工作区打开终端请求
   */
  async handle(message: WorkspaceOpenTerminalData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WORKSPACE_OPEN_TERMINAL)) {
      return
    }
    
    await sendWorkspaceResponse(message.requestId!, async (requestId) => {
      // 打开终端（这里需要根据实际UI实现）
      await hostWorkspaceService.openTerminalPanel({
        requestId,
        response: null // 实际应该返回打开结果
      })
    })
  }
}

/**
 * 工作区在文件资源管理器中打开请求处理器类
 */
export class WorkspaceOpenInFileExplorerPanelHandler implements SubscriptionHandler<WorkspaceOpenInFileExplorerPanelData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL
  }

  /**
   * 处理工作区在文件资源管理器中打开请求
   */
  async handle(message: WorkspaceOpenInFileExplorerPanelData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL)) {
      return
    }
    
    await sendWorkspaceResponse(message.requestId!, async (requestId) => {
      // 在文件资源管理器中打开（这里需要根据实际UI实现）
      await hostWorkspaceService.openInFileExplorerPanel({
        requestId,
        response: null // 实际应该返回打开结果
      })
    })
  }
}

/**
 * 导出处理器实例（单例）
 */
export const workspaceGetWorkspaceProblemHandler = new WorkspaceGetWorkspaceProblemHandler()
export const workspaceOpenProblemsPanelHandler = new WorkspaceOpenProblemsPanelHandler()
export const workspaceOpenTerminalHandler = new WorkspaceOpenTerminalHandler()
export const workspaceOpenInFileExplorerPanelHandler = new WorkspaceOpenInFileExplorerPanelHandler()
