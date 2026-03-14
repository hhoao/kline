package com.hhoa.kline.web.dto.knowledge.segment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "AI 知识库段落向量进度 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiKnowledgeSegmentProcessRespVO {

    @Schema(description = "文档编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long documentId;

    @Schema(description = "总段落数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Long count;

    @Schema(description = "已向量化段落数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    private Long embeddingCount;
}
