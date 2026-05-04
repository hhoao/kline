package com.hhoa.kline.core.core.tools.args;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public record TodoWriteInput(
        @JsonProperty(value = "todos", required = true)
                @JsonPropertyDescription("The updated todo list.")
                List<TodoItem> todos) {

    public record TodoItem(
            @JsonProperty(value = "content", required = true)
                    @JsonPropertyDescription(
                            "Imperative form describing what needs to be done.")
                    String content,
            @JsonProperty(value = "status", required = true)
                    @JsonPropertyDescription("Current state of the task.")
                    TodoStatus status,
            @JsonProperty(value = "activeForm", required = true)
                    @JsonPropertyDescription(
                            "Present continuous form shown while this task is in progress.")
                    String activeForm) {}

    public enum TodoStatus {
        @JsonProperty("pending")
        PENDING,
        @JsonProperty("in_progress")
        IN_PROGRESS,
        @JsonProperty("completed")
        COMPLETED
    }
}
