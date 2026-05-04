package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ApplyPatchInput(
        @JsonProperty(value = "input", required = true)
                @JsonPropertyDescription("The apply_patch command that you wish to execute.")
                String input) {}
