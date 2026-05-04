package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.SummarizeTaskInput;
import com.hhoa.kline.core.core.tools.handlers.SummarizeTaskHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/** 任务总结工具规格。 */
public final class SummarizeTaskTool implements ToolSpecProvider<SummarizeTaskInput> {

    private static final SummarizeTaskHandler HANDLER = new SummarizeTaskHandler();

    private static final String DESCRIPTION =
            "Summarize task state before context exhaustion.";

    private static final String PROMPT =
            """
            Use this tool when context is running out and a continuation summary is required.
            The output must preserve all critical information needed to continue immediately.

            Required structure:
            1) Primary Request and Intent
            2) Key Technical Concepts
            3) Files and Code Sections (include concrete file-level details)
            4) Problem Solving Progress
            5) Pending Tasks
            6) Task Evolution (original task, changes, current active task)
            7) Current Work (exactly what was in progress most recently)
            8) Next Step (directly aligned with latest explicit user intent)
            9) Required Files (only if necessary)

            Quality constraints:
            - Keep the summary technically complete and implementation-oriented.
            - Prioritize the most recent user intent.
            - Include enough detail to resume work without re-reading prior messages.
            """;

    @Override
    public String name() {
        return ClineDefaultTool.SUMMARIZE_TASK.getValue();
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
    public Class<SummarizeTaskInput> inputType(ModelFamily family) {
        return SummarizeTaskInput.class;
    }

    @Override
    public ToolHandler<SummarizeTaskInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
