package com.hhoa.kline.web.common.biz.system.oauth2.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 * OAuth2.0 访问令牌的校验 Response DTO
 *
 */
@Data
public class OAuth2AccessTokenCheckRespDTO implements Serializable {

    /** 用户编号 */
    private Long userId;

    /** 用户信息 */
    private Map<String, String> userInfo;

    /** 授权范围的数组 */
    private List<String> scopes;

    /** 过期时间 */
    private LocalDateTime expiresTime;
}
