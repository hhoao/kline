/**
 * 订阅消息类型定义
 */

import { ClineAsk, ClineSay, ExtensionState } from "@/shared/ExtensionMessage";
import { ShowMessageType } from "@/shared/proto/index.host";

// 基础订阅消息类型
export interface SubscriptionMessage {
  type: string;
  requestId?: string;
}


// ShowMessageRequestOptions 类型（对应 Java 的 ShowMessageRequestOptions）
export interface ShowMessageRequestOptions {
  items?: string[];
  modal?: boolean;
  detail?: string;
}

// ShowMessageRequest 类型（对应 Java 的 ShowMessageRequest）
export interface ShowMessageRequest {
  type: ShowMessageType;
  message?: string;
  options?: ShowMessageRequestOptions;
}

// ShowTextDocumentOptions 类型（对应 Java 的 ShowTextDocumentOptions）
export interface ShowTextDocumentOptions {
  preview?: boolean;
  preserveFocus?: boolean;
  viewColumn?: number;
}

// ShowTextDocumentRequest 类型（对应 Java 的 ShowTextDocumentRequest）
export interface ShowTextDocumentRequest {
  path?: string;
  options?: ShowTextDocumentOptions;
}

// GetWorkspaceProblemsRequest 类型（对应 Java 的 GetWorkspaceProblemsRequest）
export interface GetWorkspaceProblemsRequest {
  action?: string;
  cwd?: string;
}

// OpenProblemsPanelRequest 类型（对应 Java 的 OpenProblemsPanelRequest）
export interface OpenProblemsPanelRequest {
  // 空接口，用于协议兼容性
}

// OpenTerminalRequest 类型（对应 Java 的 OpenTerminalRequest）
export interface OpenTerminalRequest {
  // 空接口，用于协议兼容性
}

// OpenInFileExplorerPanelRequest 类型（对应 Java 的 OpenInFileExplorerPanelRequest）
export interface OpenInFileExplorerPanelRequest {
  path?: string;
}

// 部分消息类型（匹配后端 PartialMessage 结构）
export interface PartialMessageData extends SubscriptionMessage {
  type: "PARTIAL_MESSAGE";
  ts?: number | null;
  taskId?: string | null;
  clineMessageType?: "ask" | "say" | null;
  ask?: ClineAsk | null;
  say?: ClineSay;
  incrementContent?: string | null;
  format?: "json" | "plain" | null;
  reasoning?: string | null;
  images?: string[] | null;
  files?: string[] | null;
  commandCompleted?: boolean | null;
  isUpdatingPreviousPartial?: boolean | null;
}

// 状态消息类型
export interface StateMessageData extends SubscriptionMessage {
  type: "STATE";
  state: ExtensionState;
}

// 窗口显示消息请求
export interface WindowShowMessageRequestData extends SubscriptionMessage {
  type: "WINDOW_SHOW_MESSAGE";
  requestId: string;
  request: ShowMessageRequest;
}

// 窗口显示文本文档请求
export interface WindowShowTextDocumentData extends SubscriptionMessage {
  type: "WINDOW_SHOW_TEXT_DOCUMENT";
  requestId: string;
  request: ShowTextDocumentRequest;
}

// 工作区获取问题请求
export interface WorkspaceGetWorkspaceProblemData extends SubscriptionMessage {
  type: "WORKSPACE_GET_WORKSPACE_PROBLEM";
  requestId: string;
  getWorkspaceProblemsRequest: GetWorkspaceProblemsRequest;
}

// 工作区打开问题面板请求
export interface WorkspaceOpenProblemsPanelData extends SubscriptionMessage {
  type: "WORKSPACE_OPEN_PROBLEMS_PANEL";
  requestId: string;
  request?: OpenProblemsPanelRequest;
}

// 工作区打开终端请求
export interface WorkspaceOpenTerminalData extends SubscriptionMessage {
  type: "WORKSPACE_OPEN_TERMINAL";
  requestId: string;
  request: OpenTerminalRequest;
}

// 工作区在文件资源管理器中打开请求
export interface WorkspaceOpenInFileExplorerPanelData
  extends SubscriptionMessage {
  type: "WORKSPACE_OPEN_IN_FILE_EXPLORER_PANEL";
  requestId: string;
  request?: OpenInFileExplorerPanelRequest;
}

// 聊天按钮点击消息
export interface ChatButtonClickedData extends SubscriptionMessage {
  type: "CHAT_BUTTON_CLICKED";
}
