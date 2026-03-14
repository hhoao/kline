package com.hhoa.kline.core.core.controller;

import cn.hutool.core.bean.BeanUtil;
import com.hhoa.ai.kline.commons.exception.BizException;
import com.hhoa.kline.core.core.api.TaskContext;
import com.hhoa.kline.core.core.api.TaskContextHolder;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.controller.utils.ControllerUtils;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.shared.ClineFeatureSetting;
import com.hhoa.kline.core.core.shared.DictationSettings;
import com.hhoa.kline.core.core.shared.ExtensionState;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.McpDisplayMode;
import com.hhoa.kline.core.core.shared.Platform;
import com.hhoa.kline.core.core.shared.TelemetrySetting;
import com.hhoa.kline.core.core.shared.TerminalExecutionMode;
import com.hhoa.kline.core.core.shared.api.ApiConfiguration;
import com.hhoa.kline.core.core.shared.proto.cline.BrowserSettings;
import com.hhoa.kline.core.core.shared.storage.Secrets;
import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.shared.storage.types.OpenaiReasoningEffort;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.ContextFactory;
import com.hhoa.kline.core.core.task.Task;
import com.hhoa.kline.core.core.task.TaskLockUtils;
import com.hhoa.kline.core.core.task.TaskParams;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManagerFactory;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import com.hhoa.kline.core.core.workspace.WorkspaceDetection;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.core.workspace.WorkspaceSetup;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.StateMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

@Slf4j
public class TaskManager {

    @Getter private final IMcpHub mcpHub;

    @Getter private final StateManager stateManager;

    private final SystemPromptService systemPromptService;

    private final ContextManager contextManager;

    private final ToolExecutor toolExecutor;

    private final ApiHandler apiHandler;
    private final ContextFactory contextFactory;

    private final SubscriptionManager subscriptionManager;

    private final ShellIntegrationWarningTracker shellIntegrationWarningTracker =
            new ShellIntegrationWarningTracker();

    private final Map<String, Task> tasks = new HashMap<>();

    @Getter private WorkspaceRootManager workspaceManager;

    private boolean backgroundCommandRunning = false;

    private String backgroundCommandTaskId;

    private FocusChainManagerFactory focusChainManagerFactory;

    private final ExecutorService taskExecutor;

    public TaskManager(
            IMcpHub mcpHub,
            StateManager stateManager,
            SystemPromptService systemPromptService,
            ContextManager contextManager,
            ToolExecutor toolExecutor,
            ApiHandler apiHandler,
            FocusChainManagerFactory focusChainManagerFactory,
            ContextFactory contextFactory) {
        this.mcpHub = mcpHub;
        this.stateManager = stateManager;
        this.systemPromptService = systemPromptService;
        this.contextManager = contextManager;
        this.toolExecutor = toolExecutor;
        this.apiHandler = apiHandler;
        this.contextFactory = contextFactory;
        this.subscriptionManager = DefaultSubscriptionManager.getInstance();
        this.workspaceManager = setupWorkspaceManager();
        this.focusChainManagerFactory = focusChainManagerFactory;
        this.taskExecutor =
                Executors.newCachedThreadPool(
                        new ThreadFactory() {
                            private final AtomicInteger threadNumber = new AtomicInteger(1);

                            @Override
                            public Thread newThread(Runnable r) {
                                Thread t =
                                        new Thread(
                                                r,
                                                "TaskManager-TaskExecutor-"
                                                        + threadNumber.getAndIncrement());
                                t.setDaemon(true);
                                return t;
                            }
                        });
    }

