package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ListCodeDefinitionNamesInput(
        @JsonProperty(value = "path", required = true)
                @JsonPropertyDescription(
                        "The path of a directory relative to the current working directory.")
                String path) {}
