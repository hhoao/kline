import type { ClineMessage } from "@/shared/ExtensionMessage";
import { convertProtoToClineMessage } from "@/shared/proto-conversions/cline-message";
import type { ClineMessage as ProtoClineMessage } from "@/shared/proto/cline/ui";
import {
  clineAskFromJSON,
  ClineMessageType,
  clineSayFromJSON,
} from "@/shared/proto/cline/ui";
import { useExtensionStateStore } from "@/stores/extensionState";
import { SubscriptionMessageType } from "../../constants/subscription";
import type { PartialMessageData } from "../../types/subscription";
import type { SubscriptionHandler } from "./handler";

/**
 * 将 PartialMessageData 转换为 proto 格式的 ClineMessage
 */
function convertPartialMessageDataToProto(
  message: PartialMessageData
): ProtoClineMessage | null {
  // 获取时间戳
  const timestamp = message.ts || Date.now();
  if (!timestamp || timestamp <= 0) {
    console.warn("[PartialMessageHandler] 无效的时间戳:", message);
    return null;
  }

  // 从单独字段构建 proto 消息
  // 确定消息类型
  let messageType = ClineMessageType.SAY;
  if (message.clineMessageType === "ask") {
    messageType = ClineMessageType.ASK;
  }

  // 构建文本内容
  let text = "";
  if (message.incrementContent) {
    text = message.incrementContent;
  }

  const protoMessage: ProtoClineMessage = {
    ts: timestamp,
    type: messageType,
    ask: clineAskFromJSON(message.ask?.toUpperCase()),
    say: clineSayFromJSON(message.say?.toUpperCase()),
    text: text,
    reasoning: message.reasoning || "",
    images: message.images || [],
    files: message.files || [],
    partial: true,
    lastCheckpointHash: "",
    isCheckpointCheckedOut: false,
    isOperationOutsideWorkspace: false,
    conversationHistoryIndex: 0,
    conversationHistoryDeletedRange: undefined,
  };

  return protoMessage;
}

/**
 * 部分消息处理器类
 */
