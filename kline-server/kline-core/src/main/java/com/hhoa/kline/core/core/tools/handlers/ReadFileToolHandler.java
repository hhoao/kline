package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ImageContentBlock;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.FileContentExtractor;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskState;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.specs.ReadFileTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
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
        try {
            if (context.getApi() != null
                    && context.getApi().getModel() != null
                    && context.getApi().getModel().getInfo() != null
                    && Boolean.TRUE.equals(
                            context.getApi().getModel().getInfo().getSupportsImages())) {
                supportsImages = true;
            }
        } catch (Exception ignored) {
            // fallback to false
        }

        Integer startLine = getIntParam(block, "start_line");
        Integer endLine = getIntParam(block, "end_line");

        // === File Read Deduplication ===
        // Check if we've already read this exact file in this task.
        String cacheKey = absolutePathStr.toLowerCase();
        var cached = context.getTaskState().getFileReadCache().get(cacheKey);

        if (cached != null) {
            // Check if file has been modified externally by comparing mtime
            try {
                long currentMtime =
                        java.nio.file.Files.getLastModifiedTime(absolutePath).toMillis();
                if (currentMtime != cached.getMtime()) {
                    // File was modified — evict cache entry
                    context.getTaskState().getFileReadCache().remove(cacheKey);
                    cached = null;
                }
            } catch (Exception e) {
                context.getTaskState().getFileReadCache().remove(cacheKey);
                cached = null;
            }
        }

        // Re-check after possible mtime eviction
        var validCached = context.getTaskState().getFileReadCache().get(cacheKey);

        if (validCached != null) {
            validCached.setReadCount(validCached.getReadCount() + 1);

            // Re-push image block for multimodal models
            if (validCached.getImageBlock() != null
                    && context.getTaskState().getNextUserMessageContent() != null) {
                context.getTaskState().getNextUserMessageContent().add(validCached.getImageBlock());
            }

            // Re-read from disk (cache doesn't store content to save memory)
            FileContentExtractor.FileContentResult fileContent;
            try {
                fileContent = FileContentExtractor.extractFileContent(absolutePath, supportsImages);
            } catch (Exception e) {
                context.getTaskState()
                        .setConsecutiveMistakeCount(
                                context.getTaskState().getConsecutiveMistakeCount() + 1);
                String errorMessage = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
                String normalizedMessage =
                        errorMessage.startsWith("Error reading file:")
                                ? errorMessage
                                : "Error reading file: " + errorMessage;
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolError(normalizedMessage));
            }

            String content = fileContent.text != null ? fileContent.text : "[Image file]";
            if (startLine != null || endLine != null) {
                content = extractLineRange(content, startLine, endLine);
            }

            if (validCached.getReadCount() >= 3) {
                return HandlerUtils.createToolExecuteResult(
                        "[DUPLICATE READ] You have already read '"
                                + displayPath
                                + "' "
                                + validCached.getReadCount()
                                + " times in this conversation. The content has not changed since your last read. Please use the information you already have and proceed with your task.\n\n"
                                + content);
            }

            return HandlerUtils.createToolExecuteResult(
                    "[File already read] The file '"
                            + displayPath
                            + "' was already read earlier in this conversation. Returning content:\n"
                            + content);
        }

        // Execute the actual file read operation
        FileContentExtractor.FileContentResult fileContent;
        try {
            fileContent = FileContentExtractor.extractFileContent(absolutePath, supportsImages);
        } catch (Exception e) {
            // Return a graceful tool error instead of crashing
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorMessage = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
            String normalizedMessage =
                    errorMessage.startsWith("Error reading file:")
                            ? errorMessage
                            : "Error reading file: " + errorMessage;
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(normalizedMessage));
        }

        // Only reset mistake count after a successful read
        context.getTaskState().setConsecutiveMistakeCount(0);

        // Track file read operation
        if (context.getServices() != null
                && context.getServices().getFileContextTracker() != null) {
            context.getServices()
                    .getFileContextTracker()
                    .trackFileContext(relPath, "read_tool")
                    .join();
        }

        // Cache metadata for deduplication (no content stored — saves memory)
        long mtime = 0;
        try {
            mtime = java.nio.file.Files.getLastModifiedTime(absolutePath).toMillis();
        } catch (Exception e) {
            // If stat fails, use 0 — next cache hit will evict due to mtime mismatch
        }

        ImageContentBlock imageBlock = null;
        if (fileContent.imageBlock != null && fileContent.imageBlock.source != null) {
            imageBlock =
                    new ImageContentBlock(
                            fileContent.imageBlock.source.data,
                            fileContent.imageBlock.source.type,
                            fileContent.imageBlock.source.mediaType);
        }
        context.getTaskState()
                .getFileReadCache()
                .put(cacheKey, new TaskState.FileReadCacheEntry(1, mtime, imageBlock));

        // Handle image blocks — push to userMessageContent
        if (imageBlock != null && context.getTaskState().getNextUserMessageContent() != null) {
            context.getTaskState().getNextUserMessageContent().add(imageBlock);
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
