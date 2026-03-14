package com.hhoa.kline.core.core.workspace;

/**
 * WorkspaceConfig - 工作区配置
 *
 * <p>所有工作区解析都依赖 {@link WorkspaceRootManager}，不再提供单根兼容模式。
 */
public record WorkspaceConfig(WorkspaceRootManager workspaceManager) {

    public WorkspaceConfig {
        if (workspaceManager == null) {
            throw new IllegalArgumentException("workspaceManager 不能为空");
        }
    }
}
