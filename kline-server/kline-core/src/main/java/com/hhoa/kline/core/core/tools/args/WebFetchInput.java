package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record WebFetchInput(
        @JsonProperty(value = "url", required = true)
                @JsonPropertyDescription("The URL to fetch content from.")
                String url,
        @JsonProperty(value = "prompt", required = true)
                @JsonPropertyDescription("The prompt to use for analyzing the webpage content.")
                String prompt) {}
