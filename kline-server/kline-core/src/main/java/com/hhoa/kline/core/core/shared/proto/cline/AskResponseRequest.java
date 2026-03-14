package com.hhoa.kline.core.core.shared.proto.cline;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AskResponseRequest {
    private Object metadata;
    @Builder.Default private String responseType = "";
    @Builder.Default private String text = "";
    @Builder.Default private List<String> images = new ArrayList<>();
    @Builder.Default private List<String> files = new ArrayList<>();
}
