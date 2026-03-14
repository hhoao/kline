package com.hhoa.kline.web.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleToolAutoApproveRequestDTO {
    @Valid private Object metadata;

    @NotBlank(message = "服务器名称不能为空")
    private String serverName;

    private List<String> toolNames;

    @NotNull(message = "自动批准标志不能为空")
    private Boolean autoApprove;
}
