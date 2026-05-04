package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.handlers.ToolHandler;
import java.util.function.Function;

/**
 * Binds tool metadata to a typed handler.
 *
 * @param <T> Tool argument type: must match both {@link ToolHandler}{@code <T>} and {@link
 *     #inputType(ModelFamily)} (JSON schema is generated from the same class).
 */
public interface ToolSpecProvider<T> {
    String name();

    String description(ModelFamily family);

    String prompt(ModelFamily family);

    default Function<SystemPromptContext, Boolean> contextRequirements(ModelFamily family) {
        return null;
    }

    Class<T> inputType(ModelFamily family);

    ToolHandler<T> handler(ModelFamily family);
}
