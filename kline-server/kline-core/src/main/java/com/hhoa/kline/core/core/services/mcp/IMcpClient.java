package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;

public interface IMcpClient {

    McpSchema.InitializeResult getCurrentInitializationResult();

    McpSchema.ServerCapabilities getServerCapabilities();

    String getServerInstructions();

    McpSchema.Implementation getServerInfo();

    boolean isInitialized();

    ClientCapabilities getClientCapabilities();

    McpSchema.Implementation getClientInfo();

    void close();
}
