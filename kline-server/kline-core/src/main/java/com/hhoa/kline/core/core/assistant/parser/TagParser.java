package com.hhoa.kline.core.core.assistant.parser;

/**
 * 流式标签解析器接口：逐块喂入文本，通过 {@link TagEventHandler} 回调输出事件。
 *
 * @author hhoa
 * @see StreamingTagParser
 */
public interface TagParser {

    /** 喂入新的文本块 */
    void feed(String chunk);

    /** 通知输入结束，刷新残留缓冲 */
    void endOfInput();

    /** 重置解析器状态 */
    void reset();
}
