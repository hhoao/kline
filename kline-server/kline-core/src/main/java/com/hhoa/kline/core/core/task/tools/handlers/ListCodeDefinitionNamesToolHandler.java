package com.hhoa.kline.core.core.task.tools.handlers;

import static com.google.common.io.Files.getFileExtension;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ListCodeDefinitionNamesTool;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 列出源码顶层定义名称的工具处理器 使用正则表达式提取代码定义（类、函数、方法等）
 *
 * @author hhoa
 */
public class ListCodeDefinitionNamesToolHandler implements FullyManagedTool {

    private static final int MAX_FILES = 200;

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.LIST_CODE_DEF.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + HandlerUtils.getStringParam(block, "path") + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ListCodeDefinitionNamesTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String relPath = HandlerUtils.getStringParam(block, "path");
        TaskConfig config = ui.getConfig();

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

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String relPath = HandlerUtils.getStringParam(block, "path");

        config.getTaskState().setConsecutiveMistakeCount(0);

        if (config.getWorkspaceManager() == null) {
            throw new IllegalStateException("workspaceManager 未配置，无法列举代码定义");
        }
        WorkspaceConfig workspaceConfig = new WorkspaceConfig(config.getWorkspaceManager());
        Object pathResult =
                WorkspaceResolver.resolveWorkspacePath(
                        workspaceConfig, relPath, "ListCodeDefinitionNamesToolHandler.execute");

        String absolutePath;
        String displayPath;
        if (pathResult instanceof String) {
            absolutePath = (String) pathResult;
            displayPath = relPath;
        } else {
            WorkspaceResolver.WorkspacePathResult workspacePathResult =
                    (WorkspaceResolver.WorkspacePathResult) pathResult;
            absolutePath = workspacePathResult.absolutePath();
            displayPath = workspacePathResult.displayPath();
        }

        String result;
        try {
            result = parseCodeDefinitions(Paths.get(absolutePath), config);
        } catch (IOException e) {
            result = "Error parsing code definitions: " + e.getMessage();
        }

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("tool", "listCodeDefinitionNames");
        completeMessageMap.put("path", HandlerUtils.getReadablePath(config.getCwd(), displayPath));
        completeMessageMap.put("content", result);
        completeMessageMap.put(
                "operationIsLocatedInWorkspace",
                String.valueOf(HandlerUtils.isLocatedInWorkspace(relPath, config)));
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean approve =
                config.getCallbacks()
                        .shouldAutoApproveToolWithPath(
                                ClineDefaultTool.LIST_CODE_DEF.getValue(), relPath);
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
                        .captureToolUsage(config.getUlid(), block.getName(), modelId, true, true);
            }

            return HandlerUtils.createTextBlocks(result);
        } else {
            String notificationMessage =
                    "Cline wants to analyze code definitions in "
                            + WorkspaceResolver.getWorkspaceBasename(
                                    absolutePath,
                                    "ListCodeDefinitionNamesToolHandler.notification");
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
                                    config.getUlid(), block.getName(), modelId, false, false);
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
                        .captureToolUsage(config.getUlid(), block.getName(), modelId, false, true);
            }

            return HandlerUtils.createTextBlocks(result);
        }
    }

    private String parseCodeDefinitions(Path dirPath, TaskConfig config) throws IOException {
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
