package com.hhoa.kline.core.core.task.handler;

/**
 * PrepareContextResult
 *
 * @author xianxing
 * @since 2026/3/18
 */
public interface PrepareContextResult {
    record Abort() implements PrepareContextResult {}

    record Success() implements PrepareContextResult {}

    record Failed() implements PrepareContextResult {}

    record MaxMistakeLimitReached(String message) implements PrepareContextResult {}

    record AutoApprovalMaxReqReached(String message) implements PrepareContextResult {}

    /** Signals that the task loop should end (e.g. yolo mode consecutive mistake failure). */
    record EndLoop() implements PrepareContextResult {}
}
