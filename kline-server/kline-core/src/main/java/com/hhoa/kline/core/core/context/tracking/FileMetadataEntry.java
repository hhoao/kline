package com.hhoa.kline.core.core.context.tracking;

import lombok.Getter;
import lombok.Setter;

/** 文件元数据条目 用于跟踪文件操作和状态 */
@Setter
@Getter
public class FileMetadataEntry {
    private String path;

    /** 记录状态：active（活跃）或 stale（过期） */
    private String recordState;

    /** 记录来源：read_tool（读取工具）、user_edited（用户编辑）、cline_edited（Cline编辑）、file_mentioned（文件提及） */
    private String recordSource;

    private Long clineReadDate;

    private Long clineEditDate;

    private Long userEditDate;

    public FileMetadataEntry() {}

    public FileMetadataEntry(
            String path,
            String recordState,
            String recordSource,
            Long clineReadDate,
            Long clineEditDate,
            Long userEditDate) {
        this.path = path;
        this.recordState = recordState;
        this.recordSource = recordSource;
        this.clineReadDate = clineReadDate;
        this.clineEditDate = clineEditDate;
        this.userEditDate = userEditDate;
    }
}
