package com.hhoa.kline.core.core.task.handler;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskState;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2ResumeHandler {

    private final ResponseFormatter responseFormatter;
    private final StateManager stateManager;
    private final FileContextTracker fileContextTracker;
    private final String taskId;
    private final String cwd;
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;
    private final Supplier<ICheckpointManager> getCheckpointManager;
    private final TaskV2SayAskHandler sayAskHandler;

    public TaskV2ResumeHandler(
            ResponseFormatter responseFormatter,
            StateManager stateManager,
            FileContextTracker fileContextTracker,
            String taskId,
            String cwd,
            TaskState taskState,
            MessageStateHandler messageStateHandler,
            Supplier<ICheckpointManager> getCheckpointManager,
            TaskV2SayAskHandler sayAskHandler) {
        this.responseFormatter = responseFormatter;
        this.stateManager = stateManager;
        this.fileContextTracker = fileContextTracker;
        this.taskId = taskId;
        this.cwd = cwd;
        this.taskState = taskState;
        this.messageStateHandler = messageStateHandler;
        this.getCheckpointManager = getCheckpointManager;
        this.sayAskHandler = sayAskHandler;
    }

    //
    //    public TaskStatus resumeFromAsk(AskResult askResult, ResumeContext ctx) {
    //        ResumePoint point = ctx.getResumePoint();
    //        switch (point) {
    //            case RESUME_TASK_HISTORY:
    //                return resumeAfterResumeTaskAsk(askResult, ctx);
    //            case MISTAKE_LIMIT:
    //            case AUTO_APPROVAL_MAX:
    //                return resumeAfterPreCheckAsk(askResult, ctx);
    //            case API_REQ_FAILED:
    //                return resumeAfterApiReqFailedAsk(askResult, ctx);
    //            case EMPTY_RESPONSE:
    //                return resumeAfterEmptyResponseAsk(askResult, ctx);
    //            case TOOL_EXECUTION:
    //                return resumeAfterToolAsk(askResult, ctx);
    //            case COMMAND_OUTPUT:
    //                return resumeAfterCommandOutputAsk(askResult, ctx);
    //            default:
    //                log.warn("Unknown ResumePoint: {}, defaulting to PREPARE_CONTEXT", point);
    //                return TaskStatus.PREPARE_CONTEXT;
    //        }
    //    }
    //
    //    private TaskStatus resumeAfterResumeTaskAsk(AskResult askResult, ResumeContext ctx) {
    //        try {
    //            List<String> responseImages = null;
    //            List<String> responseFiles = null;
    //            String responseText = null;
    //
    //            if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())) {
    //                responseText = askResult.getText();
    //                responseImages =
    //                    askResult.getImages() != null
    //                        ? new ArrayList<>(askResult.getImages())
    //                        : null;
    //                responseFiles =
    //                    askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) :
    // null;
    //                sayAskHandler.say(
    //                    ClineSay.USER_FEEDBACK, responseText, responseImages, responseFiles,
    // null);
    //                ICheckpointManager cm = getCheckpointManager.get();
    //                if (cm != null) {
    //                    cm.saveCheckpoint(false, null)
    //                        .exceptionally(
    //                            error -> {
    //                                log.error(
    //                                    "Failed to save checkpoint: {}",
    //                                    error.getMessage(),
    //                                    error);
    //                                return null;
    //                            });
    //                }
    //            }
    //
    //            List<UserContentBlock> newUserContent =
    //                buildResumeUserContent(responseText, responseImages, responseFiles);
    //
    //            taskState.setResumeContext(null);
    //            taskState.setCurrentUserContent(newUserContent);
    //            taskState.setCurrentIncludeFileDetails(false);
    //            return TaskStatus.PREPARE_CONTEXT;
    //        } catch (AskSuspendException e) {
    //            throw e;
    //        } catch (Exception e) {
    //            log.error("Error resuming after resume task ask: {}", e.getMessage(), e);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //    }
    //
    //    private TaskStatus resumeAfterPreCheckAsk(AskResult askResult, ResumeContext ctx) {
    //        try {
    //            List<UserContentBlock> userContent = ctx.getUserContent();
    //            if (userContent == null) {
    //                userContent = new ArrayList<>();
    //            }
    //
    //            if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())) {
    //                List<String> imagesList =
    //                    askResult.getImages() != null
    //                        ? new ArrayList<>(askResult.getImages())
    //                        : null;
    //                List<String> filesList =
    //                    askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) :
    // null;
    //                sayAskHandler.say(
    //                    ClineSay.USER_FEEDBACK, askResult.getText(), imagesList, filesList, null);
    //
    //                List<UserContentBlock> feedbackContent = new ArrayList<>();
    //                String feedbackText = askResult.getText() != null ? askResult.getText() : "";
    //                TextContentBlock feedbackBlock;
    //                if (ctx.getResumePoint() == ResumePoint.MISTAKE_LIMIT) {
    //                    feedbackBlock =
    //                        new TextContentBlock(responseFormatter.tooManyMistakes(feedbackText));
    //                } else {
    //                    feedbackBlock =
    //                        new TextContentBlock(
    //                            responseFormatter.autoApprovalMaxReached(feedbackText));
    //                }
    //                feedbackContent.add(feedbackBlock);
    //                addImageAndFileBlocks(feedbackContent, askResult);
    //                userContent = feedbackContent;
    //            }
    //
    //            if (ctx.getResumePoint() == ResumePoint.MISTAKE_LIMIT) {
    //                taskState.setConsecutiveMistakeCount(0);
    //                taskState.setAutoRetryAttempts(0);
    //            } else {
    //                taskState.setConsecutiveAutoApprovedRequestsCount(0);
    //            }
    //
    //            taskState.setResumeContext(null);
    //            taskState.setCurrentUserContent(userContent);
    //            taskState.setCurrentIncludeFileDetails(ctx.isIncludeFileDetails());
    //            return TaskStatus.PREPARE_CONTEXT;
    //        } catch (AskSuspendException e) {
    //            throw e;
    //        } catch (Exception e) {
    //            log.error("Error resuming after pre-check ask: {}", e.getMessage(), e);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //    }
    //
    //    private TaskStatus resumeAfterApiReqFailedAsk(AskResult askResult, ResumeContext ctx) {
    //        if (ClineAskResponse.YES_BUTTON_CLICKED.equals(askResult.getResponse())) {
    //            taskState.setAutoRetryAttempts(0);
    //            taskState.setResumeContext(null);
    //            List<UserContentBlock> userContent = ctx.getUserContent();
    //            if (userContent == null) {
    //                userContent = new ArrayList<>();
    //            }
    //            taskState.setCurrentUserContent(userContent);
    //            taskState.setCurrentIncludeFileDetails(ctx.isIncludeFileDetails());
    //            return TaskStatus.PREPARE_CONTEXT;
    //        } else {
    //            taskState.setResumeContext(null);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //
    //    }
    //
    //    private TaskStatus resumeAfterEmptyResponseAsk(AskResult askResult, ResumeContext ctx) {
    //        try {
    //            if (ClineAskResponse.YES_BUTTON_CLICKED.equals(askResult.getResponse())) {
    //                taskState.setAutoRetryAttempts(0);
    //                taskState.setResumeContext(null);
    //                List<UserContentBlock> userContent = ctx.getUserContent();
    //                if (userContent == null) {
    //                    userContent = new ArrayList<>();
    //                }
    //                taskState.setCurrentUserContent(userContent);
    //                taskState.setCurrentIncludeFileDetails(false);
    //                return TaskStatus.PREPARE_CONTEXT;
    //            } else {
    //                taskState.setResumeContext(null);
    //                return TaskStatus.TASK_COMPLETE;
    //            }
    //        } catch (AskSuspendException e) {
    //            throw e;
    //        } catch (Exception e) {
    //            log.error("Error resuming after empty response ask: {}", e.getMessage(), e);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //    }
    //
    //    private TaskStatus resumeAfterToolAsk(AskResult askResult, ResumeContext ctx) {
    //        try {
    //            List<UserContentBlock> userContent = ctx.getUserContent();
    //            if (userContent == null) {
    //                userContent = new ArrayList<>();
    //            }
    //
    //            if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())
    //                || ClineAskResponse.YES_BUTTON_CLICKED.equals(askResult.getResponse())) {
    //                taskState.setResumeContext(null);
    //                List<UserContentBlock> nextUserContent =
    // taskState.getNextUserMessageContent();
    //                if (nextUserContent == null || nextUserContent.isEmpty()) {
    //                    nextUserContent = userContent;
    //                }
    //                taskState.setCurrentUserContent(nextUserContent);
    //                taskState.setCurrentIncludeFileDetails(false);
    //                return TaskStatus.PREPARE_CONTEXT;
    //            } else if (ClineAskResponse.NO_BUTTON_CLICKED.equals(askResult.getResponse())) {
    //                taskState.setDidRejectTool(true);
    //                taskState.setResumeContext(null);
    //
    //                if (askResult.getText() != null && !askResult.getText().isEmpty()) {
    //                    List<UserContentBlock> feedbackContent = new ArrayList<>();
    //                    feedbackContent.add(new TextContentBlock(askResult.getText()));
    //                    addImageAndFileBlocks(feedbackContent, askResult);
    //                    taskState.setCurrentUserContent(feedbackContent);
    //                    taskState.setCurrentIncludeFileDetails(false);
    //                    return TaskStatus.PREPARE_CONTEXT;
    //                }
    //                return TaskStatus.TASK_COMPLETE;
    //            } else {
    //                taskState.setResumeContext(null);
    //                return TaskStatus.TASK_COMPLETE;
    //            }
    //        } catch (AskSuspendException e) {
    //            throw e;
    //        } catch (Exception e) {
    //            log.error("Error resuming after tool ask: {}", e.getMessage(), e);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //    }
    //
    //    private TaskStatus resumeAfterCommandOutputAsk(AskResult askResult, ResumeContext ctx) {
    //        try {
    //            taskState.setResumeContext(null);
    //            List<UserContentBlock> nextContent = taskState.getNextUserMessageContent();
    //            if (nextContent == null || nextContent.isEmpty()) {
    //                nextContent = ctx.getUserContent();
    //            }
    //            if (nextContent == null) {
    //                nextContent = new ArrayList<>();
    //            }
    //            taskState.setCurrentUserContent(nextContent);
    //            taskState.setCurrentIncludeFileDetails(false);
    //            return TaskStatus.PREPARE_CONTEXT;
    //        } catch (AskSuspendException e) {
    //            throw e;
    //        } catch (Exception e) {
    //            log.error("Error resuming after command output ask: {}", e.getMessage(), e);
    //            return TaskStatus.TASK_COMPLETE;
    //        }
    //    }
    //
    //    private List<UserContentBlock> buildResumeUserContent(
    //        String responseText, List<String> responseImages, List<String> responseFiles) {
    //        List<MessageParam> existingApiConversationHistory =
    //            messageStateHandler.getApiConversationHistory();
    //        List<MessageParam> modifiedApiConversationHistory = new ArrayList<>();
    //        List<UserContentBlock> modifiedOldUserContent = new ArrayList<>();
    //
    //        if (!existingApiConversationHistory.isEmpty()) {
    //            MessageParam lastMessage = existingApiConversationHistory.getLast();
    //            if (lastMessage.getRole() != null) {
    //                if (lastMessage instanceof AssistantMessage) {
    //                    modifiedApiConversationHistory =
    //                        new ArrayList<>(existingApiConversationHistory);
    //                } else if (lastMessage instanceof UserMessage userMessage) {
    //                    modifiedApiConversationHistory =
    //                        new ArrayList<>(
    //                            existingApiConversationHistory.subList(
    //                                0, existingApiConversationHistory.size() - 1));
    //                    for (UserContentBlock block : userMessage.getContent()) {
    //                        if (block instanceof TextContentBlock) {
    //                            modifiedOldUserContent.add(block);
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //
    //        List<UserContentBlock> newUserContent = new ArrayList<>(modifiedOldUserContent);
    //
    //        List<ClineMessage> savedClineMessages = messageStateHandler.getClineMessages();
    //        ClineMessage lastClineMessage = null;
    //        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
    //            ClineMessage msg = savedClineMessages.get(i);
    //            if (!ClineAsk.RESUME_TASK.equals(msg.getAsk())
    //                && !ClineAsk.RESUME_COMPLETED_TASK.equals(msg.getAsk())) {
    //                lastClineMessage = msg;
    //                break;
    //            }
    //        }
    //
    //        long timestamp =
    //            lastClineMessage != null && lastClineMessage.getTs() != null
    //                ? lastClineMessage.getTs()
    //                : System.currentTimeMillis();
    //        long now = System.currentTimeMillis();
    //        String agoText = TimeUtils.getAgoText(now, timestamp);
    //        boolean wasRecent = timestamp > 0 && (now - timestamp) < 30000;
    //
    //        List<String> pendingContextWarning =
    //            fileContextTracker.retrieveAndClearPendingFileContextWarning();
    //        boolean hasPendingFileContextWarnings =
    //            pendingContextWarning != null && !pendingContextWarning.isEmpty();
    //
    //        String[] taskResumptionMessages =
    //            responseFormatter.taskResumption(
    //                stateManager.getSettings().getMode(),
    //                agoText,
    //                cwd,
    //                wasRecent,
    //                responseText,
    //                hasPendingFileContextWarnings);
    //
    //        if (taskResumptionMessages != null) {
    //            for (String message : taskResumptionMessages) {
    //                if (message != null && !message.isEmpty()) {
    //                    newUserContent.add(new TextContentBlock(message));
    //                }
    //            }
    //        }
    //
    //        if (responseImages != null && !responseImages.isEmpty()) {
    //            for (String image : responseImages) {
    //                newUserContent.add(new ImageContentBlock(image, "base64", "image/png"));
    //            }
    //        }
    //
    //        if (responseFiles != null && !responseFiles.isEmpty()) {
    //            String fileContentString =
    //                processFilesIntoText(
    //                    responseFiles.stream().map(Path::of).collect(Collectors.toList()));
    //            if (!fileContentString.isEmpty()) {
    //                newUserContent.add(new TextContentBlock(fileContentString));
    //            }
    //        }
    //
    //        messageStateHandler.overwriteApiConversationHistory(modifiedApiConversationHistory);
    //        return newUserContent;
    //    }
    //
    //    private void addImageAndFileBlocks(List<UserContentBlock> content, AskResult askResult) {
    //        if (askResult.getImages() != null && !askResult.getImages().isEmpty()) {
    //            for (String image : askResult.getImages()) {
    //                content.add(new ImageContentBlock(image, "base64", "image/png"));
    //            }
    //        }
    //        if (askResult.getFiles() != null && !askResult.getFiles().isEmpty()) {
    //            String fileContentString =
    //                processFilesIntoText(
    //                    askResult.getFiles().stream()
    //                        .map(Path::of)
    //                        .collect(Collectors.toList()));
    //            if (!fileContentString.isEmpty()) {
    //                content.add(new TextContentBlock(fileContentString));
    //            }
    //        }
    //    }

    public void resumeTaskFromHistory() {
        List<ClineMessage> savedClineMessages = stateManager.getSavedClineMessages(taskId);

        int lastRelevantIndex = -1;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (!ClineAsk.RESUME_TASK.equals(msg.getAsk())
                    && !ClineAsk.RESUME_COMPLETED_TASK.equals(msg.getAsk())) {
                lastRelevantIndex = i;
                break;
            }
        }
        if (lastRelevantIndex != -1 && lastRelevantIndex < savedClineMessages.size() - 1) {
            savedClineMessages =
                    new ArrayList<>(savedClineMessages.subList(0, lastRelevantIndex + 1));
        }

        int lastApiReqStartedIndex = -1;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                lastApiReqStartedIndex = i;
                break;
            }
        }
        if (lastApiReqStartedIndex != -1) {
            ClineMessage lastApiReqStarted = savedClineMessages.get(lastApiReqStartedIndex);
            try {
                String text = lastApiReqStarted.getText();
                ClineApiReqInfo apiReqInfo =
                        JsonUtils.readValueWithException(text, ClineApiReqInfo.class);
                if (apiReqInfo.getCost() == null && apiReqInfo.getCancelReason() == null) {
                    savedClineMessages.remove(lastApiReqStartedIndex);
                }
            } catch (Exception e) {
                savedClineMessages.remove(lastApiReqStartedIndex);
            }
        }

        messageStateHandler.overwriteClineMessages(savedClineMessages);

        List<MessageParam> savedApiConversationHistory =
                stateManager.getSavedApiConversationHistory(taskId);
        messageStateHandler.setApiConversationHistory(savedApiConversationHistory);

        ClineMessage lastClineMessage = null;
        for (int i = savedClineMessages.size() - 1; i >= 0; i--) {
            ClineMessage msg = savedClineMessages.get(i);
            if (!ClineAsk.RESUME_TASK.equals(msg.getAsk())
                    && !ClineAsk.RESUME_COMPLETED_TASK.equals(msg.getAsk())) {
                lastClineMessage = msg;
                break;
            }
        }

        ClineAsk askType;
        if (lastClineMessage != null
                && ClineAsk.COMPLETION_RESULT.equals(lastClineMessage.getAsk())) {
            askType = ClineAsk.RESUME_COMPLETED_TASK;
        } else {
            askType = ClineAsk.RESUME_TASK;
        }

        taskState.setInitialized(true);

        sayAskHandler.ask(askType, null);
    }
}
