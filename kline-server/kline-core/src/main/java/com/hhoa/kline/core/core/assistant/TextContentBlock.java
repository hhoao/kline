package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TextContentBlock extends UserContentBlock {
    @JsonProperty("text")
    private String text;

    @JsonProperty("reasoning_details")
    private List<Object> reasoningDetails; // 仅用于 cline/openrouter 提供者

    public TextContentBlock(String text) {
        this.text = text;
    }

    public TextContentBlock(String text, List<Object> reasoningDetails) {
        this.text = text;
        this.reasoningDetails = reasoningDetails;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.TEXT;
    }
}
