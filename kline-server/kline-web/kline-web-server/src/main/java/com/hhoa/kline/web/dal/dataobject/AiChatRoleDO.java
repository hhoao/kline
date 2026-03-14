package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_chat_role")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatRoleDO extends BaseDO {

    @TableId private Long id;
    private String name;
    private String avatar;
    private String category;
    private String description;
    private String systemMessage;
    private Long userId;
    private Long modelId;
    private String knowledgeIds;
    private String toolIds;
    private Boolean publicStatus;
    private Integer sort;
    private Integer status;
}
