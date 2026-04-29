package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.context.instructions.userinstructions.SkillContent;
import com.hhoa.kline.core.core.context.instructions.userinstructions.SkillDiscovery;
import com.hhoa.kline.core.core.context.instructions.userinstructions.SkillMetadata;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.core.core.tools.args.UseSkillInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Use Skill 工具处理器 - 加载并激活技能。 */
public class UseSkillToolHandler implements ToolHandler<UseSkillInput> {
    @Override
    public String getDescription(ToolUse block) {
        String skillName = HandlerUtils.getStringParam(block, "skill_name");
        return skillName != null
                ? "[" + block.getName() + " for \"" + skillName + "\"]"
                : "[" + block.getName() + "]";
    }

    @Override
    public void handlePartialBlock(UseSkillInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String skillName = input.skillName();
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool", "useSkill");
        payload.put("path", skillName != null ? skillName : "");
        ui.say(ClineSay.TOOL, JsonUtils.toJsonString(payload), null, null, true, null);
    }

    @Override
    public ToolExecuteResult execute(UseSkillInput input, ToolContext context, ToolUse block) {
        String skillName = input.skillName();
        if (skillName == null || skillName.isBlank()) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            return HandlerUtils.createToolExecuteResult(
                    "Error: Missing required parameter 'skill_name'. Please provide the name of the skill to activate.");
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("tool", "useSkill");
        payload.put("path", skillName);
        context.getCallbacks()
                .say(ClineSay.TOOL, JsonUtils.toJsonString(payload), null, null, false, null);

        List<SkillMetadata> allSkills =
                SkillDiscovery.discoverSkills(context.getCwd(), GlobalFileNames.GLOBAL_SKILLS_DIR);
        List<SkillMetadata> resolvedSkills = SkillDiscovery.getAvailableSkills(allSkills);

        Map<String, Boolean> globalSkillsToggles =
                context.getServices().getStateManager().getSettings().getGlobalSkillsToggles();
        Map<String, Boolean> localSkillsToggles =
                context.getServices().getStateManager().getLocalState().getLocalSkillsToggles();

        List<SkillMetadata> availableSkills =
                resolvedSkills.stream()
                        .filter(
                                skill -> {
                                    Map<String, Boolean> toggles =
                                            "global".equals(skill.getSource())
                                                    ? globalSkillsToggles
                                                    : localSkillsToggles;
                                    return toggles == null
                                            || !Boolean.FALSE.equals(toggles.get(skill.getPath()));
                                })
                        .collect(Collectors.toList());

        if (availableSkills.isEmpty()) {
            return HandlerUtils.createToolExecuteResult(
                    "Error: No skills are available. Skills may be disabled or not configured.");
        }

        SkillContent skillContent = SkillDiscovery.getSkillContent(skillName, availableSkills);
        if (skillContent == null) {
            String availableNames =
                    availableSkills.stream()
                            .map(SkillMetadata::getName)
                            .sorted()
                            .collect(Collectors.joining(", "));
            return HandlerUtils.createToolExecuteResult(
                    "Error: Skill \""
                            + skillName
                            + "\" not found. Available skills: "
                            + (availableNames.isBlank() ? "none" : availableNames));
        }

        context.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);
        String skillDirectory =
                skillContent
                        .getPath()
                        .replaceAll("[/\\\\]SKILL\\.md$", "")
                        .replaceAll("SKILL\\.md$", "");

        return HandlerUtils.createToolExecuteResult(
                "# Skill \""
                        + skillContent.getName()
                        + "\" is now active\n\n"
                        + skillContent.getInstructions()
                        + "\n\n---\nIMPORTANT: The skill is now loaded. Do NOT call use_skill again for this task. Simply follow the instructions above to complete the user's request. You may access other files in the skill directory at: "
                        + skillDirectory);
    }
}
