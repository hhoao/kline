package com.hhoa.kline.web.dto.model.tool;

import com.hhoa.ai.kline.commons.enums.InEnum;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "AI 工具新增/修改 Request VO")
@Data
public class AiToolSaveReqVO {

    @Schema(description = "工具编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "19661")
    private Long id;

    @Schema(description = "工具名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "工具名称不能为空")
    private String name;

    @Schema(description = "工具描述", example = "你猜")
    private String description;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @InEnum(CommonStatusEnum.class)
    private Integer status;
}
