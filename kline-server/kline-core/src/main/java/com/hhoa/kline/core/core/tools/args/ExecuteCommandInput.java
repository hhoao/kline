package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ExecuteCommandInput(
        @JsonProperty(value = "command", required = true)
                @JsonPropertyDescription(
                        "The CLI command to execute. This should be valid for the current operating system. Ensure the command is properly formatted and does not contain any harmful instructions.")
                String command,
        @JsonProperty(value = "requires_approval", required = true)
                @JsonPropertyDescription(
                        "A boolean indicating whether this command requires explicit user approval before execution in case the user has auto-approve mode enabled. Set to true for potentially impactful operations and false for safe, non-destructive operations.")
                Boolean requiresApproval,
        @JsonProperty(value = "timeout", required = false)
                @JsonPropertyDescription(
                        "Integer representing the timeout in seconds for how long to run the terminal command before timing out and continuing the task.")
                Integer timeout) {}
