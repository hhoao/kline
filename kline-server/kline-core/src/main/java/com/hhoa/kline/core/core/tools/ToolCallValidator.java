package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validates and normalizes tool call inputs before handler execution. */
public final class ToolCallValidator {
    private ToolCallValidator() {}

    public static ValidationResult validate(ToolSpec spec, ToolUse toolUse, ToolContext context) {
        if (spec == null) {
            return ValidationResult.valid();
        }
        SystemPromptContext promptContext = resolvePromptContext(context);
        if (!isContextEnabled(spec.getContextRequirements(), promptContext)) {
            return ValidationResult.invalid(
                    ErrorCode.TOOL_DISABLED,
                    null,
                    "Tool '%s' is not available in the current context."
                            .formatted(toolUse.getName()));
        }

        Map<String, Object> params = toolUse.getParams();
        if (params == null) {
            params = new LinkedHashMap<>();
            toolUse.setParams(params);
        }

        Map<String, ToolParameterSpec> availableParams = availableParameters(spec, promptContext);
        for (String paramName : params.keySet()) {
            if (!availableParams.containsKey(paramName)) {
                return ValidationResult.invalid(
                        ErrorCode.UNKNOWN_PARAMETER,
                        paramName,
                        "Tool '%s' does not accept parameter '%s'."
                                .formatted(toolUse.getName(), paramName));
            }
        }

        for (ToolParameterSpec parameter : availableParams.values()) {
            String paramName = parameter.getName();
            Object value = params.get(paramName);
            if (isMissing(value)) {
                if (parameter.isRequired()) {
                    return ValidationResult.invalid(
                            ErrorCode.MISSING_REQUIRED_PARAMETER,
                            paramName,
                            "Missing required parameter '%s'.".formatted(paramName));
                }
                continue;
            }

            Object normalized = normalizeValue(parameter, value);
            if (normalized == INVALID_VALUE) {
                return ValidationResult.invalid(
                        ErrorCode.INVALID_PARAMETER_TYPE,
                        paramName,
                        "Parameter '%s' must be %s.".formatted(paramName, parameter.getType()));
            }
            params.put(paramName, normalized);
        }

        return ValidationResult.valid();
    }

    private static SystemPromptContext resolvePromptContext(ToolContext context) {
        if (context != null && context.getSystemPromptContext() != null) {
            return context.getSystemPromptContext();
        }
        SystemPromptContext promptContext = new SystemPromptContext();
        if (context != null) {
            promptContext.setCwd(context.getCwd());
            promptContext.setYoloModeToggled(context.isYoloModeToggled());
            promptContext.setEnableParallelToolCalling(context.isEnableParallelToolCalling());
        }
        return promptContext;
    }

    private static Map<String, ToolParameterSpec> availableParameters(
            ToolSpec spec, SystemPromptContext context) {
        Map<String, ToolParameterSpec> available = new LinkedHashMap<>();
        List<ToolParameterSpec> parameters = spec.getParameters();
        if (parameters == null) {
            return available;
        }
        for (ToolParameterSpec parameter : parameters) {
            if (parameter.getName() == null || parameter.getName().isBlank()) {
                continue;
            }
            if (!isContextEnabled(parameter.getContextRequirements(), context)) {
                continue;
            }
            available.put(parameter.getName(), parameter);
        }
        return available;
    }

    private static boolean isContextEnabled(
            java.util.function.Function<SystemPromptContext, Boolean> requirement,
            SystemPromptContext context) {
        if (requirement == null) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(requirement.apply(context));
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isMissing(Object value) {
        return value == null || (value instanceof String text && text.trim().isEmpty());
    }

    private static final Object INVALID_VALUE = new Object();

    private static Object normalizeValue(ToolParameterSpec parameter, Object value) {
        String type =
                parameter.getType() == null || parameter.getType().isBlank()
                        ? "string"
                        : parameter.getType();
        return switch (type) {
            case "boolean" -> normalizeBoolean(value);
            case "integer" -> normalizeInteger(value);
            case "number" -> normalizeNumber(value);
            case "array" -> value instanceof List<?> ? value : INVALID_VALUE;
            case "object" -> value instanceof Map<?, ?> ? value : INVALID_VALUE;
            case "string" -> value instanceof String ? value : INVALID_VALUE;
            default -> value;
        };
    }

    private static Object normalizeBoolean(Object value) {
        if (value instanceof Boolean) {
            return value;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase();
            if ("true".equals(normalized)) {
                return Boolean.TRUE;
            }
            if ("false".equals(normalized)) {
                return Boolean.FALSE;
            }
        }
        return INVALID_VALUE;
    }

    private static Object normalizeInteger(Object value) {
        if (value instanceof Integer) {
            return value;
        }
        if (value instanceof Number number
                && Math.rint(number.doubleValue()) == number.doubleValue()) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return INVALID_VALUE;
            }
        }
        return INVALID_VALUE;
    }

    private static Object normalizeNumber(Object value) {
        if (value instanceof Number) {
            return value;
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return INVALID_VALUE;
            }
        }
        return INVALID_VALUE;
    }

    public enum ErrorCode {
        MISSING_REQUIRED_PARAMETER,
        INVALID_PARAMETER_TYPE,
        UNKNOWN_PARAMETER,
        TOOL_DISABLED
    }

    public record ValidationResult(
            boolean isValid, ErrorCode errorCode, String paramName, String message) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null, null, null);
        }

        public static ValidationResult invalid(
                ErrorCode errorCode, String paramName, String message) {
            return new ValidationResult(false, errorCode, paramName, message);
        }
    }
}
