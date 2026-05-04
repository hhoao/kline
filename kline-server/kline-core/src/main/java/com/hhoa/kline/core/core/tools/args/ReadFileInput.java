package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReadFileInput(
        @JsonProperty(value = "path", required = true)
                @JsonPropertyDescription("The path of the file to read.")
                String path,
        @JsonProperty(value = "start_line", required = false)
                @JsonPropertyDescription("The starting line number to read from.")
                Integer startLine,
        @JsonProperty(value = "end_line", required = false)
                @JsonPropertyDescription("The ending line number to read to.")
                Integer endLine) {}
