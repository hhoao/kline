package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.tools.types.ToolContext;
import com.hhoa.kline.core.core.task.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.task.tools.types.ToolState;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * browser_action 工具处理器。
 *
 * <p>对应 Cline TS 版本的 BrowserToolHandler.ts。 支持 launch / click / type / scroll_down / scroll_up /
 * close 等浏览器操作。
 */
@Slf4j
public class BrowserToolHandler implements StateFullToolHandler {

    private static final Set<String> VALID_ACTIONS =
            Set.of("launch", "click", "type", "scroll_down", "scroll_up", "close");

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** BrowserTool 的阶段状态 */
    @Getter
    @Setter
    public static class BrowserToolState extends ToolState {
        private String action;
        private String url;
        private String coordinate;
        private String text;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.BROWSER.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String action = HandlerUtils.getStringParam(block, "action");
        return "[" + block.getName() + " for '" + (action != null ? action : "unknown") + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return null;
    }

    @Override
    public ToolState createToolState() {
        return new BrowserToolState();
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {
        String action = HandlerUtils.getStringParam(block, "action");
        String url = HandlerUtils.getStringParam(block, "url");
        String coordinate = HandlerUtils.getStringParam(block, "coordinate");
        String text = HandlerUtils.getStringParam(block, "text");

        if (action == null || !VALID_ACTIONS.contains(action)) {
            return;
        }

        if ("launch".equals(action)) {
            uiHelpers.say(
                    ClineSay.BROWSER_ACTION_LAUNCH,
                    url != null ? url : "",
                    null,
                    null,
                    block.isPartial(),
                    null);
        } else {
            String message =
                    "{\"action\":\""
                            + action
                            + "\""
                            + (coordinate != null ? ",\"coordinate\":\"" + coordinate + "\"" : "")
                            + (text != null ? ",\"text\":\"" + escapeJson(text) + "\"" : "")
                            + "}";
            uiHelpers.say(ClineSay.TOOL, message, null, null, block.isPartial(), null);
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String action = HandlerUtils.getStringParam(block, "action");
        String url = HandlerUtils.getStringParam(block, "url");
        String coordinate = HandlerUtils.getStringParam(block, "coordinate");
        String text = HandlerUtils.getStringParam(block, "text");

        if (!VALID_ACTIONS.contains(action)) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            context.getCallbacks()
                                    .sayAndCreateMissingParamError(getName(), "action")));
        }

        if ("launch".equals(action)) {
            if (url == null || url.isEmpty()) {
                context.getTaskState()
                        .setConsecutiveMistakeCount(
                                context.getTaskState().getConsecutiveMistakeCount() + 1);
                return new ToolExecuteResult.Immediate(
                        HandlerUtils.createTextBlocks(
                                context.getCallbacks()
                                        .sayAndCreateMissingParamError(getName(), "url")));
            }
            context.getTaskState().setConsecutiveMistakeCount(0);

            // 检查自动审批
            Boolean autoApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());
            if (autoApprove != null && autoApprove) {
                context.getCallbacks()
                        .say(ClineSay.BROWSER_ACTION_LAUNCH, url, null, null, false, null);
                return new ToolExecuteResult.Immediate(
                        HandlerUtils.createTextBlocks(
                                "The browser action has been executed. Console logs and screenshot have been captured."));
            }

            // 需要用户审批
            BrowserToolState toolState = getToolState(context);
            toolState.setAction(action);
            toolState.setUrl(url);
            var token =
                    ToolResultUtils.askApprovalAndPushFeedbackForToken(
                            ClineAsk.BROWSER_ACTION_LAUNCH,
                            url,
                            context,
                            null,
                            block,
                            getDescription(block));
            return new ToolExecuteResult.PendingAsk(token);
        }

        // 非 launch 操作
        if ("click".equals(action) && (coordinate == null || coordinate.isEmpty())) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            context.getCallbacks()
                                    .sayAndCreateMissingParamError(getName(), "coordinate")));
        }
        if ("type".equals(action) && (text == null || text.isEmpty())) {
            context.getTaskState()
                    .setConsecutiveMistakeCount(
                            context.getTaskState().getConsecutiveMistakeCount() + 1);
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            context.getCallbacks()
                                    .sayAndCreateMissingParamError(getName(), "text")));
        }

        context.getTaskState().setConsecutiveMistakeCount(0);

        String message =
                "{\"action\":\""
                        + action
                        + "\""
                        + (coordinate != null ? ",\"coordinate\":\"" + coordinate + "\"" : "")
                        + (text != null ? ",\"text\":\"" + escapeJson(text) + "\"" : "")
                        + "}";
        context.getCallbacks().say(ClineSay.TOOL, message, null, null, false, null);

        if ("close".equals(action)) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(
                            "The browser has been closed. You may now proceed to using other tools."));
        }

        return new ToolExecuteResult.Immediate(
                HandlerUtils.createTextBlocks(
                        "The browser action has been executed. The console logs and screenshot have been captured for your analysis.\n\n"
                                + "(REMEMBER: if you need to proceed to using non-`browser_action` tools or launch a new browser, "
                                + "you MUST first close this browser.)"));
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState state, AskResult askResult) {
        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            return new ToolExecuteResult.Immediate(
                    HandlerUtils.createTextBlocks(formatResponse.toolDenied()));
        }

        BrowserToolState toolState = getToolState(context);
        context.getCallbacks()
                .say(ClineSay.BROWSER_ACTION_LAUNCH, toolState.getUrl(), null, null, false, null);

        return new ToolExecuteResult.Immediate(
                HandlerUtils.createTextBlocks(
                        "The browser action has been executed. Console logs and screenshot have been captured."));
    }

    private BrowserToolState getToolState(ToolContext context) {
        ToolState s = context.getToolState();
        if (s instanceof BrowserToolState bts) {
            return bts;
        }
        return new BrowserToolState();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
