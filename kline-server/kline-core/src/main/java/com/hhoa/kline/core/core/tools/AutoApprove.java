package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.shared.AutoApprovalSettings;
import com.hhoa.kline.core.core.storage.StateManager;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.workspace.WorkspaceRootManager;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Arrays;

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
            return yoloOrApproveAllResult(tool);
        }
        if (stateManager.getSettings().isAutoApproveAllToggled()) {
            return yoloOrApproveAllResult(tool);
        }

        AutoApprovalSettings settings = stateManager.getSettings().getAutoApprovalSettings();
        if (settings == null) {
            return AutoApproveToolResult.of(false);
        }
        AutoApprovalSettings.AutoApprovalActions actions = settings.getActions();
        if (actions == null) {
            return AutoApproveToolResult.of(false);
        }

        return switch (tool) {
            case FILE_READ, LIST_FILES, LIST_CODE_DEF, SEARCH, USE_SUBAGENTS ->
                    AutoApproveToolResult.of(
                            actions.isReadFiles(),
                            Boolean.TRUE.equals(actions.getReadFilesExternally()));
            case NEW_RULE, FILE_NEW, FILE_EDIT, APPLY_PATCH ->
                    AutoApproveToolResult.of(
                            actions.isEditFiles(),
                            Boolean.TRUE.equals(actions.getEditFilesExternally()));
            case BASH ->
                    AutoApproveToolResult.of(
                            actions.isExecuteSafeCommands(), actions.isExecuteAllCommands());
            case BROWSER, WEB_FETCH, WEB_SEARCH -> AutoApproveToolResult.of(actions.isUseBrowser());
            case MCP_ACCESS, MCP_USE -> AutoApproveToolResult.of(actions.isUseMcp());
            default -> AutoApproveToolResult.of(false);
        };
    }

    /** 与 Cline {@code autoApprove.ts} 中 yolo / autoApproveAll 分支一致。 */
    private static AutoApproveToolResult yoloOrApproveAllResult(ClineDefaultTool tool) {
        return switch (tool) {
            case FILE_READ,
                    LIST_FILES,
                    LIST_CODE_DEF,
                    SEARCH,
                    NEW_RULE,
                    FILE_NEW,
                    FILE_EDIT,
                    APPLY_PATCH,
                    BASH,
                    USE_SUBAGENTS ->
                    AutoApproveToolResult.of(true, true);
            case BROWSER, WEB_FETCH, WEB_SEARCH, MCP_ACCESS, MCP_USE ->
                    AutoApproveToolResult.of(true);
            default -> AutoApproveToolResult.of(false);
        };
    }

    public boolean shouldAutoApproveToolWithPath(String toolName, String path, String cwd) {
        if (stateManager.getSettings().isYoloModeToggled()) {
            return true;
        }
        if (stateManager.getSettings().isAutoApproveAllToggled()) {
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
