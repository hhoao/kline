package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.MessageRole;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.context.management.KeepStrategy;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.notifications.NotificationType;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class CondenseHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class CondenseToolState extends ToolState {
        private String context;
    }

    @Override
    public ToolState createToolState() {
        return new CondenseToolState();
    }

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
    public ToolExecuteResult execute(ToolContext toolContext, ToolUse block) {
        String context = HandlerUtils.getStringParam(block, "context");

        if (toolContext.getAutoApprovalSettings() != null
                && toolContext.getAutoApprovalSettings().isEnabled()
                && toolContext.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg =
                    "Cline is suggesting to condense your conversation with: " + context;
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            toolContext
                    .getServices()
                    .getNotificationService()
                    .showNotification(
                            "Cline wants to condense the conversation...",
                            notificationMsg,
                            NotificationType.INFO);
        }

        CondenseToolState state = (CondenseToolState) toolContext.getToolState();
        state.setPhase(1);
        state.setContext(context);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.CONDENSE,
                        context,
                        toolContext,
                        null,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {

        String text = askResult != null ? askResult.getText() : null;
        String[] images =
                askResult != null && askResult.getImages() != null
                        ? askResult.getImages().toArray(new String[0])
                        : null;
        String[] files =
                askResult != null && askResult.getFiles() != null
                        ? askResult.getFiles().toArray(new String[0])
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

            context.getCallbacks()
                    .say(
                            ClineSay.USER_FEEDBACK,
                            text == null ? "" : text,
                            images,
                            files,
                            false,
                            null);

            String feedbackText = text == null ? "" : text;
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(
                            "The user provided feedback on the condensed conversation summary:\n<feedback>\n"
                                    + feedbackText
                                    + "\n</feedback>",
                            images,
                            fileContentString));
        }

        List<MessageParam> apiConversationHistory =
                context.getMessageState().getApiConversationHistory();

        boolean summaryAlreadyAppended = false;
        if (!apiConversationHistory.isEmpty()) {
            MessageParam lastMsg = apiConversationHistory.get(apiConversationHistory.size() - 1);
            summaryAlreadyAppended = lastMsg.getRole() == MessageRole.ASSISTANT;
        }

        KeepStrategy keepStrategy =
                summaryAlreadyAppended ? KeepStrategy.LAST_TWO : KeepStrategy.NONE;

        int[] currentDeletedRange = context.getTaskState().getConversationHistoryDeletedRange();
        int[] newDeletedRange =
                context.getServices()
                        .getContextManager()
                        .getNextTruncationRange(
                                apiConversationHistory, currentDeletedRange, keepStrategy);

        context.getTaskState().setConversationHistoryDeletedRange(newDeletedRange);

        context.getMessageState().saveClineMessagesAndUpdateHistory();

        context.getServices()
                .getContextManager()
                .triggerApplyStandardContextTruncationNoticeChange(
                        System.currentTimeMillis(), apiConversationHistory);

        if (context.getServices() != null && context.getServices().getTelemetryService() != null) {
            String modelId =
                    context.getApi() != null && context.getApi().getModel() != null
                            ? context.getApi().getModel().getId()
                            : "unknown";
            context.getServices()
                    .getTelemetryService()
                    .captureToolUsage(context.getUlid(), block.getName(), modelId, false, true);
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(formatResponse.condense(), null, null));
    }
}
