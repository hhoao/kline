/**
 * 状态消息处理器
 */
import { SubscriptionMessageType } from '../../constants/subscription'
import type { SubscriptionHandler } from './handler'
import { useExtensionStateStore } from '@/stores/extensionState'
import { StateMessageData } from '@/types/subscription'

/**
 * 状态消息处理器类
 */
export class StateMessageHandler implements SubscriptionHandler<StateMessageData> {
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.STATE
  }

  /**
   * 处理状态消息
   * @param message 状态消息数据
   */
  handle(message: StateMessageData) {
    try {
      const extensionStateStore = useExtensionStateStore()
      
      // 使用 extensionStateStore 的 updateExtensionState 方法更新状态
      if (message.state) {
        extensionStateStore.updateExtensionState(message.state)
      }
    } catch (error) {
      console.error('[StateMessageHandler] 处理状态消息失败:', error)
    }
  }
}

/**
 * 导出处理器实例（单例）
 */
export const stateMessageHandler = new StateMessageHandler()
