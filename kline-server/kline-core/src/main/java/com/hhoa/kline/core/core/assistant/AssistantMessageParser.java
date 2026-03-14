package com.hhoa.kline.core.core.assistant;

import com.hhoa.kline.core.enums.ClineDefaultTool;
import com.hhoa.kline.core.enums.ToolParamName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 助手消息解析器
 *
 * @author hhoa
 */
@Slf4j
public class AssistantMessageParser {

    private static final Set<String> TOOL_USE_NAMES =
            Arrays.stream(ClineDefaultTool.values())
                    .map(ClineDefaultTool::getValue)
                    .collect(Collectors.toSet());

    // 内部标签名称集合，这些标签会被移除但保留其内容
    private static final Set<String> INTERNAL_TAG_NAMES = Set.of("thinking");

    private static final Map<String, ToolParamName> TOOL_PARAM_NAMES = new HashMap<>();

    static {
        for (ToolParamName paramName : ToolParamName.values()) {
            TOOL_PARAM_NAMES.put(paramName.getValue(), paramName);
        }
    }

    /**
     * 解析助手消息
     *
     * @param assistantMessage 助手消息字符串
     * @return 解析后的消息内容列表
     */
    public List<AssistantMessageContent> parseAssistantMessage(String assistantMessage) {
        return parseAssistantMessage(assistantMessage, false);
    }

