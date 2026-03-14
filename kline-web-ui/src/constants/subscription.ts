/**
 * 订阅消息类型常量
 * 与后端 MessageType 枚举保持一致
 */
export const SubscriptionMessageType = {
  /** 部分消息更新 */
  PARTIAL_MESSAGE: 'PARTIAL_MESSAGE',
  /** 状态更新 */
  STATE: 'STATE',
  /** 聊天按钮点击 */
  CHAT_BUTTON_CLICKED: 'CHAT_BUTTON_CLICKED',
  /** 窗口显示消息请求 */
  WINDOW_SHOW_MESSAGE: 'WINDOW_SHOW_MESSAGE',
  /** 窗口显示文本文档请求 */
  WINDOW_SHOW_TEXT_DOCUMENT: 'WINDOW_SHOW_TEXT_DOCUMENT',
  /** 工作区打开问题面板请求 */
  WORKSPACE_OPEN_PROBLEMS_PANEL: 'WORKSPACE_OPEN_PROBLEMS_PANEL',
  /** 工作区打开终端请求 */
  WORKSPACE_OPEN_TERMINAL: 'WORKSPACE_OPEN_TERMINAL',
  /** 工作区获取问题请求 */
  WORKSPACE_GET_WORKSPACE_PROBLEM: 'WORKSPACE_GET_WORKSPACE_PROBLEM',
  /** 工作区在文件资源管理器中打开请求 */
  WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL: 'WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL'
} as const

export type SubscriptionMessageType = typeof SubscriptionMessageType[keyof typeof SubscriptionMessageType]

