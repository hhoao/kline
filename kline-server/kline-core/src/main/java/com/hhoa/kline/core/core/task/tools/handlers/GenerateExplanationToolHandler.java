package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.GenerateExplanationTool;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate Explanation 工具处理器 - 生成 AI 驱动的代码变更解释
 *
 * @author hhoa
 */
public class GenerateExplanationToolHandler implements ToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.GENERATE_EXPLANATION.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String title = HandlerUtils.getStringParam(block, "title");
        return "[" + block.getName() + " for '" + (title != null ? title : "code changes") + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return GenerateExplanationTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String title = HandlerUtils.getStringParam(block, "title");
        String fromRef = HandlerUtils.getStringParam(block, "from_ref");
        String toRef = HandlerUtils.getStringParam(block, "to_ref");
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
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String title = HandlerUtils.getStringParam(block, "title");
        String fromRef = HandlerUtils.getStringParam(block, "from_ref");
        String toRef = HandlerUtils.getStringParam(block, "to_ref");

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
