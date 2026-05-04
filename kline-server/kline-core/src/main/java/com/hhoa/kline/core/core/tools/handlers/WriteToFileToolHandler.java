package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.integrations.editor.DiffViewProvider;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;

/**
 * 写入文件工具处理器，仅负责 write_to_file。
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
        String rawRelPath = getPathParam(input.path(), input.absolutePath());
        String rawContent = input.content();

        if (rawRelPath == null || rawContent == null) {
            return;
        }

        ToolContext config = ui.getContext();
        try {
            FileOperationResult result =
                    validateAndPrepareFileOperation(config, rawRelPath, rawContent);
            if (result == null) {
                return;
            }

            String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
            String msg = FileToolSupport.buildToolMessage(tool, result.relPath, result.content);

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

    public ToolExecuteResult execute(WriteToFileInput input, ToolContext context, ToolUse block) {
        String rawRelPath = getPathParam(input.path(), input.absolutePath());
        String rawContent = input.content();

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

        try {
            FileOperationResult result =
                    validateAndPrepareFileOperation(context, rawRelPath, rawContent);
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

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        WriteFileToolState state = (WriteFileToolState) toolState;
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
            ToolContext config, String rawRelPath, String rawContent) {
        try {
            if (config.getWorkspaceManager() == null) {
                throw new IllegalStateException("workspaceManager 未配置，无法写入文件");
            }
            FileToolSupport.ResolvedFileTarget target =
                    FileToolSupport.resolveFileTarget(
                            config,
                            rawRelPath,
                            "WriteToFileToolHandler.validateAndPrepareFileOperation");

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

            if (rawContent == null) {
                return null;
            }
            String newContent = rawContent;

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
            newContent = StringUtils.trimEnd(newContent);

            return new FileOperationResult(
                    rawRelPath,
                    target.absolutePath(),
                    fileExists,
                    rawContent,
                    newContent,
                    target.workspaceContext());
        } catch (Exception e) {
            throw new RuntimeException("Failed to prepare file operation: " + e.getMessage(), e);
        }
    }

    private ToolExecuteResult executeFileOperation(
            ToolContext config, ToolUse block, FileOperationResult result) {

        String tool = result.fileExists ? "editedExistingFile" : "newFileCreated";
        String completeMessage =
                FileToolSupport.buildToolMessage(tool, result.relPath, result.content);

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
                    FileToolSupport.getModelId(config),
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
            String partialMessage =
                    FileToolSupport.buildToolMessage(tool, result.relPath, result.content);

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
        String userFeedbackMsg =
                FileToolSupport.buildUserFeedbackMessage(tool, result.relPath, save.userEdits);
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
        String content;
        String newContent;
        TelemetryService.WorkspaceContext workspaceContext;
        String error;

        FileOperationResult(
                String relPath,
                Path absolutePath,
                boolean fileExists,
                String content,
                String newContent,
                TelemetryService.WorkspaceContext workspaceContext) {
            this.relPath = relPath;
            this.absolutePath = absolutePath;
            this.fileExists = fileExists;
            this.content = content;
            this.newContent = newContent;
            this.workspaceContext = workspaceContext;
        }

        FileOperationResult(String error) {
            this.error = error;
        }
    }
}
