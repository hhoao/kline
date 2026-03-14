package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RedactedThinkingContentBlock extends UserContentBlock {
    @JsonProperty("data")
    private String data;

    public RedactedThinkingContentBlock(String data) {
        this.data = data;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.REDACTED_THINKING;
    }
}
