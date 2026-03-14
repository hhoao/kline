package com.hhoa.kline.web.model;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.func.Func0;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.hhoa.kline.web.config.AiModelProperties;
import com.hhoa.kline.web.enums.AiPlatformEnum;
import com.hhoa.kline.web.model.midjourney.api.MidjourneyApi;
import com.hhoa.kline.web.model.siliconflow.SiliconFlowApiConstants;
import com.hhoa.kline.web.model.siliconflow.SiliconFlowImageApi;
import com.hhoa.kline.web.model.siliconflow.SiliconFlowImageModel;
import io.micrometer.observation.ObservationRegistry;
import io.milvus.client.MilvusServiceClient;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import java.io.File;
import java.time.Duration;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import lombok.SneakyThrows;
import org.jspecify.annotations.NonNull;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.observation.ChatModelObservationConvention;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatProperties;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiClientBuilderConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiConnectionProperties;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingProperties;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxConnectionProperties;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingAutoConfiguration;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingProperties;
import org.springframework.ai.model.ollama.autoconfigure.OllamaInitializationProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiConnectionProperties;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiImageAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionEligibilityPredicate;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatProperties;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiConnectionProperties;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingProperties;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.stabilityai.StabilityAiImageModel;
import org.springframework.ai.stabilityai.api.StabilityAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientConnectionDetails;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientProperties;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.DefaultVectorStoreObservationConvention;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiEmbeddingModel;
import org.springframework.ai.zhipuai.ZhiPuAiImageModel;
import org.springframework.ai.zhipuai.api.ZhiPuAiImageApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/** AI Model 模型工厂的实现类 */
public class AiModelFactoryImpl implements AiModelFactory {
    private final ToolCallingManager toolCallingManager;
    private final ObjectProvider<ObservationRegistry> observationRegistry;
    private final ObjectProvider<RestClient.Builder> restClientBuilderProvider;
    private final ObjectProvider<WebClient.Builder> webClientBuilderProvider;
    private final RetryTemplate retryTemplate;
    private final ResponseErrorHandler responseErrorHandler;
    private final ObjectProvider<ChatModelObservationConvention> chatObservationConvention;
    private final ObjectProvider<EmbeddingModelObservationConvention>
            embeddingModelObservationConvention;
    private final ObjectProvider<ToolExecutionEligibilityPredicate>
            toolExecutionEligibilityPredicate;

    public AiModelFactoryImpl(
            ToolCallingManager toolCallingManager,
            ObjectProvider<ObservationRegistry> observationRegistry,
            ObjectProvider<RestClient.Builder> restClientBuilderProvider,
            ObjectProvider<WebClient.Builder> webClientBuilderProvider,
            RetryTemplate retryTemplate,
            ResponseErrorHandler responseErrorHandler,
            ObjectProvider<ChatModelObservationConvention> chatObservationConvention,
            ObjectProvider<ToolExecutionEligibilityPredicate> toolExecutionEligibilityPredicate,
            ObjectProvider<EmbeddingModelObservationConvention>
                    embeddingModelObservationConvention) {
        this.toolCallingManager = toolCallingManager;
        this.observationRegistry = observationRegistry;
        this.restClientBuilderProvider = restClientBuilderProvider;
        this.webClientBuilderProvider = webClientBuilderProvider;
        this.retryTemplate = retryTemplate;
        this.responseErrorHandler = responseErrorHandler;
        this.chatObservationConvention = chatObservationConvention;
        this.toolExecutionEligibilityPredicate = toolExecutionEligibilityPredicate;
        this.embeddingModelObservationConvention = embeddingModelObservationConvention;
    }

    private static ObjectProvider<VectorStoreObservationConvention>
            getCustomObservationConvention() {
        return new ObjectProvider<>() {
            @Override
            public @NonNull VectorStoreObservationConvention getObject() throws BeansException {
                return new DefaultVectorStoreObservationConvention();
            }
        };
    }

    private static BatchingStrategy getBatchingStrategy() {
        return SpringUtil.getBean(BatchingStrategy.class);
    }

    private String buildClientCacheKey(Class<?> clazz, Object... params) {
        if (ArrayUtil.isEmpty(params)) {
            return clazz.getName();
        }
        return StrUtil.format("{}#{}", clazz.getName(), ArrayUtil.join(params, "_"));
    }

