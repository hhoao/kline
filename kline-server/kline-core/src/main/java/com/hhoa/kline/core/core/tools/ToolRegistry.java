package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ToolRegistry {
    ToolHandler getHandler(String toolName);

    ToolSpec getSpec(String toolName, ModelFamily family);

    List<ToolSpec> getToolSpecs(ModelFamily variant, SystemPromptContext context);

    default List<ToolSpec> getToolsForVariantWithFallback(
            ModelFamily variant, List<String> requestedIds) {
        return getToolsForVariantWithFallback(variant, requestedIds, null);
    }

    List<ToolSpec> getToolsForVariantWithFallback(
            ModelFamily variant, List<String> requestedIds, SystemPromptContext context);

    List<ToolSpec> getEnabledTools(ModelFamily variant, SystemPromptContext context);

    List<Map<String, Object>> getNativeTools(
            ModelFamily variant,
            SystemPromptContext context,
            Function<ToolSpecConverter.ToolConversionInput, Map<String, Object>> converter);

    boolean has(String toolName);
}
