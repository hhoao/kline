package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import lombok.RequiredArgsConstructor;

import java.text.MessageFormat;

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

        return MessageFormat.format("""
            SKILLS
            
            The following skills provide specialized instructions for specific tasks. When a user''s request matches a skill description, use the use_skill tool to load and activate the skill.
            
            Available skills:
            {0}
            To use a skill:
            1. Match the user''s request to a skill based on its description
            2. Call use_skill with the skill_name parameter set to the exact skill name
            3. Follow the instructions returned by the tool""",
            skillsList);
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.SKILLS;
    }
}
