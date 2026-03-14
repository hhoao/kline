package com.hhoa.kline.core.core.task.tools.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.assistant.UserContentBlock;
import com.hhoa.kline.core.core.prompts.ResponseFormatter;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.tools.UseMcpToolTool;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
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
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UseMcpToolHandler implements FullyManagedTool {

    private final ResponseFormatter formatResponse = new ResponseFormatter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return ClineDefaultTool.MCP_USE.getValue();
    }

    @Override
    public String getDescription(ToolUse block) {
        String server = HandlerUtils.getStringParam(block, "server_name");
        return "[" + block.getName() + " for '" + (server == null ? "" : server) + "']";
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return UseMcpToolTool.create(ModelFamily.GENERIC);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers ui) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        String toolName = HandlerUtils.getStringParam(block, "tool_name");
        String mcpArguments = HandlerUtils.getStringParam(block, "arguments");

        Map<String, Object> partialMessageMap = new HashMap<>();
        partialMessageMap.put("type", "use_mcp_tool");
        partialMessageMap.put("serverName", serverName);
        partialMessageMap.put("toolName", toolName);
        partialMessageMap.put("arguments", mcpArguments);
        String partialMessage = JsonUtils.toJsonString(partialMessageMap);

        TaskConfig config = ui.getConfig();
        boolean autoApprove = config.getCallbacks().shouldAutoApproveTool(block.getName());

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

    @Override
    public List<UserContentBlock> execute(TaskConfig config, ToolUse block) {
        String serverName = HandlerUtils.getStringParam(block, "server_name");
        String toolName = HandlerUtils.getStringParam(block, "tool_name");
        String mcpArguments = HandlerUtils.getStringParam(block, "arguments");

        if (StringUtils.isBlank(serverName)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult =
                    config.getCallbacks()
                            .sayAndCreateMissingParamError(block.getName(), "server_name");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        if (StringUtils.isBlank(toolName)) {
            config.getTaskState()
                    .setConsecutiveMistakeCount(
                            config.getTaskState().getConsecutiveMistakeCount() + 1);
            String errorResult =
                    config.getCallbacks()
                            .sayAndCreateMissingParamError(block.getName(), "tool_name");
            return HandlerUtils.createTextBlocks(errorResult);
        }

        final Map<String, Object> parsedArguments;
        if (mcpArguments != null && !mcpArguments.isEmpty()) {
            try {
                JsonNode jsonNode = objectMapper.readTree(mcpArguments);
                @SuppressWarnings("unchecked")
                Map<String, Object> parsed = objectMapper.convertValue(jsonNode, Map.class);
                parsedArguments = parsed;
            } catch (Exception e) {
                config.getTaskState()
                        .setConsecutiveMistakeCount(
                                config.getTaskState().getConsecutiveMistakeCount() + 1);
                config.getCallbacks()
                        .say(
                                ClineSay.ERROR,
                                "Cline tried to use "
                                        + toolName
                                        + " with an invalid JSON argument. Retrying...",
                                null,
                                null,
                                null,
                                null);
                return HandlerUtils.createTextBlocks(
                        formatResponse.toolError(
                                formatResponse.invalidMcpToolArgumentError(serverName, toolName)));
            }
        } else {
            parsedArguments = null;
        }

        config.getTaskState().setConsecutiveMistakeCount(0);

        Map<String, Object> completeMessageMap = new HashMap<>();
        completeMessageMap.put("type", "use_mcp_tool");
        completeMessageMap.put("serverName", serverName);
        completeMessageMap.put("toolName", toolName);
        completeMessageMap.put("arguments", mcpArguments);
        String completeMessage = JsonUtils.toJsonString(completeMessageMap);

        boolean isToolAutoApproved = false;
        if (config.getServices() != null && config.getServices().getMcpHub() != null) {
            try {
                isToolAutoApproved =
                        config.getServices().getMcpHub().isToolAutoApproved(serverName, toolName);
            } catch (Exception e) {
                log.error("Failed to check MCP tool auto-approval status: " + e.getMessage());
            }
        }

        Boolean shouldAutoApprove = config.getCallbacks().shouldAutoApproveTool(block.getName());
        boolean autoApprove = shouldAutoApprove && isToolAutoApproved;

        if (autoApprove) {
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

            return executeMcpToolCall(config, serverName, toolName, parsedArguments);
        } else {
            String notificationMessage =
                    "Cline wants to use "
                            + (toolName == null ? "unknown tool" : toolName)
                            + " on "
                            + (serverName == null ? "unknown server" : serverName);

            TaskUtils.showNotificationForApprovalIfAutoApprovalEnabled(
                    notificationMessage,
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnabled(),
                    config.getAutoApprovalSettings() != null
                            && config.getAutoApprovalSettings().isEnableNotifications(),
                    (subtitle, msg) -> {});

            Boolean didApprove =
                    ToolResultUtils.askApprovalAndPushFeedback(
                            ClineAsk.USE_MCP_SERVER,
                            completeMessage,
                            config,
                            ClineMessageFormat.JSON);
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

                return executeMcpToolCall(config, serverName, toolName, parsedArguments);
            }
        }
    }

    private List<UserContentBlock> executeMcpToolCall(
            TaskConfig config,
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
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolResult(toolResultText.toString(), imagesArray, null));
        } catch (Exception error) {
            return HandlerUtils.createTextBlocks(
                    formatResponse.toolError(
                            "Error executing MCP tool: "
                                    + (error.getMessage() != null
                                            ? error.getMessage()
                                            : "Unknown error")));
        }
    }
}
