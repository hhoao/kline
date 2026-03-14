package com.hhoa.kline.core.core.prompts.systemprompt.templates;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 * 模板引擎
 *
 * @author hhoa
 */
@Slf4j
public class TemplateEngine {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public String resolve(
            String template, SystemPromptContext context, Map<String, String> placeholders) {
        return resolve((Object) template, context, placeholders);
    }

    /** 解析模板占位符（格式：{{PLACEHOLDER}}），使用提供的值替换 支持函数模板：template可以是字符串或函数 */
    public String resolve(
            Object template, SystemPromptContext context, Map<String, String> placeholders) {
        String templateStr;
        if (template instanceof Function) {
            @SuppressWarnings("unchecked")
            Function<SystemPromptContext, String> templateFn =
                    (Function<SystemPromptContext, String>) template;
            templateStr = templateFn.apply(context);
        } else if (template instanceof String) {
            templateStr = (String) template;
        } else {
            return "";
        }

        if (templateStr == null || templateStr.isEmpty()) {
            return "";
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(templateStr);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = getNestedValue(placeholders, key);

            String replacement;
            if (value != null) {
                if (value instanceof String) {
                    replacement = (String) value;
                } else {
                    try {
                        replacement = objectMapper.writeValueAsString(value);
                    } catch (Exception e) {
                        replacement = String.valueOf(value);
                    }
                }
            } else {
                replacement = matcher.group(0);
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public List<String> validate(String template, List<String> requiredPlaceholders) {
        List<String> missingPlaceholders = new ArrayList<>();

        if (template == null || requiredPlaceholders == null) {
            return missingPlaceholders;
        }

        for (String placeholder : requiredPlaceholders) {
            Pattern pattern =
                    Pattern.compile("\\{\\{\\s*" + Pattern.quote(placeholder) + "\\s*\\}\\}");
            if (!pattern.matcher(template).find()) {
                missingPlaceholders.add(placeholder);
            }
        }

        return missingPlaceholders;
    }

    public List<String> extractPlaceholders(String template) {
        List<String> placeholders = new ArrayList<>();

        if (template == null) {
            return placeholders;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            String placeholder = matcher.group(1).trim();
            if (!placeholders.contains(placeholder)) {
                placeholders.add(placeholder);
            }
        }

        return placeholders;
    }

    /** 获取嵌套值（使用点号表示法，例如 "user.name" -> obj.user.name） */
    private Object getNestedValue(Object obj, String path) {
        if (obj == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] keys = path.split("\\.");
        Object current = obj;

        for (String key : keys) {
            if (current == null || !(current instanceof Map)) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) current;
            current = map.get(key);
        }

        return current;
    }

    public String escape(String template) {
        if (template == null) {
            return "";
        }
        return template.replace("{{", "\\{\\{").replace("}}", "\\}\\}");
    }

    public String unescape(String template) {
        if (template == null) {
            return "";
        }
        return template.replace("\\{\\{", "{{").replace("\\}\\}", "}}");
    }
}
