package com.hhoa.kline.core.core.services.mcp;

import java.util.Optional;

public interface IMcpClientFactory {

    Optional<IMcpClient> createClient(String serverName, McpServerConfig config);
}
