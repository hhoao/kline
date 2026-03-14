package com.hhoa.kline.web.dto.knowledge.knowledge;

import com.hhoa.ai.kline.commons.enums.InEnum;
import com.hhoa.ai.kline.commons.utils.DateUtil;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "AI 知识库的分页 Request VO")
@Data
public class AiKnowledgePageReqVO extends PageParam {

    @Schema(description = "知识库名称", example = "全知")
    private String name;

    @Schema(description = "是否启用", example = "1")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = DateUtil.DATE_PATTERN.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime[] createTime;
}
