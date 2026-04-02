package com.hhoa.kline.core.core.prompts.systemprompt.variants.hermes;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.*;

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
 * Hermes variant configuration - Prompt optimized for Hermes-4 model
 *
 * @author hhoa
 */
@Slf4j
public class HermesVariantConfig {

    private static final List<SystemPromptSection> COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.RULES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.TODO,
                    SystemPromptSection.MCP,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> TOOLS =
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
                            ClineDefaultTool.TODO,
                            ClineDefaultTool.GENERATE_EXPLANATION,
                            ClineDefaultTool.USE_SKILL,
                            ClineDefaultTool.USE_SUBAGENTS)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", ModelFamily.HERMES.name());

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.HERMES)
                        .description(
                                "Prompt optimized for Hermes-4 model with advanced agentic capabilities.")
                        .version(1)
                        .tags(Arrays.asList("hermes", "stable"))
                        .labels(labels)
                        .matcher(context -> isHermesModelFamily(getModelId(context)))
                        .componentOrder(COMPONENT_ORDER)
                        .componentOverrides(HermesComponentOverrides.getOverrides())
                        .tools(TOOLS)
                        .placeholders(placeholders)
                        .config(PromptConfig.builder().build()));
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createVariant();
        ConfigTemplate.validateVariantConfig(built, "hermes");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
