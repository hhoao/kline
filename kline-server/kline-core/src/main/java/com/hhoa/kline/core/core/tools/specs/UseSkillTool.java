package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.UseSkillInput;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.handlers.UseSkillToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;
import java.util.function.Function;

/**
 * Use Skill 工具规格 - 加载并激活技能
 *
 * @author hhoa
 */
public final class UseSkillTool implements ToolSpecProvider<UseSkillInput> {

    private static final UseSkillToolHandler HANDLER = new UseSkillToolHandler();

    private static final String DESCRIPTION = "Load and activate a skill by name.";

    private static final String PROMPT =
            "Load and activate a skill by name. Skills provide specialized instructions for specific tasks. "
                    + "Use this tool ONCE when a user's request matches one of the available skill descriptions shown "
                    + "in the SKILLS section of your system prompt. After activation, follow the skill's instructions "
                    + "directly - do not call use_skill again.";

    @Override
    public String name() {
        return ClineDefaultTool.USE_SKILL.getValue();
    }

    @Override
    public String description(ModelFamily family) {
        return DESCRIPTION;
    }

    @Override
    public String prompt(ModelFamily family) {
        return PROMPT;
    }

    @Override
    public Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return context -> context.getSkills() != null && !context.getSkills().isEmpty();
    }

    @Override
    public Class<UseSkillInput> inputType(ModelFamily family) {
        return UseSkillInput.class;
    }

    @Override
    public ToolHandler<UseSkillInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
