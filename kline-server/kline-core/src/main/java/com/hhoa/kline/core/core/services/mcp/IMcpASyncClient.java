package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingLevel;
import io.modelcontextprotocol.spec.McpSchema.Root;
import reactor.core.publisher.Mono;

public interface IMcpASyncClient extends IMcpClient {

    Mono<Void> closeGracefully();

    Mono<McpSchema.InitializeResult> initialize();

    Mono<Object> ping();

    Mono<Void> addRoot(Root root);

    Mono<Void> removeRoot(String rootUri);

    Mono<Void> rootsListChangedNotification();

    Mono<McpSchema.CallToolResult> callTool(McpSchema.CallToolRequest callToolRequest);

    Mono<McpSchema.ListToolsResult> listTools();

    Mono<McpSchema.ListToolsResult> listTools(String cursor);

    Mono<McpSchema.ListResourcesResult> listResources();

    Mono<McpSchema.ListResourcesResult> listResources(String cursor);

    Mono<McpSchema.ReadResourceResult> readResource(McpSchema.Resource resource);

    Mono<McpSchema.ReadResourceResult> readResource(
            McpSchema.ReadResourceRequest readResourceRequest);

    Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates();

    Mono<McpSchema.ListResourceTemplatesResult> listResourceTemplates(String cursor);

    Mono<Void> subscribeResource(McpSchema.SubscribeRequest subscribeRequest);

    Mono<Void> unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest);

    Mono<ListPromptsResult> listPrompts();

    Mono<ListPromptsResult> listPrompts(String cursor);

    Mono<GetPromptResult> getPrompt(GetPromptRequest getPromptRequest);

    Mono<Void> setLoggingLevel(LoggingLevel loggingLevel);

    Mono<McpSchema.CompleteResult> completeCompletion(McpSchema.CompleteRequest completeRequest);
}
