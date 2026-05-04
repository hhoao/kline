package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.CondenseInput;
import com.hhoa.kline.core.core.tools.handlers.CondenseHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** 压缩上下文工具规格。 */
public final class CondenseTool implements ToolSpecProvider<CondenseInput> {

    private static final CondenseHandler HANDLER = new CondenseHandler();

    private static final String DESCRIPTION =
            "Create a detailed compact summary of the conversation.";

    private static final String PROMPT =
            """
            Create a detailed summary for context compaction.
            The summary must preserve continuity so work can resume without drift.

            Include:
            1) Previous Conversation (high-level flow and user intent)
            2) Current Work (what was being done right before condense)
            3) Key Technical Concepts (frameworks, patterns, conventions)
            4) Relevant Files and Code (important files, edits, snippets when useful)
            5) Problem Solving (resolved and ongoing issues)
            6) Pending Tasks and Next Steps (explicit outstanding work)

            If possible, anchor next steps to recent conversation details to avoid misinterpretation.
            Prefer completeness and technical accuracy over brevity.
            """;

    @Override
    public String name() {
        return ClineDefaultTool.CONDENSE.getValue();
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
    public Class<CondenseInput> inputType(ModelFamily family) {
        return CondenseInput.class;
    }

    @Override
    public ToolHandler<CondenseInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
