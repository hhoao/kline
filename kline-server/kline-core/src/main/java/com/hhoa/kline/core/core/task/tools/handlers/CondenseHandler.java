package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.context.management.KeepStrategy;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.notifications.NotificationType;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话压缩（摘要）工具处理器 处理对话历史压缩、用户反馈、上下文截断
 *
 * @author hhoa
 */
public class CondenseHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineAsk.CONDENSE.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ClineToolSpec.builder()
                .name(ClineAsk.CONDENSE.getValue())
                .parameters(
                        List.of(
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("context")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build()))
                .build();
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String context = HandlerUtils.getStringParam(block, "context");

        ui.ask(ClineAsk.CONDENSE, context == null ? "" : context, block.isPartial(), null);
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String context = HandlerUtils.getStringParam(block, "context");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnabled()
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg =
                    "Cline is suggesting to condense your conversation with: " + context;
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            config.getServices()
                    .getNotificationService()
                    .showNotification(
                            "Cline wants to condense the conversation...",
                            notificationMsg,
                            NotificationType.INFO);
        }

        AskResult res = config.getCallbacks().ask(ClineAsk.CONDENSE, context, false, null);
        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] files =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
                        : null;

        boolean hasFeedback =
                (text != null && !text.isEmpty())
                        || (images != null && images.length > 0)
                        || (files != null && files.length > 0);

        if (hasFeedback) {
            String fileContentString = "";
            if (files != null && files.length > 0) {
                List<Path> filePaths = new ArrayList<>();
                for (String file : files) {
                    if (file != null && !file.isEmpty()) {
                        filePaths.add(Paths.get(file));
                    }
                }
                if (!filePaths.isEmpty()) {
                    fileContentString = ExtractText.processFilesIntoText(filePaths);
                }
            }

            config.getCallbacks()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            text == null ? "" : text,
                            images,
                            files,
                            false,
                            null);

            String feedbackText = text == null ? "" : text;
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(
                            "The user provided feedback on the condensed conversation summary:\n<feedback>\n"
                                    + feedbackText
                                    + "\n</feedback>",
                            images,
                            fileContentString));
        } else {
            List<MessageParam> apiConversationHistory =
                    config.getMessageState().getApiConversationHistory();

            boolean summaryAlreadyAppended = false;
            if (!apiConversationHistory.isEmpty()) {
                MessageParam lastMsg =
                        apiConversationHistory.get(apiConversationHistory.size() - 1);
                summaryAlreadyAppended = lastMsg.getRole() == MessageRole.ASSISTANT;
            }

            KeepStrategy keepStrategy =
                    summaryAlreadyAppended ? KeepStrategy.LAST_TWO : KeepStrategy.NONE;

            int[] currentDeletedRange = config.getTaskState().getConversationHistoryDeletedRange();
            int[] newDeletedRange =
                    config.getServices()
                            .getContextManager()
                            .getNextTruncationRange(
                                    apiConversationHistory, currentDeletedRange, keepStrategy);

            config.getTaskState().setConversationHistoryDeletedRange(newDeletedRange);

            config.getMessageState().saveClineMessagesAndUpdateHistory();

            config.getServices()
                    .getContextManager()
                    .triggerApplyStandardContextTruncationNoticeChange(
                            System.currentTimeMillis(), apiConversationHistory);

            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(config.getUlid(), block.getName(), modelId, false, true);
            }

            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(formatResponse.condense(), null, null));
        }
    }
}
