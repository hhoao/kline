package com.hhoa.kline.core.core.hooks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hook 脚本的标准输出 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HookOutput {
    /** true 表示取消任务 */
    @Builder.Default private boolean cancel = false;

    /** 可选的上下文修改（注入到对话中） */
    @Builder.Default private String contextModification = "";

    /** 可选的错误消息 */
    @Builder.Default private String errorMessage = "";

    /** 创建一个表示成功（不取消）的默认输出 */
    public static HookOutput success() {
        return HookOutput.builder().build();
    }

    /** 创建一个请求取消的输出 */
    public static HookOutput cancelled() {
        return HookOutput.builder().cancel(true).build();
    }
}
