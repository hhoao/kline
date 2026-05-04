package com.hhoa.kline.core.core.prompts.systemprompt.variants.gpt5;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.*;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.ConfigTemplate;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * GPT-5 variant configuration Prompt optimized for GPT-5 model.
 *
 * @author hhoa
 */
@Slf4j
public class Gpt5VariantConfig {

    private static final List<SystemPromptSection> GPT5_COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.MCP,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.FEEDBACK,
                    SystemPromptSection.RULES,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> GPT5_TOOLS =
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
                            ClineDefaultTool.WEB_SEARCH,
                            ClineDefaultTool.MCP_USE,
                            ClineDefaultTool.MCP_ACCESS,
                            ClineDefaultTool.ASK,
                            ClineDefaultTool.ATTEMPT,
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.MCP_DOCS,
                            ClineDefaultTool.TODO,
                            ClineDefaultTool.GENERATE_EXPLANATION,
                            ClineDefaultTool.USE_SKILL,
                            ClineDefaultTool.AGENT)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createGpt5Variant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", "gpt-5");

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.GPT_5)
                        .description("Prompt optimized for GPT-5 model.")
                        .version(1)
                        .tags(Arrays.asList("gpt-5", "stable"))
                        .labels(labels)
                        .matcher(
                                context -> {
                                    var providerInfo = context.getProviderInfo();
                                    String modelId = getModelId(context);
                                    return isGPT5ModelFamily(modelId)
                                            && !modelId.toLowerCase().contains("chat")
                                            && isNextGenModelProvider(providerInfo)
                                            && !Boolean.TRUE.equals(
                                                    context.getEnableNativeToolCalls());
                                })
                        .componentOrder(GPT5_COMPONENT_ORDER)
                        .tools(GPT5_TOOLS)
                        .placeholders(placeholders)
                        .componentOverrides(Gpt5ComponentOverrides.getOverrides())
                        .config(PromptConfig.builder().build()));
    }

    public static void validateGpt5Variant() {
        try {
            PromptVariant variant = createGpt5Variant();
            ConfigTemplate.validateVariantConfig(variant, "gpt-5");
        } catch (Exception e) {
            log.error("Failed to validate GPT-5 variant configuration", e);
            throw e;
        }
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createGpt5Variant();
        ConfigTemplate.validateVariantConfig(built, "gpt-5");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
