package com.hhoa.kline.core.core.prompts.systemprompt.variants.generic;

import static com.hhoa.kline.core.core.prompts.systemprompt.ModelFamilyMatchers.*;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptConfig;
import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.variants.VariantValidator;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * Generic variant configuration The fallback prompt for generic use cases and models.
 *
 * @author hhoa
 */
@Slf4j
public class GenericVariantConfig {

    private static final List<SystemPromptSection> GENERIC_COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.MCP,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.RULES,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> GENERIC_TOOLS =
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
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.MCP_DOCS,
                            ClineDefaultTool.TODO,
                            ClineDefaultTool.GENERATE_EXPLANATION,
                            ClineDefaultTool.USE_SKILL,
                            ClineDefaultTool.AGENT)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createGenericVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("fallback", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", "generic");

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.GENERIC)
                        .description("The fallback prompt for generic use cases and models.")
                        .version(1)
                        .tags(Arrays.asList("fallback", "stable"))
                        .labels(labels)
                        .matcher(
                                context -> {
                                    var providerInfo = context.getProviderInfo();
                                    if (providerInfo == null
                                            || providerInfo.getProviderId() == null
                                            || providerInfo.getModel() == null) {
                                        return true;
                                    }
                                    String modelId = getModelId(context);
                                    return !(("compact".equals(providerInfo.getCustomPrompt())
                                                    && isLocalModel(providerInfo))
                                            || (isNextGenModelProvider(providerInfo)
                                                    && isNextGenModelFamily(modelId))
                                            || isGLMModelFamily(modelId)
                                            || isTrinityModelFamily(modelId));
                                })
                        .componentOrder(GENERIC_COMPONENT_ORDER)
                        .tools(GENERIC_TOOLS)
                        .placeholders(placeholders)
                        .config(PromptConfig.builder().build()));
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createGenericVariant();
        VariantValidator.ValidationResult validationResult =
                VariantValidator.validateVariant(built, true);
        if (!validationResult.isValid()) {
            log.error(
                    "Generic variant configuration validation failed: {}",
                    validationResult.getErrors());
            throw new RuntimeException(
                    "Invalid generic variant configuration: "
                            + String.join(", ", validationResult.getErrors()));
        }
        if (!validationResult.getWarnings().isEmpty()) {
            log.warn("Generic variant configuration warnings: {}", validationResult.getWarnings());
        }
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
