package com.hhoa.kline.web.controller.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointRestoreRequestDTO {
    private Object metadata;

    private String taskId;

    @NotNull(message = "检查点编号不能为空")
    private Long number;

    @NotNull(message = "恢复类型不能为空")
    private String restoreType;

    private Long offset;
}
