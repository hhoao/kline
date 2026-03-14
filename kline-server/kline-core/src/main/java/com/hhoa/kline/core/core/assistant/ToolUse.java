package com.hhoa.kline.core.core.assistant;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ToolUse extends AssistantMessageContent {

    private String name;

    private Map<String, Object> params;

    public ToolUse() {
        this.type = "tool_use";
    }

    public ToolUse(String name, Map<String, Object> params, Boolean partial) {
        this();
        this.name = name;
        this.params = params;
        this.partial = partial;
    }
}
