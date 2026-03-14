package com.hhoa.kline.core.core.prompts.systemprompt.variants.glm;

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
 * GLM variant configuration Prompt optimized for GLM-4.6 model with advanced agentic capabilities.
 *
 * @author hhoa
 */
@Slf4j
public class GlmVariantConfig {

    private static final List<SystemPromptSection> GLM_COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.COMPLETE_TRUNCATED_CONTENT,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.RULES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CLI_SUBAGENTS,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.TODO,
                    SystemPromptSection.MCP,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS);

    private static final List<String> GLM_TOOLS =
            Stream.of(
                            ClineDefaultTool.BASH,
                            ClineDefaultTool.FILE_READ,
                            ClineDefaultTool.FILE_NEW,
                            ClineDefaultTool.FILE_EDIT,
                            ClineDefaultTool.SEARCH,
                            ClineDefaultTool.LIST_FILES,
                            ClineDefaultTool.LIST_CODE_DEF,
                            ClineDefaultTool.BROWSER,
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

    public static PromptVariant createGlmVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", "glm");

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.GLM)
                        .description(
                                "Prompt optimized for GLM-4.6 model with advanced agentic capabilities.")
                        .version(1)
                        .tags(Arrays.asList("glm", "stable"))
                        .labels(labels)
                        .componentOrder(GLM_COMPONENT_ORDER)
                        .tools(GLM_TOOLS)
                        .placeholders(placeholders)
                        .config(PromptConfig.builder().build()));
    }

    public static void validateGlmVariant() {
        try {
            PromptVariant variant = createGlmVariant();
            ConfigTemplate.validateVariantConfig(variant, "glm");
        } catch (Exception e) {
            log.error("Failed to validate GLM variant configuration", e);
            throw e;
        }
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createGlmVariant();
        ConfigTemplate.validateVariantConfig(built, "glm");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
