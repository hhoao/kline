package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.MessageParam;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.context.management.ContextTelemetryData;
import com.hhoa.kline.core.core.context.management.KeepStrategy;
import com.hhoa.kline.core.core.ignore.ClineIgnoreController;
import com.hhoa.kline.core.core.integrations.FileContentExtractor;
import com.hhoa.kline.core.core.prompts.ContextManagement;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver.WorkspacePathResult;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 任务总结工具处理器 处理任务总结、文件读取、上下文管理
 *
 * @author hhoa
 */
@Slf4j
public class SummarizeTaskHandler implements ToolHandler {

    private static final String NAME = "summarize_task";
    private static final int MAX_FILES_LOADED = 8;
    private static final int MAX_FILES_PROCESSED = 10;
    private static final int MAX_CHARS = 100_000;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    private static List<String> parseRequiredFiles(String context) {
        List<String> out = new ArrayList<>();
        Pattern pattern =
                Pattern.compile(
                        "9\\.\\s*(?:Optional\\s+)?Required Files:\\s*((?:\\n\\s*-\\s*.+)+)",
                        Pattern.MULTILINE);
        Matcher m = pattern.matcher(context);
        if (m.find()) {
            String block = m.group(1);
            for (String line : block.split("\\n")) {
                Pattern pathPattern = Pattern.compile("^\\s*-\\s*(.+)$");
                Matcher pm = pathPattern.matcher(line);
                if (pm.find()) {
                    out.add(pm.group(1).trim());
                }
            }
        }
        return out;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ClineToolSpec.builder()
                .name(NAME)
                .parameters(
                        List.of(
                                ClineToolSpec.ClineToolSpecParameter.builder()
                                        .name("context")
                                        .required(true)
                                        .instruction("")
                                        .usage("")
                                        .build()))
                .build();
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String context = HandlerUtils.getStringParam(block, "context");
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("tool", "summarizeTask");
        msgMap.put("content", context == null ? "" : context);
        String msg = JsonUtils.toJsonString(msgMap);
        ui.say(ClineSay.TOOL, msg, null, null, block.isPartial(), ClineMessageFormat.JSON);
    }

    @Override
    public ToolExecuteResult execute(ToolContext toolContext, ToolUse block) {
        String context = HandlerUtils.getStringParam(block, "context");

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", "summarizeTask");
        completeMessageMap.put("content", context);
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        toolContext
                .getCallbacks()
                .say(ClineSay.TOOL, completeMessage, null, null, false, ClineMessageFormat.JSON);
        return summarize(toolContext, block, context);
    }

