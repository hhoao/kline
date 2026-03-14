package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFavoriteRequest {
    private Object metadata;
    @Builder.Default private String taskId = "";
    @Builder.Default private boolean isFavorited = false;
}
