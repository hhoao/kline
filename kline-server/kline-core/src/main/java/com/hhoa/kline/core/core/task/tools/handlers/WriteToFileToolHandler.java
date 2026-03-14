package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.DiffProcessor;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;

/**
 * 写入/替换文件工具处理器 支持 write_to_file / replace_in_file / new_rule 三类操作
 *
 * @author hhoa
 */
public class WriteToFileToolHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    private static String getStringParam(ToolUse block, String key) {
        return HandlerUtils.getStringParam(block, key);
    }

    private static String getToolDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + getStringParam(block, "path") + "']";
    }

    private static String buildToolMessage(String tool, String path, String content) {
        return buildJsonMessage(tool, path, content, "true");
    }

    private static String buildUserFeedbackMessage(String tool, String path, String diff) {
        return """
            {"tool":"%s","path":"%s","diff":"%s"}"""
                .formatted(tool, StringUtils.escapeJson(path), StringUtils.escapeJson(diff));
    }

    private static String buildJsonMessage(
            String tool, String path, String content, String operationIsLocatedInWorkspace) {
        return MessageFormat.format(
                """
            '{'"tool":"{0}","path":"{1}","content":"{2}","operationIsLocatedInWorkspace":{3}'}'""",
                tool,
                StringUtils.escapeJson(path),
                StringUtils.escapeJson(content),
                operationIsLocatedInWorkspace != null ? operationIsLocatedInWorkspace : "true");
    }

    private static String getModelId(TaskConfig config) {
        return config.getApi() != null && config.getApi().getModel() != null
                ? config.getApi().getModel().getId()
                : "unknown";
    }

    private static boolean determineFileExists(DiffViewProvider dvp, Path absolutePath) {
        if (dvp != null && dvp.getEditType() != null) {
            return dvp.getEditType() == DiffViewProvider.EditType.MODIFY;
        }

        boolean exists = Files.exists(absolutePath);
        if (dvp != null) {
            dvp.setEditType(
                    exists ? DiffViewProvider.EditType.MODIFY : DiffViewProvider.EditType.CREATE);
        }
        return exists;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.FILE_NEW.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + getStringParam(block, "path") + "']";
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String rawRelPath = getStringParam(block, "path");
        String rawContent = getStringParam(block, "content");
        String rawDiff = getStringParam(block, "diff");

        if (rawRelPath == null || (rawContent == null && rawDiff == null)) {
            return;
        }

        TaskConfig config = ui.getConfig();
        try {
            FileOperationResult result =
                    validateAndPrepareFileOperation(config, block, rawRelPath, rawDiff, rawContent);
            if (result == null) {
                return;
            }

            String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
            String contentValue = result.diff != null ? result.diff : result.content;
            String msg = buildToolMessage(tool, result.relPath, contentValue);

            Boolean approve = ui.shouldAutoApproveToolWithPath(block.getName(), result.relPath);

            if (Boolean.TRUE.equals(approve)) {
                ui.say(ClineSay.TOOL, msg, null, null, block.isPartial(), ClineMessageFormat.JSON);
            } else {
                ui.ask(ClineAsk.TOOL, msg, block.isPartial(), ClineMessageFormat.JSON);
            }
        } catch (Exception error) {
            DiffViewProvider dvp = config.getServices().getDiffViewProvider();
            if (dvp != null) {
                try {
                    dvp.revertChanges();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    dvp.reset();
                } catch (Exception e) {
                }
            }
            throw new RuntimeException(error);
        }
    }

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String rawRelPath = getStringParam(block, "path");
        String rawContent = getStringParam(block, "content");
        String rawDiff = getStringParam(block, "diff");

        if ("replace_in_file".equals(block.getName()) && StringUtils.isBlank(rawDiff)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            if (config.getServices().getDiffViewProvider() != null) {
                config.getServices().getDiffViewProvider().reset();
            }
            String errorResult =
                    config.getCallbacks().sayAndCreateMissingParamError(block.getName(), "diff");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        if (("write_to_file".equals(block.getName()) || "new_rule".equals(block.getName()))
                && StringUtils.isBlank(rawContent)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            if (config.getServices().getDiffViewProvider() != null) {
                config.getServices().getDiffViewProvider().reset();
            }
            String errorResult =
                    config.getCallbacks().sayAndCreateMissingParamError(block.getName(), "content");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        try {
            FileOperationResult result =
                    validateAndPrepareFileOperation(config, block, rawRelPath, rawDiff, rawContent);
            if (result == null) {
                return HandlerUtils.createTextBlocks("");
            }
            return executeFileOperation(config, block, result);
        } catch (Exception e) {
            if (config.getServices().getDiffViewProvider() != null) {
                try {
                    config.getServices().getDiffViewProvider().revertChanges();
                    config.getServices().getDiffViewProvider().reset();
                } catch (Exception ex) {
                }
            }
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolError("File operation failed: " + e.getMessage()));
        }
    }

    private FileOperationResult validateAndPrepareFileOperation(
            TaskConfig config,
            ToolUse block,
            String rawRelPath,
            String rawDiff,
            String rawContent) {
        try {
            if (config.getWorkspaceManager() == null) {
                throw new IllegalStateException("workspaceManager 未配置，无法写入文件");
            }
            WorkspaceConfig workspaceConfig = new WorkspaceConfig(config.getWorkspaceManager());
            WorkspaceResolver.WorkspacePathResult pathResult =
                    WorkspaceResolver.resolveWorkspacePath(
                            workspaceConfig,
                            rawRelPath,
                            "WriteToFileToolHandler.validateAndPrepareFileOperation");

            Path absolutePath = Paths.get(pathResult.absolutePath());
            String resolvedPath = pathResult.resolvedPath();

            Path fallbackAbsolutePath = Paths.get(config.getCwd(), rawRelPath).normalize();
            boolean usedHint = true;
            TelemetryService.WorkspaceContext workspaceContext =
                    new TelemetryService.WorkspaceContext(
                            usedHint, !absolutePath.equals(fallbackAbsolutePath), "hint");

            ClineIgnoreController controller = config.getServices().getClineIgnoreController();
            if (controller != null && !controller.validateAccess(resolvedPath)) {
                config.getCallbacks()
                        .say(ClineSay.CLINEIGNORE_ERROR, resolvedPath, null, null, false, null);

                String errorResponse =
                        formatResponse.toolError(formatResponse.clineIgnoreError(resolvedPath));
                ToolResultUtils.pushToolResult(
                        errorResponse,
                        block,
                        config.getTaskState().getUserMessageContent(),
                        WriteToFileToolHandler::getToolDescription,
                        () -> {
                            config.getTaskState().setDidAlreadyUseTool(true);
                        },
                        config.getCoordinator());
                return null;
            }

            DiffViewProvider dvp = config.getServices().getDiffViewProvider();
            boolean fileExists = determineFileExists(dvp, absolutePath);

            String newContent;

            String diff = rawDiff;

            if (diff != null) {
                if (!config.getApi().getModel().getId().contains("claude")) {
                    diff = StringUtils.fixModelHtmlEscaping(diff);
                    diff = StringUtils.removeInvalidChars(diff);
                }

                if (!config.getServices().getDiffViewProvider().isEditing()) {
                    TaskConfig.OpenOptions opts = new TaskConfig.OpenOptions();
                    opts.displayPath = rawRelPath;
                    try {
                        config.getServices()
                                .getDiffViewProvider()
                                .open(absolutePath.toString(), opts.displayPath);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to open diff editor", e);
                    }
                }

                try {
                    String originalContent =
                            dvp != null && dvp.getOriginalContent() != null
                                    ? dvp.getOriginalContent()
                                    : (fileExists ? Files.readString(absolutePath) : "");
                    newContent =
                            new DiffProcessor()
                                    .constructNewFileContent(
                                            diff, originalContent, !block.isPartial());
                } catch (Exception error) {
                    config.getCallbacks()
                            .say(ClineSay.DIFF_ERROR, resolvedPath, null, null, false, null);

                    String errorType =
                            error.getMessage() != null
                                            && error.getMessage()
                                                    .contains("does not match anything")
                                    ? "search_not_found"
                                    : "other_diff_error";

                    TelemetryService telemetry = config.getServices().getTelemetryService();
                    if (telemetry != null) {
                        telemetry.captureDiffEditFailure(
                                config.getUlid(), getModelId(config), errorType);
                    }

                    String errorResponse =
                            formatResponse.toolError(
                                    (error.getMessage() != null ? error.getMessage() : "")
                                            + "\n\n"
                                            + formatResponse.diffError(
                                                    resolvedPath,
                                                    dvp != null && dvp.getOriginalContent() != null
                                                            ? dvp.getOriginalContent()
                                                            : ""));
                    ToolResultUtils.pushToolResult(
                            errorResponse,
                            block,
                            config.getTaskState().getUserMessageContent(),
                            WriteToFileToolHandler::getToolDescription,
                            () -> {
                                config.getTaskState().setDidAlreadyUseTool(true);
                            },
                            config.getCoordinator());

                    if (dvp != null) {
                        try {
                            dvp.revertChanges();
                            dvp.reset();
                        } catch (Exception ex) {
                        }
                    }

                    return null;
                }
            } else if (rawContent != null) {
                newContent = rawContent;

                if (newContent.startsWith("```")) {
                    String[] lines = newContent.split("\n");
                    if (lines.length > 1) {
                        newContent =
                                String.join("\n", Arrays.copyOfRange(lines, 1, lines.length))
                                        .trim();
                    }
                }
                if (newContent.endsWith("```")) {
                    String[] lines = newContent.split("\n");
                    if (lines.length > 1) {
                        newContent =
                                String.join("\n", Arrays.copyOfRange(lines, 0, lines.length - 1))
                                        .trim();
                    }
                }

                if (!config.getApi().getModel().getId().contains("claude")) {
                    newContent = StringUtils.fixModelHtmlEscaping(newContent);
                    newContent = StringUtils.removeInvalidChars(newContent);
                }
            } else {
                return null;
            }

            newContent = StringUtils.trimEnd(newContent);

            return new FileOperationResult(
                    rawRelPath,
                    absolutePath,
                    fileExists,
                    diff,
                    rawContent,
                    newContent,
                    workspaceContext);
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare file operation: " + e.getMessage(), e);
        }
    }

    private List<UserContentBlock> executeFileOperation(
            TaskConfig config, ToolUse block, FileOperationResult result) {

        String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
        String contentValue = result.diff != null ? result.diff : result.content;
        String completeMessage = buildToolMessage(tool, result.relPath, contentValue);

        Boolean approve =
                config.getCallbacks()
                        .shouldAutoApproveToolWithPath(block.getName(), result.relPath);
        if (Boolean.TRUE.equals(approve)) {
            return executeAutoApprovedFileOperation(config, block, result, completeMessage);
        } else {
            return executeManualApprovedFileOperation(config, block, result, completeMessage);
        }
    }

    private List<UserContentBlock> executeAutoApprovedFileOperation(
            TaskConfig config, ToolUse block, FileOperationResult result, String completeMessage) {

        config.getCallbacks()
                .say(ClineSay.TOOL, completeMessage, null, null, false, ClineMessageFormat.JSON);
        if (!config.isYoloModeToggled()) {
            config.getTaskState()
                    .setConsecutiveAutoApprovedRequestsCount(
                            config.getTaskState().getConsecutiveAutoApprovedRequestsCount() + 1);
        }

        TelemetryService telemetry = config.getServices().getTelemetryService();
        if (telemetry != null) {
            telemetry.captureToolUsage(
                    config.getUlid(),
                    block.getName(),
                    getModelId(config),
                    true,
                    true,
                    result.workspaceContext);
        }

        List<UserContentBlock> saveResult = performFileSave(config, block, result);
        try {
            Thread.sleep(3_500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return saveResult;
    }

    private List<UserContentBlock> executeManualApprovedFileOperation(
            TaskConfig config, ToolUse block, FileOperationResult result, String completeMessage) {

        Boolean didApprove =
                ToolResultUtils.askApprovalAndPushFeedback(
                        ClineAsk.TOOL, completeMessage, config, ClineMessageFormat.JSON);
        String modelId = getModelId(config);
        TelemetryService telemetry = config.getServices().getTelemetryService();

        if (!didApprove) {
            if (telemetry != null) {
                telemetry.captureToolUsage(
                        config.getUlid(),
                        block.getName(),
                        modelId,
                        false,
                        false,
                        result.workspaceContext);
            }

            DiffViewProvider dvp = config.getServices().getDiffViewProvider();
            if (dvp != null) {
                try {
                    dvp.revertChanges();
                } catch (IOException e) {
                }
            }

            String fileDeniedNote =
                    result.fileExists
                            ? "The file was not updated, and maintains its original contents."
                            : "The file was not created.";
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolDenied() + " " + fileDeniedNote);
        }

        if (telemetry != null) {
            telemetry.captureToolUsage(
                    config.getUlid(),
                    block.getName(),
                    modelId,
                    false,
                    true,
                    result.workspaceContext);
        }

        return performFileSave(config, block, result);
    }

    private List<UserContentBlock> performFileSave(
            TaskConfig config, ToolUse block, FileOperationResult result) {

        DiffViewProvider dvp = config.getServices().getDiffViewProvider();
        if (dvp == null) {
            throw new IllegalStateException("DiffViewProvider is required but not available");
        }

        TaskConfig.OpenOptions opts = new TaskConfig.OpenOptions();
        opts.displayPath = result.relPath;

        if (!dvp.isEditing()) {
            String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
            String contentValue = result.diff != null ? result.diff : result.content;
            String partialMessage = buildToolMessage(tool, result.relPath, contentValue);

            config.getCallbacks().ask(ClineAsk.TOOL, partialMessage, true, ClineMessageFormat.JSON);
            try {
                dvp.open(result.absolutePath.toString(), opts.displayPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to open diff editor", e);
            }
        }

        dvp.update(result.newContent, true, null);
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        dvp.scrollToFirstDiff();
        DiffViewProvider.SaveResult save = dvp.saveChanges();

        config.getServices().getFileContextTracker().markFileAsEditedByCline(result.relPath);

        config.getTaskState().setDidEditFile(true);

        config.getServices()
                .getFileContextTracker()
                .trackFileContext(result.relPath, "cline_edited")
                .join();
        dvp.reset();

        if (save.userEdits != null && !save.userEdits.isEmpty()) {
            return handleUserEdits(config, result, save);
        } else {
            return HandlerUtils.createTextBlocks(
                    formatResponse.fileEditWithoutUserChanges(
                            result.relPath,
                            save.autoFormattingEdits,
                            save.finalContent,
                            save.newProblemsMessage));
        }
    }

    private List<UserContentBlock> handleUserEdits(
            TaskConfig config, FileOperationResult result, DiffViewProvider.SaveResult save) {
        config.getServices()
                .getFileContextTracker()
                .trackFileContext(result.relPath, "user_edited")
                .join();
        String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
        String userFeedbackMsg = buildUserFeedbackMessage(tool, result.relPath, save.userEdits);
        config.getCallbacks()
                .say(ClineSay.USER_FEEDBACK_DIFF, userFeedbackMsg, null, null, false, null);
        return HandlerUtils.createTextBlocks(
                formatResponse.fileEditWithUserChanges(
                        result.relPath,
                        save.userEdits,
                        save.autoFormattingEdits,
                        save.finalContent,
                        save.newProblemsMessage));
    }

    private static class FileOperationResult {
        String relPath;
        Path absolutePath;
        boolean fileExists;
        String diff;
        String content;
        String newContent;
        TelemetryService.WorkspaceContext workspaceContext;

        FileOperationResult(
                String relPath,
                Path absolutePath,
                boolean fileExists,
                String diff,
                String content,
                String newContent,
                TelemetryService.WorkspaceContext workspaceContext) {
            this.relPath = relPath;
            this.absolutePath = absolutePath;
            this.fileExists = fileExists;
            this.diff = diff;
            this.content = content;
            this.newContent = newContent;
            this.workspaceContext = workspaceContext;
        }
    }
}
