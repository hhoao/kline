/**
 * 订阅消息处理器接口
 */
import type { SubscriptionMessage } from '../../types/subscription'
import type { ChatMessage } from '../../types/chat'
import { ExtensionState } from '@/shared/ExtensionMessage'

/**
 * 处理器上下文
 */
export interface HandlerContext {
  /** 聊天消息列表 */
  chatMessages?: ChatMessage[]
  /** 当前任务ID */
  currentTaskId?: string | null
  /** 设置扩展状态 */
  setState?: (state: ExtensionState) => void
}

/**
 * 订阅消息处理器接口
 */
export interface SubscriptionHandler<T extends SubscriptionMessage = SubscriptionMessage> {
  /**
   * 获取该处理器支持的消息类型
   */
  getMessageType(): string

  /**
   * 处理消息
   * @param message 消息数据
   * @param context 处理器上下文（可选）
   * @returns 处理结果（可以是任意类型，根据具体处理器而定）
   */
  handle(message: T): any
}

