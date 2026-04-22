package com.hhoa.kline.core.core.task.tools;

import com.hhoa.kline.core.core.assistant.TextContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.hooks.HookData;
import com.hhoa.kline.core.core.hooks.HookExecutionResult;
import com.hhoa.kline.core.core.hooks.HookExecutor;
import com.hhoa.kline.core.core.hooks.HookInput;
import com.hhoa.kline.core.core.hooks.HookName;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.HookExecution;
import com.hhoa.kline.core.core.task.tools.handlers.HandlerUtils;
import com.hhoa.kline.core.core.task.tools.handlers.StateFullToolHandler;
import com.hhoa.kline.core.core.task.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.PendingAskToken.ToolUsePendingAskToken;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultExecutor implements ToolExecutor {
    private static final List<String> PLAN_MODE_RESTRICTED_TOOLS =
            Arrays.asList("write_to_file", "replace_in_file", "new_rule", "apply_patch");

    private final ToolRegistry registry;
    private final ClineIgnoreController clineIgnoreController;
    private final ResponseFormatter responseFormatter;

    /** Hook state tracking callbacks (set by TaskV2 after construction). */
    @Setter private Consumer<HookExecution> setActiveHookExecution;

    @Setter private Runnable clearActiveHookExecution;

    public DefaultExecutor(
            ClineIgnoreController clineIgnoreController, ResponseFormatter responseFormatter) {
        this.clineIgnoreController = clineIgnoreController;
        this.responseFormatter =
                responseFormatter != null ? responseFormatter : new ResponseFormatter();
        this.registry = new DefaultToolRegistry();
    }

    @Override
    public ToolRegistry getRegistry() {
        return registry;
    }

    /**
     * 非阻塞工具执行。返回 {@link ToolExecuteResult}：
     *
     * <ul>
     *   <li>{@code Immediate} — 工具立即完成
     *   <li>{@code PendingAsk} — 工具挂起等待用户响应，续延已注册到 PendingAskRegistry
     * </ul>
     */
    @Override
    public ToolExecuteResult executeTool(ToolUse block, ToolContext config) {
        if (block == null) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            responseFormatter.toolError("ToolUse block is null")));
        }
        if (config == null) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            responseFormatter.toolError("ToolContext is null")));
        }
        try {
            return executeInternalAsync(block, config);
        } catch (Exception error) {
            log.error("Error executing tool async: {}", block.getName(), error);
            return new ToolExecuteResult.Immediate(
                    handleError("executing " + block.getName(), error, block, config));
        }
    }

    @Override
    public ToolExecuteResult resume(
            PendingAskToken askToken, AskResult askResult, ToolContext context) {
        if (askToken instanceof ToolUsePendingAskToken toolUsePendingAskToken) {
            ToolHandler handler =
                    registry.getHandler(toolUsePendingAskToken.getToolUse().getName());

            if (handler instanceof StateFullToolHandler stateFullHandler) {
                ToolState toolState = context.getToolState();
                ToolExecuteResult result =
                        stateFullHandler.resume(
                                context, toolUsePendingAskToken.getToolUse(), toolState, askResult);
                if (result instanceof ToolExecuteResult.Immediate) {
                    if (context.getCallbacks() != null) {
                        context.getCallbacks().saveCheckpoint(false, null);
                    }
                    updateFocusChainIfNeeded(toolUsePendingAskToken.getToolUse(), context);
                }
                return result;
            } else {
                return handleCompleteBlockAsyncInternal(
                        toolUsePendingAskToken.getToolUse(), context, handler);
            }
        } else {
            throw new IllegalArgumentException(
                    "Invalid token type: " + askToken.getClass().getName());
        }
    }

    @Override
    public ToolState getOrCreateToolState(String name) {
        ToolHandler handler = registry.getHandler(name);
        ToolState toolState;
        if (handler instanceof StateFullToolHandler stateFullHandler) {
            toolState = stateFullHandler.createToolState();
        } else {
            toolState = new ToolState();
        }
        toolState.setName(name);
        return toolState;
    }

    private ToolExecuteResult executeInternalAsync(ToolUse block, ToolContext config) {
        String toolName = block.getName();
        if (toolName == null || toolName.isEmpty()) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            responseFormatter.toolError("Tool name is empty")));
        }
        if (!registry.has(toolName)) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            responseFormatter.toolError("(Unsupported tool: " + toolName + ")")));
        }
        if (config.getTaskState() != null && config.getTaskState().isDidRejectTool()) {
            String reason =
                    block.isPartial()
                            ? "Tool was interrupted and not executed due to user rejecting a previous tool."
                            : "Skipping tool due to user rejecting a previous tool.";
            createToolRejectionMessage(block, reason, config);
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(responseFormatter.toolDenied()));
        }
        if (config.getTaskState() != null && config.getTaskState().isDidAlreadyUseTool()) {
            String msg = responseFormatter.toolAlreadyUsed(toolName);
            if (config.getTaskState().getNextUserMessageContent() != null) {
                config.getTaskState().getNextUserMessageContent().add(new TextContentBlock(msg));
            }
            return new ToolExecuteResult.Immediate(HandlerUtils.createTextBlocks(msg));
        }
        if (isPlanModeToolRestricted(toolName, config)) {
            String errorMessage =
                    String.format(
                            "Tool '%s' is not available in PLAN MODE. This tool is restricted to ACT MODE for file modifications. Only use tools available for PLAN MODE when in that mode.",
                            toolName);
            config.getCallbacks().say(ClineSay.ERROR, errorMessage, null, null, false, null);
            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMessage)));
        }
        if (block.isPartial()) {
            handlePartialBlock(block, config);
            return new ToolExecuteResult.Partial();
        }
        return handleCompleteBlockAsync(block, config);
    }

    private ToolExecuteResult handleCompleteBlockAsync(ToolUse block, ToolContext config) {
        ToolHandler handler = registry.getHandler(block.getName());
        if (handler == null) {
            String errorMsg = "Handler not found for tool: " + block.getName();
            List<UserContentBlock> errorResult =
                    HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMsg));
            return new ToolExecuteResult.Immediate(errorResult);
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
                    return new ToolExecuteResult.Immediate(errorBlocks);
                }
            }
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        try {
            return handleCompleteBlockAsyncInternal(block, config, handler);
        } catch (Exception error) {
            log.error("Error in handleCompleteBlockAsync for tool: {}", block.getName(), error);
            String errorMsg = "Tool execution failed: " + error.getMessage();
            List<UserContentBlock> errorResult =
                    HandlerUtils.createTextBlocks(responseFormatter.toolError(errorMsg));
            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }
            return new ToolExecuteResult.Immediate(errorResult);
        }
    }

    private ToolExecuteResult handleCompleteBlockAsyncInternal(
            ToolUse block, ToolContext config, ToolHandler handler) {
        // Check abort flag at the very start to prevent execution after cancellation
        if (config.getTaskState() != null && config.getTaskState().isAbort()) {
            return new ToolExecuteResult.Immediate(List.of());
        }

        long executionStartTime = System.currentTimeMillis();
        ToolExecuteResult execResult = handler.execute(config, block);

        if (execResult instanceof ToolExecuteResult.Immediate immediateResult) {
            if (config.getCallbacks() != null) {
                config.getCallbacks().saveCheckpoint(false, null);
            }

            // Track the last executed tool for consecutive call detection
            if (config.getTaskState() != null) {
                config.getTaskState().setLastToolName(block.getName());
            }

            updateFocusChainIfNeeded(block, config);

            // Check abort before running PostToolUse hook
            if (config.getTaskState() != null && config.getTaskState().isAbort()) {
                return execResult;
            }

            // Execute PostToolUse hook
            runPostToolUseHook(block, immediateResult, true, executionStartTime, config);
        }
        return execResult;
    }

    /**
     * Runs the PostToolUse hook after tool execution completes.
     *
     * @param block The tool use block that was executed
     * @param result The immediate result from the tool execution
     * @param executionSuccess Whether the tool executed successfully
     * @param executionStartTime The timestamp when tool execution started
     * @param config The tool context
     */
    private void runPostToolUseHook(
            ToolUse block,
            ToolExecuteResult.Immediate result,
            boolean executionSuccess,
            long executionStartTime,
            ToolContext config) {
        try {
            boolean hooksEnabled = getHooksEnabled(config);
            if (!hooksEnabled) {
                return;
            }

            long executionTimeMs = System.currentTimeMillis() - executionStartTime;

            // Build tool parameters map from block params
            Map<String, String> parameters = new HashMap<>();
            if (block.getParams() != null) {
                block.getParams()
                        .forEach(
                                (k, v) -> {
                                    if (v != null) {
                                        parameters.put(k, String.valueOf(v));
                                    }
                                });
            }

            // Extract result text from the immediate result
            String resultText = extractResultText(result);

            HookData.PostToolUseData postToolUseData =
                    HookData.PostToolUseData.builder()
                            .toolName(block.getName())
                            .parameters(parameters)
                            .result(resultText)
                            .success(executionSuccess)
                            .executionTimeMs(executionTimeMs)
                            .build();

            HookInput hookInput =
                    HookInput.builder()
                            .hookName(HookName.POST_TOOL_USE.getValue())
                            .postToolUse(postToolUseData)
                            .taskId(config.getTaskId())
                            .workspaceRoots(getWorkspaceRootPaths(config))
                            .build();

            List<String> hooksDirs = getHooksDirs(config);
            List<String> workspaceRoots = getWorkspaceRootPaths(config);

            HookExecutionResult hookResult =
                    HookExecutor.executeHook(
                            HookExecutor.HookExecutionOptions.builder()
                                    .hookName(HookName.POST_TOOL_USE)
                                    .hookInput(hookInput)
                                    .cancellable(true)
                                    .hooksEnabled(hooksEnabled)
                                    .taskId(config.getTaskId())
                                    .toolName(block.getName())
                                    .hooksDirs(hooksDirs)
                                    .workspaceRoots(workspaceRoots)
                                    .say(
                                            config.getCallbacks() != null
                                                    ? (type, text) ->
                                                            config.getCallbacks()
                                                                    .say(
                                                                            ClineSay.valueOf(
                                                                                    type
                                                                                            .toUpperCase()),
                                                                            text,
                                                                            null,
                                                                            null,
                                                                            false,
                                                                            null)
                                                    : null)
                                    .setActiveHookExecution(setActiveHookExecution)
                                    .clearActiveHookExecution(clearActiveHookExecution)
                                    .build());

            // Handle cancellation request from hook
            if (hookResult.getCancel() != null && hookResult.getCancel()) {
                String errorMessage =
                        hookResult.getErrorMessage() != null
                                ? hookResult.getErrorMessage()
                                : "PostToolUse hook requested task cancellation";
                if (config.getCallbacks() != null) {
                    config.getCallbacks()
                            .say(ClineSay.ERROR, errorMessage, null, null, false, null);
                }
                log.warn("PostToolUse hook requested cancellation for tool: {}", block.getName());
            }

            // Add context modification to conversation if provided
            if (hookResult.getContextModification() != null
                    && !hookResult.getContextModification().trim().isEmpty()
                    && config.getTaskState() != null
                    && config.getTaskState().getNextUserMessageContent() != null) {
                String contextText = hookResult.getContextModification().trim();
                String hookContextBlock =
                        String.format(
                                "<hook_context source=\"PostToolUse\" type=\"general\">\n%s\n</hook_context>",
                                contextText);
                config.getTaskState()
                        .getNextUserMessageContent()
                        .add(new TextContentBlock(hookContextBlock));
            }

        } catch (Exception e) {
            log.error("PostToolUse hook execution failed for tool: {}", block.getName(), e);
            // Fail-open: do not propagate hook errors to tool execution
        }
    }

    private boolean getHooksEnabled(ToolContext config) {
        if (config.getServices() != null && config.getServices().getStateManager() != null) {
            try {
                return config.getServices().getStateManager().getSettings().isHooksEnabled();
            } catch (Exception e) {
                log.debug("Failed to get hooksEnabled from StateManager", e);
            }
        }
        return false;
    }

    private List<String> getWorkspaceRootPaths(ToolContext config) {
        List<String> roots = new ArrayList<>();
        if (config.getWorkspaceManager() != null) {
            try {
                config.getWorkspaceManager().getRoots().forEach(root -> roots.add(root.getPath()));
            } catch (Exception e) {
                log.debug("Failed to get workspace roots", e);
            }
        }
        if (roots.isEmpty() && config.getCwd() != null) {
            roots.add(config.getCwd());
        }
        return roots;
    }

    private List<String> getHooksDirs(ToolContext config) {
        List<String> hooksDirs = new ArrayList<>();
        List<String> workspaceRoots = getWorkspaceRootPaths(config);
        for (String root : workspaceRoots) {
            hooksDirs.add(root + "/.clinerules/hooks");
        }
        return hooksDirs;
    }

    private String extractResultText(ToolExecuteResult.Immediate result) {
        if (result.blocks() == null || result.blocks().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (var contentBlock : result.blocks()) {
            if (contentBlock instanceof TextContentBlock textBlock) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(textBlock.getText());
            }
        }
        return sb.toString();
    }

    private void updateFocusChainIfNeeded(ToolUse block, ToolContext config) {
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
                                            && block.getParams().get("task_progress") != null
                                    ? String.valueOf(block.getParams().get("task_progress"))
                                    : null;
                    if (taskProgress != null) {
                        config.getCallbacks().updateFCListFromToolResponse(taskProgress);
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to update Focus Chain", e);
            }
        }
    }

    private boolean isPlanModeToolRestricted(String toolName, ToolContext config) {
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

    private Boolean getStrictPlanModeEnabled(ToolContext config) {
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

    private void createToolRejectionMessage(ToolUse block, String reason, ToolContext config) {
        if (config.getTaskState() == null
                || config.getTaskState().getNextUserMessageContent() == null) {
            return;
        }
        String description = getToolDescription(block);
        String message = reason + " " + description;
        TextContentBlock userContentBlock = new TextContentBlock(message);
        config.getTaskState().getNextUserMessageContent().add(userContentBlock);
    }

    private String getToolDescription(ToolUse toolUse) {
        return getHandler(toolUse.getName()).getDescription(toolUse);
    }

    private void handlePartialBlock(ToolUse block, ToolContext config) {
        ToolHandler handler = registry.getHandler(block.getName());
        if (handler == null) {
            return;
        }
        UIHelpers uiHelpers = UIHelpers.create(config);

        handler.handlePartialBlock(block, uiHelpers);
    }

    private List<UserContentBlock> handleError(
            String action, Throwable error, ToolUse block, ToolContext config) {
        String errorString =
                "Error " + action + ": " + (error != null ? error.getMessage() : "Unknown error");
        if (config.getCallbacks() != null) {
            config.getCallbacks().say(ClineSay.ERROR, errorString, null, null, false, null);
        }

        return HandlerUtils.createTextBlocks(responseFormatter.toolError(errorString));
    }
}
