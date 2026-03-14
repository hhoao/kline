package com.hhoa.kline.core.core.services.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultMcpTransportFactory implements IMcpTransportFactory {

    private static final String DEFAULT_HTTP_ENDPOINT = "/mcp";
    private static final String DEFAULT_SSE_ENDPOINT = "/sse";

    private final ObjectMapper objectMapper;

    public DefaultMcpTransportFactory() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Optional<McpClientTransport> createTransport(String serverName, McpServerConfig config) {
        if (config == null || Boolean.TRUE.equals(config.getDisabled())) {
            return Optional.empty();
        }
        McpTransportType type = config.getMcpTransportType();
        if (type == null) {
            type = inferTransportType(config);
        }
        return switch (type) {
            case STDIO -> createStdioTransport(config);
            case HTTP -> createStreamableHttpTransport(config);
            case SSE -> createSseTransport(config);
        };
    }

    private McpTransportType inferTransportType(McpServerConfig config) {
        if (config.getCommand() != null && !config.getCommand().isBlank()) {
            return McpTransportType.STDIO;
        }
        if (config.getUrl() != null && !config.getUrl().isBlank()) {
            return McpTransportType.HTTP;
        }
        return null;
    }

    private Optional<McpClientTransport> createStdioTransport(McpServerConfig config) {
        String command = config.getCommand();
        if (command == null || command.isBlank()) {
            return Optional.empty();
        }
        ServerParameters.Builder paramsBuilder = ServerParameters.builder(command);
        List<String> args = config.getArgs();
        if (args != null && !args.isEmpty()) {
            paramsBuilder.args(args);
        }
        Map<String, String> env = config.getEnv();
        if (env != null && !env.isEmpty()) {
            paramsBuilder.env(env);
        }
        ServerParameters params = paramsBuilder.build();
        StdioClientTransport transport =
                new StdioClientTransport(params, new JacksonMcpJsonMapper(objectMapper));
        return Optional.of(transport);
    }

    private Optional<McpClientTransport> createStreamableHttpTransport(McpServerConfig config) {
        String url = config.getUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        HttpClientStreamableHttpTransport.Builder builder =
                HttpClientStreamableHttpTransport.builder(url)
                        .endpoint(DEFAULT_HTTP_ENDPOINT)
                        .clientBuilder(HttpClient.newBuilder())
                        .jsonMapper(new JacksonMcpJsonMapper(objectMapper));
        applyHeaders(builder, config.getHeaders());
        return Optional.of(builder.build());
    }

    private Optional<McpClientTransport> createSseTransport(McpServerConfig config) {
        String url = config.getUrl();
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        HttpClientSseClientTransport.Builder builder =
                HttpClientSseClientTransport.builder(url)
                        .sseEndpoint(DEFAULT_SSE_ENDPOINT)
                        .clientBuilder(HttpClient.newBuilder())
                        .jsonMapper(new JacksonMcpJsonMapper(objectMapper));
        applySseHeaders(builder, config.getHeaders());
        return Optional.of(builder.build());
    }

    private void applyHeaders(
            HttpClientStreamableHttpTransport.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            builder.customizeRequest(req -> headers.forEach((k, v) -> req.header(k, v)));
        }
    }

    private void applySseHeaders(
            HttpClientSseClientTransport.Builder builder, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            builder.customizeRequest(req -> headers.forEach((k, v) -> req.header(k, v)));
        }
    }
}
