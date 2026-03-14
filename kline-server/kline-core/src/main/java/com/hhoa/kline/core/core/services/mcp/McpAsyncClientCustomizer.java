package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.client.McpClient;

public interface McpAsyncClientCustomizer {

    void customize(String serverName, McpClient.AsyncSpec spec);
}
