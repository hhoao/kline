package com.hhoa.kline.web.mcp;

import com.hhoa.ai.kline.commons.utils.JsonUtils;
import com.hhoa.kline.core.core.services.mcp.IMcpSyncClient;
import com.hhoa.kline.web.dal.dataobject.AiKnowledgeDocumentDO;
import com.hhoa.kline.web.dto.knowledge.document.AiKnowledgeDocumentPageReqVO;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiToolService;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities;
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.ListPromptsResult;
import io.modelcontextprotocol.spec.McpSchema.Root;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuiltinMcpClient implements IMcpSyncClient {

    public static final String KNOWLEDGE_DOCUMENT_URI_PREFIX = "knowledge://document/";
    private static final String SERVER_NAME = "internal";
    private static final String VERSION = "1.0";
    private static final int RESOURCES_PAGE_SIZE = 50;

    private final AiKnowledgeDocumentService knowledgeDocumentService;
    private final AiToolService toolService;
    private McpSchema.InitializeResult initResult;

    public BuiltinMcpClient(
            AiKnowledgeDocumentService knowledgeDocumentService, AiToolService toolService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.toolService = toolService;
        this.initResult = null;
    }

    @Override
    public McpSchema.InitializeResult getCurrentInitializationResult() {
        return initResult;
    }

    @Override
    public McpSchema.ServerCapabilities getServerCapabilities() {
        return new McpSchema.ServerCapabilities.Builder().resources(true, true).build();
    }

    @Override
    public String getServerInstructions() {
        return "";
    }

    @Override
    public McpSchema.Implementation getServerInfo() {
        return new McpSchema.Implementation(SERVER_NAME, VERSION);
    }

    @Override
    public boolean isInitialized() {
        return initResult != null;
    }

    @Override
    public ClientCapabilities getClientCapabilities() {
        return new McpSchema.ClientCapabilities.Builder().build();
    }

    @Override
    public McpSchema.Implementation getClientInfo() {
        return new McpSchema.Implementation(SERVER_NAME + "-client", VERSION);
    }

    @Override
    public void close() {}

    @Override
    public boolean closeGracefully() {
        close();
        return true;
    }

    @Override
    public McpSchema.InitializeResult initialize() {
        if (initResult != null) {
            return initResult;
        }
        McpSchema.ServerCapabilities caps = getServerCapabilities();
        initResult =
                new McpSchema.InitializeResult(
                        "2024-11-05", caps, new McpSchema.Implementation(SERVER_NAME, VERSION), "");
        return initResult;
    }

    @Override
    public void rootsListChangedNotification() {}

    @Override
    public void addRoot(Root root) {}

    @Override
    public void removeRoot(String rootUri) {}

    @Override
    public Object ping() {
        return null;
    }

    @Override
    public McpSchema.CallToolResult callTool(McpSchema.CallToolRequest callToolRequest) {
        if (callToolRequest == null) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("callToolRequest is null")
                    .isError(true)
                    .build();
        }
        if (toolService == null) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("内置服务器未配置工具服务")
                    .isError(true)
                    .build();
        }
        String toolName = callToolRequest.name();
        if (toolName == null || toolName.isBlank()) {
            return McpSchema.CallToolResult.builder()
                    .addTextContent("工具名称为空")
                    .isError(true)
                    .build();
        }
        Map<String, Object> args = callToolRequest.arguments();
        String argsJson = (args == null || args.isEmpty()) ? "{}" : JsonUtils.toJsonString(args);
        try {
            String result = toolService.callTool(toolName, argsJson);
            if (result == null) {
                return McpSchema.CallToolResult.builder()
                        .addTextContent("工具不存在: " + toolName)
                        .isError(true)
                        .build();
            }
            return McpSchema.CallToolResult.builder().addTextContent(result).isError(false).build();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return McpSchema.CallToolResult.builder()
                    .addTextContent("工具执行失败: " + msg)
                    .isError(true)
                    .build();
        }
    }

    @Override
    public McpSchema.ListToolsResult listTools() {
        List<McpSchema.Tool> tools = toolService != null ? toolService.getMcpTools() : List.of();
        return new McpSchema.ListToolsResult(tools, null);
    }

    @Override
    public McpSchema.ListToolsResult listTools(String cursor) {
        return listTools();
    }

    @Override
    public McpSchema.ListResourcesResult listResources() {
        return listResources(null);
    }

    @Override
    public McpSchema.ListResourcesResult listResources(String cursor) {
        if (knowledgeDocumentService == null) {
            return new McpSchema.ListResourcesResult(List.of(), null);
        }
        AiKnowledgeDocumentPageReqVO req = new AiKnowledgeDocumentPageReqVO();
        req.setPageNo(1);
        req.setPageSize(RESOURCES_PAGE_SIZE);
        var pageResult = knowledgeDocumentService.getKnowledgeDocumentPage(req);
        List<AiKnowledgeDocumentDO> list =
                pageResult != null && pageResult.getList() != null
                        ? pageResult.getList()
                        : List.of();
        List<McpSchema.Resource> resources = new ArrayList<>();
        for (AiKnowledgeDocumentDO doc : list) {
            String uri = KNOWLEDGE_DOCUMENT_URI_PREFIX + doc.getId();
            String name = doc.getName() != null ? doc.getName() : ("文档 " + doc.getId());
            String desc = doc.getDescription() != null ? doc.getDescription() : "";
            resources.add(
                    McpSchema.Resource.builder()
                            .uri(uri)
                            .name(name)
                            .description(desc)
                            .mimeType("text/plain")
                            .build());
        }
        return new McpSchema.ListResourcesResult(resources, null);
    }

    @Override
    public McpSchema.ReadResourceResult readResource(McpSchema.Resource resource) {
        if (resource != null && resource.uri() != null) {
            return readResource(new McpSchema.ReadResourceRequest(resource.uri()));
        }
        return new McpSchema.ReadResourceResult(List.of());
    }

    @Override
    public McpSchema.ReadResourceResult readResource(
            McpSchema.ReadResourceRequest readResourceRequest) {
        String uri = readResourceRequest != null ? readResourceRequest.uri() : null;
        if (uri == null || !uri.startsWith(KNOWLEDGE_DOCUMENT_URI_PREFIX)) {
            return new McpSchema.ReadResourceResult(List.of());
        }
        String idStr = uri.substring(KNOWLEDGE_DOCUMENT_URI_PREFIX.length()).trim();
        long id;
        try {
            id = Long.parseLong(idStr);
        } catch (NumberFormatException e) {
            return new McpSchema.ReadResourceResult(List.of());
        }
        AiKnowledgeDocumentDO doc = knowledgeDocumentService.getKnowledgeDocument(id);
        if (doc == null) {
            return new McpSchema.ReadResourceResult(List.of());
        }
        String content = doc.getContent() != null ? doc.getContent() : "";
        McpSchema.TextResourceContents textContent =
                new McpSchema.TextResourceContents(uri, "text/plain", content);
        return new McpSchema.ReadResourceResult(List.of(textContent));
    }

    @Override
    public McpSchema.ListResourceTemplatesResult listResourceTemplates() {
        return listResourceTemplates(null);
    }

    @Override
    public McpSchema.ListResourceTemplatesResult listResourceTemplates(String cursor) {
        McpSchema.ResourceTemplate template =
                McpSchema.ResourceTemplate.builder()
                        .uriTemplate(KNOWLEDGE_DOCUMENT_URI_PREFIX + "{id}")
                        .name("知识库文档")
                        .description("通过文档 id 获取知识库文档内容")
                        .build();
        return new McpSchema.ListResourceTemplatesResult(List.of(template), null);
    }

    @Override
    public void subscribeResource(McpSchema.SubscribeRequest subscribeRequest) {}

    @Override
    public void unsubscribeResource(McpSchema.UnsubscribeRequest unsubscribeRequest) {}

    @Override
    public ListPromptsResult listPrompts() {
        return new McpSchema.ListPromptsResult(List.of(), null);
    }

    @Override
    public ListPromptsResult listPrompts(String cursor) {
        return new McpSchema.ListPromptsResult(List.of(), null);
    }

    @Override
    public GetPromptResult getPrompt(GetPromptRequest getPromptRequest) {
        throw new UnsupportedOperationException("内置服务器不支持 prompts");
    }

    @Override
    public void setLoggingLevel(McpSchema.LoggingLevel loggingLevel) {}

    @Override
    public McpSchema.CompleteResult completeCompletion(McpSchema.CompleteRequest completeRequest) {
        throw new UnsupportedOperationException("内置服务器不支持 completion");
    }
}
