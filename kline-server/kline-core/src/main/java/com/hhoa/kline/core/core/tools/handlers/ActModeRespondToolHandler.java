package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.tools.args.ActModeRespondInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;

/**
 * Act Mode Respond 工具处理器 - 在 ACT MODE 执行期间提供进度更新
 *
 * @author hhoa
 */
public class ActModeRespondToolHandler implements ToolHandler<ActModeRespondInput> {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    public void handlePartialBlock(ActModeRespondInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String response = input.response();
        if (response != null) {
            ui.say(ClineSay.TEXT, response, null, null, true, null);
        }
    }

    public ToolExecuteResult execute(
            ActModeRespondInput input, ToolContext context, ToolUse block) {
        String response = input.response();

        // Only available in ACT mode
        if (!Mode.ACT.equals(context.getMode())) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            String modeName =
                    context.getMode() != null
                            ? context.getMode().getValue().toUpperCase()
                            : "UNKNOWN";
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            "The act_mode_respond tool is only available in ACT MODE. "
                                    + "You are currently in "
                                    + modeName
                                    + " MODE. Please use the appropriate tool for your current mode."));
        }

        context.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);

        context.getCallbacks().say(ClineSay.TEXT, response, null, null, false, null);

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "[Message displayed. Now proceed with your next tool call - "
                                + "it must be a different tool (read_file, replace_in_file, execute_command, etc.), "
                                + "not act_mode_respond again.]",
                        null,
                        null));
    }
}
