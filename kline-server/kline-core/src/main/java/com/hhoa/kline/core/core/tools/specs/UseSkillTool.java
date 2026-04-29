package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.UseSkillInput;
import com.hhoa.kline.core.core.tools.handlers.UseSkillToolHandler;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.function.Function;

/**
 * Use Skill 工具规格 - 加载并激活技能
 *
 * @author hhoa
 */
public final class UseSkillTool extends BaseToolSpec
        implements ToolSpecProvider<UseSkillInput, UseSkillToolHandler> {

    private static final String DESCRIPTION =
            "Load and activate a skill by name. Skills provide specialized instructions for specific tasks. "
                    + "Use this tool ONCE when a user's request matches one of the available skill descriptions shown "
                    + "in the SKILLS section of your system prompt. After activation, follow the skill's instructions "
                    + "directly - do not call use_skill again.";

    @Override
    public String id() {
        return ClineDefaultTool.USE_SKILL.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> context.getSkills() != null && !context.getSkills().isEmpty();
    }
}
