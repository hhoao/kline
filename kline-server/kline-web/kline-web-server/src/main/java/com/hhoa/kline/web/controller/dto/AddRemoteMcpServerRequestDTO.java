package com.hhoa.kline.web.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddRemoteMcpServerRequestDTO {
    @NotBlank(message = "服务器名称不能为空")
    private String serverName;

    @NotBlank(message = "服务器URL不能为空")
    private String serverUrl;
}
