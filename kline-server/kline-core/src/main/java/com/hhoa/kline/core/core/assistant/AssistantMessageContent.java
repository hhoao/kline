package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContent.class, name = "text"),
    @JsonSubTypes.Type(value = ToolUse.class, name = "tool_use")
})
public abstract class AssistantMessageContent {

    protected String type;

    protected boolean partial;
}
