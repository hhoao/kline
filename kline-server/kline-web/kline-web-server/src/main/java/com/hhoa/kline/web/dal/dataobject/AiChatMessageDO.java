package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_chat_message")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatMessageDO extends BaseDO {

    @TableId private Long id;
    private Long conversationId;
    private Long replyId;
    private String type;
    private Long userId;
    private Long roleId;
    private String model;
    private Long modelId;
    private String content;
    private Boolean useContext;
    private String segmentIds;
    private String metadata;
}
