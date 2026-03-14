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
public class NewTaskRequest {
    private Object metadata;
    @Builder.Default private String text = "";
    @Builder.Default private List<String> images = new ArrayList<>();
    @Builder.Default private List<String> files = new ArrayList<>();
    // TODO: 需要从 state.proto 导入 Settings 类型
    private Object taskSettings;
}
