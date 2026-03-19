package com.hhoa.kline.core.core.task.handler;

import com.hhoa.ai.kline.commons.utils.ExceptionUtils;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.AssistantMessage;
import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.AssistantMessageParser;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.RedactedThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ThinkingContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.assistant.UserMessage;
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
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageType;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.task.ApiChunk;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.core.core.task.ApiRequestResult;
import com.hhoa.kline.core.core.task.AskPending;
import com.hhoa.kline.core.core.task.ClineMessage;
import com.hhoa.kline.core.core.task.ClineRequestResult;
import com.hhoa.kline.core.core.task.ContextFactory;
import com.hhoa.kline.core.core.task.MessageStateHandler;
import com.hhoa.kline.core.core.task.ProviderInfo;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.deps.TaskDispatch;
import com.hhoa.kline.core.core.task.event.ApiCallingRetryEvent;
import com.hhoa.kline.core.core.task.event.ApiFailedEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public final class TaskV2ApiCallHandler {

    private final ResponseFormatter responseFormatter;
    private final StateManager stateManager;
    private final ContextManager contextManager;
    private final TelemetryService telemetryService;
    private final DiffViewProvider diffViewProvider;
    private final AssistantMessageParser messageParser;
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

    public TaskV2ApiCallHandler(
            ResponseFormatter responseFormatter,
            StateManager stateManager,
            ContextManager contextManager,
            TelemetryService telemetryService,
            DiffViewProvider diffViewProvider,
            AssistantMessageParser messageParser,
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
        this.messageParser = messageParser;
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

    public void doCallApi() {
        if (taskState.isAbort()) {
            return;
        }

        int previousApiReqIndex = taskState.getCurrentPreviousApiReqIndex();

        try {
            doCallApiInternal(previousApiReqIndex);
        } catch (Exception e) {
            if (autoRetryDispatched) {
                return;
            }

            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = cause.getMessage();

            log.error("Error in doCallApi: {}", errorMessage, e);

            boolean isContextWindowExceeded =
                    errorMessage != null
                            && (errorMessage.contains("context window")
                                    || errorMessage.contains("token limit")
                                    || errorMessage.contains("maximum context length"));

            if (isContextWindowExceeded && !taskState.isDidAutomaticallyRetryFailedApiRequest()) {
                try {
                    contextWindowHandler.handleContextWindowExceededError();
                    taskState.setDidAutomaticallyRetryFailedApiRequest(true);
                    taskDispatch.dispatch(new ApiFailedEvent(taskId, e));
                    return;
                } catch (Exception retryError) {
                    log.error(
                            "Error handling context window exceeded: {}",
                            retryError.getMessage(),
                            retryError);
                }
            }

            sayAskHandler.say(
                    ClineSay.ERROR,
                    "Failed to process request: "
                            + (errorMessage != null ? errorMessage : "Unknown error"),
                    null,
                    null,
                    null);

            sayAskHandler.ask(
                    ClineAsk.API_REQ_FAILED, "Error processing request. Would you like to retry?");

            taskDispatch.dispatch(new ApiFailedEvent(taskId, cause));
        }
    }

    public void doCallApiInternal(int previousApiReqIndex) {
        taskState.setCurrentStreamingContentIndex(0);
        taskState.setAssistantMessageContent(new ArrayList<>());
        taskState.setDidCompleteReadingStream(false);
        taskState.setNextUserMessageContent(new ArrayList<>());
        taskState.setUserMessageContentReady(false);
        taskState.setDidRejectTool(false);
        taskState.setDidAlreadyUseTool(false);
        taskState.setDidAutomaticallyRetryFailedApiRequest(false);

        if (diffViewProvider != null) {
            try {
                diffViewProvider.reset();
            } catch (Exception e) {
                log.error("Failed to reset diff view provider: " + e.getMessage());
            }
        }

        taskState.setStreaming(true);

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

        try {

            CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            return attemptApiRequest(truncatedConversationHistory, null);
                        } catch (Exception ex) {
                            taskDispatch.dispatch(new ApiFailedEvent(taskId, ex));
                            return null;
                        }
                    });
        } catch (Exception e) {
            taskDispatch.dispatch(new ApiFailedEvent(taskId, e));
        }
    }

    public ClineRequestResult processAssistantResponse(
            ApiRequestResult apiResult, ProviderInfo providerInfo) {
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
                    messageParser.parseAssistantMessage(assistantMessage);

            for (AssistantMessageContent item : contentBlocks) {
                if (item.isPartial()) {
                    item.setPartial(false);
                }
            }

            taskState.setAssistantMessageContent(contentBlocks);
            taskState.setCurrentStreamingContentIndex(0);

            taskState.setStreaming(false);

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

            taskState.setAutoRetryAttempts(0);

            return didToolUse ? ClineRequestResult.DID_TOOL_USE : ClineRequestResult.FAILED;
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

        return ClineRequestResult.FAILED;
    }

    private @Nullable AskPending retry(String askMessage) {
        if (taskState.getAutoRetryAttempts() < 3) {
            taskState.setAutoRetryAttempts(taskState.getAutoRetryAttempts() + 1);

            int delayMs = 2000 * (1 << (taskState.getAutoRetryAttempts() - 1));

            Map<String, Object> retryInfoMap = new HashMap<String, Object>();
            retryInfoMap.put("attempt", taskState.getAutoRetryAttempts());
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", delayMs / 1000);

            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        } else {
            Map<String, Object> retryInfoMap = new HashMap<String, Object>();
            retryInfoMap.put("attempt", 3);
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", 0);
            retryInfoMap.put("failed", true);
            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);

            return sayAskHandler.ask(ClineAsk.API_REQ_FAILED, askMessage);
        }
    }

    private ApiRequestResult attemptApiRequest(
            List<MessageParam> conversationHistory, StringBuilder unCompleteAssistantMessage)
            throws Exception {
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

            chunkFlux
                    .takeWhile(
                            chunk -> {
                                if (taskState.isAbort() || taskState.isDidRejectTool()) {
                                    return false;
                                }
                                if (taskState.isDidAlreadyUseTool()) {
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
                                                                                            () ->
                                                                                                    extracted(
                                                                                                            chunk,
                                                                                                            streamInterrupted,
                                                                                                            assistantMessageBuilder,
                                                                                                            didReceiveUsageChunk,
                                                                                                            apiReqInfo,
                                                                                                            reasoningMessageBuilder,
                                                                                                            reasoningDetailsList,
                                                                                                            antThinkingContentList)))
                                                            .then(Mono.just(chunk))))
                    .contextWrite(contextFactory::modifyContext)
                    .blockLast();
        } catch (Exception streamError) {
            if (autoRetryDispatched) {
                throw new RuntimeException("Auto-retry scheduled", streamError);
            }
            handleApiRequestError(streamError, lastApiReqIndex);
        } finally {
            taskState.setStreaming(false);
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
            throw new RuntimeException("Cline instance aborted");
        }

        ApiRequestResult result = new ApiRequestResult();
        result.setAssistantMessage(finalAssistantMessage);
        result.setReasoningDetails(new ArrayList<>(reasoningDetailsList));
        result.setAntThinkingContent(new ArrayList<>(antThinkingContentList));

        result.setApiReqInfo(apiReqInfo);

        checkAndProcessTruncatedContent(
                conversationHistory, finalAssistantMessage, assistantMessageBuilder, result);

        taskState.setDidCompleteReadingStream(true);
        taskState.setApiRequestResult(result);
        return result;
    }

    private void extracted(
            ApiChunk chunk,
            boolean[] streamInterrupted,
            StringBuilder assistantMessageBuilder,
            boolean[] didReceiveUsageChunk,
            ClineApiReqInfo apiReqInfo,
            StringBuilder reasoningMessageBuilder,
            List<Object> reasoningDetailsList,
            List<UserContentBlock> antThinkingContentList) {
        if (!streamInterrupted[0]) {
            processApiChunk(
                    chunk,
                    streamInterrupted,
                    assistantMessageBuilder,
                    didReceiveUsageChunk,
                    apiReqInfo,
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

            if (taskState.isDidRejectTool()) {
                if (!streamInterrupted[0]
                        || !ClineApiReqCancelReason.USER_FEEDBACK.equals(
                                apiReqInfo.getCancelReason())) {
                    streamInterrupted[0] = true;
                    apiReqInfo.setCancelReason(ClineApiReqCancelReason.USER_FEEDBACK);
                    assistantMessageBuilder.append("\n\n[Response interrupted by user feedback]");
                }
                return;
            }
            if (taskState.isDidAlreadyUseTool()) {
                if (!streamInterrupted[0]) {
                    streamInterrupted[0] = true;
                    assistantMessageBuilder.append(
                            "\n\n[Response interrupted by a tool use result. Only one tool may be used at a time and should be placed at the end of the message.]");
                }
            }
        } else {
            if (chunk.type().equals(ApiChunk.ChunkType.USAGE)) {
                getUsage(chunk, didReceiveUsageChunk, apiReqInfo);
            }
        }
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

    private boolean autoRetryDispatched;

    private void handleApiRequestError(Exception streamError, int lastApiReqIndex)
            throws Exception {
        autoRetryDispatched = false;

        if (taskState.isAbandoned()) {
            throw streamError;
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

        if (taskState.getAutoRetryAttempts() < 3) {
            taskState.setAutoRetryAttempts(taskState.getAutoRetryAttempts() + 1);

            int delayMs = 2000 * (1 << (taskState.getAutoRetryAttempts() - 1));

            try {
                Map<String, Object> retryInfoMap = new HashMap<>();
                retryInfoMap.put("attempt", taskState.getAutoRetryAttempts());
                retryInfoMap.put("maxAttempts", 3);
                retryInfoMap.put("delaySeconds", delayMs / 1000);
                sayAskHandler.say(
                        ClineSay.ERROR_RETRY,
                        JsonUtils.toJsonString(retryInfoMap),
                        null,
                        null,
                        null);
            } catch (Exception sayError) {
                log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
            }

            autoRetryDispatched = true;
            taskDispatch.dispatch(new ApiCallingRetryEvent(taskId, delayMs, errorMessage));
            throw new RuntimeException("Auto-retry scheduled", streamError);
        }

        try {
            Map<String, Object> retryInfoMap = new HashMap<>();
            retryInfoMap.put("attempt", 3);
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", 0);
            retryInfoMap.put("failed", true);
            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);
        } catch (Exception sayError) {
            log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
        }

        throw new RuntimeException("Streaming failed: " + errorMessage, streamError);
    }

    private Flux<ApiChunk> getApiChunkFlux(
            String systemPrompt, List<MessageParam> conversationHistory) {
        Flux<ApiChunk> chunkFlux =
                apiHandler.createMessageStream(
                        systemPrompt != null ? systemPrompt : "", conversationHistory);

        taskState.setWaitingForFirstChunk(true);

        final AtomicBoolean isFirstChunk = new AtomicBoolean(true);
        return chunkFlux
                .doOnSubscribe(subscription -> taskState.setWaitingForFirstChunk(true))
                .doOnNext(
                        chunk -> {
                            if (isFirstChunk.compareAndSet(true, false)) {
                                taskState.setWaitingForFirstChunk(false);
                            }
                        })
                .onErrorResume(
                        throwable -> {
                            taskState.setWaitingForFirstChunk(false);
                            if (isFirstChunk.get()) {
                                return handleApiReqException(throwable, systemPrompt);
                            }
                            return Flux.empty();
                        })
                .doOnComplete(() -> taskState.setWaitingForFirstChunk(false))
                .doOnCancel(() -> taskState.setWaitingForFirstChunk(false));
    }

    private Flux<ApiChunk> handleApiReqException(Throwable e, String systemPrompt) {
        boolean isContextWindowExceededError =
                ContextErrorHandling.checkContextWindowExceededError(e);
        ProviderInfo providerInfo = getCurrentProviderInfo.get();

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
                ulid,
                providerInfo.getProviderId(),
                providerInfo.getModel(),
                apiHandler.getLastRequestId(),
                errorMessage,
                e);

        telemetryService.captureProviderApiError(
                ulid,
                providerInfo.getModel(),
                errorMessage.length() > 500 ? errorMessage.substring(0, 500) : errorMessage,
                providerInfo.getProviderId(),
                null,
                apiHandler.getLastRequestId());

        if (isContextWindowExceededError && !taskState.isDidAutomaticallyRetryFailedApiRequest()) {
            try {
                contextWindowHandler.handleContextWindowExceededError();
                taskState.setDidAutomaticallyRetryFailedApiRequest(true);

                List<MessageParam> truncatedMessages =
                        contextManager.getTruncatedMessages(
                                messageStateHandler.getApiConversationHistory(),
                                taskState.getConversationHistoryDeletedRange());

                return getApiChunkFlux(systemPrompt, truncatedMessages);
            } catch (Exception retryError) {
                log.error(
                        "Failed to handle context window exceeded error: {}",
                        retryError.getMessage(),
                        retryError);
            }
        }

        String finalErrorMessage = errorMessage;
        if (isContextWindowExceededError) {
            List<MessageParam> truncatedConversationHistory =
                    contextManager.getTruncatedMessages(
                            messageStateHandler.getApiConversationHistory(),
                            taskState.getConversationHistoryDeletedRange());

            if (truncatedConversationHistory.size() > 3) {
                finalErrorMessage =
                        "Context window exceeded. Click retry to truncate the conversation and try again.";
                taskState.setDidAutomaticallyRetryFailedApiRequest(false);
            }
        }

        String streamingFailedMessage = finalErrorMessage;

        messagePresenterHandler.updateApiReqMessage(
                null,
                null,
                null,
                null,
                null,
                ClineApiReqCancelReason.STREAMING_FAILED,
                streamingFailedMessage);

        if (taskState.getAutoRetryAttempts() < 3) {
            taskState.setAutoRetryAttempts(taskState.getAutoRetryAttempts() + 1);

            int delayMs = 2000 * (1 << (taskState.getAutoRetryAttempts() - 1));

            try {
                messageStateHandler.saveClineMessagesAndUpdateHistory();
            } catch (Exception saveError) {
                log.error("Failed to save messages: {}", saveError.getMessage(), saveError);
            }
            if (postStateToWebview != null) {
                postStateToWebview.run();
            }

            try {
                Map<String, Object> retryInfoMap = new HashMap<>();
                retryInfoMap.put("attempt", taskState.getAutoRetryAttempts());
                retryInfoMap.put("maxAttempts", 3);
                retryInfoMap.put("delaySeconds", delayMs / 1000);
                sayAskHandler.say(
                        ClineSay.ERROR_RETRY,
                        JsonUtils.toJsonString(retryInfoMap),
                        null,
                        null,
                        null);
            } catch (Exception sayError) {
                log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
            }

            autoRetryDispatched = true;
            taskDispatch.dispatch(
                    new ApiCallingRetryEvent(taskId, delayMs, streamingFailedMessage));
            return Flux.error(new RuntimeException("Auto-retry scheduled"));
        }

        try {
            Map<String, Object> retryInfoMap = new HashMap<>();
            retryInfoMap.put("attempt", 3);
            retryInfoMap.put("maxAttempts", 3);
            retryInfoMap.put("delaySeconds", 0);
            retryInfoMap.put("failed", true);
            sayAskHandler.say(
                    ClineSay.ERROR_RETRY, JsonUtils.toJsonString(retryInfoMap), null, null, null);
        } catch (Exception sayError) {
            log.error("Failed to say error retry: {}", sayError.getMessage(), sayError);
        }

        sayAskHandler.ask(ClineAsk.API_REQ_FAILED, streamingFailedMessage);
        taskDispatch.dispatch(new ApiFailedEvent(taskId, e));
        return Flux.error(new RuntimeException("API request failed"));
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
                    String newAssistantMessage = assistantMessageBuilder.toString();

                    messagePresenterHandler.updateAssistantMessageContent(
                            newAssistantMessage, oldAssistantMessage);
                }
                break;
            default:
                break;
        }
    }
}
