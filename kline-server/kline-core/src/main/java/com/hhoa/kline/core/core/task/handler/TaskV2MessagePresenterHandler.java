package com.hhoa.kline.core.core.task.handler;

import com.hhoa.kline.core.common.Tuple2;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.parser.StreamingAssistantMessageParser;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.tracking.FileContextTracker;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.checkpoints.ICheckpointManager;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.integrations.notifications.NotificationService;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.AssistantMessageUpdate;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.MessageUtils;
import com.hhoa.kline.core.core.task.ProviderInfo;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.focuschain.FocusChainManager;
import com.hhoa.kline.core.core.task.tools.AutoApprove;
import com.hhoa.kline.core.core.task.tools.ToolExecutor;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken.ToolUsePendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class TaskV2MessagePresenterHandler {

    private final StreamingAssistantMessageParser messageParser;
    private final TelemetryService telemetryService;
    private final DiffViewProvider diffViewProvider;
    private final TaskState taskState;
    private final BlockingQueue<AssistantMessageUpdate> messageQueue;
    private final ReentrantLock lock;
    private final StateManager stateManager;
    private final WorkspaceRootManager workspaceManager;
    private final ApiHandler apiHandler;
    private final ContextManager contextManager;
    private final IMcpHub mcpHub;
    private final NotificationService notificationService;
    private final ClineIgnoreController clineIgnoreController;
    private final FileContextTracker fileContextTracker;
    private final ToolExecutor toolExecutor;
    private final TaskV2SayAskHandler sayAskHandler;
    private final MessageStateHandler messageStateHandler;
    private final Supplier<ProviderInfo> getCurrentProviderInfo;
    private final Supplier<ICheckpointManager> getCheckpointManager;
    private final Supplier<FocusChainManager> getFocusChainManager;
    private final BiFunction<String, Integer, ToolContext.ExecuteResult> executeCommandToolFn;
    private final String ulid;
    private final String taskId;
    private final String cwd;

    public TaskV2MessagePresenterHandler(
            StreamingAssistantMessageParser messageParser,
            TelemetryService telemetryService,
            DiffViewProvider diffViewProvider,
            TaskState taskState,
            BlockingQueue<AssistantMessageUpdate> messageQueue,
            ReentrantLock lock,
            StateManager stateManager,
            WorkspaceRootManager workspaceManager,
            ApiHandler apiHandler,
            ContextManager contextManager,
            IMcpHub mcpHub,
            NotificationService notificationService,
            ClineIgnoreController clineIgnoreController,
            FileContextTracker fileContextTracker,
            Supplier<ICheckpointManager> getCheckpointManager,
            Supplier<FocusChainManager> getFocusChainManager,
            BiFunction<String, Integer, ToolContext.ExecuteResult> executeCommandToolFn,
            ToolExecutor toolExecutor,
            TaskV2SayAskHandler sayAskHandler,
            MessageStateHandler messageStateHandler,
            Supplier<ProviderInfo> getCurrentProviderInfo,
            String taskId,
            String ulid,
            String cwd) {
        this.messageParser = messageParser;
        this.telemetryService = telemetryService;
        this.diffViewProvider = diffViewProvider;
        this.taskState = taskState;
        this.messageQueue = messageQueue;
        this.lock = lock;
        this.stateManager = stateManager;
        this.workspaceManager = workspaceManager;
        this.apiHandler = apiHandler;
        this.contextManager = contextManager;
        this.mcpHub = mcpHub;
        this.notificationService = notificationService;
        this.clineIgnoreController = clineIgnoreController;
        this.fileContextTracker = fileContextTracker;
        this.toolExecutor = toolExecutor;
        this.sayAskHandler = sayAskHandler;
        this.messageStateHandler = messageStateHandler;
        this.getCurrentProviderInfo = getCurrentProviderInfo;
        this.getCheckpointManager = getCheckpointManager;
        this.getFocusChainManager = getFocusChainManager;
        this.executeCommandToolFn = executeCommandToolFn;
        this.ulid = ulid;
        this.taskId = taskId;
        this.cwd = cwd;
    }

    private ToolContext buildToolContext(ToolState toolState) {
        boolean yoloModeToggled = stateManager.getSettings().isYoloModeToggled();

        ToolContext.Api api = null;
        if (apiHandler != null) {
            api = () -> (ToolContext.Model) apiHandler::getModelId;
        }

        ToolContext.Services services =
                ToolContext.Services.builder()
                        .contextManager(contextManager)
                        .stateManager(stateManager)
                        .mcpHub(mcpHub)
                        .notificationService(notificationService)
                        .diffViewProvider(diffViewProvider)
                        .clineIgnoreController(clineIgnoreController)
                        .fileContextTracker(fileContextTracker)
                        .telemetryService(telemetryService)
                        .build();

        AutoApprovalSettings autoApprovalSettings =
                stateManager.getSettings().getAutoApprovalSettings();

        AutoApprove autoApprover = new AutoApprove(stateManager, workspaceManager);

        ToolContext.ToolContextBuilder configBuilder =
                ToolContext.builder()
                        .taskId(taskId)
                        .ulid(ulid)
                        .cwd(cwd)
                        .mode(stateManager.getSettings().getMode())
                        .taskState(taskState)
                        .messageState(messageStateHandler)
                        .services(services)
                        .autoApprovalSettings(autoApprovalSettings)
                        .callbacks(buildCallbacks(autoApprover))
                        .coordinator(toolExecutor)
                        .yoloModeToggled(yoloModeToggled)
                        .toolState(toolState)
                        .autoApprover(autoApprover);

        configBuilder.api(api);
        configBuilder.workspaceManager(workspaceManager);
        return configBuilder.build();
    }

    private ToolContext.Callbacks buildCallbacks(AutoApprove autoApprover) {
        return new ToolContext.Callbacks() {
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
                sayAskHandler.say(type, text, imagesList, filesList, partial, format);
            }

            @Override
            public AskPending ask(
                    ClineAsk type, String text, Boolean partial, ClineMessageFormat format) {
                return sayAskHandler.ask(type, text, partial, format);
            }

            @Override
            public void saveCheckpoint(
                    Boolean isAttemptCompletionMessage, Long completionMessageTs) {
                if (taskState.isAbort()) {
                    return;
                }
                ICheckpointManager cm = getCheckpointManager.get();
                if (cm != null) {
                    cm.saveCheckpoint(
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
                return autoApprover.shouldAutoApproveToolWithPath(toolName, path, cwd);
            }

            @Override
            public Boolean shouldAutoApproveTool(String toolName) {
                return autoApprover.shouldAutoApproveTool(toolName).asBoolean();
            }

            @Override
            public String sayAndCreateMissingParamError(String toolName, String paramName) {
                return sayAskHandler.sayAndCreateMissingParamError(toolName, paramName, null);
            }

            @Override
            public ToolContext.ExecuteResult executeCommandTool(
                    String command, Integer timeoutSeconds) {
                return executeCommandToolFn.apply(command, timeoutSeconds);
            }

            @Override
            public void sayUserFeedback(String text, String[] images, String[] files) {
                List<String> imagesList = images != null ? Arrays.asList(images) : null;
                List<String> filesList = files != null ? Arrays.asList(files) : null;

                sayAskHandler.say(ClineSay.USER_FEEDBACK, text, imagesList, filesList, null, null);

                ICheckpointManager cm = getCheckpointManager.get();
                if (cm != null) {
                    cm.saveCheckpoint(false, null)
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
                FocusChainManager fcm = getFocusChainManager.get();
                if (fcm == null) {
                    return false;
                }
                try {
                    fcm.updateFCListFromToolResponse(text);
                    return true;
                } catch (Exception e) {
                    log.error("Error updating FC list from tool response: {}", e.getMessage());
                    return false;
                }
            }

            @Override
            public Boolean doesLatestTaskCompletionHaveNewChanges() {
                ICheckpointManager cm = getCheckpointManager.get();
                if (cm != null) {
                    try {
                        return cm.doesLatestTaskCompletionHaveNewChanges().get();
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

    public void startAssistantResponseStream() {
        messageParser.reset();
        messageQueue.clear();
        taskState.setAssistantMessageContent(new ArrayList<>());
        taskState.setCurrentStreamingContentIndex(0);
        taskState.setUserMessageContentReady(false);
    }

    public void updateAssistantMessageContent(String chunk) {
        if (taskState.isAbort()) {
            return;
        }
        AssistantMessageUpdate update = new AssistantMessageUpdate(chunk);
        messageQueue.offer(update);
        checkAndPresentAssistantMessage(false);
    }

    public void checkAndPresentAssistantMessage(boolean wait) {
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
                if (taskState.isDidRejectTool() || taskState.isAbort()) {
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

    public void presentAssistantMessage(AssistantMessageUpdate update) {
        if (taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }
        if (update != null) {
            presentAssistantMessageContent(update);
            checkAndPresentAssistantMessage(false);
        } else {
            checkAndPresentAssistantMessage(false);
        }
    }

    public void presentAssistantMessageContent(AssistantMessageUpdate update) {
        if (taskState.isAbort()) {
            throw new IllegalStateException("Cline instance aborted");
        }
        try {
            List<AssistantMessageContent> newContentBlocks = messageParser.feed(update.chunk());
            if (newContentBlocks.isEmpty()) {
                return;
            }
            List<AssistantMessageContent> oldContentBlocks = taskState.getAssistantMessageContent();

            if (newContentBlocks.size() > oldContentBlocks.size()) {
                taskState.setUserMessageContentReady(false);
            }

            taskState.setAssistantMessageContent(new ArrayList<>(newContentBlocks));
            presentAvailableAssistantBlocks();
        } catch (Exception e) {
            log.error("Error in presentAssistantMessageContent: {}", e.getMessage(), e);
        }
    }

    private void presentAvailableAssistantBlocks() {
        while (true) {
            int currentIndex = taskState.getCurrentStreamingContentIndex();
            List<AssistantMessageContent> currentAssistantContent =
                    taskState.getAssistantMessageContent();

            if (currentIndex >= currentAssistantContent.size()) {
                if (taskState.isDidCompleteReadingStream()) {
                    taskState.setUserMessageContentReady(true);
                }
                return;
            }

            AssistantMessageContent block = currentAssistantContent.get(currentIndex);

            if (block instanceof TextContent textContent) {
                processTextContent(textContent);
            } else if (block instanceof ToolUse toolUse) {
                if (!taskState.isDidAlreadyUseTool()) {
                    String toolUseId = UUID.randomUUID().toString();
                    toolUse.setId(toolUseId);
                    ToolState toolState = toolExecutor.getOrCreateToolState(toolUse.getName());
                    taskState.getToolStates().put(toolUseId, toolState);
                    ToolContext cfg = buildToolContext(toolState);
                    ToolExecuteResult execResult = toolExecutor.executeTool(toolUse, cfg);
                    if (execResult
                            instanceof ToolExecuteResult.PendingAsk(PendingAskToken pendingToken)) {
                        taskState
                                .getPendingAskTokens()
                                .put(pendingToken.getPendingId(), pendingToken);
                    } else if (execResult
                            instanceof ToolExecuteResult.Immediate(List<UserContentBlock> blocks)) {
                        pushToolResult(
                                blocks,
                                toolExecutor.getHandler(toolUse.getName()).getDescription(toolUse));
                    }
                }
            }

            if (block.isPartial()) {
                return;
            }

            if (currentIndex == currentAssistantContent.size() - 1) {
                taskState.setUserMessageContentReady(true);
            }

            taskState.setCurrentStreamingContentIndex(currentIndex + 1);
        }
    }

    public void processTextContent(TextContent textContent) {
        // Skip text rendering if tool was rejected, or if a tool was already used and parallel
        // calling is disabled
        if (taskState.isDidRejectTool() || taskState.isDidAlreadyUseTool()) {
            return;
        }
        String content = textContent.getContent();
        if (content != null) {
            // Remove all instances of <thinking> tags
            content = content.replaceAll("<thinking>\\s?", "");
            content = content.replaceAll("\\s?</thinking>", "");
            // Remove all instances of <think> tags (alternative to <thinking>, some models use
            // this)
            content = content.replaceAll("<think>\\s?", "");
            content = content.replaceAll("\\s?</think>", "");
            // Remove <function_calls> tags
            content = content.replaceAll("<function_calls>\\s?", "");
            content = content.replaceAll("\\s?</function_calls>", "");
        }
        String incrementalContent = textContent.getIncrementalContent();
        try {
            sayAskHandler.say(
                    ClineSay.TEXT,
                    content,
                    incrementalContent,
                    null,
                    null,
                    textContent.isPartial());
        } catch (Exception e) {
            log.error("Error saying text: {}", e.getMessage());
        }
    }

    /**
     * 工具 ask 响应后，续延完成，继续处理剩余内容块。 由 ApiCallingToolAskRespondedTransition 在续延完成后调用。 注意：pushToolResult
     * 已由 ToolExecutor 中的续延包装器处理，此处不需要再 push。
     *
     * @return 如果所有块处理完毕返回 true；如果又遇到 PendingAsk 返回 false
     */
    public boolean continueAfterToolAskResolved(String pendingId, AskResult askResult) {
        PendingAskToken askToken = taskState.getPendingAskTokens().remove(pendingId);
        if (askToken instanceof ToolUsePendingAskToken toolUsePendingAskToken) {
            ToolState toolState =
                    taskState.getToolStates().get(toolUsePendingAskToken.getToolUse().getId());
            ToolContext context = buildToolContext(toolState);
            ToolExecuteResult resumeResult = toolExecutor.resume(askToken, askResult, context);
            if (resumeResult instanceof ToolExecuteResult.PendingAsk(PendingAskToken token)) {
                taskState.getPendingAskTokens().put(token.getPendingId(), token);
            } else if (resumeResult
                    instanceof ToolExecuteResult.Immediate(List<UserContentBlock> blocks)) {
                pushToolResult(blocks, toolUsePendingAskToken.getToolDescription());
            }

            ConcurrentHashMap<String, PendingAskToken> allPendingAsks =
                    taskState.getPendingAskTokens();
            return allPendingAsks.isEmpty();
        } else {
            throw new IllegalArgumentException(
                    "Invalid token type: " + askToken.getClass().getName());
        }
    }

    private void pushToolResult(List<UserContentBlock> toolResult, String toolDescription) {

        ToolResultUtils.pushToolResult(
                toolResult,
                taskState.getNextUserMessageContent(),
                toolDescription,
                () -> {
                    taskState.setDidAlreadyUseTool(true);
                });
    }

    public AssistantMessage buildApiAssistantMessage(
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

    public void updateApiReqMessage(
            Integer inputTokens,
            Integer outputTokens,
            Integer cacheWriteTokens,
            Integer cacheReadTokens,
            Double totalCost,
            ClineApiReqCancelReason cancelReason,
            String streamingFailedMessage) {
        List<ClineMessage> messages = messageStateHandler.getClineMessages();
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
            messageStateHandler.updateClineMessage(tuple.f0, tuple.f1);
        }
    }

    public void abortStream(
            String assistantMessage,
            ClineApiReqInfo apiReqInfo,
            ClineApiReqCancelReason cancelReason,
            String streamingFailedMessage) {
        if (diffViewProvider != null && diffViewProvider.isEditing()) {
            try {
                diffViewProvider.revertChanges();
            } catch (Exception e) {
                log.error("Failed to revert changes in diff view provider: {}", e.getMessage(), e);
            }
        }

        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();
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
        messageStateHandler.addToApiConversationHistory(interruptedMessage);

        updateApiReqMessage(
                apiReqInfo.getTokensIn(),
                apiReqInfo.getTokensOut(),
                apiReqInfo.getCacheWrites(),
                apiReqInfo.getCacheReads(),
                apiReqInfo.getCost(),
                cancelReason,
                streamingFailedMessage);

        ProviderInfo providerInfo = getCurrentProviderInfo.get();
        telemetryService.captureConversationTurnEvent(
                ulid, providerInfo.getProviderId(), providerInfo.getModel(), "assistant");

        taskState.setDidFinishAbortingStream(true);
    }
}
