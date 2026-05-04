package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record WriteToFileInput(
        @JsonProperty(value = "path", required = false)
                @JsonPropertyDescription("The path of the file to write to.")
                String path,
        @JsonProperty(value = "absolutePath", required = false)
                @JsonPropertyDescription("The absolute path of the file to write to.")
                String absolutePath,
        @JsonProperty(value = "content", required = true)
                @JsonPropertyDescription("The complete content to write to the file.")
                String content) {}
