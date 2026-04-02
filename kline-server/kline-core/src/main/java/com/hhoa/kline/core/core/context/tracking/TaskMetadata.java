package com.hhoa.kline.core.core.context.tracking;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** 任务元数据 包含文件上下文和模型使用信息 */
@Setter
@Getter
public class TaskMetadata {
    private List<FileMetadataEntry> filesInContext;

    private List<ModelMetadataEntry> modelUsage;

    private List<EnvironmentMetadataEntry> environmentHistory;

    public TaskMetadata() {
        this.filesInContext = new ArrayList<>();
        this.modelUsage = new ArrayList<>();
        this.environmentHistory = new ArrayList<>();
    }

    public TaskMetadata(
            List<FileMetadataEntry> filesInContext,
            List<ModelMetadataEntry> modelUsage,
            List<EnvironmentMetadataEntry> environmentHistory) {
        this.filesInContext = filesInContext != null ? filesInContext : new ArrayList<>();
        this.modelUsage = modelUsage != null ? modelUsage : new ArrayList<>();
        this.environmentHistory =
                environmentHistory != null ? environmentHistory : new ArrayList<>();
    }
}
