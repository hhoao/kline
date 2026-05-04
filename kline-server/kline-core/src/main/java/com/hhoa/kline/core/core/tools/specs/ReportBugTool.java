package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ReportBugInput;
import com.hhoa.kline.core.core.tools.handlers.ReportBugHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** Bug 反馈工具规格。 */
public final class ReportBugTool implements ToolSpecProvider<ReportBugInput> {

    private static final ReportBugHandler HANDLER = new ReportBugHandler();

    private static final String DESCRIPTION =
            "Prepare and submit a structured bug report.";

    private static final String PROMPT =
            """
            Help the user submit a bug report.
            First collect all required fields before calling this tool:
            - title
            - what_happened (include expected vs actual behavior)
            - steps_to_reproduce
            Optional:
            - api_request_output
            - additional_context

            Behavior rules:
            - If required info is missing, ask follow-up questions first.
            - Do not assume issue details unless clearly provided.
            - Refer to fields in user-friendly wording while gathering information.
            - Submit only after explicit user confirmation.
            """;

    @Override
    public String name() {
        return ClineDefaultTool.REPORT_BUG.getValue();
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
    public Class<ReportBugInput> inputType(ModelFamily family) {
        return ReportBugInput.class;
    }

    @Override
    public ToolHandler<ReportBugInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
