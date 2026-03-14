package com.hhoa.kline.web.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hhoa.kline.web.common.mybatis.core.dataobject.BaseDO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("ai_chat_conversation")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatConversationDO extends BaseDO {

    public static final String TITLE_DEFAULT = "新对话";

    @TableId private Long id;
    private Long userId;
    private String title;
    private Boolean pinned;
    private LocalDateTime pinnedTime;
    private Long roleId;
    private Long modelId;
    private String model;
    private String systemMessage;
    private Double temperature;
    private Integer maxTokens;
    private Integer maxContexts;
}
