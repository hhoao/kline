package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import lombok.Data;

@TableName("ai_knowledge")
@Data
public class AiKnowledgeDO extends BaseDO {

    @TableId private Long id;
    private String name;
    private String description;
    private Long embeddingModelId;
    private String embeddingModel;

    @TableField("top_k")
    private Integer topK;

    private Double similarityThreshold;
    private Integer status;
}