    public String initTask(
            String text,
            List<String> images,
            List<String> files,
            HistoryItem historyItem,
            Settings taskSettings) {
        try {
            AutoApprovalSettings autoApprovalSettings =
                    stateManager.getSettings().getAutoApprovalSettings();

            if (autoApprovalSettings != null) {
                int currentVersion = autoApprovalSettings.getVersion();
                autoApprovalSettings.setVersion(currentVersion + 1);
                stateManager.getSettings().setAutoApprovalSettings(autoApprovalSettings);
                stateManager.updateSettings(stateManager.getSettings());
            }

            int shellIntegrationTimeout = stateManager.getSettings().getShellIntegrationTimeout();
            boolean terminalReuseEnabled = stateManager.getGlobalState().isTerminalReuseEnabled();
            int terminalOutputLineLimit = stateManager.getSettings().getTerminalOutputLineLimit();
            int subagentTerminalOutputLineLimit =
                    stateManager.getSettings().getSubagentTerminalOutputLineLimit();
            String defaultTerminalProfile = stateManager.getSettings().getDefaultTerminalProfile();

            this.workspaceManager = setupWorkspaceManager();

            String cwd = this.workspaceManager.getPrimaryRoot().getPath();

            String taskId =
                    historyItem != null && historyItem.getId() != null
                            ? historyItem.getId()
                            : String.valueOf(System.currentTimeMillis());

            boolean taskLockAcquired;
            TaskLockUtils.FolderLockWithRetryResult lockResult =
                    TaskLockUtils.tryAcquireTaskLockWithRetry(taskId);

            if (!lockResult.acquired() && !lockResult.skipped()) {
                String errorMessage =
                        lockResult.conflictingLock() != null
                                ? "Task locked by instance (" + lockResult.conflictingLock() + ")"
                                : "Failed to acquire task lock";
                throw new RuntimeException(errorMessage);
            }

            taskLockAcquired = lockResult.acquired();
            if (lockResult.acquired()) {
                log.debug("[Task {}] Task lock acquired", taskId);
            } else {
                log.debug("[Task {}] Task lock skipped", taskId);
            }

            if (taskSettings != null) {
                Settings newSettings = stateManager.getSettings();
                BeanUtil.copyProperties(taskSettings, newSettings);
            }

            TaskContext taskContext = TaskContextHolder.get();
            TaskParams.TaskParamsBuilder paramsBuilder =
                    TaskParams.builder(
                                    systemPromptService,
                                    cwd,
                                    contextManager,
                                    apiHandler,
                                    toolExecutor,
                                    taskContext,
                                    contextFactory)
                            .taskId(taskId)
                            .cwd(cwd)
                            .mcpHub(mcpHub)
                            .postStateToWebview(() -> postStateToWebview(taskId))
                            .reinitExistingTaskFromId(this::reinitExistingTaskFromId)
                            .cancelTask(() -> cancelTask(taskId))
                            .updateBackgroundCommandState(this::updateBackgroundCommandState)
                            .shouldShowBackgroundTerminalSuggestion(
                                    this::shouldShowBackgroundTerminalSuggestion)
                            .shellIntegrationTimeout(shellIntegrationTimeout)
                            .terminalReuseEnabled(terminalReuseEnabled)
                            .terminalOutputLineLimit(terminalOutputLineLimit)
                            .subagentTerminalOutputLineLimit(subagentTerminalOutputLineLimit)
                            .defaultTerminalProfile(defaultTerminalProfile)
                            .stateManager(stateManager)
                            .workspaceManager(workspaceManager)
                            .initialTaskText(text)
                            .focusChainManagerFactory(focusChainManagerFactory)
                            .images(images)
                            .files(files)
                            .historyItem(historyItem)
                            .taskLockAcquired(taskLockAcquired);

            TaskParams taskParams = paramsBuilder.build();
            Task task = new Task(taskParams);
            tasks.put(taskId, task);

            task.getTaskState().setRunning(true);
            CompletableFuture<Void> future = null;
            if (taskParams.getHistoryItem() != null) {
                future =
                        CompletableFuture.runAsync(
                                () -> taskContext.run(task::resumeTaskFromHistory), taskExecutor);
            } else if (taskParams.getInitialTaskText() != null
                    && !taskParams.getInitialTaskText().isEmpty()) {
                future =
                        CompletableFuture.runAsync(
                                () ->
                                        taskContext.run(
                                                () ->
                                                        task.startTask(
                                                                taskParams.getInitialTaskText(),
                                                                taskParams.getImages(),
                                                                taskParams.getFiles())),
                                taskExecutor);
            }
            if (future != null) {
                future.whenComplete(
                        (t, e) -> {
                            task.getTaskState().setRunning(false);
                        });
            }

            return taskId;
        } catch (Exception e) {
            log.error("[TaskManager] 初始化任务失败", e);
            throw new RuntimeException("初始化任务失败", e);
        }
    }

