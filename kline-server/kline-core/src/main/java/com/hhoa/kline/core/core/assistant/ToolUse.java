package com.hhoa.kline.core.core.assistant;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ToolUse extends AssistantMessageContent {

    /** 工具使用的唯一标识，由执行框架在工具执行前分配 */
    private String id;

    private String name;

    private Map<String, Object> params;

    /** The tool call signature (for native tool calls). */
    private String signature;

    /** The call_id for tracking tool execution chains. */
    private String callId;

    /** Whether this tool use came from a native tool call (not XML-parsed). */
    private boolean isNativeToolCall;

    /** Reasoning details attached to this tool use (from reasoning summary). */
    private Object reasoningDetails;

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
