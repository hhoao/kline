package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record GenerateExplanationInput(
        @JsonProperty(value = "title", required = true)
                @JsonPropertyDescription("A descriptive title for the diff view.")
                String title,
        @JsonProperty(value = "from_ref", required = true)
                @JsonPropertyDescription("The git reference for the before state.")
                String fromRef,
        @JsonProperty(value = "to_ref", required = false)
                @JsonPropertyDescription("The git reference for the after state.")
                String toRef) {}
