package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplaceTextRequest {
    private Object metadata;
    private String diffId;
    private String content;
    private Integer startLine;
    private Integer endLine;
}
