package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.WebFetchTool;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WebFetchToolHandler implements FullyManagedTool {

    @Override
    public String getName() {
        return ClineDefaultTool.WEB_FETCH.getValue();
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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String url = HandlerUtils.getStringParam(block, "url");
        if (url == null || url.isEmpty()) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult =
                    config.getCallbacks()
                            .sayAndCreateMissingParamError(
                                    ClineDefaultTool.WEB_FETCH.getValue(), "url");
            return HandlerUtils.createTextBlocks(errorResult);
        }
        config.getTaskState().setConsecutiveMistakeCount(0);

        Map<String, Object> messageProps = new HashMap<>();
        messageProps.put("tool", "webFetch");
        messageProps.put("path", url);
        messageProps.put("content", "Fetching URL: " + url);
        messageProps.put("operationIsLocatedInWorkspace", false);

        String message = JsonUtils.toJsonString(messageProps);

        ResponseFormatter formatResponse = new ResponseFormatter();

        Boolean autoApprove =
                config.getCallbacks().shouldAutoApproveTool(ClineDefaultTool.WEB_FETCH.getValue());
        if (Boolean.TRUE.equals(autoApprove)) {
            config.getCallbacks()
                    .say(ClineSay.TOOL, message, null, null, false, ClineMessageFormat.JSON);
            if (!config.isYoloModeToggled()) {
                config.getTaskState()
                        .setConsecutiveAutoApprovedRequestsCount(
                                config.getTaskState().getConsecutiveAutoApprovedRequestsCount()
                                        + 1);
            }
            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(config.getUlid(), "web_fetch", modelId, true, true);
            }
            return executeWebFetch(config, url);
        } else {
            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    "Cline wants to fetch content from " + url,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, msg) -> {});
            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.TOOL, message, config, ClineMessageFormat.JSON);
            if (!didApprove) {
                if (config.getServices() != null
                        && config.getServices().getTelemetryService() != null) {
                    String modelId =
                            config.getApi() != null && config.getApi().getModel() != null
                                    ? config.getApi().getModel().getId()
                                    : "unknown";
                    config.getServices()
                            .getTelemetryService()
                            .captureToolUsage(
                                    config.getUlid(), block.getName(), modelId, false, false);
                }
                return HandlerUtils.createTextBlocks(formatResponse.toolDenied());
            } else {
                if (config.getServices() != null
                        && config.getServices().getTelemetryService() != null) {
                    String modelId =
                            config.getApi() != null && config.getApi().getModel() != null
                                    ? config.getApi().getModel().getId()
                                    : "unknown";
                    config.getServices()
                            .getTelemetryService()
                            .captureToolUsage(
                                    config.getUlid(), block.getName(), modelId, false, true);
                }
                return executeWebFetch(config, url);
            }
        }
    }

    private List<UserContentBlock> executeWebFetch(TaskConfig config, String url) {
        try {
            String content = fetchWebContent(url);
            ResponseFormatter formatResponse = new ResponseFormatter();
            return HandlerUtils.createTextBlocks(formatResponse.toolResult(content, null, null));
        } catch (Exception e) {
            ResponseFormatter formatResponse = new ResponseFormatter();
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolError("Error fetching web content: " + e.getMessage()));
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
