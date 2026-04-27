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
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.specs.WebFetchTool;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WebFetchToolHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Getter
    @Setter
    public static class WebFetchToolState extends ToolState {
        private String url;
        private String prompt;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.WEB_FETCH.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new WebFetchToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        return "[" + block.getName() + " for '" + HandlerUtils.getStringParam(block, "url") + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return WebFetchTool.create(ModelFamily.GENERIC);
    }

    @Override
    public boolean isConcurrencySafe(ToolUse block, ToolContext context) {
        return context != null
                && context.getCallbacks() != null
                && Boolean.TRUE.equals(context.getCallbacks().shouldAutoApproveTool(getName()));
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String url = HandlerUtils.getStringParam(block, "url");
        Map<String, Object> messageProps = new HashMap<>();
        messageProps.put("tool", "webFetch");
        messageProps.put("path", url);
        messageProps.put("content", "Fetching URL: " + url);
        messageProps.put("operationIsLocatedInWorkspace", false);
        String message = JsonUtils.toJsonString(messageProps);

        ui.ask(ClineAsk.TOOL, message, block.isPartial(), ClineMessageFormat.JSON);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String url = HandlerUtils.getStringParam(block, "url");
        String prompt = HandlerUtils.getStringParam(block, "prompt");

        Map<String, Object> messageProps = new HashMap<>();
        messageProps.put("tool", "webFetch");
        messageProps.put("path", url);
        messageProps.put("content", "Fetching URL: " + url);
        messageProps.put("operationIsLocatedInWorkspace", false);

        String message = JsonUtils.toJsonString(messageProps);

        Boolean autoApprove =
                context.getCallbacks().shouldAutoApproveTool(ClineDefaultTool.WEB_FETCH.getValue());
        if (Boolean.TRUE.equals(autoApprove)) {
            context.getCallbacks()
                    .say(ClineSay.TOOL, message, null, null, false, ClineMessageFormat.JSON);

            captureTelemetry(context, block, true, true);
            return executeWebFetch(context, url, prompt);
        }

        // Need to ask user -- save state and return PendingAsk
        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                "Cline wants to fetch content from " + url,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, msg) -> {});

        WebFetchToolState state = (WebFetchToolState) context.getToolState();
        state.setPhase(1);
        state.setUrl(url);
        state.setPrompt(prompt);

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
        WebFetchToolState state = (WebFetchToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false);
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true);
        return executeWebFetch(context, state.getUrl(), state.getPrompt());
    }

    private ToolExecuteResult executeWebFetch(ToolContext config, String url, String prompt) {
        try {
            String content = fetchWebContent(url);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(content, null, null));
        } catch (Exception e) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("Error fetching web content: " + e.getMessage()));
        }
    }

    private void captureTelemetry(
            ToolContext context, ToolUse block, boolean autoApproved, boolean approved) {
        if (context.getServices() != null && context.getServices().getTelemetryService() != null) {
            String modelId =
                    context.getApi() != null && context.getApi().getModel() != null
                            ? context.getApi().getModel().getId()
                            : "unknown";
            context.getServices()
                    .getTelemetryService()
                    .captureToolUsage(
                            context.getUlid(), block.getName(), modelId, autoApproved, approved);
        }
    }

    private static String fetchWebContent(String urlString) throws Exception {
        Document doc =
                Jsoup.connect(urlString)
                        .userAgent("Mozilla/5.0 (compatible; ClineBot/1.0)")
                        .timeout(10000)
                        .get();

        doc.select("script, style, nav, footer, header, aside, iframe, noscript").remove();

        String title = doc.title();

        StringBuilder markdown = new StringBuilder();

        if (title != null && !title.isEmpty()) {
            markdown.append("# ").append(title).append("\n\n");
        }

        Element body = doc.body();
        if (body != null) {
            markdown.append(convertToMarkdown(body));
        }

        String result = markdown.toString().replaceAll("\\n{3,}", "\n\n").trim();

        if (result.length() > 50000) {
            result = result.substring(0, 50000) + "\n\n[Content truncated due to length...]";
        }

        return result;
    }

    private static String convertToMarkdown(Element element) {
        StringBuilder markdown = new StringBuilder();

        for (Element child : element.children()) {
            String tagName = child.tagName().toLowerCase();

            switch (tagName) {
                case "h1":
                    markdown.append("# ").append(child.text()).append("\n\n");
                    break;
                case "h2":
                    markdown.append("## ").append(child.text()).append("\n\n");
                    break;
                case "h3":
                    markdown.append("### ").append(child.text()).append("\n\n");
                    break;
                case "h4":
                    markdown.append("#### ").append(child.text()).append("\n\n");
                    break;
                case "h5":
                    markdown.append("##### ").append(child.text()).append("\n\n");
                    break;
                case "h6":
                    markdown.append("###### ").append(child.text()).append("\n\n");
                    break;
                case "p":
                    markdown.append(child.text()).append("\n\n");
                    break;
                case "a":
                    String href = child.attr("href");
                    markdown.append("[").append(child.text()).append("](").append(href).append(")");
                    break;
                case "ul":
                case "ol":
                    for (Element li : child.select("li")) {
                        markdown.append("- ").append(li.text()).append("\n");
                    }
                    markdown.append("\n");
                    break;
                case "code":
                    markdown.append("`").append(child.text()).append("`");
                    break;
                case "pre":
                    markdown.append("```\n").append(child.text()).append("\n```\n\n");
                    break;
                case "blockquote":
                    markdown.append("> ").append(child.text()).append("\n\n");
                    break;
                case "strong":
                case "b":
                    markdown.append("**").append(child.text()).append("**");
                    break;
                case "em":
                case "i":
                    markdown.append("*").append(child.text()).append("*");
                    break;
                case "br":
                    markdown.append("\n");
                    break;
                default:
                    if (child.children().size() > 0) {
                        markdown.append(convertToMarkdown(child));
                    } else {
                        String text = child.text();
                        if (!text.isEmpty()) {
                            markdown.append(text).append(" ");
                        }
                    }
                    break;
            }
        }

        return markdown.toString();
    }
}
