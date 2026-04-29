package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ReportBugInput(
        @JsonProperty(value = "title", required = true)
                @JsonPropertyDescription("The title of the bug report.")
                String title,
        @JsonProperty(value = "what_happened", required = true)
                @JsonPropertyDescription("A description of what happened.")
                String whatHappened,
        @JsonProperty(value = "steps_to_reproduce", required = true)
                @JsonPropertyDescription("Steps to reproduce the issue.")
                String stepsToReproduce,
        @JsonProperty(value = "api_request_output", required = true)
                @JsonPropertyDescription("Relevant API request output.")
                String apiRequestOutput,
        @JsonProperty(value = "additional_context", required = true)
                @JsonPropertyDescription("Additional context for the bug report.")
                String additionalContext) {}
