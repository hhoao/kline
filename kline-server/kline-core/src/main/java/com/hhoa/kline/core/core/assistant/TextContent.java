package com.hhoa.kline.core.core.assistant;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TextContent extends AssistantMessageContent {

    private String content;

    /** 增量内容（仅包含新增的文本部分，用于流式传输优化） */
    private String incrementalContent;

    public TextContent() {
        this.type = "text";
    }

    public TextContent(String content, Boolean partial) {
        this();
        this.content = content;
        this.partial = partial;
    }
}
