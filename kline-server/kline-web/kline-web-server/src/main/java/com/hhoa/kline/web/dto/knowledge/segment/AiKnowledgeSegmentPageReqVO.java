package com.hhoa.kline.web.dto.knowledge.segment;

import com.hhoa.ai.kline.commons.enums.InEnum;
import com.hhoa.kline.web.common.enums.CommonStatusEnum;
import com.hhoa.kline.web.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI 知识库分段的分页 Request VO")
@Data
public class AiKnowledgeSegmentPageReqVO extends PageParam {

    @Schema(description = "文档编号", example = "1")
    private Long documentId;

    @Schema(description = "分段内容关键字", example = "Java 开发")
    private String content;

    @Schema(description = "分段状态", example = "1")
    @InEnum(CommonStatusEnum.class)
    private Integer status;
}
