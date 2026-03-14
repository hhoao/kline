package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ThinkingContentBlock extends UserContentBlock {
    @JsonProperty("thinking")
    private String thinking;

    @JsonProperty("signature")
    private String signature; // 可选

    public ThinkingContentBlock(String thinking, String signature) {
        this.thinking = thinking;
        this.signature = signature;
    }

    public ThinkingContentBlock(String thinking) {
        this.thinking = thinking;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.THINKING;
    }
}
