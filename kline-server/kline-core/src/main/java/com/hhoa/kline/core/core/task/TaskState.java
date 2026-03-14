package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.shared.ClineAskResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class TaskState {

    private boolean isStreaming = false;
    private boolean isWaitingForFirstChunk = false;
    private boolean didCompleteReadingStream = false;

    private int currentStreamingContentIndex = 0;
    private List<AssistantMessageContent> assistantMessageContent = new ArrayList<>();
    private List<UserContentBlock> userMessageContent = new ArrayList<>();
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
    private boolean running = false;
}
