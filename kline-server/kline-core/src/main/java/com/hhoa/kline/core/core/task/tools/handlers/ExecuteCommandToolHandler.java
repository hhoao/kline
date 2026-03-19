package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ExecuteCommandTool;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.AutoApproveToolResult;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspacePathAdapter;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 执行命令的工具处理器 支持命令执行、自动批准、遥测数据收集
 *
 * @author hhoa
 */
public class ExecuteCommandToolHandler implements StateFullToolHandler {

    private static final String COMMAND_REQ_APP_STRING = " (requires approval)";

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** ExecuteCommandToolHandler 的阶段状态 */
    @Getter
    @Setter
    public static class ExecuteCommandToolState extends ToolState {
        private String finalActualCommand;
        private String finalExecutionDir;
        private Integer timeoutSeconds;
        private boolean autoApproveSafe;
        private TelemetryService.WorkspaceContext workspaceContext;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.BASH.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new ExecuteCommandToolState();
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
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String rawCommand = HandlerUtils.getStringParam(block, "command");
        String requiresApprovalRaw = HandlerUtils.getStringParam(block, "requires_approval");
        String timeoutParam = HandlerUtils.getStringParam(block, "timeout");

        String command = rawCommand;
        if (context.getApi() != null
                && context.getApi().getModel() != null
                && context.getApi().getModel().getId() != null
                && context.getApi().getModel().getId().toLowerCase().contains("gemini")) {
            command = StringUtils.fixModelHtmlEscaping(command);
        }

        String executionDir = context.getCwd();
        String actualCommand = command;
        boolean workspaceHintUsed = false;
        String workspaceHint;

        if (context.getWorkspaceManager() != null) {
            Pattern commandMatch = Pattern.compile("^@(\\w+):(.+)$");
            Matcher matcher = commandMatch.matcher(command);

            if (matcher.find()) {
                workspaceHintUsed = true;
                workspaceHint = matcher.group(1);
                actualCommand = matcher.group(2).trim();

                WorkspaceConfig adapterConfig = new WorkspaceConfig(context.getWorkspaceManager());
                WorkspacePathAdapter adapter = new WorkspacePathAdapter(adapterConfig);

                executionDir = adapter.resolvePath(".", workspaceHint);

                command = actualCommand;
            }
        }

        final String finalExecutionDir = executionDir;
        final String finalActualCommand = actualCommand;

        if (context.getServices().getClineIgnoreController() != null) {
            String ignoredFileAttemptedToAccess =
                    context.getServices()
                            .getClineIgnoreController()
                            .validateCommand(finalActualCommand);
            if (ignoredFileAttemptedToAccess != null) {
                context.getCallbacks()
                        .say(
                                ClineSay.CLINEIGNORE_ERROR,
                                ignoredFileAttemptedToAccess,
                                null,
                                null,
                                false,
                                null);
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolError(
                                formatResponse.clineIgnoreError(ignoredFileAttemptedToAccess)));
            }
        }

        boolean requiresApproval = Boolean.parseBoolean(requiresApprovalRaw.toLowerCase());
        final Integer timeoutSeconds =
                computeTimeoutSeconds(context.isYoloModeToggled(), timeoutParam);

        boolean autoApproveSafe;
        boolean autoApproveAll;
        if (context.getAutoApprover() != null) {
            AutoApproveToolResult result =
                    context.getAutoApprover().shouldAutoApproveTool(ClineDefaultTool.BASH);
            autoApproveSafe = result.getFirst();
            autoApproveAll = result.isPair() && result.getSecond();
        } else {
            autoApproveSafe =
                    context.getAutoApprovalSettings() != null
                            ? context.getCallbacks().shouldAutoApproveTool(block.getName())
                            : false;
            autoApproveAll = false;
        }
        boolean shouldAutoRun =
                (!requiresApproval && autoApproveSafe)
                        || (requiresApproval && autoApproveSafe && autoApproveAll);

