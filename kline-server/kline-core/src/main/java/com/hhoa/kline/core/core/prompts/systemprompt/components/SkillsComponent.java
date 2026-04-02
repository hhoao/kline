package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import lombok.RequiredArgsConstructor;

/**
 * 技能组件
 *
 * @author hhoa
 */
@RequiredArgsConstructor
public class SkillsComponent implements SystemPromptComponent {

    private final TemplateEngine templateEngine;

    @Override
    public String apply(PromptVariant variant, SystemPromptContext context) {
        var skills = context.getSkills();
        if (skills == null || skills.isEmpty()) {
            return null;
        }

        StringBuilder skillsList = new StringBuilder();
        for (var skill : skills) {
            skillsList.append("  - \"").append(skill.getName()).append("\": ");
            skillsList.append(skill.getDescription()).append("\n");
        }

        return "SKILLS\n\n"
                + "The following skills provide specialized instructions for specific tasks. "
                + "When a user's request matches a skill description, use the use_skill tool to "
                + "load and activate the skill.\n\n"
                + "Available skills:\n"
                + skillsList
                + "\n"
                + "To use a skill:\n"
                + "1. Match the user's request to a skill based on its description\n"
                + "2. Call use_skill with the skill_name parameter set to the exact skill name\n"
                + "3. Follow the instructions returned by the tool";
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.SKILLS;
    }
}
