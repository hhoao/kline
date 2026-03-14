package com.hhoa.kline.plugins.jdbc.tools.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Jdbc执行SQL请求DTO
 *
 * @author xianxing
 * @since 2025/9/23
 */
@Schema(description = "Jdbc执行SQL请求DTO")
@Data
public class JdbcExecuteSqlDTO {

    /** SQL语句 */
    @Schema(description = "要执行的SQL语句，必填")
    private String sql;

    /** 是否只读 */
    @Schema(description = "SQL执行是否只读，默认true")
    private Boolean readOnly = true;

    /** 是否级联 */
    @Schema(description = "SQL执行是否级联，默认false")
    private Boolean cascade = false;
}
