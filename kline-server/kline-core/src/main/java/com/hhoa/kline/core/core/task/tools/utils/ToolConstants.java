package com.hhoa.kline.core.core.task.tools.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 工具校验与配置的共享常量与辅助方法。 */
public final class ToolConstants {

    private ToolConstants() {}

    public static final List<String> TASK_CONFIG_KEYS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "taskId",
                            "ulid",
                            "cwd",
                            "mode",
                            "strictPlanModeEnabled",
                            "yoloModeToggled",
                            "context",
                            "taskState",
                            "messageState",
                            "api",
                            "services",
                            "autoApprovalSettings",
                            "autoApprover",
                            "browserSettings",
                            "focusChainSettings",
                            "callbacks",
                            "coordinator"));

    public static final List<String> TASK_SERVICES_KEYS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "mcpHub",
                            "browserSession",
                            "urlContentFetcher",
                            "diffViewProvider",
                            "fileContextTracker",
                            "clineIgnoreController",
                            "contextManager",
                            "stateManager"));

    public static final List<String> TASK_CALLBACKS_KEYS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "say",
                            "ask",
                            "saveCheckpoint",
                            "sayAndCreateMissingParamError",
                            "executeCommandTool",
                            "doesLatestTaskCompletionHaveNewChanges",
                            "updateFCListFromToolResponse",
                            "shouldAutoApproveToolWithPath",
                            "postStateToWebview",
                            "reinitExistingTaskFromId",
                            "cancelTask",
                            "updateTaskHistory",
                            "switchToActMode"));

    public static final List<String> PATH_REQUIRED_TOOLS =
            Collections.unmodifiableList(
                    Arrays.asList(
                            "read_file",
                            "write_to_file",
                            "replace_in_file",
                            "new_rule",
                            "list_files",
                            "list_code_definition_names",
                            "search_files"));

    public static final List<String> BROWSER_ACTIONS =
            Collections.unmodifiableList(
                    Arrays.asList("launch", "click", "type", "scroll_down", "scroll_up", "close"));

    public static final List<String> VALIDATION_ERROR_PATTERNS =
            Collections.unmodifiableList(
                    Arrays.asList("Missing required parameter", "blocked by .clineignore"));
}
