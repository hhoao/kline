package com.hhoa.kline.core.core.assistant.parser;

import com.hhoa.kline.core.core.assistant.AssistantMessageContent;
import com.hhoa.kline.core.core.assistant.TextContent;
import com.hhoa.kline.core.core.assistant.ToolUse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 {@link StreamingTagParser} 的流式助手消息解析器。
 *
 * <p>实现两层架构中的 Layer 2：将底层标签事件翻译为 {@link ToolUse} / {@link TextContent} 语义。
 *
 * @author hhoa
 */
public class DefaultStreamingAssistantMessageParser
        implements StreamingAssistantMessageParser, TagEventHandler {

    private final StreamingTagParser tagParser;

    private final List<AssistantMessageContent> blocks = new ArrayList<>();
    private final StringBuilder currentTextBuffer = new StringBuilder();
    private boolean hasTextContent = false;

    private ToolUse currentToolUse = null;
    private String currentParamName = null;
    private final StringBuilder paramValueBuffer = new StringBuilder();

    private boolean inThinking = false;
    private boolean completed = false;
    private final StringBuilder feedIncrementalText = new StringBuilder();
    private final StringBuilder accumulatedMessage = new StringBuilder();

    public DefaultStreamingAssistantMessageParser(TagHierarchy config) {
        this.tagParser = new StreamingTagParser(config, this);
    }

    // ==================== StreamingAssistantMessageParser ====================

    @Override
    public void reset() {
        tagParser.reset();
        blocks.clear();
        currentTextBuffer.setLength(0);
        hasTextContent = false;
        currentToolUse = null;
        currentParamName = null;
        paramValueBuffer.setLength(0);
        inThinking = false;
        completed = false;
        feedIncrementalText.setLength(0);
        accumulatedMessage.setLength(0);
    }

    @Override
    public List<AssistantMessageContent> feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return getCurrentBlocks();
        }
        if (completed) {
            return getCurrentBlocks();
        }
        accumulatedMessage.append(chunk);
        feedIncrementalText.setLength(0);
        tagParser.feed(chunk);
        return getCurrentBlocks();
    }

    @Override
    public List<AssistantMessageContent> complete() {
        if (completed) {
            return getCurrentBlocks();
        }
        tagParser.endOfInput();

        if (currentParamName != null && currentToolUse != null) {
            currentToolUse.getParams().put(currentParamName, paramValueBuffer.toString().trim());
            currentParamName = null;
            paramValueBuffer.setLength(0);
        }

        if (currentToolUse != null) {
            currentToolUse.setPartial(false);
            blocks.add(currentToolUse);
            currentToolUse = null;
        }

        finalizeCurrentText();

        for (AssistantMessageContent block : blocks) {
            block.setPartial(false);
        }

        completed = true;
        return getCurrentBlocks();
    }

    @Override
    public List<AssistantMessageContent> getCurrentBlocks() {
        List<AssistantMessageContent> result = new ArrayList<>(blocks.size() + 2);
        result.addAll(blocks);

        if (currentToolUse != null) {
            ToolUse snapshot = new ToolUse();
            snapshot.setName(currentToolUse.getName());
            Map<String, Object> params = new HashMap<>(currentToolUse.getParams());
            if (currentParamName != null) {
                params.put(currentParamName, paramValueBuffer.toString().trim());
            }
            snapshot.setParams(params);
            snapshot.setPartial(true);
            result.add(snapshot);
        }

        if (hasTextContent && currentToolUse == null && !inThinking) {
            appendTextSnapshot(result);
        } else if (hasTextContent && currentToolUse == null && inThinking) {
            appendTextSnapshot(result);
        }

        return result;
    }

    @Override
    public String getAccumulatedMessage() {
        return accumulatedMessage.toString();
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    // ==================== TagEventHandler ====================

    @Override
    public void onStartElement(String tagName, Map<String, String> attributes) {
        if ("function_calls".equals(tagName)) {
            return;
        }

        if ("thinking".equals(tagName)) {
            inThinking = true;
            return;
        }

        if (currentToolUse == null) {
            finalizeCurrentText();

            String toolName;
            if ("invoke".equals(tagName)) {
                toolName = attributes.getOrDefault("name", "unknown");
            } else {
                toolName = tagName;
            }

            currentToolUse = new ToolUse();
            currentToolUse.setName(toolName);
            currentToolUse.setParams(new HashMap<>());
            currentToolUse.setPartial(true);
        } else if (currentParamName == null) {
            String paramName;
            if ("parameter".equals(tagName)) {
                paramName = attributes.getOrDefault("name", "unknown");
            } else {
                paramName = tagName;
            }

            currentParamName = paramName;
            paramValueBuffer.setLength(0);
        }
    }

    @Override
    public void onEndElement(String tagName) {
        if ("function_calls".equals(tagName)) {
            return;
        }

        if ("thinking".equals(tagName)) {
            inThinking = false;
            return;
        }

        if (currentParamName != null) {
            String value = paramValueBuffer.toString().trim();
            if (currentToolUse != null) {
                currentToolUse.getParams().put(currentParamName, value);
            }
            currentParamName = null;
            paramValueBuffer.setLength(0);
        } else if (currentToolUse != null) {
            currentToolUse.setPartial(false);
            blocks.add(currentToolUse);
            currentToolUse = null;
            hasTextContent = false;
            currentTextBuffer.setLength(0);
        }
    }

    @Override
    public void onCharacters(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (currentToolUse != null && currentParamName != null) {
            paramValueBuffer.append(text);
        } else if (currentToolUse == null) {
            currentTextBuffer.append(text);
            hasTextContent = true;
            feedIncrementalText.append(text);
        }
    }

    // ==================== 内部方法 ====================

    private void finalizeCurrentText() {
        if (hasTextContent) {
            String trimmed = currentTextBuffer.toString().trim();
            if (!trimmed.isEmpty()) {
                TextContent textContent = new TextContent(trimmed, false);
                blocks.add(textContent);
            }
            currentTextBuffer.setLength(0);
            hasTextContent = false;
        }
    }

    private void appendTextSnapshot(List<AssistantMessageContent> result) {
        String trimmed = currentTextBuffer.toString().trim();
        if (!trimmed.isEmpty()) {
            TextContent snapshot = new TextContent(trimmed, true);
            snapshot.setIncrementalContent(feedIncrementalText.toString());
            result.add(snapshot);
        }
    }
}
