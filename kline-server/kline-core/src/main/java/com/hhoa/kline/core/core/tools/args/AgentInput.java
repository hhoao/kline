package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AgentInput(
        @JsonProperty(value = "description", required = true)
                @JsonPropertyDescription("A short (3-5 word) description of the task.")
                String description,
        @JsonProperty(value = "prompt", required = true)
                @JsonPropertyDescription("The task for the agent to perform.")
                String prompt,
        @JsonProperty(value = "subagent_type", required = false)
                @JsonPropertyDescription("The type of specialized agent to use for this task.")
                String subagentType,
        @JsonProperty(value = "model", required = false)
                @JsonPropertyDescription("Optional model override: sonnet, opus, or haiku.")
                String model,
        @JsonProperty(value = "run_in_background", required = false)
                @JsonPropertyDescription(
                        "Set to true to run this agent in the background. Background execution is not available in this server runtime yet.")
                Boolean runInBackground) {}
