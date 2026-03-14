package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.integrations.misc.ExtractText;
import com.hhoa.kline.core.core.integrations.notifications.NotificationType;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AttemptCompletionTool;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 尝试完成任务的工具处理器 处理任务完成、用户反馈、命令执行
 *
 * @author hhoa
 */
public class AttemptCompletionHandler implements FullyManagedTool {

    private static final String COMPLETION_RESULT_CHANGES_FLAG = "HAS_CHANGES";

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.ATTEMPT.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String result = HandlerUtils.getStringParam(block, "result");
        String command = HandlerUtils.getStringParam(block, "command");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnabled()
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            String notificationMsg = result.replace("\n", " ");
            if (notificationMsg.length() > 100) {
                notificationMsg = notificationMsg.substring(0, 100) + "...";
            }
            config.getServices()
                    .getNotificationService()
                    .showNotification("Task Completed", notificationMsg, NotificationType.INFO);
        }

        List<ClineMessage> clineMessages = config.getMessageState().getClineMessages();
        ClineMessage lastMessage =
                clineMessages.isEmpty() ? null : clineMessages.get(clineMessages.size() - 1);

        List<UserContentBlock> cmdResult;
        if (command != null && !command.isEmpty()) {
            if (lastMessage == null || !ClineAsk.COMMAND.equals(lastMessage.getAsk())) {
                long l = System.currentTimeMillis();
                config.getCallbacks()
                        .say(ClineSay.COMPLETION_RESULT, result, null, null, false, null);
                config.getCallbacks().saveCheckpoint(true, l);
                addNewChangesFlagToLastCompletionResultMessage(config);
            } else {
                config.getCallbacks().saveCheckpoint(true, null);
            }

            if (!block.isPartial()) {
                String taskProgress = HandlerUtils.getStringParam(block, "task_progress");
                if (taskProgress != null) {
                    config.getCallbacks().updateFCListFromToolResponse(taskProgress);
                }
            }

            Boolean approved =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.COMMAND, command, config, null);

            if (config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(
                                config.getUlid(), block.getName(), modelId, false, approved);
            }

            if (!approved) {
                cmdResult = HandlerUtils.createTextBlocks(formatResponse.toolDenied());
            } else {
                TaskConfig.ExecuteResult exec =
                        config.getCallbacks().executeCommandTool(command, null);
                if (exec.userRejected) {
                    config.getTaskState().setDidRejectTool(true);
                }
                cmdResult = HandlerUtils.createTextBlocks(exec.result);
            }
        } else {
            long l = System.currentTimeMillis();
            config.getCallbacks().say(ClineSay.COMPLETION_RESULT, result, null, null, false, null);
            config.getCallbacks().saveCheckpoint(true, l);
            addNewChangesFlagToLastCompletionResultMessage(config);
            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                config.getServices().getTelemetryService().captureTaskCompleted(config.getUlid());
            }
            cmdResult = null;
        }

        List<ClineMessage> currentMessages = config.getMessageState().getClineMessages();
        if (!currentMessages.isEmpty()) {
            ClineMessage lastMsg = currentMessages.getLast();
            if (ClineAsk.COMMAND_OUTPUT.equals(lastMsg.getAsk())) {
                config.getCallbacks().say(ClineSay.COMMAND_OUTPUT, "", null, null, false, null);
            }
        }

        if (!block.isPartial()) {
            String taskProgress = HandlerUtils.getStringParam(block, "task_progress");
            if (taskProgress != null) {
                config.getCallbacks().updateFCListFromToolResponse(taskProgress);
            }
        }

        AskResult res = config.getCallbacks().ask(ClineAsk.COMPLETION_RESULT, "", false, null);
        if (res != null && ClineAskResponse.YES_BUTTON_CLICKED.equals(res.getResponse())) {
            return HandlerUtils.createTextBlocks("");
        }

        String text = res != null ? res.getText() : null;
        String[] images =
                res != null && res.getImages() != null
                        ? res.getImages().toArray(new String[0])
                        : null;
        String[] completionFiles =
                res != null && res.getFiles() != null
                        ? res.getFiles().toArray(new String[0])
                        : null;

        config.getCallbacks()
                .say(
                        ClineSay.USER_FEEDBACK,
                        text == null ? "" : text,
                        images,
                        completionFiles,
                        false,
                        null);

        List<UserContentBlock> toolResults = new ArrayList<>();

        if (cmdResult != null) {
            toolResults.addAll(cmdResult);
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

        return toolResults;
    }

    private void addNewChangesFlagToLastCompletionResultMessage(TaskConfig config) {
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
