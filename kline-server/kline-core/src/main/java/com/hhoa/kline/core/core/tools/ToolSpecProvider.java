package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Provides a tool spec seed and binds it to a typed handler/input pair. */
public interface ToolSpecProvider<I, H extends ToolHandler> {
    String id();

    String description(ModelFamily family);

    default String name() {
        return id();
    }

    default String instruction(ModelFamily family) {
        return null;
    }

    default boolean enabled(ModelFamily family) {
        return true;
    }

    default Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return null;
    }

    default Set<String> excludedParameters(ModelFamily family) {
        return Set.of();
    }

    default void customizeInputSchema(ModelFamily family, Map<String, Object> inputSchema) {}
}
