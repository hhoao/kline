package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ListFilesTool;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
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

/**
 * 列出目录文件的工具处理器 支持递归列表、ClineIgnore 检查、遥测数据收集
 *
 * @author hhoa
 */
public class ListFilesToolHandler implements FullyManagedTool {

    private static final int MAX_FILES = 200;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.LIST_FILES.getValue();
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
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String relPath = HandlerUtils.getStringParam(block, "path");
        TaskConfig config = ui.getConfig();

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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String relDirPath = HandlerUtils.getStringParam(block, "path");
        String recursiveRaw = HandlerUtils.getStringParam(block, "recursive");
        boolean recursive = recursiveRaw != null && recursiveRaw.equalsIgnoreCase("true");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getWorkspaceManager() == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法列出工作区文件");
        }
        WorkspaceConfig workspaceConfig = new WorkspaceConfig(config.getWorkspaceManager());
        WorkspaceResolver.WorkspacePathResult pathResult =
                WorkspaceResolver.resolveWorkspacePath(
                        workspaceConfig, relDirPath, "ListFilesToolHandler.execute");

        String absolutePath = pathResult.absolutePath();
        String displayPath = pathResult.displayPath();

        String fallbackAbsolutePath =
                Paths.get(config.getCwd()).resolve(relDirPath != null ? relDirPath : "").toString();
        boolean resolvedToNonPrimary = !arePathsEqual(absolutePath, fallbackAbsolutePath);
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        true, // 多根路径结果表示使用了提示
                        resolvedToNonPrimary,
                        "hint");

        List<String> files;
        boolean didHitLimit;
        List<String> filesList = listFiles(absolutePath, recursive, MAX_FILES, config);
        if (filesList.size() >= MAX_FILES) {
            files = filesList.subList(0, MAX_FILES);
            didHitLimit = true;
        } else {
            files = filesList;
            didHitLimit = false;
        }

        String result =
                formatResponse.formatFilesList(
                        absolutePath,
                        files,
                        didHitLimit,
                        config.getServices().getClineIgnoreController());

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", recursive ? "listFilesRecursive" : "listFilesTopLevel");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), displayPath));
        completeMessageMap.put("content", result);
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relDirPath, config)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                config.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), relDirPath);
        if (Boolean.TRUE.equals(approve)) {
            config.getCallbacks()
                    .say(
                            ClineSay.TOOL,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);
            if (!config.isYoloModeToggled()) {
                config.getTaskState()
                        .setConsecutiveAutoApprovedRequestsCount(
                                config.getTaskState().getConsecutiveAutoApprovedRequestsCount()
                                        + 1);
            }

            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
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

            return HandlerUtils.createTextBlocks(result);
        } else {
            String notificationMessage =
                    "Cline wants to view directory "
                            + WorkspaceResolver.getWorkspaceBasename(
                                    absolutePath, "ListFilesToolHandler.notification")
                            + "/";
            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    notificationMessage,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, message) -> {});

            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.TOOL, completeMessage, config, ClineMessageFormat.JSON);
            if (!didApprove) {
                if (config.getServices() != null
                        && config.getServices().getTelemetryService() != null) {
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

            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
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

            return HandlerUtils.createTextBlocks(result);
        }
    }

    private List<String> listFiles(
            String absolutePath, boolean recursive, int limit, TaskConfig config) {
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
