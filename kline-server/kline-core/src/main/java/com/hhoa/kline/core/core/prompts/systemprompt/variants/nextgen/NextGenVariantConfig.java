package com.hhoa.kline.core.core.prompts.systemprompt.variants.nextgen;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.ConfigTemplate;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Next-gen variant configuration Prompt tailored to newer frontier models with smarter agentic
 * capabilities.
 *
 * @author hhoa
 */
@Slf4j
public class NextGenVariantConfig {

    private static final List<SystemPromptSection> NEXT_GEN_COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.COMPLETE_TRUNCATED_CONTENT,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.TODO,
                    SystemPromptSection.MCP,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CLI_SUBAGENTS,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.FEEDBACK,
                    SystemPromptSection.RULES,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS);

    private static final List<String> NEXT_GEN_TOOLS =
            Stream.of(
                            ClineDefaultTool.BASH,
                            ClineDefaultTool.FILE_READ,
                            ClineDefaultTool.FILE_NEW,
                            ClineDefaultTool.FILE_EDIT,
                            ClineDefaultTool.SEARCH,
                            ClineDefaultTool.LIST_FILES,
                            ClineDefaultTool.LIST_CODE_DEF,
                            ClineDefaultTool.BROWSER,
                            ClineDefaultTool.WEB_FETCH,
                            ClineDefaultTool.MCP_USE,
                            ClineDefaultTool.MCP_ACCESS,
                            ClineDefaultTool.ASK,
                            ClineDefaultTool.ATTEMPT,
                            ClineDefaultTool.NEW_TASK,
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.MCP_DOCS,
                            ClineDefaultTool.TODO)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createNextGenVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);
        labels.put("advanced", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", ModelFamily.NEXT_GEN.name());

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.NEXT_GEN)
                        .description(
                                "Prompt tailored to newer frontier models with smarter agentic capabilities.")
                        .version(1)
                        .tags(Arrays.asList("next-gen", "advanced", "production"))
                        .labels(labels)
                        .componentOrder(NEXT_GEN_COMPONENT_ORDER)
                        .tools(NEXT_GEN_TOOLS)
                        .placeholders(placeholders)
                        .config(PromptConfig.builder().build()));
    }

    public static void validateNextGenVariant() {
        try {
            PromptVariant variant = createNextGenVariant();
            ConfigTemplate.validateVariantConfig(variant, "next-gen");
        } catch (Exception e) {
            log.error("Failed to validate Next-gen variant configuration", e);
            throw e;
        }
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createNextGenVariant();
        ConfigTemplate.validateVariantConfig(built, "next-gen");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
