package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ActModeRespondInput(
        @JsonProperty(value = "response", required = true)
                @JsonPropertyDescription("The message to provide to the user.")
                String response) {}
