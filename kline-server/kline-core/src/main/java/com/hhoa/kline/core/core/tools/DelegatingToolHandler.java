package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.ClineToolSpec;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.hhoa.kline.core.core.tools.types.ToolExecuteResult;
import com.hhoa.kline.core.core.tools.types.UIHelpers;

/** 将动态工具名（如 {@code use_subagent_*}）委托给同一实现（如 {@code SubagentToolHandler}）。 */
public final class DelegatingToolHandler implements ToolHandler {

    private final String name;
    private final ToolHandler delegate;
    private final ClineToolSpec specOverride;

    public DelegatingToolHandler(String name, ToolHandler delegate) {
        this(name, delegate, null);
    }

    public DelegatingToolHandler(String name, ToolHandler delegate, ClineToolSpec specOverride) {
        this.name = name;
        this.delegate = delegate;
        this.specOverride = specOverride;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription(ToolUse block) {
        return delegate.getDescription(block);
    }

    @Override
    public void handlePartialBlock(ToolUse block, UIHelpers uiHelpers) {
        delegate.handlePartialBlock(block, uiHelpers);
    }

    @Override
    public ToolExecuteResult execute(ToolContext context, ToolUse block) {
        return delegate.execute(context, block);
    }

    @Override
    public ClineToolSpec getClineToolSpec() {
        return specOverride != null ? specOverride : delegate.getClineToolSpec();
    }
}
