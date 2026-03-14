package com.hhoa.kline.web.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteQuickWinRequestDTO {
    @Builder.Default private String command = "";

    @Builder.Default private String title = "";
}
