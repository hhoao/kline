package com.hhoa.kline.web.config;

import com.hhoa.kline.web.model.AiModelFactory;
import com.hhoa.kline.web.model.AiModelFactoryImpl;
import com.hhoa.kline.web.model.midjourney.api.MidjourneyApi;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI 自动配置
 *
 * @author hhoa
 */
@Configuration
@Import(PromptAutoConfiguration.class)
@EnableConfigurationProperties({
    AiModelProperties.class,
})
@Slf4j
public class AiAutoConfiguration {
    @Bean
    public AiModelFactory aiModelFactory(
            ToolCallingManager toolCallingManager,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            RetryTemplate retryTemplate,
            ResponseErrorHandler responseErrorHandler,
            ObjectProvider<ChatModelObservationConvention> observationConvention,
            ObjectProvider<ToolExecutionEligibilityPredicate>
                    azureOpenAiToolExecutionEligibilityPredicate,
            ObjectProvider<EmbeddingModelObservationConvention>
                    embeddingModelObservationConvention) {

        return new AiModelFactoryImpl(
                toolCallingManager,
                observationRegistry,
                restClientBuilderProvider,
                webClientBuilderProvider,
                retryTemplate,
                responseErrorHandler,
                observationConvention,
                azureOpenAiToolExecutionEligibilityPredicate,
                embeddingModelObservationConvention);
    }

    // ========== RAG 相关 ==========

    @Bean
    @ConditionalOnProperty(value = "kline.ai.midjourney.enable", havingValue = "true")
    public MidjourneyApi midjourneyApi(AiModelProperties aiModelProperties) {
        AiModelProperties.MidjourneyProperties config = aiModelProperties.getMidjourney();
        return new MidjourneyApi(config.getBaseUrl(), config.getApiKey(), config.getNotifyUrl());
    }

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }
}
