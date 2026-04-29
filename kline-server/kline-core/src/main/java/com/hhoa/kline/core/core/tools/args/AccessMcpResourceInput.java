package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AccessMcpResourceInput(
        @JsonProperty(value = "server_name", required = true)
                @JsonPropertyDescription("The name of the MCP server providing the resource")
                String serverName,
        @JsonProperty(value = "uri", required = true)
                @JsonPropertyDescription("The URI identifying the specific resource to access")
                String uri,
        @JsonProperty(value = "task_progress", required = false)
                @JsonPropertyDescription(
                        "A checklist showing task progress after this tool use is completed.")
                String taskProgress) {}
