package com.hhoa.kline.core.core.hooks;

import lombok.Builder;
import lombok.Getter;

/** Hook 执行的最终结果 */
@Getter
@Builder
public class HookExecutionResult {
    /** 是否请求取消 */
    private final Boolean cancel;

    /** 上下文修改（注入到对话中） */
    private final String contextModification;

    /** 错误消息 */
    private final String errorMessage;

    /** 是否被用户取消 */
    @Builder.Default private final boolean wasCancelled = false;

    /** 空结果（hook 未执行或不存在） */
    public static HookExecutionResult empty() {
        return HookExecutionResult.builder().wasCancelled(false).build();
    }
}
