package com.hhoa.kline.core.core.task;

/** 表示单次请求处理后的后续动作 */
public enum ClineRequestResult {
    DID_TOOL_USE,
    DID_NOT_TOOL_USE,
    ABORT,
    FAILED;
}
