package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
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

    private boolean isStreaming = false;
    private boolean isWaitingForFirstChunk = false;
    private boolean didCompleteReadingStream = false;

    private int currentStreamingContentIndex = 0;
    private List<AssistantMessageContent> assistantMessageContent = new ArrayList<>();
    // 系统自动获取的信息，用于下一轮次的用户内容
    private List<UserContentBlock> nextUserMessageContent = new ArrayList<>();
    private boolean userMessageContentReady = false;

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

    private int consecutiveAutoApprovedRequestsCount = 0;

    private int consecutiveMistakeCount = 0;
    private boolean didAutomaticallyRetryFailedApiRequest = false;
    private String checkpointManagerErrorMessage;

    private int autoRetryAttempts = 0;

    private boolean isInitialized = false;

    private int apiRequestCount = 0;
    private int apiRequestsSinceLastTodoUpdate = 0;
    private String currentFocusChainChecklist = null;
    private boolean todoListWasUpdatedByUser = false;

    private boolean abort = false;
    private boolean didFinishAbortingStream = false;
    private boolean abandoned = false;

    private boolean currentlySummarizing = false;
    private Integer lastAutoCompactTriggerIndex;

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
