package com.hhoa.kline.core.core.tools;

import com.hhoa.kline.core.core.assistant.ToolUse;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import com.hhoa.kline.core.core.tools.types.ToolContext;
import com.networknt.schema.Error;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates and normalizes tool call inputs before handler execution. */
public final class ToolCallValidator {
    private ToolCallValidator() {}

    public static ValidationResult validate(ToolSpec spec, ToolUse toolUse, ToolContext context) {
        if (spec == null) {
            return ValidationResult.valid();
        }
        Map<String, Object> inputSchema = spec.getInputSchema();
        if (inputSchema == null) {
            return ValidationResult.valid();
        }
        SystemPromptContext promptContext = resolvePromptContext(context);
        if (!isContextEnabled(spec.getEnabled(), promptContext)) {
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

        String key = JsonSchemaValidationSupport.cacheKey(toolUse.getName(), inputSchema);
        for (Error message :
                JsonSchemaValidationSupport.validate(key, inputSchema, params)) {
            ValidationResult result = mapValidationFailure(message);
            if (!result.isValid()) {
                return result;
            }
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

    private static final Pattern UNKNOWN_PARAM_PATTERN = Pattern.compile("'([^']+)'");
    private static final Pattern REQUIRED_PARAM_PATTERN = Pattern.compile("'([^']+)'");

    private static ValidationResult mapValidationFailure(Error message) {
        String messageKey = message.getKeyword();
        String details = message.getMessage();
        String paramName = extractParamName(message);

        if ("additionalProperties".equals(messageKey)) {
            return ValidationResult.invalid(
                    ErrorCode.UNKNOWN_PARAMETER,
                    paramName,
                    "Tool does not accept parameter '%s'.".formatted(paramName));
        }
        if ("required".equals(messageKey)) {
            return ValidationResult.invalid(
                    ErrorCode.MISSING_REQUIRED_PARAMETER,
                    paramName,
                    "Missing required parameter '%s'.".formatted(paramName));
        }
        if ("type".equals(messageKey)) {
            return ValidationResult.invalid(
                    ErrorCode.INVALID_PARAMETER_TYPE,
                    paramName,
                    "Parameter '%s' has invalid type.".formatted(paramName));
        }
        return ValidationResult.invalid(
                ErrorCode.INVALID_PARAMETER_TYPE,
                paramName,
                details != null && !details.isBlank() ? details : "Invalid tool parameters.");
    }

    private static String extractParamName(Error message) {
        String messageKey = message.getKeyword();
        String details = message.getMessage();
        if (details == null) {
            return null;
        }
        Pattern pattern =
                "required".equals(messageKey) ? REQUIRED_PARAM_PATTERN : UNKNOWN_PARAM_PATTERN;
        Matcher matcher = pattern.matcher(details);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String instanceLocation =
                message.getInstanceLocation() != null
                        ? message.getInstanceLocation().toString()
                        : null;
        if (instanceLocation != null && instanceLocation.startsWith("/")) {
            return instanceLocation.substring(1);
        }
        return message.getProperty();
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
