package com.hhoa.kline.core.core.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import java.util.LinkedHashMap;
import java.util.Map;

/** 将动态工具名（如 {@code use_subagent_*}）委托给同一实现（如 {@code SubagentToolHandler}）。 */
public final class DelegatingToolHandler implements ToolHandler<DelegatingToolHandler.DelegatedPromptInput> {

    @SuppressWarnings("unused")
    private final String name;
    private final ToolHandler<?> delegate;
    private final ToolSpec specOverride;

    public DelegatingToolHandler(String name, ToolHandler<?> delegate) {
        this(name, delegate, null);
    }

    public DelegatingToolHandler(String name, ToolHandler<?> delegate, ToolSpec specOverride) {
        this.name = name;
        this.delegate = delegate;
        this.specOverride = specOverride;
    }

    @Override
    public String getDescription(ToolUse block) {
        return delegate.getDescription(block);
    }

    @Override
    public void handlePartialBlock(DelegatedPromptInput input, ToolContext context, ToolUse block) {
        ToolHandlerInvocationSupport.handlePartialBlock(
                delegate, context, normalizeDelegatedInput(block));
    }

    @Override
    public ToolExecuteResult execute(
            DelegatedPromptInput input, ToolContext context, ToolUse block) {
        return ToolHandlerInvocationSupport.invoke(
                delegate, context, normalizeDelegatedInput(block));
    }

    private ToolUse normalizeDelegatedInput(ToolUse block) {
        if (specOverride == null || block == null || block.getParams() == null) {
            return block;
        }
        Map<String, Object> params = block.getParams();
        if (!params.containsKey("prompt") || params.containsKey("prompt_1")) {
            return block;
        }
        Map<String, Object> normalized = new LinkedHashMap<>(params);
        normalized.put("prompt_1", normalized.remove("prompt"));
        ToolUse copy = new ToolUse(block.getName(), normalized, block.isPartial());
        copy.setId(block.getId());
        copy.setSignature(block.getSignature());
        copy.setCallId(block.getCallId());
        copy.setNativeToolCall(block.isNativeToolCall());
        copy.setReasoningDetails(block.getReasoningDetails());
        return copy;
    }

    public record DelegatedPromptInput(
            @JsonProperty(value = "prompt", required = false) String prompt) {}
}