export class PartialMessageHandler
  implements SubscriptionHandler<PartialMessageData>
{
  /**
   * 获取消息类型
   */
  getMessageType(): string {
    return SubscriptionMessageType.PARTIAL_MESSAGE;
  }

  /**
   * 处理部分消息
   * @param message 部分消息数据
   */
  handle(message: PartialMessageData): void {
    try {
      const extensionStateStore = useExtensionStateStore();

      // 检查 taskId 是否匹配（如果不匹配，忽略此消息）
      const currentTaskId = extensionStateStore.conversationId;
      if (message.taskId && currentTaskId && message.taskId !== currentTaskId) {
        console.debug("[PartialMessageHandler] 任务ID不匹配，跳过消息:", {
          messageTaskId: message.taskId,
          currentTaskId: currentTaskId,
        });
        return;
      }

      const extensionState = extensionStateStore.extensionState;
      if (!extensionState) {
        console.debug("[PartialMessageHandler] 扩展状态不存在，跳过部分消息");
        return;
      }

      const timestamp = message.ts || Date.now();
      if (!timestamp || timestamp <= 0) {
        console.warn("[PartialMessageHandler] 无效的时间戳:", message);
        return;
      }

      const messages = extensionState.clineMessages || [];

      // 判断是更新现有消息还是新增消息
      let targetIndex = -1;

      // 如果 isUpdatingPreviousPartial 为 true，尝试查找要更新的目标消息
      if (message.isUpdatingPreviousPartial === true) {
        const exactIndex = messages.findIndex((msg) => msg.ts === timestamp);

        // 计算要更新的目标消息索引：
        // 1）优先用精确 ts 匹配
        // 2）如果后端在流式过程中不断变化 ts，则回退到"最后一条 partial 消息"
        targetIndex = exactIndex;
        if (targetIndex < 0 && messages.length > 0) {
          const lastIndex = messages.length - 1;
          const lastMessage = messages[lastIndex];

          // 如果最后一条消息本身就是 partial，则认为是这次增量的目标
          if (lastMessage.partial) {
            targetIndex = lastIndex;
          }
        }
      }

      // 找到了要更新的目标消息：执行增量合并
      if (targetIndex >= 0) {
        const existingMessage = messages[targetIndex];

        let mergedText = existingMessage.text || "";

        if (
          message.incrementContent != null &&
          message.incrementContent !== ""
        ) {
          const inc = message.incrementContent;

          const shouldUseJsonDiff = message.format === "json";

          if (shouldUseJsonDiff) {
            try {
              const prevJson = existingMessage.text
                ? JSON.parse(existingMessage.text)
                : {};
              const deltaJson = JSON.parse(inc);

              const nextJson: any = { ...prevJson };

              Object.entries(deltaJson).forEach(([key, value]) => {
                const prevVal = (prevJson as any)[key];
                if (key === "tool" || key === "followup") {
                  nextJson[key] = value;
                } else {
                  if (typeof value === "string") {
                    const prevStr = typeof prevVal === "string" ? prevVal : "";
                    (nextJson as any)[key] = prevStr + value;
                  } else if (Array.isArray(value)) {
                    const prevArr = Array.isArray(prevVal) ? prevVal : [];
                    const mergedArr: any[] = [];
                    
                    for (let i = 0; i < value.length; i++) {
                      const deltaItem = value[i];
                      const prevItem = i < prevArr.length ? prevArr[i] : null;
                      
                      if (deltaItem === null) {
                        mergedArr.push(prevItem);
                      } else if (typeof deltaItem === "string" && typeof prevItem === "string") {
                        mergedArr.push(prevItem + deltaItem);
                      } else {
                        mergedArr.push(deltaItem);
                      }
                    }
                    
                    (nextJson as any)[key] = mergedArr;
                  } else {
                    // 其它类型直接覆盖
                    (nextJson as any)[key] = value;
                  }
                }
              });

              mergedText = JSON.stringify(nextJson);
            } catch (e) {
              console.error(
                "[PartialMessageHandler] JSON merge failed for partial message, fallback to text append:",
                e,
                message
              );
              mergedText = (existingMessage.text || "") + inc;
            }
          } else {
            // 普通文本部分消息：直接在末尾追加
            mergedText = (existingMessage.text || "") + inc;
          }
        }

        const updatedMessage: ClineMessage = {
          ...existingMessage,
          ts: existingMessage.ts,
          ...(message.clineMessageType != null && {
            type:
              message.clineMessageType === "ask" ? "ask" : "say",
          }),
          ...(message.ask != null && { ask: message.ask || undefined }),
          ...(message.say != null && { say: message.say || undefined }),
          text: mergedText,
          ...(message.reasoning != null && {
            reasoning: message.reasoning || undefined,
          }),
          ...(message.images != null && { images: message.images || [] }),
          ...(message.files != null && { files: message.files || [] }),
          ...(typeof message.commandCompleted === "boolean" && {
            commandCompleted: message.commandCompleted,
            partial: !message.commandCompleted,
          }),
        };

        // 这里直接更新 clineMessages，避免依赖 ts 精确匹配
        const newClineMessages = [...messages];
        newClineMessages[targetIndex] = updatedMessage;
        extensionStateStore.extensionState = {
          ...extensionState,
          clineMessages: newClineMessages,
        };
        return;
      } else {
        // 没有找到要更新的消息（isUpdatingPreviousPartial 为 false 或找不到目标消息）：创建新的 partial 消息
        const protoMessage = convertPartialMessageDataToProto(message);
        if (!protoMessage) {
          return;
        }

        const newMessageFromProto = convertProtoToClineMessage(protoMessage);
        const finalMessage: ClineMessage = {
          ...newMessageFromProto,
          ...(typeof message.commandCompleted === "boolean" && {
            commandCompleted: message.commandCompleted,
            partial: !message.commandCompleted,
          }),
        };

        const newClineMessages = [...messages, finalMessage];
        extensionStateStore.extensionState = {
          ...extensionState,
          clineMessages: newClineMessages,
        };
      }
    } catch (error) {
      console.error("[PartialMessageHandler] 处理部分消息失败:", error);
    }
  }
}

/**
 * 导出处理器实例（单例）
 */
export const partialMessageHandler = new PartialMessageHandler();
