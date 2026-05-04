package com.hhoa.kline.core.core.tools.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.prompts.systemprompt.ModelFamily;
import com.hhoa.kline.core.core.tools.ToolSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolPromptBuilderTest {

    @Test
    void rendersExplicitToolPromptInsteadOfDescription() {
        ToolSpec spec =
                ToolSpec.builder()
                        .variant(ModelFamily.GENERIC)
                        .name("sample_tool")
                        .name("sample_tool")
                        .description("Short schema description.")
                        .prompt("Use sample_tool for prompt-only guidance.")
                        .inputSchema(inputSchemaWithPath())
                        .build();

        String rendered = ToolPromptBuilder.render(spec, List.of("sample_tool"), null);

        assertTrue(rendered.contains("## sample_tool"));
        assertTrue(rendered.contains("Use sample_tool for prompt-only guidance."));
        assertFalse(rendered.contains("Parameters:"));
        assertFalse(rendered.contains("Usage:"));
        assertFalse(rendered.contains("- path: (required) Path to inspect."));
        assertFalse(rendered.contains("<sample_tool>"));
        assertFalse(rendered.contains("Short schema description."));
        assertFalse(rendered.contains("Description:"));
    }

    @Test
    void blankToolPromptDoesNotFallbackToDescription() {
        ToolSpec spec =
                ToolSpec.builder()
                        .variant(ModelFamily.GENERIC)
                        .name("sample_tool")
                        .name("sample_tool")
                        .description("Short schema description.")
                        .prompt(" ")
                        .inputSchema(emptyObjectSchema())
                        .build();

        String rendered = ToolPromptBuilder.render(spec, List.of("sample_tool"), null);

        assertTrue(rendered.isBlank());
    }

    private static Map<String, Object> inputSchemaWithPath() {
        Map<String, Object> path = new LinkedHashMap<>();
        path.put("type", "string");
        path.put("description", "Path to inspect.");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("path", path);

        Map<String, Object> schema = emptyObjectSchema();
        schema.put("properties", properties);
        schema.put("required", List.of("path"));
        return schema;
    }

    private static Map<String, Object> emptyObjectSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<String, Object>());
        schema.put("required", new ArrayList<String>());
        schema.put("additionalProperties", false);
        return schema;
    }
}
