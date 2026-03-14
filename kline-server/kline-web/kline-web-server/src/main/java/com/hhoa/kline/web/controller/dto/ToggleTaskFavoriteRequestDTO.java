package com.hhoa.kline.web.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToggleTaskFavoriteRequestDTO {
    @NotBlank(message = "任务ID不能为空")
    private String taskId;

    private Boolean favorited;
}
