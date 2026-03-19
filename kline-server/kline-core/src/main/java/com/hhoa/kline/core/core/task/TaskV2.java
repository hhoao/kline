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
import com.hhoa.kline.core.core.task.transition.AbortTransition;
import com.hhoa.kline.core.core.task.transition.ApiCallingFailedTransition;
import com.hhoa.kline.core.core.task.transition.ApiCompletedTransition;
import com.hhoa.kline.core.core.task.transition.ApiRetryTransition;
import com.hhoa.kline.core.core.task.transition.AskUserForResetRetryTimesTransition;
import com.hhoa.kline.core.core.task.transition.ContextReadyTransition;
import com.hhoa.kline.core.core.task.transition.NoRetryTransition;
import com.hhoa.kline.core.core.task.transition.NoopTransition;
import com.hhoa.kline.core.core.task.transition.PrepareContextTransition;
import com.hhoa.kline.core.core.task.transition.PrepareFailedTransition;
import com.hhoa.kline.core.core.task.transition.ResumeTaskTransition;
import com.hhoa.kline.core.core.task.transition.RetryTransition;
import com.hhoa.kline.core.core.task.transition.StartTaskTransition;
import com.hhoa.kline.core.core.task.transition.TaskCompleteTransition;
import com.hhoa.kline.core.core.task.transition.ToolAskRespondedTransition;
import com.hhoa.kline.core.core.task.transition.UserRespondedTransition;
import com.hhoa.kline.core.core.task.transition.WaitingUserAskResponseTransition;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 基于事件与状态机的任务实现 */
@Slf4j
@Data
public class TaskV2 implements Recoverable<TaskState> {

    private static final StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>
            STATE_MACHINE_FACTORY =
                    new StateMachineFactory<TaskV2, TaskStatus, TaskEventType, TaskEvent>(
                                    TaskStatus.NEW)
                            .addTransition(
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    Set.of(TaskStatus.PREPARE_CONTEXT),
                                    TaskEventType.USER_RESPONDED,
                                    new UserRespondedTransition())
                            .addTransition(
                                    TaskStatus.NEW,
                                    TaskStatus.START_TASK,
                                    TaskEventType.START_TASK,
                                    new StartTaskTransition())
                            .addTransition(
                                    TaskStatus.START_TASK,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.PREPARE_CONTEXT,
                                    new PrepareContextTransition())
                            .addTransition(
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskStatus.CALLING_API,
                                    TaskEventType.CONTEXT_READY,
                                    new ContextReadyTransition())
                            .addTransition(
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    TaskEventType.MAX_MISTAKE_LIMIT_REACHED,
                                    new WaitingUserAskResponseTransition())
                            .addTransition(
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskStatus.ABORT,
                                    TaskEventType.PREPARE_FAILED,
                                    new PrepareFailedTransition())
                            .addTransition(
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.API_CALLING_FAILED,
                                    TaskEventType.API_CALLING_FAILED,
                                    new ApiCallingFailedTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.API_CALLING_RETRY,
                                    TaskEventType.API_CALLING_RETRY,
                                    new ApiRetryTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.RETRY,
                                    new RetryTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.API_COMPLETED,
                                    TaskEventType.API_COMPLETED,
                                    new ApiCompletedTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.RESUME_TASK,
                                    new ResumeTaskTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.CALLING_API,
                                    TaskEventType.USER_RESPONDED,
                                    new ToolAskRespondedTransition())
                            .addTransition(
                                    TaskStatus.CALLING_API,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.API_COMPLETED,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.CONTINUE_NEXT_TURN,
                                    new PrepareContextTransition())
                            .addTransition(
                                    TaskStatus.API_COMPLETED,
                                    TaskStatus.TASK_COMPLETE,
                                    TaskEventType.TASK_COMPLETE,
                                    new TaskCompleteTransition())
                            .addTransition(
                                    TaskStatus.API_COMPLETED,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.API_CALLING_RETRY,
                                    TaskStatus.CALLING_API,
                                    TaskEventType.CONTEXT_READY,
                                    new ContextReadyTransition())
                            .addTransition(
                                    TaskStatus.API_CALLING_RETRY,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.API_CALLING_FAILED,
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    TaskEventType.ASK_USER_FOR_RESET_RETRY_TIMES,
                                    new AskUserForResetRetryTimesTransition())
                            .addTransition(
                                    TaskStatus.API_CALLING_FAILED,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.RETRY,
                                    new RetryTransition())
                            .addTransition(
                                    TaskStatus.API_CALLING_FAILED,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    EnumSet.of(
                                            TaskStatus.PREPARE_CONTEXT,
                                            TaskStatus.CALLING_API,
                                            TaskStatus.WAITING_USER_ASK_RESPONSE,
                                            TaskStatus.TASK_COMPLETE,
                                            TaskStatus.ABORT),
                                    TaskEventType.USER_RESPONDED,
                                    new UserRespondedTransition())
                            .addTransition(
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.RETRY,
                                    new RetryTransition())
                            .addTransition(
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    TaskStatus.TASK_COMPLETE,
                                    TaskEventType.NO_RETRY,
                                    new NoRetryTransition())
                            .addTransition(
                                    TaskStatus.WAITING_USER_ASK_RESPONSE,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new AbortTransition())
                            .addTransition(
                                    TaskStatus.ABORT,
                                    TaskStatus.PREPARE_CONTEXT,
                                    TaskEventType.RESTORE_TASK,
                                    new PrepareContextTransition())
                            .addTransition(
                                    TaskStatus.TASK_COMPLETE,
                                    TaskStatus.TASK_COMPLETE,
                                    TaskEventType.ABORT,
                                    new NoopTransition())
                            .addTransition(
                                    TaskStatus.ABORT,
                                    TaskStatus.ABORT,
                                    TaskEventType.ABORT,
                                    new NoopTransition())
                            .installTopology();

