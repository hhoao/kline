package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;

@Data
public class TaskState {

    // Task-level timing
    private long taskStartTimeMs = System.currentTimeMillis();
    private Long taskFirstTokenTimeMs;

    // Streaming flags
    private boolean isStreaming = false;
    private boolean isWaitingForFirstChunk = false;
    private boolean didCompleteReadingStream = false;

    // Presentation lock flags — used by TaskPresentationScheduler to prevent
    // concurrent presentation of assistant messages
    private boolean presentAssistantMessageLocked = false;
    private boolean presentAssistantMessageHasPendingUpdates = false;

    private int currentStreamingContentIndex = 0;
    private List<AssistantMessageContent> assistantMessageContent = new ArrayList<>();
    // 系统自动获取的信息，用于下一轮次的用户内容
    private List<UserContentBlock> nextUserMessageContent = new ArrayList<>();
    private boolean userMessageContentReady = false;

    /** Map of tool names to their tool_use_id for creating proper ToolResultBlockParam. */
    private final ConcurrentHashMap<String, String> toolUseIdMap = new ConcurrentHashMap<>();

    private ClineAskResponse askResponse;
    private String askResponseText;
    private List<String> askResponseImages;
    private List<String> askResponseFiles;
    private Long lastMessageTs;

    private boolean isAwaitingPlanResponse = false;
    private boolean didRespondToPlanAskBySwitchingMode = false;

    private int[] conversationHistoryDeletedRange;

    private boolean didRejectTool = false;
    private boolean didAlreadyUseTool = false;
    private boolean didEditFile = false;

    /** Track last tool used for consecutive call detection (used by act_mode_respond). */
    private String lastToolName = "";

    /**
     * File read deduplication cache — prevents the model from endlessly reading the same files.
     * Maps lowercase absolute file path → cache entry.
     */
    private final ConcurrentHashMap<String, FileReadCacheEntry> fileReadCache =
            new ConcurrentHashMap<>();

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

    private int consecutiveMistakeCount = 0;
    private boolean doubleCheckCompletionPending = false;
    private boolean didAutomaticallyRetryFailedApiRequest = false;
    private String checkpointManagerErrorMessage;

    private int autoRetryAttempts = 0;

    private boolean isInitialized = false;

    private int apiRequestCount = 0;
    private int apiRequestsSinceLastTodoUpdate = 0;
    private String currentFocusChainChecklist = null;
    private boolean todoListWasUpdatedByUser = false;

    /** The currently executing hook, if any (for cancellation and status tracking). */
    private HookExecution activeHookExecution;

    private boolean abort = false;
    private boolean didFinishAbortingStream = false;
    private boolean abandoned = false;

    private boolean currentlySummarizing = false;
    private Integer lastAutoCompactTriggerIndex;

    /**
     * Whether the task is using native tool calls. Used to determine response formatting (e.g. we
     * don't add noToolsUsed response when native tool call is used because of the expected format
     * from the tool calls is different).
     */
    private boolean useNativeToolCalls = false;

    /** 当前 API 轮次的用户内容 */
    private List<UserContentBlock> currentUserContent;

    /** 当前轮次是否需要包含文件详情 */
    private boolean currentIncludeFileDetails;

    /** 当前轮次的 previousApiReqIndex（由 doPrepareContext 计算，供 doCallApi 使用） */
    private int currentPreviousApiReqIndex = -1;

    private ApiRequestResult apiRequestResult;

    private final ConcurrentHashMap<String, PendingAskToken> pendingAskTokens =
            new ConcurrentHashMap<>();

    private Queue<AskResult> pendingUserResponses = new ArrayDeque<>();

    private Map<String, ToolState> toolStates = new ConcurrentHashMap<>();
}