    /**
     * 移除内部标签但保留其内容
     *
     * @param message 原始消息
     * @return 移除内部标签后的消息
     */
    private String removeInternalTags(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        // 快速检查：如果没有 '<'，直接返回原字符串
        if (message.indexOf('<') == -1) {
            return message;
        }

        StringBuilder result = new StringBuilder(message.length());
        int len = message.length();
        int i = 0;

        while (i < len) {
            char ch = message.charAt(i);

            if (ch == '<') {
                boolean isInternalTag = false;

                for (String tagName : INTERNAL_TAG_NAMES) {
                    int tagLen = tagName.length();

                    if (i + tagLen + 2 <= len
                            && message.charAt(i + tagLen + 1) == '>'
                            && message.regionMatches(i + 1, tagName, 0, tagLen)) {

                        String closeTag = "</" + tagName + ">";
                        int closeTagLen = closeTag.length();

                        int closeStart = message.indexOf(closeTag, i + tagLen + 2);

                        if (closeStart != -1) {
                            String content = message.substring(i + tagLen + 2, closeStart);
                            result.append(content);
                            i = closeStart + closeTagLen;
                            isInternalTag = true;
                            break;
                        } else {
                            // 没有完整的闭合标签，移除开始标签但保留内容
                            int contentStart = i + tagLen + 2;
                            String remainingContent = message.substring(contentStart);

                            int lastCloseAttempt = remainingContent.lastIndexOf("</");
                            if (lastCloseAttempt != -1) {
                                String afterClose =
                                        remainingContent.substring(lastCloseAttempt + 2);
                                if (tagName.startsWith(afterClose)) {
                                    result.append(remainingContent, 0, lastCloseAttempt);
                                    i = len;
                                    isInternalTag = true;
                                    break;
                                }
                            }

                            result.append(remainingContent);
                            i = len;
                            isInternalTag = true;
                            break;
                        }
                    }

                    // 检查不完整的标签前缀（如 "<th" 或 "<thinking"）
                    if (!isInternalTag) {
                        int remainingLen = len - i - 1;
                        if (remainingLen > 0
                                && remainingLen <= tagLen
                                && tagName.regionMatches(0, message, i + 1, remainingLen)) {
                            i = len;
                            isInternalTag = true;
                            break;
                        }
                    }
                }

                if (!isInternalTag) {
                    result.append(ch);
                    i++;
                }
            } else {
                result.append(ch);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * 解析助手消息
     *
     * @param assistantMessage 助手消息字符串
     * @param isPartial 是否为部分消息，如果为true，则会忽略不完整的工具标签
     * @return 解析后的消息内容列表
     */
    public List<AssistantMessageContent> parseAssistantMessage(
            String assistantMessage, boolean isPartial) {
        if (assistantMessage == null || assistantMessage.isEmpty()) {
            return Collections.emptyList();
        }

        // 移除内部标签
        assistantMessage = removeInternalTags(assistantMessage);

        List<AssistantMessageContent> contentBlocks = new ArrayList<>();
        int currentTextContentStart = 0;
        TextContent currentTextContent = null;
        int currentToolUseStart = 0;
        ToolUse currentToolUse = null;
        int currentParamValueStart = 0;
        String currentParamName = null;

        int len = assistantMessage.length();
        for (int i = 0; i < len; i++) {
            if (currentToolUse != null && currentParamName != null) {
                if (assistantMessage.charAt(i) == '>') {
                    String closeTag = "</" + currentParamName + ">";
                    int expectedStart = i - closeTag.length() + 1;
                    if (expectedStart >= 0
                            && assistantMessage.regionMatches(
                                    expectedStart, closeTag, 0, closeTag.length())) {

                        String value =
                                assistantMessage
                                        .substring(currentParamValueStart, expectedStart)
                                        .trim();
                        currentToolUse.getParams().put(currentParamName, value);
                        currentParamName = null;
                    } else {
                        continue;
                    }
                } else {
                    continue;
                }
            }

            if (currentToolUse != null && currentParamName == null) {
                if (assistantMessage.charAt(i) == '>') {
                    boolean startedNewParam = false;
                    for (String paramName : TOOL_PARAM_NAMES.keySet()) {
                        String tag = "<" + paramName + ">";
                        int expectedStart = i - tag.length() + 1;
                        if (expectedStart >= 0
                                && assistantMessage.regionMatches(
                                        expectedStart, tag, 0, tag.length())) {
                            currentParamName = paramName;
                            currentParamValueStart = i + 1;
                            startedNewParam = true;
                            break;
                        }
                    }
                    if (startedNewParam) {
                        continue;
                    }

                    String toolCloseTag = "</" + currentToolUse.getName() + ">";
                    int expectedStart = i - toolCloseTag.length() + 1;
                    if (expectedStart >= 0
                            && assistantMessage.regionMatches(
                                    expectedStart, toolCloseTag, 0, toolCloseTag.length())) {

                        // 特殊处理 write_to_file 的 content 参数（因为该工具的参数格式特殊）
                        int sliceEnd = expectedStart;
                        String contentParamName = "content";
                        if ("write_to_file".equals(currentToolUse.getName())) {
                            String contentStartTag = "<" + contentParamName + ">";
                            String contentEndTag = "</" + contentParamName + ">";
                            int contentStart =
                                    assistantMessage.indexOf(contentStartTag, currentToolUseStart);

                            if (contentStart != -1 && contentStart < sliceEnd) {
                                int contentEnd =
                                        assistantMessage.lastIndexOf(contentEndTag, sliceEnd);
                                if (contentEnd != -1 && contentEnd > contentStart) {
                                    String contentValue =
                                            assistantMessage
                                                    .substring(
                                                            contentStart + contentStartTag.length(),
                                                            contentEnd)
                                                    .trim();
                                    currentToolUse.getParams().put(contentParamName, contentValue);
                                }
                            }
                        }

                        currentToolUse.setPartial(false);
                        contentBlocks.add(currentToolUse);
                        currentToolUse = null;
                        currentTextContentStart = i + 1;
                        continue;
                    }
                }
                continue;
            }

            if (currentToolUse == null) {
                if (assistantMessage.charAt(i) == '>') {
                    boolean startedNewTool = false;
                    for (String toolName : TOOL_USE_NAMES) {
                        String tag = "<" + toolName + ">";
                        int expectedStart = i - tag.length() + 1;
                        if (expectedStart >= 0
                                && assistantMessage.regionMatches(
                                        expectedStart, tag, 0, tag.length())) {

                            if (currentTextContent != null) {
                                currentTextContent.setContent(
                                        assistantMessage
                                                .substring(currentTextContentStart, expectedStart)
                                                .trim());
                                currentTextContent.setPartial(false);
                                if (!currentTextContent.getContent().isEmpty()) {
                                    contentBlocks.add(currentTextContent);
                                }
                                currentTextContent = null;
                            } else {
                                String potentialText =
                                        assistantMessage
                                                .substring(currentTextContentStart, expectedStart)
                                                .trim();
                                if (!potentialText.isEmpty()) {
                                    contentBlocks.add(new TextContent(potentialText, false));
                                }
                            }

                            currentToolUse = new ToolUse();
                            currentToolUse.setName(toolName);
                            currentToolUse.setParams(new HashMap<>());
                            currentToolUse.setPartial(true);
                            currentToolUseStart = i + 1;
                            startedNewTool = true;
                            break;
                        }
                    }

                    if (startedNewTool) {
                        continue;
                    }
                }

                if (currentTextContent == null) {
                    currentTextContentStart = i;
                    currentTextContent = new TextContent();
                    currentTextContent.setPartial(true);
                }
            }
        }

        if (isPartial) {
            // 对于部分消息，未闭合的工具使用应该被添加（标记为partial=true）
            if (currentToolUse != null) {
                if (currentParamName != null) {
                    String paramValue = assistantMessage.substring(currentParamValueStart).trim();
                    int lastOpen = paramValue.lastIndexOf('<');
                    if (lastOpen != -1) {
                        String trailing = paramValue.substring(lastOpen + 1);
                        String closeTagBody = "/" + currentParamName;
                        if (closeTagBody.startsWith(trailing)) {
                            paramValue = paramValue.substring(0, lastOpen).trim();
                        }
                    }
                    currentToolUse.getParams().put(currentParamName, paramValue);
                }
                contentBlocks.add(currentToolUse);
                return contentBlocks;
            }

            // 检查文本内容中是否有不完整的工具标签开始（例如 "<rea" 或 "<list_files"）
            if (currentTextContent != null || currentTextContentStart < len) {
                String remainingText = assistantMessage.substring(currentTextContentStart);

                int lastOpenBracket = remainingText.lastIndexOf('<');
                if (lastOpenBracket != -1) {
                    String afterBracket = remainingText.substring(lastOpenBracket + 1);
                    boolean isIncompleteTag = false;

                    // 检查是否是不完整的工具标签（部分工具名或完整工具名但缺少闭合>）
                    for (String toolName : TOOL_USE_NAMES) {
                        if (toolName.startsWith(afterBracket)) {
                            isIncompleteTag = true;
                            break;
                        }
                    }

                    if (!isIncompleteTag) {
                        // 检查是否是不完整的参数标签
                        for (String paramName : TOOL_PARAM_NAMES.keySet()) {
                            if (paramName.startsWith(afterBracket)) {
                                isIncompleteTag = true;
                                break;
                            }
                        }
                    }

                    if (isIncompleteTag) {
                        remainingText = remainingText.substring(0, lastOpenBracket).trim();

                        if (remainingText.isEmpty()) {
                            return contentBlocks;
                        }

                        contentBlocks.add(new TextContent(remainingText, true));
                        return contentBlocks;
                    }
                }

                remainingText = remainingText.trim();
                if (!remainingText.isEmpty()) {
                    contentBlocks.add(new TextContent(remainingText, true));
                }
                return contentBlocks;
            }

            return contentBlocks;
        }

        if (currentToolUse != null && currentParamName != null) {
            currentToolUse
                    .getParams()
                    .put(
                            currentParamName,
                            assistantMessage.substring(currentParamValueStart).trim());
        }

        if (currentToolUse != null) {
            contentBlocks.add(currentToolUse);
        } else if (currentTextContent != null) {
            currentTextContent.setContent(
                    assistantMessage.substring(currentTextContentStart).trim());
            if (!currentTextContent.getContent().isEmpty()) {
                contentBlocks.add(currentTextContent);
            }
        }

        return contentBlocks;
    }
}
