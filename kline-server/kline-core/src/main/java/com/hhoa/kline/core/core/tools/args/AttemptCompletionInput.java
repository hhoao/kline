package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AttemptCompletionInput(
        @JsonProperty(value = "result", required = true)
                @JsonPropertyDescription("The result of the completed task.")
                String result,
        @JsonProperty(value = "command", required = false)
                @JsonPropertyDescription(
                        "A CLI command to execute to show a live demo of the result.")
                String command,
        @JsonProperty(value = "task_progress", required = false)
                @JsonPropertyDescription(
                        "A checklist showing task progress after this tool use is completed.")
                String taskProgress) {}
