package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserSettings {
    private Viewport viewport;
    private String remoteBrowserHost;
    private Boolean remoteBrowserEnabled;
    private String chromeExecutablePath;
    private Boolean disableToolUse;
    private String customArgs;
}
