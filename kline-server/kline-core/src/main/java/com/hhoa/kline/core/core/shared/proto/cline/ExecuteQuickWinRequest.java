package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteQuickWinRequest {
    private Object metadata;
    @Builder.Default private String command = "";
    @Builder.Default private String title = "";
}
