package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpecProvider;
import com.hhoa.kline.core.core.tools.args.ActModeRespondInput;
import com.hhoa.kline.core.core.tools.handlers.ActModeRespondToolHandler;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.ClineDefaultTool;

/**
 * Act Mode Respond 工具规格 - 在 ACT MODE 执行期间提供进度更新
 *
 * @author hhoa
 */
public final class ActModeRespondTool implements ToolSpecProvider<ActModeRespondInput> {

    private static final ActModeRespondToolHandler HANDLER = new ActModeRespondToolHandler();

    private static final String DESCRIPTION = "Provide a progress update during ACT MODE.";

    private static final String PROMPT =
            "Provide a progress update or preamble to the user during ACT MODE execution. "
                    + "This tool allows you to communicate your thought process and planned actions without "
                    + "interrupting the execution flow. After displaying your message, execution automatically "
                    + "continues, allowing you to proceed with subsequent tool calls immediately. "
                    + "This tool is only available in ACT MODE. This tool may not be called immediately "
                    + "after a previous act_mode_respond call.\n\n"
                    + "IMPORTANT: Use this tool when it adds value to the user experience, but always follow "
                    + "it with an actual tool call - never call it twice in a row.\n\n"
                    + "Use this tool when:\n"
                    + "- After reading files and before making any edits - explain your analysis and what changes you plan to make\n"
                    + "- When starting a new phase of work (e.g., transitioning from backend to frontend, or from one feature to another)\n"
                    + "- During long sequences of operations to provide progress updates\n"
                    + "- When your approach or strategy changes mid-task\n"
                    + "- Before executing complex or potentially risky operations\n"
                    + "- To explain why you're choosing one approach over another\n\n"
                    + "Do NOT use this tool when you have completed all required actions and are ready to present "
                    + "the final output; in that case, use the attempt_completion tool instead.\n\n"
                    + "CRITICAL CONSTRAINT: You MUST NOT call this tool more than once in a row. After using "
                    + "act_mode_respond, your next assistant message MUST either call a different tool or perform "
                    + "additional work without using act_mode_respond again. If you attempt to call act_mode_respond "
                    + "consecutively, the tool call will fail with an explicit error.";

    @Override
    public String name() {
        return ClineDefaultTool.ACT_MODE.getValue();
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
    public Class<ActModeRespondInput> inputType(ModelFamily family) {
        return ActModeRespondInput.class;
    }

    @Override
    public ToolHandler<ActModeRespondInput> handler(ModelFamily family) {
        return HANDLER;
    }
}
