package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.tools.specs.WebSearchTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Web Search 工具处理器 - 执行网络搜索并返回结果
 *
 * @author hhoa
 */
public class WebSearchToolHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class WebSearchToolState extends ToolState {
        private String query;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.WEB_SEARCH.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new WebSearchToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        String query = HandlerUtils.getStringParam(block, "query");
        return "[" + block.getName() + " for '" + (query != null ? query : "") + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return WebSearchTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String query = HandlerUtils.getStringParam(block, "query");
        String message = buildMessage(query);
        ui.ask(ClineAsk.TOOL, message, block.isPartial(), ClineMessageFormat.JSON);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String query = HandlerUtils.getStringParam(block, "query");

        String message = buildMessage(query);

        // Check auto-approval
        Boolean shouldAutoApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());
        if (Boolean.TRUE.equals(shouldAutoApprove)) {
            context.getCallbacks()
                    .say(ClineSay.TOOL, message, null, null, false, ClineMessageFormat.JSON);
            return executeWebSearch(query);
        }

        // Need to ask user -- save state and return PendingAsk
        WebSearchToolState state = (WebSearchToolState) context.getToolState();
        state.setPhase(1);
        state.setQuery(query);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.TOOL,
                        message,
                        context,
                        ClineMessageFormat.JSON,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        WebSearchToolState state = (WebSearchToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        return executeWebSearch(state.getQuery());
    }

    private ToolExecuteResult executeWebSearch(String query) {
        // TODO: Implement actual web search API call
        return HandlerUtils.createToolExecuteResult(
                formatResponse.toolResult(
                        "Web search is not yet fully implemented. "
                                + "Searched for: \""
                                + query
                                + "\". "
                                + "Please use alternative methods to find the information.",
                        null,
                        null));
    }

    private String buildMessage(String query) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("tool", "webSearch");
        payload.put("path", query != null ? query : "");
        payload.put("content", "Searching for: " + (query != null ? query : ""));
        payload.put("operationIsLocatedInWorkspace", false);
        return JsonUtils.toJsonString(payload);
    }
}
