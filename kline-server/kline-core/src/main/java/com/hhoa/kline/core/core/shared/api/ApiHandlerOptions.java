package com.hhoa.kline.core.core.shared.api;

import com.hhoa.kline.core.core.shared.LanguageModelChatSelector;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 注意：onRetryAttempt 回调函数在 Java 中使用函数式接口表示 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiHandlerOptions {
    private String ulid;

    private String liteLlmBaseUrl;
    private Boolean liteLlmUsePromptCache;

    @Builder.Default private Map<String, String> openAiHeaders = new HashMap<>();

    private String anthropicBaseUrl;
    private String openRouterProviderSorting;
    private String awsRegion;
    private Boolean awsUseCrossRegionInference;
    private Boolean awsUseGlobalInference;
    private Boolean awsBedrockUsePromptCache;
    private String awsAuthentication;
    private Boolean awsUseProfile;
    private String awsProfile;
    private String awsBedrockEndpoint;
    private String claudeCodePath;
    private String vertexProjectId;
    private String vertexRegion;
    private String openAiBaseUrl;
    private String ollamaBaseUrl;
    private String ollamaApiOptionsCtxNum;
    private String lmStudioBaseUrl;
    private String lmStudioModelId;
    private String lmStudioMaxTokens;
    private String geminiBaseUrl;
    private String requestyBaseUrl;
    private Integer fireworksModelMaxCompletionTokens;
    private Integer fireworksModelMaxTokens;
    private String qwenCodeOauthPath;
    private String azureApiVersion;
    private String qwenApiLine;
    private String moonshotApiLine;
    private String asksageApiUrl;
    private Integer requestTimeoutMs;
    private String sapAiResourceGroup;
    private String sapAiCoreTokenUrl;
    private String sapAiCoreBaseUrl;
    private Boolean sapAiCoreUseOrchestrationMode;
    private String difyBaseUrl;
    private String zaiApiLine;

    private RetryAttemptCallback onRetryAttempt;

    private String ocaBaseUrl;
    private String minimaxApiLine;
    private String ocaMode;

    private String planModeApiModelId;
    private Integer planModeThinkingBudgetTokens;
    private String planModeReasoningEffort;

    private LanguageModelChatSelector planModeVsCodeLmModelSelector;

    private Boolean planModeAwsBedrockCustomSelected;
    private String planModeAwsBedrockCustomModelBaseId;
    private String planModeOpenRouterModelId;
    private ModelInfo planModeOpenRouterModelInfo;
    private String planModeOpenAiModelId;
    private OpenAiCompatibleModelInfo planModeOpenAiModelInfo;
    private String planModeOllamaModelId;
    private String planModeLmStudioModelId;
    private String planModeLiteLlmModelId;
    private LiteLLMModelInfo planModeLiteLlmModelInfo;
    private String planModeRequestyModelId;
    private ModelInfo planModeRequestyModelInfo;
    private String planModeTogetherModelId;
    private String planModeFireworksModelId;
    private String planModeSapAiCoreModelId;
    private String planModeSapAiCoreDeploymentId;
    private String planModeGroqModelId;
    private ModelInfo planModeGroqModelInfo;
    private String planModeBasetenModelId;
    private ModelInfo planModeBasetenModelInfo;
    private String planModeHuggingFaceModelId;
    private ModelInfo planModeHuggingFaceModelInfo;
    private String planModeHuaweiCloudMaasModelId;
    private ModelInfo planModeHuaweiCloudMaasModelInfo;
    private String planModeVercelAiGatewayModelId;
    private ModelInfo planModeVercelAiGatewayModelInfo;
    private String planModeOcaModelId;
    private OcaModelInfo planModeOcaModelInfo;

    private String actModeApiModelId;
    private Integer actModeThinkingBudgetTokens;
    private String actModeReasoningEffort;

    private LanguageModelChatSelector actModeVsCodeLmModelSelector;

    private Boolean actModeAwsBedrockCustomSelected;
    private String actModeAwsBedrockCustomModelBaseId;
    private String actModeOpenRouterModelId;
    private ModelInfo actModeOpenRouterModelInfo;
    private String actModeOpenAiModelId;
    private OpenAiCompatibleModelInfo actModeOpenAiModelInfo;
    private String actModeOllamaModelId;
    private String actModeLmStudioModelId;
    private String actModeLiteLlmModelId;
    private LiteLLMModelInfo actModeLiteLlmModelInfo;
    private String actModeRequestyModelId;
    private ModelInfo actModeRequestyModelInfo;
    private String actModeTogetherModelId;
    private String actModeFireworksModelId;
    private String actModeSapAiCoreModelId;
    private String actModeSapAiCoreDeploymentId;
    private String actModeGroqModelId;
    private ModelInfo actModeGroqModelInfo;
    private String actModeBasetenModelId;
    private ModelInfo actModeBasetenModelInfo;
    private String actModeHuggingFaceModelId;
    private ModelInfo actModeHuggingFaceModelInfo;
    private String actModeHuaweiCloudMaasModelId;
    private ModelInfo actModeHuaweiCloudMaasModelInfo;
    private String actModeVercelAiGatewayModelId;
    private ModelInfo actModeVercelAiGatewayModelInfo;
    private String actModeOcaModelId;
    private OcaModelInfo actModeOcaModelInfo;

    @FunctionalInterface
    public interface RetryAttemptCallback {
        void onRetryAttempt(int attempt, int maxRetries, long delay, Object error);
    }
}
