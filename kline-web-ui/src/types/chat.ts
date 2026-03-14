/**
 * 聊天相关类型定义
 */

import { ClineMessage } from "@/shared/ExtensionMessage"

export type ClineAskResponse = 
  | "yesButtonClicked"
  | "noButtonClicked"
  | "messageResponse"

// 聊天消息类型
export interface ChatMessage {
  id: number | string
  type: 'user' | 'assistant'
  content: string
  createTime: string
  [key: string]: any
}

// 对话类型定义
export interface Conversation {
  id: string
  title: string
  lastMessageTime: string | null
}


// 状态数据（对应 ExtensionState，但这里只包含我们需要的字段）
export interface StateData {
  clineMessages?: ClineMessage[]
  [key: string]: any
}

