/**
 * 窗口消息处理器
 */
import type { WindowShowMessageRequestData, WindowShowTextDocumentData } from '../../types/subscription'
import { validateRequestMessage } from '../../utils/messageUtils'
import { SubscriptionMessageType } from '../../constants/subscription'
import { hostWindowService } from '@/api/host/window'
import type { SubscriptionHandler } from './handler'

/**
 * 窗口显示消息请求处理器类
 */
export class WindowShowMessageHandler implements SubscriptionHandler<WindowShowMessageRequestData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WINDOW_SHOW_MESSAGE
  }

  /**
   * 处理窗口显示消息请求
   */
  async handle(message: WindowShowMessageRequestData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WINDOW_SHOW_MESSAGE)) {
      return
    }
    
    try {
      // 显示消息对话框（这里需要根据实际UI实现）
      // 暂时发送空响应，实际应该显示对话框并等待用户选择
      await hostWindowService.showMessage({
        requestId: message.requestId!,
        response: null // 实际应该从用户交互获取
      })
    } catch (error) {
      console.error('[WindowMessageHandler] 处理窗口显示消息请求失败:', error)
    }
  }
}

/**
 * 窗口显示文本文档请求处理器类
 */
export class WindowShowTextDocumentHandler implements SubscriptionHandler<WindowShowTextDocumentData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.WINDOW_SHOW_TEXT_DOCUMENT
  }

  /**
   * 处理窗口显示文本文档请求
   */
  async handle(message: WindowShowTextDocumentData): Promise<void> {
    if (!validateRequestMessage(message, SubscriptionMessageType.WINDOW_SHOW_TEXT_DOCUMENT)) {
      return
    }
    
    try {
      // 显示文本文档（这里需要根据实际UI实现）
      // 暂时发送空响应，实际应该打开文档并返回编辑器信息
      await hostWindowService.showTextDocument({
        requestId: message.requestId!,
        response: null // 实际应该从打开的文档获取编辑器信息
      })
    } catch (error) {
      console.error('[WindowMessageHandler] 处理窗口显示文本文档请求失败:', error)
    }
  }
}

/**
 * 导出处理器实例（单例）
 */
export const windowShowMessageHandler = new WindowShowMessageHandler()
export const windowShowTextDocumentHandler = new WindowShowTextDocumentHandler()
