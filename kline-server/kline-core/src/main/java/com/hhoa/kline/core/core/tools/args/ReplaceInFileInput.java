package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReplaceInFileInput(
        @JsonProperty(value = "path", required = false)
                @JsonPropertyDescription("The path of the file to modify.")
                String path,
        @JsonProperty(value = "absolutePath", required = false)
                @JsonPropertyDescription("The absolute path to the file to modify.")
                String absolutePath,
        @JsonProperty(value = "diff", required = true)
                @JsonPropertyDescription("The search and replace blocks to apply.")
                String diff) {}
