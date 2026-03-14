package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Agent角色组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class AgentRoleComponent implements SystemPromptComponent {

    private static final String AGENT_ROLE =
            """
            You are Cline, \
            a highly skilled software engineer \
            with extensive knowledge in many programming languages, frameworks, design patterns, and best practices.
            """;

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        String template = AGENT_ROLE;

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.AGENT_ROLE)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.AGENT_ROLE);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }
        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.AGENT_ROLE;
    }
}
