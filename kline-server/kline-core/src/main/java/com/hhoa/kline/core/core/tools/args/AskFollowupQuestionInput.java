package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AskFollowupQuestionInput(
        @JsonProperty(value = "question", required = true)
                @JsonPropertyDescription("The question to ask the user.")
                String question,
        @JsonProperty(value = "options", required = false)
                @JsonPropertyDescription("An array of 2-5 options for the user to choose from.")
                String options) {}
