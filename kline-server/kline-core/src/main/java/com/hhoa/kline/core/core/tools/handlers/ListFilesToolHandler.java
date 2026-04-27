package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.specs.ListFilesTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

/**
 * 列出目录文件的工具处理器 支持递归列表、ClineIgnore 检查、遥测数据收集
 *
 * @author hhoa
 */
public class ListFilesToolHandler implements StateFullToolHandler {

    private static final int MAX_FILES = 200;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** ListFilesToolHandler 的阶段状态 */
    @Getter
    @Setter
    public static class ListFilesToolState extends ToolState {
        private String result;
        private TelemetryService.WorkspaceContext workspaceContext;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.LIST_FILES.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new ListFilesToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        String p = HandlerUtils.getStringParam(block, "path");
        return "[" + block.getName() + " for '" + (p == null ? "" : p) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ListFilesTool.create(ModelFamily.GENERIC);
    }

    @Override
    public boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        String path = HandlerUtils.getStringParam(block, "path");
        return context != null
                && context.getCallbacks() != null
                && Boolean.TRUE.equals(
                        context.getCallbacks().shouldAutoApproveToolWithPath(getName(), path));
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String relPath = HandlerUtils.getStringParam(block, "path");
        ToolContext config = ui.getContext();

        String recursiveRaw = HandlerUtils.getStringParam(block, "recursive");
        boolean recursive = recursiveRaw != null && recursiveRaw.equalsIgnoreCase("true");

