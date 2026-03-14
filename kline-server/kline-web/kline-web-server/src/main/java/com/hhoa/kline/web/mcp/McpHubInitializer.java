package com.hhoa.kline.web.mcp;

import com.hhoa.kline.core.core.services.mcp.IMcpClient;
import com.hhoa.kline.core.core.services.mcp.IMcpHub;
import com.hhoa.kline.core.core.services.mcp.IMcpHubInitializer;
import java.util.Optional;

public class McpHubInitializer implements IMcpHubInitializer {

    private final DefaultInternalClientFactory internalClientFactory;

    public McpHubInitializer(DefaultInternalClientFactory internalClientFactory) {
        this.internalClientFactory = internalClientFactory;
    }

    @Override
    public void initialize(IMcpHub mcpHub) {
        if (mcpHub == null || internalClientFactory == null) {
            return;
        }
        Optional<IMcpClient> clientOpt = internalClientFactory.createInternalClient();
        clientOpt.ifPresent(mcpHub::registerInternalClient);
    }
}
