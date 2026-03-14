package com.hhoa.kline.web.config;

import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiImageAutoConfiguration;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxChatAutoConfiguration;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiImageAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * 向量存储自动配置排除配置 用于在代码中禁用特定的向量存储自动配置
 *
 * @author hhoa
 */
@Configuration
@EnableAutoConfiguration(
        exclude = {
            QdrantVectorStoreAutoConfiguration.class,
            MilvusVectorStoreAutoConfiguration.class,
            AzureOpenAiAudioTranscriptionAutoConfiguration.class,
            AzureOpenAiEmbeddingAutoConfiguration.class,
            AzureOpenAiChatAutoConfiguration.class,
            AzureOpenAiImageAutoConfiguration.class,
            MiniMaxChatAutoConfiguration.class,
            MiniMaxEmbeddingAutoConfiguration.class,
            OpenAiChatAutoConfiguration.class,
            OpenAiAudioTranscriptionAutoConfiguration.class,
            OpenAiEmbeddingAutoConfiguration.class,
            OpenAiImageAutoConfiguration.class,
            OpenAiAudioSpeechAutoConfiguration.class,
            OpenAiModerationAutoConfiguration.class,
            StabilityAiImageAutoConfiguration.class,
            ZhiPuAiChatAutoConfiguration.class,
            ZhiPuAiImageAutoConfiguration.class,
            ZhiPuAiEmbeddingAutoConfiguration.class
        })
public class SpringModelAutoConfiguration {}
