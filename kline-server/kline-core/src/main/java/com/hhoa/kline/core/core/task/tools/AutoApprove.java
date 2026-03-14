package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Arrays;

/** 工具自动批准逻辑，对齐 TS 版本 autoApprove：yolo 模式、按工具/路径的自动批准设置、本地/外部区分。 */
public class AutoApprove {

    private final StateManager stateManager;
    private final WorkspaceRootManager workspaceManager;

    public AutoApprove(StateManager stateManager, WorkspaceRootManager workspaceManager) {
        this.stateManager = stateManager;
        this.workspaceManager = workspaceManager;
    }

    public AutoApproveToolResult shouldAutoApproveTool(String toolName) {
        ClineDefaultTool tool = fromValue(toolName);
        return tool != null ? shouldAutoApproveTool(tool) : AutoApproveToolResult.of(false);
    }

    public AutoApproveToolResult shouldAutoApproveTool(ClineDefaultTool tool) {
        if (tool == null) {
            return AutoApproveToolResult.of(false);
        }
        if (stateManager.getSettings().isYoloModeToggled()) {
            return switch (tool) {
                case FILE_READ,
                        LIST_FILES,
                        LIST_CODE_DEF,
                        SEARCH,
                        NEW_RULE,
                        FILE_NEW,
                        FILE_EDIT,
                        BASH ->
                        AutoApproveToolResult.of(true, true);
                case BROWSER, WEB_FETCH, MCP_ACCESS, MCP_USE -> AutoApproveToolResult.of(true);
                default -> AutoApproveToolResult.of(false);
            };
        }

        AutoApprovalSettings settings = stateManager.getSettings().getAutoApprovalSettings();
        if (settings == null || !settings.isEnabled()) {
            return AutoApproveToolResult.of(false);
        }
        AutoApprovalSettings.AutoApprovalActions actions = settings.getActions();
        if (actions == null) {
            return AutoApproveToolResult.of(false);
        }

        return switch (tool) {
            case FILE_READ, LIST_FILES, LIST_CODE_DEF, SEARCH ->
                    AutoApproveToolResult.of(
                            actions.isReadFiles(),
                            Boolean.TRUE.equals(actions.getReadFilesExternally()));
            case NEW_RULE, FILE_NEW, FILE_EDIT ->
                    AutoApproveToolResult.of(
                            actions.isEditFiles(),
                            Boolean.TRUE.equals(actions.getEditFilesExternally()));
            case BASH ->
                    AutoApproveToolResult.of(
                            actions.isExecuteSafeCommands(), actions.isExecuteAllCommands());
            case BROWSER, WEB_FETCH -> AutoApproveToolResult.of(actions.isUseBrowser());
            case MCP_ACCESS, MCP_USE -> AutoApproveToolResult.of(actions.isUseMcp());
            default -> AutoApproveToolResult.of(false);
        };
    }

    public boolean shouldAutoApproveToolWithPath(String toolName, String path, String cwd) {
        if (stateManager.getSettings().isYoloModeToggled()) {
            return true;
        }

        boolean isLocalRead = false;
        if (path != null && !path.isEmpty() && workspaceManager != null) {
            try {
                WorkspaceResolver.WorkspacePathResult pathResult =
                        WorkspaceResolver.resolveWorkspacePath(
                                new WorkspaceConfig(workspaceManager),
                                path,
                                "AutoApprove.shouldAutoApproveToolWithPath");
                String absolutePath = pathResult.absolutePath();
                isLocalRead = workspaceManager.isPathInWorkspace(absolutePath);
            } catch (Exception e) {
                isLocalRead = false;
            }
        }

        ClineDefaultTool tool = fromValue(toolName);
        AutoApproveToolResult result = shouldAutoApproveTool(tool);
        boolean autoApproveLocal = result.getFirst();
        boolean autoApproveExternal = result.isPair() ? result.getSecond() : false;

        return (isLocalRead && autoApproveLocal)
                || (!isLocalRead && autoApproveLocal && autoApproveExternal);
    }

    private static ClineDefaultTool fromValue(String value) {
        if (value == null || value.isEmpty()) return null;
        return Arrays.stream(ClineDefaultTool.values())
                .filter(t -> value.equals(t.getValue()))
                .findFirst()
                .orElse(null);
    }
}
