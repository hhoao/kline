/**
 * 订阅消息处理器主入口
 */
import type { SubscriptionHandler } from './handler'
import { partialMessageHandler } from './partialMessageHandler'
import { stateMessageHandler } from './stateMessageHandler'
import { windowShowMessageHandler, windowShowTextDocumentHandler } from './windowMessageHandlers'
import {
  workspaceGetWorkspaceProblemHandler,
  workspaceOpenProblemsPanelHandler,
  workspaceOpenTerminalHandler,
  workspaceOpenInFileExplorerPanelHandler
} from './workspaceMessageHandlers'
import { chatButtonClickedHandler } from './chatButtonHandler'

/**
 * 所有订阅消息处理器配置
 */
export const subscriptionHandlers: SubscriptionHandler[] = [
  partialMessageHandler,
  stateMessageHandler,
  windowShowMessageHandler,
  windowShowTextDocumentHandler,
  workspaceGetWorkspaceProblemHandler,
  workspaceOpenProblemsPanelHandler,
  workspaceOpenTerminalHandler,
  workspaceOpenInFileExplorerPanelHandler,
  chatButtonClickedHandler
]

/**
 * 根据消息类型获取对应的处理器
 */
export function getHandlerByMessageType(messageType: string): SubscriptionHandler | undefined {
  return subscriptionHandlers.find(handler => handler.getMessageType() === messageType)
}
