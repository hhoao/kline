package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import lombok.Data;

@TableName("ai_knowledge_segment")
@Data
public class AiKnowledgeSegmentDO extends BaseDO {

    public static final String VECTOR_ID_EMPTY = "";

    @TableId private Long id;
    private Long knowledgeId;
    private Long documentId;
    private String content;
    private Integer contentLength;
    private String vectorId;
    private Integer tokens;
    private Integer retrievalCount;
    private Integer status;
}
