package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.List;

public interface ToolRegistry {
    ToolHandler<?> getToolHandler(String toolName);

    ToolSpec getToolSpec(String toolName, ModelFamily family);

    boolean has(String toolName);

    List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context, Boolean enabled);

    List<ToolSpec> getToolSpecs(
        ModelFamily variant, SystemPromptContext context, List<String> names, Boolean enabled);

    <T> DefaultToolRegistry register(ToolSpecProvider<T> specProvider);
}
