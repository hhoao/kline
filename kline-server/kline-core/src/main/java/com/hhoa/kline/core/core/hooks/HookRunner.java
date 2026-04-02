package com.hhoa.kline.core.core.hooks;

import java.util.List;
import lombok.Getter;

/**
 * 运行 hook 脚本并返回结果的抽象基类。
 *
 * <p>设计：HookRunner 是无状态的，可重用。每次调用 run() 都是独立的。
 */
@Getter
public abstract class HookRunner {

    private final HookName hookName;

    protected HookRunner(HookName hookName) {
        this.hookName = hookName;
    }

    /**
     * 执行 hook 并补充公共元数据
     *
     * @param input 完整的 HookInput
     * @return Hook 输出
     */
    public abstract HookOutput execute(HookInput input);

    /**
     * 补充公共参数后执行
     *
     * @param input 已完成参数的输入
     * @param workspaceRoots 工作区根目录列表
     * @param clineVersion Cline 版本
     * @param userId 用户 ID
     * @return 完整的 HookInput
     */
    protected HookInput completeInput(
            HookInput input, List<String> workspaceRoots, String clineVersion, String userId) {
        input.setClineVersion(clineVersion);
        input.setHookName(hookName.getValue());
        input.setTimestamp(String.valueOf(System.currentTimeMillis()));
        input.setWorkspaceRoots(workspaceRoots);
        input.setUserId(userId);
        return input;
    }
}
