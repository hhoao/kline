package com.hhoa.kline.core.core.assistant.parser;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 手写流式 XML 标签解析器（替代 Aalto）。
 *
 * <p>只识别 {@link TagHierarchy} 中配置的结构标签并触发事件回调； 非结构标签的原始 XML 文本通过 {@code onCharacters} 输出，从不进入标签栈。
 *
 * @author hhoa
 */
public class StreamingTagParser implements TagParser {

    private enum State {
        TEXT,
        TAG_OPEN,
        OPEN_TAG_NAME,
        SELF_CLOSE_CHECK,
        CLOSE_TAG_NAME,
        TAG_ATTRS,
        ATTR_NAME,
        ATTR_VALUE_START,
        ATTR_VALUE,
        PASSTHROUGH_TAG
    }

    private final TagHierarchy config;
    private final TagEventHandler handler;

    private State state = State.TEXT;
    private final StringBuilder textBuffer = new StringBuilder();
    private final StringBuilder rawTagBuffer = new StringBuilder();
    private final StringBuilder tagNameBuffer = new StringBuilder();
    private final StringBuilder attrNameBuffer = new StringBuilder();
    private final StringBuilder attrValueBuffer = new StringBuilder();
    private final Map<String, String> attributes = new LinkedHashMap<>();
    private char attrQuoteChar;
    private char passQuoteChar;
    private final Deque<String> tagStack = new ArrayDeque<>();

    public StreamingTagParser(TagHierarchy config, TagEventHandler handler) {
        this.config = config;
        this.handler = handler;
    }

    @Override
    public void feed(String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        for (int i = 0; i < chunk.length(); i++) {
            processChar(chunk.charAt(i));
        }
        postFeedFlush();
    }

    @Override
    public void endOfInput() {
        if (state != State.TEXT) {
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            tagNameBuffer.setLength(0);
            attrNameBuffer.setLength(0);
            attrValueBuffer.setLength(0);
            attributes.clear();
            state = State.TEXT;
        }
        if (textBuffer.length() > 0) {
            handler.onCharacters(textBuffer.toString());
            textBuffer.setLength(0);
        }
    }

    @Override
    public void reset() {
        state = State.TEXT;
        textBuffer.setLength(0);
        rawTagBuffer.setLength(0);
        tagNameBuffer.setLength(0);
        attrNameBuffer.setLength(0);
        attrValueBuffer.setLength(0);
        attributes.clear();
        attrQuoteChar = 0;
        passQuoteChar = 0;
        tagStack.clear();
    }

    public Deque<String> getTagStack() {
        return tagStack;
    }

    // ==================== 状态处理 ====================

    private void processChar(char ch) {
        switch (state) {
            case TEXT -> processText(ch);
            case TAG_OPEN -> processTagOpen(ch);
            case OPEN_TAG_NAME -> processOpenTagName(ch);
            case SELF_CLOSE_CHECK -> processSelfCloseCheck(ch);
            case CLOSE_TAG_NAME -> processCloseTagName(ch);
            case TAG_ATTRS -> processTagAttrs(ch);
            case ATTR_NAME -> processAttrName(ch);
            case ATTR_VALUE_START -> processAttrValueStart(ch);
            case ATTR_VALUE -> processAttrValue(ch);
            case PASSTHROUGH_TAG -> processPassthroughTag(ch);
        }
    }

    private void processText(char ch) {
        if (ch == '<') {
            flushTextBuffer();
            rawTagBuffer.setLength(0);
            rawTagBuffer.append('<');
            state = State.TAG_OPEN;
        } else {
            textBuffer.append(ch);
        }
    }

    private void processTagOpen(char ch) {
        if (ch == '/') {
            rawTagBuffer.append('/');
            tagNameBuffer.setLength(0);
            state = State.CLOSE_TAG_NAME;
        } else if (isTagNameStart(ch)) {
            tagNameBuffer.setLength(0);
            tagNameBuffer.append(ch);
            rawTagBuffer.append(ch);
            state = State.OPEN_TAG_NAME;
        } else if (ch == '<') {
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            flushTextBuffer();
            rawTagBuffer.append('<');
        } else {
            textBuffer.append(rawTagBuffer);
            textBuffer.append(ch);
            rawTagBuffer.setLength(0);
            state = State.TEXT;
        }
    }

