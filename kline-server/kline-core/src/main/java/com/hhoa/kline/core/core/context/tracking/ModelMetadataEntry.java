package com.hhoa.kline.core.core.context.tracking;

import lombok.Getter;
import lombok.Setter;

/** 模型元数据条目 用于跟踪模型使用情况 */
@Setter
@Getter
public class ModelMetadataEntry {
    private Long ts;

    private String modelId;

    private String modelProviderId;

    private String mode;

    public ModelMetadataEntry() {}

    public ModelMetadataEntry(Long ts, String modelId, String modelProviderId, String mode) {
        this.ts = ts;
        this.modelId = modelId;
        this.modelProviderId = modelProviderId;
        this.mode = mode;
    }
}
