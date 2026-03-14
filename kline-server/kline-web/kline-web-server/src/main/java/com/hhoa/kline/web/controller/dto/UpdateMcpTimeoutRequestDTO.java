package com.hhoa.kline.web.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMcpTimeoutRequestDTO {
    @NotBlank(message = "服务器名称不能为空")
    private String serverName;

    @NotNull(message = "超时时间不能为空")
    private Integer timeout;
}
