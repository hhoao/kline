package com.hhoa.kline.core.core.shared.proto.host;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextEditorInfo {
    @Builder.Default private String documentPath = "";
    private Integer viewColumn;
    @Builder.Default private boolean isActive = false;
}
