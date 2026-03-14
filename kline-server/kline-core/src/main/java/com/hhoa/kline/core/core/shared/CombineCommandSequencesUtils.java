package com.hhoa.kline.core.core.shared;

import com.hhoa.kline.core.core.task.ClineMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在 ClineMessage 对象数组中组合命令和 command_output 消息序列。
 *
 * <p>此函数处理 ClineMessage 对象数组，查找 'command' 消息后跟一个或多个 'command_output' 消息的序列。
 * 当找到这样的序列时，将它们组合成单个消息，合并它们的文本内容。
 *
 * @param messages 要处理的 ClineMessage 对象数组
 * @return 组合了命令序列的 ClineMessage 对象新数组
 */
public class CombineCommandSequencesUtils {
    public static final String COMMAND_OUTPUT_STRING = "Output:";

    public static final String COMMAND_REQ_APP_STRING = "REQ_APP";

    public static List<ClineMessage> combineCommandSequences(List<ClineMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ClineMessage> combinedCommands = new ArrayList<>();

        // 第一遍：将命令与其输出组合
        for (int i = 0; i < messages.size(); i++) {
            ClineMessage message = messages.get(i);
            if (message != null
                    && (ClineAsk.COMMAND.equals(message.getAsk())
                            || ClineSay.COMMAND.equals(message.getSay()))) {
                String combinedText = message.getText() != null ? message.getText() : "";
                boolean didAddOutput = false;
                int j = i + 1;

                while (j < messages.size()) {
                    ClineMessage nextMessage = messages.get(j);
                    if (nextMessage != null
                            && (ClineAsk.COMMAND.equals(nextMessage.getAsk())
                                    || ClineSay.COMMAND.equals(nextMessage.getSay()))) {
                        break;
                    }
                    if (nextMessage != null
                            && ("command_output".equals(nextMessage.getAsk())
                                    || "command_output".equals(nextMessage.getSay()))) {
                        if (!didAddOutput) {
                            combinedText += "\n" + COMMAND_OUTPUT_STRING;
                            didAddOutput = true;
                        }
                        // 处理我们收到空 command_output 的情况（例如当扩展放弃对退出命令按钮的控制时）
                        String output = nextMessage.getText() != null ? nextMessage.getText() : "";
                        if (!output.isEmpty()) {
                            combinedText += "\n" + output;
                        }
                    }
                    j++;
                }

                ClineMessage combinedMessage = new ClineMessage();
                combinedMessage.setTs(message.getTs());
                combinedMessage.setType(message.getType());
                combinedMessage.setAsk(message.getAsk());
                combinedMessage.setSay(message.getSay());
                combinedMessage.setText(combinedText);
                combinedMessage.setReasoning(message.getReasoning());
                combinedMessage.setImages(message.getImages());
                combinedMessage.setFiles(message.getFiles());
                combinedMessage.setPartial(message.getPartial());
                combinedMessage.setCommandCompleted(message.getCommandCompleted());
                combinedMessage.setLastCheckpointHash(message.getLastCheckpointHash());
                combinedMessage.setIsCheckpointCheckedOut(message.getIsCheckpointCheckedOut());
                combinedMessage.setIsOperationOutsideWorkspace(
                        message.getIsOperationOutsideWorkspace());
                combinedMessage.setConversationHistoryIndex(message.getConversationHistoryIndex());
                combinedMessage.setConversationHistoryDeletedRange(
                        message.getConversationHistoryDeletedRange());
                combinedCommands.add(combinedMessage);

                i = j - 1;
            }
        }

        // 第二遍：移除 command_outputs 并用组合的命令替换原始命令
        return messages.stream()
                .filter(
                        msg ->
                                !(msg != null
                                        && ("command_output".equals(msg.getAsk())
                                                || "command_output".equals(msg.getSay()))))
                .map(
                        msg -> {
                            if (msg != null
                                    && ("command".equals(msg.getAsk())
                                            || "command".equals(msg.getSay()))) {
                                ClineMessage combinedCommand =
                                        combinedCommands.stream()
                                                .filter(
                                                        cmd ->
                                                                cmd != null
                                                                        && cmd.getTs() != null
                                                                        && cmd.getTs()
                                                                                .equals(
                                                                                        msg
                                                                                                .getTs()))
                                                .findFirst()
                                                .orElse(null);
                                return combinedCommand != null ? combinedCommand : msg;
                            }
                            return msg;
                        })
                .collect(Collectors.toList());
    }
}
