package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.DiffProcessor;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.args.ReplaceInFileInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.Getter;
import lombok.Setter;

/** replace_in_file 专属处理器，职责等价于 Claude Code 的 FileEditTool。 */
public class ReplaceInFileToolHandler implements StateFullToolHandler<ReplaceInFileInput> {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class ReplaceFileToolState extends ToolState {
        private FileOperationResult result;
        private String completeMessage;
    }

    @Override
    public ToolState createToolState() {
        return new ReplaceFileToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + HandlerUtils.getPathParam(block) + "']";
    }

    @Override
    public void handlePartialBlock(ReplaceInFileInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String rawRelPath = getPathParam(input.path(), input.absolutePath());
        String rawDiff = input.diff();
        if (rawRelPath == null || rawDiff == null || rawDiff.isBlank()) {
            return;
        }
        try {
            FileOperationResult result =
                    validateAndPrepareReplaceOperation(context, block, rawRelPath, rawDiff);
            if (result == null) {
                return;
            }
            String msg =
                    FileToolSupport.buildToolMessage("editedExistingFile", result.relPath, result.diff);
            Boolean approve = ui.shouldAutoApproveToolWithPath(block.getName(), result.relPath);
            if (Boolean.TRUE.equals(approve)) {
                ui.say(ClineSay.TOOL, msg, null, null, block.isPartial(), ClineMessageFormat.JSON);
            } else {
                ui.ask(ClineAsk.TOOL, msg, block.isPartial(), ClineMessageFormat.JSON);
            }
        } catch (Exception error) {
            DiffViewProvider dvp = context.getServices().getDiffViewProvider();
            if (dvp != null) {
                try {
                    dvp.revertChanges();
                    dvp.reset();
                } catch (Exception ignored) {
                }
            }
            throw new RuntimeException(error);
        }
    }

