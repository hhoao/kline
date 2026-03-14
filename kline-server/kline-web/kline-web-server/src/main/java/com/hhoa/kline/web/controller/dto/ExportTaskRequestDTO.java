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
public class ExportTaskRequestDTO {
    @NotBlank(message = "任务ID不能为空")
    private String value;
}
