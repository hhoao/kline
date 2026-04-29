package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.tools.args.GenerateExplanationInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate Explanation 工具处理器 - 生成 AI 驱动的代码变更解释
 *
 * @author hhoa
 */
public class GenerateExplanationToolHandler implements ToolHandler<GenerateExplanationInput> {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getDescription(ToolUse block) {
        String title = HandlerUtils.getStringParam(block, "title");
        return "[" + block.getName() + " for '" + (title != null ? title : "code changes") + "']";
    }

    @Override
    public void handlePartialBlock(
            GenerateExplanationInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String title = input.title();
        String fromRef = input.fromRef();
        String toRef = input.toRef();
        String message =
                createExplanationMessage(
                        title != null ? title : "code changes",
                        fromRef != null ? fromRef : "",
                        toRef != null ? toRef : "working directory",
                        "generating",
                        null);
        ui.say(ClineSay.GENERATE_EXPLANATION, message, null, null, true, null);
    }

    @Override
    public ToolExecuteResult execute(
            GenerateExplanationInput input, ToolContext context, ToolUse block) {
        String title = input.title();
        String fromRef = input.fromRef();
        String toRef = input.toRef();

        String toRefDisplay = toRef != null ? toRef : "working directory";
        context.getCallbacks()
                .say(
                        ClineSay.GENERATE_EXPLANATION,
                        createExplanationMessage(title, fromRef, toRefDisplay, "generating", null),
                        null,
                        null,
                        true,
                        null);

        // TODO: Implement actual git diff and AI explanation generation
        context.getCallbacks()
                .say(
                        ClineSay.GENERATE_EXPLANATION,
                        createExplanationMessage(title, fromRef, toRefDisplay, "complete", null),
                        null,
                        null,
                        false,
                        null);

        String refDescription =
                toRef != null
                        ? "'" + fromRef + "' and '" + toRef + "'"
                        : "'" + fromRef + "' and working directory";
        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "Generate explanation is not yet fully implemented. "
                                + "Requested comparison between "
                                + refDescription
                                + " with title: \""
                                + title
                                + "\".",
                        null,
                        null));
    }

    private String createExplanationMessage(
            String title, String fromRef, String toRef, String status, String error) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("fromRef", fromRef);
        payload.put("toRef", toRef);
        payload.put("status", status);
        if (error != null) {
            payload.put("error", error);
        }
        return JsonUtils.toJsonString(payload);
    }
}
