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
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * 读取文件的工具处理器
 *
 * @author hhoa
 */
public class ReadFileToolHandler implements StateFullToolHandler {

    private static final int MAX_LINES_WITHOUT_RANGE = 500;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** ReadFileToolHandler 的阶段状态 */
    @Getter
    @Setter
    public static class ReadFileToolState extends ToolState {
        private String relPath;
        private String absolutePath;
        private String displayPath;
        private TelemetryService.WorkspaceContext workspaceContext;
    }

    private static WorkspaceConfig createWorkspaceConfig(ToolContext config) {
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

    @Override
    public String getName() {
        return ClineDefaultTool.FILE_READ.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new ReadFileToolState();
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
        ToolContext config = ui.getContext();

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
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String relPath = getStringParam(block, "path");

        ClineIgnoreController controller = context.getServices().getClineIgnoreController();
        if (controller != null && !controller.validateAccess(relPath)) {
            context.getCallbacks()
                    .say(ClineSay.CLINEIGNORE_ERROR, relPath, null, null, false, null);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(formatResponse.clineIgnoreError(relPath)));
        }

        WorkspaceResolver.WorkspacePathResult pathResult =
                WorkspaceResolver.resolveWorkspacePath(
                        createWorkspaceConfig(context), relPath, "ReadFileToolHandler.execute");

        Path absolutePath = Paths.get(pathResult.absolutePath());
        String displayPath = pathResult.displayPath();

        Path fallbackAbsolutePath = Paths.get(context.getCwd(), relPath).normalize();
        TelemetryService.WorkspaceContext workspaceContext =
                new TelemetryService.WorkspaceContext(
                        true, !absolutePath.equals(fallbackAbsolutePath), "hint");

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", "readFile");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(context.getCwd(), displayPath));
        completeMessageMap.put("content", absolutePath.toString());
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, context)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                context.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), relPath);
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
            return executeReadFile(context, block, relPath, absolutePath.toString(), displayPath);
        }

        // 需要 ask 用户 —— 保存状态并返回 PendingAsk
        String notificationMessage =
                "Cline wants to read "
                        + WorkspaceResolver.getWorkspaceBasename(
                                absolutePath.toString(), "ReadFileToolHandler.notification");
        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                notificationMessage,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, message) -> {});

        ReadFileToolState state = (ReadFileToolState) context.getToolState();
        state.setPhase(1);
        state.setRelPath(relPath);
        state.setAbsolutePath(absolutePath.toString());
        state.setDisplayPath(displayPath);
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
        ReadFileToolState state = (ReadFileToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false, state.getWorkspaceContext());
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true, state.getWorkspaceContext());
        return executeReadFile(
                context,
                block,
                state.getRelPath(),
                state.getAbsolutePath(),
                state.getDisplayPath());
    }

    private ToolExecuteResult executeReadFile(
            ToolContext context,
            ToolUse block,
            String relPath,
            String absolutePathStr,
            String displayPath) {
        Path absolutePath = Paths.get(absolutePathStr);
        boolean supportsImages = false;

        Integer startLine = getIntParam(block, "start_line");
        Integer endLine = getIntParam(block, "end_line");

        FileContentExtractor.FileContentResult fileContent =
                FileContentExtractor.extractFileContent(absolutePath, supportsImages);

        List<UserContentBlock> userContentBlocks = new ArrayList<>();

        if (fileContent.imageBlock != null) {
            if (fileContent.imageBlock.source != null) {
                ImageContentBlock imageContentBlock =
                        new ImageContentBlock(
                                fileContent.imageBlock.source.data,
                                fileContent.imageBlock.source.type,
                                fileContent.imageBlock.source.mediaType);
                userContentBlocks.add(imageContentBlock);
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
                content = message;
            }
        }

        if (context.getServices() != null
                && context.getServices().getFileContextTracker() != null) {
            context.getServices()
                    .getFileContextTracker()
                    .trackFileContext(relPath, "read_tool")
                    .join();
        }
        return HandlerUtils.createToolExecuteResult(content);
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
}
