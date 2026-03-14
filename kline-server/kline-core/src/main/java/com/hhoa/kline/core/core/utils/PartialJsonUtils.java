package com.hhoa.kline.core.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hhoa.ai.kline.commons.utils.JsonUtils;
import java.util.ArrayList;
import java.util.List;

public class PartialJsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String getJsonPartialContent(String text, String newText, String oldText) {
        String partialContent = null;
        try {
            JsonNode newNode = objectMapper.readTree(newText);
            JsonNode oldNode = objectMapper.readTree(oldText);

            ObjectNode deltaNode = objectMapper.createObjectNode();
            var fieldNames = newNode.fieldNames();
            while (fieldNames.hasNext()) {
                String field = fieldNames.next();
                JsonNode newVal = newNode.get(field);
                JsonNode oldVal = oldNode != null ? oldNode.get(field) : null;

                if (newVal == null || newVal.isNull()) {
                    continue;
                }

                if (newVal.isTextual()) {
                    String newStr = newVal.asText("");
                    String oldStr = (oldVal != null && oldVal.isTextual()) ? oldVal.asText("") : "";
                    String deltaStr;
                    if (newStr.startsWith(oldStr)) {
                        deltaStr = newStr.substring(oldStr.length());
                    } else {
                        deltaStr = newStr;
                    }
                    if (!deltaStr.isEmpty()) {
                        deltaNode.put(field, deltaStr);
                    }
                } else if (newVal.isArray()) {
                    int newLen = newVal.size();
                    int oldLen = (oldVal != null && oldVal.isArray()) ? oldVal.size() : 0;
                    int maxLen = Math.max(newLen, oldLen);

                    var deltaArr = objectMapper.createArrayNode();
                    boolean hasChanges = false;

                    for (int i = 0; i < maxLen; i++) {
                        if (i < newLen) {
                            JsonNode newItem = newVal.get(i);
                            JsonNode oldItem =
                                    (i < oldLen && oldVal != null) ? oldVal.get(i) : null;

                            if (oldItem == null) {
                                deltaArr.add(newItem);
                                hasChanges = true;
                            } else if (newItem.isTextual() && oldItem.isTextual()) {
                                String newStr = newItem.asText("");
                                String oldStr = oldItem.asText("");
                                if (newStr.startsWith(oldStr)) {
                                    String deltaStr = newStr.substring(oldStr.length());
                                    if (!deltaStr.isEmpty()) {
                                        deltaArr.add(deltaStr);
                                        hasChanges = true;
                                    } else {
                                        deltaArr.addNull();
                                    }
                                } else {
                                    deltaArr.add(newItem);
                                    hasChanges = true;
                                }
                            } else if (!newItem.equals(oldItem)) {
                                deltaArr.add(newItem);
                                hasChanges = true;
                            } else {
                                deltaArr.addNull();
                            }
                        }
                    }

                    if (hasChanges) {
                        deltaNode.set(field, deltaArr);
                    }
                } else {
                    if (oldVal == null || !oldVal.equals(newVal)) {
                        deltaNode.set(field, newVal);
                    }
                }
            }

            if (!deltaNode.isEmpty()) {
                partialContent = objectMapper.writeValueAsString(deltaNode);
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON diff for partial message failed", e);
        }
        return partialContent;
    }

    public static List<String> parseArrayString(String s) {
        if (s == null || s.isEmpty()) {
            return new ArrayList<>();
        }

        String trimmed = s.trim();

        // 尝试使用 JSON 解析器解析完整的 JSON 数组
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            List<String> result =
                    JsonUtils.readValue(trimmed, new TypeReference<List<String>>() {});
            return result != null ? result : new ArrayList<>();
        }

        // 手动解析不完整的 JSON 数组（流式传输时）
        // 逐字符扫描，提取所有完整的字符串元素，保留未完成的元素
        List<String> result = new ArrayList<>();

        if (!trimmed.startsWith("[")) {
            return result;
        }

        int i = 1; // 跳过开头的 [
        boolean inString = false;
        boolean escaped = false;
        StringBuilder currentElement = new StringBuilder();

        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);

            if (escaped) {
                currentElement.append(c);
                escaped = false;
            } else if (c == '\\') {
                currentElement.append(c);
                escaped = true;
            } else if (c == '"') {
                if (inString) {
                    // 字符串结束：这是一个完整的元素
                    result.add(currentElement.toString());
                    currentElement = new StringBuilder();
                    inString = false;
                } else {
                    // 字符串开始
                    inString = true;
                }
            } else if (inString) {
                currentElement.append(c);
            }

            i++;
        }

        // 如果还有未完成的元素（正在流式传输中），也添加进去
        if (inString && currentElement.length() > 0) {
            result.add(currentElement.toString());
        }

        return result;
    }
}
