package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ExecuteCommandTool;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.AutoApproveToolResult;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspacePathAdapter;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 执行命令的工具处理器 支持命令执行、自动批准、遥测数据收集
 *
 * @author hhoa
 */
public class ExecuteCommandToolHandler implements FullyManagedTool {

    private static final String COMMAND_REQ_APP_STRING = " (requires approval)";

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.BASH.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String cmd = HandlerUtils.getStringParam(block, "command");
        return "[" + block.getName() + " for '" + (cmd == null ? "" : cmd) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ExecuteCommandTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String command = HandlerUtils.getStringParam(block, "command");
        Boolean shouldAutoApprove = ui.shouldAutoApproveTool(ClineDefaultTool.BASH.getValue());
        if (shouldAutoApprove) {
            ui.ask(ClineAsk.COMMAND, command, block.isPartial(), null);
        }
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String rawCommand = HandlerUtils.getStringParam(block, "command");
        String requiresApprovalRaw = HandlerUtils.getStringParam(block, "requires_approval");
        String timeoutParam = HandlerUtils.getStringParam(block, "timeout");

        config.getTaskState().setConsecutiveMistakeCount(0);

        String command = rawCommand;
        if (config.getApi() != null
                && config.getApi().getModel() != null
                && config.getApi().getModel().getId() != null
                && config.getApi().getModel().getId().toLowerCase().contains("gemini")) {
            command = StringUtils.fixModelHtmlEscaping(command);
        }

        String executionDir = config.getCwd();
        String actualCommand = command;
        boolean workspaceHintUsed = false;
        String workspaceHint;

        if (config.getWorkspaceManager() != null) {
            Pattern commandMatch = Pattern.compile("^@(\\w+):(.+)$");
            Matcher matcher = commandMatch.matcher(command);

            if (matcher.find()) {
                workspaceHintUsed = true;
                workspaceHint = matcher.group(1);
                actualCommand = matcher.group(2).trim();

                WorkspaceConfig adapterConfig = new WorkspaceConfig(config.getWorkspaceManager());
                WorkspacePathAdapter adapter = new WorkspacePathAdapter(adapterConfig);

                executionDir = adapter.resolvePath(".", workspaceHint);

                command = actualCommand;
            }
        }

        final String finalExecutionDir = executionDir;
        final String finalActualCommand = actualCommand;

        if (config.getServices().getClineIgnoreController() != null) {
            String ignoredFileAttemptedToAccess =
                    config.getServices()
                            .getClineIgnoreController()
                            .validateCommand(finalActualCommand);
            if (ignoredFileAttemptedToAccess != null) {
                config.getCallbacks()
                        .say(
                                ClineSay.CLINEIGNORE_ERROR,
                                ignoredFileAttemptedToAccess,
                                null,
                                null,
                                false,
                                null);
                return HandlerUtils.createTextBlocks(
                        formatResponse.toolError(
                                formatResponse.clineIgnoreError(ignoredFileAttemptedToAccess)));
            }
        }

        boolean requiresApproval = Boolean.parseBoolean(requiresApprovalRaw.toLowerCase());
        final Integer timeoutSeconds =
                computeTimeoutSeconds(config.isYoloModeToggled(), timeoutParam);

        boolean autoApproveSafe;
        boolean autoApproveAll;
        if (config.getAutoApprover() != null) {
            AutoApproveToolResult result =
                    config.getAutoApprover().shouldAutoApproveTool(ClineDefaultTool.BASH);
            autoApproveSafe = result.getFirst();
            autoApproveAll = result.isPair() && result.getSecond();
        } else {
            autoApproveSafe =
                    config.getAutoApprovalSettings() != null
                            ? config.getCallbacks().shouldAutoApproveTool(block.getName())
                            : false;
            autoApproveAll = false;
        }
        boolean shouldAutoRun =
                (!requiresApproval && autoApproveSafe)
                        || (requiresApproval && autoApproveSafe && autoApproveAll);

