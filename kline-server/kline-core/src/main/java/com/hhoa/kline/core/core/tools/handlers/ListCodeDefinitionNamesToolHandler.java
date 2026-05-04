package com.hhoa.kline.core.core.tools.handlers;

import static com.google.common.io.Files.getFileExtension;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.args.ListCodeDefinitionNamesInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.workspace.WorkspaceConfig;
import com.hhoa.kline.core.core.workspace.WorkspaceResolver;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;

/**
 * 列出源码顶层定义名称的工具处理器 使用正则表达式提取代码定义（类、函数、方法等）
 *
 * @author hhoa
 */
public class ListCodeDefinitionNamesToolHandler
        implements StateFullToolHandler<ListCodeDefinitionNamesInput> {

    private static final int MAX_FILES = 200;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** ListCodeDefinitionNamesToolHandler 的阶段状态 */
    @Getter
    @Setter
    public static class ListCodeDefToolState extends ToolState {
        private String result;
    }

    @Override
    public ToolState createToolState() {
        return new ListCodeDefToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + HandlerUtils.getStringParam(block, "path") + "']";
    }

    @Override
    public boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        String path = HandlerUtils.getStringParam(block, "path");
        return context != null
                && context.getCallbacks() != null
                && Boolean.TRUE.equals(
                        context.getCallbacks()
                                .shouldAutoApproveToolWithPath(block.getName(), path));
    }

    public void handlePartialBlock(
            ListCodeDefinitionNamesInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String relPath = input.path();
        ToolContext config = ui.getContext();

        Map<String, Object> sharedMessageMap = new HashMap<>();
        sharedMessageMap.put("tool", "listCodeDefinitionNames");
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

    public ToolExecuteResult execute(
            ListCodeDefinitionNamesInput input, ToolContext context, ToolUse block) {
        String relPath = input.path();

        // try/catch so that failures (e.g. bad workspace hint, non-existent
        // directory) return a graceful tool error instead of crashing the task.
        String absolutePath;
        String displayPath;
        String result;
        try {
            if (context.getWorkspaceManager() == null) {
                throw new IllegalStateException("workspaceManager 未配置，无法列举代码定义");
            }
            WorkspaceConfig workspaceConfig = new WorkspaceConfig(context.getWorkspaceManager());
            WorkspaceResolver.WorkspacePathResult pathResult =
                    WorkspaceResolver.resolveWorkspacePath(
                            workspaceConfig, relPath, "ListCodeDefinitionNamesToolHandler.execute");

            absolutePath = pathResult.absolutePath();
            displayPath = pathResult.displayPath();
            result = parseCodeDefinitions(Paths.get(absolutePath), context);
        } catch (Exception e) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            String errorMessage = e.getMessage() != null ? e.getMessage() : String.valueOf(e);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("Error listing code definitions: " + errorMessage));
        }

        // parseCodeDefinitions returns error strings for file paths
        // and non-existent directories rather than throwing. Check for these error
        // conditions and increment the counter so repeated failures accumulate.
        boolean isErrorResult =
                result.contains("does not exist or you do not have permission")
                        || result.contains("The specified path is not a directory");
        if (isErrorResult) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            return HandlerUtils.createToolExecuteResult(formatResponse.toolError(result));
        }

        // Only reset after a successful operation so repeated failures
        // accumulate toward the yolo-mode mistake limit.
        context.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", "listCodeDefinitionNames");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(context.getCwd(), displayPath));
        completeMessageMap.put("content", result);
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, context)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                context.getCallbacks()
                        .shouldAutoApproveToolWithPath(
                                ClineDefaultTool.LIST_CODE_DEF.getValue(), relPath);
        if (Boolean.TRUE.equals(approve)) {
            context.getCallbacks()
                    .say(
                            ClineSay.TOOL,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);

            captureTelemetry(context, block, true, true);

            return HandlerUtils.createToolExecuteResult(result);
        }

        // 需要 ask 用户 —— 保存状态并返回 PendingAsk
        String notificationMessage =
                "Cline wants to analyze code definitions in "
                        + WorkspaceResolver.getWorkspaceBasename(
                                absolutePath, "ListCodeDefinitionNamesToolHandler.notification");
        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                notificationMessage,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, message) -> {});

        ListCodeDefToolState state = (ListCodeDefToolState) context.getToolState();
        state.setPhase(1);
        state.setResult(result);

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
        ListCodeDefToolState state = (ListCodeDefToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false);
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true);
        return HandlerUtils.createToolExecuteResult(state.getResult());
    }

    private void captureTelemetry(
            ToolContext context, ToolUse block, boolean autoApproved, boolean approved) {
        if (context.getServices() != null && context.getServices().getTelemetryService() != null) {
            String modelId =
                    context.getApi() != null && context.getApi().getModel() != null
                            ? context.getApi().getModel().getId()
                            : "unknown";
            context.getServices()
                    .getTelemetryService()
                    .captureToolUsage(
                            context.getUlid(), block.getName(), modelId, autoApproved, approved);
        }
    }

    private String parseCodeDefinitions(Path dirPath, ToolContext config) throws IOException {
        if (!Files.exists(dirPath)) {
            return "This directory does not exist or you do not have permission to access it.";
        }

        if (!Files.isDirectory(dirPath)) {
            return "The specified path is not a directory.";
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(dirPath)) {
            files =
                    stream.filter(Files::isRegularFile)
                            .filter(
                                    file -> {
                                        if (config.getServices().getClineIgnoreController()
                                                != null) {
                                            String relPath = dirPath.relativize(file).toString();
                                            return config.getServices()
                                                    .getClineIgnoreController()
                                                    .validateAccess(relPath);
                                        }
                                        return true;
                                    })
                            .filter(file -> isSupportedFile(file.getFileName().toString()))
                            .limit(MAX_FILES)
                            .toList();
        }

        if (files.isEmpty()) {
            return "No source code files found.";
        }

        StringBuilder result = new StringBuilder();
        for (Path file : files) {
            String definitions = parseFile(file);
            if (definitions != null && !definitions.isEmpty()) {
                String relPath = dirPath.relativize(file).toString();
                result.append(relPath).append("\n");
                result.append(definitions).append("\n");
            }
        }

        return result.length() > 0 ? result.toString() : "No source code definitions found.";
    }

    private boolean isSupportedFile(String filename) {
        String ext = getFileExtension(filename).toLowerCase();
        return ext.matches("java|js|ts|tsx|py|rs|go|cpp|c|cs|rb|php|swift|kt|jsx");
    }

    private String parseFile(Path file) {
        try {
            String content = Files.readString(file);
            String ext = getFileExtension(file.getFileName().toString()).toLowerCase();

            List<String> definitions = new ArrayList<>();

            switch (ext) {
                case "java":
                    definitions.addAll(parseJavaDefinitions(content));
                    break;
                case "js":
                case "jsx":
                case "ts":
                case "tsx":
                    definitions.addAll(parseJavaScriptDefinitions(content));
                    break;
                case "py":
                    definitions.addAll(parsePythonDefinitions(content));
                    break;
                case "go":
                    definitions.addAll(parseGoDefinitions(content));
                    break;
                case "rs":
                    definitions.addAll(parseRustDefinitions(content));
                    break;
                default:
                    definitions.addAll(parseGenericDefinitions(content));
            }

            return definitions.isEmpty() ? null : String.join("\n", definitions);
        } catch (IOException e) {
            return null;
        }
    }

    private List<String> parseJavaDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern classPattern =
                Pattern.compile(
                        "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:abstract)?\\s*(?:class|interface|enum)\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            definitions.add("class " + classMatcher.group(1));
        }

        Pattern methodPattern =
                Pattern.compile(
                        "(?:public|private|protected)?\\s*(?:static)?\\s*(?:final)?\\s*(?:<[^>]+>)?\\s*\\w+\\s+(\\w+)\\s*\\([^)]*\\)");
        Matcher methodMatcher = methodPattern.matcher(content);
        while (methodMatcher.find()) {
            String methodName = methodMatcher.group(1);
            if (!methodName.matches("if|for|while|switch|catch|return|new|throw")) {
                definitions.add("  " + methodName + "()");
            }
        }

        return definitions;
    }

    private List<String> parseJavaScriptDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern classPattern = Pattern.compile("(?:export\\s+)?(?:default\\s+)?class\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            definitions.add("class " + classMatcher.group(1));
        }

        Pattern functionPattern =
                Pattern.compile("(?:export\\s+)?(?:async\\s+)?function\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            definitions.add("  " + functionMatcher.group(1) + "()");
        }

        Pattern arrowPattern =
                Pattern.compile(
                        "(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s+)?\\([^)]*\\)\\s*=>");
        Matcher arrowMatcher = arrowPattern.matcher(content);
        while (arrowMatcher.find()) {
            definitions.add("  " + arrowMatcher.group(1) + "()");
        }

        return definitions;
    }

    private List<String> parsePythonDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern classPattern = Pattern.compile("class\\s+(\\w+)");
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            definitions.add("class " + classMatcher.group(1));
        }

        Pattern functionPattern = Pattern.compile("def\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            definitions.add("  " + functionMatcher.group(1) + "()");
        }

        return definitions;
    }

    private List<String> parseGoDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern typePattern = Pattern.compile("type\\s+(\\w+)\\s+(?:struct|interface)");
        Matcher typeMatcher = typePattern.matcher(content);
        while (typeMatcher.find()) {
            definitions.add("type " + typeMatcher.group(1));
        }

        Pattern functionPattern = Pattern.compile("func\\s+(?:\\([^)]+\\)\\s+)?(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            definitions.add("  " + functionMatcher.group(1) + "()");
        }

        return definitions;
    }

    private List<String> parseRustDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern structPattern = Pattern.compile("(?:pub\\s+)?(?:struct|enum|trait)\\s+(\\w+)");
        Matcher structMatcher = structPattern.matcher(content);
        while (structMatcher.find()) {
            definitions.add("struct " + structMatcher.group(1));
        }

        Pattern functionPattern = Pattern.compile("(?:pub\\s+)?fn\\s+(\\w+)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            definitions.add("  " + functionMatcher.group(1) + "()");
        }

        return definitions;
    }

    private List<String> parseGenericDefinitions(String content) {
        List<String> definitions = new ArrayList<>();

        Pattern pattern = Pattern.compile("(?:function|def|func|fn)\\s+(\\w+)\\s*\\(");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            definitions.add("  " + matcher.group(1) + "()");
        }

        return definitions;
    }
}
