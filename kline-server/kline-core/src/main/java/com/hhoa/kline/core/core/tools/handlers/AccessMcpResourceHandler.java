package com.hhoa.kline.core.core.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.ToolSpec;
import com.hhoa.kline.core.core.tools.specs.AccessMcpResourceTool;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/** 访问 MCP 资源的工具处理器。 */
public class AccessMcpResourceHandler implements StateFullToolHandler {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    /** AccessMcpResourceHandler 的阶段状态 */
    @Getter
    @Setter
    public static class AccessMcpToolState extends ToolState {
        private String serverName;
        private String uri;
    }

    @Override
    public String getName() {
        return ClineDefaultTool.MCP_ACCESS.getValue();
    }

    @Override
    public ToolState createToolState() {
        return new AccessMcpToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        return "[" + block.getName() + " for '" + (serverName == null ? "" : serverName) + "']";
    }

    @Override
    public ToolSpec getToolSpec() {
        return AccessMcpResourceTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        String uri = HandlerUtils.getStringParam(block, "uri");

        Map<String, Object> partialMessageMap = new HashMap<>();
        partialMessageMap.put("type", "access_mcp_resource");
        partialMessageMap.put("serverName", serverName);
        partialMessageMap.put("toolName", null);
        partialMessageMap.put("uri", uri);
        partialMessageMap.put("arguments", null);
        String partialMessage = JsonUtils.toJsonString(partialMessageMap);

        Boolean shouldAutoApprove = ui.shouldAutoApproveTool(block.getName());

        if (shouldAutoApprove) {
            ui.say(
                    ClineSay.USE_MCP_SERVER,
                    partialMessage,
                    null,
                    null,
                    true,
                    ClineMessageFormat.JSON);
        } else {
            ui.ask(ClineAsk.USE_MCP_SERVER, partialMessage, true, ClineMessageFormat.JSON);
        }
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        String uri = HandlerUtils.getStringParam(block, "uri");

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("type", "access_mcp_resource");
        completeMessageMap.put("serverName", serverName);
        completeMessageMap.put("toolName", null);
        completeMessageMap.put("uri", uri);
        completeMessageMap.put("arguments", null);
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean shouldApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());
        if (Boolean.TRUE.equals(shouldApprove)) {
            // Auto-approval flow
            context.getCallbacks()
                    .say(
                            ClineSay.USE_MCP_SERVER,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);

            // Capture telemetry
            captureTelemetry(context, block, true, true);

            context.getCallbacks()
                    .say(ClineSay.MCP_SERVER_REQUEST_STARTED, null, null, null, null, null);
            return executeMcpReadAndRespond(context, serverName, uri);
        }

        // Manual approval flow — save state and return PendingAsk
        String notificationMessage =
                "Cline wants to access "
                        + (uri == null ? "unknown resource" : uri)
                        + " on "
                        + (serverName == null ? "unknown server" : serverName);

        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                notificationMessage,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, message) -> {});

        AccessMcpToolState state = (AccessMcpToolState) context.getToolState();
        state.setPhase(1);
        state.setServerName(serverName);
        state.setUri(uri);

        var token =
                ToolResultUtils.askApprovalAndPushFeedbackForToken(
                        ClineAsk.USE_MCP_SERVER,
                        completeMessage,
                        context,
                        ClineMessageFormat.JSON,
                        block,
                        getDescription(block));
        return new ToolExecuteResult.PendingAsk(token);
    }

    @Override
    public ToolExecuteResult resume(
            ToolContext context, ToolUse block, ToolState toolState, AskResult askResult) {
        AccessMcpToolState state = (AccessMcpToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false);
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true);
        context.getCallbacks()
                .say(ClineSay.MCP_SERVER_REQUEST_STARTED, null, null, null, null, null);
        return executeMcpReadAndRespond(context, state.getServerName(), state.getUri());
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

    private ToolExecuteResult executeMcpReadAndRespond(
            ToolContext config, String serverName, String uri) {
        try {
            if (config.getServices().getMcpHub() == null) {
                throw new IllegalArgumentException("未找到服务器: " + serverName);
            }
            McpSchema.ReadResourceResult resourceResult =
                    config.getServices().getMcpHub().readResource(serverName, uri);
            String pretty = formatExternalResourceResult(resourceResult);
            config.getCallbacks().say(ClineSay.MCP_SERVER_RESPONSE, pretty, null, null, null, null);
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(pretty, null, null));
        } catch (Exception e) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError("Error reading resource: " + e.getMessage()));
        }
    }

    private static String formatExternalResourceResult(McpSchema.ReadResourceResult result) {
        if (result == null || result.contents() == null || result.contents().isEmpty()) {
            return "(Empty response)";
        }

        List<String> parts = new ArrayList<>();
        for (McpSchema.ResourceContents content : result.contents()) {
            if (content == null) {
                continue;
            }

            if (content instanceof McpSchema.TextResourceContents textContent) {
                String text = textContent.text();
                if (text != null && !text.isEmpty()) {
                    parts.add(text);
                    continue;
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("uri: ").append(content.uri());
            if (content.mimeType() != null && !content.mimeType().isEmpty()) {
                sb.append("\nmimeType: ").append(content.mimeType());
            }
            if (content instanceof McpSchema.BlobResourceContents blobContent) {
                sb.append("\nblob: ").append(blobContent.blob());
            }
            parts.add(sb.toString());
        }

        if (parts.isEmpty()) {
            return "(Empty response)";
        }

        return String.join("\n\n", parts);
    }
}
