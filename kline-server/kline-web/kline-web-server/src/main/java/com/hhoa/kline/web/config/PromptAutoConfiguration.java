package com.hhoa.kline.web.config;

import com.hhoa.kline.core.core.api.TaskContextHolder;
import com.hhoa.kline.core.core.controller.LocalTaskManagerFactory;
import com.hhoa.kline.core.core.controller.TaskManagerFactory;
import com.hhoa.kline.core.core.prompts.systemprompt.DefaultSystemPromptServiceFactory;
import com.hhoa.kline.core.core.prompts.systemprompt.SystemPromptService;
import com.hhoa.kline.core.core.storage.GlobalFileNames;
import com.hhoa.kline.core.core.task.ApiHandler;
import com.hhoa.kline.web.core.SpringAIApiHandler;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import com.hhoa.kline.web.mcp.DefaultInternalClientFactory;
import com.hhoa.kline.web.mcp.McpHubInitializer;
import com.hhoa.kline.web.model.AiModelFactory;
import com.hhoa.kline.web.service.AiKnowledgeDocumentService;
import com.hhoa.kline.web.service.AiToolService;
import com.hhoa.kline.web.utils.AiUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Supplier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PromptDefaultChatProperties.class)
public class PromptAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SpringAIApiHandler springAIApiHandler(
            AiModelFactory aiModelFactory, PromptDefaultChatProperties defaultChatProps) {
        String apiKey = defaultChatProps.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("kline.prompt.default-chat.api-key 未配置");
        }
        ChatModel orCreateChatModel =
                aiModelFactory.getOrCreateChatModel(
                        AiPlatformEnum.DEEP_SEEK, apiKey, defaultChatProps.getBaseUrl());

        String modelId =
                defaultChatProps.getModelId() != null ? defaultChatProps.getModelId() : "3";
        String providerId =
                defaultChatProps.getProviderId() != null
                        ? defaultChatProps.getProviderId()
                        : "deepseek";

        ChatOptions chatOptions =
                AiUtils.buildChatOptions(
                        AiPlatformEnum.DEEP_SEEK,
                        "deepseek-chat",
                        null,
                        8000,
                        new HashSet<>(),
                        new ArrayList<>(),
                        new HashMap<>());

        ChatClient chatClient = ChatClient.create(orCreateChatModel);

        return new SpringAIApiHandler(chatClient, modelId, providerId, chatOptions);
    }

    @Bean
    @ConditionalOnMissingBean
    public TaskManagerFactory taskManagerFactory(
            ApiHandler apiHandler,
            AiToolService toolService,
            @Autowired(required = false) AiKnowledgeDocumentService knowledgeDocumentService) {

        SystemPromptService systemPromptService =
                DefaultSystemPromptServiceFactory.createSystemPromptService();
        Supplier<Path> basePathSupplier =
                () -> {
                    var requestContext = TaskContextHolder.get();
                    if (requestContext == null) {
                        throw new IllegalStateException("TaskContext not set");
                    }
                    return Path.of(
                            GlobalFileNames.BASE_DIR, requestContext.getTaskManagerId().toString());
                };

        DefaultInternalClientFactory internalClientFactory =
                new DefaultInternalClientFactory(knowledgeDocumentService, toolService);
        McpHubInitializer mcpHubInitializer = new McpHubInitializer(internalClientFactory);

        return new LocalTaskManagerFactory(
                apiHandler, systemPromptService, basePathSupplier, mcpHubInitializer);
    }
}
