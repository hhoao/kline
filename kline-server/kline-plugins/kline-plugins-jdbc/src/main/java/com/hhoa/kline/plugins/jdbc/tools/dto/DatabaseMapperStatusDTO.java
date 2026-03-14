package com.hhoa.kline.plugins.jdbc.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "查询数据库映射状态请求DTO")
@Data
public class DatabaseMapperStatusDTO {

    @Schema(description = "表名，必填")
    private String tableName;
}
