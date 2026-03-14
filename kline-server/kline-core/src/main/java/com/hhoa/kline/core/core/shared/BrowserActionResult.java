package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserActionResult {
    private String screenshot;
    private String logs;
    private String currentUrl;
    private String currentMousePosition;
}
