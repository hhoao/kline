package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.tools.handlers.AccessMcpResourceHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AskFollowupQuestionToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.AttemptCompletionHandler;
import com.hhoa.kline.core.core.task.tools.handlers.CondenseHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ExecuteCommandToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.FullyManagedTool;
import com.hhoa.kline.core.core.task.tools.handlers.HandlerUtils;
import com.hhoa.kline.core.core.task.tools.handlers.ListCodeDefinitionNamesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ListFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.LoadMcpDocumentationHandler;
import com.hhoa.kline.core.core.task.tools.handlers.NewTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.PlanModeRespondHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReadFileToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ReportBugHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SearchFilesToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.SummarizeTaskHandler;
import com.hhoa.kline.core.core.task.tools.handlers.UseMcpToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WebFetchToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.WriteToFileToolHandler;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolDisplayUtils;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** 工具执行器：负责注册与路由各类工具处理器。 参考 TypeScript 版本实现，包含错误处理、计划模式限制、部分块处理等功能。 */
@Slf4j
public class ToolExecutor implements ToolExecutorCoordinator {
    private static final List<String> PLAN_MODE_RESTRICTED_TOOLS =
            Arrays.asList("write_to_file", "replace_in_file", "new_rule");

    private final Map<String, FullyManagedTool> nameToHandler = new HashMap<>();
    private final ClineIgnoreController clineIgnoreController;
    private final ResponseFormatter responseFormatter;

    public ToolExecutor(
            ClineIgnoreController clineIgnoreController, ResponseFormatter responseFormatter) {
        this.clineIgnoreController = clineIgnoreController;
        this.responseFormatter =
                responseFormatter != null ? responseFormatter : new ResponseFormatter();
        registerToolHandlers();
    }

    public ToolExecutor register(FullyManagedTool handler) {
        if (handler != null && handler.getName() != null) {
            nameToHandler.put(handler.getName(), handler);
        }
        return this;
    }

    public boolean has(String toolName) {
        return nameToHandler.containsKey(toolName);
    }

    @Override
    public ToolHandler getHandler(String toolName) {
        FullyManagedTool h = nameToHandler.get(toolName);
        if (h == null) return null;
        return h::getDescription;
    }

    public List<UserContentBlock> execute(String toolName, ToolUse block, TaskConfig config) {
        if (block == null) {
            return HandlerUtils.createTextBlocks(
                    responseFormatter.toolError("ToolUse block is null"));
        }
        // 确保 block 的名称与 toolName 一致
        if (block.getName() == null) {
            block.setName(toolName);
        }
        return executeTool(block, config);
    }

    public List<UserContentBlock> executeTool(ToolUse block, TaskConfig config) {
        if (block == null) {
            return HandlerUtils.createTextBlocks(
                    responseFormatter.toolError("ToolUse block is null"));
        }
        if (config == null) {
            return HandlerUtils.createTextBlocks(responseFormatter.toolError("TaskConfig is null"));
        }

        try {
            return executeInternal(block, config);
        } catch (Exception error) {
            log.error("Error executing tool: {}", block.getName(), error);
            return handleError("executing " + block.getName(), error, block, config);
        }
    }

