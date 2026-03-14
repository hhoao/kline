package com.hhoa.kline.plugins.jdbc.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Jdbc获取字段列表请求DTO
 *
 * @author xianxing
 * @since 2025/9/23
 */
@Schema(description = "Jdbc获取字段列表请求DTO")
@Data
public class JdbcGetColumnsDTO {
    /** 表名 */
    @Schema(description = "表名，必填")
    private String tableName;
}