    private void processOpenTagName(char ch) {
        if (isTagNameChar(ch)) {
            tagNameBuffer.append(ch);
            rawTagBuffer.append(ch);
        } else if (ch == '>') {
            resolveOpenTag(ch);
        } else if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r') {
            resolveOpenTagWithSpace(ch);
        } else if (ch == '/') {
            rawTagBuffer.append('/');
            state = State.SELF_CLOSE_CHECK;
        } else if (ch == '<') {
            rejectTagAndRestart();
        } else {
            rawTagBuffer.append(ch);
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            tagNameBuffer.setLength(0);
            state = State.TEXT;
        }
    }

    private void resolveOpenTag(char closingChar) {
        String tagName = tagNameBuffer.toString();
        rawTagBuffer.append(closingChar);
        if (config.isStructuralTag(tagName, tagStack)) {
            handler.onStartElement(tagName, Collections.emptyMap());
            tagStack.push(tagName);
            rawTagBuffer.setLength(0);
        } else {
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
        }
        tagNameBuffer.setLength(0);
        state = State.TEXT;
    }

    private void resolveOpenTagWithSpace(char spaceChar) {
        String tagName = tagNameBuffer.toString();
        rawTagBuffer.append(spaceChar);
        if (config.isStructuralTag(tagName, tagStack)) {
            attributes.clear();
            state = State.TAG_ATTRS;
        } else {
            passQuoteChar = 0;
            state = State.PASSTHROUGH_TAG;
        }
    }

    private void processSelfCloseCheck(char ch) {
        if (ch == '>') {
            rawTagBuffer.append('>');
            String tagName = tagNameBuffer.toString();
            if (config.isStructuralTag(tagName, tagStack)) {
                Map<String, String> attrs =
                        attributes.isEmpty()
                                ? Collections.emptyMap()
                                : new LinkedHashMap<>(attributes);
                handler.onStartElement(tagName, attrs);
                handler.onEndElement(tagName);
                rawTagBuffer.setLength(0);
            } else {
                textBuffer.append(rawTagBuffer);
                rawTagBuffer.setLength(0);
            }
            tagNameBuffer.setLength(0);
            attributes.clear();
            state = State.TEXT;
        } else {
            rawTagBuffer.append(ch);
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            tagNameBuffer.setLength(0);
            attributes.clear();
            state = State.TEXT;
        }
    }

    private void processCloseTagName(char ch) {
        if (isTagNameChar(ch)) {
            tagNameBuffer.append(ch);
            rawTagBuffer.append(ch);
        } else if (ch == '>') {
            String tagName = tagNameBuffer.toString();
            rawTagBuffer.append('>');
            if (!tagStack.isEmpty() && tagStack.peek().equals(tagName)) {
                handler.onEndElement(tagName);
                tagStack.pop();
                rawTagBuffer.setLength(0);
            } else {
                textBuffer.append(rawTagBuffer);
                rawTagBuffer.setLength(0);
            }
            tagNameBuffer.setLength(0);
            state = State.TEXT;
        } else if (ch == '<') {
            rejectCloseTagAndRestart();
        } else {
            rawTagBuffer.append(ch);
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            tagNameBuffer.setLength(0);
            state = State.TEXT;
        }
    }

    // ==================== 属性解析 ====================

    private void processTagAttrs(char ch) {
        if (ch == '>') {
            emitStructuralOpen();
        } else if (ch == '/') {
            rawTagBuffer.append('/');
            state = State.SELF_CLOSE_CHECK;
        } else if (isTagNameStart(ch)) {
            attrNameBuffer.setLength(0);
            attrNameBuffer.append(ch);
            rawTagBuffer.append(ch);
            state = State.ATTR_NAME;
        } else {
            rawTagBuffer.append(ch);
        }
    }

    private void processAttrName(char ch) {
        if (ch == '=') {
            rawTagBuffer.append('=');
            state = State.ATTR_VALUE_START;
        } else if (isTagNameChar(ch) || ch == ':') {
            attrNameBuffer.append(ch);
            rawTagBuffer.append(ch);
        } else if (ch == '>') {
            attributes.put(attrNameBuffer.toString(), "");
            attrNameBuffer.setLength(0);
            emitStructuralOpen();
        } else {
            rawTagBuffer.append(ch);
        }
    }

    private void processAttrValueStart(char ch) {
        rawTagBuffer.append(ch);
        if (ch == '"' || ch == '\'') {
            attrQuoteChar = ch;
            attrValueBuffer.setLength(0);
            state = State.ATTR_VALUE;
        } else if (ch == '>') {
            attributes.put(attrNameBuffer.toString(), "");
            attrNameBuffer.setLength(0);
            emitStructuralOpen();
        } else {
            attrValueBuffer.setLength(0);
            attrValueBuffer.append(ch);
            attrQuoteChar = 0;
            state = State.ATTR_VALUE;
        }
    }

    private void processAttrValue(char ch) {
        rawTagBuffer.append(ch);
        if (attrQuoteChar != 0) {
            if (ch == attrQuoteChar) {
                attributes.put(attrNameBuffer.toString(), attrValueBuffer.toString());
                attrNameBuffer.setLength(0);
                attrValueBuffer.setLength(0);
                state = State.TAG_ATTRS;
            } else {
                attrValueBuffer.append(ch);
            }
        } else {
            if (ch == '>' || ch == ' ' || ch == '\t') {
                attributes.put(attrNameBuffer.toString(), attrValueBuffer.toString());
                attrNameBuffer.setLength(0);
                attrValueBuffer.setLength(0);
                if (ch == '>') {
                    emitStructuralOpen();
                } else {
                    state = State.TAG_ATTRS;
                }
            } else {
                attrValueBuffer.append(ch);
            }
        }
    }

    private void emitStructuralOpen() {
        rawTagBuffer.append('>');
        String tagName = tagNameBuffer.toString();
        Map<String, String> attrs =
                attributes.isEmpty() ? Collections.emptyMap() : new LinkedHashMap<>(attributes);
        handler.onStartElement(tagName, attrs);
        tagStack.push(tagName);
        rawTagBuffer.setLength(0);
        tagNameBuffer.setLength(0);
        attributes.clear();
        state = State.TEXT;
    }

    // ==================== 非结构标签透传 ====================

    private void processPassthroughTag(char ch) {
        rawTagBuffer.append(ch);
        if (ch == '"' || ch == '\'') {
            if (passQuoteChar == 0) {
                passQuoteChar = ch;
            } else if (passQuoteChar == ch) {
                passQuoteChar = 0;
            }
        } else if (ch == '>' && passQuoteChar == 0) {
            textBuffer.append(rawTagBuffer);
            rawTagBuffer.setLength(0);
            tagNameBuffer.setLength(0);
            state = State.TEXT;
        }
    }

    // ==================== 辅助方法 ====================

    private void postFeedFlush() {
        if (state == State.TEXT) {
            flushTextBuffer();
        } else if (state == State.OPEN_TAG_NAME) {
            if (!config.isPossiblePrefix(tagNameBuffer.toString(), tagStack)) {
                textBuffer.append(rawTagBuffer);
                rawTagBuffer.setLength(0);
                tagNameBuffer.setLength(0);
                state = State.TEXT;
                flushTextBuffer();
            }
        } else if (state == State.CLOSE_TAG_NAME) {
            String name = tagNameBuffer.toString();
            boolean couldMatch = false;
            if (!tagStack.isEmpty()) {
                couldMatch = tagStack.peek().startsWith(name);
            }
            if (!couldMatch) {
                textBuffer.append(rawTagBuffer);
                rawTagBuffer.setLength(0);
                tagNameBuffer.setLength(0);
                state = State.TEXT;
                flushTextBuffer();
            }
        }
    }

    private void flushTextBuffer() {
        if (textBuffer.length() > 0) {
            handler.onCharacters(textBuffer.toString());
            textBuffer.setLength(0);
        }
    }

    private void rejectTagAndRestart() {
        textBuffer.append(rawTagBuffer);
        rawTagBuffer.setLength(0);
        tagNameBuffer.setLength(0);
        flushTextBuffer();
        rawTagBuffer.append('<');
        state = State.TAG_OPEN;
    }

    private void rejectCloseTagAndRestart() {
        textBuffer.append(rawTagBuffer);
        rawTagBuffer.setLength(0);
        tagNameBuffer.setLength(0);
        flushTextBuffer();
        rawTagBuffer.append('<');
        state = State.TAG_OPEN;
    }

    private static boolean isTagNameStart(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    private static boolean isTagNameChar(char ch) {
        return isTagNameStart(ch) || (ch >= '0' && ch <= '9') || ch == '-' || ch == '.';
    }
}