    private final StateMachine<TaskStatus, TaskEventType, TaskEvent> stateMachine;
    @Getter private final TaskState taskState;
    @Getter private final MessageStateHandler messageStateHandler;
    @Getter private final String taskId;
    @Getter private final String cwd;
    @Getter private final String ulid;
    @Getter private final long taskInitializationStartTime;
    private final ToolExecutor toolExecutor;
    private final AtomicLong tsGenerator = new AtomicLong(System.currentTimeMillis());
    private final ApiHandler apiHandler;
    private final SystemPromptService systemPromptService;
    private final ContextManager contextManager;
    private final ResponseFormatter responseFormatter;
    private final BlockingQueue<AssistantMessageUpdate> messageQueue = new LinkedBlockingQueue<>();
    private final IMcpHub mcpHub;
    private final Runnable postStateToWebview;
    private final Runnable cancelTask;
    private final BiConsumer<Boolean, String> updateBackgroundCommandState;
    private final Supplier<Boolean> shouldShowBackgroundTerminalSuggestion;
    private final StateManager stateManager;
    private final WorkspaceRootManager workspaceManager;
    private final DiffViewProvider diffViewProvider;
    private final ClineIgnoreController clineIgnoreController;
    private final FileContextTracker fileContextTracker;
    private final ModelContextTracker modelContextTracker;
    private final TelemetryService telemetryService;
    private final ReentrantLock lock = new ReentrantLock();
    private final AssistantMessageParser messageParser;
    private final int askResponseTimeout;
    private final FocusChainManager focusChainManager;
    private final TerminalManager terminalManager;
    private final Queue<TaskEvent> eventQueue = new ArrayDeque<>();
    private final AtomicBoolean eventLoopScheduled = new AtomicBoolean(false);
    private final ExecutorService eventLoopExecutor;
    private final TaskV2ResumeHandler resumeHandler;
    private final TaskV2ContextPrepareHandler contextPrepareHandler;
    private final TaskV2ContextWindowHandler contextWindowHandler;
    private final TaskV2MessagePresenterHandler messagePresenterHandler;
    private final TaskV2ApiCallHandler apiCallHandler;
    private final TaskV2CommandHandler commandHandler;
    private final TaskV2SayAskHandler sayAskHandler;
    private final TaskV2AbortHandler abortHandler;
    private final TaskV2StartTaskHandler startTaskHandler;
    @Getter private ICheckpointManager checkpointManager;
    private boolean taskLockAcquired;
    private ActiveBackgroundCommand activeBackgroundCommand;
    private NotificationService notificationService;
    private ContextFactory contextFactory;

