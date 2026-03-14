package com.hhoa.kline.web.common.biz.system.oauth2.dto;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * OAuth2.0 访问令牌创建 Request DTO
 *
 */
@Data
public class OAuth2AccessTokenCreateReqDTO implements Serializable {

    /** 用户编号 */
    @NotNull(message = "用户编号不能为空")
    private Long userId;

    /** 客户端编号 */
    @NotNull(message = "客户端编号不能为空")
    private String clientId;

    /** 授权范围 */
    private List<String> scopes;
}
