package com.hhoa.kline.core.core.hooks;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** Hook 事件数据类型集合 */
public final class HookData {

    private HookData() {}

    /** PreToolUse hook 数据 */
    @Getter
    @Builder
    public static class PreToolUseData {
        private final String toolName;
        private final Map<String, String> parameters;
    }

    /** PostToolUse hook 数据 */
    @Getter
    @Builder
    public static class PostToolUseData {
        private final String toolName;
        private final Map<String, String> parameters;
        private final String result;
        private final boolean success;
        private final long executionTimeMs;
    }

    /** UserPromptSubmit hook 数据 */
    @Getter
    @Builder
    public static class UserPromptSubmitData {
        private final String prompt;
        private final List<String> attachments;
    }

    /** TaskStart hook 数据 */
    @Getter
    @Builder
    public static class TaskStartData {
        private final String task;
        private final Map<String, String> taskMetadata;
    }

    /** TaskResume hook 数据 */
    @Getter
    @Builder
    public static class TaskResumeData {
        private final String task;
        private final Map<String, String> taskMetadata;
        private final Map<String, String> previousState;
    }

    /** TaskCancel hook 数据 */
    @Getter
    @Builder
    public static class TaskCancelData {
        private final String task;
        private final Map<String, String> taskMetadata;
    }

    /** TaskComplete hook 数据 */
    @Getter
    @Builder
    public static class TaskCompleteData {
        private final String task;
        private final Map<String, String> taskMetadata;
    }

    /** Notification hook 数据 */
    @Getter
    @Builder
    public static class NotificationData {
        private final String event;
        private final String source;
        private final String message;
        private final boolean waitingForUserInput;
        private final Map<String, String> notificationData;
    }

    /** PreCompact hook 数据 */
    @Getter
    @Builder
    public static class PreCompactData {
        private final String taskId;
        private final String ulid;
        private final int contextSize;
        private final int messagesToCompact;
        private final String compactionStrategy;
        private final int previousApiReqIndex;
        private final int tokensIn;
        private final int tokensOut;
        private final int tokensInCache;
        private final int tokensOutCache;
        private final int deletedRangeStart;
        private final int deletedRangeEnd;
        private final String contextJsonPath;
        private final String contextRawPath;
    }
}
