package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record WebSearchInput(
        @JsonProperty(value = "query", required = true)
                @JsonPropertyDescription("The search query to use.")
                String query,
        @JsonProperty(value = "allowed_domains", required = false)
                @JsonPropertyDescription("Domains to restrict results to.")
                String allowedDomains,
        @JsonProperty(value = "blocked_domains", required = false)
                @JsonPropertyDescription("Domains to exclude from results.")
                String blockedDomains) {}
