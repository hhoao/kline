package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.DiffProcessor;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.ToolArgumentMapper;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

/**
 * 写入/替换文件工具处理器 支持 write_to_file / replace_in_file / new_rule 三类操作
 *
 * @author hhoa
 */
public class WriteToFileToolHandler implements StateFullToolHandler<WriteToFileInput> {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** WriteToFileToolHandler 的阶段状态 */
    @Getter
    @Setter
    public static class WriteFileToolState extends ToolState {
        private FileOperationResult result;
        private String completeMessage;
    }

    private static String getPathParam(String path, String absolutePath) {
        return path != null && !path.isEmpty() ? path : absolutePath;
    }

    private static FileToolInput inputForTool(WriteToFileInput input, ToolUse block) {
        if ("replace_in_file".equals(block.getName())) {
            ReplaceInFileInput replaceInput =
                    ToolArgumentMapper.map(block, ReplaceInFileInput.class);
            return new FileToolInput(
                    replaceInput.path(), replaceInput.absolutePath(), null, replaceInput.diff());
        }
        return new FileToolInput(input.path(), input.absolutePath(), input.content(), null);
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

    private static String getModelId(ToolContext config) {
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
    public ToolState createToolState() {
        return new WriteFileToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + HandlerUtils.getPathParam(block) + "']";
    }

    public void handlePartialBlock(WriteToFileInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        FileToolInput toolInput = inputForTool(input, block);
        String rawRelPath = getPathParam(toolInput.path(), toolInput.absolutePath());
        String rawContent = toolInput.content();
        String rawDiff = toolInput.diff();

        if (rawRelPath == null || (rawContent == null && rawDiff == null)) {
            return;
        }

        ToolContext config = ui.getContext();
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

    public void handleReplaceInFilePartialBlock(
            ReplaceInFileInput input, ToolContext context, ToolUse block) {
        WriteToFileInput writeInput =
                new WriteToFileInput(
                        input.path(), input.absolutePath(), null, input.taskProgress());
        handlePartialBlock(writeInput, context, block);
    }

    public ToolExecuteResult execute(WriteToFileInput input, ToolContext context, ToolUse block) {
        FileToolInput toolInput = inputForTool(input, block);
        String rawRelPath = getPathParam(toolInput.path(), toolInput.absolutePath());
        String rawContent = toolInput.content();
        String rawDiff = toolInput.diff();

        if ("replace_in_file".equals(block.getName())
                && (rawDiff == null || rawDiff.trim().isEmpty())) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            if (context.getServices().getDiffViewProvider() != null) {
                try {
                    context.getServices().getDiffViewProvider().reset();
                } catch (Exception ignored) {
                }
            }
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(formatResponse.missingToolParameterError("diff")));
        }

        if ("write_to_file".equals(block.getName())
                && (rawContent == null || rawContent.trim().isEmpty())) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            if (context.getServices().getDiffViewProvider() != null) {
                try {
                    context.getServices().getDiffViewProvider().reset();
                } catch (Exception ignored) {
                }
            }
            int mistakeCount =
                    context.getTaskState().getApiTurnState().getConsecutiveMistakeCount();
            context.getCallbacks()
                    .say(
                            ClineSay.ERROR,
                            "Cline tried to use write_to_file for '"
                                    + rawRelPath
                                    + "' without value for required parameter 'content'. "
                                    + (mistakeCount >= 2
                                            ? "This has happened multiple times — Cline will try a different approach."
                                            : "Retrying..."),
                            null,
                            null,
                            false,
                            null);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            formatResponse.writeToFileMissingContentError(
                                    rawRelPath, mistakeCount, null)));
        }

        if ("new_rule".equals(block.getName())
                && (rawContent == null || rawContent.trim().isEmpty())) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            if (context.getServices().getDiffViewProvider() != null) {
                try {
                    context.getServices().getDiffViewProvider().reset();
                } catch (Exception ignored) {
                }
            }
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(formatResponse.missingToolParameterError("content")));
        }

        try {
            FileOperationResult result =
                    validateAndPrepareFileOperation(
                            context, block, rawRelPath, rawDiff, rawContent);
            if (result == null) {
                return HandlerUtils.createToolExecuteResult("");
            }
            if (result.error != null) {
                return HandlerUtils.createToolExecuteResult(result.error);
            }

            return executeFileOperation(context, block, result);
        } catch (Exception e) {
            if (context.getServices().getDiffViewProvider() != null) {
                try {
                    context.getServices().getDiffViewProvider().revertChanges();
                    context.getServices().getDiffViewProvider().reset();
                } catch (Exception ex) {
                }
            }
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("File operation failed: " + e.getMessage()));
        }
    }

    public ToolExecuteResult executeReplaceInFile(
            ReplaceInFileInput input, ToolContext context, ToolUse block) {
        WriteToFileInput writeInput =
                new WriteToFileInput(
                        input.path(), input.absolutePath(), null, input.taskProgress());
        return execute(writeInput, context, block);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        WriteFileToolState state = (WriteFileToolState) toolState;
        FileOperationResult result = state.getResult();

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        String modelId = getModelId(context);
        TelemetryService telemetry = context.getServices().getTelemetryService();

        if (!approved) {
            if (telemetry != null) {
                telemetry.captureToolUsage(
                        context.getUlid(),
                        block.getName(),
                        modelId,
                        false,
                        false,
                        result.workspaceContext);
            }

            DiffViewProvider dvp = context.getServices().getDiffViewProvider();
            if (dvp != null) {
                try {
                    dvp.revertChanges();
                } catch (IOException e) {
                }
            }

            context.getTaskState().getToolExecutionState().setDidRejectTool(true);

            String fileDeniedNote =
                    result.fileExists
                            ? "The file was not updated, and maintains its original contents."
                            : "The file was not created.";
            return HandlerUtils.createToolExecuteResult(
                    "The user denied this operation. " + fileDeniedNote);
        }

        if (telemetry != null) {
            telemetry.captureToolUsage(
                    context.getUlid(),
                    block.getName(),
                    modelId,
                    false,
                    true,
                    result.workspaceContext);
        }

        return performFileSave(context, block, result);
    }

    private FileOperationResult validateAndPrepareFileOperation(
            ToolContext config,
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

                return new FileOperationResult(errorResponse);
            }

            DiffViewProvider dvp = config.getServices().getDiffViewProvider();
            boolean fileExists = determineFileExists(dvp, absolutePath);

            String newContent = "";

            String diff = rawDiff;

            if (diff != null) {
                if (!config.getApi().getModel().getId().contains("claude")) {
                    diff = StringUtils.fixModelHtmlEscaping(diff);
                    diff = StringUtils.removeInvalidChars(diff);
                }

                if (!config.getServices().getDiffViewProvider().isEditing()) {
                    ToolContext.OpenOptions opts = new ToolContext.OpenOptions();
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

                    if (dvp != null) {
                        try {
                            dvp.revertChanges();
                            dvp.reset();
                        } catch (Exception ex) {
                        }
                    }

                    return new FileOperationResult(errorResponse);
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

    private ToolExecuteResult executeFileOperation(
            ToolContext config, ToolUse block, FileOperationResult result) {

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

    private ToolExecuteResult executeAutoApprovedFileOperation(
            ToolContext config, ToolUse block, FileOperationResult result, String completeMessage) {

        config.getCallbacks()
                .say(ClineSay.TOOL, completeMessage, null, null, false, ClineMessageFormat.JSON);

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

        ToolExecuteResult saveResult = performFileSave(config, block, result);
        try {
            Thread.sleep(3_500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return saveResult;
    }

    private ToolExecuteResult executeManualApprovedFileOperation(
            ToolContext config, ToolUse block, FileOperationResult result, String completeMessage) {

        WriteFileToolState state = (WriteFileToolState) config.getToolState();
        state.setPhase(1);
        state.setResult(result);
        state.setCompleteMessage(completeMessage);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.TOOL,
                        completeMessage,
                        config,
                        ClineMessageFormat.JSON,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    private ToolExecuteResult performFileSave(
            ToolContext config, ToolUse block, FileOperationResult result) {

        DiffViewProvider dvp = config.getServices().getDiffViewProvider();
        if (dvp == null) {
            throw new IllegalStateException("DiffViewProvider is required but not available");
        }

        ToolContext.OpenOptions opts = new ToolContext.OpenOptions();
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

        // Reset consecutive mistake counter on successful file operation
        config.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);

        // Invalidate file read cache for this file so re-reads get fresh content
        config.getTaskState()
                .getToolExecutionState()
                .getFileReadCache()
                .remove(result.absolutePath.toString().toLowerCase());

        config.getServices().getFileContextTracker().markFileAsEditedByCline(result.relPath);

        config.getServices()
                .getFileContextTracker()
                .trackFileContext(result.relPath, "cline_edited")
                .join();
        dvp.reset();

        if (save.userEdits != null && !save.userEdits.isEmpty()) {
            return handleUserEdits(config, result, save);
        } else {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.fileEditWithoutUserChanges(
                            result.relPath,
                            save.autoFormattingEdits,
                            save.finalContent,
                            save.newProblemsMessage));
        }
    }

    private ToolExecuteResult handleUserEdits(
            ToolContext config, FileOperationResult result, DiffViewProvider.SaveResult save) {
        config.getServices()
                .getFileContextTracker()
                .trackFileContext(result.relPath, "user_edited")
                .join();
        String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
        String userFeedbackMsg = buildUserFeedbackMessage(tool, result.relPath, save.userEdits);
        config.getCallbacks()
                .say(ClineSay.USER_FEEDBACK_DIFF, userFeedbackMsg, null, null, false, null);
        return HandlerUtils.createToolExecuteResult(
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
        String error;

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

        FileOperationResult(String error) {
            this.error = error;
        }
    }

    private record FileToolInput(String path, String absolutePath, String content, String diff) {}
}