        boolean resolvedToNonPrimary = !arePathsEqual(finalExecutionDir, config.getCwd());
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        workspaceHintUsed,
                        resolvedToNonPrimary,
                        workspaceHintUsed ? "hint" : "primary_fallback");

        if (config.getWorkspaceManager() != null
                && config.getServices().getTelemetryService() != null) {
            config.getServices()
                    .getTelemetryService()
                    .captureWorkspacePathResolved(
                            config.getUlid(),
                            "ExecuteCommandToolHandler",
                            workspaceHintUsed ? "hint_provided" : "fallback_to_primary",
                            workspaceHintUsed ? "workspace_name" : null,
                            resolvedToNonPrimary,
                            null,
                            true);
        }

        AtomicBoolean didAutoApprove = new AtomicBoolean(false);

        if (shouldAutoRun) {
            config.getCallbacks()
                    .say(ClineSay.COMMAND, finalActualCommand, null, null, false, null);
            if (!config.isYoloModeToggled()) {
                config.getTaskState()
                        .setConsecutiveAutoApprovedRequestsCount(
                                config.getTaskState().getConsecutiveAutoApprovedRequestsCount()
                                        + 1);
            }
            didAutoApprove.set(true);

            if (config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(
                                config.getUlid(),
                                block.getName(),
                                modelId,
                                true,
                                true,
                                workspaceContext);
            }

            TaskConfig.ExecuteResult result =
                    doExecuteWithTimeoutNotification(
                            config,
                            finalActualCommand,
                            finalExecutionDir,
                            timeoutSeconds,
                            true,
                            didAutoApprove);
            return HandlerUtils.createTextBlocks(result.result);
        } else {
            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    "Cline wants to execute a command: " + finalActualCommand,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, msg) -> {});

            String askMessage =
                    finalActualCommand + (autoApproveSafe ? COMMAND_REQ_APP_STRING : "");
            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.COMMAND, askMessage, config, null);
            if (!didApprove) {
                if (config.getServices().getTelemetryService() != null) {
                    String modelId =
                            config.getApi() != null && config.getApi().getModel() != null
                                    ? config.getApi().getModel().getId()
                                    : "unknown";
                    config.getServices()
                            .getTelemetryService()
                            .captureToolUsage(
                                    config.getUlid(),
                                    block.getName(),
                                    modelId,
                                    false,
                                    false,
                                    workspaceContext);
                }
                return HandlerUtils.createTextBlocks(formatResponse.toolDenied());
            }

            if (config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(
                                config.getUlid(),
                                block.getName(),
                                modelId,
                                false,
                                true,
                                workspaceContext);
            }

            TaskConfig.ExecuteResult result =
                    doExecuteWithTimeoutNotification(
                            config,
                            finalActualCommand,
                            finalExecutionDir,
                            timeoutSeconds,
                            false,
                            didAutoApprove);
            return HandlerUtils.createTextBlocks(result.result);
        }
    }

    private static Integer computeTimeoutSeconds(Boolean yoloModeToggled, String timeoutParam) {
        if (!Boolean.TRUE.equals(yoloModeToggled)) return null;
        if (timeoutParam == null) return 30;
        try {
            int parsed = Integer.parseInt(timeoutParam);
            return parsed <= 0 ? 30 : parsed;
        } catch (NumberFormatException e) {
            return 30;
        }
    }

    private static TaskConfig.ExecuteResult doExecuteWithTimeoutNotification(
            TaskConfig config,
            String actualCommand,
            String executionDir,
            Integer timeoutSeconds,
            boolean autoApproved,
            AtomicBoolean didAutoApprove) {
        Timer timer = new Timer("auto-approved-command-timer", true);
        if (didAutoApprove.get()
                && config.getAutoApprovalSettings() != null
                && config.getAutoApprovalSettings().isEnableNotifications()) {
            timer.schedule(
                    new TimerTask() {
                        @Override
                        public void run() {}
                    },
                    30_000L);
        }

        String finalCommand = actualCommand;
        if (!executionDir.equals(config.getCwd())) {
            finalCommand = String.format("cd \"%s\" && %s", executionDir, actualCommand);
        }

        try {
            TaskConfig.ExecuteResult result =
                    config.getCallbacks().executeCommandTool(finalCommand, timeoutSeconds);
            if (result.userRejected) {
                config.getTaskState().setDidRejectTool(true);
            }
            timer.cancel();
            return result;
        } catch (Exception e) {
            timer.cancel();
            throw e;
        }
    }

    /**
     * 检查两个路径是否相等（规范化后比较）
     *
     * @param path1 路径1
     * @param path2 路径2
     * @return 如果路径相等则返回 true
     */
    private static boolean arePathsEqual(String path1, String path2) {
        if (path1 == null && path2 == null) {
            return true;
        }
        if (path1 == null || path2 == null) {
            return false;
        }
        try {
            Path p1 = Paths.get(path1).normalize().toAbsolutePath();
            Path p2 = Paths.get(path2).normalize().toAbsolutePath();
            return p1.equals(p2);
        } catch (Exception e) {
            return path1.equals(path2);
        }
    }
}
