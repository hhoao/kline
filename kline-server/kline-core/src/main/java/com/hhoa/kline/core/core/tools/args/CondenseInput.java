package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CondenseInput(
        @JsonProperty(value = "context", required = true)
                @JsonPropertyDescription("Context to use when condensing the conversation.")
                String context) {}
