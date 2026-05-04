package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.WriteToFileInput;
import com.hhoa.kline.core.core.tools.handlers.NewRuleToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** 创建新规则文件工具规格。 */
public final class NewRuleTool implements ToolSpecProvider<WriteToFileInput> {
    private static final NewRuleToolHandler HANDLER = new NewRuleToolHandler();

    private static final String DESCRIPTION =
            "Create a new rule file under .clinerules.";

    private static final String PROMPT =
            """
            The user asked you to create a new Cline rule file based on conversation context.
            You MUST use this tool to create that file and MUST NOT overwrite existing rule files.

            Requirements:
            - Create the file under the top-level .clinerules directory.
            - File must be markdown (.md) with a concise, hyphenated filename.
            - Filename must not be default-clineignore.md.
            - Provide COMPLETE file content (no truncation).
            - Start with "## Brief overview" and then organize distinct guideline sections with bullet points.

            Guideline quality rules:
            - Capture real user/project preferences from the conversation.
            - Cover practical areas like communication style, workflow, coding practices, project context.
            - Do not invent preferences.
            - Do not dump arbitrary conversation history.
            - Keep guidance concrete and reasonably concise.
            """;

    @Override
    public String name() {
        return ClineDefaultTool.NEW_RULE.getValue();
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
    public Class<WriteToFileInput> inputType(ModelFamily family) {
        return WriteToFileInput.class;
    }

    @Override
    public ToolHandler<WriteToFileInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
