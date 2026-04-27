package com.hhoa.kline.core.core.tools.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolParameterSpec;
import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** 构建单个工具在 system prompt 中的文本说明。 */
public final class ToolPromptBuilder {
    private ToolPromptBuilder() {}

    public static String render(
            ToolSpec config, List<String> registry, SystemPromptContext context) {
        if ((config.getParameters() == null || config.getParameters().isEmpty())
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

        List<ToolParameterSpec> params =
                config.getParameters() != null
                        ? new ArrayList<>(config.getParameters())
                        : new ArrayList<>();

        List<ToolParameterSpec> filteredParams =
                params.stream()
                        .filter(
                                p -> {
                                    if (p.getDependencies() != null
                                            && !p.getDependencies().isEmpty()) {
                                        if (!p.getDependencies().stream()
                                                .allMatch(registry::contains)) {
                                            return false;
                                        }
                                    }

                                    if (p.getContextRequirements() != null) {
                                        try {
                                            return Boolean.TRUE.equals(
                                                    p.getContextRequirements().apply(context));
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }

                                    return true;
                                })
                        .collect(Collectors.toList());

        List<String> additionalDesc =
                filteredParams.stream()
                        .map(ToolParameterSpec::getDescription)
                        .filter(desc -> desc != null && !desc.isBlank())
                        .collect(Collectors.toList());
        if (!additionalDesc.isEmpty()) {
            description.addAll(additionalDesc);
        }

        List<String> sections = new ArrayList<>();
        sections.add(title);
        sections.add(String.join("\n", description));
        sections.add(buildParametersSection(filteredParams, context));
        sections.add(buildUsageSection(displayName, filteredParams));

        return sections.stream()
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private static String buildParametersSection(
            List<ToolParameterSpec> params, SystemPromptContext context) {
        if (params == null || params.isEmpty()) {
            return "Parameters: None";
        }

        List<String> paramList =
                params.stream()
                        .map(
                                p -> {
                                    String requiredText = p.isRequired() ? "required" : "optional";
                                    String instruction = resolveInstruction(p, context);
                                    return String.format(
                                            "- %s: (%s) %s",
                                            p.getName(), requiredText, instruction);
                                })
                        .collect(Collectors.toList());

        List<String> sections = new ArrayList<>();
        sections.add("Parameters:");
        sections.addAll(paramList);
        return String.join("\n", sections);
    }

    private static String resolveInstruction(ToolParameterSpec param, SystemPromptContext context) {
        if (param.getInstructionFn() != null && context != null) {
            try {
                return param.getInstructionFn().apply(context);
            } catch (Exception e) {
                // fallback to static instruction
            }
        }
        return param.getInstruction() != null ? param.getInstruction() : "";
    }

    private static String buildUsageSection(String toolId, List<ToolParameterSpec> params) {
        List<String> usageSection = new ArrayList<>();
        usageSection.add("Usage:");
        String usageTag = "<" + toolId + ">";
        String usageEndTag = "</" + toolId + ">";

        usageSection.add(usageTag);

        if (params != null) {
            for (ToolParameterSpec param : params) {
                String usage = param.getUsage() != null ? param.getUsage() : "";
                usageSection.add(
                        String.format("<%s>%s</%s>", param.getName(), usage, param.getName()));
            }
        }

        usageSection.add(usageEndTag);
        return String.join("\n", usageSection);
    }
}
