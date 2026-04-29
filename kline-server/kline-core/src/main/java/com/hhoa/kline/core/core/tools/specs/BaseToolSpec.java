package com.hhoa.kline.core.core.tools.specs;

import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.ToolSchema;
import java.util.Map;
import java.util.function.Function;

/** Base class for schema customization helpers shared by tool spec providers. */
public abstract class BaseToolSpec {
    protected static void describe(Map<String, Object> schema, String name, String description) {
        ToolSchema.description(schema, name, description);
    }

    protected static void usage(Map<String, Object> schema, String name, String usage) {
        ToolSchema.usage(schema, name, usage);
    }

    protected static void require(Map<String, Object> schema, String name) {
        ToolSchema.require(schema, name);
    }

    protected static void optional(Map<String, Object> schema, String name) {
        ToolSchema.optional(schema, name);
    }

    protected static void instructionFn(
            Map<String, Object> schema,
            String name,
            Function<SystemPromptContext, String> instructionFn) {
        ToolSchema.instructionFn(schema, name, instructionFn);
    }

    protected static void contextRequirements(
            Map<String, Object> schema,
            String name,
            Function<SystemPromptContext, Boolean> contextRequirements) {
        ToolSchema.contextRequirements(schema, name, contextRequirements);
    }
}
