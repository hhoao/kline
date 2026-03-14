package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.FileContentExtractor;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ReadFileTool;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 读取文件的工具处理器
 *
 * @author hhoa
 */
public class ReadFileToolHandler implements FullyManagedTool {

    private static final int MAX_LINES_WITHOUT_RANGE = 500;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.FILE_READ.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String p = getStringParam(block, "path");
        return "[" + block.getName() + " for '" + (p == null ? "" : p) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ReadFileTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String partialPath = getStringParam(block, "path");
        TaskConfig config = ui.getConfig();

        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("tool", "readFile");
        messageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), partialPath));
        messageMap.put("content", null);
        messageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(partialPath, config)));
        String message = JsonUtils.toJsonString(messageMap);

        Boolean approve = ui.shouldAutoApproveToolWithPath(block.getName(), partialPath);
        if (Boolean.TRUE.equals(approve)) {
            ui.say(ClineSay.TOOL, message, null, null, block.isPartial(), ClineMessageFormat.JSON);
        } else {
            ui.ask(ClineAsk.TOOL, message, block.isPartial(), ClineMessageFormat.JSON);
        }
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String relPath = getStringParam(block, "path");

        ClineIgnoreController controller = config.getServices().getClineIgnoreController();
        if (controller != null && !controller.validateAccess(relPath)) {
            config.getCallbacks().say(ClineSay.CLINEIGNORE_ERROR, relPath, null, null, false, null);
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolError(formatResponse.clineIgnoreError(relPath)));
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        WorkspaceResolver.WorkspacePathResult pathResult =
                WorkspaceResolver.resolveWorkspacePath(
                        createWorkspaceConfig(config), relPath, "ReadFileToolHandler.execute");

        Path absolutePath = Paths.get(pathResult.absolutePath());
        String displayPath = pathResult.displayPath();

        Path fallbackAbsolutePath = Paths.get(config.getCwd(), relPath).normalize();
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        true, !absolutePath.equals(fallbackAbsolutePath), "hint");

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", "readFile");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), displayPath));
        completeMessageMap.put("content", absolutePath.toString());
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, config)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                config.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), relPath);
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

            boolean supportsImages = false;
            // TODO: 从 config.getApi().getModel().getInfo().supportsImages 获取

            Integer startLine = getIntParam(block, "start_line");
            Integer endLine = getIntParam(block, "end_line");

            FileContentExtractor.FileContentResult fileContent =
                    FileContentExtractor.extractFileContent(absolutePath, supportsImages);

            if (fileContent.imageBlock != null) {
                if (fileContent.imageBlock.source != null) {
                    ImageContentBlock imageContentBlock =
                            new ImageContentBlock(
                                    fileContent.imageBlock.source.data,
                                    fileContent.imageBlock.source.type,
                                    fileContent.imageBlock.source.mediaType);
                    config.getTaskState().getUserMessageContent().add(imageContentBlock);
                }
            }

            String content = fileContent.text != null ? fileContent.text : "[Image file]";

            if (startLine != null || endLine != null) {
                content = extractLineRange(content, startLine, endLine);
            } else {
                int lineCount = countLines(content);
                if (lineCount > MAX_LINES_WITHOUT_RANGE) {
                    String message =
                            String.format(
                                    "File has %d lines, which exceeds the maximum allowed (%d lines) for reading without specifying a line range.\n\n"
                                            + "Please use the start_line and end_line parameters to read specific sections of the file.\n\n"
                                            + "Example:\n"
                                            + "<read_file>\n"
                                            + "<path>%s</path>\n"
                                            + "<start_line>1</start_line>\n"
                                            + "<end_line>250</end_line>\n"
                                            + "</read_file>",
                                    lineCount, MAX_LINES_WITHOUT_RANGE, displayPath);
                    return HandlerUtils.createTextBlocks(message);
                }
            }

            if (config.getServices() != null
                    && config.getServices().getFileContextTracker() != null) {
                config.getServices()
                        .getFileContextTracker()
                        .trackFileContext(relPath, "read_tool")
                        .join();
            }
            return HandlerUtils.createTextBlocks(content);
        } else {
            String notificationMessage =
                    "Cline wants to read "
                            + WorkspaceResolver.getWorkspaceBasename(
                                    absolutePath.toString(), "ReadFileToolHandler.notification");
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

            boolean supportsImages = false;
            // TODO: 从 config.getApi().getModel().getInfo().supportsImages 获取

            Integer startLine = getIntParam(block, "start_line");
            Integer endLine = getIntParam(block, "end_line");

            FileContentExtractor.FileContentResult fileContent =
                    FileContentExtractor.extractFileContent(absolutePath, supportsImages);

            if (fileContent.imageBlock != null) {
                if (fileContent.imageBlock.source != null) {
                    ImageContentBlock imageContentBlock =
                            new ImageContentBlock(
                                    fileContent.imageBlock.source.data,
                                    fileContent.imageBlock.source.type,
                                    fileContent.imageBlock.source.mediaType);
                    config.getTaskState().getUserMessageContent().add(imageContentBlock);
                }
            }

            String content = fileContent.text != null ? fileContent.text : "[Image file]";

            if (startLine != null || endLine != null) {
                content = extractLineRange(content, startLine, endLine);
            } else {
                int lineCount = countLines(content);
                if (lineCount > MAX_LINES_WITHOUT_RANGE) {
                    String message =
                            String.format(
                                    "File has %d lines, which exceeds the maximum allowed (%d lines) for reading without specifying a line range.\n\n"
                                            + "Please use the start_line and end_line parameters to read specific sections of the file.\n\n"
                                            + "Example:\n"
                                            + "<read_file>\n"
                                            + "<path>%s</path>\n"
                                            + "<start_line>1</start_line>\n"
                                            + "<end_line>250</end_line>\n"
                                            + "</read_file>",
                                    lineCount, MAX_LINES_WITHOUT_RANGE, displayPath);
                    return HandlerUtils.createTextBlocks(message);
                }
            }

            if (config.getServices() != null
                    && config.getServices().getFileContextTracker() != null) {
                config.getServices()
                        .getFileContextTracker()
                        .trackFileContext(relPath, "read_tool")
                        .join();
            }
            return HandlerUtils.createTextBlocks(content);
        }
    }

    private static WorkspaceConfig createWorkspaceConfig(TaskConfig config) {
        if (config.getWorkspaceManager() == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法读取文件");
        }
        return new WorkspaceConfig(config.getWorkspaceManager());
    }

    private static String getStringParam(ToolUse block, String key) {
        if (block.getParams() == null) {
            return null;
        }
        Object v = block.getParams().get(key);
        return v == null ? null : String.valueOf(v);
    }

    private static Integer getIntParam(ToolUse block, String key) {
        String strValue = getStringParam(block, key);
        if (strValue == null || strValue.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(strValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String extractLineRange(String content, Integer startLine, Integer endLine) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        if (startLine == null && endLine == null) {
            return content;
        }

        String[] lines = content.split("\n", -1);
        int totalLines = lines.length;

        int start = startLine != null && startLine > 0 ? startLine - 1 : 0;
        int end = endLine != null && endLine > 0 ? endLine - 1 : totalLines - 1;

        if (end >= totalLines) {
            end = totalLines - 1;
        }
        if (start > end) {
            return String.format(
                    "[Error: Invalid line range. File has %d lines, but requested range is %d-%d]",
                    totalLines,
                    startLine != null ? startLine : 1,
                    endLine != null ? endLine : totalLines);
        }

        StringBuilder result = new StringBuilder();
        for (int i = start; i <= end; i++) {
            result.append(lines[i]);
            if (i < end) {
                result.append('\n');
            }
        }

        if (start > 0 || end < totalLines - 1) {
            String header =
                    String.format(
                            "[Showing lines %d-%d of %d total lines]\n",
                            start + 1, end + 1, totalLines);
            return header + result.toString();
        }

        return result.toString();
    }

    private static int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        String[] lines = content.split("\n", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty() && content.endsWith("\n")) {
            return lines.length - 1;
        }
        return lines.length;
    }
}
