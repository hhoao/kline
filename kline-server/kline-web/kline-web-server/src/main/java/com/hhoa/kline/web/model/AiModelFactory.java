package com.hhoa.kline.web.model;

import com.hhoa.kline.web.enums.AiPlatformEnum;
import com.hhoa.kline.web.model.midjourney.api.MidjourneyApi;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * AI Model 模型工厂的接口类
 *
 * @author hhoa
 */
public interface AiModelFactory {

    /**
     * 基于指定配置，获得 ChatModel 对象
     *
     * <p>如果不存在，则进行创建
     *
     * @param platform 平台
     * @param apiKey API KEY
     * @param url API URL
     * @return ChatModel 对象
     */
    ChatModel getOrCreateChatModel(AiPlatformEnum platform, String apiKey, String url);

    /**
     * 基于指定配置，获得 ImageModel 对象
     *
     * <p>如果不存在，则进行创建
     *
     * @param platform 平台
     * @param apiKey API KEY
     * @param url API URL
     * @return ImageModel 对象
     */
    ImageModel getOrCreateImageModel(AiPlatformEnum platform, String apiKey, String url);

    /**
     * 基于指定配置，获得 MidjourneyApi 对象
     *
     * <p>如果不存在，则进行创建
     *
     * @param apiKey API KEY
     * @param url API URL
     * @return MidjourneyApi 对象
     */
    MidjourneyApi getOrCreateMidjourneyApi(String apiKey, String url);

    /**
     * 基于指定配置，获得 EmbeddingModel 对象
     *
     * <p>如果不存在，则进行创建
     *
     * @param platform 平台
     * @param apiKey API KEY
     * @param url API URL
     * @param model 模型
     * @return ChatModel 对象
     */
    EmbeddingModel getOrCreateEmbeddingModel(
            AiPlatformEnum platform, String apiKey, String url, String model);

    /**
     * 基于指定配置，获得 VectorStore 对象
     *
     * <p>如果不存在，则进行创建
     *
     * @param type 向量存储类型
     * @param embeddingModel 向量模型
     * @param metadataFields 元数据字段
     * @return VectorStore 对象
     */
    VectorStore getOrCreateVectorStore(
            Class<? extends VectorStore> type,
            EmbeddingModel embeddingModel,
            Map<String, Class<?>> metadataFields);
}