    public TaskV2(TaskParams params) {
        if (params == null) throw new IllegalArgumentException("params == null");
        if (params.getTaskId() == null) throw new IllegalArgumentException("taskId == null");
        if (params.getCwd() == null) throw new IllegalArgumentException("cwd == null");

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

        this.diffViewProvider = new DefaultDiffViewProvider();
        this.clineIgnoreController = new DefaultClineIgnoreController(this.cwd);
        this.fileContextTracker = new FileContextTracker(this.taskId, this.cwd, this.stateManager);
        this.modelContextTracker = new ModelContextTracker(this.taskId, this.stateManager);
        this.telemetryService = new DefaultTelemetryService();
        this.messageParser = new AssistantMessageParser();
        this.workspaceManager = params.getWorkspaceManager();
        this.contextFactory = params.getContextFactory();

        this.taskState = new TaskState();

        this.messageStateHandler =
                new MessageStateHandler(
                        this.taskId,
                        this.ulid,
                        this.taskState,
                        this.stateManager,
                        params.getWorkspaceManager());
        this.toolExecutor = params.getToolExecutor();

        this.apiHandler = params.getApiHandler();

        this.systemPromptService = params.getSystemPromptService();
        if (this.systemPromptService == null) {
            throw new IllegalArgumentException("SystemPromptService is required");
        }

        this.contextManager = params.getContextManager();
        if (this.contextManager == null) {
            throw new IllegalArgumentException("ContextManager is required");
        }

        this.responseFormatter = new ResponseFormatter();

        this.mcpHub = params.getMcpHub();

        this.postStateToWebview = params.getPostStateToWebview();
        this.cancelTask = params.getCancelTask();
        this.updateBackgroundCommandState = params.getUpdateBackgroundCommandState();
        this.shouldShowBackgroundTerminalSuggestion =
                params.getShouldShowBackgroundTerminalSuggestion();

        this.diffViewProvider.setWorkspaceManager(this.workspaceManager);

        this.contextManager.initializeContextHistory();

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

        this.askResponseTimeout = params.getAskResponseTimeout();
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

        boolean isMultiRootWorkspace =
                this.workspaceManager != null && this.workspaceManager.getRoots().size() > 1;

        if (!isMultiRootWorkspace) {
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
                                        this.taskState.setCheckpointManagerErrorMessage(
                                                errorMessage);
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
                                    .message(
                                            "Failed to initialize checkpoint manager: "
                                                    + errorMessage)
                                    .build();
                    DefaultSubscriptionManager.getInstance()
                            .send(new WindowShowMessageRequestMessage(request));
                }
                this.checkpointManager = null;
            }
        } else {
            this.checkpointManager = null;
        }

        if (this.mcpHub != null) {
            this.mcpHub.setNotificationCallback(
                    (notification) -> {
                        String serverName = notification.serverName();
                        String message = notification.message();
                        sayAskHandler.say(
                                ClineSay.MCP_NOTIFICATION,
                                String.format("[%s] %s", serverName, message));
                    });
        }

        this.contextWindowHandler =
                new TaskV2ContextWindowHandler(
                        contextManager, this.messageStateHandler, this.taskState, sayAskHandler);
        this.resumeHandler =
                new TaskV2ResumeHandler(
                        responseFormatter,
                        stateManager,
                        fileContextTracker,
                        taskId,
                        cwd,
                        taskState,
                        messageStateHandler,
                        this::getCheckpointManager,
                        sayAskHandler);
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

        this.stateMachine = STATE_MACHINE_FACTORY.make(this, TaskStatus.NEW);
    }

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

    /**
     * 事件循环（纯状态机驱动）。处理 eventQueue 中的事件，驱动状态机迁移。 每次状态迁移后调用 onStateEntered() 触发该状态对应的工作阶段。 当
     * AskSuspendException 被捕获时，派发 ASK_SUSPENDED 事件使状态机 迁移到 WAITING_USER_ASK_RESPONSE，然后退出循环释放线程。
     */
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

    @Override
    public void recover(TaskState state) {}

    private String generateUlid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26);
    }

    public ProviderInfo getCurrentProviderInfo() {
        ProviderInfo info = new ProviderInfo();
        if (this.apiHandler != null) {
            info.model = this.apiHandler.getModelId();
            info.providerId = this.apiHandler.getProviderId();
        } else {
            info.model = "claude-3-5-sonnet-20241022"; // 默认模型
            info.providerId = "anthropic"; // 默认提供者
        }
        info.customPrompt = null;
        return info;
    }
}
