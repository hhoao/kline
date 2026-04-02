package com.hhoa.kline.core.core.context.instructions.userinstructions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** Skill 元数据 */
@Getter
@Builder
@AllArgsConstructor
public class SkillMetadata {
    /** Skill 名称（必须与目录名匹配） */
    private final String name;

    /** Skill 描述 */
    private final String description;

    /** SKILL.md 文件路径 */
    private final String path;

    /** 来源："global" 或 "project" */
    private final String source;
}
