package com.hhoa.kline.core.core.tools.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.shared.ClineAsk;
import com.hhoa.kline.core.core.shared.ClineMessageFormat;
import com.hhoa.kline.core.core.shared.ClineSay;
import com.hhoa.kline.core.core.task.AskResult;
import com.hhoa.kline.core.core.task.TaskUtils;
import com.hhoa.kline.core.core.tools.args.UseMcpToolInput;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.ToolState;
import com.hhoa.kline.core.core.tools.types.UIHelpers;
import com.hhoa.kline.core.core.tools.utils.ToolResultUtils;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UseMcpToolHandler implements StateFullToolHandler<UseMcpToolInput> {

    private final ResponseFormatter formatResponse = new ResponseFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Getter
    @Setter
    public static class UseMcpToolState extends ToolState {
        private String serverName;
        private String toolName;
        private Map<String, Object> parsedArguments;
    }

    @Override
    public ToolState createToolState() {
        return new UseMcpToolState();
    }

    @Override
    public String getDescription(ToolUse block) {
        String server = HandlerUtils.getStringParam(block, "server_name");
        return "[" + block.getName() + " for '" + (server == null ? "" : server) + "']";
    }

    public void handlePartialBlock(UseMcpToolInput input, ToolContext context, ToolUse block) {
        UIHelpers ui = UIHelpers.create(context);
        String serverName = input.serverName();
        String toolName = input.toolName();
        String mcpArguments = input.arguments();

        Map<String, Object> partialMessageMap = new HashMap<>();
        partialMessageMap.put("type", "use_mcp_tool");
        partialMessageMap.put("serverName", serverName);
        partialMessageMap.put("toolName", toolName);
        partialMessageMap.put("arguments", mcpArguments);
        String partialMessage = JsonUtils.toJsonString(partialMessageMap);

        boolean autoApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());

        if (autoApprove) {
            ui.say(
                    ClineSay.USE_MCP_SERVER,
                    partialMessage,
                    null,
                    null,
                    block.isPartial(),
                    ClineMessageFormat.JSON);
        } else {
            ui.ask(
                    ClineAsk.USE_MCP_SERVER,
                    partialMessage,
                    block.isPartial(),
                    ClineMessageFormat.JSON);
        }
    }

    public ToolExecuteResult execute(UseMcpToolInput input, ToolContext context, ToolUse block) {
        String serverName = input.serverName();
        String toolName = input.toolName();
        String mcpArguments = input.arguments();

        final Map<String, Object> parsedArguments;
        if (mcpArguments != null && !mcpArguments.isEmpty()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(mcpArguments);
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.convertValue(jsonNode, Map.class);
                parsedArguments = parsed;
            } catch (Exception e) {
                context.getTaskState()
                        .getApiTurnState()
                        .setConsecutiveMistakeCount(
                                context.getTaskState()
                                                .getApiTurnState()
                                                .getConsecutiveMistakeCount()
                                        + 1);
                context.getCallbacks()
                        .say(
                                ClineSay.ERROR,
                                "Cline tried to use "
                                        + toolName
                                        + " with an invalid JSON argument. Retrying...",
                                null,
                                null,
                                null,
                                null);
                return HandlerUtils.createToolExecuteResult(
                        formatResponse.toolError(
                                formatResponse.invalidMcpToolArgumentError(serverName, toolName)));
            }
        } else {
            parsedArguments = null;
        }

        context.getTaskState().getApiTurnState().setConsecutiveMistakeCount(0);

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("type", "use_mcp_tool");
        completeMessageMap.put("serverName", serverName);
        completeMessageMap.put("toolName", toolName);
        completeMessageMap.put("arguments", mcpArguments);
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        boolean isToolAutoApproved = false;
        if (context.getServices() != null && context.getServices().getMcpHub() != null) {
            try {
                isToolAutoApproved =
                        context.getServices().getMcpHub().isToolAutoApproved(serverName, toolName);
            } catch (Exception e) {
                log.error("Failed to check MCP tool auto-approval status: " + e.getMessage());
            }
        }

        Boolean shouldAutoApprove = context.getCallbacks().shouldAutoApproveTool(block.getName());
        boolean autoApprove = Boolean.TRUE.equals(shouldAutoApprove) || isToolAutoApproved;

        if (autoApprove) {
            context.getCallbacks()
                    .say(
                            ClineSay.USE_MCP_SERVER,
                            completeMessage,
                            null,
                            null,
                            false,
                            ClineMessageFormat.JSON);

            captureTelemetry(context, block, true, true);

            return executeMcpToolCall(context, serverName, toolName, parsedArguments);
        }

        // Need to ask user -- save state and return PendingAsk
        String notificationMessage =
                "Cline wants to use "
                        + (toolName == null ? "unknown tool" : toolName)
                        + " on "
                        + (serverName == null ? "unknown server" : serverName);

        TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                notificationMessage,
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnabled(),
                context.getAutoApprovalSettings() != null
                        && context.getAutoApprovalSettings().isEnableNotifications(),
                (subtitle, msg) -> {});

        UseMcpToolState state = (UseMcpToolState) context.getToolState();
        state.setPhase(1);
        state.setServerName(serverName);
        state.setToolName(toolName);
        state.setParsedArguments(parsedArguments);

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
        UseMcpToolState state = (UseMcpToolState) toolState;

        boolean approved = ToolResultUtils.processAskResult(askResult, context);
        if (!approved) {
            captureTelemetry(context, block, false, false);
            return HandlerUtils.createToolExecuteResult(formatResponse.toolDenied());
        }

        captureTelemetry(context, block, false, true);
        return executeMcpToolCall(
                context, state.getServerName(), state.getToolName(), state.getParsedArguments());
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

    private ToolExecuteResult executeMcpToolCall(
            ToolContext config,
            String serverName,
            String toolName,
            Map<String, Object> parsedArguments) {
        config.getCallbacks()
                .say(ClineSay.MCP_SERVER_REQUEST_STARTED, null, null, null, null, null);
        try {
            List<IMcpHub.Notification> notificationsBefore =
                    config.getServices().getMcpHub().getPendingNotifications();
            for (IMcpHub.Notification notification : notificationsBefore) {
                config.getCallbacks()
                        .say(
                                ClineSay.MCP_NOTIFICATION,
                                "[" + notification.serverName() + "] " + notification.message(),
                                null,
                                null,
                                false,
                                null);
            }

            McpSchema.CallToolResult toolResult =
                    config.getServices()
                            .getMcpHub()
                            .callTool(serverName, toolName, parsedArguments, config.getUlid());

            List<IMcpHub.Notification> notificationsAfter =
                    config.getServices().getMcpHub().getPendingNotifications();
            for (IMcpHub.Notification notification : notificationsAfter) {
                config.getCallbacks()
                        .say(
                                ClineSay.MCP_NOTIFICATION,
                                "[" + notification.serverName() + "] " + notification.message(),
                                null,
                                null,
                                false,
                                null);
            }

            List<String> toolResultImages = new ArrayList<>();
            StringBuilder toolResultText = new StringBuilder();

            if (Boolean.TRUE.equals(toolResult.isError())) {
                toolResultText.append("Error:\n");
            }

            List<McpSchema.Content> contents = toolResult.content();
            if (contents != null) {
                for (McpSchema.Content item : contents) {
                    if (item instanceof McpSchema.ImageContent imageContent) {
                        String mimeType = imageContent.mimeType();
                        String data = imageContent.data();
                        if (mimeType != null && data != null) {
                            toolResultImages.add("data:" + mimeType + ";base64," + data);
                        }
                    } else if (item instanceof McpSchema.TextContent textContent) {
                        String text = textContent.text();
                        if (text != null && !text.isEmpty()) {
                            if (toolResultText.length() > 0) {
                                toolResultText.append("\n\n");
                            }
                            toolResultText.append(text);
                        }
                    } else if (item instanceof McpSchema.EmbeddedResource embeddedResource) {
                        if (!toolResultText.isEmpty()) {
                            toolResultText.append("\n\n");
                        }
                        try {
                            McpSchema.ResourceContents resourceContents =
                                    embeddedResource.resource();
                            Map<String, Object> resourceView = new HashMap<>();
                            resourceView.put("type", "resource");
                            resourceView.put("uri", resourceContents.uri());
                            resourceView.put("mimeType", resourceContents.mimeType());
                            if (resourceContents
                                    instanceof
                                    McpSchema.TextResourceContents textResourceContents) {
                                resourceView.put("text", textResourceContents.text());
                            }
                            toolResultText.append(objectMapper.writeValueAsString(resourceView));
                        } catch (Exception e) {
                            toolResultText.append("(Resource data)");
                        }
                    }
                }
            }

            if (toolResultText.length() == 0) {
                toolResultText.append("(No response)");
            }

            String toolResultToDisplay = toolResultText.toString();
            if (!toolResultImages.isEmpty()) {
                for (String image : toolResultImages) {
                    toolResultToDisplay += "\n\n" + image;
                }
            }
            config.getCallbacks()
                    .say(ClineSay.MCP_SERVER_RESPONSE, toolResultToDisplay, null, null, null, null);

            boolean supportsImages = false;
            // TODO: Check model.info.supportsImages if available

            if (!toolResultImages.isEmpty() && !supportsImages) {
                toolResultText.append(
                        "\n\n["
                                + toolResultImages.size()
                                + " images were provided in the response, and while they are displayed to the user, you do not have the ability to view them.]");
            }

            String[] imagesArray =
                    supportsImages && !toolResultImages.isEmpty()
                            ? toolResultImages.toArray(new String[0])
                            : null;
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolResult(toolResultText.toString(), imagesArray, null));
        } catch (Exception error) {
            return HandlerUtils.createToolExecuteResult(
                    formatResponse.toolError(
                            "Error executing MCP tool: "
                                    + (error.getMessage() != null
                                            ? error.getMessage()
                                            : "Unknown error")));
        }
    }
}
