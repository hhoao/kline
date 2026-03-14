package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface IMcpHub {

    record Notification(String serverName, String level, String message, long timestamp) {}

    record ServerConnectionForPrompt(
            String serverName, McpServerConfig config, IMcpClient client) {}

    List<IMcpClient> getServers();

    List<ServerConnectionForPrompt> getConnectionsForPrompt();

    boolean isToolAutoApproved(String serverName, String toolName);

    void updateServerConnections(Map<String, McpServerConfig> newServers);

    List<IMcpClient> toggleServerDisabled(String serverName, boolean disabled) throws Exception;

    List<IMcpClient> toggleToolAutoApprove(
            String serverName, List<String> toolNames, boolean shouldAllow) throws Exception;

    List<IMcpClient> addRemoteServer(String serverName, String serverUrl) throws Exception;

    List<IMcpClient> deleteServer(String serverName) throws Exception;

    List<IMcpClient> updateServerTimeout(String serverName, int timeout) throws Exception;

    List<IMcpClient> restartServer(String serverName) throws Exception;

    McpSchema.CallToolResult callTool(
            String serverName, String toolName, Map<String, Object> toolArguments, String ulid)
            throws Exception;

    McpSchema.ReadResourceResult readResource(String serverName, String uri) throws Exception;

    List<IMcpHub.Notification> getPendingNotifications();

    void setNotificationCallback(Consumer<Notification> callback);

    void clearNotificationCallback();

    void dispose();

    void registerInternalClient(IMcpClient client);
}
