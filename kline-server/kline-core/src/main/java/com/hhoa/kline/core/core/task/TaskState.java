package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.tools.types.ToolState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.Getter;

/**
 * Aggregates task-scoped mutable state.
 *
 * <p>The public sub-states group fields by responsibility. Existing top-level accessors are kept as
 * thin delegates while callers are migrated toward the grouped state objects and semantic methods.
 */
@Getter
public class TaskState {

    private final TaskLifecycleState lifecycleState = new TaskLifecycleState();
    private final StreamState streamState = new StreamState();
    private final PresentationState presentationState = new PresentationState();
    private final ToolExecutionState toolExecutionState = new ToolExecutionState();
    private final AskState askState = new AskState();
    private final ContextWindowState contextWindowState = new ContextWindowState();
    private final CheckpointState checkpointState = new CheckpointState();
    private final FocusChainState focusChainState = new FocusChainState();
    private final ApiTurnState apiTurnState = new ApiTurnState();

    public void resetForNewApiRequest() {
        presentationState.resetForNewApiRequest();
        streamState.setDidCompleteReadingStream(false);
        toolExecutionState.resetForNewApiRequest();
        apiTurnState.setDidAutomaticallyRetryFailedApiRequest(false);
    }

    public void markNativeToolCallsPresent() {
        toolExecutionState.setUseNativeToolCalls(true);
    }

    public ClineAskResponse getAskResponse() {
        return askState.getAskResponse();
    }

    public void setAskResponse(ClineAskResponse askResponse) {
        askState.setAskResponse(askResponse);
    }

    public Long getLastMessageTs() {
        return askState.getLastMessageTs();
    }

    public void setLastMessageTs(Long lastMessageTs) {
        askState.setLastMessageTs(lastMessageTs);
    }

    public boolean isAwaitingPlanResponse() {
        return askState.isAwaitingPlanResponse();
    }

    public void setAwaitingPlanResponse(boolean awaitingPlanResponse) {
        askState.setAwaitingPlanResponse(awaitingPlanResponse);
    }

    public boolean isDidRespondToPlanAskBySwitchingMode() {
        return askState.isDidRespondToPlanAskBySwitchingMode();
    }

    public void setDidRespondToPlanAskBySwitchingMode(boolean didRespond) {
        askState.setDidRespondToPlanAskBySwitchingMode(didRespond);
    }

    public Queue<AskResult> getPendingUserResponses() {
        return askState.getPendingUserResponses();
    }

    public void setPendingUserResponses(Queue<AskResult> pendingUserResponses) {
        askState.setPendingUserResponses(pendingUserResponses);
    }

    public int[] getConversationHistoryDeletedRange() {
        return contextWindowState.getConversationHistoryDeletedRange();
    }

    public void setConversationHistoryDeletedRange(int[] conversationHistoryDeletedRange) {
        contextWindowState.setConversationHistoryDeletedRange(conversationHistoryDeletedRange);
    }

    public boolean isCurrentlySummarizing() {
        return contextWindowState.isCurrentlySummarizing();
    }

    public void setCurrentlySummarizing(boolean currentlySummarizing) {
        contextWindowState.setCurrentlySummarizing(currentlySummarizing);
    }

    public Integer getLastAutoCompactTriggerIndex() {
        return contextWindowState.getLastAutoCompactTriggerIndex();
    }

    public void setLastAutoCompactTriggerIndex(Integer lastAutoCompactTriggerIndex) {
        contextWindowState.setLastAutoCompactTriggerIndex(lastAutoCompactTriggerIndex);
    }

    public String getCheckpointManagerErrorMessage() {
        return checkpointState.getCheckpointManagerErrorMessage();
    }

    public void setCheckpointManagerErrorMessage(String checkpointManagerErrorMessage) {
        checkpointState.setCheckpointManagerErrorMessage(checkpointManagerErrorMessage);
    }

    public boolean isInitialized() {
        return lifecycleState.isInitialized();
    }

    public void setInitialized(boolean initialized) {
        lifecycleState.setInitialized(initialized);
    }

    public HookExecution getActiveHookExecution() {
        return lifecycleState.getActiveHookExecution();
    }

    public void setActiveHookExecution(HookExecution activeHookExecution) {
        lifecycleState.setActiveHookExecution(activeHookExecution);
    }

    public boolean isAbort() {
        return lifecycleState.isAbort();
    }

    public void setAbort(boolean abort) {
        lifecycleState.setAbort(abort);
    }

    public boolean isDidFinishAbortingStream() {
        return lifecycleState.isDidFinishAbortingStream();
    }

    public void setDidFinishAbortingStream(boolean didFinishAbortingStream) {
        lifecycleState.setDidFinishAbortingStream(didFinishAbortingStream);
    }

