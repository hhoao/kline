package com.hhoa.kline.core.core.context.management;

import java.util.List;
import lombok.Data;

/** 上下文更新：[时间戳, 更新类型, 更新内容, 元数据] */
@Data
public class ContextUpdate {
    private final long timestamp;
    private final String updateType;
    private final List<String> update;
    private final List<List<String>> metadata;

    public ContextUpdate(
            long timestamp, String updateType, List<String> update, List<List<String>> metadata) {
        this.timestamp = timestamp;
        this.updateType = updateType;
        this.update = update;
        this.metadata = metadata;
    }
}
