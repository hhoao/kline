package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ListFilesArgs(
        @JsonProperty(value = "path", required = true)
                @JsonPropertyDescription(
                        "The path of the directory to list contents for, relative to the current working directory.")
                String path,
        @JsonProperty(value = "recursive", required = false)
                @JsonPropertyDescription(
                        "Whether to list files recursively. Use true for recursive listing, false or omit for top-level only.")
                Boolean recursive) {}
