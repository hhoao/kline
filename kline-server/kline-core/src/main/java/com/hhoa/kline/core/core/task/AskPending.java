package com.hhoa.kline.core.core.task;

import com.hhoa.kline.core.core.shared.ClineAsk;
import lombok.Data;

@Data
public class AskPending {
    private String pendingId;
    private ClineAsk askType;

    /** Whether this ask was cancelled by a hook. */
    private boolean cancelled;

    /** Hook context modification to inject into user content (set by TaskResume hook). */
    private String hookContextModification;
}
