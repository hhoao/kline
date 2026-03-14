package com.hhoa.kline.core.core.context.management;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 上下文错误处理工具类 用于检测和处理上下文窗口超出错误 */
public class ContextErrorHandling {

    private static final Pattern[] CONTEXT_ERROR_PATTERNS = {
        Pattern.compile("\\bcontext\\s*(?:length|window)\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bmaximum\\s*context\\b", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\b(?:input\\s*)?tokens?\\s*exceed", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\btoo\\s*many\\s*tokens?\\b", Pattern.CASE_INSENSITIVE)
    };

    /**
     * 检查错误是否为上下文窗口超出错误
     *
     * @param error 错误对象
     * @return 如果是上下文窗口错误则返回 true
     */
    public static boolean checkContextWindowExceededError(Throwable error) {
        if (error == null) {
            return false;
        }

        return checkIsOpenAIContextWindowError(error)
                || checkIsOpenRouterContextWindowError(error)
                || checkIsAnthropicContextWindowError(error)
                || checkIsCerebrasContextWindowError(error);
    }

    /**
     * 检查是否为 OpenRouter 上下文窗口错误
     *
     * @param error 错误对象
     * @return 如果是 OpenRouter 上下文窗口错误则返回 true
     */
    private static boolean checkIsOpenRouterContextWindowError(Throwable error) {
        try {
            String message = error.getMessage();
            if (message == null) {
                return false;
            }

            String status = extractStatusCode(message);

            // 已知的 OpenAI/OpenRouter 风格信号（代码 400 且消息包含 "context length"）
            if ("400".equals(status)) {
                for (Pattern pattern : CONTEXT_ERROR_PATTERNS) {
                    if (pattern.matcher(message).find()) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为 OpenAI 上下文窗口错误 文档：https://platform.openai.com/docs/guides/error-codes/api-errors
     *
     * @param error 错误对象
     * @return 如果是 OpenAI 上下文窗口错误则返回 true
     */
    private static boolean checkIsOpenAIContextWindowError(Throwable error) {
        try {
            String message = error.getMessage();
            if (message == null) {
                return false;
            }

            String lowerMessage = message.toLowerCase();
            return (lowerMessage.contains("token") || lowerMessage.contains("context length"))
                    && lowerMessage.contains("400");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为 Anthropic 上下文窗口错误
     *
     * @param error 错误对象
     * @return 如果是 Anthropic 上下文窗口错误则返回 true
     */
    private static boolean checkIsAnthropicContextWindowError(Throwable error) {
        try {
            String message = error.getMessage();
            if (message == null) {
                return false;
            }

            // Anthropic 错误通常包含 "invalid_request_error"
            return message.contains("invalid_request_error");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为 Cerebras 上下文窗口错误
     *
     * @param error 错误对象
     * @return 如果是 Cerebras 上下文窗口错误则返回 true
     */
    private static boolean checkIsCerebrasContextWindowError(Throwable error) {
        try {
            String message = error.getMessage();
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

    /**
     * 从错误消息中提取状态码
     *
     * @param message 错误消息
     * @return 状态码字符串，如果未找到则返回 null
     */
    private static String extractStatusCode(String message) {
        if (message == null) {
            return null;
        }

        Pattern codePattern = Pattern.compile("\"code\":\\s*(\\d+)");
        Matcher matcher = codePattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }

        if (message.contains("400")) {
            return "400";
        }

        return null;
    }
}
