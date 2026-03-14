package com.hhoa.kline.web.controller.dto;

import com.hhoa.kline.core.core.shared.storage.Settings;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewTaskRequestDTO {
    @NotBlank(message = "任务文本不能为空")
    private String text;

    private List<String> images;

    private List<String> files;

    private Settings taskSettings;
}