    @Override
    public ToolExecuteResult execute(ReplaceInFileInput input, ToolContext context, ToolUse block) {
        String rawRelPath = getPathParam(input.path(), input.absolutePath());
        String rawDiff = input.diff();

        if (rawDiff == null || rawDiff.trim().isEmpty()) {
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

        try {
            FileOperationResult result =
                    validateAndPrepareReplaceOperation(context, block, rawRelPath, rawDiff);
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
                } catch (Exception ignored) {
                }
            }
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("File operation failed: " + e.getMessage()));
        }
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        ReplaceFileToolState state = (ReplaceFileToolState) toolState;
        FileOperationResult result = state.getResult();

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        String modelId = FileToolSupport.getModelId(context);
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
                } catch (IOException ignored) {
                }
            }
            context.getTaskState().getToolExecutionState().setDidRejectTool(true);
            return HandlerUtils.createToolExecuteResult(
                    "The user denied this operation. The file was not updated, and maintains its original contents.");
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
        return performFileSave(context, result);
    }

    private FileOperationResult validateAndPrepareReplaceOperation(
            ToolContext config, ToolUse block, String rawRelPath, String rawDiff) {
        try {
            if (config.getWorkspaceManager() == null) {
                throw new IllegalStateException("workspaceManager 未配置，无法编辑文件");
            }
            FileToolSupport.ResolvedFileTarget target =
                    FileToolSupport.resolveFileTarget(
                            config,
                            rawRelPath,
                            "ReplaceInFileToolHandler.validateAndPrepareReplaceOperation");

            if (!FileToolSupport.validateClineIgnore(config, target.resolvedPath())) {
                config.getCallbacks()
                        .say(
                                ClineSay.CLINEIGNORE_ERROR,
                                target.resolvedPath(),
                                null,
                                null,
                                false,
                                null);
                String errorResponse =
                        formatResponse.toolError(
                                formatResponse.clineIgnoreError(target.resolvedPath()));
                return new FileOperationResult(errorResponse);
            }

            DiffViewProvider dvp = config.getServices().getDiffViewProvider();
            boolean fileExists = FileToolSupport.determineFileExists(dvp, target.absolutePath());
            if (!fileExists) {
                return new FileOperationResult(
                        formatResponse.toolError("Cannot use replace_in_file on a non-existent file."));
            }

            String diff = rawDiff;
            if (!config.getApi().getModel().getId().contains("claude")) {
                diff = StringUtils.fixModelHtmlEscaping(diff);
                diff = StringUtils.removeInvalidChars(diff);
            }

            if (dvp != null && !dvp.isEditing()) {
                ToolContext.OpenOptions opts = new ToolContext.OpenOptions();
                opts.displayPath = rawRelPath;
                dvp.open(target.absolutePath().toString(), opts.displayPath);
            }

            try {
                String originalContent =
                        dvp != null && dvp.getOriginalContent() != null
                                ? dvp.getOriginalContent()
                                : Files.readString(target.absolutePath());
                String newContent =
                        new DiffProcessor()
                                .constructNewFileContent(diff, originalContent, !block.isPartial());
                newContent = StringUtils.trimEnd(newContent);
                return new FileOperationResult(
                        rawRelPath,
                        target.absolutePath(),
                        true,
                        diff,
                        newContent,
                        target.workspaceContext());
            } catch (Exception error) {
                config.getCallbacks()
                        .say(
                                ClineSay.DIFF_ERROR,
                                target.resolvedPath(),
                                null,
                                null,
                                false,
                                null);
                String errorType =
                        error.getMessage() != null && error.getMessage().contains("does not match anything")
                                ? "search_not_found"
                                : "other_diff_error";
                TelemetryService telemetry = config.getServices().getTelemetryService();
                if (telemetry != null) {
                    telemetry.captureDiffEditFailure(
                            config.getUlid(), FileToolSupport.getModelId(config), errorType);
                }
                String errorResponse =
                        formatResponse.toolError(
                                (error.getMessage() != null ? error.getMessage() : "")
                                        + "\n\n"
                                        + formatResponse.diffError(
                                                target.resolvedPath(),
                                                dvp != null && dvp.getOriginalContent() != null
                                                        ? dvp.getOriginalContent()
                                                        : ""));
                if (dvp != null) {
                    try {
                        dvp.revertChanges();
                        dvp.reset();
                    } catch (Exception ignored) {
                    }
                }
                return new FileOperationResult(errorResponse);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare file operation: " + e.getMessage(), e);
        }
    }

    private ToolExecuteResult executeFileOperation(
            ToolContext config, ToolUse block, FileOperationResult result) {
        String completeMessage =
                FileToolSupport.buildToolMessage("editedExistingFile", result.relPath, result.diff);
        Boolean approve =
                config.getCallbacks().shouldAutoApproveToolWithPath(block.getName(), result.relPath);
        if (Boolean.TRUE.equals(approve)) {
            config.getCallbacks()
                    .say(ClineSay.TOOL, completeMessage, null, null, false, ClineMessageFormat.JSON);
            TelemetryService telemetry = config.getServices().getTelemetryService();
            if (telemetry != null) {
                telemetry.captureToolUsage(
                        config.getUlid(),
                        block.getName(),
                        FileToolSupport.getModelId(config),
                        true,
                        true,
                        result.workspaceContext);
            }
            ToolExecuteResult saveResult = performFileSave(config, result);
            try {
                Thread.sleep(3_500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return saveResult;
        }

        ReplaceFileToolState state = (ReplaceFileToolState) config.getToolState();
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

    private ToolExecuteResult performFileSave(ToolContext config, FileOperationResult result) {
        DiffViewProvider dvp = config.getServices().getDiffViewProvider();
        if (dvp == null) {
            throw new IllegalStateException("DiffViewProvider is required but not available");
        }

        ToolContext.OpenOptions opts = new ToolContext.OpenOptions();
        opts.displayPath = result.relPath;
        if (!dvp.isEditing()) {
            String partialMessage =
                    FileToolSupport.buildToolMessage("editedExistingFile", result.relPath, result.diff);
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

        config.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);
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
            config.getServices()
                    .getFileContextTracker()
                    .trackFileContext(result.relPath, "user_edited")
                    .join();
            String userFeedbackMsg =
                    FileToolSupport.buildUserFeedbackMessage(
                            "editedExistingFile", result.relPath, save.userEdits);
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

        return HandlerUtils.createToolExecuteResult(
                formatResponse.fileEditWithoutUserChanges(
                        result.relPath,
                        save.autoFormattingEdits,
                        save.finalContent,
                        save.newProblemsMessage));
    }

    private static String getPathParam(String path, String absolutePath) {
        return path != null && !path.isEmpty() ? path : absolutePath;
    }

    private static class FileOperationResult {
        String relPath;
        Path absolutePath;
        String diff;
        String newContent;
        TelemetryService.WorkspaceContext workspaceContext;
        String error;

        FileOperationResult(
                String relPath,
                Path absolutePath,
                boolean ignoredFileExists,
                String diff,
                String newContent,
                TelemetryService.WorkspaceContext workspaceContext) {
            this.relPath = relPath;
            this.absolutePath = absolutePath;
            this.diff = diff;
            this.newContent = newContent;
            this.workspaceContext = workspaceContext;
        }

        FileOperationResult(String error) {
            this.error = error;
        }
    }
}
