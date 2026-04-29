package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolCallValidatorTest {
    @Test
    void failsWhenRequiredParameterIsMissing() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("read_file")
                        .inputSchema(schema(Map.of("path", property("string")), "path"))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(spec, toolUse("read_file", Map.of()), context());

        assertFalse(result.isValid());
        assertEquals(ToolCallValidator.ErrorCode.MISSING_REQUIRED_PARAMETER, result.errorCode());
        assertEquals("path", result.paramName());
    }

    @Test
    void normalizesBooleanIntegerAndNumberStringParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("requires_approval", "true");
        params.put("timeout", "30");
        params.put("ratio", "1.5");
        ToolUse toolUse = toolUse("execute_command", params);
        ToolSpec spec =
                ToolSpec.builder()
                        .name("execute_command")
                        .inputSchema(
                                schema(
                                        Map.of(
                                                "requires_approval",
                                                property("boolean"),
                                                "timeout",
                                                property("integer"),
                                                "ratio",
                                                property("number")),
                                        "requires_approval"))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(spec, toolUse, context());

        assertTrue(result.isValid());
        assertEquals(Boolean.TRUE, toolUse.getParams().get("requires_approval"));
        assertEquals(30, toolUse.getParams().get("timeout"));
        assertEquals(1.5d, toolUse.getParams().get("ratio"));
    }

    @Test
    void failsWhenParameterTypeDoesNotMatch() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("list_files")
                        .inputSchema(schema(Map.of("recursive", property("boolean"))))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(
                        spec, toolUse("list_files", Map.of("recursive", "maybe")), context());

        assertFalse(result.isValid());
        assertEquals(ToolCallValidator.ErrorCode.INVALID_PARAMETER_TYPE, result.errorCode());
        assertEquals("recursive", result.paramName());
    }

    @Test
    void failsWhenUnknownParameterIsProvided() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("read_file")
                        .inputSchema(schema(Map.of("path", property("string")), "path"))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(
                        spec,
                        toolUse("read_file", Map.of("path", "README.md", "extra", "noise")),
                        context());

        assertFalse(result.isValid());
        assertEquals(ToolCallValidator.ErrorCode.UNKNOWN_PARAMETER, result.errorCode());
        assertEquals("extra", result.paramName());
    }

    @Test
    void skipsParametersDisabledByContextRequirements() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("execute_command")
                        .inputSchema(
                                schema(
                                        Map.of(
                                                "timeout",
                                                property(
                                                        "integer",
                                                        context ->
                                                                Boolean.TRUE.equals(
                                                                        context
                                                                                .getYoloModeToggled()))),
                                        "timeout"))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(spec, toolUse("execute_command", Map.of()), context());

        assertTrue(result.isValid());
    }

    @Test
    void failsWhenToolIsDisabledByContextRequirements() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("browser_action")
                        .contextRequirements(
                                context -> Boolean.TRUE.equals(context.getSupportsBrowserUse()))
                        .inputSchema(schema(Map.of("action", property("string")), "action"))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(
                        spec, toolUse("browser_action", Map.of("action", "launch")), context());

        assertFalse(result.isValid());
        assertEquals(ToolCallValidator.ErrorCode.TOOL_DISABLED, result.errorCode());
    }

    private static Map<String, Object> schema(
            Map<String, Map<String, Object>> properties, String... required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", new LinkedHashMap<>(properties));
        schema.put("required", java.util.List.of(required));
        schema.put("additionalProperties", false);
        return schema;
    }

    private static Map<String, Object> property(String type) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", type);
        return property;
    }

    private static Map<String, Object> property(
            String type,
            java.util.function.Function<SystemPromptContext, Boolean> contextRequirements) {
        Map<String, Object> property = property(type);
        property.put(ToolSchema.X_CONTEXT_REQUIREMENTS, contextRequirements);
        return property;
    }

    private static ToolUse toolUse(String name, Map<String, Object> params) {
        ToolUse toolUse = new ToolUse();
        toolUse.setName(name);
        toolUse.setParams(new HashMap<>(params));
        return toolUse;
    }

    private static ToolContext context() {
        SystemPromptContext systemPromptContext = new SystemPromptContext();
        systemPromptContext.setYoloModeToggled(false);
        systemPromptContext.setSupportsBrowserUse(false);
        return ToolContext.builder().systemPromptContext(systemPromptContext).build();
    }
}
