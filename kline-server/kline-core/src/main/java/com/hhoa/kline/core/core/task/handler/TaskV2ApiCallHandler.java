package com.hhoa.kline.core.core.task.handler;

import com.hhoa.ai.kline.commons.utils.ExceptionUtils;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.RedactedThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
import com.hhoa.kline.core.core.assistant.parser.StreamingAssistantMessageParser;
import com.hhoa.kline.core.core.context.management.ContextErrorHandling;
import com.hhoa.kline.core.core.context.management.ContextManager;
import com.hhoa.kline.core.core.context.management.ContextWindowUtils;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineApiReqCancelReason;
import com.hhoa.kline.core.core.shared.ClineApiReqInfo;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.ClineRequestResult;
import com.hhoa.kline.core.core.task.ContextFactory;
import com.hhoa.kline.core.core.task.ExistState;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.PresentationPriority;
import com.hhoa.kline.core.core.task.ProviderInfo;
import com.hhoa.kline.core.core.task.StreamChunkCoordinator;
import com.hhoa.kline.core.core.task.StreamResponseHandler;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.deps.TaskDispatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public final class TaskV2ApiCallHandler {

    private final ResponseFormatter responseFormatter;
    private final StateManager stateManager;
    private final ContextManager contextManager;
    private final TelemetryService telemetryService;
    private final DiffViewProvider diffViewProvider;
    private final Supplier<StreamingAssistantMessageParser> messageParserFactory;
    private final SystemPromptService systemPromptService;
    private final ContextFactory contextFactory;
    private final ApiHandler apiHandler;
    private final TaskV2ContextWindowHandler contextWindowHandler;
    private final TaskState taskState;
    private final MessageStateHandler messageStateHandler;
    private final TaskDispatch taskDispatch;
    private final TaskV2SayAskHandler sayAskHandler;
    private final String taskId;
    private final String ulid;
    private final Supplier<ProviderInfo> getCurrentProviderInfo;
    private final TaskV2MessagePresenterHandler messagePresenterHandler;
    private final TaskV2ContextPrepareHandler contextPrepareHandler;
    private final Runnable postStateToWebview;

    /** Handler for native tool use streaming (accumulates TOOL_USE chunks). */
    private StreamResponseHandler streamResponseHandler;

    /** Optional presentation scheduler callback for coalescing UI updates during streaming. */
    private java.util.function.Consumer<PresentationPriority> schedulePresentation;

    public TaskV2ApiCallHandler(
            ResponseFormatter responseFormatter,
            StateManager stateManager,
            ContextManager contextManager,
            TelemetryService telemetryService,
            DiffViewProvider diffViewProvider,
            Supplier<StreamingAssistantMessageParser> messageParserFactory,
            SystemPromptService systemPromptService,
            ContextFactory contextFactory,
            ApiHandler apiHandler,
            TaskV2ContextWindowHandler contextWindowHandler,
            TaskState taskState,
            MessageStateHandler messageStateHandler,
            TaskDispatch taskDispatch,
            TaskV2SayAskHandler sayAskHandler,
            String taskId,
            String ulid,
            Supplier<ProviderInfo> getCurrentProviderInfo,
            TaskV2MessagePresenterHandler messagePresenterHandler,
            TaskV2ContextPrepareHandler contextPrepareHandler,
            Runnable postStateToWebview) {
        this.responseFormatter = responseFormatter;
        this.stateManager = stateManager;
        this.contextManager = contextManager;
        this.telemetryService = telemetryService;
        this.diffViewProvider = diffViewProvider;
        this.messageParserFactory = messageParserFactory;
        this.systemPromptService = systemPromptService;
        this.contextFactory = contextFactory;
        this.apiHandler = apiHandler;
        this.contextWindowHandler = contextWindowHandler;
        this.taskState = taskState;
        this.messageStateHandler = messageStateHandler;
        this.taskDispatch = taskDispatch;
        this.sayAskHandler = sayAskHandler;
        this.taskId = taskId;
        this.ulid = ulid;
        this.getCurrentProviderInfo = getCurrentProviderInfo;
        this.messagePresenterHandler = messagePresenterHandler;
        this.contextPrepareHandler = contextPrepareHandler;
        this.postStateToWebview = postStateToWebview;
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

    public ApiRequestResult doCallApi() {
        if (taskState.isAbort()) {
            return new ApiRequestResult(new ExistState.Abort());
        }

        messagePresenterHandler.startAssistantResponseStream();

        int previousApiReqIndex = taskState.getApiTurnState().getCurrentPreviousApiReqIndex();

        taskState.resetForNewApiRequest();

        if (diffViewProvider != null) {
            try {
                diffViewProvider.reset();
            } catch (Exception e) {
                log.error("Failed to reset diff view provider: " + e.getMessage());
            }
        }

        taskState.getStreamState().setStreaming(true);

        List<MessageParam> apiConversationHistory = messageStateHandler.getApiConversationHistory();
        List<ClineMessage> clineMessages = messageStateHandler.getClineMessages();

        ProviderInfo providerInfo = getCurrentProviderInfo.get();
        ContextWindowUtils.ContextWindowInfo contextWindowInfo =
                ContextWindowUtils.getContextWindowInfo(200000, providerInfo.getModel());
        int contextWindow = contextWindowInfo.contextWindow();
        int maxAllowedSize = contextWindowInfo.maxAllowedSize();

        int[] conversationHistoryDeletedRange = taskState.getConversationHistoryDeletedRange();

        boolean useAutoCondense = stateManager.getSettings().isUseAutoCondense();
        ContextManager.ContextMessagesResult contextResult =
                contextManager.getNewContextMessagesAndMetadata(
                        apiConversationHistory,
                        clineMessages,
                        contextWindow,
                        maxAllowedSize,
                        conversationHistoryDeletedRange,
                        previousApiReqIndex,
                        useAutoCondense);

        if (contextResult.isUpdatedConversationHistoryDeletedRange()) {
            taskState.setConversationHistoryDeletedRange(
                    contextResult.getConversationHistoryDeletedRange());
            messageStateHandler.saveClineMessagesAndUpdateHistory();
        }

        List<MessageParam> truncatedConversationHistory =
                contextResult.getTruncatedConversationHistory();

        ApiRequestResult result = attemptApiRequest(truncatedConversationHistory, null);
        taskState.getStreamState().setDidCompleteReadingStream(true);

        return result;
    }

    public ClineRequestResult processAssistantResponse(
            ApiRequestResult apiResult, ProviderInfo providerInfo) {
        if (taskState.isAbort()) {
            return new ClineRequestResult.Abort();
        }
        messagePresenterHandler.checkAndPresentAssistantMessage(true);

        messagePresenterHandler.updateApiReqMessage(
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
                    ulid, providerInfo.getProviderId(), providerInfo.getModel(), "assistant");

            List<UserContentBlock> antThinkingContent = apiResult.getAntThinkingContent();
            List<Object> reasoningDetails = apiResult.getReasoningDetails();
            messageStateHandler.addToApiConversationHistory(
                    messagePresenterHandler.buildApiAssistantMessage(
                            assistantMessage, antThinkingContent, reasoningDetails));

            List<AssistantMessageContent> contentBlocks =
                    parseAssistantMessageSnapshot(assistantMessage);

            for (AssistantMessageContent item : contentBlocks) {
                if (item.isPartial()) {
                    item.setPartial(false);
                }
            }

            taskState.getPresentationState().setAssistantMessageContent(contentBlocks);
            taskState.getPresentationState().setCurrentStreamingContentIndex(0);

            taskState.getStreamState().setStreaming(false);

            List<ClineMessage> messagesForUpdate = messageStateHandler.getClineMessages();

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
                ClineApiReqCancelReason cancelReason = apiResult.getApiReqInfo().getCancelReason();
                messagePresenterHandler.updateApiReqMessage(
                        apiResult.getApiReqInfo().getTokensIn(),
                        apiResult.getApiReqInfo().getTokensOut(),
                        apiResult.getApiReqInfo().getCacheWrites(),
                        apiResult.getApiReqInfo().getCacheReads(),
                        apiResult.getApiReqInfo().getCost(),
                        cancelReason,
                        apiResult.getApiReqInfo().getStreamingFailedMessage());
                try {
                    messageStateHandler.saveClineMessagesAndUpdateHistory();
                } catch (Exception e) {
                    log.error(
                            "Failed to save messages after updating token usage: {}",
                            e.getMessage());
                }
            }

            boolean didToolUse = contentBlocks.stream().anyMatch(block -> block instanceof ToolUse);

            taskState.getApiTurnState().setAutoRetryAttempts(0);

            return didToolUse
                    ? new ClineRequestResult.DidToolUse()
                    : new ClineRequestResult.DidNotToolUse();
        }
    }

    private ClineRequestResult processEmptyAssistantMessage() {
        ProviderInfo providerInfo = getCurrentProviderInfo.get();

        log.error(
                "[EmptyAssistantMessage] ulid={}, providerId={}, modelId={}, requestId={}",
                ulid,
                providerInfo.getProviderId(),
                providerInfo.getModel(),
                apiHandler.getLastRequestId());

        telemetryService.captureProviderApiError(
                ulid,
                providerInfo.getModel(),
                "empty_assistant_message",
                providerInfo.getProviderId(),
                null,
                apiHandler.getLastRequestId());

        String baseErrorMessage =
                "Invalid API Response: The provider returned an empty or unparsable response. "
                        + "This is a provider-side issue where the model failed to generate valid output or returned tool calls that Cline cannot process. "
                        + "Retrying the request may help resolve this issue.";
        String errorText =
                apiHandler.getLastRequestId() != null && !apiHandler.getLastRequestId().isEmpty()
                        ? baseErrorMessage + " (Request ID: " + apiHandler.getLastRequestId() + ")"
                        : baseErrorMessage;

        sayAskHandler.say(ClineSay.ERROR, errorText, null, null, null);

        messageStateHandler.addToApiConversationHistory(
                messagePresenterHandler.buildApiAssistantMessage(
                        "Failure: I did not provide a response.", null, null));

        return new ClineRequestResult.Failed(errorText, null);
    }

    public boolean tryRetryAsk(String askMessage) {
        if (taskState.getApiTurnState().getAutoRetryAttempts() < 3) {
            taskState
                    .getApiTurnState()
                    .setAutoRetryAttempts(taskState.getApiTurnState().getAutoRetryAttempts() + 1);

            int delayMs = 2000 * (1 << (taskState.getApiTurnState().getAutoRetryAttempts() - 1));

            Map<String, Object> retryInfoMap = new HashMap<String, Object>();
            retryInfoMap.put("attempt", taskState.getApiTurnState().getAutoRetryAttempts());
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", delayMs / 1000);

            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        } else {
            Map<String, Object> retryInfoMap = new HashMap<String, Object>();
            retryInfoMap.put("attempt", 3);
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", 0);
            retryInfoMap.put("failed", true);
            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);

            return true;
        }
    }

    private ApiRequestResult attemptApiRequest(
            List<MessageParam> conversationHistory, StringBuilder unCompleteAssistantMessage) {
        SystemPromptContext context =
                contextPrepareHandler.buildSystemPromptContext(unCompleteAssistantMessage != null);
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

        // Reset stream response handler for this request
        if (streamResponseHandler != null) {
            streamResponseHandler.reset();
        }

        int lastApiReqIndex = -1;
        List<ClineMessage> messagesForIndex = messageStateHandler.getClineMessages();
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

            StreamChunkCoordinator coordinator =
                    new StreamChunkCoordinator(
                            chunkFlux,
                            usageChunk -> getUsage(usageChunk, didReceiveUsageChunk, apiReqInfo));
            try {
                ApiChunk chunk;
                while ((chunk = coordinator.nextChunk()) != null) {
                    if (taskState.isAbort()
                            || taskState.getToolExecutionState().isDidRejectTool()) {
                        break;
                    }
                    if (taskState.getToolExecutionState().isDidAlreadyUseTool()
                            && didReceiveUsageChunk[0]) {
                        break;
                    }
                    processContentChunk(
                            chunk,
                            streamInterrupted,
                            assistantMessageBuilder,
                            apiReqInfo,
                            reasoningMessageBuilder,
                            reasoningDetailsList,
                            antThinkingContentList);
                }
            } finally {
                coordinator.stop();
            }
        } catch (Exception streamError) {
            return handleApiRequestError(streamError, lastApiReqIndex);
        } finally {
            taskState.getStreamState().setStreaming(false);
        }

        if (!didReceiveUsageChunk[0]) {
            ApiHandler.ApiStreamUsage apiStreamUsage = apiHandler.getApiStreamUsage();
            if (apiStreamUsage != null) {
                apiReqInfo.setTokensIn(apiStreamUsage.inputTokens());
                apiReqInfo.setTokensOut(apiStreamUsage.outputTokens());
                apiReqInfo.setCacheWrites(apiStreamUsage.cacheWriteTokens());
                apiReqInfo.setCacheReads(apiStreamUsage.cacheReadTokens());
                apiReqInfo.setCost(apiStreamUsage.totalCost());
            }
        }

        if (!reasoningMessageBuilder.isEmpty()) {
            sayAskHandler.say(
                    ClineSay.REASONING, reasoningMessageBuilder.toString(), null, null, false);
            reasoningMessageBuilder.setLength(0);
        }

        String finalAssistantMessage = assistantMessageBuilder.toString();
        if (streamInterrupted[0]) {
            if (ClineApiReqCancelReason.USER_CANCELLED == apiReqInfo.getCancelReason()) {
                finalAssistantMessage += "\n\n[Response interrupted by user]";
            }
        }

        if (taskState.isAbort()) {
            return new ApiRequestResult(new ExistState.Abort());
        }

        // Process native tool calls if any were accumulated during streaming
        if (streamResponseHandler != null
                && !streamResponseHandler.getToolUseHandler().getAllFinalizedToolUses().isEmpty()) {
            processNativeToolCalls(
                    finalAssistantMessage,
                    streamResponseHandler
                            .getToolUseHandler()
                            .getAllFinalizedToolUses(
                                    streamResponseHandler
                                            .getReasoningHandler()
                                            .getCurrentReasoning()));
        }

        ApiRequestResult result = new ApiRequestResult(new ExistState.Success());
        result.setAssistantMessage(finalAssistantMessage);
        result.setReasoningDetails(new ArrayList<>(reasoningDetailsList));
        result.setAntThinkingContent(new ArrayList<>(antThinkingContentList));

        result.setApiReqInfo(apiReqInfo);

        checkAndProcessTruncatedContent(
                conversationHistory, finalAssistantMessage, assistantMessageBuilder, result);
        return result;
    }

    /**
     * Processes a non-USAGE chunk from the stream coordinator. USAGE chunks are handled separately
     * by the coordinator's onUsageChunk callback.
     */
    private void processContentChunk(
            ApiChunk chunk,
            boolean[] streamInterrupted,
            StringBuilder assistantMessageBuilder,
            ClineApiReqInfo apiReqInfo,
            StringBuilder reasoningMessageBuilder,
            List<Object> reasoningDetailsList,
            List<UserContentBlock> antThinkingContentList) {
        if (streamInterrupted[0]) {
            return;
        }

        processApiChunkContent(
                chunk,
                assistantMessageBuilder,
                reasoningMessageBuilder,
                reasoningDetailsList,
                antThinkingContentList);

        if (taskState.isAbort()) {
            streamInterrupted[0] = true;
            apiReqInfo.setCancelReason(ClineApiReqCancelReason.USER_CANCELLED);
            messagePresenterHandler.abortStream(
                    assistantMessageBuilder.toString(),
                    apiReqInfo,
                    ClineApiReqCancelReason.USER_CANCELLED,
                    null);
            return;
        }

        if (taskState.getToolExecutionState().isDidRejectTool()) {
            if (!streamInterrupted[0]
                    || !ClineApiReqCancelReason.USER_FEEDBACK.equals(
                            apiReqInfo.getCancelReason())) {
                streamInterrupted[0] = true;
                apiReqInfo.setCancelReason(ClineApiReqCancelReason.USER_FEEDBACK);
                assistantMessageBuilder.append("\n\n[Response interrupted by user feedback]");
            }
            return;
        }
        if (taskState.getToolExecutionState().isDidAlreadyUseTool()) {
            if (!streamInterrupted[0]) {
                streamInterrupted[0] = true;
                assistantMessageBuilder.append(
                        "\n\n[Response interrupted by a tool use result. Only one tool may be used at a time and should be placed at the end of the message.]");
            }
        }
    }

    private void checkAndProcessTruncatedContent(
            List<MessageParam> conversationHistory,
            String finalAssistantMessage,
            StringBuilder assistantMessageBuilder,
            ApiRequestResult result) {
        List<AssistantMessageContent> contentBlocks =
                parseAssistantMessageSnapshot(finalAssistantMessage);
        boolean hasTruncatedToolUse =
                contentBlocks.stream()
                        .anyMatch(block -> block.isPartial() && block instanceof ToolUse);

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

    private List<AssistantMessageContent> parseAssistantMessageSnapshot(String message) {
        if (message == null || message.isEmpty()) {
            return new ArrayList<>();
        }

        StreamingAssistantMessageParser messageParser = messageParserFactory.get();
        messageParser.feed(message);
        return messageParser.getCurrentBlocks();
    }

    private ApiRequestResult handleApiRequestError(Exception streamError, int lastApiReqIndex) {
        if (taskState.isAbandoned()) {
            return new ApiRequestResult(new ExistState.Abort());
        }

        boolean isContextWindowExceededError =
                ContextErrorHandling.checkContextWindowExceededError(streamError);

        if (isContextWindowExceededError
                && !taskState.getApiTurnState().isDidAutomaticallyRetryFailedApiRequest()) {
            try {
                contextWindowHandler.handleContextWindowExceededError();
                return new ApiRequestResult(new ExistState.ContextWindowExceeded());
            } catch (Exception retryError) {
                log.error(
                        "Failed to handle context window exceeded error: {}",
                        retryError.getMessage(),
                        retryError);
            }
        }

        ProviderInfo providerInfo = getCurrentProviderInfo.get();

        String errorMessage = ExceptionUtils.stringifyException(streamError);
        if (errorMessage == null) {
            Throwable cause = streamError.getCause() != null ? streamError.getCause() : streamError;
            errorMessage = cause.getMessage();
        }
        if (errorMessage == null) {
            errorMessage = "Streaming failed";
        }

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
                this.taskState.getApiTurnState().setDidAutomaticallyRetryFailedApiRequest(false);
            }
        }

        log.error(
                "[StreamingError] ulid={}, providerId={}, modelId={}, requestId={}, error={}",
                ulid,
                providerInfo.getProviderId(),
                providerInfo.getModel(),
                apiHandler.getLastRequestId(),
                errorMessage,
                streamError);

        telemetryService.captureProviderApiError(
                ulid,
                providerInfo.getModel(),
                errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage,
                providerInfo.getProviderId(),
                null,
                apiHandler.getLastRequestId());

        if (lastApiReqIndex >= 0) {
            try {
                messagePresenterHandler.updateApiReqMessage(
                        0, 0, 0, 0, null, ClineApiReqCancelReason.STREAMING_FAILED, errorMessage);
            } catch (Exception updateError) {
                log.error(
                        "Failed to update API request message: {}",
                        updateError.getMessage(),
                        updateError);
            }
        }

        return new ApiRequestResult(new ExistState.Failed(finalErrorMessage, streamError));
    }

    private Flux<ApiChunk> getApiChunkFlux(
            String systemPrompt, List<MessageParam> conversationHistory) {
        Flux<ApiChunk> chunkFlux =
                apiHandler.createMessageStream(
                        systemPrompt != null ? systemPrompt : "", conversationHistory);

        taskState.getStreamState().setWaitingForFirstChunk(true);

        final AtomicBoolean isFirstChunk = new AtomicBoolean(true);
        return chunkFlux
                .doOnSubscribe(
                        subscription -> taskState.getStreamState().setWaitingForFirstChunk(true))
                .doOnNext(
                        chunk -> {
                            if (isFirstChunk.compareAndSet(true, false)) {
                                taskState.getStreamState().setWaitingForFirstChunk(false);
                            }
                        })
                .doOnComplete(() -> taskState.getStreamState().setWaitingForFirstChunk(false))
                .doOnCancel(() -> taskState.getStreamState().setWaitingForFirstChunk(false));
    }

    /**
     * Processes a non-USAGE content chunk. USAGE chunks are handled by the StreamChunkCoordinator's
     * onUsageChunk callback.
     */
    private void processApiChunkContent(
            ApiChunk chunk,
            StringBuilder assistantMessageBuilder,
            StringBuilder reasoningMessageBuilder,
            List<Object> reasoningDetailsList,
            List<UserContentBlock> antThinkingContentList) {
        ApiChunk.ChunkType type = chunk.type();
        if (type == null) {
            return;
        }

        switch (type) {
            case USAGE:
                // Handled by StreamChunkCoordinator — should not reach here
                break;
            case REASONING:
                String reasoning = chunk.reasoning();
                if (reasoning != null && !reasoning.isEmpty()) {
                    reasoningMessageBuilder.append(reasoning);
                    if (!taskState.isAbort()) {
                        sayAskHandler.say(
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
                        @SuppressWarnings("unchecked")
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
            case TOOL_USE:
                // Native tool use streaming — accumulate via StreamResponseHandler
                if (streamResponseHandler != null) {
                    streamResponseHandler.getToolUseHandler().processToolUseDelta(chunk);

                    // Flush presentation immediately on tool transitions
                    if (schedulePresentation != null) {
                        schedulePresentation.accept(PresentationPriority.IMMEDIATE);
                    }
                }
                break;
            case TEXT:
                if (!reasoningMessageBuilder.isEmpty() && assistantMessageBuilder.isEmpty()) {
                    sayAskHandler.say(
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
                    messagePresenterHandler.updateAssistantMessageContent(text);

                    // Schedule presentation flush — IMMEDIATE for first visible token, NORMAL
                    // for subsequent text to coalesce rapid updates
                    if (schedulePresentation != null) {
                        boolean isFirstToken = oldAssistantMessage.isEmpty();
                        schedulePresentation.accept(
                                isFirstToken
                                        ? PresentationPriority.IMMEDIATE
                                        : PresentationPriority.NORMAL);
                    }
                }
                break;
            default:
                break;
        }
    }

    /**
     * Processes native tool calls after streaming completes. Builds assistantMessageContent from
     * text blocks + tool blocks, and sets the streaming index to the first tool block.
     */
    private void processNativeToolCalls(String assistantText, List<ToolUse> toolBlocks) {
        List<AssistantMessageContent> content = new ArrayList<>();
        taskState.markNativeToolCallsPresent();

        // Add text block if there's any assistant text
        if (assistantText != null && !assistantText.trim().isEmpty()) {
            content.add(new TextContent(assistantText, false));
        }

        // Add tool use blocks
        content.addAll(toolBlocks);

        taskState.getPresentationState().setAssistantMessageContent(content);

        // Set streaming index to first tool block (skip text blocks)
        int firstToolIndex = 0;
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) instanceof ToolUse) {
                firstToolIndex = i;
                break;
            }
        }
        taskState.getPresentationState().setCurrentStreamingContentIndex(firstToolIndex);
        taskState.getPresentationState().setUserMessageContentReady(false);
    }

    /** Sets the StreamResponseHandler for native tool call support. */
    public void setStreamResponseHandler(StreamResponseHandler handler) {
        this.streamResponseHandler = handler;
    }

    /** Sets the presentation scheduler callback for coalescing UI updates during streaming. */
    public void setSchedulePresentation(
            java.util.function.Consumer<PresentationPriority> schedulePresentation) {
        this.schedulePresentation = schedulePresentation;
    }
}
