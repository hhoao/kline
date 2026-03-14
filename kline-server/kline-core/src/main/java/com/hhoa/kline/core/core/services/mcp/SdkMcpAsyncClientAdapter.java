package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.Root;
import reactor.core.publisher.Mono;

public class SdkMcpAsyncClientAdapter implements IMcpASyncClient {

    private final McpAsyncClient client;

    public SdkMcpAsyncClientAdapter(McpAsyncClient client) {
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
    public Mono<Void> closeGracefully() {
        return client.closeGracefully();
    }

    @Override
    public Mono<McpSchema.InitializeResult> initialize() {
        return client.initialize();
    }

    @Override
    public Mono<Object> ping() {
        return client.ping();
    }

    @Override
    public Mono<Void> addRoot(Root root) {
        return client.addRoot(root);
    }

    @Override
    public Mono<Void> removeRoot(String rootUri) {
        return client.removeRoot(rootUri);
    }

    @Override
    public Mono<Void> rootsListChangedNotification() {
        return client.rootsListChangedNotification();
    }

    @Override
    public Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest) {
        return client.callTool(callToolRequest);
    }

    @Override
    public Mono<McpSchema.ListToolsResult> listTools() {
        return client.listTools();
    }

    @Override
    public Mono<McpSchema.ListToolsResult> listTools(String cursor) {
        return client.listTools(cursor);
    }

    @Override
    public Mono<McpSchema.ListResourcesResult> listResources() {
        return client.listResources();
    }

    @Override
    public Mono<McpSchema.ListResourcesResult> listResources(String cursor) {
        return client.listResources(cursor);
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource) {
        return client.readResource(resource);
    }

    @Override
    public Mono<McpSchema.ReadResourceResult> readResource(
            McpSchema.ReadResourceRequest readResourceRequest) {
        return client.readResource(readResourceRequest);
    }

    @Override
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates() {
        return client.listResourceTemplates();
    }

    @Override
    public Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates(String cursor) {
        return client.listResourceTemplates(cursor);
    }

    @Override
    public Mono<Void> subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {
        return client.subscribeResource(subscribeRequest);
    }

    @Override
    public Mono<Void> unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {
        return client.unsubscribeResource(unsubscribeRequest);
    }

    @Override
    public Mono<ListPromptsResult> listPrompts() {
        return client.listPrompts();
    }

    @Override
    public Mono<ListPromptsResult> listPrompts(String cursor) {
        return client.listPrompts(cursor);
    }

    @Override
    public Mono<GetPromptResult> getPrompt(GetPromptRequest getPromptRequest) {
        return client.getPrompt(getPromptRequest);
    }

    @Override
    public Mono<Void> setLoggingLevel(LoggingLevel loggingLevel) {
        return client.setLoggingLevel(loggingLevel);
    }

    @Override
    public Mono<McpSchema.CompleteResult> completeCompletion(
            McpSchema.CompleteRequest completeRequest) {
        return client.completeCompletion(completeRequest);
    }
}
