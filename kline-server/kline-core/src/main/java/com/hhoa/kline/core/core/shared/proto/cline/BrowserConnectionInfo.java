package com.hhoa.kline.core.core.shared.proto.cline;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowserConnectionInfo {
    @Builder.Default private boolean isConnected = false;
    @Builder.Default private boolean isRemote = false;
    private String host;
}
