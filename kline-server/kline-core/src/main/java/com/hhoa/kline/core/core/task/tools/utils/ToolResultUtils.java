package com.hhoa.kline.core.core.task.tools.utils;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.handlers.HandlerUtils;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/** 处理工具结果与用户反馈的辅助方法。 */
@Slf4j
public final class ToolResultUtils {

    private ToolResultUtils() {}

    public static void pushToolResult(
            List<UserContentBlock> toolResult,
            List<UserContentBlock> userMessageContent,
            String toolDescription,
            Runnable markToolAsUsed) {
        toolResult =
                toolResult.isEmpty()
                        ? HandlerUtils.createTextBlocks("(tool did not return anything)")
                        : toolResult;

        TextContentBlock headerBlock = new TextContentBlock(toolDescription + " Result:");
        userMessageContent.add(headerBlock);

        userMessageContent.addAll(toolResult);
        if (markToolAsUsed != null) {
            markToolAsUsed.run();
        }
    }

    public static void pushToolResult(
            String content,
            List<UserContentBlock> userMessageContent,
            String toolDescription,
            Runnable markToolAsUsed) {
        pushToolResult(
                content.isEmpty() ? new ArrayList<>() : HandlerUtils.createTextBlocks(content),
                userMessageContent,
                toolDescription,
                markToolAsUsed);
    }

    public static void pushAdditionalToolFeedback(
            List<UserContentBlock> userMessageContent,
            String feedback,
            String[] images,
            String fileContentString) {
        boolean hasMeaningfulFeedback = feedback != null && !feedback.trim().isEmpty();
        boolean hasImages = images != null && images.length > 0;
        boolean hasMeaningfulFileContent =
                fileContentString != null && !fileContentString.trim().isEmpty();

        if (!hasMeaningfulFeedback && !hasImages && !hasMeaningfulFileContent) {
            return;
        }

        String feedbackText =
                hasMeaningfulFeedback
                        ? "The user provided the following feedback:\n<feedback>\n"
                                + feedback
                                + "\n</feedback>"
                        : "The user provided additional content:";

        TextContentBlock textBlock = new TextContentBlock(feedbackText);
        userMessageContent.add(textBlock);

        if (hasMeaningfulFileContent) {
            userMessageContent.add(new TextContentBlock(fileContentString));
        }
    }

    public static PendingAskToken.ToolUsePendingAskToken askApprovalAndPushFeedbackForToken(
            ClineAsk type,
            String completeMessage,
            ToolContext config,
            ClineMessageFormat format,
            ToolUse toolUse,
            String toolDescription) {
        // Subagent execution auto-approves all tools
        if (config.isSubagentExecution()) {
            return null;
        }
        AskPending askResult = config.getCallbacks().ask(type, completeMessage, false, format);
        PendingAskToken.ToolUsePendingAskToken token =
                new PendingAskToken.ToolUsePendingAskToken(
                        askResult.getPendingId(),
                        config.getTaskId(),
                        toolDescription,
                        type,
                        completeMessage,
                        format,
                        toolUse);
        return token;
    }

    /** 处理 ask 恢复后的用户响应。从 AskResult 中提取反馈并推送，返回用户是否批准。 由 StateFullToolHandler.resume() 调用。 */
    public static boolean processAskResult(AskResult result, ToolContext config) {
        String text = result != null ? result.getText() : null;
        String[] images =
                result != null && result.getImages() != null
                        ? result.getImages().toArray(new String[0])
                        : null;
        String[] files =
                result != null && result.getFiles() != null
                        ? result.getFiles().toArray(new String[0])
                        : null;

        boolean hasText = text != null && !text.isEmpty();
        boolean hasImages = images != null && images.length > 0;
        boolean hasFiles = files != null && files.length > 0;
        if (hasText || hasImages || hasFiles) {
            String fileContentString = hasFiles ? processFilesIntoText(files) : "";
            pushAdditionalToolFeedback(
                    config.getTaskState().getNextUserMessageContent(),
                    text,
                    images,
                    fileContentString);
            config.getCallbacks().sayUserFeedback(text, images, files);
        }

        if (result == null || !ClineAskResponse.YES_BUTTON_CLICKED.equals(result.getResponse())) {
            config.getTaskState().setDidRejectTool(true);
            return false;
        }
        return true;
    }

    private static String processFilesIntoText(String[] files) {
        if (files == null || files.length == 0) return "";
        List<String> parts = new ArrayList<>();
        for (String f : files) {
            try {
                String content = Files.readString(Path.of(f), StandardCharsets.UTF_8);
                parts.add("<file path=\"" + f + "\">\n" + content + "\n</file>");
            } catch (IOException ignored) {
                parts.add("<file path=\"" + f + "\">(failed to read)</file>");
            }
        }
        return String.join("\n\n", parts);
    }
}
