package com.hhoa.kline.core.core.services.mcp;

import com.hhoa.kline.core.core.shared.storage.Settings;
import com.hhoa.kline.core.core.storage.StateManager;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultMcpHub implements IMcpHub {

    private static final int DEFAULT_TIMEOUT_SECONDS = 20;
    static final String INTERNAL_SERVER_NAME = "internal";

    private final IMcpClientFactory clientFactory;
    private final StateManager stateManager;
    private final Map<String, McpConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<Notification> pendingNotifications =
            new ConcurrentLinkedQueue<>();

    private volatile Consumer<Notification> notificationCallback;

    public DefaultMcpHub(StateManager stateManager) {
        this(null, stateManager);
    }

    public DefaultMcpHub(IMcpClientFactory clientFactory, StateManager stateManager) {
        if (clientFactory == null) {
            this.clientFactory = new DefaultMcpClientFactory(new DefaultMcpTransportFactory());
        } else {
            this.clientFactory = Objects.requireNonNull(clientFactory);
        }
        this.stateManager = Objects.requireNonNull(stateManager);
        initializeFromSettings();
    }

    @Override
    public synchronized void registerInternalClient(IMcpClient client) {
        if (client == null) {
            return;
        }
        if (connections.containsKey(INTERNAL_SERVER_NAME)) {
            return;
        }
        McpServerConfig config = internalServerConfig();
        connections.put(INTERNAL_SERVER_NAME, new McpConnection(config, client));
    }

    @Override
    public List<IMcpClient> getServers() {
        List<IMcpClient> result = new ArrayList<>();
        for (McpConnection connection : connections.values()) {
            if (connection.getClient() != null
                    && !Boolean.TRUE.equals(connection.getServer().getDisabled())) {
                result.add(connection.getClient());
            }
        }
        return result;
    }

    @Override
    public List<IMcpHub.ServerConnectionForPrompt> getConnectionsForPrompt() {
        List<IMcpHub.ServerConnectionForPrompt> result = new ArrayList<>();
        for (Map.Entry<String, McpConnection> entry : connections.entrySet()) {
            McpConnection conn = entry.getValue();
            if (conn.getClient() != null && !Boolean.TRUE.equals(conn.getServer().getDisabled())) {
                result.add(
                        new IMcpHub.ServerConnectionForPrompt(
                                entry.getKey(), conn.getServer(), conn.getClient()));
            }
        }
        return result;
    }

    @Override
    public boolean isToolAutoApproved(String serverName, String toolName) {
        McpConnection connection = getRequiredConnection(serverName);
        List<String> autoApprove = connection.getServer().getAutoApprove();
        return autoApprove != null && autoApprove.contains(toolName);
    }

    @Override
    public synchronized void updateServerConnections(Map<String, McpServerConfig> newServers) {
        McpConnection preservedInternal = connections.remove(INTERNAL_SERVER_NAME);
        Map<String, McpServerConfig> normalized = normalizeConfigs(newServers);
        Set<String> incomingNames = new HashSet<>(normalized.keySet());
        Set<String> currentNames = new HashSet<>(connections.keySet());

        for (String currentName : currentNames) {
            if (!incomingNames.contains(currentName)) {
                removeConnection(currentName);
            }
        }

        for (Map.Entry<String, McpServerConfig> entry : normalized.entrySet()) {
            String serverName = entry.getKey();
            McpServerConfig nextConfig = entry.getValue();
            McpConnection existing = connections.get(serverName);
            if (existing == null) {
                connect(serverName, nextConfig);
                continue;
            }

            if (!Objects.equals(existing.getServer(), nextConfig)) {
                reconnect(serverName, nextConfig);
            }
        }
        if (preservedInternal != null) {
            connections.put(INTERNAL_SERVER_NAME, preservedInternal);
        }
        persistMcpServers();
    }

    @Override
    public synchronized List<IMcpClient> toggleServerDisabled(String serverName, boolean disabled)
            throws Exception {
        McpConnection connection = getRequiredConnection(serverName);
        McpServerConfig next = copyConfig(connection.getServer());
        next.setDisabled(disabled);
        reconnect(serverName, next);
        persistMcpServers();
        return getServers();
    }

    @Override
    public synchronized List<IMcpClient> toggleToolAutoApprove(
            String serverName, List<String> toolNames, boolean shouldAllow) throws Exception {
        McpConnection connection = getRequiredConnection(serverName);
        McpServerConfig next = copyConfig(connection.getServer());

        List<String> autoApprove =
                next.getAutoApprove() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(next.getAutoApprove());
        if (toolNames != null) {
            for (String toolName : toolNames) {
                if (toolName == null || toolName.isBlank()) {
                    continue;
                }
                if (shouldAllow) {
                    if (!autoApprove.contains(toolName)) {
                        autoApprove.add(toolName);
                    }
                } else {
                    autoApprove.remove(toolName);
                }
            }
        }
        next.setAutoApprove(autoApprove);

        connections.put(serverName, new McpConnection(next, connection.getClient()));
        persistMcpServers();
        return getServers();
    }

    @Override
    public synchronized List<IMcpClient> addRemoteServer(String serverName, String serverUrl)
            throws Exception {
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName 不能为空");
        }
        if (serverUrl == null || serverUrl.isBlank()) {
            throw new IllegalArgumentException("serverUrl 不能为空");
        }
        if (connections.containsKey(serverName)) {
            throw new IllegalArgumentException("serverName 已存在: " + serverName);
        }

        McpServerConfig config = new McpServerConfig();
        config.setName(serverName);
        config.setUrl(serverUrl);
        config.setDisabled(false);
        config.setTimeout(DEFAULT_TIMEOUT_SECONDS);
        config.setAutoApprove(new ArrayList<>());
        config.setMcpTransportType(McpTransportType.HTTP);

        connect(serverName, config);
        persistMcpServers();
        return getServers();
    }

    @Override
    public synchronized List<IMcpClient> deleteServer(String serverName) throws Exception {
        if (!connections.containsKey(serverName)) {
            throw new IllegalArgumentException("未找到服务器: " + serverName);
        }
        removeConnection(serverName);
        persistMcpServers();
        return getServers();
    }

    @Override
    public synchronized List<IMcpClient> updateServerTimeout(String serverName, int timeout)
            throws Exception {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout 必须大于 0");
        }
        McpConnection connection = getRequiredConnection(serverName);
        McpServerConfig next = copyConfig(connection.getServer());
        next.setTimeout(timeout);
        reconnect(serverName, next);
        persistMcpServers();
        return getServers();
    }

    @Override
    public synchronized List<IMcpClient> restartServer(String serverName) throws Exception {
        McpConnection connection = getRequiredConnection(serverName);
        McpServerConfig config = copyConfig(connection.getServer());
        reconnect(serverName, config);
        persistMcpServers();
        return getServers();
    }

    @Override
    public McpSchema.CallToolResult callTool(
            String serverName, String toolName, Map<String, Object> toolArguments, String ulid)
            throws Exception {
        McpConnection connection = getRequiredConnection(serverName);
        if (Boolean.TRUE.equals(connection.getServer().getDisabled())) {
            throw new IllegalStateException("服务器已禁用: " + serverName);
        }
        IMcpClient client = connection.getClient();
        if (client == null) {
            throw new IllegalStateException("服务器未连接: " + serverName);
        }
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest(toolName, toolArguments);
        try {
            if (client instanceof IMcpSyncClient syncClient) {
                return syncClient.callTool(request);
            }
            if (client instanceof IMcpASyncClient asyncClient) {
                return asyncClient.callTool(request).block();
            }
            throw new IllegalStateException("不支持的客户端类型");
        } catch (Exception e) {
            emitNotification(
                    serverName, "error", e.getMessage() == null ? "MCP 工具调用失败" : e.getMessage());
            throw e;
        }
    }

    @Override
    public McpSchema.ReadResourceResult readResource(String serverName, String uri)
            throws Exception {
        McpConnection connection = getRequiredConnection(serverName);
        if (Boolean.TRUE.equals(connection.getServer().getDisabled())) {
            throw new IllegalStateException("服务器已禁用: " + serverName);
        }
        IMcpClient client = connection.getClient();
        if (client == null) {
            throw new IllegalStateException("服务器未连接: " + serverName);
        }
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(uri);
        if (client instanceof IMcpSyncClient syncClient) {
            return syncClient.readResource(request);
        }
        if (client instanceof IMcpASyncClient asyncClient) {
            return asyncClient.readResource(request).block();
        }
        throw new IllegalStateException("不支持的客户端类型");
    }

    @Override
    public List<Notification> getPendingNotifications() {
        List<Notification> notifications = new ArrayList<>();
        Notification notification;
        while ((notification = pendingNotifications.poll()) != null) {
            notifications.add(notification);
        }
        return notifications;
    }

    @Override
    public void setNotificationCallback(Consumer<Notification> callback) {
        this.notificationCallback = callback;
    }

    @Override
    public void clearNotificationCallback() {
        this.notificationCallback = null;
    }

    @Override
    public synchronized void dispose() {
        for (String serverName : new ArrayList<>(connections.keySet())) {
            removeConnection(serverName);
        }
    }

    private McpServerConfig internalServerConfig() {
        McpServerConfig config = new McpServerConfig();
        config.setName(INTERNAL_SERVER_NAME);
        config.setDisabled(false);
        config.setAutoApprove(new ArrayList<>());
        return config;
    }

    private McpConnection getRequiredConnection(String serverName) {
        McpConnection connection = connections.get(serverName);
        if (connection == null) {
            throw new IllegalArgumentException("未找到服务器: " + serverName);
        }
        return connection;
    }

    private void emitNotification(String serverName, String level, String message) {
        Notification notification =
                new Notification(
                        serverName,
                        level,
                        message == null ? "" : message,
                        System.currentTimeMillis());
        Consumer<Notification> callback = notificationCallback;
        if (callback != null) {
            callback.accept(notification);
        } else {
            pendingNotifications.add(notification);
        }
    }

    private void connect(String serverName, McpServerConfig config) {
        McpServerConfig next = copyConfig(config);
        next.setName(serverName);

        if (Boolean.TRUE.equals(next.getDisabled())) {
            connections.put(serverName, new McpConnection(next, null));
            return;
        }

        Optional<IMcpClient> maybeClient = clientFactory.createClient(serverName, next);
        IMcpClient client = maybeClient.orElse(null);
        connections.put(serverName, new McpConnection(next, client));

        if (client == null) {
            emitNotification(serverName, "error", "MCP 客户端创建失败");
        }
    }

    private void reconnect(String serverName, McpServerConfig config) {
        removeConnection(serverName);
        connect(serverName, config);
    }

    private void removeConnection(String serverName) {
        McpConnection existing = connections.remove(serverName);
        if (existing == null) {
            return;
        }
        IMcpClient client = existing.getClient();
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭 MCP 连接失败, server={}", serverName, e);
            }
        }
    }

    private McpServerConfig copyConfig(McpServerConfig source) {
        McpServerConfig target = new McpServerConfig();
        target.setType(source.getType());
        target.setDisabled(source.getDisabled());
        target.setTimeout(source.getTimeout());
        target.setAutoApprove(
                source.getAutoApprove() == null ? null : new ArrayList<>(source.getAutoApprove()));
        target.setCommand(source.getCommand());
        target.setMcpTransportType(source.getMcpTransportType());
        target.setArgs(source.getArgs() == null ? null : new ArrayList<>(source.getArgs()));
        target.setCwd(source.getCwd());
        target.setEnv(source.getEnv() == null ? null : new HashMap<>(source.getEnv()));
        target.setUrl(source.getUrl());
        target.setHeaders(source.getHeaders() == null ? null : new HashMap<>(source.getHeaders()));
        target.setName(source.getName());
        return target;
    }

    private Map<String, McpServerConfig> normalizeConfigs(Map<String, McpServerConfig> configs) {
        Map<String, McpServerConfig> result = new HashMap<>();
        if (configs == null) {
            return result;
        }
        for (Map.Entry<String, McpServerConfig> entry : configs.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            McpServerConfig copy = copyConfig(entry.getValue());
            copy.setName(entry.getKey());
            if (copy.getTimeout() == null || copy.getTimeout() <= 0) {
                copy.setTimeout(DEFAULT_TIMEOUT_SECONDS);
            }
            if (copy.getAutoApprove() == null) {
                copy.setAutoApprove(new ArrayList<>());
            }
            result.put(entry.getKey(), copy);
        }
        return result;
    }

    private Map<String, McpServerConfig> snapshotConfigs() {
        Map<String, McpServerConfig> snapshot = new HashMap<>();
        for (Map.Entry<String, McpConnection> entry : connections.entrySet()) {
            if (INTERNAL_SERVER_NAME.equals(entry.getKey())) {
                continue;
            }
            snapshot.put(entry.getKey(), copyConfig(entry.getValue().getServer()));
        }
        return snapshot;
    }

    private void persistMcpServers() {
        try {
            Settings settings = stateManager.getSettings();
            settings.setMcpServers(snapshotConfigs());
            stateManager.updateSettings(settings);
        } catch (Exception e) {
            log.warn("写入 MCP settings 失败", e);
        }
    }

    private void initializeFromSettings() {
        try {
            Map<String, McpServerConfig> mcpServers = stateManager.getSettings().getMcpServers();
            updateServerConnections(mcpServers != null ? mcpServers : Map.of());
        } catch (Exception e) {
            log.warn("初始化 MCP settings 失败", e);
        }
    }
}
