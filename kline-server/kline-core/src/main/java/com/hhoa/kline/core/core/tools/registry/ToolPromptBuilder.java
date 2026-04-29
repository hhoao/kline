package com.hhoa.kline.core.core.tools.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSchema;
import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 构建单个工具在 system prompt 中的文本说明。 */
public final class ToolPromptBuilder {
    private ToolPromptBuilder() {}

    public static String render(
            ToolSpec config, List<String> registry, SystemPromptContext context) {
        if (ToolSchema.properties(config.getInputSchema()).isEmpty()
                && (config.getDescription() == null || config.getDescription().isBlank())) {
            return "";
        }

        String displayName =
                config.getName() != null && !config.getName().isBlank()
                        ? config.getName()
                        : config.getId();
        String title = "## " + displayName;
        List<String> description = new ArrayList<>();
        description.add(
                "Description: " + (config.getDescription() != null ? config.getDescription() : ""));

        Map<String, Map<String, Object>> params = ToolSchema.properties(config.getInputSchema());
        List<Map.Entry<String, Map<String, Object>>> filteredParams =
                params.entrySet().stream()
                        .filter(
                                entry ->
                                        ToolSchema.parameterEnabled(
                                                entry.getValue(), context, registry))
                        .collect(Collectors.toList());

        List<String> additionalDesc =
                filteredParams.stream()
                        .map(
                                entry ->
                                        String.valueOf(
                                                entry.getValue().getOrDefault("description", "")))
                        .filter(desc -> desc != null && !desc.isBlank())
                        .collect(Collectors.toList());
        if (!additionalDesc.isEmpty()) {
            description.addAll(additionalDesc);
        }

        List<String> sections = new ArrayList<>();
        sections.add(title);
        sections.add(String.join("\n", description));
        sections.add(buildParametersSection(config, filteredParams, context));
        sections.add(buildUsageSection(displayName, filteredParams));

        return sections.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static String buildParametersSection(
            ToolSpec spec,
            List<Map.Entry<String, Map<String, Object>>> params,
            SystemPromptContext context) {
        if (params == null || params.isEmpty()) {
            return "Parameters: None";
        }

        List<String> paramList =
                params.stream()
                        .map(
                                entry -> {
                                    String requiredText =
                                            ToolSchema.required(spec.getInputSchema())
                                                            .contains(entry.getKey())
                                                    ? "required"
                                                    : "optional";
                                    String instruction =
                                            ToolSchema.instruction(entry.getValue(), context);
                                    return String.format(
                                            "- %s: (%s) %s",
                                            entry.getKey(), requiredText, instruction);
                                })
                        .collect(Collectors.toList());

        List<String> sections = new ArrayList<>();
        sections.add("Parameters:");
        sections.addAll(paramList);
        return String.join("\n", sections);
    }

    private static String buildUsageSection(
            String toolId, List<Map.Entry<String, Map<String, Object>>> params) {
        List<String> usageSection = new ArrayList<>();
        usageSection.add("Usage:");
        String usageTag = "<" + toolId + ">";
        String usageEndTag = "</" + toolId + ">";

        usageSection.add(usageTag);

        if (params != null) {
            for (Map.Entry<String, Map<String, Object>> param : params) {
                String usage = ToolSchema.usage(param.getValue());
                usageSection.add(
                        String.format("<%s>%s</%s>", param.getKey(), usage, param.getKey()));
            }
        }

        usageSection.add(usageEndTag);
        return String.join("\n", usageSection);
    }
}
