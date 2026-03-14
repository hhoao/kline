package com.hhoa.kline.core.core.shared.api;

import com.hhoa.kline.core.core.shared.storage.ModelConfig;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * planModeApiProvider 和 actModeApiProvider
 *
 * <p>注意：由于 Java 不支持多重继承，这里使用组合而不是继承
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApiConfiguration {
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
    private ApiHandlerOptions.RetryAttemptCallback onRetryAttempt;
    private String ocaBaseUrl;
    private String minimaxApiLine;
    private String ocaMode;
    private String planModeApiModelId;
    private Integer planModeThinkingBudgetTokens;
    private String planModeReasoningEffort;
    @Builder.Default private Map<ApiProvider, ModelConfig> planModeModelConfigs = new HashMap<>();
    private String actModeApiModelId;
    private Integer actModeThinkingBudgetTokens;
    private String actModeReasoningEffort;
    @Builder.Default private Map<ApiProvider, ModelConfig> actModeModelConfigs = new HashMap<>();

    /** 按 ApiProvider.getValue() 为 key，如 "mistral" -> key */
    @Builder.Default private Map<String, String> apiKeys = new HashMap<>();

    private String clineAccountId;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsSessionToken;
    private String awsBedrockApiKey;
    private String sapAiCoreClientId;
    private String sapAiCoreClientSecret;
    private String authNonce;

    private ApiProvider planModeApiProvider;
    private ApiProvider actModeApiProvider;

    public String getApiKey(ApiProvider provider) {
        if (provider == null || apiKeys == null) {
            return null;
        }
        return apiKeys.get(provider.getValue());
    }

    public void setApiKey(ApiProvider provider, String value) {
        if (provider == null) {
            return;
        }
        if (apiKeys == null) {
            apiKeys = new HashMap<>();
        }
        if (value != null) {
            apiKeys.put(provider.getValue(), value);
        } else {
            apiKeys.remove(provider.getValue());
        }
    }
}
