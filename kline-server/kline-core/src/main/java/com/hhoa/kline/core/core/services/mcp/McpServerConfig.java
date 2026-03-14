package com.hhoa.kline.core.core.services.mcp;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class McpServerConfig {
    private McpClientType type;

    private Boolean disabled;

    private Integer timeout;

    private List<String> autoApprove;

    private String command;

    private McpTransportType mcpTransportType;

    private List<String> args;

    private String cwd;

    private Map<String, String> env;

    private String url;

    private Map<String, String> headers;

    private String name;
}
