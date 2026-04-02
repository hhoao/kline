package com.hhoa.kline.core.core.task;

import lombok.Builder;
import lombok.Data;

/** Tracks a currently executing hook for cancellation and status display purposes. */
@Data
@Builder
public class HookExecution {
    /** The name of the hook being executed (e.g. "pre_tool_use"). */
    private final String hookName;

    /** The name of the tool that triggered the hook (if applicable). */
    private final String toolName;

    /** Timestamp of the message showing hook execution status. */
    private final Long messageTs;

    /** Whether this hook execution has been cancelled. */
    private volatile boolean cancelled;

    /**
     * Action to run when cancelling (e.g. kill subprocess). Set by HookRunner when process starts.
     * Mirrors TypeScript's AbortController pattern.
     */
    @Builder.Default private volatile Runnable abortAction = null;

    public void cancel() {
        this.cancelled = true;
        Runnable action = this.abortAction;
        if (action != null) {
            action.run();
        }
    }
}
