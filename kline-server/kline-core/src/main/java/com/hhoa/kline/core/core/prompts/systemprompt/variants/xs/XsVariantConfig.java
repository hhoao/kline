package com.hhoa.kline.core.core.prompts.systemprompt.variants.xs;

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
 * XS variant configuration Compact models with limited context windows. Streamlined for efficiency
 * with essential tools only.
 *
 * @author hhoa
 */
@Slf4j
public class XsVariantConfig {

    private static final List<SystemPromptSection> XS_COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.RULES,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.EDITING_FILES,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> XS_TOOLS =
            Stream.of(
                            ClineDefaultTool.BASH,
                            ClineDefaultTool.FILE_READ,
                            ClineDefaultTool.FILE_NEW,
                            ClineDefaultTool.FILE_EDIT,
                            ClineDefaultTool.SEARCH,
                            ClineDefaultTool.ASK,
                            ClineDefaultTool.ATTEMPT,
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.AGENT)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createXsVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);
        labels.put("advanced", 1);
        labels.put("use_native_tools", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", "xs");

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.XS)
                        .description(
                                "Compact models with limited context windows. Streamlined for efficiency with essential tools only.")
                        .version(1)
                        .tags(Arrays.asList("local", "xs", "compact", "native_tools"))
                        .labels(labels)
                        .matcher(
                                context -> {
                                    var providerInfo = context.getProviderInfo();
                                    if (!isLocalModel(providerInfo)) {
                                        return false;
                                    }
                                    return "compact".equals(providerInfo.getCustomPrompt());
                                })
                        .componentOrder(XS_COMPONENT_ORDER)
                        .tools(XS_TOOLS)
                        .placeholders(placeholders)
                        .componentOverrides(XsComponentOverrides.getOverrides())
                        .config(PromptConfig.builder().build()));
    }

    public static void validateXsVariant() {
        try {
            PromptVariant variant = createXsVariant();
            ConfigTemplate.validateVariantConfig(variant, "xs");
        } catch (Exception e) {
            log.error("Failed to validate XS variant configuration", e);
            throw e;
        }
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createXsVariant();
        ConfigTemplate.validateVariantConfig(built, "xs");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
