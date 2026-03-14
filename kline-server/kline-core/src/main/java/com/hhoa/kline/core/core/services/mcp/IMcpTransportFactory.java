package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.spec.McpClientTransport;
import java.util.Optional;

public interface IMcpTransportFactory {

    Optional<McpClientTransport> createTransport(String serverName, McpServerConfig config);
}
