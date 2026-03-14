package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import java.time.Duration;
import java.util.Optional;

public class DefaultMcpClientFactory implements IMcpClientFactory {

    private static final String DEFAULT_VERSION = "1.0";
    private static final int DEFAULT_TIMEOUT_SECONDS = 20;

    private final IMcpTransportFactory transportFactory;

    public DefaultMcpClientFactory(IMcpTransportFactory transportFactory) {
        this.transportFactory = transportFactory;
    }

    @Override
    public Optional<IMcpClient> createClient(String serverName, McpServerConfig config) {
        if (config == null || Boolean.TRUE.equals(config.getDisabled())) {
            return Optional.empty();
        }
        return transportFactory
                .createTransport(serverName, config)
                .flatMap(transport -> buildClient(serverName, config, transport));
    }

    private String connectedClientName(String clientName, String serverConnectionName) {
        return clientName + " - " + serverConnectionName;
    }

    private Optional<IMcpClient> buildClient(
            String serverName,
            McpServerConfig config,
            io.modelcontextprotocol.spec.McpClientTransport transport) {
        Duration requestTimeout =
                Duration.ofSeconds(
                        config.getTimeout() != null
                                ? config.getTimeout()
                                : DEFAULT_TIMEOUT_SECONDS);
        String clientName = config.getName() != null ? config.getName() : serverName;
        String connectedName = connectedClientName(clientName, serverName);
        String version = DEFAULT_VERSION;

        boolean async = config.getType() == McpClientType.ASYNC;
        try {
            if (async) {
                McpSchema.Implementation clientInfo =
                        new McpSchema.Implementation(connectedName, version);
                McpClient.AsyncSpec spec =
                        McpClient.async(transport)
                                .clientInfo(clientInfo)
                                .requestTimeout(requestTimeout);
                McpAsyncClient sdkClient = spec.build();
                if (!Boolean.TRUE.equals(config.getDisabled())) {
                    sdkClient.initialize().block(requestTimeout);
                }
                return Optional.of(new SdkMcpAsyncClientAdapter(sdkClient));
            } else {
                McpSchema.Implementation clientInfo =
                        new McpSchema.Implementation(connectedName, serverName, version);
                McpClient.SyncSpec spec =
                        McpClient.sync(transport)
                                .clientInfo(clientInfo)
                                .requestTimeout(requestTimeout);
                McpSyncClient sdkClient = spec.build();
                if (!Boolean.TRUE.equals(config.getDisabled())) {
                    sdkClient.initialize();
                }
                return Optional.of(new SdkMcpSyncClientAdapter(sdkClient));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
