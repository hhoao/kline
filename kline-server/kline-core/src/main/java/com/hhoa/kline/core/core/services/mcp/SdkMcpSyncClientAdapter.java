package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Root;

public class SdkMcpSyncClientAdapter implements IMcpSyncClient, AutoCloseable {

    private final McpSyncClient client;

    public SdkMcpSyncClientAdapter(McpSyncClient client) {
        this.client = client;
    }

    @Override
    public McpSchema.InitializeResult getCurrentInitializationResult() {
        return client.getCurrentInitializationResult();
    }

    @Override
    public McpSchema.ServerCapabilities getServerCapabilities() {
        return client.getServerCapabilities();
    }

    @Override
    public String getServerInstructions() {
        return client.getServerInstructions();
    }

    @Override
    public McpSchema.Implementation getServerInfo() {
        return client.getServerInfo();
    }

    @Override
    public boolean isInitialized() {
        return client.isInitialized();
    }

    @Override
    public ClientCapabilities getClientCapabilities() {
        return client.getClientCapabilities();
    }

    @Override
    public McpSchema.Implementation getClientInfo() {
        return client.getClientInfo();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public boolean closeGracefully() {
        return client.closeGracefully();
    }

    @Override
    public McpSchema.InitializeResult initialize() {
        return client.initialize();
    }

    @Override
    public void rootsListChangedNotification() {
        client.rootsListChangedNotification();
    }

    @Override
    public void addRoot(Root root) {
        client.addRoot(root);
    }

    @Override
    public void removeRoot(String rootUri) {
        client.removeRoot(rootUri);
    }

    @Override
    public Object ping() {
        return client.ping();
    }

    @Override
    public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
        return client.callTool(callToolRequest);
    }

    @Override
    public McpSchema.ListToolsResult listTools() {
        return client.listTools();
    }

    @Override
    public McpSchema.ListToolsResult listTools(String cursor) {
        return client.listTools(cursor);
    }

    @Override
    public McpSchema.ListResourcesResult listResources() {
        return client.listResources();
    }

    @Override
    public McpSchema.ListResourcesResult listResources(String cursor) {
        return client.listResources(cursor);
    }

    @Override
    public McpSchema.ReadResourceResult readResource(McpSchema.Resource resource) {
        return client.readResource(resource);
    }

    @Override
    public McpSchema.ReadResourceResult readResource(
            McpSchema.ReadResourceRequest readResourceRequest) {
        return client.readResource(readResourceRequest);
    }

    @Override
    public McpSchema.ListResourceTemplatesResult listResourceTemplates() {
        return client.listResourceTemplates();
    }

    @Override
    public McpSchema.ListResourceTemplatesResult listResourceTemplates(String cursor) {
        return client.listResourceTemplates(cursor);
    }

    @Override
    public void subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
        client.subscribeResource(subscribeRequest);
    }

    @Override
    public void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
        client.unsubscribeResource(unsubscribeRequest);
    }

    @Override
    public ListPromptsResult listPrompts() {
        return client.listPrompts();
    }

    @Override
    public ListPromptsResult listPrompts(String cursor) {
        return client.listPrompts(cursor);
    }

    @Override
    public GetPromptResult getPrompt(GetPromptRequest getPromptRequest) {
        return client.getPrompt(getPromptRequest);
    }

    @Override
    public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {
        client.setLoggingLevel(loggingLevel);
    }

    @Override
    public McpSchema.CompleteResult completeCompletion(McpSchema.CompleteRequest completeRequest) {
        return client.completeCompletion(completeRequest);
    }
}
