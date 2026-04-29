package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record PlanModeRespondInput(
        @JsonProperty(value = "response", required = true)
                @JsonPropertyDescription("The response to provide to the user.")
                String response,
        @JsonProperty(value = "needs_more_exploration", required = false)
                @JsonPropertyDescription(
                        "Whether more exploration is needed before presenting a complete plan.")
                Boolean needsMoreExploration,
        @JsonProperty(value = "options", required = false)
                @JsonPropertyDescription("Options for the user to choose from.")
                String options,
        @JsonProperty(value = "task_progress", required = false)
                @JsonPropertyDescription(
                        "A checklist showing task progress after this tool use is completed.")
                String taskProgress) {}
