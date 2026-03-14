package com.hhoa.kline.core.core.prompts.systemprompt.components;

import com.hhoa.kline.core.core.prompts.systemprompt.PromptVariant;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptComponent;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptSection;
import com.hhoa.kline.core.core.prompts.systemprompt.templates.TemplateEngine;
import java.util.Map;
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
        String template = getTemplateText();

        if (variant.getComponentOverrides() != null
                && variant.getComponentOverrides().containsKey(SystemPromptSection.SKILLS)) {
            var override = variant.getComponentOverrides().get(SystemPromptSection.SKILLS);
            if (override.getTemplate() != null) {
                template = override.getTemplate();
            }
        }

        return templateEngine.resolve(template, context, Map.of());
    }

    @Override
    public SystemPromptSection getSystemPromptSection() {
        return SystemPromptSection.SKILLS;
    }

    private String getTemplateText() {
        return """
            In order to help Cline achieve the highest-quality results possible, Anthropic has compiled a set of "skills" which are essentially folders that contain a set of best practices for use in creating docs of different kinds. For instance, there is a docx skill which contains specific instructions for creating high-quality word documents, a PDF skill for creating and filling in PDFs, etc. These skill folders have been heavily labored over and contain the condensed wisdom of a lot of trial and error working with LLMs to make really good, professional, outputs. Sometimes multiple skills may be required to get the best results, so Cline should not limit itself to just reading one.

            We've found that Cline's efforts are greatly aided by reading the documentation available in the skill BEFORE writing any code, creating any files, or using any computer tools. As such, when using the Linux computer to accomplish tasks, Cline's first order of business should always be to examine the skills available in Cline's <available_skills> and decide which skills, if any, are relevant to the task. Then, Cline can and should use the `view` tool to read the appropriate SKILL.md files and follow their instructions.

            For instance:

            User: Can you make me a powerpoint with a slide for each month of pregnancy showing how my body will be affected each month?
            Cline: [immediately calls the view tool on /mnt/skills/public/pptx/SKILL.md]

            User: Please read this document and fix any grammatical errors.
            Cline: [immediately calls the view tool on /mnt/skills/public/docx/SKILL.md]

            User: Please create an AI image based on the document I uploaded, then add it to the doc.
            Cline: [immediately calls the view tool on /mnt/skills/public/docx/SKILL.md followed by reading the /mnt/skills/user/imagegen/SKILL.md file (this is an example user-uploaded skill and may not be present at all times, but Cline should attend very closely to user-provided skills since they're more than likely to be relevant)]

            Please invest the extra effort to read the appropriate SKILL.md file before jumping in -- it's worth it!

            <additional_skills_reminder>
            Repeating again for emphasis: please begin the response to each and every request in which computer use is implicated by using the `view` tool to read the appropriate SKILL.md files (remember, multiple skill files may be relevant and essential) so that CLINE can learn from the best practices that have been built up by trial and error to help CLINE produce the highest-quality outputs. In particular:

            - When creating presentations, ALWAYS call `view` on /mnt/skills/public/pptx/SKILL.md before starting to make the presentation.
            - When creating spreadsheets, ALWAYS call `view` on /mnt/skills/public/xlsx/SKILL.md before starting to make the spreadsheet.
            - When creating word documents, ALWAYS call `view` on /mnt/skills/public/docx/SKILL.md before starting to make the document.
            - When creating PDFs? That's right, ALWAYS call `view` on /mnt/skills/public/pdf/SKILL.md before starting to make the PDF. (Don't use pypdf.)

            Please note that the above list of examples is *nonexhaustive* and in particular it does not cover either "user skills" (which are skills added by the user that are typically in `/mnt/skills/user`), or "example skills" (which are some other skills that may or may not be enabled that will be in `/mnt/skills/example`). These should also be attended to closely and used promiscuously when they seem at all relevant, and should usually be used in combination with the core document creation skills.

            This is extremely important, so thanks for paying attention to it.
            </additional_skills_reminder>
            """;
    }
}
