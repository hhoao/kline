package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record BrowserActionInput(
        @JsonProperty(value = "action", required = true)
                @JsonPropertyDescription("The browser action to perform.")
                String action,
        @JsonProperty(value = "url", required = false)
                @JsonPropertyDescription("The URL for the launch action.")
                String url,
        @JsonProperty(value = "coordinate", required = false)
                @JsonPropertyDescription("The x,y coordinates for the click action.")
                String coordinate,
        @JsonProperty(value = "text", required = false)
                @JsonPropertyDescription("The text for the type action.")
                String text) {}
