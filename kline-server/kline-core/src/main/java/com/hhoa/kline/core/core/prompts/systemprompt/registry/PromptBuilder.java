package com.hhoa.kline.core.core.prompts.systemprompt.registry;

import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.Placeholders;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import com.hhoa.kline.core.core.tools.registry.ToolPromptBuilder;
import com.hhoa.kline.core.core.tools.registry.ToolSpecManager;
import com.hhoa.kline.core.core.tools.subagent.AgentConfigLoader;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 提示构建器
 *
 * @author hhoa
 */
@RequiredArgsConstructor
@Slf4j
public class PromptBuilder {

    private static final List<String> STANDARD_PLACEHOLDER_KEYS;

    static {
        STANDARD_PLACEHOLDER_KEYS = new ArrayList<>(Placeholders.STANDARD_PLACEHOLDERS.values());
    }

    private final ComponentRegistry componentRegistry;
    private final TemplateEngine templateEngine;

    /** 构建系统提示 */
    public String build(PromptVariant variant, SystemPromptContext context) {
        Map<String, String> componentSections = buildComponents(variant, context);

        Map<String, String> placeholderValues =
                preparePlaceholders(variant, context, componentSections);

        String prompt =
                templateEngine.resolve(variant.getBaseTemplate(), context, placeholderValues);

        return postProcess(prompt);
    }

    /** 构建所有组件 */
    private Map<String, String> buildComponents(
            PromptVariant variant, SystemPromptContext context) {
        Map<String, String> sections = new HashMap<>();

        if (variant.getComponentOrder() == null) {
            return sections;
        }

        for (SystemPromptSection section : variant.getComponentOrder()) {
            String componentId = section.name();
            var componentOpt = componentRegistry.get(section);

            if (componentOpt.isEmpty()) {
                log.warn("Warning: Component '{}' not found", componentId);
                continue;
            }

            try {
                String result = componentOpt.get().apply(variant, context);
                if (result != null && !result.trim().isEmpty()) {
                    sections.put(componentId, result);
                }
            } catch (Exception e) {
                log.warn(
                        "Warning: Failed to build component '{}': {}",
                        componentId,
                        e.getMessage(),
                        e);
            }
        }

        return sections;
    }

    /** 准备占位符值 */
    private Map<String, String> preparePlaceholders(
            PromptVariant variant,
            SystemPromptContext context,
            Map<String, String> componentSections) {
        Map<String, String> placeholders = new LinkedHashMap<>();

        if (variant.getPlaceholders() != null) {
            variant.getPlaceholders()
                    .forEach((key, value) -> placeholders.put(key, value != null ? value : ""));
        }

        placeholders.put(
                Placeholders.STANDARD_PLACEHOLDERS.get("CWD"),
                context.getCwd() != null ? context.getCwd() : System.getProperty("user.dir"));
        placeholders.put(
                Placeholders.STANDARD_PLACEHOLDERS.get("SUPPORTS_BROWSER"),
                String.valueOf(Boolean.TRUE.equals(context.getSupportsBrowserUse())));
        placeholders.put(
                Placeholders.STANDARD_PLACEHOLDERS.get("MODEL_FAMILY"), getModelFamily(context));
        placeholders.put(
                Placeholders.STANDARD_PLACEHOLDERS.get("CURRENT_DATE"), LocalDate.now().toString());

        placeholders.putAll(componentSections);

        for (String key : STANDARD_PLACEHOLDER_KEYS) {
            if (!placeholders.containsKey(key)) {
                placeholders.put(key, componentSections.getOrDefault(key, ""));
            }
        }

        // 合并运行时占位符（优先级最高）
        if (context.getRuntimePlaceholders() != null) {
            context.getRuntimePlaceholders()
                    .forEach(
                            (key, value) ->
                                    placeholders.put(key, value != null ? value.toString() : ""));
        }

        return placeholders;
    }

    /** 从providerInfo获取模型家族 */
    private String getModelFamily(SystemPromptContext context) {
        if (context.getProviderInfo() == null || context.getProviderInfo().getModel() == null) {
            return ModelFamily.GENERIC.getValue();
        }

        String modelId = context.getProviderInfo().getModel().getId().toLowerCase();

        if (modelId.contains("gpt-5") || modelId.contains("gpt5")) {
            return ModelFamily.GPT_5.getValue();
        }

        if (isNextGenModelFamily(modelId)) {
            return ModelFamily.NEXT_GEN.getValue();
        }

        if (isGLMModelFamily(modelId)) {
            return ModelFamily.GLM.getValue();
        }

        if ("compact".equals(context.getProviderInfo().getCustomPrompt())
                && isLocalModel(context.getProviderInfo())) {
            return ModelFamily.XS.getValue();
        }

        return ModelFamily.GENERIC.getValue();
    }

