package com.hhoa.kline.core.core.context.instructions.userinstructions;

import lombok.Getter;

/** Skill 完整内容，包含指令 */
@Getter
public class SkillContent extends SkillMetadata {
    /** SKILL.md 正文内容（去除 frontmatter 后） */
    private final String instructions;

    public SkillContent(SkillMetadata metadata, String instructions) {
        super(
                metadata.getName(),
                metadata.getDescription(),
                metadata.getPath(),
                metadata.getSource());
        this.instructions = instructions;
    }
}
