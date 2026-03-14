package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * @see <a
 *     href="https://github.com/anthropics/anthropic-sdk-typescript/blob/main/src/resources/messages/messages.ts">TypeScript
 *     ContentBlockParam</a>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextContentBlock.class, name = "text"),
    @JsonSubTypes.Type(value = ImageContentBlock.class, name = "image"),
    @JsonSubTypes.Type(value = ToolUseContentBlock.class, name = "tool_use"),
    @JsonSubTypes.Type(value = ThinkingContentBlock.class, name = "thinking"),
    @JsonSubTypes.Type(value = RedactedThinkingContentBlock.class, name = "redacted_thinking")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class UserContentBlock {
    /**
     * 获取内容块类型 使用 @JsonIgnore 避免与 @JsonTypeInfo 生成的 type 字段重复
     *
     * @return 内容块类型
     */
    @JsonIgnore
    public abstract ContentBlockType getType();
}
