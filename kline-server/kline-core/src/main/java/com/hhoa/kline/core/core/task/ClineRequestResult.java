package com.hhoa.kline.core.core.task;

/** 表示单次请求处理后的后续动作 */
public interface ClineRequestResult {
    record DidToolUse() implements ClineRequestResult {}

    record DidNotToolUse() implements ClineRequestResult {}

    record Abort() implements ClineRequestResult {}

    record Failed(String message, Throwable throwable) implements ClineRequestResult {}
}
