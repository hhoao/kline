package com.hhoa.kline.plugins.jdbc.service;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 表字段信息DTO */
@Schema(description = "表字段信息 Response VO")
@Data
public class TableColumnInfo {

    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "id")
    private String columnName;

    @Schema(description = "默认值", example = "nextval('table_id_seq'::regclass)")
    private String columnDefault;

    @Schema(description = "是否可空", requiredMode = Schema.RequiredMode.REQUIRED, example = "false")
    private Boolean isNullable;

    @Schema(description = "数据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "bigint")
    private String dataType;

    @Schema(description = "字段位置", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer ordinalPosition;

    @Schema(description = "字段注释", example = "主键ID")
    private String columnComment;

    @Schema(description = "是否为自增字段", example = "true")
    private Boolean isAutoIncrement;
}
