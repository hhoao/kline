package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record NewTaskInput(
        @JsonProperty(value = "context", required = true)
                @JsonPropertyDescription("The context to preload the new task with.")
                String context) {}
