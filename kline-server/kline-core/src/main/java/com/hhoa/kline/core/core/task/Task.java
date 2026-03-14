package com.hhoa.kline.core.core.task;

import static com.hhoa.kline.core.core.integrations.misc.ExtractText.processFilesIntoText;
import static com.hhoa.kline.core.core.shared.LanguageKey.getLanguageKey;
import static com.hhoa.kline.core.core.utils.TimeUtils.waitFor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hhoa.ai.kline.commons.utils.ExceptionUtils;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.common.Tuple2;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.AssistantMessageParser;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.RedactedThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.context.instructions.userinstructions.ClineRules;
import com.hhoa.kline.core.core.context.management.ContextErrorHandling;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.management.ContextWindowUtils;
import com.hhoa.kline.core.core.context.management.KeepStrategy;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.context.tracking.ModelContextTracker;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.ignore.DefaultClineIgnoreController;
import com.hhoa.kline.core.core.integrations.checkpoints.CheckpointInitializer;
import com.hhoa.kline.core.core.integrations.checkpoints.CheckpointManagerFactory;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.integrations.checkpoints.TaskCheckpointManager;
import com.hhoa.kline.core.core.integrations.editor.DefaultDiffViewProvider;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.integrations.notifications.NotificationService;
import com.hhoa.kline.core.core.integrations.subagents.SubagentCommandUtils;
import com.hhoa.kline.core.core.integrations.terminal.TerminalInfo;
import com.hhoa.kline.core.core.integrations.terminal.TerminalManager;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessListeners;
import com.hhoa.kline.core.core.integrations.terminal.TerminalProcessResultPromise;
import com.hhoa.kline.core.core.mentions.Mentions;
import com.hhoa.kline.core.core.prompts.ContextManagement;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.telemetry.DefaultTelemetryService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.ExtensionMessageConstants;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.LanguageKey;
import com.hhoa.kline.core.core.shared.TerminalExecutionMode;
import com.hhoa.kline.core.core.shared.api.ModelInfo;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageRequest;
import com.hhoa.kline.core.core.shared.proto.host.ShowMessageType;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.slashcommands.SlashCommandParser;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManager;
import com.hhoa.kline.core.core.task.tools.AutoApprove;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.utils.PartialJsonUtils;
import com.hhoa.kline.core.core.utils.TimeUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.subscription.DefaultSubscriptionManager;
import com.hhoa.kline.core.subscription.SubscriptionManager;
import com.hhoa.kline.core.subscription.message.PartialMessage;
import com.hhoa.kline.core.subscription.message.WindowShowMessageRequestMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class Task {
    private static final ObjectMapper objectMapper = new ObjectMapper();
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
    private final java.util.function.BiConsumer<Boolean, String> updateBackgroundCommandState;
    private final java.util.function.Supplier<Boolean> shouldShowBackgroundTerminalSuggestion;
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
    @Getter private ICheckpointManager checkpointManager;
    private boolean taskLockAcquired;
    private ActiveBackgroundCommand activeBackgroundCommand;
    private NotificationService notificationService;
    private ContextFactory contextFactory;

    public Task(TaskParams params) {

        if (params == null) throw new IllegalArgumentException("params == null");
        if (params.getTaskId() == null) throw new IllegalArgumentException("taskId == null");
        if (params.getCwd() == null) throw new IllegalArgumentException("cwd == null");

        this.taskInitializationStartTime = System.nanoTime() / 1_000_000;
        this.taskId = params.getTaskId();
        this.cwd = params.getCwd();
        this.ulid = params.getUlid() != null ? params.getUlid() : generateUlid();

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

        this.focusChainManager =
                params.getFocusChainManagerFactory()
                        .createFocusChainManager(
                                this.taskId,
                                this.taskState,
                                this.postStateToWebview,
                                this::say,
                                this.telemetryService);
        this.focusChainManager.setupFocusChain();
        this.askResponseTimeout = params.getAskResponseTimeout();

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
                                Task.this.say(type, text, images, files, partial);
                                Long ts = Task.this.taskState.getLastMessageTs();
                                return CompletableFuture.completedFuture(ts);
                            }

                            @Override
                            public Runnable getPostStateToWebview() {
                                return Task.this.postStateToWebview;
                            }

                            @Override
                            public Runnable getCancelTask() {
                                return Task.this.cancelTask;
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
                        this.say(
                                ClineSay.MCP_NOTIFICATION,
                                String.format("[%s] %s", serverName, message));
                    });
        }
    }

    private static void getUsage(
            ApiChunk chunk, boolean[] didReceiveUsageChunk, ClineApiReqInfo apiReqInfo) {
        didReceiveUsageChunk[0] = true;
        if (chunk.inputTokens() != null) {
            apiReqInfo.setTokensIn(chunk.inputTokens());
        }
        if (chunk.outputTokens() != null) {
            apiReqInfo.setTokensOut(chunk.outputTokens());
        }
        if (chunk.cacheWriteTokens() != null) {
            apiReqInfo.setCacheWrites(chunk.cacheWriteTokens());
        }
        if (chunk.cacheReadTokens() != null) {
            apiReqInfo.setCacheReads(chunk.cacheReadTokens());
        }
        if (chunk.totalCost() != null) {
            apiReqInfo.setCost(chunk.totalCost());
        }
    }

    private String generateUlid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 26);
    }

    private void sendPartialMessageEvent(
            ClineMessage message, String partialContent, Boolean isUpdatingPreviousPartial) {
        if ((partialContent == null || partialContent.isEmpty()) && isUpdatingPreviousPartial) {
            return;
        }

        SubscriptionManager defaultSubscriptionManager = DefaultSubscriptionManager.getInstance();

        PartialMessage partialMessage = new PartialMessage();
        partialMessage.setTs(message.getTs());
        partialMessage.setTaskId(this.taskId);
        partialMessage.setClineMessageType(message.getType());
        partialMessage.setAsk(message.getAsk());
        partialMessage.setSay(message.getSay());
        partialMessage.setIncrementContent(partialContent);
        partialMessage.setReasoning(message.getReasoning());
        partialMessage.setImages(message.getImages());
        partialMessage.setFiles(message.getFiles());
        partialMessage.setCommandCompleted(message.getCommandCompleted());
        partialMessage.setFormat(message.getFormat());
        partialMessage.setIsUpdatingPreviousPartial(isUpdatingPreviousPartial);

        defaultSubscriptionManager.send(partialMessage);
    }

    public AskResult ask(ClineAsk type, String text, Boolean partial) {
        return ask(type, text, partial, null);
    }

    public AskResult ask(ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
        String partialContent = null;

        if (partial != null) {
            List<ClineMessage> clineMessages = this.messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();

            boolean isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());

            String newText = text != null ? text : "";

            if (isUpdatingPreviousPartial) {
                String oldText = lastMessage.getText() != null ? lastMessage.getText() : "";

                boolean shouldUseJsonDiff = ClineMessageFormat.JSON.equals(format);

                if (shouldUseJsonDiff) {
                    partialContent = PartialJsonUtils.getJsonPartialContent(text, newText, oldText);
                } else {
                    if (oldText.length() > newText.length()) {
                        log.error(
                                "Old text length larger than new text oldText: {}, newText: {}",
                                oldText,
                                newText);
                    }
                    partialContent = newText.substring(oldText.length());
                }
            } else {
                partialContent = newText;
            }
        }

        return ask(type, text, partialContent, partial, format);
    }

    public AskResult ask(ClineAsk type, String text) {
        return ask(type, text, null);
    }

    public AskResult ask(ClineAsk type, String text, String partialContent, Boolean partial) {
        return ask(type, text, partialContent, partial, null);
    }

    public AskResult ask(
            ClineAsk type,
            String text,
            String partialContent,
            Boolean partial,
            ClineMessageFormat format) {
        if (this.taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }

        AskResult result = new AskResult();

        final long askTs = nowTs();

        this.taskState.setLastMessageTs(askTs);

        ClineMessage msg =
                new ClineMessage()
                        .setTs(askTs)
                        .setType(ClineMessageType.ASK)
                        .setAsk(type)
                        .setText(text)
                        .setPartial(partial)
                        .setFormat(format);

        boolean isUpdatingPreviousPartial = false;

        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();

            isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
        }

        if (isUpdatingPreviousPartial) {
            messageStateHandler.updateClineMessage(
                    messageStateHandler.getClineMessages().size() - 1, msg);
        } else {
            messageStateHandler.addToClineMessages(msg);
        }

        sendPartialMessageEvent(msg, partialContent, isUpdatingPreviousPartial);

        if (partial == null || (!partial)) {
            resetAskResponse();

            postStateToWebview.run();

            try {
                waitFor(
                        () ->
                                taskState.getAskResponse() != null
                                        || (taskState.getLastMessageTs() != null
                                                && !taskState.getLastMessageTs().equals(askTs)),
                        Duration.ofMillis(this.askResponseTimeout));
            } catch (InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }

            result.setResponse(taskState.getAskResponse());
            result.setText(taskState.getAskResponseText());
            result.setImages(taskState.getAskResponseImages());
            result.setFiles(taskState.getAskResponseFiles());

            resetAskResponse();
            return result;
        }

        return result;
    }

    public synchronized void say(
            ClineSay type, String text, List<String> images, List<String> files, Boolean partial) {
        say(type, text, images, files, partial, null);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            List<String> images,
            List<String> files,
            Boolean partial,
            ClineMessageFormat format) {
        String incrementContent = null;

        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();

            boolean isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
            String newText = text != null ? text : "";

            if (isUpdatingPreviousPartial) {
                String oldText = lastMessage.getText() != null ? lastMessage.getText() : "";

                boolean shouldUseJsonDiff = ClineMessageFormat.JSON.equals(format);

                if (shouldUseJsonDiff) {
                    incrementContent =
                            PartialJsonUtils.getJsonPartialContent(text, newText, oldText);
                } else {
                    incrementContent = newText.substring(oldText.length());
                }
            } else {
                incrementContent = newText;
            }
        }

        say(type, text, incrementContent, images, files, partial, format);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            String incrementContent,
            List<String> images,
            List<String> files,
            Boolean partial) {
        say(type, text, incrementContent, images, files, partial, null);
    }

    public synchronized void say(
            ClineSay type,
            String text,
            String incrementContent,
            List<String> images,
            List<String> files,
            Boolean partial,
            ClineMessageFormat format) {
        if (taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }
        if ((text == null || text.isEmpty())
                && (incrementContent == null || incrementContent.isEmpty())
                && partial != null
                && partial) {
            return;
        }

        long sayTs = nowTs();

        taskState.setLastMessageTs(sayTs);
        ClineMessage msg =
                new ClineMessage()
                        .setType(ClineMessageType.SAY)
                        .setSay(type)
                        .setText(text)
                        .setImages(images)
                        .setFiles(files)
                        .setPartial(partial)
                        .setFormat(format)
                        .setTs(sayTs);

        boolean isUpdatingPreviousPartial = false;

        if (partial != null) {
            List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
            ClineMessage lastMessage = clineMessages.isEmpty() ? null : clineMessages.getLast();

            isUpdatingPreviousPartial =
                    lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial());
        }

        if (isUpdatingPreviousPartial) {
            messageStateHandler.updateClineMessage(
                    messageStateHandler.getClineMessages().size() - 1, msg);
        } else {
            messageStateHandler.addToClineMessages(msg);
        }

        sendPartialMessageEvent(msg, incrementContent, isUpdatingPreviousPartial);
    }

    public void say(ClineSay type, String text) {
        say(type, text, text, null, null, null, null);
    }

    public synchronized void handleWebviewAskResponse(
            ClineAskResponse response, String text, List<String> images, List<String> files) {
        this.taskState.setAskResponse(response);
        this.taskState.setAskResponseText(text);
        this.taskState.setAskResponseImages(images);
        this.taskState.setAskResponseFiles(files);
    }

    public void resetAskResponse() {
        this.taskState.setAskResponse(null);
        this.taskState.setAskResponseText(null);
        this.taskState.setAskResponseImages(null);
        this.taskState.setAskResponseFiles(null);
    }

    public void startTask(String taskText) {
        startTask(taskText, null, null);
    }

    public void startTask(String taskText, List<String> images, List<String> files) {
        this.messageStateHandler.setClineMessages(new ArrayList<>());
        this.messageStateHandler.getApiConversationHistory().clear();
        if (this.postStateToWebview != null) {
            this.postStateToWebview.run();
        }
        say(ClineSay.TEXT, taskText, images, files, null);
        this.taskState.setInitialized(true);

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

        initiateTaskLoop(userContent, true);
    }

    public void resumeTaskFromHistory() {
        List<ClineMessage> savedClineMessages = stateManager.getSavedClineMessages(this.taskId);

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

        this.messageStateHandler.overwriteClineMessages(savedClineMessages);

        List<MessageParam> savedApiConversationHistory =
                stateManager.getSavedApiConversationHistory(this.taskId);
        this.messageStateHandler.setApiConversationHistory(savedApiConversationHistory);

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

        this.taskState.setInitialized(true);

        AskResult askResult = this.ask(askType, null, null);

        String responseText = null;
        List<String> responseImages = null;
        List<String> responseFiles = null;

        if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())) {
            List<String> imagesList =
                    askResult.getImages() != null ? new ArrayList<>(askResult.getImages()) : null;
            List<String> filesList =
                    askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) : null;
            this.say(ClineSay.USER_FEEDBACK, askResult.getText(), imagesList, filesList, null);
            if (this.checkpointManager != null) {
                this.checkpointManager
                        .saveCheckpoint(false, null)
                        .exceptionally(
                                error -> {
                                    log.error(
                                            "Failed to save checkpoint after user feedback: {}",
                                            error.getMessage(),
                                            error);
                                    return null;
                                });
            }
            responseText = askResult.getText();
            responseImages =
                    askResult.getImages() != null ? new ArrayList<>(askResult.getImages()) : null;
            responseFiles =
                    askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) : null;
        }

        List<MessageParam> existingApiConversationHistory =
                this.messageStateHandler.getApiConversationHistory();
        List<MessageParam> modifiedApiConversationHistory = new ArrayList<>();
        List<UserContentBlock> modifiedOldUserContent = new ArrayList<>();

        if (!existingApiConversationHistory.isEmpty()) {
            MessageParam lastMessage = existingApiConversationHistory.getLast();
            if (lastMessage.getRole() != null) {
                if (lastMessage instanceof AssistantMessage) {
                    modifiedApiConversationHistory =
                            new ArrayList<>(existingApiConversationHistory);
                    modifiedOldUserContent = new ArrayList<>();
                } else if (lastMessage instanceof UserMessage userMessage) {
                    modifiedApiConversationHistory =
                            new ArrayList<>(
                                    existingApiConversationHistory.subList(
                                            0, existingApiConversationHistory.size() - 1));
                    List<UserContentBlock> content = userMessage.getContent();
                    for (UserContentBlock block : content) {
                        if (block instanceof TextContentBlock) {
                            modifiedOldUserContent.add(block);
                        }
                    }
                }
            }
        }

        List<UserContentBlock> newUserContent = new ArrayList<>(modifiedOldUserContent);

        long timestamp =
                lastClineMessage != null && lastClineMessage.getTs() != null
                        ? lastClineMessage.getTs()
                        : System.currentTimeMillis();
        long now = System.currentTimeMillis();
        String agoText = TimeUtils.getAgoText(now, timestamp);

        boolean wasRecent = timestamp > 0 && (now - timestamp) < 30000;

        List<String> pendingContextWarning =
                this.fileContextTracker.retrieveAndClearPendingFileContextWarning();

        boolean hasPendingFileContextWarnings =
                pendingContextWarning != null && !pendingContextWarning.isEmpty();
        String[] taskResumptionMessages =
                this.responseFormatter.taskResumption(
                        this.stateManager.getSettings().getMode(),
                        agoText,
                        cwd,
                        wasRecent,
                        responseText,
                        hasPendingFileContextWarnings);

        if (taskResumptionMessages != null) {
            for (String message : taskResumptionMessages) {
                if (message != null && !message.isEmpty()) {
                    TextContentBlock resumptionBlock = new TextContentBlock(message);
                    newUserContent.add(resumptionBlock);
                }
            }
        }

        if (responseImages != null && !responseImages.isEmpty()) {
            for (String image : responseImages) {
                ImageContentBlock imageBlock = new ImageContentBlock(image, "base64", "image/png");
                newUserContent.add(imageBlock);
            }
        }

        if (responseFiles != null && !responseFiles.isEmpty()) {
            String fileContentString =
                    processFilesIntoText(
                            responseFiles.stream().map(Path::of).collect(Collectors.toList()));
            if (!fileContentString.isEmpty()) {
                TextContentBlock fileBlock = new TextContentBlock(fileContentString);
                newUserContent.add(fileBlock);
            }
        }

        this.messageStateHandler.overwriteApiConversationHistory(modifiedApiConversationHistory);
        initiateTaskLoop(newUserContent, false);
    }

    private TaskConfig buildTaskConfig() {
        boolean yoloModeToggled = this.stateManager.getSettings().isYoloModeToggled();

        TaskConfig.Api api = null;
        if (this.apiHandler != null) {
            api = () -> (TaskConfig.Model) Task.this.apiHandler::getModelId;
        }

        TaskConfig.Services services =
                TaskConfig.Services.builder()
                        .contextManager(this.contextManager)
                        .stateManager(this.stateManager)
                        .mcpHub(this.mcpHub)
                        .notificationService(this.notificationService)
                        .diffViewProvider(this.diffViewProvider)
                        .clineIgnoreController(this.clineIgnoreController)
                        .fileContextTracker(this.fileContextTracker)
                        .telemetryService(this.telemetryService)
                        .build();

        AutoApprovalSettings autoApprovalSettings;
        autoApprovalSettings = this.stateManager.getSettings().getAutoApprovalSettings();

        AutoApprove autoApprover = new AutoApprove(this.stateManager, this.workspaceManager);

        TaskConfig.TaskConfigBuilder configBuilder =
                TaskConfig.builder()
                        .taskId(this.taskId)
                        .ulid(this.ulid)
                        .cwd(this.cwd)
                        .mode(this.stateManager.getSettings().getMode())
                        .taskState(this.taskState)
                        .messageState(this.messageStateHandler)
                        .services(services)
                        .autoApprovalSettings(autoApprovalSettings)
                        .callbacks(buildCallbacks(autoApprover))
                        .coordinator(this.toolExecutor)
                        .yoloModeToggled(yoloModeToggled)
                        .autoApprover(autoApprover);

        configBuilder.api(api);
        configBuilder.workspaceManager(this.workspaceManager);

        return configBuilder.build();
    }

    private TaskConfig.Callbacks buildCallbacks(AutoApprove autoApprover) {
        return new TaskConfig.Callbacks() {
            @Override
            public void say(
                    ClineSay type,
                    String text,
                    String[] images,
                    String[] files,
                    Boolean partial,
                    ClineMessageFormat format) {
                List<String> imagesList = images != null ? Arrays.asList(images) : null;
                List<String> filesList = files != null ? Arrays.asList(files) : null;
                Task.this.say(type, text, imagesList, filesList, partial, format);
            }

            @Override
            public AskResult ask(
                    ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
                try {
                    return Task.this.ask(type, text, partial, format);
                } catch (Exception e) {
                    abortTask();
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void saveCheckpoint(
                    Boolean isAttemptCompletionMessage, Long completionMessageTs) {
                if (taskState.isAbort()) {
                    return;
                }
                if (Task.this.checkpointManager != null) {
                    Task.this
                            .checkpointManager
                            .saveCheckpoint(
                                    isAttemptCompletionMessage != null
                                            && isAttemptCompletionMessage,
                                    completionMessageTs)
                            .exceptionally(
                                    error -> {
                                        log.error(
                                                "Failed to save checkpoint: {}",
                                                error.getMessage(),
                                                error);
                                        return null;
                                    })
                            .join();
                }
            }

            @Override
            public Boolean shouldAutoApproveToolWithPath(String toolName, String path) {
                return autoApprover.shouldAutoApproveToolWithPath(toolName, path, Task.this.cwd);
            }

            @Override
            public Boolean shouldAutoApproveTool(String toolName) {
                return autoApprover.shouldAutoApproveTool(toolName).asBoolean();
            }

            @Override
            public String sayAndCreateMissingParamError(String toolName, String paramName) {
                return Task.this.sayAndCreateMissingParamError(toolName, paramName, null);
            }

            @Override
            public TaskConfig.ExecuteResult executeCommandTool(
                    String command, Integer timeoutSeconds) {
                return Task.this.executeCommandTool(command, timeoutSeconds);
            }

            @Override
            public void sayUserFeedback(String text, String[] images, String[] files) {
                List<String> imagesList = images != null ? Arrays.asList(images) : null;
                List<String> filesList = files != null ? Arrays.asList(files) : null;
                Task.this.say(ClineSay.USER_FEEDBACK, text, imagesList, filesList, null);
                if (Task.this.checkpointManager != null) {
                    Task.this
                            .checkpointManager
                            .saveCheckpoint(false, null)
                            .exceptionally(
                                    error -> {
                                        log.error(
                                                "Failed to save checkpoint after user feedback: {}",
                                                error.getMessage(),
                                                error);
                                        return null;
                                    });
                }
            }

            @Override
            public Boolean switchToActMode() {
                stateManager.getSettings().setMode(Mode.ACT);
                return true;
            }

            @Override
            public Boolean updateFCListFromToolResponse(String text) {
                if (Task.this.focusChainManager == null) return false;
                try {
                    Task.this.focusChainManager.updateFCListFromToolResponse(text);
                    return true;
                } catch (Exception e) {
                    log.error("Error updating FC list from tool response: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public Boolean doesLatestTaskCompletionHaveNewChanges() {
                if (Task.this.checkpointManager != null) {
                    try {
                        return Task.this
                                .checkpointManager
                                .doesLatestTaskCompletionHaveNewChanges()
                                .get();
                    } catch (Exception e) {
                        log.error(
                                "Failed to check latest task completion changes: {}",
                                e.getMessage(),
                                e);
                        return false;
                    }
                }
                return false;
            }
        };
    }

    public synchronized void abortTask() {
        if (this.taskState.isAbort()) return;

        if (this.focusChainManager != null) {
            this.focusChainManager.checkIncompleteProgressOnCompletion();
        }

        this.taskState.setAbort(true);

        if (this.activeBackgroundCommand != null) {
            cancelBackgroundCommand();
        }

        if (this.terminalManager != null) {
            try {
                this.terminalManager.close();
            } catch (Exception e) {
                log.error("Failed to close terminal manager: {}", e.getMessage(), e);
            }
        }

        if (this.taskLockAcquired) {
            TaskLockUtils.releaseTaskLock(this.taskId);
            this.taskLockAcquired = false;
        }

        if (this.focusChainManager != null) {
            this.focusChainManager.dispose();
        }

        if (this.mcpHub != null) {
            this.mcpHub.clearNotificationCallback();
        }
    }

    public synchronized Boolean cancelBackgroundCommand() {
        TerminalExecutionMode terminalExecutionMode =
                this.stateManager.getGlobalState().getTerminalExecutionMode();
        if (terminalExecutionMode == null) {
            terminalExecutionMode = TerminalExecutionMode.VSCODE_TERMINAL;
        }
        if (terminalExecutionMode != TerminalExecutionMode.BACKGROUND_EXEC) {
            return false;
        }
        if (this.activeBackgroundCommand == null) {
            return false;
        }
        ActiveBackgroundCommand activeCmd = this.activeBackgroundCommand;
        this.activeBackgroundCommand = null;

        if (this.updateBackgroundCommandState != null) {
            this.updateBackgroundCommandState.accept(false, this.taskId);
        }

        try {
            activeCmd.process.getProcess().shutdown();
        } catch (Exception e) {
            log.error("Failed to terminate background command: " + e.getMessage());
            return false;
        }

        try {
            this.say(
                    ClineSay.COMMAND_OUTPUT,
                    "Command cancelled. Background execution has been terminated.");

            return true;
        } catch (Exception e) {
            log.error("Failed to cancel background command: " + e.getMessage());
            return false;
        }
    }

    TaskConfig.ExecuteResult executeCommandTool(String command, Integer timeoutSeconds) {
        TaskConfig.ExecuteResult er = new TaskConfig.ExecuteResult();
        er.userRejected = false;
        if (command == null || command.trim().isEmpty()) {
            er.result = "(empty command)";
            return er;
        }

        try {
            boolean isSubagent = SubagentCommandUtils.isSubagentCommand(command);
            long subAgentStartTime = isSubagent ? System.currentTimeMillis() : 0;

            if (SubagentCommandUtils.transformClineCommand(command) != command && isSubagent) {
                command = SubagentCommandUtils.transformClineCommand(command);
            }

            log.info("Executing command in terminal: {}", command);

            TerminalManager terminalManagerToUse = this.terminalManager;
            if (isSubagent) {
                terminalManagerToUse = new TerminalManager();
                terminalManagerToUse.setShellIntegrationTimeout(
                        this.terminalManager.getShellIntegrationTimeout());
                terminalManagerToUse.setTerminalReuseEnabled(
                        this.terminalManager.isTerminalReuseEnabled());
                terminalManagerToUse.setTerminalOutputLineLimit(
                        this.terminalManager.getTerminalOutputLineLimit());
                terminalManagerToUse.setSubagentTerminalOutputLineLimit(
                        this.terminalManager.getSubagentTerminalOutputLineLimit());
            }

            TerminalInfo terminalInfo = terminalManagerToUse.getOrCreateTerminal(this.cwd).join();
            terminalInfo.getTerminal().show();

            // Prepare variables before creating listeners
            final int CHUNK_LINE_COUNT = 20;
            final int CHUNK_BYTE_SIZE = 2048;
            final long CHUNK_DEBOUNCE_MS = 100;
            final long BUFFER_STUCK_TIMEOUT_MS = 6000;
            final long COMPLETION_TIMEOUT_MS = 6000;

            List<String> outputLines = new ArrayList<>();
            List<String> outputBuffer = new ArrayList<>();
            AtomicInteger bufferBytes = new AtomicInteger(0);
            AtomicBoolean didContinue = new AtomicBoolean(false);
            AtomicBoolean didCancelViaUi = new AtomicBoolean(false);
            AtomicBoolean completed = new AtomicBoolean(false);
            AtomicReference<ScheduledFuture<?>> chunkTimer = new AtomicReference<>();
            AtomicReference<ScheduledFuture<?>> bufferStuckTimer = new AtomicReference<>();
            AtomicReference<ScheduledFuture<?>> completionTimer = new AtomicReference<>();
            ScheduledExecutorService debounceExecutor =
                    Executors.newSingleThreadScheduledExecutor();

            UserFeedback userFeedback = new UserFeedback();

            final List<String> finalOutputBuffer = outputBuffer;
            final AtomicInteger finalBufferBytes = bufferBytes;
            final AtomicBoolean finalDidContinue = didContinue;
            final AtomicBoolean finalDidCancelViaUi = didCancelViaUi;
            final AtomicReference<ScheduledFuture<?>> finalChunkTimer = chunkTimer;
            final AtomicReference<ScheduledFuture<?>> finalBufferStuckTimer = bufferStuckTimer;

            final Consumer<Boolean>[] flushBufferRef = (Consumer<Boolean>[]) new Consumer[1];

            // Create listeners configuration BEFORE calling runCommand
            TerminalProcessListeners listeners = new TerminalProcessListeners();

            // Setup line listener
            listeners.onLine(
                    line -> {
                        if (didCancelViaUi.get()) {
                            return;
                        }
                        outputLines.add(line);

                        if (!didContinue.get()) {
                            outputBuffer.add(line);
                            bufferBytes.addAndGet(line.getBytes(StandardCharsets.UTF_8).length + 1);

                            if (outputBuffer.size() >= CHUNK_LINE_COUNT
                                    || bufferBytes.get() >= CHUNK_BYTE_SIZE) {
                                flushBufferRef[0].accept(false);
                            } else {
                                ScheduledFuture<?> timer = finalChunkTimer.get();
                                if (timer != null) {
                                    timer.cancel(false);
                                }
                                ScheduledFuture<?> newTimer =
                                        debounceExecutor.schedule(
                                                () -> flushBufferRef[0].accept(false),
                                                CHUNK_DEBOUNCE_MS,
                                                TimeUnit.MILLISECONDS);
                                finalChunkTimer.set(newTimer);
                            }
                        } else {
                            this.say(ClineSay.COMMAND_OUTPUT, line);
                        }
                    });

            // Create process reference holder (will be set after runCommand)
            final AtomicReference<TerminalProcessResultPromise> processRef =
                    new AtomicReference<>();

            // Setup flush buffer logic
            flushBufferRef[0] =
                    (force) -> {
                        if (finalOutputBuffer.isEmpty() && !force) {
                            return;
                        }
                        String chunk = String.join("\n", finalOutputBuffer);
                        finalOutputBuffer.clear();
                        finalBufferBytes.set(0);

                        ScheduledFuture<?> timer = finalChunkTimer.getAndSet(null);
                        if (timer != null) {
                            timer.cancel(false);
                        }

                        ScheduledFuture<?> stuckTimer = finalBufferStuckTimer.getAndSet(null);
                        if (stuckTimer != null) {
                            stuckTimer.cancel(false);
                        }

                        ScheduledFuture<?> newStuckTimer =
                                debounceExecutor.schedule(
                                        () -> {
                                            telemetryService.captureTerminalHang("buffer_stuck");
                                            finalBufferStuckTimer.set(null);
                                        },
                                        BUFFER_STUCK_TIMEOUT_MS,
                                        TimeUnit.MILLISECONDS);
                        finalBufferStuckTimer.set(newStuckTimer);

                        try {
                            AskResult askResult = this.ask(ClineAsk.COMMAND_OUTPUT, chunk);
                            String responseText = askResult.getText();
                            List<String> responseImages = askResult.getImages();
                            List<String> responseFiles = askResult.getFiles();

                            if (ClineAskResponse.YES_BUTTON_CLICKED.equals(
                                    askResult.getResponse())) {
                                telemetryService.captureTerminalUserIntervention(
                                        "process_while_running");
                                finalDidContinue.set(true);
                                TerminalProcessResultPromise process = processRef.get();
                                if (process != null) {
                                    process.continueExecution();
                                }

                                if (responseText != null
                                        || (responseImages != null && !responseImages.isEmpty())
                                        || (responseFiles != null && !responseFiles.isEmpty())) {
                                    userFeedback.text = responseText;
                                    userFeedback.images = responseImages;
                                    userFeedback.files = responseFiles;
                                }

                                if (finalDidCancelViaUi.get()) {
                                    finalOutputBuffer.clear();
                                    finalBufferBytes.set(0);
                                    this.say(ClineSay.COMMAND_OUTPUT, "Command cancelled");
                                }

                                if (!finalDidCancelViaUi.get() && !finalOutputBuffer.isEmpty()) {
                                    flushBufferRef[0].accept(false);
                                }
                            } else if (ClineAskResponse.NO_BUTTON_CLICKED.equals(
                                            askResult.getResponse())
                                    && ExtensionMessageConstants.COMMAND_CANCEL_TOKEN.equals(
                                            responseText)) {
                                telemetryService.captureTerminalUserIntervention("cancelled");
                                finalDidCancelViaUi.set(true);
                                finalOutputBuffer.clear();
                                finalBufferBytes.set(0);
                                this.say(ClineSay.COMMAND_OUTPUT, "Command cancelled");
                            } else {
                                userFeedback.text = responseText;
                                userFeedback.images = responseImages;
                                userFeedback.files = responseFiles;
                            }
                        } catch (Exception e) {
                            log.error(
                                    "Error while asking for command output: {}", e.getMessage(), e);
                        } finally {
                            ScheduledFuture<?> stuckTimerToClear =
                                    finalBufferStuckTimer.getAndSet(null);
                            if (stuckTimerToClear != null) {
                                stuckTimerToClear.cancel(false);
                            }
                        }
                    };

            // Setup completed listener
            listeners.onCompleted(
                    () -> {
                        completed.set(true);

                        ScheduledFuture<?> timer = finalChunkTimer.getAndSet(null);
                        if (timer != null) {
                            timer.cancel(false);
                        }

                        ScheduledFuture<?> completionTimerToClear = completionTimer.getAndSet(null);
                        if (completionTimerToClear != null) {
                            completionTimerToClear.cancel(false);
                        }

                        if (!finalDidContinue.get() && !finalOutputBuffer.isEmpty()) {
                            flushBufferRef[0].accept(true);
                        }
                    });

            // Setup error listener
            TerminalExecutionMode terminalExecutionMode =
                    this.stateManager.getGlobalState().getTerminalExecutionMode();
            if (terminalExecutionMode == null) {
                terminalExecutionMode = TerminalExecutionMode.VSCODE_TERMINAL;
            }
            final TerminalExecutionMode finalTerminalExecutionMode = terminalExecutionMode;
            Runnable clearCommandState =
                    () -> {
                        if (finalTerminalExecutionMode == TerminalExecutionMode.BACKGROUND_EXEC) {
                            if (this.activeBackgroundCommand == null) {
                                return;
                            }
                            this.activeBackgroundCommand = null;
                        }
                        if (this.updateBackgroundCommandState != null) {
                            this.updateBackgroundCommandState.accept(false, this.taskId);
                        }
                        List<ClineMessage> clineMessages =
                                this.messageStateHandler.getClineMessages();
                        int lastCommandIndex = -1;
                        for (int i = clineMessages.size() - 1; i >= 0; i--) {
                            ClineMessage msg = clineMessages.get(i);
                            if ((ClineAsk.COMMAND.equals(msg.getAsk())
                                    || ClineSay.COMMAND.equals(msg.getSay()))) {
                                lastCommandIndex = i;
                                break;
                            }
                        }
                        if (lastCommandIndex != -1) {
                            ClineMessage commandMessage = clineMessages.get(lastCommandIndex);
                            commandMessage.setCommandCompleted(true);
                            this.messageStateHandler.updateClineMessage(
                                    lastCommandIndex, commandMessage);
                        }
                    };
            listeners.onError(clearCommandState);
            listeners.onCompleted(clearCommandState);

            // Setup no shell integration listener
            listeners.onNoShellIntegration(
                    () -> {
                        boolean shouldShowSuggestion = false;
                        if (this.shouldShowBackgroundTerminalSuggestion != null) {
                            shouldShowSuggestion =
                                    this.shouldShowBackgroundTerminalSuggestion.get();
                        }
                        if (shouldShowSuggestion) {
                            this.say(
                                    ClineSay.SHELL_INTEGRATION_WARNING_WITH_SUGGESTION,
                                    null,
                                    null,
                                    null,
                                    null);
                        } else {
                            this.say(ClineSay.SHELL_INTEGRATION_WARNING, null, null, null, null);
                        }
                    });

            // Now call runCommand with listeners (listeners are registered BEFORE process.run() is
            // called)
            TerminalProcessResultPromise process =
                    terminalManagerToUse.runCommand(terminalInfo, command, listeners);
            processRef.set(process);

            if (this.updateBackgroundCommandState != null) {
                this.updateBackgroundCommandState.accept(true, this.taskId);
            }

            if (terminalExecutionMode == TerminalExecutionMode.BACKGROUND_EXEC) {
                ActiveBackgroundCommand activeCmd = new ActiveBackgroundCommand();
                activeCmd.process = process;
                activeCmd.command = command;
                this.activeBackgroundCommand = activeCmd;
            }

            completionTimer.set(
                    debounceExecutor.schedule(
                            () -> {
                                if (!completed.get()) {
                                    telemetryService.captureTerminalHang("waiting_for_completion");
                                    completionTimer.set(null);
                                }
                            },
                            COMPLETION_TIMEOUT_MS,
                            TimeUnit.MILLISECONDS));

            if (timeoutSeconds != null && timeoutSeconds > 0) {
                try {
                    process.getPromise().get(timeoutSeconds, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    didContinue.set(true);
                    process.continueExecution();
                    ScheduledFuture<?> timer = chunkTimer.getAndSet(null);
                    if (timer != null) {
                        timer.cancel(false);
                    }
                    ScheduledFuture<?> completionTimerToClear = completionTimer.getAndSet(null);
                    if (completionTimerToClear != null) {
                        completionTimerToClear.cancel(false);
                    }
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    String result = terminalManagerToUse.processOutput(outputLines, null, false);
                    er.result =
                            String.format(
                                    "Command execution timed out after %d seconds. %s",
                                    timeoutSeconds,
                                    result.length() > 0 ? "\nOutput so far:\n" + result : "");
                    return er;
                } catch (Exception e) {
                    log.error("Error waiting for command completion: {}", e.getMessage(), e);
                }
            } else {
                try {
                    process.getPromise().join();
                } catch (Exception e) {
                    log.error("Error waiting for command completion: {}", e.getMessage(), e);
                }
            }

            ScheduledFuture<?> completionTimerToClear = completionTimer.getAndSet(null);
            if (completionTimerToClear != null) {
                completionTimerToClear.cancel(false);
            }

            if (!didCancelViaUi.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }

            String result =
                    terminalManagerToUse.processOutput(
                            outputLines,
                            isSubagent
                                    ? terminalManagerToUse.getSubagentTerminalOutputLineLimit()
                                    : null,
                            isSubagent);

            if (isSubagent && subAgentStartTime > 0) {
                long durationMs = System.currentTimeMillis() - subAgentStartTime;
                telemetryService.captureSubagentExecution(
                        this.ulid, durationMs, outputLines.size(), completed.get());
            }

            if (didCancelViaUi.get()) {
                er.result =
                        "Command cancelled. "
                                + (result.length() > 0
                                        ? "\nOutput captured before cancellation:\n" + result
                                        : "");
                er.userRejected = true;
            } else if (userFeedback.hasFeedback()) {
                this.say(
                        ClineSay.USER_FEEDBACK,
                        userFeedback.text,
                        userFeedback.images,
                        userFeedback.files,
                        null);

                String fileContentString = "";
                if (userFeedback.files != null && !userFeedback.files.isEmpty()) {
                    fileContentString =
                            processFilesIntoText(
                                    userFeedback.files.stream()
                                            .map(Path::of)
                                            .collect(Collectors.toList()));
                }

                StringBuilder feedbackResult = new StringBuilder();
                feedbackResult.append("Command is still running in the user's terminal.");
                if (result.length() > 0) {
                    feedbackResult.append("\nHere's the output so far:\n").append(result);
                }
                feedbackResult.append(
                        "\n\nThe user provided the following feedback:\n<feedback>\n");
                if (userFeedback.text != null) {
                    feedbackResult.append(userFeedback.text);
                }
                feedbackResult.append("\n</feedback>");

                er.result = feedbackResult.toString();
                if (fileContentString != null && !fileContentString.isEmpty()) {
                    er.result += "\n\n" + fileContentString;
                }
            } else if (completed.get()) {
                er.result =
                        "Command executed." + (result.length() > 0 ? "\nOutput:\n" + result : "");
            } else {
                er.result =
                        "Command is still running in the user's terminal."
                                + (result.length() > 0
                                        ? "\nHere's the output so far:\n" + result
                                        : "")
                                + "\n\nYou will be updated on the terminal status and new output in the future.";
            }

            ScheduledFuture<?> timer = chunkTimer.getAndSet(null);
            if (timer != null) {
                timer.cancel(false);
            }
            ScheduledFuture<?> stuckTimer = bufferStuckTimer.getAndSet(null);
            if (stuckTimer != null) {
                stuckTimer.cancel(false);
            }

            if (isSubagent && terminalManagerToUse != this.terminalManager) {
                try {
                    terminalManagerToUse.close();
                } catch (Exception e) {
                    log.error("Failed to close subagent terminal manager: {}", e.getMessage(), e);
                }
            }

            debounceExecutor.shutdown();

            return er;
        } catch (Exception e) {
            er.result = "Error executing command: " + e.getMessage();
            log.error("Error executing command: {}", e.getMessage(), e);
            return er;
        }
    }

    /**
     * 创建缺失参数错误并输出
     *
     * @param toolName 工具名称
     * @param paramName 参数名称
     * @param relPath 相关路径（可选）
     * @return 格式化的错误消息
     */
    public String sayAndCreateMissingParamError(String toolName, String paramName, String relPath) {
        String message = "Cline tried to use " + toolName;
        if (relPath != null && !relPath.isEmpty()) {
            message += " for '" + relPath + "'";
        }
        message += " without value for required parameter '" + paramName + "'. Retrying...";
        this.say(ClineSay.ERROR, message);

        return "(missing param: " + paramName + ")";
    }

    private long nowTs() {
        long base = System.currentTimeMillis();
        long last = tsGenerator.get();
        long next = Math.max(base, last + 1);
        tsGenerator.set(next);
        return next;
    }

    /**
     * 进入任务循环
     *
     * @param userContent 用户内容块列表
     * @param includeFileDetails 是否包含文件详情（仅第一次需要）
     */
    public void initiateTaskLoop(List<UserContentBlock> userContent, boolean includeFileDetails) {
        List<UserContentBlock> nextUserContent = userContent;
        boolean includeDetails = includeFileDetails;

        while (!this.taskState.isAbort()) {
            try {
                boolean didEndLoop = recursivelyMakeClineRequests(nextUserContent, includeDetails);
                includeDetails = false;

                if (didEndLoop) {
                    break;
                } else {
                    nextUserContent = new ArrayList<>();
                    TextContentBlock noToolsBlock =
                            new TextContentBlock(this.responseFormatter.noToolsUsed());
                    nextUserContent.add(noToolsBlock);
                    this.taskState.setConsecutiveMistakeCount(
                            this.taskState.getConsecutiveMistakeCount() + 1);
                }
            } catch (Exception e) {
                log.error("Error in task loop: {}", e.getMessage(), e);
                break;
            }
        }
    }

    /**
     * 递归执行 Cline 请求（核心方法）
     *
     * @param userContent 用户内容块列表
     * @param includeFileDetails 是否包含文件详情
     * @return 是否结束循环
     */
    private Boolean recursivelyMakeClineRequests(
            List<UserContentBlock> userContent, boolean includeFileDetails) {

        if (this.taskState.isAbort()) {
            return true;
        }

        this.taskState.setApiRequestCount(this.taskState.getApiRequestCount() + 1);
        this.taskState.setApiRequestsSinceLastTodoUpdate(
                this.taskState.getApiRequestsSinceLastTodoUpdate() + 1);

        ProviderInfo providerInfo = getCurrentProviderInfo();
        if (providerInfo.providerId != null && providerInfo.model != null) {
            this.modelContextTracker.recordModelUsage(
                    providerInfo.providerId,
                    providerInfo.model,
                    this.stateManager.getSettings().getMode());
        }

        int maxConsecutiveMistakes = this.stateManager.getSettings().getMaxConsecutiveMistakes();
        if (this.taskState.getConsecutiveMistakeCount() >= maxConsecutiveMistakes) {
            String mistakeMessage;
            if (providerInfo.model != null && providerInfo.model.contains("claude")) {
                mistakeMessage =
                        "This may indicate a failure in his thought process or inability to use a tool properly, which can be mitigated with some user guidance (e.g. \"Try breaking down the task into smaller steps\").";
            } else {
                mistakeMessage =
                        "Cline uses complex prompts and iterative task execution that may be challenging for less capable models. For best results, it's recommended to use Claude 4 Sonnet for its advanced agentic coding capabilities.";
            }

            AskResult askResult = this.ask(ClineAsk.MISTAKE_LIMIT_REACHED, mistakeMessage, null);

            if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())) {
                List<String> imagesList =
                        askResult.getImages() != null
                                ? new ArrayList<>(askResult.getImages())
                                : null;
                List<String> filesList =
                        askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) : null;
                this.say(ClineSay.USER_FEEDBACK, askResult.getText(), imagesList, filesList, null);

                List<UserContentBlock> feedbackContent = new ArrayList<>();
                String feedbackText = askResult.getText() != null ? askResult.getText() : "";
                TextContentBlock feedbackBlock =
                        new TextContentBlock(this.responseFormatter.tooManyMistakes(feedbackText));
                feedbackContent.add(feedbackBlock);

                if (askResult.getImages() != null && !askResult.getImages().isEmpty()) {
                    for (String image : askResult.getImages()) {
                        ImageContentBlock imageBlock =
                                new ImageContentBlock(image, "base64", "image/png");
                        feedbackContent.add(imageBlock);
                    }
                }

                if (askResult.getFiles() != null && !askResult.getFiles().isEmpty()) {
                    String fileContentString =
                            processFilesIntoText(
                                    askResult.getFiles().stream()
                                            .map(Path::of)
                                            .collect(Collectors.toList()));
                    if (!fileContentString.isEmpty()) {
                        TextContentBlock fileBlock = new TextContentBlock(fileContentString);
                        feedbackContent.add(fileBlock);
                    }
                }

                userContent = feedbackContent;
            }
            this.taskState.setConsecutiveMistakeCount(0);
            this.taskState.setAutoRetryAttempts(0);
        }

        boolean yoloModeToggled =
                this.stateManager.getSettings().isYoloModeToggled(); // 默认值，实际应从配置读取
        boolean autoApprovalEnabled =
                this.stateManager.getSettings().getAutoApprovalSettings().isEnabled();
        int maxAutoApprovedRequests =
                this.stateManager.getSettings().getAutoApprovalSettings().getMaxRequests();

        if (!yoloModeToggled
                && autoApprovalEnabled
                && this.taskState.getConsecutiveAutoApprovedRequestsCount()
                        >= maxAutoApprovedRequests) {
            AskResult askResult =
                    this.ask(
                            ClineAsk.AUTO_APPROVAL_MAX_REQ_REACHED,
                            String.format(
                                    "Cline has auto-approved %d API requests. Would you like to reset the count and proceed with the task?",
                                    maxAutoApprovedRequests),
                            null);

            this.taskState.setConsecutiveAutoApprovedRequestsCount(0);

            if (ClineAskResponse.MESSAGE_RESPONSE.equals(askResult.getResponse())) {
                List<String> imagesList =
                        askResult.getImages() != null
                                ? new ArrayList<>(askResult.getImages())
                                : null;
                List<String> filesList =
                        askResult.getFiles() != null ? new ArrayList<>(askResult.getFiles()) : null;
                this.say(ClineSay.USER_FEEDBACK, askResult.getText(), imagesList, filesList, null);

                List<UserContentBlock> feedbackContent = new ArrayList<>();
                String feedbackText = askResult.getText() != null ? askResult.getText() : "";
                TextContentBlock feedbackBlock =
                        new TextContentBlock(
                                this.responseFormatter.autoApprovalMaxReached(feedbackText));
                feedbackContent.add(feedbackBlock);

                if (askResult.getImages() != null && !askResult.getImages().isEmpty()) {
                    for (String image : askResult.getImages()) {
                        ImageContentBlock imageBlock =
                                new ImageContentBlock(image, "base64", "image/png");
                        feedbackContent.add(imageBlock);
                    }
                }

                if (askResult.getFiles() != null && !askResult.getFiles().isEmpty()) {
                    String fileContentString =
                            processFilesIntoText(
                                    askResult.getFiles().stream()
                                            .map(Path::of)
                                            .collect(Collectors.toList()));
                    if (!fileContentString.isEmpty()) {
                        TextContentBlock fileBlock = new TextContentBlock(fileContentString);
                        feedbackContent.add(fileBlock);
                    }
                }

                userContent = feedbackContent;
            }
        }

        final int[] previousApiReqIndexHolder = {-1};
        List<ClineMessage> clineMessagesForIndex = this.messageStateHandler.getClineMessages();
        for (int i = clineMessagesForIndex.size() - 1; i >= 0; i--) {
            ClineMessage msg = clineMessagesForIndex.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                previousApiReqIndexHolder[0] = i;
                break;
            }
        }
        final int previousApiReqIndex = previousApiReqIndexHolder[0];

        final boolean isFirstRequest =
                clineMessagesForIndex.stream()
                        .noneMatch(
                                m ->
                                        ClineMessageType.SAY.equals(m.getType())
                                                && ClineSay.API_REQ_STARTED.equals(m.getSay()));

        String requestText =
                userContent.stream()
                        .map(
                                block -> {
                                    if (block instanceof TextContentBlock) {
                                        return ((TextContentBlock) block).getText();
                                    } else if (block instanceof ImageContentBlock) {
                                        return "[IMAGE]";
                                    }
                                    return "";
                                })
                        .filter(s -> s != null && !s.isEmpty())
                        .reduce((a, b) -> a + "\n\n" + b)
                        .orElse("");

        ClineApiReqInfo clineApiReqInfo = new ClineApiReqInfo();
        clineApiReqInfo.setRequest(requestText);
        try {
            this.say(ClineSay.API_REQ_STARTED, objectMapper.writeValueAsString(clineApiReqInfo));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (isFirstRequest
                && this.stateManager.getSettings().isEnableCheckpointsSetting()
                && this.checkpointManager != null
                && this.taskState.getCheckpointManagerErrorMessage() == null) {
            try {
                CheckpointInitializer.ensureCheckpointInitialized(
                                this.checkpointManager,
                                15_000L,
                                "Checkpoints taking too long to initialize. Consider re-opening Cline in a project that uses git, or disabling checkpoints.")
                        .get();
            } catch (Exception error) {
                String errorMessage =
                        error.getMessage() != null ? error.getMessage() : "Unknown error";
                log.error("Failed to initialize checkpoint manager: {}", errorMessage);
                this.taskState.setCheckpointManagerErrorMessage(errorMessage);
                ShowMessageRequest request =
                        ShowMessageRequest.builder()
                                .type(ShowMessageType.ERROR)
                                .message("Checkpoint initialization timed out: " + errorMessage)
                                .build();
                DefaultSubscriptionManager.getInstance()
                        .send(new WindowShowMessageRequestMessage(request));
            }
        }

        if (isFirstRequest
                && this.stateManager.getSettings().isEnableCheckpointsSetting()
                && this.checkpointManager != null) {
            this.say(ClineSay.CHECKPOINT_CREATED, null);
            int lastCheckpointMessageIndex =
                    MessageUtils.findLastIndex(
                            this.messageStateHandler.getClineMessages(),
                            m ->
                                    ClineMessageType.SAY.equals(m.getType())
                                            && ClineSay.CHECKPOINT_CREATED.equals(m.getSay()));
            if (lastCheckpointMessageIndex != -1) {
                final int finalIndex = lastCheckpointMessageIndex;
                this.checkpointManager
                        .commit()
                        .thenAccept(
                                commitHash -> {
                                    if (commitHash != null) {
                                        try {
                                            ClineMessage checkpointMessage =
                                                    this.messageStateHandler
                                                            .getClineMessages()
                                                            .get(finalIndex);
                                            checkpointMessage.setLastCheckpointHash(commitHash);
                                            this.messageStateHandler.updateClineMessage(
                                                    finalIndex, checkpointMessage);
                                        } catch (Exception e) {
                                            log.error(
                                                    "Failed to update checkpoint message: {}",
                                                    e.getMessage(),
                                                    e);
                                        }
                                    }
                                })
                        .exceptionally(
                                error -> {
                                    log.error(
                                            "Failed to create checkpoint commit for task {}: {}",
                                            this.taskId,
                                            error.getMessage());
                                    return null;
                                });
            }
        } else if (isFirstRequest
                && this.stateManager.getSettings().isEnableCheckpointsSetting()
                && this.checkpointManager == null
                && this.taskState.getCheckpointManagerErrorMessage() != null) {
        }

        boolean useAutoCondense =
                this.stateManager.getSettings().isUseAutoCondense(); // 默认值，实际应从配置读取

        boolean shouldCompact = false;

        if (useAutoCondense) {
            if (this.taskState.isCurrentlySummarizing()) {
                this.taskState.setCurrentlySummarizing(false);

                int[] deletedRange = this.taskState.getConversationHistoryDeletedRange();
                if (deletedRange != null && deletedRange.length == 2) {
                    List<MessageParam> apiHistory =
                            this.messageStateHandler.getApiConversationHistory();
                    int start = deletedRange[0];
                    int end = deletedRange[1];

                    int safeEnd = Math.min(end + 2, apiHistory.size() - 1);
                    if (end + 2 <= safeEnd) {
                        this.taskState.setConversationHistoryDeletedRange(
                                new int[] {start, end + 2});
                    }
                    this.messageStateHandler.saveClineMessagesAndUpdateHistory();
                }
            } else {
                Double autoCondenseThreshold =
                        this.stateManager.getSettings().getAutoCondenseThreshold();

                ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                        ContextWindowUtils.getContextWindowInfo(200000, providerInfo.model);
                int contextWindow = contextWindowInfo.contextWindow();
                int maxAllowedSize = contextWindowInfo.maxAllowedSize();

                shouldCompact =
                        this.contextManager.shouldCompactContextWindow(
                                this.messageStateHandler.getClineMessages(),
                                contextWindow,
                                maxAllowedSize,
                                previousApiReqIndex,
                                autoCondenseThreshold);

                if (shouldCompact && this.taskState.getConversationHistoryDeletedRange() != null) {
                    List<MessageParam> apiHistory =
                            this.messageStateHandler.getApiConversationHistory();
                    int[] deletedRange = this.taskState.getConversationHistoryDeletedRange();
                    int activeMessageCount = apiHistory.size() - deletedRange[1] - 1;
                    if (activeMessageCount <= 2) {
                        shouldCompact = false;
                    }
                }
            }
        }

        final boolean finalShouldCompact = shouldCompact;

        LoadContextResult loadResult;
        if (useAutoCondense) {
            if (finalShouldCompact) {
                loadResult = new LoadContextResult(new ArrayList<>(userContent), "", false);
                this.taskState.setLastAutoCompactTriggerIndex(previousApiReqIndex);
            } else {
                loadResult = loadContext(userContent, includeFileDetails, false);
            }
        } else {
            boolean useCompactPrompt = "compact".equals(providerInfo.customPrompt);
            loadResult = loadContext(userContent, includeFileDetails, useCompactPrompt);
        }

        try {
            if (loadResult.clinerulesError()) {
                this.say(
                        ClineSay.ERROR,
                        "Issue with processing the /newrule command. Double check that, if '.clinerules' already exists, it's a directory and not a file. Otherwise there was an issue referencing this file/directory.",
                        null,
                        null,
                        null);
            }

            List<UserContentBlock> processedContent =
                    new ArrayList<>(loadResult.processedUserContent());

            if (!finalShouldCompact
                    && loadResult.environmentDetails() != null
                    && !loadResult.environmentDetails().isEmpty()) {
                TextContentBlock envBlock = new TextContentBlock(loadResult.environmentDetails());
                processedContent.add(envBlock);
            }

            if (finalShouldCompact) {
                String summarizePrompt =
                        ContextManagement.summarizeTask(
                                stateManager.getSettings().getFocusChainSettings().isEnabled(),
                                cwd,
                                false);
                TextContentBlock summarizeBlock = new TextContentBlock(summarizePrompt);
                processedContent.add(summarizeBlock);
            }

            this.messageStateHandler.addToApiConversationHistory(
                    buildApiUserMessage(processedContent));

            telemetryService.captureConversationTurnEvent(
                    this.ulid, providerInfo.providerId, providerInfo.model, "user");

            if (isFirstRequest) {
                long durationMs = System.currentTimeMillis() - this.taskInitializationStartTime;
                telemetryService.captureTaskInitialization(
                        this.ulid,
                        this.taskId,
                        durationMs,
                        this.stateManager.getSettings().isEnableCheckpointsSetting());
            }

            String finalRequestText =
                    processedContent.stream()
                            .map(
                                    block -> {
                                        if (block instanceof TextContentBlock) {
                                            return ((TextContentBlock) block).getText();
                                        } else if (block instanceof ImageContentBlock) {
                                            return "[IMAGE]";
                                        }
                                        return "";
                                    })
                            .filter(s -> s != null && !s.isEmpty())
                            .reduce((a, b) -> a + "\n\n" + b)
                            .orElse("");

            List<ClineMessage> messages = this.messageStateHandler.getClineMessages();
            for (int i = messages.size() - 1; i >= 0; i--) {
                ClineMessage msg = messages.get(i);
                if (ClineMessageType.SAY.equals(msg.getType())
                        && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {

                    ClineApiReqInfo apiReqInfo = new ClineApiReqInfo();
                    apiReqInfo.setRequest(finalRequestText);
                    msg.setText(objectMapper.writeValueAsString(apiReqInfo));

                    this.messageStateHandler.updateClineMessage(i, msg);
                    break;
                }
            }

            if (this.postStateToWebview != null) {
                try {
                    this.postStateToWebview.run();
                } catch (Exception e) {
                    log.error("Error in postStateToWebview callback: {}", e.getMessage(), e);
                }
            }

            return processAssistantResponse(previousApiReqIndex);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = cause.getMessage();

            log.error("Error in recursivelyMakeClineRequests: {}", errorMessage, e);

            boolean isContextWindowExceeded =
                    errorMessage != null
                            && (errorMessage.contains("context window")
                                    || errorMessage.contains("token limit")
                                    || errorMessage.contains("maximum context length"));

            if (isContextWindowExceeded
                    && !this.taskState.isDidAutomaticallyRetryFailedApiRequest()) {
                try {
                    handleContextWindowExceededError();
                    this.taskState.setDidAutomaticallyRetryFailedApiRequest(true);
                    return recursivelyMakeClineRequests(userContent, includeFileDetails);
                } catch (Exception retryError) {
                    log.error(
                            "Error handling context window exceeded: {}",
                            retryError.getMessage(),
                            retryError);
                }
            }

            this.say(
                    ClineSay.ERROR,
                    "Failed to process request: "
                            + (errorMessage != null ? errorMessage : "Unknown error"),
                    null,
                    null,
                    null);

            AskResult askResult =
                    this.ask(
                            ClineAsk.API_REQ_FAILED,
                            "Error processing request. Would you like to retry?",
                            null);

            if (ClineAskResponse.YES_BUTTON_CLICKED.equals(askResult.getResponse())) {
                this.taskState.setAutoRetryAttempts(0);
                return recursivelyMakeClineRequests(userContent, includeFileDetails);
            } else {
                return true;
            }
        }
    }

    private UserMessage buildApiUserMessage(List<UserContentBlock> userContent) {
        List<UserContentBlock> content = new ArrayList<>();
        for (UserContentBlock block : userContent) {
            if (block instanceof TextContentBlock) {
                TextContentBlock textBlock =
                        new TextContentBlock(((TextContentBlock) block).getText());
                content.add(textBlock);
            } else if (block instanceof ImageContentBlock imageBlock) {
                ImageContentBlock newImageBlock =
                        new ImageContentBlock(
                                imageBlock.getSource(),
                                imageBlock.getSourceType() != null
                                        ? imageBlock.getSourceType()
                                        : "base64",
                                imageBlock.getMediaType() != null
                                        ? imageBlock.getMediaType()
                                        : "image/png");
                content.add(newImageBlock);
            }
        }
        return UserMessage.builder().content(content).build();
    }

    /**
     * 加载上下文
     *
     * <p>返回三元组：[processedUserContent, environmentDetails, clinerulesError] >
     *
     * @param userContent 用户内容块列表
     * @param includeFileDetails 是否包含详细文件信息
     * @param useCompactPrompt 是否使用紧凑提示
     */
    private LoadContextResult loadContext(
            List<UserContentBlock> userContent,
            boolean includeFileDetails,
            boolean useCompactPrompt) {

        List<UserContentBlock> processed = new ArrayList<>();
        boolean needsClinerulesFileCheck = false;

        for (UserContentBlock block : userContent) {
            if (block instanceof TextContentBlock textBlock) {
                String text = textBlock.getText();
                if (text == null) {
                    continue;
                }
                if (text.contains("<feedback>")
                        || text.contains("<answer>")
                        || text.contains("<task>")
                        || text.contains("<user_message>")) {
                    text =
                            Mentions.parseMentions(
                                            text,
                                            this.cwd,
                                            null,
                                            this.fileContextTracker,
                                            this.workspaceManager,
                                            null, // workspaceClient - 移除 HostProvider 依赖
                                            null, // windowClient - 移除 HostProvider 依赖
                                            this.telemetryService)
                                    .join();

                    SlashCommandParser.SlashCommandParseResult slashCommandParseResult =
                            SlashCommandParser.parseSlashCommands(
                                    text, null, null, null, null, this.telemetryService);
                    text = slashCommandParseResult.getProcessedText();
                    if (slashCommandParseResult.isNeedsClinerulesFileCheck()) {
                        needsClinerulesFileCheck = true;
                    }
                }
                TextContentBlock processedBlock = new TextContentBlock(text);
                processed.add(processedBlock);
            } else {
                processed.add(block);
            }
        }

        if (!useCompactPrompt
                && this.focusChainManager != null
                && this.focusChainManager.shouldIncludeFocusChainInstructions()) {
            String focusChainInstructions = this.focusChainManager.generateFocusChainInstructions();
            if (focusChainInstructions != null && !focusChainInstructions.isBlank()) {
                TextContentBlock fcBlock = new TextContentBlock(focusChainInstructions);
                processed.add(fcBlock);
                this.taskState.setApiRequestsSinceLastTodoUpdate(0);
                this.taskState.setTodoListWasUpdatedByUser(false);
            }
        }

        String environmentDetails = getEnvironmentDetails(includeFileDetails);

        boolean clinerulesError = false;
        if (needsClinerulesFileCheck) {
            try {
                Path clinerulesPath = Paths.get(this.cwd, ".clinerules");
                if (Files.exists(clinerulesPath) && !Files.isDirectory(clinerulesPath)) {
                    clinerulesError = true;
                }
            } catch (Exception e) {
                clinerulesError = true;
            }
        }

        return new LoadContextResult(processed, environmentDetails, clinerulesError);
    }

    private String getEnvironmentDetails(boolean includeFileDetails) {
        StringBuilder details = new StringBuilder();
        details.append("<environment_details>\n");

        details.append("# Current Working Directory\n");
        details.append(this.cwd).append("\n");

        ZonedDateTime now = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss a");
        details.append("\n# Current Time\n");
        details.append(now.format(formatter))
                .append(" (")
                .append(now.getZone())
                .append(", UTC")
                .append(now.getOffset().getTotalSeconds() / 3600 >= 0 ? "+" : "")
                .append(now.getOffset().getTotalSeconds() / 3600)
                .append(":00)\n");

        details.append("\n# Current Mode\n");
        Mode mode = this.stateManager.getSettings().getMode();
        if (mode == Mode.PLAN) {
            details.append("PLAN MODE\n");
            details.append(this.responseFormatter.planModeInstructions()).append("\n");
        } else {
            details.append("ACT MODE\n");
        }

        if (includeFileDetails) {
            details.append("\n# Current Working Directory Files\n");
            try {
                Path cwdPath = Paths.get(this.cwd);
                if (Files.exists(cwdPath) && Files.isDirectory(cwdPath)) {
                    List<String> fileList;
                    boolean didHitLimit = false;

                    try (Stream<Path> stream = Files.walk(cwdPath, 2)) {
                        fileList =
                                stream.filter(
                                                path -> {
                                                    String fileName =
                                                            path.getFileName() != null
                                                                    ? path.getFileName().toString()
                                                                    : "";
                                                    return !fileName.startsWith(".")
                                                            && !fileName.equals("node_modules")
                                                            && !fileName.equals("target")
                                                            && !fileName.equals("build");
                                                })
                                        .map(Path::toString)
                                        .limit(201) // 限制文件数量，+1 用于检测是否超过限制
                                        .collect(Collectors.toList());
                    }

                    if (!fileList.isEmpty()) {
                        String formattedList =
                                this.responseFormatter.formatFilesList(
                                        this.cwd,
                                        fileList,
                                        didHitLimit,
                                        this.clineIgnoreController);
                        details.append(formattedList).append("\n");
                    } else {
                        details.append("(No files found)\n");
                    }
                } else {
                    details.append("(Directory not accessible)\n");
                }
            } catch (Exception e) {
                details.append("(Error listing files: ").append(e.getMessage()).append(")\n");
            }
        }

        details.append("\n# Context Window Usage\n");
        try {
            ProviderInfo providerInfo = getCurrentProviderInfo();
            ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                    ContextWindowUtils.getContextWindowInfo(200000, providerInfo.model);
            int contextWindow = contextWindowInfo.contextWindow();

            int lastApiReqTotalTokens =
                    MessageUtils.getApiReqInfo(this.messageStateHandler.getClineMessages())
                            .f1
                            .getTotalTokens();

            int usagePercentage =
                    lastApiReqTotalTokens > 0
                            ? (int)
                                    Math.round(
                                            (lastApiReqTotalTokens / (double) contextWindow) * 100)
                            : 0;

            details.append(
                    String.format(
                            "%s / %sK tokens used (%d%%)\n",
                            formatNumber(lastApiReqTotalTokens),
                            formatNumber(contextWindow / 1000),
                            usagePercentage));
        } catch (Exception e) {
            details.append("0 / 200K tokens used (0%)\n");
        }

        details.append("</environment_details>");
        return details.toString();
    }

    private String formatNumber(int number) {
        return String.format("%,d", number);
    }

    private Boolean processAssistantResponse(int previousApiReqIndex) {
        this.taskState.setCurrentStreamingContentIndex(0);
        this.taskState.setAssistantMessageContent(new ArrayList<>());
        this.taskState.setDidCompleteReadingStream(false);
        this.taskState.setUserMessageContent(new ArrayList<>());
        this.taskState.setUserMessageContentReady(false);
        this.taskState.setDidRejectTool(false);
        this.taskState.setDidAlreadyUseTool(false);
        this.taskState.setDidAutomaticallyRetryFailedApiRequest(false);

        if (this.diffViewProvider != null) {
            try {
                this.diffViewProvider.reset();
            } catch (Exception e) {
                log.error("Failed to reset diff view provider: " + e.getMessage());
            }
        }

        this.taskState.setStreaming(true);

        List<MessageParam> apiConversationHistory =
                this.messageStateHandler.getApiConversationHistory();
        List<ClineMessage> clineMessages = this.messageStateHandler.getClineMessages();

        ProviderInfo providerInfo = getCurrentProviderInfo();
        ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                ContextWindowUtils.getContextWindowInfo(200000, providerInfo.model);
        int contextWindow = contextWindowInfo.contextWindow();
        int maxAllowedSize = contextWindowInfo.maxAllowedSize();

        int[] conversationHistoryDeletedRange = this.taskState.getConversationHistoryDeletedRange();

        boolean useAutoCondense = this.stateManager.getSettings().isUseAutoCondense();
        ContextManager.ContextMessagesResult contextResult =
                this.contextManager.getNewContextMessagesAndMetadata(
                        apiConversationHistory,
                        clineMessages,
                        contextWindow,
                        maxAllowedSize,
                        conversationHistoryDeletedRange,
                        previousApiReqIndex,
                        useAutoCondense);

        if (contextResult.isUpdatedConversationHistoryDeletedRange()) {
            this.taskState.setConversationHistoryDeletedRange(
                    contextResult.getConversationHistoryDeletedRange());
            this.messageStateHandler.saveClineMessagesAndUpdateHistory();
        }

        List<MessageParam> truncatedConversationHistory =
                contextResult.getTruncatedConversationHistory();

        try {
            ApiRequestResult apiResult = attemptApiRequest(truncatedConversationHistory, null);

            this.taskState.setDidCompleteReadingStream(true);

            checkAndPresentAssistantMessage(true);

            updateApiReqMessage(
                    apiResult.getApiReqInfo().getTokensIn(),
                    apiResult.getApiReqInfo().getTokensOut(),
                    apiResult.getApiReqInfo().getCacheWrites(),
                    apiResult.getApiReqInfo().getCacheReads(),
                    apiResult.getApiReqInfo().getCost(),
                    apiResult.getApiReqInfo().getCancelReason(),
                    apiResult.getApiReqInfo().getStreamingFailedMessage());

            String assistantMessage = apiResult.getAssistantMessage();
            if (assistantMessage == null || assistantMessage.trim().isEmpty()) {
                return processEmptyAssistantMessage();
            } else {
                telemetryService.captureConversationTurnEvent(
                        this.ulid, providerInfo.providerId, providerInfo.model, "assistant");

                List<UserContentBlock> antThinkingContent = apiResult.getAntThinkingContent();
                List<Object> reasoningDetails = apiResult.getReasoningDetails();
                this.messageStateHandler.addToApiConversationHistory(
                        buildApiAssistantMessage(
                                assistantMessage, antThinkingContent, reasoningDetails));

                waitForUserMessageContentReady();

                List<AssistantMessageContent> contentBlocks =
                        messageParser.parseAssistantMessage(assistantMessage);

                for (AssistantMessageContent item : contentBlocks) {
                    if (item.isPartial()) {
                        item.setPartial(false);
                    }
                }

                this.taskState.setAssistantMessageContent(contentBlocks);
                this.taskState.setCurrentStreamingContentIndex(0);

                this.taskState.setStreaming(false);

                List<ClineMessage> messagesForUpdate = this.messageStateHandler.getClineMessages();

                int lastApiReqIndexForUpdate = -1;
                for (int i = messagesForUpdate.size() - 1; i >= 0; i--) {
                    ClineMessage msg = messagesForUpdate.get(i);
                    if (ClineMessageType.SAY.equals(msg.getType())
                            && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                        lastApiReqIndexForUpdate = i;
                        break;
                    }
                }

                if (lastApiReqIndexForUpdate >= 0) {
                    ClineApiReqCancelReason cancelReason =
                            apiResult.getApiReqInfo().getCancelReason();
                    updateApiReqMessage(
                            apiResult.getApiReqInfo().getTokensIn(),
                            apiResult.getApiReqInfo().getTokensOut(),
                            apiResult.getApiReqInfo().getCacheWrites(),
                            apiResult.getApiReqInfo().getCacheReads(),
                            apiResult.getApiReqInfo().getCost(),
                            cancelReason,
                            apiResult.getApiReqInfo().getStreamingFailedMessage());
                    try {
                        this.messageStateHandler.saveClineMessagesAndUpdateHistory();
                    } catch (Exception e) {
                        log.error(
                                "Failed to save messages after updating token usage: {}",
                                e.getMessage());
                    }
                }

                boolean didToolUse =
                        contentBlocks.stream().anyMatch(block -> block instanceof ToolUse);

                if (!didToolUse) {
                    TextContentBlock noToolsBlock =
                            new TextContentBlock(this.responseFormatter.noToolsUsed());
                    this.taskState.getUserMessageContent().add(noToolsBlock);
                    this.taskState.setConsecutiveMistakeCount(
                            this.taskState.getConsecutiveMistakeCount() + 1);
                }

                this.taskState.setAutoRetryAttempts(0);

                List<UserContentBlock> nextUserContent = this.taskState.getUserMessageContent();
                return recursivelyMakeClineRequests(nextUserContent, false);
            }
        } catch (Exception e) {
            return true;
        }
    }

    private Boolean processEmptyAssistantMessage() {
        ProviderInfo providerInfo = getCurrentProviderInfo();

        log.error(
                "[EmptyAssistantMessage] ulid={}, providerId={}, modelId={}, requestId={}",
                this.ulid,
                providerInfo.providerId,
                providerInfo.model,
                this.apiHandler.getLastRequestId());

        telemetryService.captureProviderApiError(
                this.ulid,
                providerInfo.model,
                "empty_assistant_message",
                providerInfo.providerId,
                null,
                this.apiHandler.getLastRequestId());

        String baseErrorMessage =
                "Invalid API Response: The provider returned an empty or unparsable response. "
                        + "This is a provider-side issue where the model failed to generate valid output or returned tool calls that Cline cannot process. "
                        + "Retrying the request may help resolve this issue.";
        String errorText =
                this.apiHandler.getLastRequestId() != null
                                && !this.apiHandler.getLastRequestId().isEmpty()
                        ? baseErrorMessage
                                + " (Request ID: "
                                + this.apiHandler.getLastRequestId()
                                + ")"
                        : baseErrorMessage;

        this.say(ClineSay.ERROR, errorText, null, null, null);

        this.messageStateHandler.addToApiConversationHistory(
                buildApiAssistantMessage("Failure: I did not provide a response.", null, null));

        ClineAskResponse response;

        if (this.taskState.getAutoRetryAttempts() < 3) {
            // Auto-retry enabled with max 3 attempts: automatically approve the retry
            this.taskState.setAutoRetryAttempts(this.taskState.getAutoRetryAttempts() + 1);

            // Calculate delay: 2s, 4s, 8s
            int delayMs = 2000 * (1 << (this.taskState.getAutoRetryAttempts() - 1));
            response = ClineAskResponse.YES_BUTTON_CLICKED;

            ObjectNode retryInfo = objectMapper.createObjectNode();
            retryInfo.put("attempt", this.taskState.getAutoRetryAttempts());
            retryInfo.put("maxAttempts", 3);
            retryInfo.put("delaySeconds", delayMs / 1000);

            this.say(ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfo), null, null, null);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } else {
            ObjectNode retryInfo = objectMapper.createObjectNode();
            retryInfo.put("attempt", 3);
            retryInfo.put("maxAttempts", 3);
            retryInfo.put("delaySeconds", 0);
            retryInfo.put("failed", true);
            this.say(ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfo), null, null, null);

            AskResult askResult =
                    this.ask(
                            ClineAsk.API_REQ_FAILED,
                            "No assistant message was received. Would you like to retry the request?",
                            null);
            response = askResult.getResponse();

            if (ClineAskResponse.YES_BUTTON_CLICKED.equals(response)) {
                this.taskState.setAutoRetryAttempts(0);
            }
        }

        return !ClineAskResponse.YES_BUTTON_CLICKED.equals(response);
    }

    /**
     * 调用 API 并处理流式响应
     *
     * @param conversationHistory 对话历史
     * @return API 请求结果（包含助手消息和元数据）
     */
    private ApiRequestResult attemptApiRequest(
            List<MessageParam> conversationHistory, StringBuilder unCompleteAssistantMessage)
            throws Exception {
        SystemPromptContext context = buildSystemPromptContext(unCompleteAssistantMessage != null);
        String systemPrompt = systemPromptService.getSystemPrompt(context);

        StringBuilder assistantMessageBuilder =
                new StringBuilder(
                        unCompleteAssistantMessage == null ? "" : unCompleteAssistantMessage);
        StringBuilder reasoningMessageBuilder = new StringBuilder();

        final boolean[] didReceiveUsageChunk = {false};
        final ClineApiReqInfo apiReqInfo = new ClineApiReqInfo();

        final List<Object> reasoningDetailsList = new ArrayList<>();

        final List<UserContentBlock> antThinkingContentList = new ArrayList<>();

        final boolean[] streamInterrupted = {false};

        int lastApiReqIndex = -1;
        List<ClineMessage> messagesForIndex = this.messageStateHandler.getClineMessages();
        for (int i = messagesForIndex.size() - 1; i >= 0; i--) {
            ClineMessage msg = messagesForIndex.get(i);
            if (ClineMessageType.SAY.equals(msg.getType())
                    && ClineSay.API_REQ_STARTED.equals(msg.getSay())) {
                lastApiReqIndex = i;
                break;
            }
        }

        try {
            Flux<ApiChunk> chunkFlux = getApiChunkFlux(systemPrompt, conversationHistory);

            chunkFlux
                    .takeWhile(
                            chunk -> {
                                if (this.taskState.isAbort() || this.taskState.isDidRejectTool()) {
                                    return false;
                                }
                                if (this.taskState.isDidAlreadyUseTool()) {
                                    return !didReceiveUsageChunk[0];
                                }
                                return true;
                            })
                    .flatMap(
                            chunk ->
                                    Mono.deferContextual(
                                            ctx ->
                                                    Mono.fromRunnable(
                                                                    () ->
                                                                            contextFactory
                                                                                    .runWithContext(
                                                                                            ctx,
                                                                                            () -> {
                                                                                                if (!streamInterrupted[
                                                                                                        0]) {
                                                                                                    processApiChunk(
                                                                                                            chunk,
                                                                                                            streamInterrupted,
                                                                                                            assistantMessageBuilder,
                                                                                                            didReceiveUsageChunk,
                                                                                                            apiReqInfo,
                                                                                                            reasoningMessageBuilder,
                                                                                                            reasoningDetailsList,
                                                                                                            antThinkingContentList);

                                                                                                    if (this
                                                                                                            .taskState
                                                                                                            .isAbort()) {
                                                                                                        streamInterrupted[
                                                                                                                        0] =
                                                                                                                true;
                                                                                                        apiReqInfo
                                                                                                                .setCancelReason(
                                                                                                                        ClineApiReqCancelReason
                                                                                                                                .USER_CANCELLED);
                                                                                                        abortStream(
                                                                                                                assistantMessageBuilder
                                                                                                                        .toString(),
                                                                                                                apiReqInfo,
                                                                                                                ClineApiReqCancelReason
                                                                                                                        .USER_CANCELLED,
                                                                                                                null);
                                                                                                        return;
                                                                                                    }

                                                                                                    if (this
                                                                                                            .taskState
                                                                                                            .isDidRejectTool()) {
                                                                                                        if (!streamInterrupted[
                                                                                                                        0]
                                                                                                                || !ClineApiReqCancelReason
                                                                                                                        .USER_FEEDBACK
                                                                                                                        .equals(
                                                                                                                                apiReqInfo
                                                                                                                                        .getCancelReason())) {
                                                                                                            streamInterrupted[
                                                                                                                            0] =
                                                                                                                    true;
                                                                                                            apiReqInfo
                                                                                                                    .setCancelReason(
                                                                                                                            ClineApiReqCancelReason
                                                                                                                                    .USER_FEEDBACK);
                                                                                                            assistantMessageBuilder
                                                                                                                    .append(
                                                                                                                            "\n\n[Response interrupted by user feedback]");
                                                                                                        }
                                                                                                        return;
                                                                                                    }
                                                                                                    if (this
                                                                                                            .taskState
                                                                                                            .isDidAlreadyUseTool()) {
                                                                                                        // 只在第一次检测到 didAlreadyUseTool 时设置中断标志和添加消息
                                                                                                        // 避免在处理后续 chunk（如 usage chunk）时重复添加消息
                                                                                                        if (!streamInterrupted[
                                                                                                                0]) {
                                                                                                            streamInterrupted[
                                                                                                                            0] =
                                                                                                                    true;
                                                                                                            assistantMessageBuilder
                                                                                                                    .append(
                                                                                                                            "\n\n[Response interrupted by a tool use result. Only one tool may be used at a time and should be placed at the end of the message.]");
                                                                                                        }
                                                                                                    }
                                                                                                } else {
                                                                                                    if (chunk.type()
                                                                                                            .equals(
                                                                                                                    ApiChunk
                                                                                                                            .ChunkType
                                                                                                                            .USAGE)) {
                                                                                                        getUsage(
                                                                                                                chunk,
                                                                                                                didReceiveUsageChunk,
                                                                                                                apiReqInfo);
                                                                                                    }
                                                                                                }
                                                                                            }))
                                                            .then(Mono.just(chunk))))
                    .contextWrite((ctx) -> contextFactory.modifyContext(ctx))
                    .blockLast();
        } catch (Exception streamError) {
            handleApiRequestError(streamError, lastApiReqIndex);
        } finally {
            this.taskState.setStreaming(false);
        }

        // OpenRouter/Cline 等提供者可能不会在流中返回 token usage（因为流可能提前终止），
        // 所以在流结束后尝试获取 usage 信息
        if (!didReceiveUsageChunk[0]) {
            ApiHandler.ApiStreamUsage apiStreamUsage = this.apiHandler.getApiStreamUsage();
            if (apiStreamUsage != null) {
                apiReqInfo.setTokensIn(apiStreamUsage.inputTokens());
                apiReqInfo.setTokensOut(apiStreamUsage.outputTokens());
                apiReqInfo.setCacheWrites(apiStreamUsage.cacheWriteTokens());
                apiReqInfo.setCacheReads(apiStreamUsage.cacheReadTokens());
                apiReqInfo.setCost(apiStreamUsage.totalCost());
            }
        }

        if (!reasoningMessageBuilder.isEmpty()) {
            this.say(ClineSay.REASONING, reasoningMessageBuilder.toString(), null, null, false);
            reasoningMessageBuilder.setLength(0);
        }

        String finalAssistantMessage = assistantMessageBuilder.toString();
        if (streamInterrupted[0]) {
            if (ClineApiReqCancelReason.USER_CANCELLED == apiReqInfo.getCancelReason()) {
                finalAssistantMessage += "\n\n[Response interrupted by user]";
            }
        }

        if (this.taskState.isAbort()) {
            throw new RuntimeException("Cline instance aborted");
        }

        ApiRequestResult result = new ApiRequestResult();
        result.setAssistantMessage(finalAssistantMessage);
        result.setReasoningDetails(new ArrayList<>(reasoningDetailsList));
        result.setAntThinkingContent(new ArrayList<>(antThinkingContentList));

        result.setApiReqInfo(apiReqInfo);

        checkAndProcessTruncatedContent(
                conversationHistory, finalAssistantMessage, assistantMessageBuilder, result);

        return result;
    }

    private void checkAndProcessTruncatedContent(
            List<MessageParam> conversationHistory,
            String finalAssistantMessage,
            StringBuilder assistantMessageBuilder,
            ApiRequestResult result)
            throws Exception {
        List<AssistantMessageContent> contentBlocks =
                messageParser.parseAssistantMessage(finalAssistantMessage);
        boolean hasTruncatedToolUse =
                contentBlocks.stream()
                        .anyMatch(block -> block.isPartial() && "tool_use".equals(block.getType()));

        if (hasTruncatedToolUse) {
            List<MessageParam> completionHistory = new ArrayList<>(conversationHistory);

            AssistantMessage errorPrompt =
                    AssistantMessage.builder()
                            .content(
                                    List.of(
                                            new TextContentBlock(
                                                    "Generating content exceeds maxtokens limit.")))
                            .build();
            completionHistory.add(errorPrompt);

            String nextContinuePrompt =
                    responseFormatter.completeTruncatedContent(finalAssistantMessage);

            UserMessage build =
                    UserMessage.builder()
                            .content(List.of(new TextContentBlock(nextContinuePrompt)))
                            .build();
            completionHistory.add(build);

            ApiRequestResult completionResult =
                    attemptApiRequest(completionHistory, assistantMessageBuilder);

            String completionMessage = completionResult.getAssistantMessage();
            if (completionMessage != null && !completionMessage.trim().isEmpty()) {
                result.setAssistantMessage(finalAssistantMessage + completionMessage);
                ClineApiReqInfo completionApiReqInfo = completionResult.getApiReqInfo();
                completionApiReqInfo.setTokensIn(
                        completionApiReqInfo.getTokensIn() + result.getApiReqInfo().getTokensIn());
                completionApiReqInfo.setTokensOut(
                        completionApiReqInfo.getTokensOut()
                                + result.getApiReqInfo().getTokensOut());
                completionApiReqInfo.setCacheWrites(
                        completionApiReqInfo.getCacheWrites()
                                + result.getApiReqInfo().getCacheWrites());
                completionApiReqInfo.setCacheReads(
                        completionApiReqInfo.getCacheReads()
                                + result.getApiReqInfo().getCacheReads());
                completionApiReqInfo.setCost(
                        completionApiReqInfo.getCost() + result.getApiReqInfo().getCost());
            }
        }
    }

    private void handleApiRequestError(Exception streamError, int lastApiReqIndex)
            throws Exception {
        // abandoned happens when extension is no longer waiting for the cline instance
        // to finish aborting
        if (!this.taskState.isAbandoned()) {
            ProviderInfo providerInfo = getCurrentProviderInfo();

            String errorMessage = ExceptionUtils.stringifyException(streamError);
            if (errorMessage == null) {
                Throwable cause =
                        streamError.getCause() != null ? streamError.getCause() : streamError;
                errorMessage = cause.getMessage();
            }
            if (errorMessage == null) {
                errorMessage = "Streaming failed";
            }

            log.error(
                    "[StreamingError] ulid={}, providerId={}, modelId={}, requestId={}, error={}",
                    this.ulid,
                    providerInfo.providerId,
                    providerInfo.model,
                    this.apiHandler.getLastRequestId(),
                    errorMessage,
                    streamError);

            telemetryService.captureProviderApiError(
                    this.ulid,
                    providerInfo.model,
                    errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage,
                    providerInfo.providerId,
                    null,
                    this.apiHandler.getLastRequestId());

            // Auto-retry for streaming failures (always enabled)
            if (this.taskState.getAutoRetryAttempts() < 3) {
                this.taskState.setAutoRetryAttempts(this.taskState.getAutoRetryAttempts() + 1);

                // Calculate exponential backoff for streaming failures: 2s, 4s, 8s
                int delayMs = 2000 * (1 << (this.taskState.getAutoRetryAttempts() - 1));

                // API Request component is updated to show error message, we then display retry
                // information underneath that...
                try {
                    ObjectNode retryInfo = objectMapper.createObjectNode();
                    retryInfo.put("attempt", this.taskState.getAutoRetryAttempts());
                    retryInfo.put("maxAttempts", 3);
                    retryInfo.put("delaySeconds", delayMs / 1000);
                    this.say(
                            ClineSay.ERROR_RETRY,
                            objectMapper.writeValueAsString(retryInfo),
                            null,
                            null,
                            null);
                } catch (Exception sayError) {
                    log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
                }

                // Wait with exponential backoff before auto-resuming
                try {
                    Thread.sleep(delayMs);
                    handleWebviewAskResponse(
                            ClineAskResponse.YES_BUTTON_CLICKED, "", List.of(), List.of());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            } else {
                // Show error_retry with failed flag to indicate all retries exhausted
                try {
                    ObjectNode retryInfo = objectMapper.createObjectNode();
                    retryInfo.put("attempt", 3);
                    retryInfo.put("maxAttempts", 3);
                    retryInfo.put("delaySeconds", 0);
                    retryInfo.put("failed", true); // Special flag to indicate retries exhausted
                    this.say(
                            ClineSay.ERROR_RETRY,
                            objectMapper.writeValueAsString(retryInfo),
                            null,
                            null,
                            null);
                } catch (Exception sayError) {
                    log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
                }
            }

            // needs to happen after the say, otherwise the say would fail
            // if the stream failed, there's various states the task could be in (i.e. could
            // have streamed some tools the user may have executed), so we just resort to
            // replicating a cancel task
            this.abortTask();

            if (lastApiReqIndex >= 0) {
                try {
                    updateApiReqMessage(
                            0,
                            0,
                            0,
                            0,
                            null,
                            ClineApiReqCancelReason.STREAMING_FAILED,
                            errorMessage);
                } catch (Exception updateError) {
                    log.error(
                            "Failed to update API request message: {}",
                            updateError.getMessage(),
                            updateError);
                }
            }

            // 注意：Java 版本不调用 reinitExistingTaskFromId，因为这需要 TaskManager 的引用
            // 流式错误应该通过异常向上传播，让调用方决定如何处理
            throw new RuntimeException("Streaming failed: " + errorMessage, streamError);
        } else {
            // 已放弃，直接抛出异常
            throw streamError;
        }
    }

    /** 获取 API Chunk Flux，并处理第一个 chunk 以提前发现错误 */
    private Flux<ApiChunk> getApiChunkFlux(
            String systemPrompt, List<MessageParam> conversationHistory) {
        Flux<ApiChunk> chunkFlux =
                this.apiHandler.createMessageStream(
                        systemPrompt != null ? systemPrompt : "", conversationHistory);

        this.taskState.setWaitingForFirstChunk(true);

        final AtomicBoolean isFirstChunk = new AtomicBoolean(true);
        return chunkFlux
                .doOnSubscribe(
                        subscription -> {
                            this.taskState.setWaitingForFirstChunk(true);
                        })
                .doOnNext(
                        chunk -> {
                            if (isFirstChunk.compareAndSet(true, false)) {
                                this.taskState.setWaitingForFirstChunk(false);
                            }
                        })
                .onErrorResume(
                        throwable -> {
                            this.taskState.setWaitingForFirstChunk(false);
                            if (isFirstChunk.get()) {
                                return handleApiReqException(
                                        throwable, systemPrompt, conversationHistory);
                            }
                            return Flux.empty();
                        })
                .doOnComplete(
                        () -> {
                            this.taskState.setWaitingForFirstChunk(false);
                        })
                .doOnCancel(
                        () -> {
                            this.taskState.setWaitingForFirstChunk(false);
                        });
    }

    /** 处理 API 请求异常，返回 Flux<ApiChunk> 以支持递归重试 */
    private Flux<ApiChunk> handleApiReqException(
            Throwable e, String systemPrompt, List<MessageParam> conversationHistory) {
        boolean isContextWindowExceededError =
                ContextErrorHandling.checkContextWindowExceededError(e);
        ProviderInfo providerInfo = getCurrentProviderInfo();

        String errorMessage = e.getMessage();
        if (errorMessage == null) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            errorMessage = cause.getMessage();
        }
        if (errorMessage == null) {
            errorMessage = "Unknown error";
        }

        log.error(
                "[ApiRequestError] ulid={}, providerId={}, modelId={}, requestId={}, error={}",
                this.ulid,
                providerInfo.providerId,
                providerInfo.model,
                this.apiHandler.getLastRequestId(),
                errorMessage,
                e);

        telemetryService.captureProviderApiError(
                this.ulid,
                providerInfo.model,
                errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage,
                providerInfo.providerId,
                null, // errorStatus - 可以尝试从异常中提取，但简化处理
                this.apiHandler.getLastRequestId());

        if (isContextWindowExceededError
                && !this.taskState.isDidAutomaticallyRetryFailedApiRequest()) {
            // 自动处理上下文窗口超出错误
            try {
                handleContextWindowExceededError();
                this.taskState.setDidAutomaticallyRetryFailedApiRequest(true);

                List<MessageParam> truncatedMessages =
                        this.contextManager.getTruncatedMessages(
                                this.messageStateHandler.getApiConversationHistory(),
                                this.taskState.getConversationHistoryDeletedRange());

                // 重新调用（递归）- 返回新的 Flux
                return getApiChunkFlux(systemPrompt, truncatedMessages);
            } catch (Exception retryError) {
                // 重试失败，继续到下面的错误处理逻辑
                log.error(
                        "Failed to handle context window exceeded error: {}",
                        retryError.getMessage(),
                        retryError);
            }
        }

        // request failed after retrying automatically once, ask user if they want to
        // retry again
        // note that this api_req_failed ask is unique in that we only present this
        // option if the api hasn't streamed any content yet
        String finalErrorMessage = errorMessage;
        if (isContextWindowExceededError) {
            List<MessageParam> truncatedConversationHistory =
                    this.contextManager.getTruncatedMessages(
                            this.messageStateHandler.getApiConversationHistory(),
                            this.taskState.getConversationHistoryDeletedRange());

            // If the conversation has more than 3 messages, we can truncate again. If not,
            // then the conversation is bricked.
            if (truncatedConversationHistory.size() > 3) {
                finalErrorMessage =
                        "Context window exceeded. Click retry to truncate the conversation and try again.";
                this.taskState.setDidAutomaticallyRetryFailedApiRequest(false);
            }
        }

        String streamingFailedMessage = finalErrorMessage;

        updateApiReqMessage(
                null,
                null,
                null,
                null,
                null,
                ClineApiReqCancelReason.STREAMING_FAILED,
                streamingFailedMessage);

        // Check if this is a Cline provider insufficient credits error - don't
        // auto-retry these
        boolean isClineProviderInsufficientCredits = false;

        ClineAskResponse response;
        if (!isClineProviderInsufficientCredits && this.taskState.getAutoRetryAttempts() < 3) {
            // Auto-retry enabled with max 3 attempts: automatically approve the retry
            this.taskState.setAutoRetryAttempts(this.taskState.getAutoRetryAttempts() + 1);

            // Calculate delay: 2s, 4s, 8s
            int delayMs = 2000 * (1 << (this.taskState.getAutoRetryAttempts() - 1));

            updateApiReqMessage(
                    0,
                    0,
                    0,
                    0,
                    null,
                    ClineApiReqCancelReason.STREAMING_FAILED,
                    streamingFailedMessage);
            try {
                this.messageStateHandler.saveClineMessagesAndUpdateHistory();
            } catch (Exception saveError) {
                log.error("Failed to save messages: {}", saveError.getMessage(), saveError);
            }
            this.postStateToWebview.run();

            response = ClineAskResponse.YES_BUTTON_CLICKED;
            try {
                ObjectNode retryInfo = objectMapper.createObjectNode();
                retryInfo.put("attempt", this.taskState.getAutoRetryAttempts());
                retryInfo.put("maxAttempts", 3);
                retryInfo.put("delaySeconds", delayMs / 1000);
                this.say(
                        ClineSay.ERROR_RETRY,
                        objectMapper.writeValueAsString(retryInfo),
                        null,
                        null,
                        null);
            } catch (Exception sayError) {
                log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
            }

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during retry delay", ie);
            }
        } else {
            // Show error_retry with failed flag to indicate all retries exhausted (but not
            // for insufficient credits)
            if (!isClineProviderInsufficientCredits) {
                try {
                    ObjectNode retryInfo = objectMapper.createObjectNode();
                    retryInfo.put("attempt", 3);
                    retryInfo.put("maxAttempts", 3);
                    retryInfo.put("delaySeconds", 0);
                    retryInfo.put("failed", true); // Special flag to indicate retries exhausted
                    this.say(
                            ClineSay.ERROR_RETRY,
                            objectMapper.writeValueAsString(retryInfo),
                            null,
                            null,
                            null);
                } catch (Exception sayError) {
                    log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
                }
            }

            AskResult askResult = this.ask(ClineAsk.API_REQ_FAILED, streamingFailedMessage, null);
            response = askResult.getResponse();
            if (ClineAskResponse.YES_BUTTON_CLICKED.equals(response)) {
                this.taskState.setAutoRetryAttempts(0);
            }
        }

        if (!ClineAskResponse.YES_BUTTON_CLICKED.equals(response)) {
            // this will never happen since if noButtonClicked, we will clear current task,
            // aborting this instance
            return Flux.error(new RuntimeException("API request failed"));
        }

        updateApiReqMessage(null, null, null, null, null, null, "");

        this.say(ClineSay.API_REQ_RETRIED, null, null, null, null);

        this.taskState.setDidAutomaticallyRetryFailedApiRequest(false);

        return getApiChunkFlux(systemPrompt, conversationHistory);
    }

    private void processApiChunk(
            ApiChunk chunk,
            boolean[] streamInterrupted,
            StringBuilder assistantMessageBuilder,
            boolean[] didReceiveUsageChunk,
            ClineApiReqInfo apiReqInfo,
            StringBuilder reasoningMessageBuilder,
            List<Object> reasoningDetailsList,
            List<UserContentBlock> antThinkingContentList) {
        ApiChunk.ChunkType type = chunk.type();
        if (type == null) {
            return;
        }

        switch (type) {
            case USAGE:
                getUsage(chunk, didReceiveUsageChunk, apiReqInfo);
                break;
            case REASONING:
                String reasoning = chunk.reasoning();
                if (reasoning != null && !reasoning.isEmpty()) {
                    reasoningMessageBuilder.append(reasoning);
                    // 流式更新推理消息（partial=true）
                    // 修复 bug：取消任务时可能正在流式传输推理，say 函数会抛出错误
                    // 增量计算统一在 say 方法中处理
                    if (!this.taskState.isAbort()) {
                        this.say(
                                ClineSay.REASONING,
                                reasoningMessageBuilder.toString(),
                                null,
                                null,
                                true);
                    }
                }
                break;

            case REASONING_DETAILS:
                Object reasoningDetails = chunk.reasoningDetails();
                if (reasoningDetails != null) {
                    if (reasoningDetails instanceof List) {
                        List<Object> detailsList = (List<Object>) reasoningDetails;
                        reasoningDetailsList.addAll(detailsList);
                    } else {
                        reasoningDetailsList.add(reasoningDetails);
                    }
                }
                break;

            case ANT_THINKING:
                String thinking = chunk.thinking();
                String signature = chunk.signature();
                if (thinking != null) {
                    ThinkingContentBlock thinkingBlock =
                            new ThinkingContentBlock(thinking, signature);
                    antThinkingContentList.add(thinkingBlock);
                }
                break;

            case ANT_REDACTED_THINKING:
                String data = chunk.data();
                if (data != null) {
                    RedactedThinkingContentBlock redactedBlock =
                            new RedactedThinkingContentBlock(data);
                    antThinkingContentList.add(redactedBlock);
                }
                break;

            case TEXT:
                {
                    if (!reasoningMessageBuilder.isEmpty() && assistantMessageBuilder.isEmpty()) {
                        this.say(
                                ClineSay.REASONING,
                                reasoningMessageBuilder.toString(),
                                null,
                                null,
                                false);
                        reasoningMessageBuilder.setLength(0);
                    }

                    String text = chunk.text();
                    if (text != null && !text.isEmpty()) {
                        String oldAssistantMessage = assistantMessageBuilder.toString();
                        assistantMessageBuilder.append(text);
                        String newAssistantMessage = assistantMessageBuilder.toString();

                        updateAssistantMessageContent(newAssistantMessage, oldAssistantMessage);
                    }
                    break;
                }

            default:
                break;
        }
    }

    /**
     * 更新助手消息内容
     *
     * <p>在流式响应过程中，每次收到新的文本块时调用此方法更新助手消息内容。 新消息会被加入队列，由 presentAssistantMessage 统一处理。
     *
     * <p>
     */
    private void updateAssistantMessageContent(
            String assistantMessage, String oldAssistantMessage) {
        if (this.taskState.isAbort()) {
            return;
        }

        AssistantMessageUpdate update =
                new AssistantMessageUpdate(assistantMessage, oldAssistantMessage);
        this.messageQueue.offer(update);

        checkAndPresentAssistantMessage(false);
    }

    /**
     * 处理单个助手消息更新（从队列中取出后调用）
     *
     * <p>增量传输优化：计算并设置增量内容到 TextContent 和 TaskState
     */
    private void processAssistantMessageUpdate(TextContent newTextBlock, TextContent oldTextBlock) {
        String currentText = newTextBlock.getContent();
        String oldText = oldTextBlock == null ? "" : oldTextBlock.getContent();
        boolean hasIncrementalContent = false;
        if (currentText != null && !currentText.isEmpty()) {
            String incrementalContent = null;
            if (currentText.length() > oldText.length() && currentText.startsWith(oldText)) {
                // 新内容是在旧内容基础上追加的
                incrementalContent = currentText.substring(oldText.length());
                hasIncrementalContent = true;
            } else if (oldText.length() > currentText.length() && oldText.startsWith(currentText)) {
                // (实际这种情况不应该且不会出现)
                incrementalContent = "";
                hasIncrementalContent = true;
                log.warn(
                        "Old text length is greater than new text length and old text starts with new text, oldText: {}, newText: {}",
                        oldText,
                        currentText);
            } else if (!currentText.equals(oldText)) {
                // 内容完全不同，发送完整内容（首次或内容被替换, 这种情况应该不应该且不会出现）
                incrementalContent = currentText;
                hasIncrementalContent = true;
                log.warn(
                        "Content is different, sending full content, oldText: {}, newText: {}",
                        oldText,
                        currentText);
            }

            if (incrementalContent != null) {
                newTextBlock.setIncrementalContent(incrementalContent);
            }
        }

        if (hasIncrementalContent) {
            this.taskState.setUserMessageContentReady(false);
        }
    }

    private void checkAndPresentAssistantMessage(boolean wait) {
        boolean lockAcquired = false;
        try {
            if (wait) {
                lock.lock();
                lockAcquired = true;
            } else {
                lockAcquired = lock.tryLock();
            }

            if (lockAcquired) {
                AssistantMessageUpdate poll = messageQueue.poll();
                if (taskState.isDidAlreadyUseTool()
                        || taskState.isDidRejectTool()
                        || taskState.isAbort()) {
                    messageQueue.clear();
                    return;
                }
                if (poll != null) {
                    presentAssistantMessage(poll);
                }
            }
        } catch (Exception e) {
            log.error("Error in checkAndPresentAssistantMessage", e);
        } finally {
            if (lockAcquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void presentAssistantMessage(AssistantMessageUpdate update) {
        if (this.taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }

        if (update != null) {
            presentAssistantMessageContent(update);
            checkAndPresentAssistantMessage(false);
        } else {
            checkAndPresentAssistantMessage(false);
        }
    }

    private void presentAssistantMessageContent(AssistantMessageUpdate update) {
        if (this.taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }

        try {
            List<AssistantMessageContent> newContentBlocks =
                    messageParser.parseAssistantMessage(update.assistantMessage(), true);
            if (newContentBlocks.isEmpty()) {
                return;
            }
            int currentIndex = this.taskState.getCurrentStreamingContentIndex();
            List<AssistantMessageContent> oldContentBlocks =
                    this.taskState.getAssistantMessageContent();

            if (newContentBlocks.size() > oldContentBlocks.size()) {
                this.taskState.setUserMessageContentReady(false);
            }

            AssistantMessageContent currentContentBlock;
            try {
                currentContentBlock = newContentBlocks.get(currentIndex);
            } catch (IndexOutOfBoundsException e) {
                log.error(
                        "IndexOutOfBoundsException: currentIndex={}, newContentBlocks.size={}",
                        currentIndex,
                        newContentBlocks.size());
                return;
            }

            AssistantMessageContent oldMessageContent =
                    currentIndex < oldContentBlocks.size()
                            ? oldContentBlocks.get(currentIndex)
                            : null;
            if (currentContentBlock instanceof TextContent textContent) {
                processAssistantMessageUpdate(textContent, (TextContent) oldMessageContent);
            }

            this.taskState.setAssistantMessageContent(new ArrayList<>(newContentBlocks));

            oldContentBlocks = this.taskState.getAssistantMessageContent();

            if (currentIndex >= oldContentBlocks.size()) {
                if (this.taskState.isDidCompleteReadingStream()) {
                    this.taskState.setUserMessageContentReady(true);
                }
                checkAndPresentAssistantMessage(false);
                return;
            }

            AssistantMessageContent block = oldContentBlocks.get(currentIndex);

            if (block instanceof TextContent textContent) {
                processTextContent(textContent);
            } else if (block instanceof ToolUse toolUse) {
                if (!this.taskState.isDidAlreadyUseTool()) {
                    TaskConfig config = buildTaskConfig();
                    this.toolExecutor.executeTool(toolUse, config);
                }
            }

            boolean isComplete =
                    !block.isPartial()
                            || this.taskState.isDidRejectTool()
                            || this.taskState.isDidAlreadyUseTool();

            List<AssistantMessageContent> currentAssistantContent =
                    this.taskState.getAssistantMessageContent();

            if (isComplete) {
                if (currentIndex == currentAssistantContent.size() - 1) {
                    this.taskState.setUserMessageContentReady(true);
                }

                this.taskState.setCurrentStreamingContentIndex(currentIndex + 1);

                if (this.taskState.getCurrentStreamingContentIndex()
                        < currentAssistantContent.size()) {
                    presentAssistantMessageContent(update);
                }
            }
        } catch (Exception e) {
            log.error("Error in presentAssistantMessageContent: {}", e.getMessage(), e);
        }
    }

    private void processTextContent(TextContent textContent) {
        if (this.taskState.isDidRejectTool() || this.taskState.isDidAlreadyUseTool()) {
            return;
        }

        String incrementalContent = textContent.getIncrementalContent();

        try {
            this.say(
                    ClineSay.TEXT,
                    textContent.getContent(),
                    incrementalContent,
                    null,
                    null,
                    textContent.isPartial());
        } catch (Exception e) {
            log.error("Error saying text: {}", e.getMessage());
        }
    }

    private void waitForUserMessageContentReady() {
        try {
            waitFor(
                    () -> this.taskState.isUserMessageContentReady() || this.taskState.isAbort(),
                    Duration.ofMillis(this.askResponseTimeout));
        } catch (InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private AssistantMessage buildApiAssistantMessage(
            String assistantMessage,
            List<UserContentBlock> antThinkingContent,
            List<Object> reasoningDetails) {
        List<UserContentBlock> content = new ArrayList<>();

        if (antThinkingContent != null && !antThinkingContent.isEmpty()) {
            content.addAll(antThinkingContent);
        }

        TextContentBlock textBlock = new TextContentBlock(assistantMessage, reasoningDetails);
        content.add(textBlock);

        return AssistantMessage.builder().content(content).build();
    }

    private void updateApiReqMessage(
            Integer inputTokens,
            Integer outputTokens,
            Integer cacheWriteTokens,
            Integer cacheReadTokens,
            Double totalCost,
            ClineApiReqCancelReason cancelReason,
            String streamingFailedMessage) {
        List<ClineMessage> messages = this.messageStateHandler.getClineMessages();
        Tuple2<Integer, ClineMessage> tuple =
                MessageUtils.updateApiReqMessage(
                        messages,
                        inputTokens,
                        outputTokens,
                        cacheWriteTokens,
                        cacheReadTokens,
                        totalCost,
                        cancelReason,
                        streamingFailedMessage);
        if (tuple != null) {
            this.messageStateHandler.updateClineMessage(tuple.f0, tuple.f1);
        }
    }

    private void abortStream(
            String assistantMessage,
            ClineApiReqInfo apiReqInfo,
            ClineApiReqCancelReason cancelReason,
            String streamingFailedMessage) {
        if (this.diffViewProvider != null && this.diffViewProvider.isEditing()) {
            try {
                this.diffViewProvider.revertChanges();
            } catch (Exception e) {
                log.error("Failed to revert changes in diff view provider: {}", e.getMessage(), e);
            }
        }

        List<ClineMessage> clineMessages = this.messageStateHandler.getClineMessages();
        if (!clineMessages.isEmpty()) {
            ClineMessage lastMessage = clineMessages.getLast();
            if (lastMessage != null && Boolean.TRUE.equals(lastMessage.getPartial())) {
                lastMessage.setPartial(false);
                log.debug("Updating partial message: {}", lastMessage);
            }
        }

        String interruptMessage;
        if (ClineApiReqCancelReason.STREAMING_FAILED.equals(cancelReason)) {
            interruptMessage = "Response interrupted by API Error";
        } else {
            interruptMessage = "Response interrupted by user";
        }

        String finalAssistantMessage = assistantMessage + "\n\n[" + interruptMessage + "]";

        AssistantMessage interruptedMessage =
                buildApiAssistantMessage(finalAssistantMessage, null, null);
        this.messageStateHandler.addToApiConversationHistory(interruptedMessage);

        updateApiReqMessage(
                apiReqInfo.getTokensIn(),
                apiReqInfo.getTokensOut(),
                apiReqInfo.getCacheWrites(),
                apiReqInfo.getCacheReads(),
                apiReqInfo.getCost(),
                cancelReason,
                streamingFailedMessage);

        ProviderInfo providerInfo = getCurrentProviderInfo();
        telemetryService.captureConversationTurnEvent(
                this.ulid, providerInfo.providerId, providerInfo.model, "assistant");

        this.taskState.setDidFinishAbortingStream(true);
    }

    private void handleContextWindowExceededError() {
        List<MessageParam> apiConversationHistory =
                this.messageStateHandler.getApiConversationHistory();

        if (apiConversationHistory.size() > 4) {
            int[] currentRange = this.taskState.getConversationHistoryDeletedRange();
            int[] newRange =
                    this.contextManager.getNextTruncationRange(
                            apiConversationHistory, currentRange, KeepStrategy.QUARTER);

            this.taskState.setConversationHistoryDeletedRange(newRange);

            this.messageStateHandler.saveClineMessagesAndUpdateHistory();

            this.contextManager.triggerApplyStandardContextTruncationNoticeChange(
                    System.currentTimeMillis(), apiConversationHistory);

            this.say(
                    ClineSay.TEXT,
                    String.format(
                            "Context window exceeded. Truncated conversation history (removed messages %d-%d).",
                            newRange[0], newRange[1]),
                    null,
                    null,
                    null);
        } else {
            this.say(
                    ClineSay.ERROR,
                    "Context window exceeded, but conversation history is too short to truncate. "
                            + "Please provide a shorter initial task or start a new conversation.",
                    null,
                    null,
                    null);
        }

        this.messageStateHandler.saveClineMessagesAndUpdateHistory();

        this.taskState.setDidAutomaticallyRetryFailedApiRequest(true);
    }

    private ProviderInfo getCurrentProviderInfo() {
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

    private SystemPromptContext buildSystemPromptContext(boolean hasTruncatedConversationHistory) {
        SystemPromptContext context = new SystemPromptContext();

        context.setCwd(this.cwd);

        ProviderInfo providerInfo = getCurrentProviderInfo();
        SystemPromptContext.ApiProviderInfo apiProviderInfo =
                new SystemPromptContext.ApiProviderInfo();
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setMaxTokens(8000);

        SystemPromptContext.ApiHandlerModel apiHandlerModel =
                new SystemPromptContext.ApiHandlerModel();
        apiHandlerModel.setId(providerInfo.model);
        apiHandlerModel.setModelInfo(modelInfo);
        apiProviderInfo.setModel(apiHandlerModel);
        apiProviderInfo.setCustomPrompt(providerInfo.customPrompt);
        context.setProviderInfo(apiProviderInfo);

        context.setSupportsBrowserUse(
                stateManager.getSettings().getBrowserSettings().getRemoteBrowserEnabled());

        context.setMcpHub(mcpHub);

        FocusChainSettings focusChainSettings = stateManager.getSettings().getFocusChainSettings();
        focusChainSettings.setEnabled(focusChainSettings.isEnabled());
        context.setFocusChainSettings(focusChainSettings);

        String globalClineRulesInstructions = null;
        String globalClineRulesDir = stateManager.getGlobalClineRulesDirectory();
        if (globalClineRulesDir != null) {
            Map<String, Boolean> toggles =
                    new HashMap<>(stateManager.getSettings().getGlobalClineRulesToggles());
            globalClineRulesInstructions =
                    ClineRules.getGlobalClineRules(globalClineRulesDir, toggles);
        }
        context.setGlobalClineRulesFileInstructions(globalClineRulesInstructions);

        context.setClineIgnoreInstructions(null);

        String preferredLanguageRaw = this.stateManager.getSettings().getPreferredLanguage();
        LanguageKey preferredLanguage = getLanguageKey(preferredLanguageRaw);
        String preferredLanguageInstructions =
                preferredLanguage != null
                                && !preferredLanguage.equals(LanguageKey.DEFAULT_LANGUAGE_SETTINGS)
                        ? "# Preferred Language\n\nSpeak in " + preferredLanguage + "."
                        : "";
        context.setPreferredLanguageInstructions(preferredLanguageInstructions);

        context.setBrowserSettings(stateManager.getSettings().getBrowserSettings());

        context.setYoloModeToggled(stateManager.getSettings().isYoloModeToggled());

        context.setWorkspaceRoots(workspaceManager.getRoots());

        context.setIsSubagentsEnabledAndCliInstalled(
                stateManager.getSettings().isSubagentsEnabled());
        context.setIsCliSubagent(stateManager.getSettings().isSubagentsEnabled());

        context.setHasTruncatedConversationHistory(hasTruncatedConversationHistory);

        return context;
    }

    private static class UserFeedback {
        String text;
        List<String> images;
        List<String> files;

        boolean hasFeedback() {
            return (text != null && !text.isEmpty())
                    || (images != null && !images.isEmpty())
                    || (files != null && !files.isEmpty());
        }
    }

    private static class ActiveBackgroundCommand {
        TerminalProcessResultPromise process;
        String command;
    }

    private static class ProviderInfo {
        String model;
        String providerId;
        String customPrompt;
    }
}
