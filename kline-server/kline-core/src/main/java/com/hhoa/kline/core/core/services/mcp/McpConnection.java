package com.hhoa.kline.core.core.services.mcp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McpConnection {
    private McpServerConfig server;
    private IMcpClient client;
}
