package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ActModeRespondInput(
        @JsonProperty(value = "response", required = true)
                @JsonPropertyDescription("The message to provide to the user.")
                String response,
        @JsonProperty(value = "task_progress", required = false)
                @JsonPropertyDescription(
                        "A checklist showing task progress with the latest status.")
                String taskProgress) {}
