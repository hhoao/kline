package com.hhoa.kline.core.core.prompts.systemprompt.variants.nativegpt51;

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
 * Native GPT-5.1 variant configuration - GPT-5.1 and GPT-5.2 with native tool use support
 *
 * @author hhoa
 */
@Slf4j
public class NativeGpt51VariantConfig {

    private static final List<SystemPromptSection> COMPONENT_ORDER =
            Arrays.asList(
                    SystemPromptSection.AGENT_ROLE,
                    SystemPromptSection.TOOL_USE,
                    SystemPromptSection.TASK_PROGRESS,
                    SystemPromptSection.ACT_VS_PLAN,
                    SystemPromptSection.CAPABILITIES,
                    SystemPromptSection.FEEDBACK,
                    SystemPromptSection.RULES,
                    SystemPromptSection.SYSTEM_INFO,
                    SystemPromptSection.OBJECTIVE,
                    SystemPromptSection.USER_INSTRUCTIONS,
                    SystemPromptSection.SKILLS);

    private static final List<String> TOOLS =
            Stream.of(
                            ClineDefaultTool.BASH,
                            ClineDefaultTool.FILE_READ,
                            ClineDefaultTool.APPLY_PATCH,
                            ClineDefaultTool.SEARCH,
                            ClineDefaultTool.LIST_FILES,
                            ClineDefaultTool.LIST_CODE_DEF,
                            ClineDefaultTool.BROWSER,
                            ClineDefaultTool.WEB_FETCH,
                            ClineDefaultTool.WEB_SEARCH,
                            ClineDefaultTool.MCP_ACCESS,
                            ClineDefaultTool.ASK,
                            ClineDefaultTool.ATTEMPT,
                            ClineDefaultTool.NEW_TASK,
                            ClineDefaultTool.PLAN_MODE,
                            ClineDefaultTool.ACT_MODE,
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
        labels.put("advanced", 1);
        labels.put("use_native_tools", 1);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("MODEL_FAMILY", ModelFamily.NATIVE_GPT_5_1.name());

        return PromptVariant.buildAndValidate(
                PromptVariant.builder()
                        .family(ModelFamily.NATIVE_GPT_5_1)
                        .description(
                                "Prompt tailored to GPT-5.1 and GPT-5.2 with native tool use support")
                        .version(1)
                        .tags(
                                Arrays.asList(
                                        "gpt",
                                        "gpt-5-1",
                                        "gpt-5-2",
                                        "advanced",
                                        "production",
                                        "native_tools"))
                        .labels(labels)
                        .matcher(context -> {
                            if (!Boolean.TRUE.equals(context.getEnableNativeToolCalls())) {
                                return false;
                            }
                            var providerInfo = context.getProviderInfo();
                            String modelId = getModelId(context);
                            if (modelId.toLowerCase().contains("chat")) {
                                return false;
                            }
                            return (isGPT51Model(modelId) || isGPT52Model(modelId))
                                    && isNextGenModelProvider(providerInfo);
                        })
                        .componentOrder(COMPONENT_ORDER)
                        .tools(TOOLS)
                        .placeholders(placeholders)
                        .componentOverrides(NativeGpt51ComponentOverrides.getOverrides())
                        .config(PromptConfig.builder().build()));
    }

    private static final PromptVariant CONFIG;

    static {
        PromptVariant built = createVariant();
        ConfigTemplate.validateVariantConfig(built, "native-gpt-5-1");
        CONFIG = built;
    }

    public static PromptVariant getConfig() {
        return CONFIG;
    }
}