    private OllamaChatModel buildOllamaChatModel(String url) {
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(url).build())
                .toolCallingManager(toolCallingManager)
                .build();
    }

    /** 可参考 {@link OpenAiChatAutoConfiguration} 的 openAiChatModel 方法 */
    private OpenAiChatModel buildOpenAiChatModel(String openAiToken, String url) {
        OpenAiChatAutoConfiguration openAiChatAutoConfiguration = new OpenAiChatAutoConfiguration();
        OpenAiChatProperties openAiChatProperties = new OpenAiChatProperties();
        openAiChatProperties.setApiKey(openAiToken);
        openAiChatProperties.setBaseUrl(url);
        OpenAiApi openAiApi = OpenAiApi.builder().apiKey(openAiToken).baseUrl(url).build();
        return openAiChatAutoConfiguration.openAiChatModel(
                openAiApi,
                openAiChatProperties,
                toolCallingManager,
                retryTemplate,
                observationRegistry,
                chatObservationConvention,
                toolExecutionEligibilityPredicate);
    }

    /** 可参考 {@link AzureOpenAiChatAutoConfiguration} */
    private AzureOpenAiChatModel buildAzureOpenAiChatModel(String apiKey, String url) {
        AzureOpenAiClientBuilderConfiguration azureOpenAiClientBuilderConfiguration =
                new AzureOpenAiClientBuilderConfiguration();
        AzureOpenAiConnectionProperties connectionProperties =
                new AzureOpenAiConnectionProperties();
        connectionProperties.setApiKey(apiKey);
        connectionProperties.setEndpoint(url);
        OpenAIClientBuilder openAIClient =
                azureOpenAiClientBuilderConfiguration.openAIClientBuilder(
                        connectionProperties, null);
        AzureOpenAiChatProperties chatProperties =
                SpringUtil.getBean(AzureOpenAiChatProperties.class);
        AzureOpenAiChatAutoConfiguration azureOpenAiChatAutoConfiguration =
                new AzureOpenAiChatAutoConfiguration();
        return azureOpenAiChatAutoConfiguration.azureOpenAiChatModel(
                openAIClient,
                chatProperties,
                toolCallingManager,
                observationRegistry,
                chatObservationConvention,
                toolExecutionEligibilityPredicate);
    }

    @Override
    public ChatModel getOrCreateChatModel(AiPlatformEnum platform, String apiKey, String url) {
        String urlOrDefault = StrUtil.blankToDefault(url, platform.getUrl());
        String cacheKey = buildClientCacheKey(ChatModel.class, platform, apiKey, urlOrDefault);
        return Singleton.get(
                cacheKey,
                (Func0<ChatModel>)
                        () ->
                                switch (platform) {
                                    case AiPlatformEnum.ZHI_PU ->
                                            buildZhiPuChatModel(apiKey, urlOrDefault);
                                    case AiPlatformEnum.MINI_MAX ->
                                            buildMiniMaxChatModel(apiKey, urlOrDefault);
                                    case AiPlatformEnum.OPENAI,
                                            AiPlatformEnum.YI_YAN,
                                            AiPlatformEnum.TONG_YI,
                                            AiPlatformEnum.MOONSHOT,
                                            AiPlatformEnum.STABLE_DIFFUSION,
                                            AiPlatformEnum.DEEP_SEEK,
                                            AiPlatformEnum.DOU_BAO,
                                            AiPlatformEnum.HUN_YUAN,
                                            AiPlatformEnum.SILICON_FLOW,
                                            AiPlatformEnum.XING_HUO,
                                            AiPlatformEnum.BAI_CHUAN ->
                                            buildOpenAiChatModel(apiKey, urlOrDefault);
                                    case AiPlatformEnum.AZURE_OPENAI ->
                                            buildAzureOpenAiChatModel(apiKey, urlOrDefault);
                                    case AiPlatformEnum.OLLAMA ->
                                            buildOllamaChatModel(urlOrDefault);
                                    default ->
                                            throw new IllegalArgumentException(
                                                    StrUtil.format("未知平台({})", platform));
                                });
    }

    @Override
    public ImageModel getOrCreateImageModel(AiPlatformEnum platform, String apiKey, String url) {
        String urlOrDefault = StrUtil.blankToDefault(url, platform.getUrl());
        return switch (platform) {
            case AiPlatformEnum.ZHI_PU -> buildZhiPuAiImageModel(apiKey, urlOrDefault);
            case AiPlatformEnum.OPENAI,
                    AiPlatformEnum.YI_YAN,
                    AiPlatformEnum.TONG_YI,
                    AiPlatformEnum.MOONSHOT ->
                    buildOpenAiImageModel(apiKey, urlOrDefault);
            case AiPlatformEnum.SILICON_FLOW -> buildSiliconFlowImageModel(apiKey, urlOrDefault);
            case AiPlatformEnum.STABLE_DIFFUSION ->
                    buildStabilityAiImageModel(apiKey, urlOrDefault);
            default -> throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
        };
    }

    @Override
    public MidjourneyApi getOrCreateMidjourneyApi(String apiKey, String url) {
        String cacheKey =
                buildClientCacheKey(
                        MidjourneyApi.class, AiPlatformEnum.MIDJOURNEY.getPlatform(), apiKey, url);
        return Singleton.get(
                cacheKey,
                (Func0<MidjourneyApi>)
                        () -> {
                            AiModelProperties.MidjourneyProperties properties =
                                    SpringUtil.getBean(AiModelProperties.class).getMidjourney();
                            return new MidjourneyApi(url, apiKey, properties.getNotifyUrl());
                        });
    }

    @Override
    @SuppressWarnings("EnhancedSwitchMigration")
    public EmbeddingModel getOrCreateEmbeddingModel(
            AiPlatformEnum platform, String apiKey, String url, String model) {
        String urlOrDefault = StrUtil.blankToDefault(url, platform.getUrl());
        String modelOrDefault = StrUtil.blankToDefault(model, platform.getDefaultEmbedModel());
        String cacheKey =
                buildClientCacheKey(
                        EmbeddingModel.class, platform, apiKey, urlOrDefault, modelOrDefault);
        return Singleton.get(
                cacheKey,
                (Func0<EmbeddingModel>)
                        () -> {
                            switch (platform) {
                                case AiPlatformEnum.ZHI_PU:
                                    return buildZhiPuEmbeddingModel(
                                            apiKey, urlOrDefault, modelOrDefault);
                                case AiPlatformEnum.MINI_MAX:
                                    return buildMiniMaxEmbeddingModel(
                                            apiKey, urlOrDefault, modelOrDefault);
                                case AiPlatformEnum.OPENAI,
                                AiPlatformEnum.YI_YAN,
                                AiPlatformEnum.TONG_YI,
                                AiPlatformEnum.MOONSHOT,
                                AiPlatformEnum.SILICON_FLOW:
                                    return buildOpenAiEmbeddingModel(
                                            apiKey, urlOrDefault, modelOrDefault);
                                case AiPlatformEnum.AZURE_OPENAI:
                                    return buildAzureOpenAiEmbeddingModel(
                                            apiKey, urlOrDefault, modelOrDefault);
                                case AiPlatformEnum.OLLAMA:
                                    return buildOllamaEmbeddingModel(urlOrDefault, modelOrDefault);
                                default:
                                    throw new IllegalArgumentException(
                                            StrUtil.format("未知平台({})", platform));
                            }
                        });
    }

    @Override
    public VectorStore getOrCreateVectorStore(
            Class<? extends VectorStore> type,
            EmbeddingModel embeddingModel,
            Map<String, Class<?>> metadataFields) {
        String cacheKey = buildClientCacheKey(VectorStore.class, embeddingModel, type);
        return Singleton.get(
                cacheKey,
                (Func0<VectorStore>)
                        () -> {
                            if (type == SimpleVectorStore.class) {
                                return buildSimpleVectorStore(embeddingModel);
                            }
                            if (type == QdrantVectorStore.class) {
                                return buildQdrantVectorStore(embeddingModel);
                            }
                            if (type == MilvusVectorStore.class) {
                                return buildMilvusVectorStore(embeddingModel);
                            }
                            throw new IllegalArgumentException(StrUtil.format("未知类型({})", type));
                        });
    }

    private ZhiPuAiChatModel buildZhiPuChatModel(String apiKey, String url) {
        ZhiPuAiChatAutoConfiguration openAiChatAutoConfiguration =
                new ZhiPuAiChatAutoConfiguration();

        ZhiPuAiChatProperties zhiPuAiChatProperties = new ZhiPuAiChatProperties();
        zhiPuAiChatProperties.setApiKey(apiKey);
        zhiPuAiChatProperties.setBaseUrl(url);

        ZhiPuAiConnectionProperties zhiPuAiConnectionProperties = new ZhiPuAiConnectionProperties();
        zhiPuAiConnectionProperties.setApiKey(apiKey);
        zhiPuAiConnectionProperties.setBaseUrl(url);

        return openAiChatAutoConfiguration.zhiPuAiChatModel(
                zhiPuAiConnectionProperties,
                zhiPuAiChatProperties,
                restClientBuilderProvider,
                webClientBuilderProvider,
                retryTemplate,
                responseErrorHandler,
                observationRegistry,
                chatObservationConvention,
                toolCallingManager,
                toolExecutionEligibilityPredicate);
    }

    private ZhiPuAiImageModel buildZhiPuAiImageModel(String apiKey, String url) {
        ZhiPuAiImageApi zhiPuAiApi =
                StrUtil.isEmpty(url)
                        ? new ZhiPuAiImageApi(apiKey)
                        : new ZhiPuAiImageApi(url, apiKey, RestClient.builder());
        return new ZhiPuAiImageModel(zhiPuAiApi);
    }

    private MiniMaxChatModel buildMiniMaxChatModel(String apiKey, String url) {
        MiniMaxApi miniMaxApi =
                StrUtil.isEmpty(url) ? new MiniMaxApi(apiKey) : new MiniMaxApi(url, apiKey);
        MiniMaxChatOptions options =
                MiniMaxChatOptions.builder()
                        .model(MiniMaxApi.DEFAULT_CHAT_MODEL)
                        .temperature(0.7)
                        .build();
        return new MiniMaxChatModel(miniMaxApi, options);
    }

    private OpenAiImageModel buildOpenAiImageModel(String openAiToken, String url) {
        url = StrUtil.blankToDefault(url, OpenAiApiConstants.DEFAULT_BASE_URL);
        OpenAiImageApi openAiApi =
                OpenAiImageApi.builder().baseUrl(url).apiKey(openAiToken).build();
        return new OpenAiImageModel(openAiApi);
    }

    private SiliconFlowImageModel buildSiliconFlowImageModel(String apiToken, String url) {
        url = StrUtil.blankToDefault(url, SiliconFlowApiConstants.DEFAULT_BASE_URL);
        SiliconFlowImageApi openAiApi = new SiliconFlowImageApi(url, apiToken);
        return new SiliconFlowImageModel(openAiApi);
    }

    /** 可参考 {@link StabilityAiImageAutoConfiguration} 的 stabilityAiImageModel 方法 */
    private StabilityAiImageModel buildStabilityAiImageModel(String apiKey, String url) {
        url = StrUtil.blankToDefault(url, StabilityAiApi.DEFAULT_BASE_URL);
        StabilityAiApi stabilityAiApi =
                new StabilityAiApi(apiKey, StabilityAiApi.DEFAULT_IMAGE_MODEL, url);
        return new StabilityAiImageModel(stabilityAiApi);
    }

    private ZhiPuAiEmbeddingModel buildZhiPuEmbeddingModel(
            String apiKey, String url, String model) {
        ZhiPuAiConnectionProperties zhiPuAiConnectionProperties = new ZhiPuAiConnectionProperties();
        zhiPuAiConnectionProperties.setApiKey(apiKey);
        zhiPuAiConnectionProperties.setBaseUrl(url);

        ZhiPuAiEmbeddingProperties zhiPuAiEmbeddingProperties = new ZhiPuAiEmbeddingProperties();
        zhiPuAiEmbeddingProperties.setMetadataMode(MetadataMode.EMBED);
        zhiPuAiEmbeddingProperties.getOptions().setModel(model);

        ZhiPuAiEmbeddingAutoConfiguration zhiPuAiEmbeddingAutoConfiguration =
                new ZhiPuAiEmbeddingAutoConfiguration();
        return zhiPuAiEmbeddingAutoConfiguration.zhiPuAiEmbeddingModel(
                zhiPuAiConnectionProperties,
                zhiPuAiEmbeddingProperties,
                restClientBuilderProvider,
                webClientBuilderProvider,
                retryTemplate,
                responseErrorHandler,
                observationRegistry,
                embeddingModelObservationConvention);
    }

    private EmbeddingModel buildMiniMaxEmbeddingModel(String apiKey, String url, String model) {
        MiniMaxEmbeddingAutoConfiguration miniMaxEmbeddingAutoConfiguration =
                new MiniMaxEmbeddingAutoConfiguration();
        MiniMaxConnectionProperties miniMaxConnectionProperties = new MiniMaxConnectionProperties();
        miniMaxConnectionProperties.setApiKey(apiKey);
        miniMaxConnectionProperties.setBaseUrl(url);
        MiniMaxEmbeddingProperties miniMaxEmbeddingProperties = new MiniMaxEmbeddingProperties();
        miniMaxEmbeddingProperties.setMetadataMode(MetadataMode.EMBED);
        miniMaxEmbeddingProperties.getOptions().setModel(model);

        return miniMaxEmbeddingAutoConfiguration.miniMaxEmbeddingModel(
                miniMaxConnectionProperties,
                miniMaxEmbeddingProperties,
                restClientBuilderProvider,
                retryTemplate,
                responseErrorHandler,
                observationRegistry,
                embeddingModelObservationConvention);
    }

    private OllamaEmbeddingModel buildOllamaEmbeddingModel(String url, String model) {
        OllamaApi ollamaApi =
                OllamaApi.builder()
                        .baseUrl(url)
                        .restClientBuilder(
                                restClientBuilderProvider.getIfAvailable(RestClient::builder))
                        .responseErrorHandler(responseErrorHandler)
                        .webClientBuilder(
                                webClientBuilderProvider.getIfAvailable(WebClient::builder))
                        .build();

        OllamaEmbeddingProperties properties = new OllamaEmbeddingProperties();
        properties.setModel(model);

        OllamaInitializationProperties initProperties = new OllamaInitializationProperties();

        OllamaEmbeddingAutoConfiguration ollamaEmbeddingAutoConfiguration =
                new OllamaEmbeddingAutoConfiguration();

        return ollamaEmbeddingAutoConfiguration.ollamaEmbeddingModel(
                ollamaApi,
                properties,
                initProperties,
                observationRegistry,
                embeddingModelObservationConvention);
    }

    private OpenAiEmbeddingModel buildOpenAiEmbeddingModel(
            String openAiToken, String url, String model) {
        OpenAiEmbeddingAutoConfiguration openAiEmbeddingAutoConfiguration =
                new OpenAiEmbeddingAutoConfiguration();
        OpenAiEmbeddingProperties openAiEmbeddingProperties = new OpenAiEmbeddingProperties();
        openAiEmbeddingProperties.setApiKey(openAiToken);
        openAiEmbeddingProperties.setBaseUrl(url);
        openAiEmbeddingProperties.setMetadataMode(MetadataMode.EMBED);
        openAiEmbeddingProperties.getOptions().setModel(model);
        OpenAiConnectionProperties openAiConnectionProperties = new OpenAiConnectionProperties();
        openAiConnectionProperties.setApiKey(openAiToken);
        openAiConnectionProperties.setBaseUrl(url);

        return openAiEmbeddingAutoConfiguration.openAiEmbeddingModel(
                openAiConnectionProperties,
                openAiEmbeddingProperties,
                restClientBuilderProvider,
                webClientBuilderProvider,
                retryTemplate,
                responseErrorHandler,
                observationRegistry,
                embeddingModelObservationConvention);
    }

    private AzureOpenAiEmbeddingModel buildAzureOpenAiEmbeddingModel(
            String apiKey, String url, String model) {
        AzureOpenAiEmbeddingAutoConfiguration azureOpenAiAutoConfiguration =
                new AzureOpenAiEmbeddingAutoConfiguration();
        AzureOpenAiClientBuilderConfiguration azureOpenAiClientBuilderConfiguration =
                new AzureOpenAiClientBuilderConfiguration();

        AzureOpenAiConnectionProperties connectionProperties =
                new AzureOpenAiConnectionProperties();
        connectionProperties.setApiKey(apiKey);
        connectionProperties.setEndpoint(url);

        OpenAIClientBuilder openAIClient =
                azureOpenAiClientBuilderConfiguration.openAIClientBuilder(
                        connectionProperties, null);

        AzureOpenAiEmbeddingProperties embeddingProperties =
                SpringUtil.getBean(AzureOpenAiEmbeddingProperties.class);
        return azureOpenAiAutoConfiguration.azureOpenAiEmbeddingModel(
                openAIClient,
                embeddingProperties,
                observationRegistry,
                embeddingModelObservationConvention);
    }

    // ========== 各种创建 VectorStore 的方法 ==========

    /** 注意：仅适合本地测试使用，生产建议还是使用 Qdrant、Milvus 等 */
    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private SimpleVectorStore buildSimpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        File file =
                new File(
                        StrUtil.format(
                                "{}/vector_store/simple_{}.json",
                                FileUtil.getUserHomePath(),
                                embeddingModel.getClass().getSimpleName()));
        if (!file.exists()) {
            FileUtil.mkParentDirs(file);
            file.createNewFile();
        } else if (file.length() > 0) {
            vectorStore.load(file);
        }
        // 定时持久化，每分钟一次
        Timer timer = new Timer("SimpleVectorStoreTimer-" + file.getAbsolutePath());
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        vectorStore.save(file);
                    }
                },
                Duration.ofMinutes(1).toMillis(),
                Duration.ofMinutes(1).toMillis());
        // 关闭时，进行持久化
        RuntimeUtil.addShutdownHook(() -> vectorStore.save(file));
        return vectorStore;
    }

    /** 参考 {@link QdrantVectorStoreAutoConfiguration} 的 vectorStore 方法 */
    @SneakyThrows
    private QdrantVectorStore buildQdrantVectorStore(EmbeddingModel embeddingModel) {
        QdrantVectorStoreAutoConfiguration configuration = new QdrantVectorStoreAutoConfiguration();
        QdrantVectorStoreProperties properties =
                SpringUtil.getBean(QdrantVectorStoreProperties.class);
        // 参考 QdrantVectorStoreAutoConfiguration 实现，创建 QdrantClient 对象
        QdrantGrpcClient.Builder grpcClientBuilder =
                QdrantGrpcClient.newBuilder(
                        properties.getHost(), properties.getPort(), properties.isUseTls());
        if (StrUtil.isNotEmpty(properties.getApiKey())) {
            grpcClientBuilder.withApiKey(properties.getApiKey());
        }
        QdrantClient qdrantClient = new QdrantClient(grpcClientBuilder.build());
        QdrantVectorStore vectorStore =
                configuration.vectorStore(
                        embeddingModel,
                        properties,
                        qdrantClient,
                        observationRegistry,
                        getCustomObservationConvention(),
                        getBatchingStrategy());
        vectorStore.afterPropertiesSet();
        return vectorStore;
    }

    /** 参考 {@link MilvusVectorStoreAutoConfiguration} 的 vectorStore 方法 */
    @SneakyThrows
    private MilvusVectorStore buildMilvusVectorStore(EmbeddingModel embeddingModel) {
        MilvusVectorStoreAutoConfiguration configuration = new MilvusVectorStoreAutoConfiguration();
        MilvusVectorStoreProperties serverProperties =
                SpringUtil.getBean(MilvusVectorStoreProperties.class);
        MilvusServiceClientProperties clientProperties =
                SpringUtil.getBean(MilvusServiceClientProperties.class);

        MilvusServiceClient milvusClient =
                configuration.milvusClient(
                        serverProperties,
                        clientProperties,
                        new MilvusServiceClientConnectionDetails() {

                            @Override
                            public String getHost() {
                                return clientProperties.getHost();
                            }

                            @Override
                            public int getPort() {
                                return clientProperties.getPort();
                            }
                        });
        MilvusVectorStore vectorStore =
                configuration.vectorStore(
                        milvusClient,
                        embeddingModel,
                        serverProperties,
                        getBatchingStrategy(),
                        observationRegistry,
                        getCustomObservationConvention());

        vectorStore.afterPropertiesSet();
        return vectorStore;
    }
}
