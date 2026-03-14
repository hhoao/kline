package com.hhoa.kline.core.core.services.mcp;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MCP 配置模式定义 用于验证和转换 MCP 服务器配置
 *
 * @author hhoa
 */
public final class McpSchemas {
    private static final String TYPE_ERROR_MESSAGE =
            "MCP server config type must be one of: stdio, sse, streamableHttp";

    private McpSchemas() {
        throw new UnsupportedOperationException();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BaseConfig {
        private List<String> autoApprove;

        private Boolean disabled;

        private Integer timeout;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class StdioConfig extends BaseConfig {
        private String type = "stdio";

        private String command;

        private List<String> args;

        private String cwd;

        private Map<String, String> env;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class SseConfig extends BaseConfig {
        private String type = "sse";

        private String url;

        private Map<String, String> headers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @lombok.EqualsAndHashCode(callSuper = false)
    public static class StreamableHttpConfig extends BaseConfig {
        private String type = "streamableHttp";

        private String url;

        private Map<String, String> headers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class McpSettings {
        private Map<String, Object> mcpServers;
    }

    /**
     * 验证服务器配置类型
     *
     * @param config 配置对象
     * @return 配置类型（stdio, sse, streamableHttp）
     * @throws IllegalArgumentException 如果类型无效
     */
    public static String validateServerType(Object config) {
        if (config instanceof Map<?, ?> map) {
            Object typeObj = map.get("type");
            if (typeObj != null) {
                String type = typeObj.toString();
                if ("stdio".equals(type) || "sse".equals(type) || "streamableHttp".equals(type)) {
                    return type;
                }
            }
        }
        throw new IllegalArgumentException(TYPE_ERROR_MESSAGE);
    }
}
