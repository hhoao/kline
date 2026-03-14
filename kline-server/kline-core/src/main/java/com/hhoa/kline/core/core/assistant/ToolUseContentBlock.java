package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ToolUseContentBlock extends UserContentBlock {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("input")
    private Object input;

    public ToolUseContentBlock(String id, String name, Object input) {
        this.id = id;
        this.name = name;
        this.input = input;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.TOOL_USE;
    }
}