    private List<UserContentBlock> executeInternal(ToolUse block, TaskConfig config) {
        String toolName = block.getName();
        if (toolName == null || toolName.isEmpty()) {
            return HandlerUtils.createTextBlocks(responseFormatter.toolError("Tool name is empty"));
        }

        if (!has(toolName)) {
            String errorMsg = "(Unsupported tool: " + toolName + ")";
            return HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMsg));
        }

        if (config.getTaskState() != null && config.getTaskState().isDidRejectTool()) {
            String reason =
                    block.isPartial()
                            ? "Tool was interrupted and not executed due to user rejecting a previous tool."
                            : "Skipping tool due to user rejecting a previous tool.";
            createToolRejectionMessage(block, reason, config);
            return HandlerUtils.createTextBlocks(responseFormatter.toolDenied());
        }

        if (config.getTaskState() != null && config.getTaskState().isDidAlreadyUseTool()) {
            String toolAlreadyUsedMsg = responseFormatter.toolAlreadyUsed(toolName);
            if (config.getTaskState().getUserMessageContent() != null) {
                TextContentBlock userContentBlock = new TextContentBlock(toolAlreadyUsedMsg);
                config.getTaskState().getUserMessageContent().add(userContentBlock);
            }
            return HandlerUtils.createTextBlocks(toolAlreadyUsedMsg);
        }

        if (isPlanModeToolRestricted(toolName, config)) {
            String errorMessage =
                    String.format(
                            "Tool '%s' is not available in PLAN MODE. This tool is restricted to ACT MODE for file modifications. Only use tools available for PLAN MODE when in that mode.",
                            toolName);
            config.getCallbacks().say(ClineSay.ERROR, errorMessage, null, null, false, null);
            pushToolResult(responseFormatter.toolError(errorMessage), block, config);
            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }
            return HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMessage));
        }

        if (block.isPartial()) {
            handlePartialBlock(block, config);
            return HandlerUtils.createTextBlocks("(partial block processed)");
        }

        return handleCompleteBlock(block, config);
    }

    private boolean isPlanModeToolRestricted(String toolName, TaskConfig config) {
        Boolean strictPlanModeEnabled = getStrictPlanModeEnabled(config);
        if (strictPlanModeEnabled == null || !strictPlanModeEnabled) {
            return false;
        }

        Mode mode = config.getMode();
        if (mode != Mode.PLAN) {
            return false;
        }

        return PLAN_MODE_RESTRICTED_TOOLS.contains(toolName);
    }

    private Boolean getStrictPlanModeEnabled(TaskConfig config) {
        if (config.getServices() != null && config.getServices().getStateManager() != null) {
            try {
                return config.getServices()
                        .getStateManager()
                        .getSettings()
                        .isStrictPlanModeEnabled();
            } catch (Exception e) {
                log.debug("Failed to get strictPlanModeEnabled from StateManager", e);
            }
        }
        return null;
    }

    private void createToolRejectionMessage(ToolUse block, String reason, TaskConfig config) {
        if (config.getTaskState() == null
                || config.getTaskState().getUserMessageContent() == null) {
            return;
        }
        String description = ToolDisplayUtils.getToolDescription(block, this);
        String message = reason + " " + description;
        TextContentBlock userContentBlock = new TextContentBlock(message);
        config.getTaskState().getUserMessageContent().add(userContentBlock);
    }

    private void handlePartialBlock(ToolUse block, TaskConfig config) {
        FullyManagedTool handler = nameToHandler.get(block.getName());
        if (handler == null) {
            return;
        }

        UIHelpers uiHelpers = UIHelpers.create(config);

        handler.handlePartialBlock(block, uiHelpers);
    }

    private List<UserContentBlock> handleCompleteBlock(ToolUse block, TaskConfig config) {
        FullyManagedTool handler = nameToHandler.get(block.getName());
        if (handler == null) {
            String errorMsg = "Handler not found for tool: " + block.getName();
            List<UserContentBlock> errorResult =
                    HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMsg));
            pushToolResult(errorResult, block, config);
            return errorResult;
        }

        ClineToolSpec spec = handler.getClineToolSpec();
        if (spec != null && spec.getParameters() != null) {
            List<String> required =
                    spec.getParameters().stream()
                            .filter(ClineToolSpec.ClineToolSpecParameter::isRequired)
                            .map(ClineToolSpec.ClineToolSpecParameter::getName)
                            .toList();
            for (String paramName : required) {
                Object v = block.getParams() == null ? null : block.getParams().get(paramName);
                if (v == null || String.valueOf(v).trim().isEmpty()) {
                    if (config.getTaskState() != null) {
                        config.getTaskState()
                                .setConsecutiveMistakeCount(
                                        config.getTaskState().getConsecutiveMistakeCount() + 1);
                    }
                    String errorResult =
                            config.getCallbacks()
                                    .sayAndCreateMissingParamError(block.getName(), paramName);
                    List<UserContentBlock> errorBlocks = HandlerUtils.createTextBlocks(errorResult);
                    pushToolResult(errorBlocks, block, config);
                    return errorBlocks;
                }
            }
        }

        try {
            List<UserContentBlock> result = handler.execute(config, block);

            pushToolResult(result, block, config);

            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }

            if (config.getTaskState() != null
                    && config.getServices() != null
                    && config.getServices().getStateManager() != null
                    && !block.isPartial()) {
                try {
                    FocusChainSettings focusChainSettings =
                            config.getServices()
                                    .getStateManager()
                                    .getSettings()
                                    .getFocusChainSettings();
                    if (focusChainSettings != null
                            && focusChainSettings.isEnabled()
                            && config.getCallbacks() != null) {
                        String taskProgress =
                                block.getParams() != null
                                        ? String.valueOf(block.getParams().get("task_progress"))
                                        : null;
                        config.getCallbacks().updateFCListFromToolResponse(taskProgress);
                    }
                } catch (Exception e) {
                    log.debug("Failed to update Focus Chain", e);
                }
            }

            return result;
        } catch (Exception error) {
            log.error("Error in handleCompleteBlock for tool: {}", block.getName(), error);
            String errorMsg = "Tool execution failed: " + error.getMessage();
            List<UserContentBlock> errorResult =
                    HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMsg));
            pushToolResult(errorResult, block, config);
            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }
            return errorResult;
        }
    }

    private List<UserContentBlock> handleError(
            String action, Throwable error, ToolUse block, TaskConfig config) {
        String errorString =
                "Error " + action + ": " + (error != null ? error.getMessage() : "Unknown error");
        if (config.getCallbacks() != null) {
            config.getCallbacks().say(ClineSay.ERROR, errorString, null, null, false, null);
        }

        List<UserContentBlock> errorResponse =
                HandlerUtils.createTextBlocks(responseFormatter.toolError(errorString));
        pushToolResult(errorResponse, block, config);
        return errorResponse;
    }

    private void pushToolResult(String content, ToolUse block, TaskConfig config) {
        if (config.getTaskState() == null
                || config.getTaskState().getUserMessageContent() == null) {
            return;
        }

        ToolResultUtils.pushToolResult(
                content,
                block,
                config.getTaskState().getUserMessageContent(),
                block2 -> ToolDisplayUtils.getToolDescription(block2, this),
                () -> {
                    if (config.getTaskState() != null) {
                        config.getTaskState().setDidAlreadyUseTool(true);
                    }
                },
                this);
    }

    private void pushToolResult(
            List<UserContentBlock> contentBlocks, ToolUse block, TaskConfig config) {
        if (config.getTaskState() == null
                || config.getTaskState().getUserMessageContent() == null) {
            return;
        }

        ToolResultUtils.pushToolResult(
                contentBlocks,
                block,
                config.getTaskState().getUserMessageContent(),
                block2 -> ToolDisplayUtils.getToolDescription(block2, this),
                () -> {
                    if (config.getTaskState() != null) {
                        config.getTaskState().setDidAlreadyUseTool(true);
                    }
                },
                this);
    }

    private void registerToolHandlers() {
        this.register(new ListFilesToolHandler());
        this.register(new ReadFileToolHandler());
        this.register(new AskFollowupQuestionToolHandler());
        this.register(new WebFetchToolHandler());

        WriteToFileToolHandler writeHandler = new WriteToFileToolHandler();
        this.register(writeHandler);
        this.register(new SharedToolHandler(ClineDefaultTool.FILE_EDIT, writeHandler));
        this.register(new SharedToolHandler(ClineDefaultTool.NEW_RULE, writeHandler));

        this.register(new ListCodeDefinitionNamesToolHandler());
        this.register(new SearchFilesToolHandler());
        this.register(new ExecuteCommandToolHandler());
        this.register(new UseMcpToolHandler());
        this.register(new AccessMcpResourceHandler());
        this.register(new LoadMcpDocumentationHandler());
        this.register(new PlanModeRespondHandler());
        this.register(new NewTaskHandler());
        this.register(new AttemptCompletionHandler());
        this.register(new CondenseHandler());
        this.register(new SummarizeTaskHandler());
        this.register(new ReportBugHandler());
    }
}
