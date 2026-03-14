package com.hhoa.kline.plugins.jdbc.tools.dto;

import com.hhoa.kline.plugins.jdbc.dbfilemapping.enums.FileStructureMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "创建数据库映射请求DTO")
@Data
public class DatabaseMapperCreateDTO {

    @Schema(description = "表名，必填。要映射的数据库表名称")
    private String tableName;

    @Schema(description = "主键列名，必填。用于唯一标识每条记录的主键字段名")
    private String primaryKeyColumn;

    @Schema(
            description =
                    "文件结构模式，可选值：SINGLE_JSON 或 FIELD_FILES，默认 SINGLE_JSON。"
                            + "SINGLE_JSON：每个记录一个JSON文件，适合字段较少、数据量小的表；"
                            + "FIELD_FILES：每个字段一个独立文件，适合需要修改长文本字段、大字段或字段很多的表，"
                            + "特别适合修改字段长数据（如TEXT、CLOB类型字段）")
    private FileStructureMode fileStructureMode = FileStructureMode.SINGLE_JSON;

    @Schema(
            description =
                    "是否等待同步完成，默认 false。"
                            + "如果设置为 true，方法将等待初始同步完成后再返回，"
                            + "确保数据已完全同步到文件系统。"
                            + "适用于需要立即使用同步数据的场景。")
    private Boolean waitForSync = false;
}
