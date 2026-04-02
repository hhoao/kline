package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 用户指令组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class UserInstructionsComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        String customInstructions =
                buildUserInstructions(
                        context.getGlobalClineRulesFileInstructions(),
                        context.getLocalClineRulesFileInstructions(),
                        context.getLocalCursorRulesFileInstructions(),
                        context.getLocalCursorRulesDirInstructions(),
                        context.getLocalWindsurfRulesFileInstructions(),
                        context.getLocalAgentsRulesFileInstructions(),
                        context.getClineIgnoreInstructions(),
                        context.getPreferredLanguageInstructions());

        if (customInstructions == null || customInstructions.trim().isEmpty()) {
            return null;
        }

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides()
                        .containsKey(SystemPromptSection.USER_INSTRUCTIONS)) {
            var override =
                    variant.getComponentOverrides().get(SystemPromptSection.USER_INSTRUCTIONS);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(
                template, context, Map.of("CUSTOM_INSTRUCTIONS", customInstructions));
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.USER_INSTRUCTIONS;
    }

    private String getTemplateText() {
        return """
                USER'S CUSTOM INSTRUCTIONS

                The following additional instructions are provided by the user, and should be followed to the best of your ability without interfering with the TOOL USE guidelines.

                {{CUSTOM_INSTRUCTIONS}}
                """;
    }

    private String buildUserInstructions(
            String globalClineRulesFileInstructions,
            String localClineRulesFileInstructions,
            String localCursorRulesFileInstructions,
            String localCursorRulesDirInstructions,
            String localWindsurfRulesFileInstructions,
            String localAgentsRulesFileInstructions,
            String clineIgnoreInstructions,
            String preferredLanguageInstructions) {
        List<String> customInstructions = new ArrayList<>();

        addIfPresent(customInstructions, preferredLanguageInstructions);
        addIfPresent(customInstructions, globalClineRulesFileInstructions);
        addIfPresent(customInstructions, localClineRulesFileInstructions);
        addIfPresent(customInstructions, localCursorRulesFileInstructions);
        addIfPresent(customInstructions, localCursorRulesDirInstructions);
        addIfPresent(customInstructions, localWindsurfRulesFileInstructions);
        addIfPresent(customInstructions, localAgentsRulesFileInstructions);
        addIfPresent(customInstructions, clineIgnoreInstructions);

        if (customInstructions.isEmpty()) {
            return null;
        }

        return String.join("\n\n", customInstructions);
    }

    private void addIfPresent(List<String> list, String value) {
        if (value != null && !value.trim().isEmpty()) {
            list.add(value);
        }
    }
}
