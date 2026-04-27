package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.List;

/**
 * Use Skill 工具规格 - 加载并激活技能
 *
 * @author hhoa
 */
public class UseSkillTool extends BaseToolSpec {

    public static ToolSpec create(ModelFamily modelFamily) {
        return ToolSpec.builder()
                .variant(modelFamily)
                .id(ClineDefaultTool.USE_SKILL.getValue())
                .name(ClineDefaultTool.USE_SKILL.getValue())
                .description(
                        "Load and activate a skill by name. Skills provide specialized instructions for specific tasks. "
                                + "Use this tool ONCE when a user's request matches one of the available skill descriptions shown "
                                + "in the SKILLS section of your system prompt. After activation, follow the skill's instructions "
                                + "directly - do not call use_skill again.")
                .contextRequirements(
                        context -> context.getSkills() != null && !context.getSkills().isEmpty())
                .parameters(
                        List.of(
                                createParameter(
                                        "skill_name",
                                        true,
                                        "The name of the skill to activate (must match exactly one of the available skill names)",
                                        null)))
                .build();
    }
}
