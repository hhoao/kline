package com.hhoa.kline.web.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteTasksRequestDTO {
    @NotEmpty(message = "任务ID列表不能为空")
    private List<String> value;
}