    public void showTask(String taskId) {
        postStateToWebview(taskId);
    }

    public Task getTask(String taskId) {
        return tasks.get(taskId);
    }

    public Task getOrInitTask(String taskId) {
        if (tasks.get(taskId) == null) {
            reinitExistingTaskFromId(taskId);
        }
        return tasks.get(taskId);
    }

    public void reinitExistingTaskFromId(String taskId) {
        List<HistoryItem> history = stateManager.getGlobalState().getTaskHistory();
        if (history == null) {
            history = new ArrayList<>();
        }
        HistoryItem historyItem =
                history.stream()
                        .filter(item -> item.getId().equals(taskId))
                        .findFirst()
                        .orElse(null);
        reinitExistingTaskFromHistoryItem(historyItem);
    }

    public void reinitExistingTaskFromHistoryItem(HistoryItem historyItem) {
        initTask(null, null, null, historyItem, null);
    }

    public void cancelTask(String taskId) {
        Task task = tasks.get(taskId);
        if (task == null) {
            throw new BizException("取消任务失败，没有这个任务");
        }

        updateBackgroundCommandState(false);

        task.abortTask();

        long startTime = System.currentTimeMillis();
        long timeout = 3000;
        while (System.currentTimeMillis() - startTime < timeout) {
            if (!task.getTaskState().isStreaming()
                    || task.getTaskState().isDidFinishAbortingStream()
                    || task.getTaskState().isWaitingForFirstChunk()) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        task.getTaskState().setAbandoned(true);
        reinitExistingTaskFromId(taskId);
    }

    public void cancelAllTask() {
        tasks.forEach((id, task) -> cancelTask(id));
    }

    public void updateBackgroundCommandState(boolean running, String taskId) {
        if (this.backgroundCommandRunning == running
                && (Objects.equals(taskId, this.backgroundCommandTaskId))) {
            return;
        }
        this.backgroundCommandRunning = running;
        this.backgroundCommandTaskId = running ? taskId : null;
        postStateToWebview(taskId);
    }

    public void updateBackgroundCommandState(boolean running) {
        updateBackgroundCommandState(running, null);
    }

    public void cancelBackgroundCommand(String taskId) {
        Task task = this.tasks.get(taskId);
        if (task != null) {
            try {
                Boolean didCancel = task.cancelBackgroundCommand();
                if (!Boolean.TRUE.equals(didCancel)) {
                    updateBackgroundCommandState(false);
                }
            } catch (Exception e) {
                log.error("[TaskManager] 取消背景命令失败", e);
                updateBackgroundCommandState(false);
            }
        }
    }

    public void exportTaskWithId(String id) {
        log.info("[TaskManager] 导出任务: {}", id);
    }

    public List<HistoryItem> updateTaskHistory(HistoryItem item) {
        List<HistoryItem> history = stateManager.getGlobalState().getTaskHistory();
        if (history == null) {
            history = new ArrayList<>();
            stateManager.getGlobalState().setTaskHistory(history);
        }
        int existingItemIndex = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getId().equals(item.getId())) {
                existingItemIndex = i;
                break;
            }
        }
        if (existingItemIndex != -1) {
            history.set(existingItemIndex, item);
        } else {
            history.add(item);
        }
        stateManager.getGlobalState().setTaskHistory(history);
        return history;
    }

    public void deleteTaskFromState(String taskId) {
        cancelTask(taskId);
        stateManager.deleteTask(taskId);
    }

    public void postStateToWebview(@Nullable String taskId) {
        ExtensionState state = getStateToPostToWebview(taskId);

        StateMessage stateMessage = new StateMessage(state);
        subscriptionManager.send(stateMessage);
    }

    public ExtensionState getStateToPostToWebview(@Nullable String taskId) {
        ApiConfiguration apiConfiguration = getApiConfiguration();

        List<HistoryItem> taskHistory = stateManager.getGlobalState().getTaskHistory();
        if (taskHistory == null) {
            taskHistory = new ArrayList<>();
        }
        String lastShownAnnouncementId = stateManager.getGlobalState().getLastShownAnnouncementId();
        AutoApprovalSettings autoApprovalSettings =
                stateManager.getSettings().getAutoApprovalSettings();
        BrowserSettings browserSettings = stateManager.getSettings().getBrowserSettings();
        FocusChainSettings focusChainSettings = stateManager.getSettings().getFocusChainSettings();
        DictationSettings dictationSettings = stateManager.getSettings().getDictationSettings();
        String preferredLanguage = stateManager.getSettings().getPreferredLanguage();
        OpenaiReasoningEffort openaiReasoningEffort =
                stateManager.getSettings().getOpenaiReasoningEffort();
        Mode mode = stateManager.getSettings().getMode();
        Boolean strictPlanModeEnabled = stateManager.getSettings().isStrictPlanModeEnabled();
        Boolean yoloModeToggled = stateManager.getSettings().isYoloModeToggled();
        Boolean useAutoCondense = stateManager.getSettings().isUseAutoCondense();
        Boolean mcpMarketplaceEnabled = stateManager.getGlobalState().isMcpMarketplaceEnabled();
        McpDisplayMode mcpDisplayMode = stateManager.getGlobalState().getMcpDisplayMode();
        TelemetrySetting telemetrySetting = stateManager.getSettings().getTelemetrySetting();
        boolean planActSeparateModelsSetting =
                stateManager.getSettings().isPlanActSeparateModelsSetting();
        Boolean enableCheckpointsSetting = stateManager.getSettings().isEnableCheckpointsSetting();
        int shellIntegrationTimeout = stateManager.getSettings().getShellIntegrationTimeout();
        Boolean terminalReuseEnabled = stateManager.getGlobalState().isTerminalReuseEnabled();
        TerminalExecutionMode terminalExecutionMode =
                stateManager.getGlobalState().getTerminalExecutionMode();

        String defaultTerminalProfile =
                stateManager.getSettings().getDefaultTerminalProfile() != null
                        ? stateManager.getSettings().getDefaultTerminalProfile()
                        : "default";
        Boolean welcomeViewCompletedObj = stateManager.getGlobalState().getWelcomeViewCompleted();
        boolean welcomeViewCompleted = Boolean.TRUE.equals(welcomeViewCompletedObj);
        String customPrompt = stateManager.getSettings().getCustomPrompt();
        Boolean mcpResponsesCollapsed = stateManager.getGlobalState().isMcpResponsesCollapsed();
        int terminalOutputLineLimit = stateManager.getSettings().getTerminalOutputLineLimit();
        int maxConsecutiveMistakes = stateManager.getSettings().getMaxConsecutiveMistakes();
        int subagentTerminalOutputLineLimit =
                stateManager.getSettings().getSubagentTerminalOutputLineLimit();
        List<String> favoritedModelIds = stateManager.getGlobalState().getFavoritedModelIds();
        if (favoritedModelIds == null) {
            favoritedModelIds = new ArrayList<>();
        }
        int lastDismissedInfoBannerVersion =
                stateManager.getGlobalState().getLastDismissedInfoBannerVersion();
        int lastDismissedModelBannerVersion =
                stateManager.getGlobalState().getLastDismissedModelBannerVersion();
        int lastDismissedCliBannerVersion =
                stateManager.getGlobalState().getLastDismissedCliBannerVersion();
        Boolean subagentsEnabled = stateManager.getSettings().isSubagentsEnabled();

        Map<String, Boolean> globalClineRulesToggles =
                stateManager.getSettings().getGlobalClineRulesToggles();
        Map<String, Boolean> globalWorkflowToggles =
                stateManager.getSettings().getGlobalWorkflowToggles();
        Double autoCondenseThreshold = stateManager.getSettings().getAutoCondenseThreshold();

        HistoryItem currentTaskHistoryItem = null;
        List<ClineMessage> clineMessages = null;
        String checkpointManagerErrorMessage = null;
        String currentFocusChainChecklist = null;

        if (taskId != null) {
            currentTaskHistoryItem = getTaskHistoryItem(taskId);

            clineMessages = stateManager.getSavedClineMessages(taskId);
            currentFocusChainChecklist = stateManager.getFocusChain(taskId);
        }

        List<HistoryItem> processedTaskHistory = new ArrayList<>();
        for (HistoryItem item : taskHistory) {
            if (item.getTs() != null && item.getTask() != null) {
                processedTaskHistory.add(item);
            }
        }
        processedTaskHistory.sort(
                (a, b) -> {
                    Long tsA = a.getTs() != null ? a.getTs() : 0L;
                    Long tsB = b.getTs() != null ? b.getTs() : 0L;
                    return tsB.compareTo(tsA);
                });
        if (processedTaskHistory.size() > 100) {
            processedTaskHistory = processedTaskHistory.subList(0, 100);
        }

        String version = ControllerUtils.getVersion();
        String latestAnnouncementId = ControllerUtils.getLatestAnnouncementId(version);
        boolean shouldShowAnnouncement =
                lastShownAnnouncementId == null
                        || !lastShownAnnouncementId.equals(latestAnnouncementId);

        Platform platform = ControllerUtils.getPlatform();
        String distinctId = ControllerUtils.getDistinctId();
        String environment = ControllerUtils.getEnvironment();

        if (dictationSettings != null) {
            dictationSettings.setFeatureEnabled(platform == Platform.DARWIN);
        }
        boolean hooksEnabledUser = stateManager.getGlobalState().isHooksEnabled();

        ClineFeatureSetting hooksEnabled =
                ClineFeatureSetting.builder().user(hooksEnabledUser).featureFlag(true).build();

        return ExtensionState.builder()
                .apiConfiguration(apiConfiguration)
                .currentTaskItem(currentTaskHistoryItem)
                .clineMessages(clineMessages)
                .currentFocusChainChecklist(currentFocusChainChecklist)
                .checkpointManagerErrorMessage(checkpointManagerErrorMessage)
                .autoApprovalSettings(autoApprovalSettings)
                .browserSettings(browserSettings)
                .focusChainSettings(focusChainSettings)
                .dictationSettings(dictationSettings)
                .preferredLanguage(preferredLanguage)
                .openaiReasoningEffort(openaiReasoningEffort)
                .mode(mode)
                .strictPlanModeEnabled(strictPlanModeEnabled)
                .yoloModeToggled(yoloModeToggled)
                .useAutoCondense(useAutoCondense)
                .mcpMarketplaceEnabled(mcpMarketplaceEnabled)
                .mcpDisplayMode(mcpDisplayMode)
                .telemetrySetting(telemetrySetting)
                .planActSeparateModelsSetting(planActSeparateModelsSetting)
                .enableCheckpointsSetting(enableCheckpointsSetting)
                .shellIntegrationTimeout(shellIntegrationTimeout)
                .terminalReuseEnabled(terminalReuseEnabled)
                .terminalExecutionMode(terminalExecutionMode)
                .defaultTerminalProfile(defaultTerminalProfile)
                .welcomeViewCompleted(welcomeViewCompleted)
                .mcpResponsesCollapsed(mcpResponsesCollapsed)
                .terminalOutputLineLimit(terminalOutputLineLimit)
                .maxConsecutiveMistakes(maxConsecutiveMistakes)
                .subagentTerminalOutputLineLimit(subagentTerminalOutputLineLimit)
                .customPrompt(customPrompt)
                .taskHistory(processedTaskHistory)
                .shouldShowAnnouncement(shouldShowAnnouncement)
                .favoritedModelIds(favoritedModelIds)
                .autoCondenseThreshold(autoCondenseThreshold)
                .backgroundCommandRunning(backgroundCommandRunning)
                .backgroundCommandTaskId(backgroundCommandTaskId)
                .workspaceRoots(workspaceManager.getRoots())
                .primaryRootIndex(workspaceManager.getPrimaryIndex())
                .hooksEnabled(hooksEnabled)
                .version(version)
                .distinctId(distinctId)
                .platform(platform)
                .environment(environment)
                .globalClineRulesToggles(globalClineRulesToggles)
                .globalWorkflowToggles(globalWorkflowToggles)
                .lastDismissedInfoBannerVersion(lastDismissedInfoBannerVersion)
                .lastDismissedModelBannerVersion(lastDismissedModelBannerVersion)
                .lastDismissedCliBannerVersion(lastDismissedCliBannerVersion)
                .subagentsEnabled(subagentsEnabled)
                .build();
    }

    private @Nullable HistoryItem getTaskHistoryItem(String taskId) {
        HistoryItem currentTaskItem;
        List<HistoryItem> history = stateManager.getGlobalState().getTaskHistory();
        if (history == null) {
            history = new ArrayList<>();
        }
        currentTaskItem =
                history.stream()
                        .filter(item -> item.getId().equals(taskId))
                        .findFirst()
                        .orElse(null);
        return currentTaskItem;
    }

    private WorkspaceRootManager setupWorkspaceManager() {
        WorkspaceSetup.RootsDetector detectRoots =
                () -> {
                    List<String> workspacePaths = stateManager.getWorkspaceRoots();
                    return WorkspaceDetection.detectWorkspaceRoots(workspacePaths);
                };

        WorkspaceSetup.SetupConfig setupConfig =
                new WorkspaceSetup.SetupConfig(stateManager, detectRoots, null);

        return WorkspaceSetup.setupWorkspaceManager(setupConfig);
    }

    public boolean shouldShowBackgroundTerminalSuggestion() {
        long oneHourAgo = System.currentTimeMillis() - 60 * 60 * 1000;

        shellIntegrationWarningTracker.timestamps.removeIf(ts -> ts <= oneHourAgo);

        shellIntegrationWarningTracker.timestamps.add(System.currentTimeMillis());

        if (shellIntegrationWarningTracker.lastSuggestionShown != null
                && System.currentTimeMillis() - shellIntegrationWarningTracker.lastSuggestionShown
                        < 60 * 60 * 1000) {
            return false;
        }

        if (shellIntegrationWarningTracker.timestamps.size() >= 3) {
            shellIntegrationWarningTracker.lastSuggestionShown = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    public void dispose() {
        cancelAllTask();

        if (this.mcpHub != null) {
            this.mcpHub.dispose();
        }

        if (taskExecutor != null) {
            taskExecutor.shutdown();
            try {
                if (!taskExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("[TaskManager] 等待任务完成超时，强制关闭线程池");
                    taskExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                taskExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                log.error("[TaskManager] 关闭线程池时被中断", e);
            }
        }

        log.info("[TaskManager] TaskManager disposed");
    }

    private ApiConfiguration getApiConfiguration() {
        Settings settings = stateManager.getSettings();
        Secrets secrets = stateManager.getSecrets();
        ApiConfiguration apiConfiguration = BeanUtil.toBean(settings, ApiConfiguration.class);
        BeanUtil.copyProperties(secrets, apiConfiguration);
        return apiConfiguration;
    }

    private static class ShellIntegrationWarningTracker {
        private final List<Long> timestamps = new ArrayList<>();
        private Long lastSuggestionShown;
    }
}
