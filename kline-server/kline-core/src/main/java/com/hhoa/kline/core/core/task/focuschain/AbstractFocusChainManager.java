package com.hhoa.kline.core.core.task.focuschain;

import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.TaskState;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractFocusChainManager implements FocusChainManager {

    protected final String taskId;
    protected final TaskState taskState;
    protected final StateManager stateManager;
    protected final SayCallback say;
    protected final TelemetryService telemetryService;

    @FunctionalInterface
    public interface SayCallback {
        void say(
                ClineSay type,
                String text,
                List<String> images,
                List<String> files,
                Boolean partial);
    }

    private boolean hasTrackedFirstProgress = false;

    protected final FocusChainSettings focusChainSettings;

    public AbstractFocusChainManager(
            String taskId,
            TaskState taskState,
            StateManager stateManager,
            SayCallback say,
            TelemetryService telemetryService) {
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.taskState = Objects.requireNonNull(taskState, "taskState");
        this.stateManager = Objects.requireNonNull(stateManager, "stateManager");
        this.say = say;
        this.telemetryService = telemetryService;
        this.focusChainSettings = stateManager.getSettings().getFocusChainSettings();
    }

    @Override
    public boolean shouldIncludeFocusChainInstructions() {
        if (!focusChainSettings.isEnabled()) {
            return false;
        }

        Mode mode = stateManager.getSettings().getMode();
        boolean inPlanMode = Mode.PLAN.equals(mode);

        boolean justSwitchedFromPlanMode = taskState.isDidRespondToPlanAskBySwitchingMode();

        boolean userUpdatedList = taskState.isTodoListWasUpdatedByUser();

        int remindInterval = focusChainSettings.getRemindClineInterval();
        boolean reachedReminderInterval =
                taskState.getApiRequestsSinceLastTodoUpdate() >= remindInterval;

        boolean isFirstApiRequest =
                taskState.getApiRequestCount() == 1
                        && taskState.getCurrentFocusChainChecklist() == null;

        boolean hasNoTodoListAfterMultipleRequests =
                taskState.getCurrentFocusChainChecklist() == null
                        && taskState.getApiRequestCount() >= 2;

        return reachedReminderInterval
                || justSwitchedFromPlanMode
                || userUpdatedList
                || inPlanMode
                || isFirstApiRequest
                || hasNoTodoListAfterMultipleRequests;
    }

    @Override
    public String generateFocusChainInstructions() {
        String currentList = taskState.getCurrentFocusChainChecklist();

        if (currentList != null && !currentList.isBlank()) {
            FocusChainUtils.TodoListCounts counts =
                    FocusChainUtils.parseFocusChainListCounts(currentList);
            int totalItems = counts.totalItems();
            int completedItems = counts.completedItems();
            int percentComplete =
                    totalItems > 0 ? Math.round((float) (completedItems * 100) / totalItems) : 0;

            String introUpdateRequired =
                    "# task_progress UPDATE REQUIRED - You MUST include the task_progress parameter in your NEXT tool call.";
            String listCurrentProgress =
                    "**Current Progress: "
                            + completedItems
                            + "/"
                            + totalItems
                            + " items completed ("
                            + percentComplete
                            + "%)**";

            if (taskState.isTodoListWasUpdatedByUser()) {
                String userHasUpdatedList =
                        "**CRITICAL INFORMATION:** The user has modified this todo list - review ALL changes carefully";
                return "\n\n"
                        + introUpdateRequired
                        + "\n"
                        + listCurrentProgress
                        + "\n"
                        + "\n"
                        + currentList
                        + "\n"
                        + userHasUpdatedList
                        + "\n"
                        + FocusChainPrompts.REMINDER;
            } else {
                String progressBasedMessageStub = "";
                if (completedItems == 0 && totalItems > 0) {
                    progressBasedMessageStub =
                            """


                            **Note:** No items are marked complete yet. As you work through the task, remember to mark items as complete when finished.""";
                } else if (completedItems == totalItems && totalItems > 0) {
                    progressBasedMessageStub =
                            MessageFormat.format(
                                    FocusChainPrompts.COMPLETED, totalItems, currentList);
                } else if (percentComplete >= 75) {
                    progressBasedMessageStub =
                            "\n\n**Note:** "
                                    + percentComplete
                                    + "% of items are complete! Focus on finishing the remaining items.";
                } else if (percentComplete >= 50) {
                    progressBasedMessageStub =
                            "\n\n**Note:** "
                                    + percentComplete
                                    + "% of items are complete. Proceed with the task.";
                } else if (percentComplete >= 25) {
                    progressBasedMessageStub =
                            "\n\n**Note:** " + percentComplete + "% of items are complete.";
                }

                return "\n"
                        + introUpdateRequired
                        + "\n"
                        + listCurrentProgress
                        + "\n"
                        + currentList
                        + "\n"
                        + "\n"
                        + FocusChainPrompts.REMINDER
                        + "\n"
                        + progressBasedMessageStub;
            }
        } else if (taskState.isDidRespondToPlanAskBySwitchingMode()) {
            return FocusChainPrompts.INITIAL;
        } else if (Mode.PLAN.equals(stateManager.getSettings().getMode())) {
            return FocusChainPrompts.PLAN_MODE_REMINDER;
        } else {
            boolean isEarlyInTask = taskState.getApiRequestCount() < 10;
            if (isEarlyInTask) {
                return FocusChainPrompts.RECOMMENDED;
            } else {
                return MessageFormat.format(
                        FocusChainPrompts.API_REQUEST_COUNT, taskState.getApiRequestCount());
            }
        }
    }

    @Override
    public void updateFCListFromToolResponse(String taskProgress) {
        try {
            if (taskProgress != null && !taskProgress.trim().isEmpty()) {
                taskState.setApiRequestsSinceLastTodoUpdate(0);
            }

            if (taskProgress != null && !taskProgress.trim().isEmpty()) {
                String trimmed = taskProgress.trim();
                String previousList = taskState.getCurrentFocusChainChecklist();

                taskState.setCurrentFocusChainChecklist(trimmed);
                log.debug(
                        "[Task {}] focus chain list: LLM provided focus chain list update via task_progress parameter. Length {} > {}",
                        taskId,
                        previousList != null ? previousList.length() : 0,
                        trimmed.length());

                FocusChainUtils.TodoListCounts counts =
                        FocusChainUtils.parseFocusChainListCounts(trimmed);
                int totalItems = counts.totalItems();
                int completedItems = counts.completedItems();

                if (!hasTrackedFirstProgress && totalItems > 0) {
                    if (telemetryService != null) {
                        telemetryService.captureFocusChainProgressFirst(this.taskId, totalItems);
                    }
                    hasTrackedFirstProgress = true;
                } else if (hasTrackedFirstProgress && totalItems > 0) {
                    if (telemetryService != null) {
                        telemetryService.captureFocusChainProgressUpdate(
                                this.taskId, totalItems, completedItems);
                    }
                }

                try {
                    saveFocusChain(trimmed);

                    if (say != null) {
                        say.say(ClineSay.TASK_PROGRESS, trimmed, null, null, false);
                        return;
                    }
                } catch (Exception error) {
                    log.error(
                            "[Task {}] focus chain list: Failed to write to markdown file",
                            taskId,
                            error);
                    if (say != null) {
                        say.say(ClineSay.TASK_PROGRESS, trimmed, null, null, false);
                        log.debug(
                                "[Task {}] focus chain list: Sent fallback task_progress message to UI",
                                taskId);
                    }
                }
            } else {
                String markdownTodoList = getFocusChain();
                if (markdownTodoList != null) {
                    taskState.setCurrentFocusChainChecklist(markdownTodoList);

                    if (say != null) {
                        say.say(ClineSay.TASK_PROGRESS, markdownTodoList, null, null, false);
                        return;
                    }
                } else {
                    log.debug(
                            "[Task {}] focus chain list: No valid task progress to update with",
                            taskId);
                }
            }
        } catch (Exception error) {
            log.error(
                    "[Task {}] focus chain list: Error in updateFCListFromToolResponse",
                    taskId,
                    error);
        }
    }

    abstract String getFocusChain();

    abstract void saveFocusChain(String todoList) throws IOException;

    @Override
    public void checkIncompleteProgressOnCompletion() {
        if (focusChainSettings.isEnabled() && taskState.getCurrentFocusChainChecklist() != null) {
            FocusChainUtils.TodoListCounts counts =
                    FocusChainUtils.parseFocusChainListCounts(
                            taskState.getCurrentFocusChainChecklist());

            if (counts.totalItems() > 0 && counts.completedItems() < counts.totalItems()) {
                int incompleteItems = counts.totalItems() - counts.completedItems();
                if (telemetryService != null) {
                    telemetryService.captureFocusChainIncompleteOnCompletion(
                            this.taskId,
                            counts.totalItems(),
                            counts.completedItems(),
                            incompleteItems);
                }
            }
        }
    }
}
