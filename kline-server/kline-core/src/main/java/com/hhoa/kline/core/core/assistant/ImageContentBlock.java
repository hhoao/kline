package com.hhoa.kline.core.core.assistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ImageContentBlock extends UserContentBlock {
    @JsonProperty("source")
    private String source; // base64 编码的图像数据或 URL

    @JsonProperty("source_type")
    private String sourceType; // "base64" 或其他类型

    @JsonProperty("media_type")
    private String mediaType; // 如 "image/png"

    public ImageContentBlock(String source, String sourceType, String mediaType) {
        this.source = source;
        this.sourceType = sourceType;
        this.mediaType = mediaType;
    }

    @Override
    public ContentBlockType getType() {
        return ContentBlockType.IMAGE;
    }
}
