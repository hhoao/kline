package com.hhoa.kline.core.core.hooks;

/**
 * 空操作 runner，当没有找到 hook 脚本时使用。
 *
 * <p>Null-Object 模式：始终立即成功返回，无副作用。 调用方无需检查 null，简化了 hook 始终可选的设计。
 */
public class NoOpRunner extends HookRunner {

    public NoOpRunner(HookName hookName) {
        super(hookName);
    }

    @Override
    public HookOutput execute(HookInput input) {
        return HookOutput.success();
    }
}
