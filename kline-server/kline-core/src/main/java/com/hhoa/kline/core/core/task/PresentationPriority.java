package com.hhoa.kline.core.core.task;

/**
 * Priority level for a presentation flush request.
 *
 * <ul>
 *   <li>{@code IMMEDIATE} — flush synchronously (delay = 0 ms). Used at semantic boundaries: first
 *       visible token, tool-call transitions, and finalization.
 *   <li>{@code NORMAL} — flush after the configured cadence delay, coalescing intermediate chunks
 *       to reduce message-passing overhead.
 * </ul>
 */
public enum PresentationPriority {
    IMMEDIATE,
    NORMAL;

    private static final int IMMEDIATE_RANK = 1;
    private static final int NORMAL_RANK = 0;

    /**
     * Merge two priorities, returning the higher one.
     *
     * @param current the current priority (may be null)
     * @param next the incoming priority
     * @return the higher priority
     */
    public static PresentationPriority merge(
            PresentationPriority current, PresentationPriority next) {
        if (current == null) {
            return next;
        }
        return rank(next) > rank(current) ? next : current;
    }

    private static int rank(PresentationPriority p) {
        return p == IMMEDIATE ? IMMEDIATE_RANK : NORMAL_RANK;
    }
}