    public boolean isAbandoned() {
        return lifecycleState.isAbandoned();
    }

    public void setAbandoned(boolean abandoned) {
        lifecycleState.setAbandoned(abandoned);
    }

    public int getApiRequestCount() {
        return focusChainState.getApiRequestCount();
    }

    public void setApiRequestCount(int apiRequestCount) {
        focusChainState.setApiRequestCount(apiRequestCount);
    }

    public int getApiRequestsSinceLastTodoUpdate() {
        return focusChainState.getApiRequestsSinceLastTodoUpdate();
    }

    public void setApiRequestsSinceLastTodoUpdate(int apiRequestsSinceLastTodoUpdate) {
        focusChainState.setApiRequestsSinceLastTodoUpdate(apiRequestsSinceLastTodoUpdate);
    }

    public String getCurrentFocusChainChecklist() {
        return focusChainState.getCurrentFocusChainChecklist();
    }

    public void setCurrentFocusChainChecklist(String currentFocusChainChecklist) {
        focusChainState.setCurrentFocusChainChecklist(currentFocusChainChecklist);
    }

    public boolean isTodoListWasUpdatedByUser() {
        return focusChainState.isTodoListWasUpdatedByUser();
    }

    public void setTodoListWasUpdatedByUser(boolean todoListWasUpdatedByUser) {
        focusChainState.setTodoListWasUpdatedByUser(todoListWasUpdatedByUser);
    }

    @Data
    public static class TaskLifecycleState {
        private boolean initialized = false;
        private HookExecution activeHookExecution;
        private boolean abort = false;
        private boolean didFinishAbortingStream = false;
        private boolean abandoned = false;
    }

    @Data
    public static class StreamState {
        private boolean streaming = false;
        private boolean waitingForFirstChunk = false;
        private boolean didCompleteReadingStream = false;
    }

    @Data
    public static class PresentationState {
        private int currentStreamingContentIndex = 0;
        private List<AssistantMessageContent> assistantMessageContent = new ArrayList<>();
        private List<UserContentBlock> nextUserMessageContent = new ArrayList<>();
        private boolean userMessageContentReady = false;

        private void resetForNewApiRequest() {
            currentStreamingContentIndex = 0;
            assistantMessageContent = new ArrayList<>();
            nextUserMessageContent = new ArrayList<>();
            userMessageContentReady = false;
        }
    }

    @Data
    public static class ToolExecutionState {
        private boolean didRejectTool = false;
        private boolean didAlreadyUseTool = false;
        private String lastToolName = "";
        private final ConcurrentHashMap<String, FileReadCacheEntry> fileReadCache =
                new ConcurrentHashMap<>();
        private boolean useNativeToolCalls = false;
        private final ConcurrentHashMap<String, PendingAskToken> pendingAskTokens =
                new ConcurrentHashMap<>();
        private Map<String, ToolState> toolStates = new ConcurrentHashMap<>();
        private boolean enableParallelToolCalling = false;

        private void resetForNewApiRequest() {
            didRejectTool = false;
            didAlreadyUseTool = false;
            useNativeToolCalls = false;
        }
    }

    @Data
    public static class AskState {
        private ClineAskResponse askResponse;
        private Long lastMessageTs;
        private boolean awaitingPlanResponse = false;
        private boolean didRespondToPlanAskBySwitchingMode = false;
        private Queue<AskResult> pendingUserResponses = new ArrayDeque<>();
    }

    @Data
    public static class ContextWindowState {
        private int[] conversationHistoryDeletedRange;
        private boolean currentlySummarizing = false;
        private Integer lastAutoCompactTriggerIndex;
    }

    @Data
    public static class CheckpointState {
        private String checkpointManagerErrorMessage;
    }

    @Data
    public static class FocusChainState {
        private int apiRequestCount = 0;
        private int apiRequestsSinceLastTodoUpdate = 0;
        private String currentFocusChainChecklist = null;
        private boolean todoListWasUpdatedByUser = false;
    }

    @Data
    public static class ApiTurnState {
        private int consecutiveMistakeCount = 0;
        private boolean didAutomaticallyRetryFailedApiRequest = false;
        private int autoRetryAttempts = 0;
        private List<UserContentBlock> currentUserContent;
        private boolean currentIncludeFileDetails;
        private int currentPreviousApiReqIndex = -1;
        private ApiRequestResult apiRequestResult;
    }

    /** Cache entry for file read deduplication. */
    @Data
    public static class FileReadCacheEntry {
        private int readCount;
        private long mtime;
        private ImageContentBlock imageBlock;

        public FileReadCacheEntry(int readCount, long mtime, ImageContentBlock imageBlock) {
            this.readCount = readCount;
            this.mtime = mtime;
            this.imageBlock = imageBlock;
        }
    }
}