    private boolean isNextGenModelFamily(String modelId) {
        if (modelId == null) {
            return false;
        }
        String id = modelId.toLowerCase();
        return id.contains("sonnet-4")
                || id.contains("opus-4")
                || id.contains("4-sonnet")
                || id.contains("4-opus")
                || id.contains("haiku-4")
                || id.contains("4-5-haiku")
                || id.contains("4.5-haiku")
                || id.contains("claude-4")
                || id.contains("gemini-2.5")
                || id.contains("grok-4")
                || id.contains("gpt-5")
                || id.contains("gpt5");
    }

    private boolean isGLMModelFamily(String modelId) {
        if (modelId == null) {
            return false;
        }
        String id = modelId.toLowerCase();
        return id.contains("glm-4.6")
                || id.contains("glm-4.5")
                || id.contains("z-ai/glm")
                || id.contains("zai-org/glm");
    }

    private boolean isLocalModel(SystemPromptContext.ApiProviderInfo providerInfo) {
        if (providerInfo == null || providerInfo.getProviderId() == null) {
            return false;
        }
        String providerId = providerInfo.getProviderId().toLowerCase().trim();
        return "lmstudio".equals(providerId) || "ollama".equals(providerId);
    }

    /** 获取工具提示 */
    public static List<String> getToolsPrompts(PromptVariant variant, SystemPromptContext context) {
        ModelFamily family = variant.getFamily();
        if (family == null) {
            family = ModelFamily.GENERIC;
        }

        List<ClineToolSpec> resolvedTools;

        if (variant.getTools() != null && !variant.getTools().isEmpty()) {
            List<String> requestedIds = new ArrayList<>(variant.getTools());
            resolvedTools =
                    ToolSpecManager.getToolsForVariantWithFallback(family, requestedIds, context);

            Map<String, ClineToolSpec> toolMap =
                    resolvedTools.stream()
                            .collect(
                                    Collectors.toMap(
                                            ClineToolSpec::getId,
                                            tool -> tool,
                                            (existing, replacement) -> existing));
            resolvedTools =
                    requestedIds.stream()
                            .map(toolMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
        } else {
            resolvedTools = ToolSpecManager.getToolSpecs(family, context);
            if (resolvedTools == null || resolvedTools.isEmpty()) {
                resolvedTools = ToolSpecManager.getToolSpecs(ModelFamily.GENERIC, context);
            }
            if (resolvedTools == null || resolvedTools.isEmpty()) {
                return Collections.emptyList();
            }
            resolvedTools =
                    resolvedTools.stream()
                            .sorted(
                                    (a, b) -> {
                                        String idA = a.getId() != null ? a.getId() : "";
                                        String idB = b.getId() != null ? b.getId() : "";
                                        return idA.compareTo(idB);
                                    })
                            .collect(Collectors.toList());
        }

        resolvedTools = mergeDynamicSubagentToolSpecs(resolvedTools, variant, context);

        List<ClineToolSpec> enabledTools =
                resolvedTools.stream()
                        .filter(
                                tool -> {
                                    if (tool.getContextRequirements() != null) {
                                        try {
                                            return Boolean.TRUE.equals(
                                                    tool.getContextRequirements().apply(context));
                                        } catch (Exception e) {
                                            return false;
                                        }
                                    }
                                    return true;
                                })
                        .toList();

        List<String> ids =
                enabledTools.stream()
                        .map(ClineToolSpec::getId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(Collectors.toList());

        return enabledTools.stream()
                .map(spec -> ToolPromptBuilder.render(spec, ids, context))
                .filter(toolPrompt -> toolPrompt != null && !toolPrompt.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 与 Cline {@code ClineToolSet.getDynamicSubagentToolSpecs} / {@code getEnabledToolSpecs}
     * 一致：在启用子代理且存在 YAML 配置时，用动态 {@code use_subagent_*} 工具替换单一的 {@code use_subagents}。
     */
    private static List<ClineToolSpec> mergeDynamicSubagentToolSpecs(
            List<ClineToolSpec> resolvedTools, PromptVariant variant, SystemPromptContext context) {
        if (!Boolean.TRUE.equals(context.getSubagentsEnabled())
                || Boolean.TRUE.equals(context.getIsSubagentRun())) {
            return resolvedTools;
        }
        List<String> requestedIds =
                variant.getTools() != null && !variant.getTools().isEmpty()
                        ? variant.getTools()
                        : List.of();
        boolean shouldInclude =
                requestedIds.isEmpty()
                        || requestedIds.contains(ClineDefaultTool.USE_SUBAGENTS.getValue());
        if (!shouldInclude) {
            return resolvedTools;
        }
        List<AgentConfigLoader.AgentConfigWithToolName> agentConfigs =
                AgentConfigLoader.getInstance().getAllCachedConfigsWithToolNames();
        if (agentConfigs.isEmpty()) {
            return resolvedTools;
        }
        List<ClineToolSpec> filtered = new ArrayList<>();
        for (ClineToolSpec t : resolvedTools) {
            if (!ClineDefaultTool.USE_SUBAGENTS.getValue().equals(t.getId())) {
                filtered.add(t);
            }
        }
        ModelFamily family =
                variant.getFamily() != null ? variant.getFamily() : ModelFamily.GENERIC;
        for (AgentConfigLoader.AgentConfigWithToolName entry : agentConfigs) {
            filtered.add(buildDynamicSubagentSpec(entry, family));
        }
        return filtered;
    }

    private static ClineToolSpec buildDynamicSubagentSpec(
            AgentConfigLoader.AgentConfigWithToolName entry, ModelFamily family) {
        ClineToolSpec.ClineToolSpecParameter promptParam =
                ClineToolSpec.ClineToolSpecParameter.builder()
                        .name("prompt")
                        .required(true)
                        .instruction(
                                "Helpful instruction for the task that the subagent will perform.")
                        .build();

        return ClineToolSpec.builder()
                .variant(family)
                .id(ClineDefaultTool.USE_SUBAGENTS.getValue())
                .name(entry.toolName())
                .description(
                        String.format(
                                "Use the \"%s\" subagent: %s",
                                entry.config().name(), entry.config().description()))
                .contextRequirements(
                        ctx ->
                                Boolean.TRUE.equals(ctx.getSubagentsEnabled())
                                        && !Boolean.TRUE.equals(ctx.getIsSubagentRun()))
                .parameters(List.of(promptParam))
                .build();
    }

    /** 后处理提示内容 */
    private String postProcess(String prompt) {
        if (prompt == null || prompt.isEmpty()) {
            return "";
        }

        String result =
                prompt.replaceAll(
                                "\n\\s*\n\\s*\n", "\n\n") // Remove multiple consecutive empty lines
                        .trim() // Remove leading/trailing whitespace
                        .replaceAll("====+\\s*$", "") // Remove trailing ==== after trim
                        .replaceAll(
                                "\n====+\\s*\n+\\s*====+\n",
                                "\n====\n") // Remove empty sections between separators
                        .replaceAll(
                                "====\\s*\n\\s*====\\s*\n",
                                "====\n") // Remove consecutive empty sections
                        .replaceAll(
                                "(?m)^##\\s*$[\\r\\n]*",
                                "") // Remove empty section headers (## with no content)
                        .replaceAll("(?m)\\n##\\s*$[\\r\\n]*", "");

        result = addNewlinesAfterSeparator(result, prompt);

        result = addNewlinesBeforeSeparator(result, prompt);

        result = result.replaceAll("\n\\s*\n\\s*\n", "\n\n");

        return result.trim();
    }

    /** 在分隔符之后添加空行 */
    private String addNewlinesAfterSeparator(String text, String originalText) {
        Pattern pattern = Pattern.compile("====+\\n(?!\\n)([^\\n])");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group(0);
            int offset = matcher.start();

            int contextOffset = Math.min(offset, originalText.length());
            int beforeStart = Math.max(0, contextOffset - 50);
            int afterEnd = Math.min(originalText.length(), contextOffset + 50);
            String beforeContext = originalText.substring(beforeStart, contextOffset);
            String afterContext = originalText.substring(contextOffset, afterEnd);
            String context = beforeContext + afterContext;
            boolean isDiffLike =
                    context.contains("SEARCH")
                            || context.contains("REPLACE")
                            || context.contains("+++++++")
                            || context.contains("-------");

            String replacement = isDiffLike ? match : match.replace("\n", "\n\n");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 在分隔符之前添加空行 */
    private String addNewlinesBeforeSeparator(String text, String originalText) {
        Pattern pattern = Pattern.compile("([^\\n])\\n(?!\\n)====+");
        Matcher matcher = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group(0);
            int offset = matcher.start();

            int contextOffset = Math.min(offset, originalText.length());
            int beforeStart = Math.max(0, contextOffset - 50);
            int afterEnd = Math.min(originalText.length(), contextOffset + 50);
            String beforeContext = originalText.substring(beforeStart, contextOffset);
            String afterContext = originalText.substring(contextOffset, afterEnd);
            String context = beforeContext + afterContext;
            boolean isDiffLike =
                    context.contains("SEARCH")
                            || context.contains("REPLACE")
                            || context.contains("+++++++")
                            || context.contains("-------");

            String replacement;
            if (isDiffLike) {
                replacement = match;
            } else {
                String prevChar = matcher.group(1);
                replacement =
                        prevChar
                                + "\n\n"
                                + match.substring(prevChar.length() + 1).replace("\n", "");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 获取构建元数据 */
    public BuildMetadata getBuildMetadata(PromptVariant variant) {
        List<String> componentsUsed =
                variant.getComponentOrder() != null
                        ? variant.getComponentOrder().stream()
                                .map(SystemPromptSection::name)
                                .collect(Collectors.toList())
                        : Collections.emptyList();

        List<String> placeholdersResolved =
                templateEngine.extractPlaceholders(variant.getBaseTemplate());

        return new BuildMetadata(
                variant.getId(),
                variant.getVersion() != null ? variant.getVersion() : 1,
                componentsUsed,
                placeholdersResolved);
    }

    public record BuildMetadata(
            String variantId,
            int version,
            List<String> componentsUsed,
            List<String> placeholdersResolved) {}
}
