package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record UseMcpToolInput(
        @JsonProperty(value = "server_name", required = true)
                @JsonPropertyDescription("The name of the MCP server providing the tool.")
                String serverName,
        @JsonProperty(value = "tool_name", required = true)
                @JsonPropertyDescription("The name of the tool to execute.")
                String toolName,
        @JsonProperty(value = "arguments", required = true)
                @JsonPropertyDescription("A JSON object containing the tool's input parameters.")
                String arguments,
        @JsonProperty(value = "task_progress", required = false)
                @JsonPropertyDescription(
                        "A checklist showing task progress after this tool use is completed.")
                String taskProgress) {}
