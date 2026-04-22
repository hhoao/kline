package com.hhoa.kline.core.core.context.management;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 上下文错误处理工具类，用于检测和处理上下文窗口超出错误。 */
public class ContextErrorHandling {

    private static final Pattern[] CONTEXT_ERROR_PATTERNS = {
        Pattern.compile("\\bcontext\\s*(?:length|window)\\b.*exceed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmaximum\\s*context\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:input\\s*)?tokens?\\s*exceed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btoo\\s*many\\s*tokens?\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("input is too long", Pattern.CASE_INSENSITIVE),
        Pattern.compile(
                "requested input length.*exceeds.*maximum input length", Pattern.CASE_INSENSITIVE),
        Pattern.compile(
                "prompt is too long.*tokens?\\s*>\\s*\\d+\\s*maximum", Pattern.CASE_INSENSITIVE),
    };

    private static final Pattern[] BEDROCK_CONTEXT_PATTERNS = {
        Pattern.compile("maximum tokens.*exceeds.*model limit", Pattern.CASE_INSENSITIVE),
        Pattern.compile(
                "input length and max_tokens exceed context limit", Pattern.CASE_INSENSITIVE),
        Pattern.compile("context length.*exceeds", Pattern.CASE_INSENSITIVE),
        Pattern.compile("total number of tokens.*exceeds.*limit", Pattern.CASE_INSENSITIVE),
        Pattern.compile("requested.*tokens.*exceeds.*limit", Pattern.CASE_INSENSITIVE),
        Pattern.compile("reduce.*length.*messages.*completion", Pattern.CASE_INSENSITIVE),
        Pattern.compile("input is too long", Pattern.CASE_INSENSITIVE),
    };

    public static boolean checkContextWindowExceededError(Throwable error) {
        if (error == null) {
            return false;
        }

        return checkIsOpenAIContextWindowError(error)
                || checkIsOpenRouterContextWindowError(error)
                || checkIsAnthropicContextWindowError(error)
                || checkIsCerebrasContextWindowError(error)
                || checkIsBedrockContextWindowError(error)
                || checkIsVercelContextWindowError(error);
    }

    private static boolean checkIsOpenRouterContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }

            String status = extractStatusCode(message);
            if (!"400".equals(status)) {
                return false;
            }

            return matchesAny(message, CONTEXT_ERROR_PATTERNS);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkIsOpenAIContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }

            String lowerMessage = message.toLowerCase();
            return (lowerMessage.contains("token") || lowerMessage.contains("context length"))
                    && (lowerMessage.contains("400")
                            || lowerMessage.contains("context_length_exceeded"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkIsAnthropicContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }
            return message.contains("invalid_request_error")
                    && (message.toLowerCase().contains("token")
                            || message.toLowerCase().contains("context"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkIsCerebrasContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }

            String status = extractStatusCode(message);
            return "400".equals(status)
                    && message.contains("Please reduce the length of the messages or completion");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkIsBedrockContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }

            String lower = message.toLowerCase();
            boolean isValidationException =
                    lower.contains("validationexception")
                            || lower.contains("ai_apicallerror")
                            || "400".equals(extractStatusCode(message))
                            || lower.contains("stream_initialization_failed");
            return isValidationException && matchesAny(message, BEDROCK_CONTEXT_PATTERNS);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkIsVercelContextWindowError(Throwable error) {
        try {
            String message = safeMessage(error);
            if (message == null) {
                return false;
            }

            if (message.contains("context_length_exceeded")) {
                return true;
            }

            List<String> messages = new ArrayList<>();
            messages.add(message);
            String status = extractStatusCode(message);
            boolean hasValidStatus = "400".equals(status);
            boolean has400InMessage =
                    message.contains("\"code\":400") || message.contains("\"code\": 400");
            if (!hasValidStatus && !has400InMessage) {
                return false;
            }

            for (String candidate : messages) {
                if (matchesAny(candidate, CONTEXT_ERROR_PATTERNS)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean matchesAny(String message, Pattern[] patterns) {
        for (Pattern pattern : patterns) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return false;
    }

    private static String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message != null) {
            return message;
        }
        return error.toString();
    }

    private static String extractStatusCode(String message) {
        if (message == null) {
            return null;
        }

        Pattern codePattern = Pattern.compile("\\\"code\\\":\\s*(\\d+)");
        Matcher matcher = codePattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        Pattern statusPattern = Pattern.compile("\\\"status\\\":\\s*(\\d+)");
        matcher = statusPattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        if (message.contains("400")) {
            return "400";
        }

        return null;
    }
}
