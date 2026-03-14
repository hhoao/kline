/**
 * 聊天按钮点击处理器
 */
import type { ChatButtonClickedData } from '../../types/subscription'
import { SubscriptionMessageType } from '../../constants/subscription'
import type { SubscriptionHandler } from './handler'

/**
 * 聊天按钮点击处理器类
 */
export class ChatButtonClickedHandler implements SubscriptionHandler<ChatButtonClickedData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.CHAT_BUTTON_CLICKED
  }

  /**
   * 处理聊天按钮点击消息
   */
  handle(_message: ChatButtonClickedData): void {
    // 聊天按钮点击事件处理（这里需要根据实际UI实现）
    console.log('[ChatButtonHandler] 聊天按钮被点击')
    // TODO: 实现实际的聊天按钮点击逻辑
  }
}

/**
 * 导出处理器实例（单例）
 */
export const chatButtonClickedHandler = new ChatButtonClickedHandler()
