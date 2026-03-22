package com.hhoa.kline.core.core.assistant.parser;

import java.util.Map;

/**
 * 结构标签事件回调接口（仅对 {@link TagHierarchy} 中配置的结构标签触发）。
 *
 * <p>非结构标签的原始 XML 文本通过 {@link #onCharacters(String)} 输出。
 *
 * @author hhoa
 */
public interface TagEventHandler {

    /** 结构标签开始 */
    void onStartElement(String tagName, Map<String, String> attributes);

    /** 结构标签结束 */
    void onEndElement(String tagName);

    /**
     * 文本内容（包括非结构标签的原始 XML 文本）。
     *
     * <p>例如 {@code <div>hello</div>} 如果 div 不是结构标签， 会触发 {@code onCharacters("<div>hello</div>")}
     */
    void onCharacters(String text);
}