        Map<String, Object> sharedMessageMap = new HashMap<>();
        sharedMessageMap.put("tool", recursive ? "listFilesRecursive" : "listFilesTopLevel");
        sharedMessageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), relPath));
        sharedMessageMap.put("content", "");
        sharedMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, config)));
        String sharedMessage = JsonUtils.toJsonString(sharedMessageMap);

        Boolean approve = ui.shouldAutoApproveToolWithPath(block.getName(), relPath);
        if (Boolean.TRUE.equals(approve)) {
            ui.say(
                    ClineSay.TOOL,
                    sharedMessage,
                    null,
                    null,
                    block.isPartial(),
                    ClineMessageFormat.JSON);
        } else {
            ui.ask(ClineAsk.TOOL, sharedMessage, block.isPartial(), ClineMessageFormat.JSON);
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String relDirPath = HandlerUtils.getStringParam(block, "path");
        String recursiveRaw = HandlerUtils.getStringParam(block, "recursive");
        boolean recursive = recursiveRaw != null && recursiveRaw.equalsIgnoreCase("true");

        // Check clineignore access before performing any IO.
        // Increment the counter so repeated attempts at blocked paths
        // accumulate toward the yolo-mode mistake limit.
        if (context.getServices().getClineIgnoreController() != null
                && !context.getServices().getClineIgnoreController().validateAccess(relDirPath)) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            context.getCallbacks()
                    .say(ClineSay.CLINEIGNORE_ERROR, relDirPath, null, null, false, null);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(formatResponse.clineIgnoreError(relDirPath)));
        }

        // Resolve the path and execute the list operation inside a single
        // try/catch so that failures (e.g. bad workspace hint, non-existent
        // directory) return a graceful tool error instead of crashing the task.
        String absolutePath;
        String displayPath;
        boolean usedWorkspaceHint;
        List<String> files;
        boolean didHitLimit;
        try {
            if (context.getWorkspaceManager() == null) {
                throw new IllegalStateException("workspaceManager 未配置，无法列出工作区文件");
            }
            WorkspaceConfig workspaceConfig = new WorkspaceConfig(context.getWorkspaceManager());
            WorkspaceResolver.WorkspacePathResult pathResult =
                    WorkspaceResolver.resolveWorkspacePath(
                            workspaceConfig, relDirPath, "ListFilesToolHandler.execute");

            absolutePath = pathResult.absolutePath();
            displayPath = pathResult.displayPath();
            usedWorkspaceHint = true;

            List<String> filesList = listFiles(absolutePath, recursive, MAX_FILES, context);
            if (filesList.size() >= MAX_FILES) {
                files = filesList.subList(0, MAX_FILES);
                didHitLimit = true;
            } else {
                files = filesList;
                didHitLimit = false;
            }
        } catch (Exception e) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorMessage = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("Error listing files: " + errorMessage));
        }

        // Only reset after all validations and the core operation succeed so
        // repeated failures accumulate toward the yolo-mode mistake limit.
        context.getTaskState().setConsecutiveMistakeCount(0);

        String fallbackAbsolutePath =
                Paths.get(context.getCwd())
                        .resolve(relDirPath != null ? relDirPath : "")
                        .toString();
        boolean resolvedToNonPrimary = !arePathsEqual(absolutePath, fallbackAbsolutePath);
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        usedWorkspaceHint,
                        resolvedToNonPrimary,
                        usedWorkspaceHint ? "hint" : "primary_fallback");

        String result =
                formatResponse.formatFilesList(
                        absolutePath,
                        files,
                        didHitLimit,
                        context.getServices().getClineIgnoreController());

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", recursive ? "listFilesRecursive" : "listFilesTopLevel");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(context.getCwd(), displayPath));
        completeMessageMap.put("content", result);
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relDirPath, context)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                context.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), relDirPath);
        if (Boolean.TRUE.equals(approve)) {
            context.getCallbacks()
                    .say(
                            ClineSay.TOOL,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);

            captureTelemetry(context, block, true, true, workspaceContext);

            return HandlerUtils.createToolExecuteResult(result);
        }

        // 需要 ask 用户 —— 保存状态并返回 PendingAsk
        String notificationMessage =
                "Cline wants to view directory "
                        + WorkspaceResolver.getWorkspaceBasename(
                                absolutePath, "ListFilesToolHandler.notification")
                        + "/";
        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                notificationMessage,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, message) -> {});

        ListFilesToolState state = (ListFilesToolState) context.getToolState();
        state.setPhase(1);
        state.setResult(result);
        state.setWorkspaceContext(workspaceContext);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.TOOL,
                        completeMessage,
                        context,
                        ClineMessageFormat.JSON,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        ListFilesToolState state = (ListFilesToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false, state.getWorkspaceContext());
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true, state.getWorkspaceContext());
        return HandlerUtils.createToolExecuteResult(state.getResult());
    }

    private void captureTelemetry(
            ToolContext context,
            ToolUse block,
            boolean autoApproved,
            boolean approved,
            TelemetryService.WorkspaceContext workspaceContext) {
        if (context.getServices() != null && context.getServices().getTelemetryService() != null) {
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

    private List<String> listFiles(
            String absolutePath, boolean recursive, int limit, ToolContext config) {
        List<String> files = new ArrayList<>();
        Path root = Paths.get(absolutePath);

        if (!Files.exists(root) || !Files.isDirectory(root)) {
            return files;
        }

        try (Stream<Path> stream = recursive ? Files.walk(root) : Files.list(root)) {
            files =
                    stream.filter(
                                    path -> {
                                        return Files.isRegularFile(path) || Files.isDirectory(path);
                                    })
                            .filter(
                                    file -> {
                                        if (config.getServices().getClineIgnoreController()
                                                != null) {
                                            String relPath = root.relativize(file).toString();
                                            return config.getServices()
                                                    .getClineIgnoreController()
                                                    .validateAccess(relPath);
                                        }
                                        return true;
                                    })
                            .limit(limit)
                            .sorted(Comparator.comparing(Path::toString))
                            .map(
                                    path -> {
                                        String absPath =
                                                path.toAbsolutePath().normalize().toString();
                                        if (Files.isDirectory(path)) {
                                            return absPath + "/";
                                        }
                                        return absPath;
                                    })
                            .collect(Collectors.toList());
        } catch (IOException ignored) {
        }
        return files;
    }

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
