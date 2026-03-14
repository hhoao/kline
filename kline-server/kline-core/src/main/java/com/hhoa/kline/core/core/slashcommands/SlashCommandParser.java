package com.hhoa.kline.core.core.slashcommands;

import com.hhoa.kline.core.core.prompts.Commands;
import com.hhoa.kline.core.core.services.telemetry.TelemetryService;
import com.hhoa.kline.core.core.shared.FocusChainSettings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author hhoa
 */
@Slf4j
public class SlashCommandParser {

    private static final List<String> SUPPORTED_DEFAULT_COMMANDS =
            Arrays.asList(
                    "newtask",
                    "smol",
                    "compact",
                    "newrule",
                    "reportbug",
                    "deep-planning",
                    "subagent");

    /** 标签模式定义 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TagPattern {
        private String tag;
        private Pattern regex;
    }

    private static final List<TagPattern> tagPatterns =
            Arrays.asList(
                    new TagPattern(
                            "task",
                            Pattern.compile(
                                    "<task>(\\s*/([a-zA-Z0-9_.-]+))(\\s+.+?)?\\s*</task>",
                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL)),
                    new TagPattern(
                            "feedback",
                            Pattern.compile(
                                    "<feedback>(\\s*/([a-zA-Z0-9_.-]+))(\\s+.+?)?\\s*</feedback>",
                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL)),
                    new TagPattern(
                            "answer",
                            Pattern.compile(
                                    "<answer>(\\s*/([a-zA-Z0-9_.-]+))(\\s+.+?)?\\s*</answer>",
                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL)),
                    new TagPattern(
                            "user_message",
                            Pattern.compile(
                                    "<user_message>(\\s*/([a-zA-Z0-9_.-]+))(\\s+.+?)?\\s*</user_message>",
                                    Pattern.CASE_INSENSITIVE | Pattern.DOTALL)));

    /**
     * @param text 要处理的文本
     * @param localWorkflowToggles 本地工作流切换状态
     * @param globalWorkflowToggles 全局工作流切换状态
     * @param ulid 唯一标识符
     * @param focusChainSettings 焦点链设置（可选）
     * @param telemetryService 遥测服务
     * @return 处理结果，包含处理后的文本和是否需要检查 clinerules 文件
     */
    public static SlashCommandParseResult parseSlashCommands(
            String text,
            Map<String, Boolean> localWorkflowToggles,
            Map<String, Boolean> globalWorkflowToggles,
            String ulid,
            FocusChainSettings focusChainSettings,
            TelemetryService telemetryService) {

        Map<String, String> commandReplacements = buildCommandReplacements(focusChainSettings);

        for (TagPattern tagPattern : tagPatterns) {
            Matcher matcher = tagPattern.getRegex().matcher(text);

            if (matcher.find()) {
                // match[1] 是带前导空白字符的命令（例如 " /newtask"）
                // match[2] 是命令名称（例如 "newtask"）
                String commandName = matcher.group(2);

                if (SUPPORTED_DEFAULT_COMMANDS.contains(commandName)) {
                    int fullMatchStartIndex = matcher.start();

                    String fullMatch = matcher.group(0);
                    int relativeStartIndex = fullMatch.indexOf(matcher.group(1));

                    int slashCommandStartIndex = fullMatchStartIndex + relativeStartIndex;
                    int slashCommandEndIndex = slashCommandStartIndex + matcher.group(1).length();

                    String textWithoutSlashCommand =
                            text.substring(0, slashCommandStartIndex)
                                    + text.substring(slashCommandEndIndex);
                    String processedText =
                            commandReplacements.get(commandName) + textWithoutSlashCommand;

                    if (telemetryService != null) {
                        telemetryService.captureSlashCommandUsed(ulid, commandName, "builtin");
                    }

                    return new SlashCommandParseResult(
                            processedText, commandName.equals("newrule"));
                }

                List<WorkflowInfo> globalWorkflows = getEnabledWorkflows(globalWorkflowToggles);
                List<WorkflowInfo> localWorkflows = getEnabledWorkflows(localWorkflowToggles);

                List<WorkflowInfo> enabledWorkflows = new ArrayList<>();
                enabledWorkflows.addAll(localWorkflows);
                enabledWorkflows.addAll(globalWorkflows);

                Optional<WorkflowInfo> matchingWorkflow =
                        enabledWorkflows.stream()
                                .filter(workflow -> workflow.getFileName().equals(commandName))
                                .findFirst();

                if (matchingWorkflow.isPresent()) {
                    WorkflowInfo workflow = matchingWorkflow.get();
                    try {
                        String workflowContent =
                                Files.readString(Paths.get(workflow.getFullPath())).trim();

                        int fullMatchStartIndex = matcher.start();
                        String fullMatch = matcher.group(0);
                        int relativeStartIndex = fullMatch.indexOf(matcher.group(1));

                        int slashCommandStartIndex = fullMatchStartIndex + relativeStartIndex;
                        int slashCommandEndIndex =
                                slashCommandStartIndex + matcher.group(1).length();

                        String textWithoutSlashCommand =
                                text.substring(0, slashCommandStartIndex)
                                        + text.substring(slashCommandEndIndex);
                        String processedText =
                                String.format(
                                        "<explicit_instructions type=\"%s\">\n%s\n</explicit_instructions>\n%s",
                                        workflow.getFileName(),
                                        workflowContent,
                                        textWithoutSlashCommand);

                        if (telemetryService != null) {
                            telemetryService.captureSlashCommandUsed(ulid, commandName, "workflow");
                        }

                        return new SlashCommandParseResult(processedText, false);
                    } catch (IOException e) {
                        log.error(
                                "Error reading workflow file {}: {}",
                                workflow.getFullPath(),
                                e.getMessage(),
                                e);
                    }
                }
            }
        }

        return new SlashCommandParseResult(text, false);
    }

    /** 构建命令替换映射 */
    private static Map<String, String> buildCommandReplacements(
            FocusChainSettings focusChainSettings) {
        Map<String, String> replacements = new HashMap<>();
        Boolean focusChainEnabled = focusChainSettings != null && focusChainSettings.isEnabled();

        replacements.put("newtask", Commands.newTaskToolResponse());
        replacements.put("smol", Commands.condenseToolResponse(focusChainEnabled));
        replacements.put("compact", Commands.condenseToolResponse(focusChainEnabled));
        replacements.put("newrule", Commands.newRuleToolResponse());
        replacements.put("reportbug", Commands.reportBugToolResponse());
        replacements.put("deep-planning", Commands.deepPlanningToolResponse(focusChainEnabled));
        replacements.put("subagent", Commands.subagentToolResponse());

        return replacements;
    }

    /** 获取已启用的工作流列表 */
    private static List<WorkflowInfo> getEnabledWorkflows(Map<String, Boolean> toggles) {
        if (toggles == null || toggles.isEmpty()) {
            return Collections.emptyList();
        }

        return toggles.entrySet().stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getValue()))
                .map(
                        entry -> {
                            String filePath = entry.getKey();
                            String fileName = filePath.replaceAll("^.*[/\\\\]", "");
                            return new WorkflowInfo(filePath, fileName);
                        })
                .collect(Collectors.toList());
    }

    /** 工作流信息 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class WorkflowInfo {
        private String fullPath;
        private String fileName;
    }

    /** 斜杠命令解析结果 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlashCommandParseResult {
        /** 处理后的文本 */
        private String processedText;

        /** 是否需要检查 clinerules 文件 */
        private boolean needsClinerulesFileCheck;
    }
}
