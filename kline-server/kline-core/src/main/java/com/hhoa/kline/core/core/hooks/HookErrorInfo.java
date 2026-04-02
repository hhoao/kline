package com.hhoa.kline.core.core.hooks;

import lombok.Builder;
import lombok.Getter;

/** Hook 失败的结构化错误信息 */
@Getter
@Builder
public class HookErrorInfo {
    /** 错误类型 */
    private final HookErrorType type;

    /** 用户友好的错误消息 */
    private final String message;

    /** 调试用的技术细节 */
    private final String details;

    /** 失败的 hook 脚本路径 */
    private final String scriptPath;

    /** 退出码（如果有） */
    private final Integer exitCode;

    /** stderr 输出（如果有） */
    private final String stderr;
}
