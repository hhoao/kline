package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.notifications.NotificationType;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageUtils;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.core.tools.specs.AttemptCompletionTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 尝试完成任务的工具处理器 处理任务完成、用户反馈、命令执行 有两个 ask 阶段：phase 1 = 命令审批，phase 2 = 完成结果反馈
 *
 * @author hhoa
 */
public class AttemptCompletionHandler implements StateFullToolHandler {

    private static final String COMPLETION_RESULT_CHANGES_FLAG = "HAS_CHANGES";

    /** phase 1: 等待用户批准命令 */
    private static final int PHASE_COMMAND_ASK = 1;

    /** phase 2: 等待用户对完成结果的反馈 */
    private static final int PHASE_COMPLETION_ASK = 2;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** AttemptCompletionHandler 的阶段状态 */
    @Getter
    @Setter
    public static class AttemptCompletionToolState extends ToolState {
        private String command;
        private String result;
        private List<UserContentBlock> cmdResult;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.ATTEMPT.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new AttemptCompletionToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ToolSpec getToolSpec() {
        return AttemptCompletionTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String result = HandlerUtils.getStringParam(block, "result");
        String command = HandlerUtils.getStringParam(block, "command");

        if (command == null || command.isEmpty()) {
            ui.say(ClineSay.COMPLETION_RESULT, result, null, null, block.isPartial(), null);
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String result = HandlerUtils.getStringParam(block, "result");
        String command = HandlerUtils.getStringParam(block, "command");

        if (context.getAutoApprovalSettings() != null
                && context.getAutoApprovalSettings().isEnabled()
                && context.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg = result.replace("\n", " ");
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            context.getServices()
                    .getNotificationService()
                    .showNotification("Task Completed", notificationMsg, NotificationType.INFO);
        }

        List<ClineMessage> clineMessages = context.getMessageState().getClineMessages();
        ClineMessage lastMessage =
                clineMessages.isEmpty() ? null : clineMessages.get(clineMessages.size() - 1);

        List<UserContentBlock> cmdResult = new ArrayList<>();
        if (command != null && !command.isEmpty()) {
            if (lastMessage == null || !ClineAsk.COMMAND.equals(lastMessage.getAsk())) {
                long l = System.currentTimeMillis();
                context.getCallbacks()
                        .say(ClineSay.COMPLETION_RESULT, result, null, null, false, null);
                context.getCallbacks().saveCheckpoint(true, l);
                addNewChangesFlagToLastCompletionResultMessage(context);
            } else {
                context.getCallbacks().saveCheckpoint(true, null);
            }

            if (!block.isPartial()) {
                String taskProgress = HandlerUtils.getStringParam(block, "task_progress");
                if (taskProgress != null) {
                    context.getCallbacks().updateFCListFromToolResponse(taskProgress);
                }
            }

            // 需要 ask 用户批准命令 —— 保存状态并返回 PendingAsk
            AttemptCompletionToolState state = (AttemptCompletionToolState) context.getToolState();
            state.setPhase(PHASE_COMMAND_ASK);
            state.setCommand(command);
            state.setResult(result);

            var token =
                    ToolResultUtils.askApprovalAndPushFeedbackForToken(
                            ClineAsk.COMMAND, command, context, null, block, getDescription(block));
            return new ToolExecuteResult.PendingAsk(token);
        } else {
            long l = System.currentTimeMillis();
            context.getCallbacks().say(ClineSay.COMPLETION_RESULT, result, null, null, false, null);
            context.getCallbacks().saveCheckpoint(true, l);
            addNewChangesFlagToLastCompletionResultMessage(context);
            if (context.getServices() != null
                    && context.getServices().getTelemetryService() != null) {
                context.getServices().getTelemetryService().captureTaskCompleted(context.getUlid());
            }
            cmdResult = null;
        }

        // 无命令，直接进入完成结果 ask
        return proceedToCompletionAsk(context, block, cmdResult);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        AttemptCompletionToolState state = (AttemptCompletionToolState) toolState;

        if (state.getPhase() == PHASE_COMMAND_ASK) {
            return resumeFromCommandAsk(context, block, state, askResult);
        } else if (state.getPhase() == PHASE_COMPLETION_ASK) {
            return resumeFromCompletionAsk(context, state, askResult);
        }

        throw new IllegalStateException("Invalid phase: " + state.getPhase());
    }

    private ToolExecuteResult resumeFromCommandAsk(
            ToolContext context,
            ToolUse block,
            AttemptCompletionToolState state,
            AskResult askResult) {

        boolean approved = ToolResultUtils.processAskResult(askResult, context);

        if (context.getServices().getTelemetryService() != null) {
            String modelId =
                    context.getApi() != null && context.getApi().getModel() != null
                            ? context.getApi().getModel().getId()
                            : "unknown";
            context.getServices()
                    .getTelemetryService()
                    .captureToolUsage(context.getUlid(), block.getName(), modelId, false, approved);
        }

        List<UserContentBlock> cmdResult;
        if (!approved) {
            context.getTaskState().getToolExecutionState().setDidRejectTool(true);
            cmdResult = HandlerUtils.createTextBlocks(formatResponse.toolDenied());
        } else {
            ToolContext.ExecuteResult exec =
                    context.getCallbacks().executeCommandTool(state.getCommand(), null);
            if (exec.userRejected) {
                context.getTaskState().getToolExecutionState().setDidRejectTool(true);
            }
            cmdResult = HandlerUtils.createTextBlocks(exec.result);
        }

        state.setCmdResult(cmdResult);
        return proceedToCompletionAsk(context, block, cmdResult);
    }

    /** 进入完成结果 ask 阶段：发送 COMPLETION_RESULT ask 并返回 PendingAsk */
    private ToolExecuteResult proceedToCompletionAsk(
            ToolContext context, ToolUse block, List<UserContentBlock> cmdResult) {

        List<ClineMessage> currentMessages = context.getMessageState().getClineMessages();
        if (!currentMessages.isEmpty()) {
            ClineMessage lastMsg = currentMessages.getLast();
            if (ClineAsk.COMMAND_OUTPUT.equals(lastMsg.getAsk())) {
                context.getCallbacks().say(ClineSay.COMMAND_OUTPUT, "", null, null, false, null);
            }
        }

        if (!block.isPartial()) {
            String taskProgress = HandlerUtils.getStringParam(block, "task_progress");
            if (taskProgress != null) {
                context.getCallbacks().updateFCListFromToolResponse(taskProgress);
            }
        }

        AttemptCompletionToolState state = (AttemptCompletionToolState) context.getToolState();
        state.setPhase(PHASE_COMPLETION_ASK);
        state.setCmdResult(cmdResult);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.COMPLETION_RESULT,
                        "",
                        context,
                        null,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    private ToolExecuteResult resumeFromCompletionAsk(
            ToolContext context, AttemptCompletionToolState state, AskResult askResult) {

        if (askResult != null
                && ClineAskResponse.YES_BUTTON_CLICKED.equals(askResult.getResponse())) {
            return HandlerUtils.createToolExecuteResult("");
        }

        String text = askResult != null ? askResult.getText() : null;
        String[] images =
                askResult != null && askResult.getImages() != null
                        ? askResult.getImages().toArray(new String[0])
                        : null;
        String[] completionFiles =
                askResult != null && askResult.getFiles() != null
                        ? askResult.getFiles().toArray(new String[0])
                        : null;

        context.getCallbacks()
                .say(
                        ClineSay.USER_FEEDBACK,
                        text == null ? "" : text,
                        images,
                        completionFiles,
                        false,
                        null);

        List<UserContentBlock> toolResults = new ArrayList<>();

        if (state.getCmdResult() != null) {
            toolResults.addAll(state.getCmdResult());
        }

        TextContentBlock feedbackBlock =
                new TextContentBlock(
                        "The user has provided feedback on the results. Consider their input to continue the task, and then attempt completion again.\n<feedback>\n"
                                + (text == null ? "" : text)
                                + "\n</feedback>");
        toolResults.add(feedbackBlock);

        if (images != null) {
            for (String image : images) {
                if (image != null && !image.isEmpty()) {
                    ImageContentBlock imageBlock =
                            new ImageContentBlock(image, "base64", "image/png");
                    toolResults.add(imageBlock);
                }
            }
        }

        if (completionFiles != null && completionFiles.length > 0) {
            String fileContentString = "";
            List<Path> filePaths = new ArrayList<>();
            for (String file : completionFiles) {
                if (file != null && !file.isEmpty()) {
                    filePaths.add(Paths.get(file));
                }
            }
            if (!filePaths.isEmpty()) {
                fileContentString = ExtractText.processFilesIntoText(filePaths);
            }
            toolResults.addAll(HandlerUtils.createTextBlocks(fileContentString));
        }

        return new ToolExecuteResult.Immediate(toolResults);
    }

    private void addNewChangesFlagToLastCompletionResultMessage(ToolContext config) {
        Boolean hasNewChanges = config.getCallbacks().doesLatestTaskCompletionHaveNewChanges();
        if (!Boolean.TRUE.equals(hasNewChanges)) {
            return;
        }

        List<ClineMessage> clineMessages = config.getMessageState().getClineMessages();
        int lastCompletionResultMessageIndex =
                MessageUtils.findLastIndex(
                        clineMessages,
                        m -> m != null && ClineSay.COMPLETION_RESULT.equals(m.getSay()));

        if (lastCompletionResultMessageIndex != -1) {
            ClineMessage lastCompletionResultMessage =
                    clineMessages.get(lastCompletionResultMessageIndex);
            if (lastCompletionResultMessage != null
                    && lastCompletionResultMessage.getText() != null
                    && !lastCompletionResultMessage
                            .getText()
                            .endsWith(COMPLETION_RESULT_CHANGES_FLAG)) {
                ClineMessage update = new ClineMessage();
                update.setText(
                        lastCompletionResultMessage.getText() + COMPLETION_RESULT_CHANGES_FLAG);
                config.getMessageState()
                        .updateClineMessage(lastCompletionResultMessageIndex, update);
            }
        }
    }
}
