package com.hhoa.kline.core.core.task.handler;

import static com.hhoa.kline.core.core.integrations.misc.ExtractText.processFilesIntoText;

import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.context.tracking.EnvironmentContextTracker;
import com.hhoa.kline.core.core.hooks.HookExecutionResult;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.TaskHookSupport;
import com.hhoa.kline.core.core.task.TaskState;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2StartTaskHandler {

    private final MessageStateHandler messageStateHandler;
    private final Runnable postStateToWebview;
    private final TaskV2SayAskHandler sayAskHandler;
    private final TaskState taskState;
    private final TaskHookSupport hookSupport;
    private final EnvironmentContextTracker environmentContextTracker;
    private final Supplier<String> versionSupplier;

    public TaskV2StartTaskHandler(
            MessageStateHandler messageStateHandler,
            Runnable postStateToWebview,
            TaskV2SayAskHandler sayAskHandler,
            TaskState taskState,
            TaskHookSupport hookSupport,
            EnvironmentContextTracker environmentContextTracker,
            Supplier<String> versionSupplier) {
        this.messageStateHandler = messageStateHandler;
        this.postStateToWebview = postStateToWebview;
        this.sayAskHandler = sayAskHandler;
        this.taskState = taskState;
        this.hookSupport = hookSupport;
        this.environmentContextTracker = environmentContextTracker;
        this.versionSupplier = versionSupplier;
    }

    /** 启动新任务。返回 true 表示正常继续，返回 false 表示被 hook 取消。 */
    public boolean startTask(String taskText, List<String> images, List<String> files) {
        messageStateHandler.setClineMessages(new ArrayList<>());
        messageStateHandler.getApiConversationHistory().clear();
        if (postStateToWebview != null) {
            postStateToWebview.run();
        }
        sayAskHandler.say(ClineSay.TASK, taskText, images, files, null);
        taskState.setInitialized(true);

        List<UserContentBlock> userContent = new ArrayList<>();

        if (taskText != null && !taskText.isEmpty()) {
            TextContentBlock textBlock =
                    new TextContentBlock(String.format("<task>\n%s\n</task>", taskText));
            userContent.add(textBlock);
        }

        if (images != null && !images.isEmpty()) {
            for (String image : images) {
                ImageContentBlock imageBlock = new ImageContentBlock(image, "base64", "image/png");
                userContent.add(imageBlock);
            }
        }

        if (files != null && !files.isEmpty()) {
            String fileContentString =
                    processFilesIntoText(files.stream().map(Path::of).collect(Collectors.toList()));
            if (!fileContentString.isEmpty()) {
                TextContentBlock fileBlock = new TextContentBlock(fileContentString);
                userContent.add(fileBlock);
            }
        }

        // Execute TaskStart hook
        if (hookSupport != null) {
            HookExecutionResult taskStartResult = hookSupport.executeTaskStartHook(taskText);

            if (taskStartResult.getCancel() != null && taskStartResult.getCancel()) {
                hookSupport.handleHookCancellation("TaskStart", taskStartResult.isWasCancelled());
                return false;
            }

            // Inject hook context modification
            if (taskStartResult.getContextModification() != null
                    && !taskStartResult.getContextModification().trim().isEmpty()) {
                userContent.add(
                        new TextContentBlock(
                                "<hook_context source=\"TaskStart\">\n"
                                        + taskStartResult.getContextModification().trim()
                                        + "\n</hook_context>"));
            }
        }

        // Defensive check: abort may have been set during hook execution
        if (taskState.isAbort()) {
            return false;
        }

        // Execute UserPromptSubmit hook
        if (hookSupport != null) {
            String promptText = taskText != null ? taskText : "";
            HookExecutionResult userPromptResult =
                    hookSupport.executeUserPromptSubmitHook(promptText);

            if (taskState.isAbort()) {
                return false;
            }

            if (userPromptResult.getCancel() != null && userPromptResult.getCancel()) {
                hookSupport.handleHookCancellation(
                        "UserPromptSubmit", userPromptResult.isWasCancelled());
                return false;
            }

            if (userPromptResult.getContextModification() != null
                    && !userPromptResult.getContextModification().trim().isEmpty()) {
                userContent.add(
                        new TextContentBlock(
                                "<hook_context source=\"UserPromptSubmit\">\n"
                                        + userPromptResult.getContextModification().trim()
                                        + "\n</hook_context>"));
            }
        }

        if (environmentContextTracker != null) {
            environmentContextTracker.recordEnvironment(
                    versionSupplier != null ? versionSupplier.get() : "unknown");
        }

        taskState.setCurrentUserContent(userContent);
        taskState.setCurrentIncludeFileDetails(true);
        return true;
    }
}
