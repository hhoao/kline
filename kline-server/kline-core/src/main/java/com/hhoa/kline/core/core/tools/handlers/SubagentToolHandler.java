package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.args.SubagentInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Subagent 工具处理器 - 运行并行子代理进行研究
 *
 * @author hhoa
 */
public class SubagentToolHandler implements StateFullToolHandler<SubagentInput> {

    private static final int MAX_SUBAGENT_PROMPTS = 5;
    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class SubagentToolState extends ToolState {
        private List<String> prompts;
    }

    @Override
    public ToolState createToolState() {
        return new SubagentToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[subagents]";
    }

    public void handlePartialBlock(SubagentInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        List<String> prompts = collectPrompts(input);
        if (prompts.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("prompts", prompts);
        ui.say(
                ClineSay.USE_SUBAGENTS,
                JsonUtils.toJsonString(payload),
                null,
                null,
                block.isPartial(),
                null);
    }

    public ToolExecuteResult execute(SubagentInput input, ToolContext context, ToolUse block) {
        List<String> prompts = collectPrompts(input);

        if (prompts.isEmpty()) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            "Missing required parameter: provide at least 'prompt_1'."));
        }

        if (prompts.size() > MAX_SUBAGENT_PROMPTS) {
            context.getTaskState()
                    .getApiTurnState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getApiTurnState().getConsecutiveMistakeCount()
                                    + 1);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            "Too many subagent prompts provided ("
                                    + prompts.size()
                                    + "). Maximum is "
                                    + MAX_SUBAGENT_PROMPTS
                                    + "."));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("prompts", prompts);
        String message = JsonUtils.toJsonString(payload);

        // Check auto-approval
        Boolean shouldAutoApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());
        if (Boolean.TRUE.equals(shouldAutoApprove)) {
            context.getCallbacks().say(ClineSay.USE_SUBAGENTS, message, null, null, false, null);
            return executeSubagents(prompts);
        }

        // Need to ask user -- save state and return PendingAsk
        SubagentToolState state = (SubagentToolState) context.getToolState();
        state.setPhase(1);
        state.setPrompts(prompts);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.USE_SUBAGENTS,
                        message,
                        context,
                        null,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        SubagentToolState state = (SubagentToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        return executeSubagents(state.getPrompts());
    }

    private ToolExecuteResult executeSubagents(List<String> prompts) {
        // TODO: Implement actual subagent execution with SubagentRunner
        StringBuilder summary = new StringBuilder();
        summary.append("Subagent results:\n");
        summary.append("Total: ").append(prompts.size()).append("\n");
        summary.append("Note: Subagent execution is not yet fully implemented.\n\n");
        for (int i = 0; i < prompts.size(); i++) {
            summary.append("[")
                    .append(i + 1)
                    .append("] PENDING - ")
                    .append(prompts.get(i))
                    .append("\n");
        }

        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(summary.toString(), null, null));
    }

    private List<String> collectPrompts(SubagentInput input) {
        List<String> prompts = new ArrayList<>();
        String[] inputPrompts = {
            input.prompt1(), input.prompt2(), input.prompt3(), input.prompt4(), input.prompt5()
        };
        for (String prompt : inputPrompts) {
            if (prompt != null && !prompt.trim().isEmpty()) {
                prompts.add(prompt.trim());
            }
        }
        return prompts;
    }
}
