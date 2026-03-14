package com.hhoa.kline.web.mcp;

import com.hhoa.kline.core.core.services.mcp.IInternalClientFactory;
import com.hhoa.kline.core.core.services.mcp.IMcpClient;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiToolService;
import java.util.Optional;

public class DefaultInternalClientFactory implements IInternalClientFactory {

    private final AiKnowledgeDocumentService knowledgeDocumentService;
    private final AiToolService toolService;

    public DefaultInternalClientFactory(
            AiKnowledgeDocumentService knowledgeDocumentService, AiToolService toolService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.toolService = toolService;
    }

    @Override
    public Optional<IMcpClient> createInternalClient() {
        if (knowledgeDocumentService == null) {
            return Optional.empty();
        }
        BuiltinMcpClient client = new BuiltinMcpClient(knowledgeDocumentService, toolService);
        client.initialize();
        return Optional.of(client);
    }
}