        boolean resolvedToNonPrimary = !arePathsEqual(finalExecutionDir, context.getCwd());
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        workspaceHintUsed,
                        resolvedToNonPrimary,
                        workspaceHintUsed ? "hint" : "primary_fallback");

        if (context.getWorkspaceManager() != null
                && context.getServices().getTelemetryService() != null) {
            context.getServices()
                    .getTelemetryService()
                    .captureWorkspacePathResolved(
                            context.getUlid(),
                            "ExecuteCommandToolHandler",
                            workspaceHintUsed ? "hint_provided" : "fallback_to_primary",
                            workspaceHintUsed ? "workspace_name" : null,
                            resolvedToNonPrimary,
                            null,
                            true);
        }

        AtomicBoolean didAutoApprove = new AtomicBoolean(false);

        if (shouldAutoRun) {
            context.getCallbacks()
                    .say(ClineSay.COMMAND, finalActualCommand, null, null, false, null);
            didAutoApprove.set(true);

            captureTelemetry(context, block, true, true, workspaceContext);

            ToolContext.ExecuteResult result =
                    doExecuteWithTimeoutNotification(
                            context,
                            finalActualCommand,
                            finalExecutionDir,
                            timeoutSeconds,
                            true,
                            didAutoApprove);
            return HandlerUtils.createToolExecuteResult(result.result);
        } else {
            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    "Cline wants to execute a command: " + finalActualCommand,
                    context.getAutoApprovalSettings() != null
                            && context.getAutoApprovalSettings().isEnabled(),
                    context.getAutoApprovalSettings() != null
                            && context.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, msg) -> {});

            String askMessage =
                    finalActualCommand + (autoApproveSafe ? COMMAND_REQ_APP_STRING : "");

            // 需要 ask 用户 —— 保存状态并返回 PendingAsk
            ExecuteCommandToolState state = (ExecuteCommandToolState) context.getToolState();
            state.setPhase(1);
            state.setFinalActualCommand(finalActualCommand);
            state.setFinalExecutionDir(finalExecutionDir);
            state.setTimeoutSeconds(timeoutSeconds);
            state.setAutoApproveSafe(autoApproveSafe);
            state.setWorkspaceContext(workspaceContext);

            var token =
                    ToolResultUtils.askApprovalAndPushFeedbackForToken(
                            ClineAsk.COMMAND,
                            askMessage,
                            context,
                            null,
                            block,
                            getDescription(block));
            return new ToolExecuteResult.PendingAsk(token);
        }
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        ExecuteCommandToolState state = (ExecuteCommandToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false, state.getWorkspaceContext());
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true, state.getWorkspaceContext());

        AtomicBoolean didAutoApprove = new AtomicBoolean(false);
        ToolContext.ExecuteResult result =
                doExecuteWithTimeoutNotification(
                        context,
                        state.getFinalActualCommand(),
                        state.getFinalExecutionDir(),
                        state.getTimeoutSeconds(),
                        false,
                        didAutoApprove);
        return HandlerUtils.createToolExecuteResult(result.result);
    }

    private void captureTelemetry(
            ToolContext context,
            ToolUse block,
            boolean autoApproved,
            boolean approved,
            TelemetryService.WorkspaceContext workspaceContext) {
        if (context.getServices().getTelemetryService() != null) {
            String modelId =
                    context.getApi() != null && context.getApi().getModel() != null
                            ? context.getApi().getModel().getId()
                            : "unknown";
            context.getServices()
                    .getTelemetryService()
                    .captureToolUsage(
                            context.getUlid(),
                            block.getName(),
                            modelId,
                            autoApproved,
                            approved,
                            workspaceContext);
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

    private static ToolContext.ExecuteResult doExecuteWithTimeoutNotification(
            ToolContext config,
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
            ToolContext.ExecuteResult result =
                    config.getCallbacks().executeCommandTool(finalCommand, timeoutSeconds);
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
