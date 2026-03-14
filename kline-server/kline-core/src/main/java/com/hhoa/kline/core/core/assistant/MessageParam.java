package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

/**
 * @see <a
 *     href="https://github.com/anthropics/anthropic-sdk-typescript/blob/main/src/resources/messages/messages.ts">TypeScript
 *     MessageParam</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "role")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AssistantMessage.class, name = "assistant"),
    @JsonSubTypes.Type(value = AssistantMessage.class, name = "ASSISTANT"),
    @JsonSubTypes.Type(value = UserMessage.class, name = "user"),
    @JsonSubTypes.Type(value = UserMessage.class, name = "USER")
})
public interface MessageParam {
    MessageRole getRole();

    List<UserContentBlock> getContent();
}