    private ToolExecuteResult summarize(ToolContext config, ToolUse block, String context) {
        try {
            List<String> filePaths = parseRequiredFiles(context);
            StringBuilder fileContents = new StringBuilder();
            Set<String> loaded = new HashSet<>();
            List<String> loadedFilePaths = new ArrayList<>();
            int totalChars = 0;
            int filesLoaded = 0;
            int filesProcessed = 0;

            for (String relPath : filePaths) {
                String norm = relPath.toLowerCase();
                if (loaded.contains(norm)) {
                    continue;
                }
                loaded.add(norm);
                filesProcessed++;
                if (filesProcessed > MAX_FILES_PROCESSED) {
                    break;
                }

                ClineIgnoreController controller = config.getServices().getClineIgnoreController();
                if (controller != null && !controller.validateAccess(relPath)) {
                    continue;
                }

                // 仅在自动批准时处理（尊重工作区/工作区外设置）
                Boolean approve =
                        config.getCallbacks()
                                .shouldAutoApproveToolWithPath(
                                        ClineDefaultTool.FILE_READ.getValue(), relPath);
                if (Boolean.TRUE.equals(approve)) {
                    try {
                        // 解析路径（处理多根工作区）
                        if (config.getWorkspaceManager() == null) {
                            throw new IllegalStateException("workspaceManager 未配置，无法读取文件");
                        }
                        WorkspacePathResult pathResult =
                                WorkspaceResolver.resolveWorkspacePath(
                                        new WorkspaceConfig(config.getWorkspaceManager()),
                                        relPath,
                                        "SummarizeTaskHandler");
                        Path absolutePath = Paths.get(pathResult.absolutePath());
                        String displayPath = pathResult.displayPath();

                        FileContentExtractor.FileContentResult fileContent =
                                FileContentExtractor.extractFileContent(absolutePath, false);

                        if (totalChars + fileContent.text.length() > MAX_CHARS) {
                            continue;
                        }

                        if (config.getServices() != null
                                && config.getServices().getFileContextTracker() != null) {
                            config.getServices()
                                    .getFileContextTracker()
                                    .trackFileContext(relPath, "file_mentioned")
                                    .join();
                        }

                        fileContents
                                .append("\n\n<file_content path=\"")
                                .append(displayPath)
                                .append("\">\n")
                                .append(fileContent.text)
                                .append("\n</file_content>");
                        loadedFilePaths.add(displayPath);

                        totalChars += fileContent.text.length();
                        filesLoaded++;

                        if (filesLoaded >= MAX_FILES_LOADED) {
                            break;
                        }
                    } catch (Exception error) {
                        log.error(
                                "Failed to read "
                                        + relPath
                                        + " during summarization: "
                                        + error.getMessage());
                    }
                }
            }

            if (!fileContents.isEmpty()) {
                String fileMentionString =
                        String.join(
                                        ", ",
                                        loadedFilePaths.stream()
                                                .map(path -> "'" + path + "'")
                                                .toList())
                                + " (see below for file content)";
                fileContents.insert(
                        0,
                        "\n\nThe following files were automatically read based on the files listed in the Required Files section: "
                                + fileMentionString
                                + ". These are the latest versions of these files - you should reference them directly and not re-read them:");
            }
            String toolResult =
                    formatResponse.toolResult(
                            ContextManagement.continuationPrompt(context) + fileContents,
                            null,
                            null);

            List<MessageParam> apiConversationHistory =
                    config.getMessageState().getApiConversationHistory();
            KeepStrategy keepStrategy = KeepStrategy.NONE;

            if (config.getServices() != null && config.getServices().getContextManager() != null) {
                int[] deletedRange =
                        config.getServices()
                                .getContextManager()
                                .getNextTruncationRange(
                                        apiConversationHistory,
                                        config.getTaskState().getConversationHistoryDeletedRange(),
                                        keepStrategy);
                config.getTaskState().setConversationHistoryDeletedRange(deletedRange);
            }

            config.getMessageState().saveClineMessagesAndUpdateHistory();

            if (config.getServices() != null && config.getServices().getContextManager() != null) {
                try {
                    config.getServices()
                            .getContextManager()
                            .triggerApplyStandardContextTruncationNoticeChange(
                                    System.currentTimeMillis(), apiConversationHistory);
                } catch (Exception e) {
                    log.error("Failed to trigger context truncation notice: " + e.getMessage());
                }
            }

            config.getTaskState().setCurrentlySummarizing(true);

            if (config.getServices() != null
                    && config.getServices().getContextManager() != null
                    && config.getServices().getTelemetryService() != null) {
                ContextTelemetryData telemetryData =
                        config.getServices()
                                .getContextManager()
                                .getContextTelemetryData(
                                        config.getMessageState().getClineMessages(),
                                        0, // contextWindow - Model 接口没有
                                        // getContextWindow 方法，使用默认值
                                        config.getTaskState().getLastAutoCompactTriggerIndex());
                if (telemetryData != null && config.getServices().getTelemetryService() != null) {
                    String modelId =
                            config.getApi() != null && config.getApi().getModel() != null
                                    ? config.getApi().getModel().getId()
                                    : "unknown";
                    config.getServices()
                            .getTelemetryService()
                            .captureSummarizeTask(
                                    config.getUlid(),
                                    modelId,
                                    telemetryData.getTokensUsed(),
                                    telemetryData.getMaxContextWindow());
                }
            }

            return HandlerUtils.createToolExecuteResult(toolResult);
        } catch (Exception error) {
            return HandlerUtils.createToolExecuteResult(
                    "Error summarizing context window: "
                            + (error.getMessage() != null ? error.getMessage() : "Unknown error"));
        }
    }
}
