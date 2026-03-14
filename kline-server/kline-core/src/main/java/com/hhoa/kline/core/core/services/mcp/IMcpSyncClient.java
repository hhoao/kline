package com.hhoa.kline.core.core.services.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Root;

public interface IMcpSyncClient extends IMcpClient {

    boolean closeGracefully();

    McpSchema.InitializeResult initialize();

    void rootsListChangedNotification();

    void addRoot(Root root);

    void removeRoot(String rootUri);

    Object ping();

    McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest);

    McpSchema.ListToolsResult listTools();

    McpSchema.ListToolsResult listTools(String cursor);

    McpSchema.ListResourcesResult listResources();

    McpSchema.ListResourcesResult listResources(String cursor);

    McpSchema.ReadResourceResult readResource(McpSchema.Resource resource);

    McpSchema.ReadResourceResult readResource(McpSchema.ReadResourceRequest readResourceRequest);

    McpSchema.ListResourceTemplatesResult listResourceTemplates();

    McpSchema.ListResourceTemplatesResult listResourceTemplates(String cursor);

    void subscribeResource(McpSchema.SubscribeRequest subscribeRequest);

    void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest);

    ListPromptsResult listPrompts();

    ListPromptsResult listPrompts(String cursor);

    GetPromptResult getPrompt(GetPromptRequest getPromptRequest);

    void setLoggingLevel(McpSchema.LoggingLevel loggingLevel);

    McpSchema.CompleteResult completeCompletion(McpSchema.CompleteRequest completeRequest);
}
