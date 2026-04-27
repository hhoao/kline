package com.hhoa.kline.core.core.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ToolCallValidatorTest {
    @Test
    void failsWhenRequiredParameterIsMissing() {
        ToolSpec spec =
                ToolSpec.builder()
                        .name("read_file")
                        .parameters(
                                List.of(
                                        ToolParameterSpec.builder()
                                                .name("path")
                                                .required(true)
                                                .build()))
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
                        .parameters(
                                List.of(
                                        param("requires_approval", "boolean", true),
                                        param("timeout", "integer", false),
                                        param("ratio", "number", false)))
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
                        .parameters(List.of(param("recursive", "boolean", false)))
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
                        .parameters(List.of(param("path", "string", true)))
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
                        .parameters(
                                List.of(
                                        ToolParameterSpec.builder()
                                                .name("timeout")
                                                .required(true)
                                                .type("integer")
                                                .contextRequirements(
                                                        context ->
                                                                Boolean.TRUE.equals(
                                                                        context
                                                                                .getYoloModeToggled()))
                                                .build()))
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
                        .parameters(List.of(param("action", "string", true)))
                        .build();

        ToolCallValidator.ValidationResult result =
                ToolCallValidator.validate(
                        spec, toolUse("browser_action", Map.of("action", "launch")), context());

        assertFalse(result.isValid());
        assertEquals(ToolCallValidator.ErrorCode.TOOL_DISABLED, result.errorCode());
    }

    private static ToolParameterSpec param(String name, String type, boolean required) {
        return ToolParameterSpec.builder()
                .name(name)
                .type(type)
                .required(required)
                .instruction(name)
                .build();
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
