package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.AssistantMessageParser;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.context.tracking.ModelContextTracker;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.ignore.DefaultClineIgnoreController;
import com.hhoa.kline.core.core.integrations.checkpoints.CheckpointManagerFactory;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.integrations.checkpoints.TaskCheckpointManager;
import com.hhoa.kline.core.core.integrations.editor.DefaultDiffViewProvider;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.integrations.notifications.NotificationService;
import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.telemetry.DefaultTelemetryService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.deps.ActiveBackgroundCommand;
import com.hhoa.kline.core.core.task.event.TaskEvent;
import com.hhoa.kline.core.core.task.event.TaskEventType;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManager;
import com.hhoa.kline.core.core.task.handler.TaskV2AbortHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2ApiCallHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2CommandHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2ContextPrepareHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2ContextWindowHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2MessagePresenterHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2ResumeHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2SayAskHandler;
import com.hhoa.kline.core.core.task.handler.TaskV2StartTaskHandler;
import com.hhoa.kline.core.core.task.statemachine.Recoverable;
import com.hhoa.kline.core.core.task.statemachine.StateMachine;
import com.hhoa.kline.core.core.task.statemachine.StateMachineFactory;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TaskV2 implements Recoverable<TaskState> {

    private static final StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            STATE_MACHINE_FACTORY = TaskV2StateMachineTopology.installedFactory();

    // ---- core identity (immutable) ----
    private final String taskId;
    private final String cwd;
    private final String ulid;
    private final long taskInitializationStartTime;

    // ---- event loop (immutable) ----
    private final Queue<TaskEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean eventLoopScheduled = new AtomicBoolean(false);
    private final ExecutorService eventLoopExecutor;

    // ---- task state (immutable refs) ----
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;

    // ---- external dependencies from params (immutable refs) ----
    private final ApiHandler apiHandler;
    private final StateManager stateManager;
    private final WorkspaceRootManager workspaceManager;
    private final IMcpHub mcpHub;
    private final Runnable postStateToWebview;
    private final Runnable cancelTask;
    private final BiConsumer<Boolean, String> updateBackgroundCommandState;
    private final Supplier<Boolean> shouldShowBackgroundTerminalSuggestion;
    private final ToolExecutor toolExecutor;
    private final SystemPromptService systemPromptService;
    private final ContextManager contextManager;
    private final int askResponseTimeout;
    private final ContextFactory contextFactory;

    // ---- infrastructure (created during core init, immutable refs) ----
    private final DiffViewProvider diffViewProvider;
    private final ClineIgnoreController clineIgnoreController;
    private final FileContextTracker fileContextTracker;
    private final ModelContextTracker modelContextTracker;
    private final TelemetryService telemetryService;
    private final AssistantMessageParser messageParser;
    private final ResponseFormatter responseFormatter;
    private final AtomicLong tsGenerator = new AtomicLong(System.currentTimeMillis());
    private final BlockingQueue<AssistantMessageUpdate> messageQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock lock = new ReentrantLock();

    // ---- handlers (set by factory via initHandlers) ----
    private TaskV2SayAskHandler sayAskHandler;
    private TaskV2StartTaskHandler startTaskHandler;
    private TaskV2ResumeHandler resumeHandler;
    private TaskV2ContextPrepareHandler contextPrepareHandler;
    private TaskV2ContextWindowHandler contextWindowHandler;
    private TaskV2MessagePresenterHandler messagePresenterHandler;
    private TaskV2ApiCallHandler apiCallHandler;
    private TaskV2CommandHandler commandHandler;
    private TaskV2AbortHandler abortHandler;
    private FocusChainManager focusChainManager;
    private TerminalManager terminalManager;

    // ---- mutable runtime state ----
    private StateMachine<TaskStatus, TaskEventType, TaskEvent> stateMachine;
    private ICheckpointManager checkpointManager;
    private boolean taskLockAcquired;
    private ActiveBackgroundCommand activeBackgroundCommand;
    private NotificationService notificationService;

    /**
     * 第一阶段：核心字段与基础设施初始化。
     *
     * <p>仅由 {@link TaskV2Factory} 调用。
     */
    TaskV2(TaskParams params) {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (params.getTaskId() == null) throw new IllegalArgumentException("taskId == null");
        if (params.getCwd() == null) throw new IllegalArgumentException("cwd == null");
        if (params.getSystemPromptService() == null) {
            throw new IllegalArgumentException("SystemPromptService is required");
        }
        if (params.getContextManager() == null) {
            throw new IllegalArgumentException("ContextManager is required");
        }

        this.taskInitializationStartTime = System.nanoTime() / 1_000_000;
        this.taskId = params.getTaskId();
        this.cwd = params.getCwd();
        this.ulid = params.getUlid() != null ? params.getUlid() : generateUlid();
        this.eventLoopExecutor =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r);
                            t.setName("task-v2-event-loop-" + this.taskId);
                            return t;
                        });

        this.stateManager = params.getStateManager();
        this.workspaceManager = params.getWorkspaceManager();
        this.contextFactory = params.getContextFactory();
        this.toolExecutor = params.getToolExecutor();
        this.apiHandler = params.getApiHandler();
        this.systemPromptService = params.getSystemPromptService();
        this.contextManager = params.getContextManager();
        this.mcpHub = params.getMcpHub();
        this.postStateToWebview = params.getPostStateToWebview();
        this.cancelTask = params.getCancelTask();
        this.updateBackgroundCommandState = params.getUpdateBackgroundCommandState();
        this.shouldShowBackgroundTerminalSuggestion =
                params.getShouldShowBackgroundTerminalSuggestion();
        this.askResponseTimeout = params.getAskResponseTimeout();

        this.diffViewProvider = new DefaultDiffViewProvider();
        this.diffViewProvider.setWorkspaceManager(this.workspaceManager);
        this.clineIgnoreController = new DefaultClineIgnoreController(this.cwd);
        this.fileContextTracker = new FileContextTracker(this.taskId, this.cwd, this.stateManager);
        this.modelContextTracker = new ModelContextTracker(this.taskId, this.stateManager);
        this.telemetryService = new DefaultTelemetryService();
        this.messageParser = new AssistantMessageParser();
        this.responseFormatter = new ResponseFormatter();

        this.taskState = new TaskState();
        this.messageStateHandler =
                new MessageStateHandler(
                        this.taskId,
                        this.ulid,
                        this.taskState,
                        this.stateManager,
                        this.workspaceManager);

        this.contextManager.initializeContextHistory();
    }

    /** 第二阶段：任务锁获取。 */
    void acquireTaskLock(TaskParams params) {
        if (params.isTaskLockAcquired()) {
            this.taskLockAcquired = true;
        } else {
            TaskLockUtils.FolderLockWithRetryResult lockRes =
                    TaskLockUtils.tryAcquireTaskLockWithRetry(this.taskId);
            if (!lockRes.acquired() && !lockRes.skipped()) {
                throw new IllegalStateException(
                        "Failed to acquire task lock: " + lockRes.conflictingLock());
            }
            this.taskLockAcquired = lockRes.acquired();
        }
    }

    void initHandlers(TaskParams params) {
        this.sayAskHandler =
                new TaskV2SayAskHandler(
                        this.tsGenerator,
                        this.messageStateHandler,
                        this.taskState,
                        this.postStateToWebview,
                        this.taskId);

        this.startTaskHandler =
                new TaskV2StartTaskHandler(
                        this.messageStateHandler,
                        this.postStateToWebview,
                        this.sayAskHandler,
                        this.taskState);

        this.focusChainManager =
                params.getFocusChainManagerFactory()
                        .createFocusChainManager(
                                this.taskId,
                                this.taskState,
                                this.postStateToWebview,
                                sayAskHandler::say,
                                this.telemetryService);
        this.focusChainManager.setupFocusChain();

        this.terminalManager = new TerminalManager();
        this.terminalManager.setShellIntegrationTimeout(params.getShellIntegrationTimeout());
        this.terminalManager.setTerminalReuseEnabled(params.isTerminalReuseEnabled());
        this.terminalManager.setTerminalOutputLineLimit(params.getTerminalOutputLineLimit());
        this.terminalManager.setSubagentTerminalOutputLineLimit(
                params.getSubagentTerminalOutputLineLimit());
        if (params.getDefaultTerminalProfile() != null) {
            this.terminalManager.setDefaultTerminalProfile(params.getDefaultTerminalProfile());
        }

        this.contextWindowHandler =
                new TaskV2ContextWindowHandler(
                        contextManager, this.messageStateHandler, this.taskState, sayAskHandler);

        this.resumeHandler =
                new TaskV2ResumeHandler(
                        stateManager, taskId, taskState, messageStateHandler, sayAskHandler);

        this.contextPrepareHandler =
                new TaskV2ContextPrepareHandler(
                        stateManager,
                        modelContextTracker,
                        contextManager,
                        telemetryService,
                        fileContextTracker,
                        focusChainManager,
                        clineIgnoreController,
                        systemPromptService,
                        contextFactory,
                        responseFormatter,
                        workspaceManager,
                        mcpHub,
                        taskState,
                        messageStateHandler,
                        this::getCurrentProviderInfo,
                        sayAskHandler,
                        this::handle,
                        this::getCheckpointManager,
                        taskId,
                        ulid,
                        cwd,
                        taskInitializationStartTime,
                        postStateToWebview);

        this.messagePresenterHandler =
                new TaskV2MessagePresenterHandler(
                        messageParser,
                        telemetryService,
                        diffViewProvider,
                        taskState,
                        messageQueue,
                        lock,
                        stateManager,
                        workspaceManager,
                        apiHandler,
                        contextManager,
                        mcpHub,
                        notificationService,
                        clineIgnoreController,
                        fileContextTracker,
                        this::getCheckpointManager,
                        this::getFocusChainManager,
                        (cmd, timeout) -> getCommandHandler().executeCommandTool(cmd, timeout),
                        toolExecutor,
                        sayAskHandler,
                        messageStateHandler,
                        this::getCurrentProviderInfo,
                        taskId,
                        ulid,
                        cwd);

        this.commandHandler =
                new TaskV2CommandHandler(
                        terminalManager,
                        telemetryService,
                        stateManager,
                        cwd,
                        taskId,
                        ulid,
                        messageStateHandler,
                        this::getActiveBackgroundCommand,
                        this::setActiveBackgroundCommand,
                        updateBackgroundCommandState,
                        sayAskHandler,
                        shouldShowBackgroundTerminalSuggestion);

        this.abortHandler =
                new TaskV2AbortHandler(
                        this.taskState,
                        this.focusChainManager,
                        this::getActiveBackgroundCommand,
                        () -> getCommandHandler().cancelBackgroundCommand(),
                        this.terminalManager,
                        this::isTaskLockAcquired,
                        b -> this.taskLockAcquired = Boolean.TRUE.equals(b),
                        this.taskId,
                        this.mcpHub);

        this.apiCallHandler =
                new TaskV2ApiCallHandler(
                        responseFormatter,
                        stateManager,
                        contextManager,
                        telemetryService,
                        diffViewProvider,
                        messageParser,
                        systemPromptService,
                        contextFactory,
                        apiHandler,
                        contextWindowHandler,
                        taskState,
                        messageStateHandler,
                        this::handle,
                        sayAskHandler,
                        taskId,
                        ulid,
                        this::getCurrentProviderInfo,
                        messagePresenterHandler,
                        contextPrepareHandler,
                        postStateToWebview);
    }

    void initCheckpointManager() {
        boolean isMultiRootWorkspace =
                this.workspaceManager != null && this.workspaceManager.getRoots().size() > 1;

        if (isMultiRootWorkspace) {
            this.checkpointManager = null;
            return;
        }

        try {
            TaskCheckpointManager.CheckpointManagerCallbacks callbacks =
                    new TaskCheckpointManager.CheckpointManagerCallbacks() {
                        @Override
                        public CompletableFuture<Long> say(
                                ClineSay type,
                                String text,
                                List<String> images,
                                List<String> files,
                                Boolean partial) {
                            sayAskHandler.say(type, text, images, files, partial);
                            Long ts = taskState.getLastMessageTs();
                            return CompletableFuture.completedFuture(ts);
                        }

                        @Override
                        public Runnable getPostStateToWebview() {
                            return TaskV2.this.postStateToWebview;
                        }

                        @Override
                        public Runnable getCancelTask() {
                            return TaskV2.this.cancelTask;
                        }
                    };

            this.checkpointManager =
                    CheckpointManagerFactory.buildCheckpointManager(
                            this.taskId,
                            this.messageStateHandler,
                            this.fileContextTracker,
                            this.diffViewProvider,
                            this.taskState,
                            this.workspaceManager,
                            callbacks,
                            this.taskState.getConversationHistoryDeletedRange(),
                            this.taskState.getCheckpointManagerErrorMessage(),
                            this.stateManager);

            if (CheckpointManagerFactory.shouldUseMultiRoot(
                    this.workspaceManager,
                    this.stateManager.getSettings().isEnableCheckpointsSetting(),
                    this.stateManager)) {
                this.checkpointManager
                        .initialize()
                        .exceptionally(
                                error -> {
                                    log.error(
                                            "Failed to initialize multi-root checkpoint manager: {}",
                                            error.getMessage(),
                                            error);
                                    String errorMessage =
                                            error.getMessage() != null
                                                    ? error.getMessage()
                                                    : "Unknown error";
                                    this.taskState.setCheckpointManagerErrorMessage(errorMessage);
                                    return null;
                                });
            }
        } catch (Exception error) {
            log.error("Failed to initialize checkpoint manager: {}", error.getMessage(), error);
            if (this.stateManager.getSettings().isEnableCheckpointsSetting()) {
                String errorMessage =
                        error.getMessage() != null ? error.getMessage() : "Unknown error";
                ShowMessageRequest request =
                        ShowMessageRequest.builder()
                                .type(ShowMessageType.ERROR)
                                .message("Failed to initialize checkpoint manager: " + errorMessage)
                                .build();
                DefaultSubscriptionManager.getInstance()
                        .send(new WindowShowMessageRequestMessage(request));
            }
            this.checkpointManager = null;
        }
    }

    void initMcpCallbacks() {
        if (this.mcpHub != null) {
            this.mcpHub.setNotificationCallback(
                    notification -> {
                        String serverName = notification.serverName();
                        String message = notification.message();
                        sayAskHandler.say(
                                ClineSay.MCP_NOTIFICATION,
                                String.format("[%s] %s", serverName, message));
                    });
        }
    }

    void initStateMachine() {
        this.stateMachine = STATE_MACHINE_FACTORY.make(this, TaskStatus.NEW);
    }

    // ---- runtime methods ----

    public TaskStatus getState() {
        return stateMachine.getCurrentState();
    }

    FocusChainManager getFocusChainManager() {
        return focusChainManager;
    }

    public void handle(TaskEvent event) {
        if (event == null) {
            return;
        }
        eventQueue.offer(event);
        scheduleEventLoop();
    }

    public void setActiveBackgroundCommand(ActiveBackgroundCommand activeBackgroundCommand) {
        this.activeBackgroundCommand = activeBackgroundCommand;
    }

    @Override
    public void recover(TaskState state) {}

    public ProviderInfo getCurrentProviderInfo() {
        ProviderInfo info = new ProviderInfo();
        if (this.apiHandler != null) {
            info.model = this.apiHandler.getModelId();
            info.providerId = this.apiHandler.getProviderId();
        } else {
            info.model = "claude-3-5-sonnet-20241022";
            info.providerId = "anthropic";
        }
        info.customPrompt = null;
        return info;
    }

    // ---- event loop internals ----

    private void scheduleEventLoop() {
        if (!eventLoopScheduled.compareAndSet(false, true)) {
            return;
        }
        eventLoopExecutor.submit(
                () -> {
                    try {
                        runLoop();
                    } finally {
                        eventLoopScheduled.set(false);
                        if (!eventQueue.isEmpty()) {
                            scheduleEventLoop();
                        }
                    }
                });
    }

    private void runLoop() {
        while (!eventQueue.isEmpty()) {
            try {
                TaskEvent event = eventQueue.poll();
                stateMachine.doTransition(event.getType(), event);
            } catch (Exception e) {
                log.error("TaskV2EventLoop process error", e);
            }
        }
    }

    private String generateUlid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26);
    }
}
