package com.hhoa.kline.web.dto.chat.conversation;

import com.hhoa.ai.kline.commons.utils.DateUtil;
import com.hhoa.kline.web.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "AI 聊天对话的分页 Request VO")
@Data
public class AiChatConversationPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "对话标题", example = "你好")
    private String title;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = DateUtil.DATE_PATTERN.YYYY_MM_DD_HH_MM_SS)
    private LocalDateTime[] createTime;
}
