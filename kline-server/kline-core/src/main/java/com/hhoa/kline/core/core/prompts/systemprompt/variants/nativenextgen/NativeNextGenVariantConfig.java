package com.hhoa.kline.core.core.prompts.systemprompt.variants.nativenextgen;

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
 * Native Next-Gen variant configuration - Next gen models with native tool calling
 *
 * @author hhoa
 */
@Slf4j
public class NativeNextGenVariantConfig {

    private static final List<SystemPromptSection> COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.TODO,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.FEEDBACK,
                    SystemPromptSection.RULES,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> TOOLS =
            Stream.of(
                            ClineDefaultTool.ASK,
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
                            ClineDefaultTool.MCP_ACCESS,
                            ClineDefaultTool.ATTEMPT,
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.MCP_DOCS,
                            ClineDefaultTool.TODO,
                            ClineDefaultTool.GENERATE_EXPLANATION,
                            ClineDefaultTool.USE_SKILL,
                            ClineDefaultTool.AGENT)
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toList());

    public static PromptVariant createVariant() {
        Map<String, Integer> labels = new HashMap<>();
        labels.put("stable", 1);
        labels.put("production", 1);
        labels.put("advanced", 1);
        labels.put("use_native_tools", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", ModelFamily.NATIVE_NEXT_GEN.name());

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.NATIVE_NEXT_GEN)
                        .description("Next gen models with native tool calling")
                        .version(1)
                        .tags(Arrays.asList("advanced", "production", "native_tools"))
                        .labels(labels)
                        .matcher(
                                context -> {
                                    if (!Boolean.TRUE.equals(context.getEnableNativeToolCalls())) {
                                        return false;
                                    }
                                    var providerInfo = context.getProviderInfo();
                                    if (!isNextGenModelProvider(providerInfo)) {
                                        return false;
                                    }
                                    String modelId = getModelId(context).toLowerCase();
                                    return !isGPT5ModelFamily(modelId)
                                            && isNextGenModelFamily(modelId);
                                })
                        .componentOrder(COMPONENT_ORDER)
                        .tools(TOOLS)
                        .placeholders(placeholders)
                        .componentOverrides(NativeNextGenComponentOverrides.getOverrides())
                        .config(PromptConfig.builder().build()));
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createVariant();
        ConfigTemplate.validateVariantConfig(built, "native-next-gen");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
