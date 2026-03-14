package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import lombok.Data;

@TableName("ai_knowledge_document")
@Data
public class AiKnowledgeDocumentDO extends BaseDO {

    @TableId private Long id;
    private Long knowledgeId;
    private String name;
    private String description;
    private String url;
    private String content;
    private Integer contentLength;
    private Integer tokens;
    private Integer segmentMaxTokens;
    private Integer retrievalCount;
    private Integer status;
}
