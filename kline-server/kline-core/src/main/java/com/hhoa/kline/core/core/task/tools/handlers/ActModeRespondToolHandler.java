package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.ActModeRespondTool;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.shared.storage.types.Mode;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.enums.ClineDefaultTool;

/**
 * Act Mode Respond 工具处理器 - 在 ACT MODE 执行期间提供进度更新
 *
 * @author hhoa
 */
public class ActModeRespondToolHandler implements ToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.ACT_MODE.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + "]";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return ActModeRespondTool.create(ModelFamily.NATIVE_GPT_5);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String response = HandlerUtils.getStringParam(block, "response");
        if (response != null) {
            ui.say(ClineSay.TEXT, response, null, null, true, null);
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String response = HandlerUtils.getStringParam(block, "response");
        String taskProgress = HandlerUtils.getStringParam(block, "task_progress");

        // Only available in ACT mode
        if (!Mode.ACT.equals(context.getMode())) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
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

        context.getTaskState().setConsecutiveMistakeCount(0);

        context.getCallbacks().say(ClineSay.TEXT, response, null, null, false, null);

        if (taskProgress != null && !taskProgress.isEmpty()) {
            context.getCallbacks().updateFCListFromToolResponse(taskProgress);
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "[Message displayed. Now proceed with your next tool call - "
                                + "it must be a different tool (read_file, replace_in_file, execute_command, etc.), "
                                + "not act_mode_respond again.]",
                        null,
                        null));
    }
}
