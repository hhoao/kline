package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SubagentInput(
        @JsonProperty(value = "prompt_1", required = true)
                @JsonPropertyDescription("First subagent prompt.")
                String prompt1,
        @JsonProperty(value = "prompt_2", required = false)
                @JsonPropertyDescription("Optional second subagent prompt.")
                String prompt2,
        @JsonProperty(value = "prompt_3", required = false)
                @JsonPropertyDescription("Optional third subagent prompt.")
                String prompt3,
        @JsonProperty(value = "prompt_4", required = false)
                @JsonPropertyDescription("Optional fourth subagent prompt.")
                String prompt4,
        @JsonProperty(value = "prompt_5", required = false)
                @JsonPropertyDescription("Optional fifth subagent prompt.")
                String prompt5) {}
