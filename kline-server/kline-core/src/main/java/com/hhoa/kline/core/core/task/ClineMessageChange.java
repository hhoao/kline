package com.hhoa.kline.core.core.task;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * Represents a change to the clineMessages list, emitted by {@link MessageStateHandler} for
 * listeners that need to react to message state mutations (e.g. the presentation scheduler).
 */
@Getter
@Builder
public class ClineMessageChange {

    public enum ChangeType {
        ADD,
        UPDATE,
        DELETE,
        SET
    }

    /** The type of change. */
    private final ChangeType type;

    /** The full array after the change. */
    private final List<ClineMessage> messages;

    /** The affected index (for add/update/delete). */
    private final Integer index;

    /** The new/updated message (for add/update). */
    private final ClineMessage message;

    /** The old message before change (for update/delete). */
    private final ClineMessage previousMessage;

    /** The entire previous array (for set). */
    private final List<ClineMessage> previousMessages;
}
