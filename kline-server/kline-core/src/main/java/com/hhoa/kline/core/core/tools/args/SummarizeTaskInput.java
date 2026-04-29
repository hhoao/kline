package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SummarizeTaskInput(
        @JsonProperty(value = "context", required = true)
                @JsonPropertyDescription("Task context to summarize.")
                String context) {}
