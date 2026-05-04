package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SearchFilesInput(
        @JsonProperty(value = "path", required = true)
                @JsonPropertyDescription("The path of the directory to search in.")
                String path,
        @JsonProperty(value = "regex", required = true)
                @JsonPropertyDescription("The regular expression pattern to search for.")
                String regex,
        @JsonProperty(value = "file_pattern", required = false)
                @JsonPropertyDescription("Glob pattern to filter files.")
                String filePattern) {}
