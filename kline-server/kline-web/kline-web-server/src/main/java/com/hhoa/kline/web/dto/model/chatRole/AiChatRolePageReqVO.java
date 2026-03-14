package com.hhoa.kline.web.dto.model.chatRole;

import com.hhoa.kline.web.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "AI 聊天角色分页 Request VO")
@Data
public class AiChatRolePageReqVO extends PageParam {

    @Schema(description = "角色名称", example = "李四")
    private String name;

    @Schema(description = "角色类别", example = "创作")
    private String category;

    @Schema(description = "是否公开", example = "1")
    private Boolean publicStatus;

    @Schema(description = "用户编号", example = "1")
    private Long userId;

    @Schema(description = "状态", example = "0")
    private Integer status;
}
