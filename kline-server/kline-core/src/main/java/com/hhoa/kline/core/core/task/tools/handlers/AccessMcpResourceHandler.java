package com.hhoa.kline.core.core.task.tools.handlers;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.AccessMcpResourceTool;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.task.tools.types.TaskConfig;
import com.hhoa.kline.core.core.task.tools.types.UIHelpers;
import com.hhoa.kline.core.core.task.tools.utils.ToolResultUtils;
import com.hhoa.kline.core.core.utils.StringUtils;
import com.hhoa.kline.core.enums.ClineDefaultTool;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 访问 MCP 资源的工具处理器。 */
public class AccessMcpResourceHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();

    @Override
    public String getName() {
        return ClineDefaultTool.MCP_ACCESS.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        return "[" + block.getName() + " for '" + (serverName == null ? "" : serverName) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
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
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        String uri = HandlerUtils.getStringParam(block, "uri");

        if (StringUtils.isBlank(serverName)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult =
                    config.getCallbacks()
                            .sayAndCreateMissingParamError(
                                    ClineDefaultTool.MCP_ACCESS.getValue(), "server_name");
            return HandlerUtils.createTextBlocks(errorResult);
        }
        if (StringUtils.isBlank(uri)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult =
                    config.getCallbacks()
                            .sayAndCreateMissingParamError(
                                    ClineDefaultTool.MCP_ACCESS.getValue(), "uri");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("type", "access_mcp_resource");
        completeMessageMap.put("serverName", serverName);
        completeMessageMap.put("toolName", null);
        completeMessageMap.put("uri", uri);
        completeMessageMap.put("arguments", null);
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        Boolean shouldApprove = config.getCallbacks().shouldAutoApproveTool(block.getName());
        if (Boolean.TRUE.equals(shouldApprove)) {
            // Auto-approval flow
            config.getCallbacks()
                    .say(
                            ClineSay.USE_MCP_SERVER,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);
            if (!config.isYoloModeToggled()) {
                config.getTaskState()
                        .setConsecutiveAutoApprovedRequestsCount(
                                config.getTaskState().getConsecutiveAutoApprovedRequestsCount()
                                        + 1);
            }

            // Capture telemetry
            if (config.getServices() != null
                    && config.getServices().getTelemetryService() != null) {
                String modelId =
                        config.getApi() != null && config.getApi().getModel() != null
                                ? config.getApi().getModel().getId()
                                : "unknown";
                config.getServices()
                        .getTelemetryService()
                        .captureToolUsage(config.getUlid(), block.getName(), modelId, true, true);
            }

            config.getCallbacks()
                    .say(ClineSay.MCP_SERVER_REQUEST_STARTED, null, null, null, null, null);
            return executeMcpReadAndRespond(config, serverName, uri);
        } else {
            // Manual approval flow
            String notificationMessage =
                    "Cline wants to access "
                            + (uri == null ? "unknown resource" : uri)
                            + " on "
                            + (serverName == null ? "unknown server" : serverName);

            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    notificationMessage,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, message) -> {});

            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.USE_MCP_SERVER,
                            completeMessage,
                            config,
                            ClineMessageFormat.JSON);
            if (!didApprove) {
                // Capture telemetry for denial
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
                // Capture telemetry for approval
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

                config.getCallbacks()
                        .say(ClineSay.MCP_SERVER_REQUEST_STARTED, null, null, null, null, null);
                return executeMcpReadAndRespond(config, serverName, uri);
            }
        }
    }

    private List<UserContentBlock> executeMcpReadAndRespond(
            TaskConfig config, String serverName, String uri) {
        try {
            if (config.getServices().getMcpHub() == null) {
                throw new IllegalArgumentException("未找到服务器: " + serverName);
            }
            McpSchema.ReadResourceResult resourceResult =
                    config.getServices().getMcpHub().readResource(serverName, uri);
            String pretty = formatExternalResourceResult(resourceResult);
            config.getCallbacks().say(ClineSay.MCP_SERVER_RESPONSE, pretty, null, null, null, null);
            return HandlerUtils.createTextBlocks(formatResponse.toolResult(pretty, null, null));
        } catch (Exception e) {
            return HandlerUtils.createTextBlocks(
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
