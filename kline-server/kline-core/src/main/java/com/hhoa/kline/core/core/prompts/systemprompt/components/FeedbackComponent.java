package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 反馈组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class FeedbackComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        if (context.getFocusChainSettings() == null
                || !context.getFocusChainSettings().isEnabled()) {
            return null;
        }

        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.FEEDBACK)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.FEEDBACK);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.FEEDBACK;
    }

    private String getTemplateText() {
        return """
            If the user asks for help or wants to give feedback inform them of the following:
            - To give feedback, users should report the issue using the /reportbug slash command in the chat.

            When the user directly asks about Cline (eg 'can Cline do...', 'does Cline have...') or asks in second person (eg 'are you able...', 'can you do...'), first use the web_fetch tool to gather information to answer the question from Cline docs at https://docs.cline.bot.
              - The available sub-pages are `getting-started` (Intro for new coders, installing Cline and dev essentials), `model-selection` (Model Selection Guide, Custom Model Configs, Bedrock, Vertex, Codestral, LM Studio, Ollama), `features` (Auto approve, Checkpoints, Cline rules, Drag & Drop, Plan & Act, Workflows, etc), `task-management` (Task and Context Management in Cline), `prompt-engineering` (Improving your prompting skills, Prompt Engineering Guide), `cline-tools` (Cline Tools Reference Guide, New Task Tool, Remote Browser Support, Slash Commands), `mcp` (MCP Overview, Adding/Configuring Servers, Transport Mechanisms, MCP Dev Protocol), `enterprise` (Cloud provider integration, Security concerns, Custom instructions), `more-info` (Telemetry and other reference content)
              - Example: https://docs.cline.bot/features/auto-approve
            """;
    }
}
