package com.hhoa.kline.core.core.task.tools.utils;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.ToolExecutorCoordinator;
import com.hhoa.kline.core.core.task.tools.handlers.HandlerUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;

/** 处理工具结果与用户反馈的辅助方法。 */
@Slf4j
public final class ToolResultUtils {

    private ToolResultUtils() {}

    public static void pushToolResult(
            List<UserContentBlock> toolResult,
            ToolUse block,
            List<UserContentBlock> userMessageContent,
            Function<ToolUse, String> toolDescription,
            Runnable markToolAsUsed,
            ToolExecutorCoordinator coordinator) {
        toolResult =
                toolResult.isEmpty()
                        ? HandlerUtils.createTextBlocks("(tool did not return anything)")
                        : toolResult;
        String description;
        if (coordinator != null) {
            ToolExecutorCoordinator.ToolHandler handler = coordinator.getHandler(block.getName());
            description =
                    handler != null ? handler.getDescription(block) : toolDescription.apply(block);
        } else {
            description = toolDescription.apply(block);
        }

        TextContentBlock headerBlock = new TextContentBlock(description + " Result:");
        userMessageContent.add(headerBlock);

        userMessageContent.addAll(toolResult);
        if (markToolAsUsed != null) {
            markToolAsUsed.run();
        }
    }

    public static void pushToolResult(
            String content,
            ToolUse block,
            List<UserContentBlock> userMessageContent,
            Function<ToolUse, String> toolDescription,
            Runnable markToolAsUsed,
            ToolExecutorCoordinator coordinator) {
        pushToolResult(
                content.isEmpty() ? new ArrayList<>() : HandlerUtils.createTextBlocks(content),
                block,
                userMessageContent,
                toolDescription,
                markToolAsUsed,
                coordinator);
    }

    public static void pushAdditionalToolFeedback(
            List<UserContentBlock> userMessageContent,
            String feedback,
            String[] images,
            String fileContentString) {
        boolean hasAny =
                (feedback != null && !feedback.isEmpty())
                        || (images != null && images.length > 0)
                        || (fileContentString != null && !fileContentString.isEmpty());
        if (!hasAny) return;

        String sb =
                "The user provided the following feedback:\n<feedback>\n"
                        + (feedback == null ? "" : feedback)
                        + "\n</feedback>";

        TextContentBlock textBlock = new TextContentBlock(sb);
        userMessageContent.add(textBlock);

        if (fileContentString != null && !fileContentString.isEmpty()) {
            userMessageContent.add(new TextContentBlock(fileContentString));
        }
    }

    public static Boolean askApprovalAndPushFeedback(
            ClineAsk type, String completeMessage, TaskConfig config, ClineMessageFormat format) {
        AskResult result = config.getCallbacks().ask(type, completeMessage, false, format);
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
                    config.getTaskState().getUserMessageContent(), text, images, fileContentString);
            config.getCallbacks().sayUserFeedback(text, images, files);
        }

        if (result == null || !ClineAskResponse.YES_BUTTON_CLICKED.equals(result.getResponse())) {
            // 用户拒绝或用消息回应，视为拒绝
            config.getTaskState().setDidRejectTool(true);
            return false;
        } else {
            // 用户点击了批准按钮，且可能提供了反馈
            return true;
        }
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
