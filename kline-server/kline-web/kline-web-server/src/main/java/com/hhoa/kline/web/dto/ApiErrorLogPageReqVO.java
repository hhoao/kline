package com.hhoa.kline.web.dto;

import com.hhoa.ai.kline.commons.utils.DateUtil;
import com.hhoa.kline.web.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - API 错误日志分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ApiErrorLogPageReqVO extends PageParam {

    @Schema(description = "用户编号", example = "666")
    private Long userId;

    @Schema(description = "应用名", example = "dashboard")
    private String applicationName;

    @Schema(description = "请求地址", example = "/xx/yy")
    private String requestUrl;

    @DateTimeFormat(pattern = DateUtil.DATE_PATTERN.YYYY_MM_DD_HH_MM_SS)
    @Schema(description = "异常发生时间")
    private LocalDateTime[] exceptionTime;

    @Schema(description = "处理状态", example = "0")
    private Integer processStatus;
}
