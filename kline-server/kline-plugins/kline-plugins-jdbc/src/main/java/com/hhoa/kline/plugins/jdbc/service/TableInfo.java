package com.hhoa.kline.plugins.jdbc.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 表信息DTO */
@Schema(description = "表信息 Response VO")
@Data
public class TableInfo {

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "user_table")
    private String tableName;

    @Schema(
            description = "表类型",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "BASE TABLE")
    private String tableType;

    @Schema(description = "表注释", example = "用户信息表")
    private String tableComment;

    @Schema(
            description = "Schema名称",
            requiredMode = Schema.RequiredMode.REQUIRED,
            example = "public")
    private String schema;
}
