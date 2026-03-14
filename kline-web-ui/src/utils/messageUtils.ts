/**
 * 消息处理工具函数
 */
import type { ChatMessage } from "../types/chat";
import { combineApiRequests } from "@/shared/combineApiRequests";
import { combineCommandSequences } from "@/shared/combineCommandSequences";
import { ClineMessage, ClineSayBrowserAction } from "@/shared/ExtensionMessage";

/**
 * Cline 消息类型常量
 */
export const ClineMessageType = {
  SAY: "say",
  ASK: "ask",
  TEXT: "text",
  REASONING: "reasoning",
} as const;

/**
 * 用户反馈类型常量
 */
export const UserFeedbackType = {
  USER_FEEDBACK: "user_feedback",
  USER_FEEDBACK_DIFF: "user_feedback_diff",
} as const;

/**
 * 从 ClineMessage 中提取消息内容
 * @param message Cline 消息对象
 * @returns 提取的消息内容
 */
export function extractMessageContent(message: ClineMessage): string {
  // 优先使用 text 字段（部分消息通常使用这个字段）
  if (message.text !== undefined && message.text !== null) {
    return String(message.text);
  }
  // 其次使用 say 字段
  if (message.say !== undefined && message.say !== null) {
    return String(message.say);
  }
  // 然后使用 ask 字段
  if (message.ask !== undefined && message.ask !== null) {
    return String(message.ask);
  }
  // 最后使用 reasoning 字段
  if (message.reasoning !== undefined && message.reasoning !== null) {
    return String(message.reasoning);
  }
  return "";
}

/**
 * 将 MessageParam 转换为 ChatMessage
 * @param messageParam API 消息参数
 * @param index 消息索引（用于生成唯一ID）
 * @returns ChatMessage 对象
 */
export function convertMessageParamToChatMessage(
  messageParam: {
    role: string;
    content?: Array<{ type?: string; text?: string; [key: string]: any }>;
  },
  index: number
): ChatMessage {
  // 提取文本内容
  let content = "";
  if (messageParam.content && Array.isArray(messageParam.content)) {
    const textBlocks = messageParam.content
      .filter((block) => block.type === "text" && block.text)
      .map((block) => block.text);
    content = textBlocks.join("\n");
  }

  // 如果没有文本内容，尝试从其他字段提取
  if (!content && messageParam.content && messageParam.content.length > 0) {
    const firstBlock = messageParam.content[0];
    if (firstBlock.text) {
      content = String(firstBlock.text);
    }
  }

  return {
    id: Date.now() + index,
    type: messageParam.role === "user" ? "user" : "assistant",
    content: content || "",
    createTime: new Date().toISOString(),
  } as ChatMessage;
}

/**
 * 验证请求消息是否包含 requestId
 * @param message 订阅消息
 * @param messageType 消息类型（用于日志）
 * @returns 如果有效返回 true，否则返回 false
 */
export function validateRequestMessage(
  message: { requestId?: string },
  messageType: string
): boolean {
  if (!message.requestId) {
    console.warn(`[MessageValidator] ${messageType} 请求缺少 requestId`);
    return false;
  }
  return true;
}

/**
 * Combine API requests and command sequences in messages
 */
export function processMessages(messages: ClineMessage[]): ClineMessage[] {
  return combineApiRequests(combineCommandSequences(messages));
}

/**
 * Filter messages that should be visible in the chat
 */
export function filterVisibleMessages(
  messages: ClineMessage[]
): ClineMessage[] {
  return messages.filter((message) => {
    switch (message.ask) {
      case "completion_result":
        // don't show a chat row for a completion_result ask without text. This specific type of message only occurs if cline wants to execute a command as part of its completion result, in which case we interject the completion_result tool with the execute_command tool.
        if (message.text === "") {
          return false;
        }
        break;
      case "api_req_failed": // this message is used to update the latest api_req_started that the request failed
      case "resume_task":
      case "resume_completed_task":
        return false;
    }
    switch (message.say) {
      case "api_req_finished": // combineApiRequests removes this from modifiedMessages anyways
      case "api_req_retried": // this message is used to update the latest api_req_started that the request was retried
      case "deleted_api_reqs": // aggregated api_req metrics from deleted messages
      case "task_progress": // task progress messages are displayed in TaskHeader, not in main chat
        return false;
      case "text":
        // Sometimes cline returns an empty text message, we don't want to render these. (We also use a say text for user messages, so in case they just sent images we still render that)
        if (
          (message.text ?? "") === "" &&
          (message.images?.length ?? 0) === 0
        ) {
          return false;
        }
        break;
      case "mcp_server_request_started":
        return false;
    }
    return true;
  });
}

/**
 * Check if a message is part of a browser session
 */
export function isBrowserSessionMessage(message: ClineMessage): boolean {
  if (message.type === "ask") {
    return ["browser_action_launch"].includes(message.ask!);
  }
  if (message.type === "say") {
    return [
      "browser_action_launch",
      "api_req_started",
      "text",
      "browser_action",
      "browser_action_result",
      "checkpoint_created",
      "reasoning",
      "error_retry",
    ].includes(message.say!);
  }
  return false;
}

/**
 * Group messages, combining browser session messages into arrays
 */
export function groupMessages(
  visibleMessages: ClineMessage[]
): (ClineMessage | ClineMessage[])[] {
  const result: (ClineMessage | ClineMessage[])[] = [];
  let currentGroup: ClineMessage[] = [];
  let isInBrowserSession = false;

  const endBrowserSession = () => {
    if (currentGroup.length > 0) {
      result.push([...currentGroup]);
      currentGroup = [];
      isInBrowserSession = false;
    }
  };

  visibleMessages.forEach((message) => {
    if (
      message.ask === "browser_action_launch" ||
      message.say === "browser_action_launch"
    ) {
      // complete existing browser session if any
      endBrowserSession();
      // start new
      isInBrowserSession = true;
      currentGroup.push(message);
    } else if (isInBrowserSession) {
      // end session if api_req_started is cancelled
      if (message.say === "api_req_started") {
        // get last api_req_started in currentGroup to check if it's cancelled
        const lastApiReqStarted = [...currentGroup]
          .reverse()
          .find((m) => m.say === "api_req_started");
        if (lastApiReqStarted?.text != null) {
          const info = JSON.parse(lastApiReqStarted.text);
          const isCancelled = info.cancelReason != null;
          if (isCancelled) {
            endBrowserSession();
            result.push(message);
            return;
          }
        }
      }

      if (isBrowserSessionMessage(message)) {
        currentGroup.push(message);

        // Check if this is a close action
        if (message.say === "browser_action") {
          const browserAction = JSON.parse(
            message.text || "{}"
          ) as ClineSayBrowserAction;
          if (browserAction.action === "close") {
            endBrowserSession();
          }
        }
      } else {
        // complete existing browser session if any
        endBrowserSession();
        result.push(message);
      }
    } else {
      result.push(message);
    }
  });

  // Handle case where browser session is the last group
  if (currentGroup.length > 0) {
    result.push([...currentGroup]);
  }

  return result;
}

/**
 * Get the task message from the messages array
 */
export function getTaskMessage(
  messages: ClineMessage[]
): ClineMessage | undefined {
  return messages[0];
}

/**
 * Check if we should show the scroll to bottom button
 */
export function shouldShowScrollButton(
  disableAutoScroll: boolean,
  isAtBottom: boolean
): boolean {
  return disableAutoScroll && !isAtBottom;
}
