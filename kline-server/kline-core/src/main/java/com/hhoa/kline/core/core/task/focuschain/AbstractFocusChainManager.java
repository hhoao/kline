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
        String listInstructionsInitial =
                """

                # TODO LIST CREATION REQUIRED - ACT MODE ACTIVATED

                **You've just switched from PLAN MODE to ACT MODE!**

                ** IMMEDIATE ACTION REQUIRED:**
                1. Create a comprehensive todo list in your NEXT tool call
                2. Use the task_progress parameter to provide the list
                3. Format each item using markdown checklist syntax:
                \t- [ ] For tasks to be done
                \t- [x] For any tasks already completed

                **Your todo list should include:**
                   - All major implementation steps
                   - Testing and validation tasks
                   - Documentation updates if needed
                   - Final verification steps

                **Example format:**
                   - [ ] Set up project structure
                   - [ ] Implement core functionality
                   - [ ] Add error handling
                   - [ ] Write tests
                   - [ ] Test implementation
                   - [ ] Document changes

                **Remember:** Keeping the todo list updated helps track progress and ensures nothing is missed.""";

        String listInstructionsRecommended =
                """

                1. Include the task_progress parameter in your next tool call
                2. Create a comprehensive checklist of all steps needed
                3. Use markdown format: - [ ] for incomplete, - [x] for complete

                **Benefits of creating a todo list now:**
                \t- Clear roadmap for implementation
                \t- Progress tracking throughout the task
                \t- Nothing gets forgotten or missed
                \t- Users can see, monitor, and edit the plan

                **Example structure:**
                ```
                - [ ] Analyze requirements
                - [ ] Set up necessary files
                - [ ] Implement main functionality
                - [ ] Handle edge cases
                - [ ] Test the implementation
                - [ ] Verify results
                ```

                Keeping the todo list updated helps track progress and ensures nothing is missed.""";

        String listInstrunctionsReminder =
                """

                1. To create or update a todo list, include the task_progress parameter in the next tool call
                2. Review each item and update its status:
                   - Mark completed items with: - [x]
                   - Keep incomplete items as: - [ ]
                   - Add new items if you discover additional steps
                3. Modify the list as needed:
                \t\t- Add any new steps you've discovered
                \t\t- Reorder if the sequence has changed
                4. Ensure the list accurately reflects the current state

                **Remember:** Keeping the todo list updated helps track progress and ensures nothing is missed.""";

        String currentList = taskState.getCurrentFocusChainChecklist();

        if (currentList != null && !currentList.isBlank()) {
            FocusChainUtils.TodoListCounts counts =
                    FocusChainUtils.parseFocusChainListCounts(currentList);
            int totalItems = counts.totalItems();
            int completedItems = counts.completedItems();
            int percentComplete =
                    totalItems > 0 ? Math.round((float) (completedItems * 100) / totalItems) : 0;

            String introUpdateRequired =
                    "# TODO LIST UPDATE REQUIRED - You MUST include the task_progress parameter in your NEXT tool call.";
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
                        + listInstrunctionsReminder;
            } else {
                String progressBasedMessageStub = "";
                if (completedItems == 0 && totalItems > 0) {
                    progressBasedMessageStub =
                            """


                            **Note:** No items are marked complete yet. As you work through the task, remember to mark items as complete when finished.""";
                } else if (percentComplete >= 25 && percentComplete < 50) {
                    progressBasedMessageStub =
                            "\n\n**Note:** " + percentComplete + "% of items are complete.";
                } else if (percentComplete >= 50 && percentComplete < 75) {
                    progressBasedMessageStub =
                            "\n\n**Note:** "
                                    + percentComplete
                                    + "% of items are complete. Proceed with the task.";
                } else if (percentComplete >= 75) {
                    progressBasedMessageStub =
                            "\n\n**Note:** "
                                    + percentComplete
                                    + "% of items are complete! Focus on finishing the remaining items.";
                } else if (completedItems == totalItems && totalItems > 0) {
                    progressBasedMessageStub =
                            MessageFormat.format(
                                    """


                                    **\uD83C\uDF89 EXCELLENT! All {0} items have been completed!**

                                    **Completed Items:**
                                    {1}

                                    **Next Steps:**
                                    - If the task is fully complete and meets all requirements, use attempt_completion
                                    - If you''ve discovered additional work that wasn''t in the original scope (new features, improvements, edge cases, etc.), create a new task_progress list with those items
                                    - If there are related tasks or follow-up items the user might want, you can suggest them in a new checklist

                                    **Remember:** Only use attempt_completion if you''re confident the task is truly finished. If there''s any remaining work, create a new focus chain list to track it.""",
                                    totalItems, currentList);
                }

                return "\n"
                        + introUpdateRequired
                        + "\n"
                        + listCurrentProgress
                        + "\n"
                        + currentList
                        + "\n"
                        + "\n"
                        + listInstrunctionsReminder
                        + "\n"
                        + progressBasedMessageStub;
            }
        } else if (taskState.isDidRespondToPlanAskBySwitchingMode()) {
            return listInstructionsInitial;
        } else if (Mode.PLAN.equals(stateManager.getSettings().getMode())) {
            return MessageFormat.format(
                    """

                            # Todo List (Optional - Plan Mode)

                            While in PLAN MODE, if you''ve outlined concrete steps or requirements for the user, you may include a preliminary todo list using the task_progress parameter.
                            Reminder on how to use the task_progress parameter:
                            {0}""",
                    listInstrunctionsReminder);
        } else {
            boolean isEarlyInTask = taskState.getApiRequestCount() < 10;
            if (isEarlyInTask) {
                return MessageFormat.format(
                        """

                        # TODO LIST RECOMMENDED
                        When starting a new task, it is recommended to create a todo list.

                        {0}
                        """,
                        listInstructionsRecommended);
            } else {
                return MessageFormat.format(
                        """

                                # TODO LIST\s
                                You''ve made {0} API requests without a todo list. Consider creating one to track remaining work.

                                {1}
                                """,
                        taskState.getApiRequestCount(), listInstrunctionsReminder);
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
