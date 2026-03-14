package com.hhoa.kline.core.core.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClineAskUseMcpServer {
    private String serverName;
    private String type;
    private String toolName;
    private String arguments;
    private String uri;
}
